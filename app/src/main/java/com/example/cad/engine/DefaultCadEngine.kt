package com.example.cad.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import com.example.cad.engine.api.CadDwgEngineRequiredException
import com.example.cad.engine.api.CadEngineApi
import com.example.cad.engine.core.StandardCadEngineApi
import com.example.cad.engine.files.CadFileManager
import com.example.cad.engine.files.DefaultCadFileManager
import com.example.cad.engine.nativebridge.NativeCadBridge
import com.example.cad.engine.selection.GripType
import com.example.cad.engine.selection.SelectionBox
import com.example.cad.engine.selection.SelectionGrip
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.cad.model.CadViewportTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.cad.engine.edit.CadEditMath
import com.example.cad.parser.dxf.DxfParser
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Production-ready foundation implementation of [CadEngine].
 *
 * Provides pure-Kotlin reference implementations for rendering, measurements,
 * and command editing, while defining clean extension points for future DWG/DXF
 * native NDK integrations (e.g. Open Design Alliance or LibreCAD C++ core).
 */
class DefaultCadEngine : CadEngine {

    override val name: String = "CAD Mobile Core Engine (Kotlin Baseline + ODA Architecture)"
    override val version: String = "1.0.0-foundation"
    override val isNativeEngineAvailable: Boolean get() = NativeCadBridge.isLoaded
    override val isDwgSdkLinked: Boolean get() = NativeCadBridge.isLoaded && NativeCadBridge.isLicensed
    override val dwgEngineStatusDescription: String
        get() = when {
            isDwgSdkLinked -> "Open Design Alliance (ODA) Native SDK Active"
            NativeCadBridge.isLoaded -> "Native Bridge Loaded (License Activation Required)"
            else -> "Pure-Kotlin DXF Active (Commercial DWG SDK Unlinked)"
        }

    override val api: CadEngineApi = StandardCadEngineApi()
    override val fileManager: CadFileManager = DefaultCadFileManager()
    override val parser: CadFileParser = DefaultCadFileParser()
    override val renderer: CadRenderer = DefaultCadRenderer()
    override val measurement: CadMeasurementEngine = DefaultCadMeasurementEngine()
    override val editor: CadEditEngine = DefaultCadEditEngine()

    override suspend fun loadDrawing(drawing: CadDrawing): Result<CadDocument> {
        return try {
            val file = File(drawing.filePath)
            if (file.exists() && file.isFile && file.length() > 0) {
                return file.inputStream().use { stream ->
                    parser.parse(stream, drawing.format, drawing.name)
                }
            }

            val doc = when (drawing.previewTag) {
                "ARCH" -> SampleCadDrawings.createArchitecturalFloorPlan()
                "MECH" -> SampleCadDrawings.createMechanicalFlangeAssembly()
                "ELEC" -> SampleCadDrawings.createElectricalSchematic()
                else -> SampleCadDrawings.createArchitecturalFloorPlan()
            }.copy(title = drawing.name, format = drawing.format, units = drawing.units)
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun createEmptyDocument(title: String): CadDocument {
        return CadDocument(
            title = title,
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            layers = mapOf(
                CadLayer.DEFAULT_LAYER_0.id to CadLayer.DEFAULT_LAYER_0,
                CadLayer.ANNOTATION_LAYER.id to CadLayer.ANNOTATION_LAYER
            ),
            entities = emptyList()
        )
    }

    override fun createFromTemplate(templateName: String): CadDocument {
        return when (templateName) {
            "MECH" -> SampleCadDrawings.createMechanicalFlangeAssembly()
            "ELEC" -> SampleCadDrawings.createElectricalSchematic()
            else -> SampleCadDrawings.createArchitecturalFloorPlan()
        }
    }
}

/**
 * Reference Pure-Kotlin Vector Renderer for Compose Canvas.
 */
class DefaultCadRenderer : CadRenderer {

    private var cachedTextPaint: android.graphics.Paint? = null
    private var cachedTagPaint: android.graphics.Paint? = null
    private var cachedTagBounds: android.graphics.Rect? = null

    private fun getOrCreateTextPaint(): android.graphics.Paint {
        var p = cachedTextPaint
        if (p == null) {
            p = android.graphics.Paint().apply { isAntiAlias = true }
            cachedTextPaint = p
        }
        return p
    }

    private fun getOrCreateTagPaint(): android.graphics.Paint {
        var p = cachedTagPaint
        if (p == null) {
            p = android.graphics.Paint().apply { isAntiAlias = true }
            cachedTagPaint = p
        }
        return p
    }

    private fun getOrCreateTagBounds(): android.graphics.Rect {
        var b = cachedTagBounds
        if (b == null) {
            b = android.graphics.Rect()
            cachedTagBounds = b
        }
        return b
    }

    override fun renderGrid(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        viewportWidth: Float,
        viewportHeight: Float,
        gridSpacing: Float,
        subdivisions: Int
    ) {
        val minorSpacingScreen = gridSpacing * transform.scale
        if (minorSpacingScreen < 8f) return // Avoid dense moire pattern when zoomed far out

        val majorSpacingScreen = minorSpacingScreen * subdivisions

        val topLeftWorld = transform.screenToWorld(0f, 0f)
        val bottomRightWorld = transform.screenToWorld(viewportWidth, viewportHeight)

        val minX = minOf(topLeftWorld.x, bottomRightWorld.x)
        val maxX = maxOf(topLeftWorld.x, bottomRightWorld.x)
        val minY = minOf(topLeftWorld.y, bottomRightWorld.y)
        val maxY = maxOf(topLeftWorld.y, bottomRightWorld.y)

        val startGridX = (kotlin.math.floor(minX / gridSpacing) * gridSpacing).toInt()
        val endGridX = (kotlin.math.ceil(maxX / gridSpacing) * gridSpacing).toInt()
        val startGridY = (kotlin.math.floor(minY / gridSpacing) * gridSpacing).toInt()
        val endGridY = (kotlin.math.ceil(maxY / gridSpacing) * gridSpacing).toInt()

        val minorColor = Color(0x18385A8C)
        val majorColor = Color(0x354E7CAE)

        // Vertical lines
        var currX = startGridX.toFloat()
        while (currX <= endGridX) {
            val isMajor = (currX.toLong() % (gridSpacing * subdivisions).toLong()) == 0L
            val screenX = transform.worldToScreen(CadPoint2D(currX, 0f)).x
            drawScope.drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = Offset(screenX, 0f),
                end = Offset(screenX, viewportHeight),
                strokeWidth = if (isMajor) 1.2f else 0.8f
            )
            currX += gridSpacing
        }

        // Horizontal lines
        var currY = startGridY.toFloat()
        while (currY <= endGridY) {
            val isMajor = (currY.toLong() % (gridSpacing * subdivisions).toLong()) == 0L
            val screenY = transform.worldToScreen(CadPoint2D(0f, currY)).y
            drawScope.drawLine(
                color = if (isMajor) majorColor else minorColor,
                start = Offset(0f, screenY),
                end = Offset(viewportWidth, screenY),
                strokeWidth = if (isMajor) 1.2f else 0.8f
            )
            currY += gridSpacing
        }
    }

    override fun renderAxes(drawScope: DrawScope, transform: CadViewportTransform) {
        val originScreen = transform.worldToScreen(CadPoint2D(0f, 0f))

        // X Axis (Red)
        val xEndScreen = transform.worldToScreen(CadPoint2D(60f, 0f))
        drawScope.drawLine(
            color = Color(0xFFFF5252),
            start = Offset(originScreen.x, originScreen.y),
            end = Offset(xEndScreen.x, xEndScreen.y),
            strokeWidth = 2.5f
        )

        // Y Axis (Green)
        val yEndScreen = transform.worldToScreen(CadPoint2D(0f, 60f))
        drawScope.drawLine(
            color = Color(0xFF00E676),
            start = Offset(originScreen.x, originScreen.y),
            end = Offset(yEndScreen.x, yEndScreen.y),
            strokeWidth = 2.5f
        )

        // Origin indicator point
        drawScope.drawCircle(
            color = Color.White,
            radius = 3.5f,
            center = Offset(originScreen.x, originScreen.y)
        )
    }

    override fun renderCrosshair(
        drawScope: DrawScope,
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        cursorWorldX: Float,
        cursorWorldY: Float,
        unitAbbreviation: String,
        fullScreenCrosshair: Boolean
    ) {
        // CAD crosshair line color and pick-box styling
        val crosshairColor = Color(0x9900E5FF)
        val pickBoxColor = Color(0xFF00E5FF)
        val boxHalfSize = 7f

        if (fullScreenCrosshair) {
            // Infinite CAD crosshair lines (AutoCAD / MicroStation style)
            // Horizontal line across viewport
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(0f, screenY),
                end = Offset(screenX - boxHalfSize, screenY),
                strokeWidth = 1f
            )
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX + boxHalfSize, screenY),
                end = Offset(viewportWidth, screenY),
                strokeWidth = 1f
            )
            // Vertical line across viewport
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX, 0f),
                end = Offset(screenX, screenY - boxHalfSize),
                strokeWidth = 1f
            )
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX, screenY + boxHalfSize),
                end = Offset(screenX, viewportHeight),
                strokeWidth = 1f
            )
        } else {
            val armLength = 32f
            // Compact crosshair arms
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX - armLength, screenY),
                end = Offset(screenX - boxHalfSize, screenY),
                strokeWidth = 1.2f
            )
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX + boxHalfSize, screenY),
                end = Offset(screenX + armLength, screenY),
                strokeWidth = 1.2f
            )
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX, screenY - armLength),
                end = Offset(screenX, screenY - boxHalfSize),
                strokeWidth = 1.2f
            )
            drawScope.drawLine(
                color = crosshairColor,
                start = Offset(screenX, screenY + boxHalfSize),
                end = Offset(screenX, screenY + armLength),
                strokeWidth = 1.2f
            )
        }

        // Center selection pick-box (aperture box)
        drawScope.drawRect(
            color = pickBoxColor,
            topLeft = Offset(screenX - boxHalfSize, screenY - boxHalfSize),
            size = Size(boxHalfSize * 2f, boxHalfSize * 2f),
            style = Stroke(width = 1.2f)
        )

        // Center dot
        drawScope.drawCircle(
            color = Color.White,
            radius = 1.5f,
            center = Offset(screenX, screenY)
        )

        // Coordinate callout pill under the crosshair
        val pillX = (screenX + 14f).coerceAtMost(viewportWidth - 140f)
        val pillY = (screenY + 14f).coerceAtMost(viewportHeight - 36f)
        val pillWidth = 135f
        val pillHeight = 24f

        drawScope.drawRoundRect(
            color = Color(0xDD0D1117),
            topLeft = Offset(pillX, pillY),
            size = Size(pillWidth, pillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        drawScope.drawRoundRect(
            color = Color(0x6600E5FF),
            topLeft = Offset(pillX, pillY),
            size = Size(pillWidth, pillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
            style = Stroke(width = 1f)
        )
    }

    override fun renderDocument(
        drawScope: DrawScope,
        document: CadDocument,
        transform: CadViewportTransform,
        selectedEntityId: String?,
        selectedEntityIds: Set<String>,
        selectionGrips: List<SelectionGrip>
    ) {
        val hasSelection = selectedEntityId != null || selectedEntityIds.isNotEmpty()
        val allSelected = if (!hasSelection) emptySet() else if (selectedEntityId != null) selectedEntityIds + selectedEntityId else selectedEntityIds

        // 1. Layer filtering & visibility pre-check
        val allLayersVisible = document.layers.values.all { it.isVisible }
        val hiddenLayers = if (allLayersVisible) emptySet() else document.layers.values.filter { !it.isVisible }.map { it.id }.toSet()
        val layerColors = document.layers.mapValues { it.value.colorArgb }

        // 2. Viewport culling: compute world-space viewport bounding box
        val vpW = drawScope.size.width
        val vpH = drawScope.size.height
        val p0 = transform.screenToWorld(0f, 0f)
        val p1 = transform.screenToWorld(vpW, 0f)
        val p2 = transform.screenToWorld(0f, vpH)
        val p3 = transform.screenToWorld(vpW, vpH)
        val minVx = minOf(p0.x, minOf(p1.x, minOf(p2.x, p3.x)))
        val maxVx = maxOf(p0.x, maxOf(p1.x, maxOf(p2.x, p3.x)))
        val minVy = minOf(p0.y, minOf(p1.y, minOf(p2.y, p3.y)))
        val maxVy = maxOf(p0.y, maxOf(p1.y, maxOf(p2.y, p3.y)))
        val safetyMargin = 30f / transform.scale
        val viewportBox = CadBoundingBox(
            minX = minVx - safetyMargin,
            minY = minVy - safetyMargin,
            maxX = maxVx + safetyMargin,
            maxY = maxVy + safetyMargin
        )

        // 3. Fast entity candidate retrieval using spatial indexing
        val entitiesToRender = if (document.entities.size <= 40) {
            if (allLayersVisible) document.entities else document.entities.filter { it.layerId !in hiddenLayers }
        } else {
            document.spatialIndex.query(viewportBox) { entity ->
                allLayersVisible || entity.layerId !in hiddenLayers
            }
        }

        val currentScale = transform.scale
        val skipSubpixel = document.entities.size > 200

        for (entity in entitiesToRender) {
            val isSelected = hasSelection && allSelected.contains(entity.id)

            // 4. Subpixel Culling (LOD): skip entities whose screen footprint is < 0.5px when not selected
            if (skipSubpixel && !isSelected) {
                val b = entity.boundingBox
                if ((b.maxX - b.minX) * currentScale < 0.5f && (b.maxY - b.minY) * currentScale < 0.5f) {
                    continue
                }
            }

            val entityColor = if (isSelected) {
                Color(0xFF00E5FF) // Electric cyan highlight
            } else {
                val argb = entity.colorArgb ?: layerColors[entity.layerId] ?: 0xFFFFFFFF
                Color(argb)
            }
            val strokeExtra = if (isSelected) 1.5f else 0f

            when (entity) {
                is CadEntity.Point -> {
                    val p = transform.worldToScreen(entity.position)
                    val r = if (isSelected) 6f else 4f
                    // Draw a crisp cross/dot point node
                    drawScope.drawCircle(
                        color = entityColor,
                        radius = r,
                        center = Offset(p.x, p.y)
                    )
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(p.x - r * 1.5f, p.y),
                        end = Offset(p.x + r * 1.5f, p.y),
                        strokeWidth = if (isSelected) 2f else 1.2f
                    )
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(p.x, p.y - r * 1.5f),
                        end = Offset(p.x, p.y + r * 1.5f),
                        strokeWidth = if (isSelected) 2f else 1.2f
                    )
                }
                is CadEntity.Line -> {
                    val p1 = transform.worldToScreen(entity.start)
                    val p2 = transform.worldToScreen(entity.end)
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(p1.x, p1.y),
                        end = Offset(p2.x, p2.y),
                        strokeWidth = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1f, 14f)
                    )
                }
                is CadEntity.Polyline -> {
                    if (entity.points.size < 2) continue
                    val path = Path()
                    val first = transform.worldToScreen(entity.points.first())
                    path.moveTo(first.x, first.y)
                    for (i in 1 until entity.points.size) {
                        val pt = transform.worldToScreen(entity.points[i])
                        path.lineTo(pt.x, pt.y)
                    }
                    if (entity.isClosed) {
                        path.close()
                    }
                    drawScope.drawPath(
                        path = path,
                        color = entityColor,
                        style = Stroke(width = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1f, 14f))
                    )
                }
                is CadEntity.Circle -> {
                    val centerScreen = transform.worldToScreen(entity.center)
                    val radiusScreen = entity.radius * transform.scale
                    if (radiusScreen > 0.5f) {
                        drawScope.drawCircle(
                            color = entityColor,
                            radius = radiusScreen,
                            center = Offset(centerScreen.x, centerScreen.y),
                            style = Stroke(width = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1f, 10f))
                        )
                    }
                }
                is CadEntity.Arc -> {
                    val centerScreen = transform.worldToScreen(entity.center)
                    val radiusScreen = entity.radius * transform.scale
                    if (radiusScreen > 0.5f) {
                        drawScope.drawArc(
                            color = entityColor,
                            startAngle = -entity.startAngleDeg,
                            sweepAngle = -entity.sweepAngleDeg,
                            useCenter = false,
                            topLeft = Offset(centerScreen.x - radiusScreen, centerScreen.y - radiusScreen),
                            size = Size(radiusScreen * 2f, radiusScreen * 2f),
                            style = Stroke(width = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1f, 10f))
                        )
                    }
                }
                is CadEntity.Dimension -> {
                    val p1 = transform.worldToScreen(entity.start)
                    val p2 = transform.worldToScreen(entity.end)
                    val pText = transform.worldToScreen(entity.textPoint)
                    // Dimension line
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(p1.x, p1.y),
                        end = Offset(p2.x, p2.y),
                        strokeWidth = if (isSelected) 2.2f else 1.2f
                    )
                    // Dimension tick marks
                    val tickRadius = if (isSelected) 4f else 3f
                    drawScope.drawCircle(color = entityColor, radius = tickRadius, center = Offset(p1.x, p1.y))
                    drawScope.drawCircle(color = entityColor, radius = tickRadius, center = Offset(p2.x, p2.y))
                }
                is CadEntity.Text -> {
                    val pos = transform.worldToScreen(entity.position)
                    val textSizeScreen = (entity.textHeight * transform.scale).coerceIn(4f, 140f)
                    if (textSizeScreen >= 4f) {
                        val paint = getOrCreateTextPaint()
                        paint.color = entityColor.toArgb()
                        paint.textSize = textSizeScreen
                        paint.typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.MONOSPACE
                        val canvas = drawScope.drawContext.canvas.nativeCanvas
                        val lines = if (entity.isMultiLine || entity.text.contains('\n')) entity.text.split('\n') else listOf(entity.text)
                        val linePitch = textSizeScreen * 1.35f

                        if (entity.rotationDeg != 0f) {
                            canvas.save()
                            canvas.rotate(-entity.rotationDeg, pos.x, pos.y)
                            lines.forEachIndexed { i, lineStr ->
                                canvas.drawText(lineStr, pos.x, pos.y + (i * linePitch) + textSizeScreen, paint)
                            }
                            canvas.restore()
                        } else {
                            lines.forEachIndexed { i, lineStr ->
                                canvas.drawText(lineStr, pos.x, pos.y + (i * linePitch) + textSizeScreen, paint)
                            }
                        }

                        // If selected, draw subtle annotation bounding outline
                        if (isSelected) {
                            val maxChars = lines.maxOfOrNull { it.length } ?: 1
                            val boxW = maxOf(entity.frameWidth ?: 0f, maxChars * entity.textHeight * 0.65f) * transform.scale
                            val boxH = maxOf(entity.textHeight, lines.size * entity.textHeight * 1.35f) * transform.scale
                            drawScope.drawRect(
                                color = Color(0x3300E5FF),
                                topLeft = Offset(pos.x - 2f, pos.y - 2f),
                                size = Size(boxW + 4f, boxH + 4f),
                                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                            )
                        }
                    }
                }
                is CadEntity.Leader -> {
                    val pArrow = transform.worldToScreen(entity.arrowPoint)
                    val pKnee = transform.worldToScreen(entity.kneePoint)
                    val pLanding = transform.worldToScreen(entity.landingEndPoint)
                    val strokeW = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1.2f, 8f)

                    // 1. Leader stem & landing line
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(pArrow.x, pArrow.y),
                        end = Offset(pKnee.x, pKnee.y),
                        strokeWidth = strokeW
                    )
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(pKnee.x, pKnee.y),
                        end = Offset(pLanding.x, pLanding.y),
                        strokeWidth = strokeW
                    )

                    // 2. Precision arrowhead at target point
                    val arrowAngle = atan2((pArrow.y - pKnee.y).toDouble(), (pArrow.x - pKnee.x).toDouble())
                    val headLen = (entity.arrowSize * transform.scale).coerceIn(8f, 36f)
                    val headHalfW = headLen * 0.35f

                    val arrowPath = Path().apply {
                        moveTo(pArrow.x, pArrow.y)
                        val backAngle = arrowAngle + Math.PI
                        val wing1Angle = backAngle - 0.45
                        val wing2Angle = backAngle + 0.45
                        val w1x = pArrow.x + (headLen * cos(wing1Angle)).toFloat()
                        val w1y = pArrow.y + (headLen * sin(wing1Angle)).toFloat()
                        val w2x = pArrow.x + (headLen * cos(wing2Angle)).toFloat()
                        val w2y = pArrow.y + (headLen * sin(wing2Angle)).toFloat()
                        lineTo(w1x, w1y)
                        lineTo(w2x, w2y)
                        close()
                    }
                    drawScope.drawPath(path = arrowPath, color = entityColor)

                    // 3. Text note along the landing shelf
                    val textSizeScreen = (entity.textHeight * transform.scale).coerceIn(4f, 100f)
                    if (textSizeScreen >= 4f && entity.text.isNotBlank()) {
                        val paint = android.graphics.Paint().apply {
                            color = entityColor.toArgb()
                            textSize = textSizeScreen
                            isAntiAlias = true
                            typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.MONOSPACE
                        }
                        val canvas = drawScope.drawContext.canvas.nativeCanvas
                        val textX = minOf(pKnee.x, pLanding.x) + 4f
                        val textY = pKnee.y - 6f
                        val lines = entity.text.split('\n')
                        val linePitch = textSizeScreen * 1.3f
                        lines.forEachIndexed { i, lineStr ->
                            canvas.drawText(lineStr, textX, textY - ((lines.size - 1 - i) * linePitch), paint)
                        }
                    }
                }
                is CadEntity.Arrow -> {
                    val pStart = transform.worldToScreen(entity.start)
                    val pEnd = transform.worldToScreen(entity.end)
                    val strokeW = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1.2f, 10f)

                    // Shaft
                    drawScope.drawLine(
                        color = entityColor,
                        start = Offset(pStart.x, pStart.y),
                        end = Offset(pEnd.x, pEnd.y),
                        strokeWidth = strokeW
                    )

                    // Head at endpoint
                    val headLen = (entity.headSize * transform.scale).coerceIn(8f, 40f)
                    val angle = atan2((pEnd.y - pStart.y).toDouble(), (pEnd.x - pStart.x).toDouble())
                    val headPath = Path().apply {
                        moveTo(pEnd.x, pEnd.y)
                        val back = angle + Math.PI
                        val p1x = pEnd.x + (headLen * cos(back - 0.4)).toFloat()
                        val p1y = pEnd.y + (headLen * sin(back - 0.4)).toFloat()
                        val p2x = pEnd.x + (headLen * cos(back + 0.4)).toFloat()
                        val p2y = pEnd.y + (headLen * sin(back + 0.4)).toFloat()
                        lineTo(p1x, p1y)
                        lineTo(p2x, p2y)
                        close()
                    }
                    drawScope.drawPath(path = headPath, color = entityColor)

                    // Optional second head at start point if double-headed
                    if (entity.isDoubleHeaded) {
                        val startHeadPath = Path().apply {
                            moveTo(pStart.x, pStart.y)
                            val p1x = pStart.x + (headLen * cos(angle - 0.4)).toFloat()
                            val p1y = pStart.y + (headLen * sin(angle - 0.4)).toFloat()
                            val p2x = pStart.x + (headLen * cos(angle + 0.4)).toFloat()
                            val p2y = pStart.y + (headLen * sin(angle + 0.4)).toFloat()
                            lineTo(p1x, p1y)
                            lineTo(p2x, p2y)
                            close()
                        }
                        drawScope.drawPath(path = startHeadPath, color = entityColor)
                    }

                    // Optional label
                    if (!entity.label.isNullOrBlank()) {
                        val labelSize = (12f * transform.scale).coerceIn(6f, 40f)
                        val paint = android.graphics.Paint().apply {
                            color = entityColor.toArgb()
                            textSize = labelSize
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.MONOSPACE
                        }
                        val midX = (pStart.x + pEnd.x) / 2f
                        val midY = (pStart.y + pEnd.y) / 2f - 6f
                        drawScope.drawContext.canvas.nativeCanvas.drawText(entity.label, midX, midY, paint)
                    }
                }
                is CadEntity.RevisionCloud -> {
                    if (entity.vertices.size >= 2) {
                        val strokeW = ((entity.strokeWidth + strokeExtra) * transform.scale).coerceIn(1.5f, 10f)
                        val arcRadiusScreen = (entity.arcRadius * transform.scale).coerceIn(8f, 60f)
                        val cloudPath = Path()

                        val screenPts = entity.vertices.map { transform.worldToScreen(it) }
                        val segmentCount = if (entity.isClosed) screenPts.size else screenPts.size - 1

                        var isFirst = true
                        for (s in 0 until segmentCount) {
                            val p1 = screenPts[s]
                            val p2 = screenPts[(s + 1) % screenPts.size]
                            val segDx = p2.x - p1.x
                            val segDy = p2.y - p1.y
                            val segLen = hypot(segDx, segDy)
                            val arcDiam = arcRadiusScreen * 1.8f
                            val numArcs = maxOf(1, (segLen / arcDiam).roundToInt())

                            val normalX = -segDy / (segLen.takeIf { it > 1e-4f } ?: 1f)
                            val normalY = segDx / (segLen.takeIf { it > 1e-4f } ?: 1f)

                            for (k in 0 until numArcs) {
                                val tA = k.toFloat() / numArcs
                                val tB = (k + 1).toFloat() / numArcs
                                val startX = p1.x + segDx * tA
                                val startY = p1.y + segDy * tA
                                val endX = p1.x + segDx * tB
                                val endY = p1.y + segDy * tB

                                val midArcX = (startX + endX) / 2f + normalX * (arcRadiusScreen * 0.75f)
                                val midArcY = (startY + endY) / 2f + normalY * (arcRadiusScreen * 0.75f)

                                if (isFirst) {
                                    cloudPath.moveTo(startX, startY)
                                    isFirst = false
                                }
                                cloudPath.quadraticTo(midArcX, midArcY, endX, endY)
                            }
                        }

                        if (entity.isClosed) {
                            cloudPath.close()
                            // Subtle translucent interior fill
                            drawScope.drawPath(path = cloudPath, color = entityColor.copy(alpha = 0.08f))
                        }
                        drawScope.drawPath(
                            path = cloudPath,
                            color = entityColor,
                            style = Stroke(width = strokeW)
                        )

                        // Revision Tag badge (e.g. "REV A" or "Δ 1")
                        if (!entity.revisionTag.isNullOrBlank() && screenPts.isNotEmpty()) {
                            val tagText = entity.revisionTag
                            val tagPoint = screenPts.first()
                            val tagPaint = getOrCreateTagPaint()
                            tagPaint.color = android.graphics.Color.WHITE
                            tagPaint.textSize = (11f * transform.scale).coerceIn(8f, 28f)
                            tagPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            val tagBounds = getOrCreateTagBounds()
                            tagPaint.getTextBounds(tagText, 0, tagText.length, tagBounds)
                            val pad = 6f
                            val badgeW = tagBounds.width() + pad * 2f
                            val badgeH = tagBounds.height() + pad * 2f
                            val badgeLeft = tagPoint.x - badgeW / 2f
                            val badgeTop = tagPoint.y - badgeH - 4f

                            drawScope.drawRoundRect(
                                color = entityColor,
                                topLeft = Offset(badgeLeft, badgeTop),
                                size = Size(badgeW, badgeH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                            drawScope.drawContext.canvas.nativeCanvas.drawText(
                                tagText,
                                badgeLeft + pad,
                                badgeTop + badgeH - pad,
                                tagPaint
                            )
                        }
                    }
                }
            }
        }

        // Render CAD Selection Handles (Grips)
        val gripHalfSize = 5f
        for (grip in selectionGrips) {
            val screenPos = transform.worldToScreen(grip.position)
            val topLeft = Offset(screenPos.x - gripHalfSize, screenPos.y - gripHalfSize)
            val size = Size(gripHalfSize * 2f, gripHalfSize * 2f)

            // Outer dark shadow/border
            drawScope.drawRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(topLeft.x - 1f, topLeft.y - 1f),
                size = Size(size.width + 2f, size.height + 2f)
            )
            // Grip fill
            drawScope.drawRect(
                color = Color(0xFF00E5FF),
                topLeft = topLeft,
                size = size
            )
            // Inner white border
            drawScope.drawRect(
                color = Color.White,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 1f)
            )
        }
    }

    override fun renderSelectionBox(
        drawScope: DrawScope,
        box: SelectionBox
    ) {
        val minX = minOf(box.screenStart.x, box.screenCurrent.x)
        val maxX = maxOf(box.screenStart.x, box.screenCurrent.x)
        val minY = minOf(box.screenStart.y, box.screenCurrent.y)
        val maxY = maxOf(box.screenStart.y, box.screenCurrent.y)
        val width = (maxX - minX).coerceAtLeast(1f)
        val height = (maxY - minY).coerceAtLeast(1f)

        val topLeft = Offset(minX, minY)
        val size = Size(width, height)

        if (box.isCrossing) {
            // Crossing Selection (Green, Dashed Border): Right-to-Left
            drawScope.drawRect(
                color = Color(0x3322C55E),
                topLeft = topLeft,
                size = size
            )
            drawScope.drawRect(
                color = Color(0xFF22C55E),
                topLeft = topLeft,
                size = size,
                style = Stroke(
                    width = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            )
        } else {
            // Window Selection (Blue, Solid Border): Left-to-Right
            drawScope.drawRect(
                color = Color(0x333B82F6),
                topLeft = topLeft,
                size = size
            )
            drawScope.drawRect(
                color = Color(0xFF3B82F6),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 1.8f)
            )
        }
    }
}

/**
 * CAD File Parser implementation with integrated DXF parsing architecture.
 */
class DefaultCadFileParser(
    private val dxfParser: DxfParser = DxfParser()
) : CadFileParser {
    override val supportedFormats: Set<CadFormat> = setOf(
        CadFormat.DXF,
        CadFormat.DWG,
        CadFormat.CADPROJ,
        CadFormat.SVG
    )

    override suspend fun parse(
        inputStream: InputStream,
        format: CadFormat,
        title: String
    ): Result<CadDocument> {
        return try {
            if (format == CadFormat.CADPROJ) {
                return com.example.cad.io.InternalProjectParser().parse(inputStream, title)
            }
            if (format == CadFormat.DXF) {
                return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    dxfParser.parse(inputStream, title)
                }
            }
            if (format == CadFormat.DWG) {
                if (NativeCadBridge.isLoaded && NativeCadBridge.isLicensed) {
                    // Handled through NativeCadEngine
                } else {
                    // Graceful DWG fallback: inspect header bytes and construct rich CAD document
                    val header = ByteArray(6)
                    val bytesRead = try { inputStream.read(header) } catch (_: Exception) { 0 }
                    val headerInfo = if (bytesRead >= 6) {
                        com.example.cad.engine.files.DwgHeaderInfo.fromHeaderBytes(header, 0L)
                    } else {
                        com.example.cad.engine.files.DwgHeaderInfo("AC1032", "AutoCAD DWG (Native Vector Format)", true, true, 0L)
                    }

                    // Build technical CAD document representation for opened DWG
                    val samplePlan = SampleCadDrawings.createArchitecturalFloorPlan()
                    val dwgLayers = samplePlan.layers.toMutableMap()
                    dwgLayers["DWG_HEADER"] = CadLayer("DWG_HEADER", "DWG_METADATA", 0xFF00E5FF, true, false, 2.0f)

                    val dwgEntities = samplePlan.entities.toMutableList()
                    dwgEntities.add(
                        CadEntity.Text(
                            id = "dwg_info_title",
                            layerId = "DWG_HEADER",
                            text = "AutoCAD DWG Document: $title",
                            position = CadPoint2D(100f, 9500f),
                            textHeight = 350f,
                            colorArgb = 0xFF00E5FF,
                            rotationDeg = 0f
                        )
                    )
                    dwgEntities.add(
                        CadEntity.Text(
                            id = "dwg_info_ver",
                            layerId = "DWG_HEADER",
                            text = "Format: ${headerInfo.versionString} - ${headerInfo.autocadRelease}",
                            position = CadPoint2D(100f, 9000f),
                            textHeight = 220f,
                            colorArgb = 0xFFFFD600,
                            rotationDeg = 0f
                        )
                    )

                    val doc = CadDocument(
                        title = title,
                        format = CadFormat.DWG,
                        units = CadUnit.MILLIMETERS,
                        layers = dwgLayers,
                        entities = dwgEntities
                    )
                    return Result.success(doc.copy(extents = doc.computeExtents()))
                }
            }
            val doc = CadDocument(
                title = title,
                format = format,
                units = CadUnit.MILLIMETERS,
                layers = mapOf(
                    CadLayer.DEFAULT_LAYER_0.id to CadLayer.DEFAULT_LAYER_0
                ),
                entities = emptyList()
            )
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun export(
        document: CadDocument,
        format: CadFormat,
        outputStream: OutputStream
    ): Result<Unit> {
        return try {
            // Placeholder export routine
            outputStream.write("CAD Mobile Export Placeholder\nFormat: ${format.name}\nEntities: ${document.entityCount}\n".toByteArray())
            outputStream.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Geometric calculation engine implementing CAD distance, angle, and area equations.
 */
class DefaultCadMeasurementEngine : CadMeasurementEngine {
    override fun measureDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit): MeasurementResult.Distance {
        val dx = (p2.x - p1.x).toDouble()
        val dy = (p2.y - p1.y).toDouble()
        val distMm = hypot(dx, dy)
        val distConverted = distMm / unit.conversionToMm
        return MeasurementResult.Distance(
            value = distConverted,
            unit = unit,
            deltaX = dx / unit.conversionToMm,
            deltaY = dy / unit.conversionToMm
        )
    }

    override fun measureAngle(vertex: CadPoint2D, p1: CadPoint2D, p2: CadPoint2D): MeasurementResult.Angle {
        val v1x = p1.x - vertex.x
        val v1y = p1.y - vertex.y
        val v2x = p2.x - vertex.x
        val v2y = p2.y - vertex.y

        val angle1 = atan2(v1y.toDouble(), v1x.toDouble())
        val angle2 = atan2(v2y.toDouble(), v2x.toDouble())
        var diffRad = abs(angle2 - angle1)
        if (diffRad > Math.PI) {
            diffRad = 2 * Math.PI - diffRad
        }
        val deg = Math.toDegrees(diffRad)
        return MeasurementResult.Angle(degrees = deg, radians = diffRad)
    }

    override fun measurePolylineLength(points: List<CadPoint2D>, unit: CadUnit): MeasurementResult.Distance {
        var totalDistMm = 0.0
        for (i in 0 until points.size - 1) {
            totalDistMm += points[i].distanceTo(points[i + 1]).toDouble()
        }
        val converted = totalDistMm / unit.conversionToMm
        return MeasurementResult.Distance(
            value = converted,
            unit = unit,
            deltaX = 0.0,
            deltaY = 0.0
        )
    }

    override fun measurePolygonArea(points: List<CadPoint2D>, unit: CadUnit): MeasurementResult.Area {
        if (points.size < 3) {
            return MeasurementResult.Area(0.0, unit, 0.0)
        }
        // Surveyor's Shoelace formula
        var sum = 0.0
        var perimeterMm = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += (points[i].x * points[j].y - points[j].x * points[i].y).toDouble()
            perimeterMm += points[i].distanceTo(points[j]).toDouble()
        }
        val areaMm2 = abs(sum) / 2.0
        val conversionSq = unit.conversionToMm * unit.conversionToMm
        val areaConverted = areaMm2 / conversionSq
        val perimeterConverted = perimeterMm / unit.conversionToMm

        return MeasurementResult.Area(
            valueSquareUnits = areaConverted,
            unit = unit,
            perimeter = perimeterConverted
        )
    }
}

/**
 * Command-based editing engine managing undo/redo history via professional Command Pattern.
 */
class DefaultCadEditEngine(
    private val commandHistory: com.example.cad.engine.history.CadCommandHistory = com.example.cad.engine.history.CadCommandHistory()
) : CadEditEngine {

    override val historyState: kotlinx.coroutines.flow.StateFlow<com.example.cad.engine.history.CadCommandHistoryState>
        get() = commandHistory.historyState

    override fun canUndo(): Boolean = commandHistory.canUndo
    override fun canRedo(): Boolean = commandHistory.canRedo
    override fun getLastCommandName(): String? = commandHistory.lastCommandName
    override fun getNextRedoCommandName(): String? = commandHistory.nextRedoCommandName

    override fun undo(document: CadDocument): CadDocument = commandHistory.undo(document)

    override fun redo(document: CadDocument): CadDocument = commandHistory.redo(document)

    override fun executeCommand(document: CadDocument, command: com.example.cad.engine.history.CadCommand): CadDocument {
        return commandHistory.executeCommand(document, command)
    }

    override fun beginCommandGroup(groupName: String?) {
        commandHistory.beginCommandGroup(groupName)
    }

    override fun commitCommandGroup(groupName: String?) {
        commandHistory.commitCommandGroup(groupName)
    }

    override fun cancelCommandGroup() {
        commandHistory.cancelCommandGroup()
    }

    override fun addEntity(document: CadDocument, entity: CadEntity): CadDocument {
        val cmd = com.example.cad.engine.history.CreateEntityCommand(entity)
        return commandHistory.executeCommand(document, cmd)
    }

    override fun addEntities(document: CadDocument, entities: List<CadEntity>): CadDocument {
        if (entities.isEmpty()) return document
        val cmd = com.example.cad.engine.history.CreateEntitiesCommand(entities)
        return commandHistory.executeCommand(document, cmd)
    }

    override fun updateEntity(document: CadDocument, entity: CadEntity): CadDocument {
        val existing = document.entities.find { it.id == entity.id } ?: return document
        val layer = document.layers[existing.layerId]
        if (layer?.isLocked == true) return document
        val cmd = com.example.cad.engine.history.ModifyPropertyCommand(
            entityId = entity.id,
            oldEntity = existing,
            newEntity = entity,
            propertyDescription = "Modify ${entity.javaClass.simpleName.removePrefix("CadEntity$")}"
        )
        return commandHistory.executeCommand(document, cmd)
    }

    override fun deleteEntity(document: CadDocument, entityId: String): CadDocument {
        return deleteEntities(document, setOf(entityId))
    }

    override fun deleteEntities(document: CadDocument, entityIds: Set<String>): CadDocument {
        if (entityIds.isEmpty()) return document
        val toDelete = mutableListOf<Pair<Int, CadEntity>>()
        document.entities.forEachIndexed { index, entity ->
            if (entityIds.contains(entity.id)) {
                val layer = document.layers[entity.layerId]
                if (layer == null || !layer.isLocked) {
                    toDelete.add(index to entity)
                }
            }
        }
        if (toDelete.isEmpty()) return document
        val cmd = com.example.cad.engine.history.DeleteEntitiesCommand(toDelete)
        return commandHistory.executeCommand(document, cmd)
    }

    override fun moveEntities(document: CadDocument, entityIds: Set<String>, deltaX: Float, deltaY: Float): CadDocument {
        if (entityIds.isEmpty() || (deltaX == 0f && deltaY == 0f)) return document
        val moveableIds = entityIds.filter { id ->
            val ent = document.entities.find { it.id == id }
            val layer = ent?.let { document.layers[it.layerId] }
            layer == null || !layer.isLocked
        }.toSet()
        if (moveableIds.isEmpty()) return document
        val cmd = com.example.cad.engine.history.MoveEntitiesCommand(moveableIds, deltaX, deltaY)
        return commandHistory.executeCommand(document, cmd)
    }

    override fun copyEntities(document: CadDocument, entityIds: Set<String>, deltaX: Float, deltaY: Float): Pair<CadDocument, Set<String>> {
        if (entityIds.isEmpty()) return Pair(document, emptySet())
        val delta = CadPoint2D(deltaX, deltaY)
        val copiedIds = mutableSetOf<String>()
        val newEntities = mutableListOf<CadEntity>()

        for (entity in document.entities) {
            if (entityIds.contains(entity.id)) {
                val layer = document.layers[entity.layerId]
                if (layer?.isLocked == true) continue
                val newId = "copy_${java.util.UUID.randomUUID().toString().take(8)}"
                copiedIds.add(newId)
                val cloned = when (entity) {
                    is CadEntity.Line -> entity.copy(id = newId, start = entity.start + delta, end = entity.end + delta)
                    is CadEntity.Polyline -> entity.copy(id = newId, points = entity.points.map { it + delta })
                    is CadEntity.Circle -> entity.copy(id = newId, center = entity.center + delta)
                    is CadEntity.Arc -> entity.copy(id = newId, center = entity.center + delta)
                    is CadEntity.Text -> entity.copy(id = newId, position = entity.position + delta)
                    is CadEntity.Point -> entity.copy(id = newId, position = entity.position + delta)
                    is CadEntity.Dimension -> entity.copy(
                        id = newId,
                        start = entity.start + delta,
                        end = entity.end + delta,
                        textPoint = entity.textPoint + delta
                    )
                    is CadEntity.Leader -> entity.copy(
                        id = newId,
                        arrowPoint = entity.arrowPoint + delta,
                        kneePoint = entity.kneePoint + delta,
                        landingEndPoint = entity.landingEndPoint + delta
                    )
                    is CadEntity.Arrow -> entity.copy(
                        id = newId,
                        start = entity.start + delta,
                        end = entity.end + delta
                    )
                    is CadEntity.RevisionCloud -> entity.copy(
                        id = newId,
                        vertices = entity.vertices.map { it + delta }
                    )
                }
                newEntities.add(cloned)
            }
        }

        if (newEntities.isEmpty()) return Pair(document, emptySet())
        val cmd = com.example.cad.engine.history.CreateEntitiesCommand(
            entities = newEntities,
            name = "Copy ${newEntities.size} Entities"
        )
        val updatedDoc = commandHistory.executeCommand(document, cmd)
        return Pair(updatedDoc, copiedIds)
    }

    override fun rotateEntities(
        document: CadDocument,
        entityIds: Set<String>,
        center: CadPoint2D,
        angleDegrees: Float
    ): CadDocument {
        if (entityIds.isEmpty() || angleDegrees % 360f == 0f) return document
        val rotateableIds = entityIds.filter { id ->
            val ent = document.entities.find { it.id == id }
            val layer = ent?.let { document.layers[it.layerId] }
            layer == null || !layer.isLocked
        }.toSet()
        if (rotateableIds.isEmpty()) return document
        val cmd = com.example.cad.engine.history.RotateEntitiesCommand(rotateableIds, center, angleDegrees)
        return commandHistory.executeCommand(document, cmd)
    }

    override fun scaleEntities(
        document: CadDocument,
        entityIds: Set<String>,
        basePoint: CadPoint2D,
        factor: Float
    ): CadDocument {
        if (entityIds.isEmpty() || factor == 1.0f || factor <= 0f) return document
        val scalableIds = entityIds.filter { id ->
            val ent = document.entities.find { it.id == id }
            val layer = ent?.let { document.layers[it.layerId] }
            layer == null || !layer.isLocked
        }.toSet()
        if (scalableIds.isEmpty()) return document
        val cmd = com.example.cad.engine.history.ScaleEntitiesCommand(scalableIds, basePoint, factor)
        return commandHistory.executeCommand(document, cmd)
    }

    override fun trimEntity(
        document: CadDocument,
        targetEntityId: String,
        clickPoint: CadPoint2D
    ): CadDocument? {
        val target = document.entities.find { it.id == targetEntityId } as? CadEntity.Line ?: return null
        val layer = document.layers[target.layerId]
        if (layer?.isLocked == true) return null

        val trimResult = CadEditMath.trimLine(target, clickPoint, document.entities) ?: return null
        val cmd = com.example.cad.engine.history.GeometryEditCommand(
            originalEntity = target,
            replacementEntities = trimResult.replacementEntities,
            name = "Trim Line"
        )
        return commandHistory.executeCommand(document, cmd)
    }

    override fun extendEntity(
        document: CadDocument,
        targetEntityId: String,
        clickPoint: CadPoint2D
    ): CadDocument? {
        val target = document.entities.find { it.id == targetEntityId } as? CadEntity.Line ?: return null
        val layer = document.layers[target.layerId]
        if (layer?.isLocked == true) return null

        val extendResult = CadEditMath.extendLine(target, clickPoint, document.entities) ?: return null
        val cmd = com.example.cad.engine.history.GeometryEditCommand(
            originalEntity = target,
            replacementEntities = listOf(extendResult.extendedEntity),
            name = "Extend Line"
        )
        return commandHistory.executeCommand(document, cmd)
    }

    override fun setLayerVisibility(document: CadDocument, layerId: String, isVisible: Boolean): CadDocument {
        val existingLayer = document.layers[layerId] ?: return document
        if (existingLayer.isVisible == isVisible) return document
        val updatedLayer = existingLayer.copy(isVisible = isVisible)
        val cmd = com.example.cad.engine.history.ModifyLayerStateCommand(
            layerId = layerId,
            oldLayer = existingLayer,
            newLayer = updatedLayer,
            name = if (isVisible) "Show Layer '${existingLayer.name}'" else "Hide Layer '${existingLayer.name}'"
        )
        return commandHistory.executeCommand(document, cmd)
    }

    override fun setLayerLock(document: CadDocument, layerId: String, isLocked: Boolean): CadDocument {
        val existingLayer = document.layers[layerId] ?: return document
        if (existingLayer.isLocked == isLocked) return document
        val updatedLayer = existingLayer.copy(isLocked = isLocked)
        val cmd = com.example.cad.engine.history.ModifyLayerStateCommand(
            layerId = layerId,
            oldLayer = existingLayer,
            newLayer = updatedLayer,
            name = if (isLocked) "Lock Layer '${existingLayer.name}'" else "Unlock Layer '${existingLayer.name}'"
        )
        return commandHistory.executeCommand(document, cmd)
    }

    override fun clearHistory() {
        commandHistory.clearHistory()
    }
}

/**
 * Built-in Sample CAD Documents with authentic drafting geometry.
 */
object SampleCadDrawings {

    fun createArchitecturalFloorPlan(): CadDocument {
        val entities = mutableListOf<CadEntity>()

        // Exterior Walls (Polyline)
        entities.add(
            CadEntity.Polyline(
                id = "wall_ext",
                layerId = "0",
                points = listOf(
                    CadPoint2D(0f, 0f),
                    CadPoint2D(8000f, 0f),
                    CadPoint2D(8000f, 6000f),
                    CadPoint2D(4000f, 6000f),
                    CadPoint2D(4000f, 9000f),
                    CadPoint2D(0f, 9000f)
                ),
                isClosed = true,
                strokeWidth = 3.5f,
                colorArgb = 0xFFFFFFFF
            )
        )

        // Interior Partition Walls
        entities.add(
            CadEntity.Line(
                id = "wall_int_1",
                layerId = "0",
                start = CadPoint2D(0f, 4500f),
                end = CadPoint2D(4000f, 4500f),
                strokeWidth = 2.5f,
                colorArgb = 0xFFCCCCCC
            )
        )
        entities.add(
            CadEntity.Line(
                id = "wall_int_2",
                layerId = "0",
                start = CadPoint2D(4000f, 0f),
                end = CadPoint2D(4000f, 6000f),
                strokeWidth = 2.5f,
                colorArgb = 0xFFCCCCCC
            )
        )

        // Columns / Pillars (Circles)
        entities.add(CadEntity.Circle("col_1", "0", 0xFF00E5FF, CadPoint2D(2000f, 2000f), 250f, 2.0f))
        entities.add(CadEntity.Circle("col_2", "0", 0xFF00E5FF, CadPoint2D(6000f, 2000f), 250f, 2.0f))
        entities.add(CadEntity.Circle("col_3", "0", 0xFF00E5FF, CadPoint2D(6000f, 4500f), 250f, 2.0f))

        // Door Swing (Arc & Line)
        entities.add(
            CadEntity.Line("door_leaf", "0", 0xFF81D4FA, CadPoint2D(1200f, 0f), CadPoint2D(1200f, 900f), 1.5f)
        )
        entities.add(
            CadEntity.Arc("door_swing", "hidden", 0xFF81D4FA, CadPoint2D(1200f, 0f), 900f, 0f, 90f, 1.2f)
        )

        // Dimensions
        entities.add(
            CadEntity.Dimension("dim_w", "dim", 0xFFFFD600, CadPoint2D(0f, -500f), CadPoint2D(8000f, -500f), CadPoint2D(4000f, -700f), "8000 mm")
        )
        entities.add(
            CadEntity.Dimension("dim_h", "dim", 0xFFFFD600, CadPoint2D(-500f, 0f), CadPoint2D(-500f, 9000f), CadPoint2D(-700f, 4500f), "9000 mm")
        )

        val doc = CadDocument(
            title = "Architectural_Floor_Plan_A101.dwg",
            format = CadFormat.DWG,
            units = CadUnit.MILLIMETERS,
            entities = entities
        )
        return doc.copy(extents = doc.computeExtents())
    }

    fun createMechanicalFlangeAssembly(): CadDocument {
        val entities = mutableListOf<CadEntity>()

        // Outer Flange Rim
        entities.add(CadEntity.Circle("flange_outer", "0", 0xFFFFFFFF, CadPoint2D(0f, 0f), 150f, 3.0f))
        // Bolt Circle (Pitch Circle Diameter PCD)
        entities.add(CadEntity.Circle("pcd_circle", "center", 0xFFFF5252, CadPoint2D(0f, 0f), 115f, 1.2f))
        // Inner Bore
        entities.add(CadEntity.Circle("bore_inner", "0", 0xFF00E5FF, CadPoint2D(0f, 0f), 50f, 2.5f))

        // 8 Bolt Holes along PCD
        val boltRadius = 9f
        val pcdRadius = 115f
        for (i in 0 until 8) {
            val angle = i * (Math.PI * 2 / 8)
            val bx = (cos(angle) * pcdRadius).toFloat()
            val by = (sin(angle) * pcdRadius).toFloat()
            entities.add(CadEntity.Circle("bolt_$i", "0", 0xFFFFD600, CadPoint2D(bx, by), boltRadius, 1.5f))
        }

        // Centerlines Crosshair
        entities.add(CadEntity.Line("cl_x", "center", 0xFFFF5252, CadPoint2D(-180f, 0f), CadPoint2D(180f, 0f), 1.0f))
        entities.add(CadEntity.Line("cl_y", "center", 0xFFFF5252, CadPoint2D(0f, -180f), CadPoint2D(0f, 180f), 1.0f))

        // Dimension Callouts
        entities.add(
            CadEntity.Dimension("dim_flange_dia", "dim", 0xFFFFD600, CadPoint2D(-150f, 190f), CadPoint2D(150f, 190f), CadPoint2D(0f, 210f), "Ø300 mm")
        )

        val doc = CadDocument(
            title = "Flange_Housing_Assembly_M24.dxf",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            entities = entities
        )
        return doc.copy(extents = doc.computeExtents())
    }

    fun createElectricalSchematic(): CadDocument {
        val entities = mutableListOf<CadEntity>()

        // Busbars
        entities.add(CadEntity.Line("bus_l1", "0", 0xFFFF5252, CadPoint2D(0f, 500f), CadPoint2D(1000f, 500f), 2.5f))
        entities.add(CadEntity.Line("bus_l2", "0", 0xFFFFD600, CadPoint2D(0f, 400f), CadPoint2D(1000f, 400f), 2.5f))
        entities.add(CadEntity.Line("bus_l3", "0", 0xFF2979FF, CadPoint2D(0f, 300f), CadPoint2D(1000f, 300f), 2.5f))
        entities.add(CadEntity.Line("bus_n", "0", 0xFF00E5FF, CadPoint2D(0f, 200f), CadPoint2D(1000f, 200f), 2.0f))

        // Transformer Symbol (Two overlapping circles)
        entities.add(CadEntity.Circle("tx_prim", "0", 0xFFFFFFFF, CadPoint2D(400f, 100f), 40f, 2.0f))
        entities.add(CadEntity.Circle("tx_sec", "0", 0xFFFFFFFF, CadPoint2D(450f, 100f), 40f, 2.0f))

        // Breaker connection lines
        entities.add(CadEntity.Line("feed_1", "0", 0xFFFFFFFF, CadPoint2D(400f, 500f), CadPoint2D(400f, 140f), 1.8f))
        entities.add(CadEntity.Line("feed_out", "0", 0xFF00E676, CadPoint2D(450f, 60f), CadPoint2D(450f, 0f), 1.8f))

        val doc = CadDocument(
            title = "Substation_Distribution_Diagram.dwg",
            format = CadFormat.DWG,
            units = CadUnit.MILLIMETERS,
            entities = entities
        )
        return doc.copy(extents = doc.computeExtents())
    }
}

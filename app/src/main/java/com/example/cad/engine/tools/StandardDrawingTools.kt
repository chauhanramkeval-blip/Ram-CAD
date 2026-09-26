package com.example.cad.engine.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Standard CAD Preview Styling
val PreviewCyan = Color(0xFF00E5FF)
val PreviewDashedStroke = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
val PreviewYellow = Color(0xFFFFD600)

/**
 * 1. Line Drawing Tool:
 * Supports tap-tap or touch drag-and-release drawing of 2D line segments.
 */
class LineTool : DrawingTool {
    override val name: String = "Line"
    override val prompt: String
        get() = if (startPoint == null) "Line: Tap or drag start point" else "Line: Tap or drag endpoint"

    private var startPoint: CadPoint2D? = null
    private var currentEndPoint: CadPoint2D? = null
    private var isDragging: Boolean = false

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(startPoint, currentEndPoint)

    override val isInProgress: Boolean
        get() = startPoint != null

    override fun onPointerDown(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint)
        if (startPoint == null) {
            startPoint = resolved
            currentEndPoint = resolved
            isDragging = true
            return ToolResult.InProgress("Start point set: (%.1f, %.1f)".format(resolved.x, resolved.y), activePoints)
        }
        return ToolResult.None
    }

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (startPoint != null) {
            val resolved = context.resolveCoordinates(worldPoint, startPoint)
            currentEndPoint = resolved
            val dist = startPoint!!.distanceTo(resolved)
            return ToolResult.InProgress("Length: %.1f mm".format(dist), activePoints)
        }
        return ToolResult.None
    }

    override fun onPointerUp(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (isDragging && startPoint != null) {
            isDragging = false
            val resolved = context.resolveCoordinates(worldPoint, startPoint)
            if (resolved.distanceTo(startPoint!!) > 2f) {
                val entity = CadEntity.Line(
                    id = "line_${UUID.randomUUID().toString().take(6)}",
                    layerId = context.currentLayerId,
                    start = startPoint!!,
                    end = resolved
                )
                startPoint = null
                currentEndPoint = null
                return ToolResult.EntityCreated(entity, "Line created")
            }
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (startPoint == null) {
            val resolved = context.resolveCoordinates(worldPoint)
            startPoint = resolved
            currentEndPoint = resolved
            return ToolResult.InProgress("Start point set. Tap endpoint.", activePoints)
        } else {
            val resolved = context.resolveCoordinates(worldPoint, startPoint)
            val p1 = startPoint!!
            startPoint = null
            currentEndPoint = null
            isDragging = false
            val entity = CadEntity.Line(
                id = "line_${UUID.randomUUID().toString().take(6)}",
                layerId = context.currentLayerId,
                start = p1,
                end = resolved
            )
            return ToolResult.EntityCreated(entity, "Line created")
        }
    }

    override fun cancelOperation(): ToolResult {
        startPoint = null
        currentEndPoint = null
        isDragging = false
        return ToolResult.Cancelled("Line operation cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val p1 = startPoint ?: return
        val p2 = currentEndPoint ?: cursorWorldPos
        val s1 = transform.worldToScreen(p1)
        val s2 = transform.worldToScreen(p2)

        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(s1.x, s1.y),
            end = Offset(s2.x, s2.y),
            strokeWidth = 2.5f,
            pathEffect = PreviewDashedStroke
        )
        // Draw start anchor node
        drawScope.drawCircle(
            color = Color.White,
            radius = 5f,
            center = Offset(s1.x, s1.y)
        )
        // Draw end anchor node
        drawScope.drawCircle(
            color = PreviewCyan,
            radius = 5f,
            center = Offset(s2.x, s2.y)
        )
    }
}

/**
 * 2. Polyline Drawing Tool:
 * Supports multi-vertex interactive drawing, closing loops, and canceling.
 */
class PolylineTool : DrawingTool {
    override val name: String = "Polyline"
    override val prompt: String
        get() = if (points.isEmpty()) "Polyline: Tap to add first vertex" else "Polyline: ${points.size} vertices. Tap to add more, or press Finish"

    private val points = mutableListOf<CadPoint2D>()
    private var currentCursor: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = points.toList()

    override val isInProgress: Boolean
        get() = points.isNotEmpty()

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (points.isNotEmpty()) {
            val last = points.last()
            currentCursor = context.resolveCoordinates(worldPoint, last)
            return ToolResult.InProgress("Polyline (${points.size} pts)", points)
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val anchor = points.lastOrNull()
        val resolved = context.resolveCoordinates(worldPoint, anchor)
        points.add(resolved)
        currentCursor = resolved
        return ToolResult.InProgress("Added vertex (${points.size}). Tap Finish or Close.", points)
    }

    override fun confirmOperation(context: ToolContext): ToolResult {
        if (points.size >= 2) {
            val entity = CadEntity.Polyline(
                id = "poly_${UUID.randomUUID().toString().take(6)}",
                layerId = context.currentLayerId,
                points = points.toList(),
                isClosed = false
            )
            points.clear()
            currentCursor = null
            return ToolResult.EntityCreated(entity, "Polyline created (${entity.points.size} pts)")
        }
        points.clear()
        currentCursor = null
        return ToolResult.Cancelled("Polyline requires at least 2 vertices")
    }

    fun confirmClosedPolygon(context: ToolContext): ToolResult {
        if (points.size >= 3) {
            val entity = CadEntity.Polyline(
                id = "polygon_${UUID.randomUUID().toString().take(6)}",
                layerId = context.currentLayerId,
                points = points.toList(),
                isClosed = true
            )
            points.clear()
            currentCursor = null
            return ToolResult.EntityCreated(entity, "Closed polygon created (${entity.points.size} vertices)")
        }
        return confirmOperation(context)
    }

    override fun cancelOperation(): ToolResult {
        points.clear()
        currentCursor = null
        return ToolResult.Cancelled("Polyline cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        if (points.isEmpty()) return
        val screenPts = points.map { transform.worldToScreen(it) }

        // Render fixed committed segments
        for (i in 0 until screenPts.size - 1) {
            drawScope.drawLine(
                color = PreviewCyan,
                start = Offset(screenPts[i].x, screenPts[i].y),
                end = Offset(screenPts[i + 1].x, screenPts[i + 1].y),
                strokeWidth = 3f
            )
        }
        // Render vertex nodes
        for (pt in screenPts) {
            drawScope.drawCircle(
                color = Color.White,
                radius = 5f,
                center = Offset(pt.x, pt.y)
            )
        }

        // Render live elastic cursor segment
        val lastScreen = screenPts.last()
        val curr = currentCursor ?: cursorWorldPos
        val currScreen = transform.worldToScreen(curr)
        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(lastScreen.x, lastScreen.y),
            end = Offset(currScreen.x, currScreen.y),
            strokeWidth = 2.5f,
            pathEffect = PreviewDashedStroke
        )
        drawScope.drawCircle(
            color = PreviewCyan,
            radius = 4f,
            center = Offset(currScreen.x, currScreen.y)
        )
    }
}

/**
 * 3. Circle Drawing Tool:
 * Center point + radius boundary tap or drag.
 */
class CircleTool : DrawingTool {
    override val name: String = "Circle"
    override val prompt: String
        get() = if (centerPoint == null) "Circle: Tap or drag center point" else "Circle: Tap or drag radius boundary"

    private var centerPoint: CadPoint2D? = null
    private var currentRadiusPt: CadPoint2D? = null
    private var isDragging: Boolean = false

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(centerPoint, currentRadiusPt)

    override val isInProgress: Boolean
        get() = centerPoint != null

    override fun onPointerDown(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint)
        if (centerPoint == null) {
            centerPoint = resolved
            currentRadiusPt = resolved
            isDragging = true
            return ToolResult.InProgress("Center set: (%.1f, %.1f)".format(resolved.x, resolved.y), activePoints)
        }
        return ToolResult.None
    }

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (centerPoint != null) {
            val resolved = context.resolveCoordinates(worldPoint, centerPoint)
            currentRadiusPt = resolved
            val r = centerPoint!!.distanceTo(resolved)
            return ToolResult.InProgress("Radius: %.1f mm".format(r), activePoints)
        }
        return ToolResult.None
    }

    override fun onPointerUp(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (isDragging && centerPoint != null) {
            isDragging = false
            val resolved = context.resolveCoordinates(worldPoint, centerPoint)
            val radius = centerPoint!!.distanceTo(resolved)
            if (radius > 2f) {
                val entity = CadEntity.Circle(
                    id = "circle_${UUID.randomUUID().toString().take(6)}",
                    layerId = context.currentLayerId,
                    center = centerPoint!!,
                    radius = radius
                )
                centerPoint = null
                currentRadiusPt = null
                return ToolResult.EntityCreated(entity, "Circle created (R=%.1f mm)".format(radius))
            }
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (centerPoint == null) {
            val resolved = context.resolveCoordinates(worldPoint)
            centerPoint = resolved
            currentRadiusPt = resolved
            return ToolResult.InProgress("Center point set. Tap radius boundary.", activePoints)
        } else {
            val resolved = context.resolveCoordinates(worldPoint, centerPoint)
            val center = centerPoint!!
            val radius = center.distanceTo(resolved).coerceAtLeast(1f)
            centerPoint = null
            currentRadiusPt = null
            isDragging = false
            val entity = CadEntity.Circle(
                id = "circle_${UUID.randomUUID().toString().take(6)}",
                layerId = context.currentLayerId,
                center = center,
                radius = radius
            )
            return ToolResult.EntityCreated(entity, "Circle created (R=%.1f mm)".format(radius))
        }
    }

    override fun cancelOperation(): ToolResult {
        centerPoint = null
        currentRadiusPt = null
        isDragging = false
        return ToolResult.Cancelled("Circle cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val center = centerPoint ?: return
        val edge = currentRadiusPt ?: cursorWorldPos
        val radius = center.distanceTo(edge)
        val centerScreen = transform.worldToScreen(center)
        val radiusScreen = radius * transform.scale

        // Draw radial guide line
        val edgeScreen = transform.worldToScreen(edge)
        drawScope.drawLine(
            color = PreviewCyan.copy(alpha = 0.6f),
            start = Offset(centerScreen.x, centerScreen.y),
            end = Offset(edgeScreen.x, edgeScreen.y),
            strokeWidth = 1.5f,
            pathEffect = PreviewDashedStroke
        )

        // Draw circle outline
        if (radiusScreen > 0.5f) {
            drawScope.drawCircle(
                color = PreviewCyan,
                radius = radiusScreen,
                center = Offset(centerScreen.x, centerScreen.y),
                style = Stroke(width = 2.5f, pathEffect = PreviewDashedStroke)
            )
        }
        // Center node
        drawScope.drawCircle(
            color = Color.White,
            radius = 5f,
            center = Offset(centerScreen.x, centerScreen.y)
        )
    }
}

/**
 * 4. Arc Drawing Tool:
 * Standard 3-point CAD arc (Start, Midpoint on arc, Endpoint).
 */
class ArcTool : DrawingTool {
    override val name: String = "Arc"
    override val prompt: String
        get() = when {
            arcPoint1 == null -> "Arc: Tap start point"
            arcPoint2 == null -> "Arc: Tap second point along arc"
            else -> "Arc: Tap arc endpoint"
        }

    private var arcPoint1: CadPoint2D? = null
    private var arcPoint2: CadPoint2D? = null
    private var currentCursor: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(arcPoint1, arcPoint2, currentCursor)

    override val isInProgress: Boolean
        get() = arcPoint1 != null

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (arcPoint1 != null) {
            currentCursor = context.resolveCoordinates(worldPoint, arcPoint2 ?: arcPoint1)
            return ToolResult.InProgress("Arc in progress", activePoints)
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (arcPoint1 == null) {
            arcPoint1 = context.resolveCoordinates(worldPoint)
            return ToolResult.InProgress("Start point set. Tap second point along arc.", activePoints)
        } else if (arcPoint2 == null) {
            arcPoint2 = context.resolveCoordinates(worldPoint, arcPoint1)
            return ToolResult.InProgress("Second point set. Tap endpoint.", activePoints)
        } else {
            val p1 = arcPoint1!!
            val p2 = arcPoint2!!
            val p3 = context.resolveCoordinates(worldPoint, arcPoint2)

            val center = CadPoint2D((p1.x + p3.x) / 2f, (p1.y + p3.y) / 2f)
            val radius = center.distanceTo(p1).coerceAtLeast(1f)
            val startAngle = Math.toDegrees(atan2((p1.y - center.y).toDouble(), (p1.x - center.x).toDouble())).toFloat()
            val endAngle = Math.toDegrees(atan2((p3.y - center.y).toDouble(), (p3.x - center.x).toDouble())).toFloat()
            var sweep = endAngle - startAngle
            if (sweep < 0f) sweep += 360f

            arcPoint1 = null
            arcPoint2 = null
            currentCursor = null

            val entity = CadEntity.Arc(
                id = "arc_${UUID.randomUUID().toString().take(6)}",
                layerId = context.currentLayerId,
                center = center,
                radius = radius,
                startAngleDeg = startAngle,
                sweepAngleDeg = sweep.coerceIn(10f, 350f)
            )
            return ToolResult.EntityCreated(entity, "3-Point Arc created")
        }
    }

    override fun cancelOperation(): ToolResult {
        arcPoint1 = null
        arcPoint2 = null
        currentCursor = null
        return ToolResult.Cancelled("Arc cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val p1 = arcPoint1 ?: return
        val s1 = transform.worldToScreen(p1)
        drawScope.drawCircle(color = Color.White, radius = 5f, center = Offset(s1.x, s1.y))

        if (arcPoint2 == null) {
            // Elastic guide from p1 to cursor
            val sc = transform.worldToScreen(currentCursor ?: cursorWorldPos)
            drawScope.drawLine(
                color = PreviewCyan,
                start = Offset(s1.x, s1.y),
                end = Offset(sc.x, sc.y),
                strokeWidth = 2f,
                pathEffect = PreviewDashedStroke
            )
        } else {
            val p2 = arcPoint2!!
            val p3 = currentCursor ?: cursorWorldPos
            val s2 = transform.worldToScreen(p2)
            val s3 = transform.worldToScreen(p3)

            drawScope.drawCircle(color = Color.White, radius = 5f, center = Offset(s2.x, s2.y))
            drawScope.drawCircle(color = PreviewCyan, radius = 5f, center = Offset(s3.x, s3.y))

            val center = CadPoint2D((p1.x + p3.x) / 2f, (p1.y + p3.y) / 2f)
            val radius = center.distanceTo(p1).coerceAtLeast(1f)
            val startAngle = Math.toDegrees(atan2((p1.y - center.y).toDouble(), (p1.x - center.x).toDouble())).toFloat()
            val endAngle = Math.toDegrees(atan2((p3.y - center.y).toDouble(), (p3.x - center.x).toDouble())).toFloat()
            var sweep = endAngle - startAngle
            if (sweep < 0f) sweep += 360f

            val centerScreen = transform.worldToScreen(center)
            val radiusScreen = radius * transform.scale
            if (radiusScreen > 0.5f) {
                drawScope.drawArc(
                    color = PreviewCyan,
                    startAngle = -startAngle,
                    sweepAngle = -sweep.coerceIn(10f, 350f),
                    useCenter = false,
                    topLeft = Offset(centerScreen.x - radiusScreen, centerScreen.y - radiusScreen),
                    size = Size(radiusScreen * 2f, radiusScreen * 2f),
                    style = Stroke(width = 2.5f, pathEffect = PreviewDashedStroke)
                )
            }
        }
    }
}

/**
 * 5. Rectangle Drawing Tool:
 * Diagonal corners tap or drag, generating a closed 4-corner Polyline.
 */
class RectangleTool : DrawingTool {
    override val name: String = "Rectangle"
    override val prompt: String
        get() = if (corner1 == null) "Rectangle: Tap or drag first corner" else "Rectangle: Tap or drag opposite corner"

    private var corner1: CadPoint2D? = null
    private var currentOpposite: CadPoint2D? = null
    private var isDragging: Boolean = false

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(corner1, currentOpposite)

    override val isInProgress: Boolean
        get() = corner1 != null

    override fun onPointerDown(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint)
        if (corner1 == null) {
            corner1 = resolved
            currentOpposite = resolved
            isDragging = true
            return ToolResult.InProgress("First corner set: (%.1f, %.1f)".format(resolved.x, resolved.y), activePoints)
        }
        return ToolResult.None
    }

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (corner1 != null) {
            val resolved = context.resolveCoordinates(worldPoint)
            currentOpposite = resolved
            val w = abs(resolved.x - corner1!!.x)
            val h = abs(resolved.y - corner1!!.y)
            return ToolResult.InProgress("W: %.1f mm  H: %.1f mm".format(w, h), activePoints)
        }
        return ToolResult.None
    }

    override fun onPointerUp(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (isDragging && corner1 != null) {
            isDragging = false
            val p1 = corner1!!
            val p2 = context.resolveCoordinates(worldPoint)
            val w = abs(p2.x - p1.x)
            val h = abs(p2.y - p1.y)
            if (w > 2f && h > 2f) {
                val corners = listOf(
                    p1,
                    CadPoint2D(p2.x, p1.y),
                    p2,
                    CadPoint2D(p1.x, p2.y)
                )
                val entity = CadEntity.Polyline(
                    id = "rect_${UUID.randomUUID().toString().take(6)}",
                    layerId = context.currentLayerId,
                    points = corners,
                    isClosed = true
                )
                corner1 = null
                currentOpposite = null
                return ToolResult.EntityCreated(entity, "Rectangle created (%.1f x %.1f mm)".format(w, h))
            }
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (corner1 == null) {
            val resolved = context.resolveCoordinates(worldPoint)
            corner1 = resolved
            currentOpposite = resolved
            return ToolResult.InProgress("First corner set. Tap opposite diagonal corner.", activePoints)
        } else {
            val p1 = corner1!!
            val p2 = context.resolveCoordinates(worldPoint)
            val corners = listOf(
                p1,
                CadPoint2D(p2.x, p1.y),
                p2,
                CadPoint2D(p1.x, p2.y)
            )
            val entity = CadEntity.Polyline(
                id = "rect_${UUID.randomUUID().toString().take(6)}",
                layerId = context.currentLayerId,
                points = corners,
                isClosed = true
            )
            corner1 = null
            currentOpposite = null
            isDragging = false
            val w = abs(p2.x - p1.x)
            val h = abs(p2.y - p1.y)
            return ToolResult.EntityCreated(entity, "Rectangle created (%.1f x %.1f mm)".format(w, h))
        }
    }

    override fun cancelOperation(): ToolResult {
        corner1 = null
        currentOpposite = null
        isDragging = false
        return ToolResult.Cancelled("Rectangle cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val p1 = corner1 ?: return
        val p2 = currentOpposite ?: cursorWorldPos

        val s1 = transform.worldToScreen(p1)
        val s2 = transform.worldToScreen(p2)

        val minX = minOf(s1.x, s2.x)
        val maxX = maxOf(s1.x, s2.x)
        val minY = minOf(s1.y, s2.y)
        val maxY = maxOf(s1.y, s2.y)

        drawScope.drawRect(
            color = PreviewCyan,
            topLeft = Offset(minX, minY),
            size = Size(maxX - minX, maxY - minY),
            style = Stroke(width = 2.5f, pathEffect = PreviewDashedStroke)
        )
        // Draw corner anchors
        drawScope.drawCircle(color = Color.White, radius = 5f, center = Offset(s1.x, s1.y))
        drawScope.drawCircle(color = PreviewCyan, radius = 5f, center = Offset(s2.x, s2.y))
    }
}

/**
 * 6. Point Drawing Tool:
 * Touch-based placement of standalone precision Point nodes with snap support.
 */
class PointTool : DrawingTool {
    override val name: String = "Point"
    override val prompt: String = "Point: Tap or drag to position coordinate marker"

    private var previewPoint: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(previewPoint)

    override val isInProgress: Boolean = false

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        previewPoint = context.resolveCoordinates(worldPoint)
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint)
        val entity = CadEntity.Point(
            id = "pt_${UUID.randomUUID().toString().take(6)}",
            layerId = context.currentLayerId,
            position = resolved
        )
        previewPoint = null
        return ToolResult.EntityCreated(entity, "Point marker placed at (%.1f, %.1f)".format(resolved.x, resolved.y))
    }

    override fun cancelOperation(): ToolResult {
        previewPoint = null
        return ToolResult.Cancelled("Point operation cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val pt = previewPoint ?: cursorWorldPos
        val s = transform.worldToScreen(pt)
        val r = 7f

        drawScope.drawCircle(
            color = PreviewCyan,
            radius = r,
            center = Offset(s.x, s.y)
        )
        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(s.x - r * 2f, s.y),
            end = Offset(s.x + r * 2f, s.y),
            strokeWidth = 2f
        )
        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(s.x, s.y - r * 2f),
            end = Offset(s.x, s.y + r * 2f),
            strokeWidth = 2f
        )
    }
}

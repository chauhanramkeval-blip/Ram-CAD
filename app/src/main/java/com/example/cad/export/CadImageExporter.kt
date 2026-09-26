package com.example.cad.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.cad.engine.measurement.CadMeasurementResult
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-resolution raster image exporter (PNG / JPEG) for CAD drawings.
 * Supports transparent backgrounds, dark CAD drafting canvas or white plot mode,
 * selectable layers, custom resolutions, and measurement overlays.
 */
class CadImageExporter(private val context: Context) {

    suspend fun exportImage(
        document: CadDocument,
        config: CadImageExportConfig,
        activeMeasurements: List<CadMeasurementResult> = emptyList(),
        destinationFile: File? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val sanitizedTitle = document.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val ext = config.format.extension
            val targetFile = destinationFile ?: File(exportDir, "${sanitizedTitle}_${System.currentTimeMillis()}.$ext")

            // 1. Calculate Target Dimensions
            val extents = document.computeExtents()
            val aspect = if (extents.height > 0.001f) extents.width / extents.height else 1.77f
            val baseTarget = config.resolution.baseTargetPx

            val bmpWidth: Int
            val bmpHeight: Int
            if (aspect >= 1.0f) {
                bmpWidth = baseTarget.coerceIn(800, 8192)
                bmpHeight = (baseTarget / aspect).toInt().coerceIn(600, 8192)
            } else {
                bmpHeight = baseTarget.coerceIn(800, 8192)
                bmpWidth = (baseTarget * aspect).toInt().coerceIn(600, 8192)
            }

            val bitmapConfig = Bitmap.Config.ARGB_8888
            val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, bitmapConfig)
            val canvas = Canvas(bitmap)

            // 2. Background Fill
            val isDarkBackground = config.backgroundMode == CadImageBackgroundMode.DARK_CANVAS
            when (config.backgroundMode) {
                CadImageBackgroundMode.WHITE_PLOT -> canvas.drawColor(Color.WHITE)
                CadImageBackgroundMode.DARK_CANVAS -> canvas.drawColor(Color.rgb(14, 17, 24)) // CAD Canvas Dark
                CadImageBackgroundMode.TRANSPARENT_PNG -> {
                    if (config.format == CadImageFormat.PNG) {
                        canvas.drawColor(Color.TRANSPARENT)
                    } else {
                        canvas.drawColor(Color.WHITE)
                    }
                }
            }

            // Margin & Printable Rect
            val margin = bmpWidth * 0.05f
            val drawArea = RectF(margin, margin, bmpWidth - margin, bmpHeight - margin)

            val safeW = extents.width.coerceAtLeast(10f)
            val safeH = extents.height.coerceAtLeast(10f)
            val scaleX = drawArea.width() / safeW
            val scaleY = drawArea.height() / safeH
            val scale = minOf(scaleX, scaleY) * 0.95f

            val worldCenterX = extents.centerX
            val worldCenterY = extents.centerY
            val canvasCenterX = drawArea.centerX()
            val canvasCenterY = drawArea.centerY()

            fun worldToScreenX(wx: Float): Float = canvasCenterX + (wx - worldCenterX) * scale
            fun worldToScreenY(wy: Float): Float = canvasCenterY - (wy - worldCenterY) * scale

            // 3. Optional Grid
            if (config.includeGrid) {
                val gridPaint = Paint().apply {
                    color = if (isDarkBackground) Color.argb(45, 255, 255, 255) else Color.rgb(230, 233, 238)
                    strokeWidth = 1f * config.resolution.multiplier
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val step = (50f * scale).coerceAtLeast(20f * config.resolution.multiplier)
                var x = canvasCenterX
                while (x <= bmpWidth) { canvas.drawLine(x, 0f, x, bmpHeight.toFloat(), gridPaint); x += step }
                x = canvasCenterX - step
                while (x >= 0f) { canvas.drawLine(x, 0f, x, bmpHeight.toFloat(), gridPaint); x -= step }

                var y = canvasCenterY
                while (y <= bmpHeight) { canvas.drawLine(0f, y, bmpWidth.toFloat(), y, gridPaint); y += step }
                y = canvasCenterY - step
                while (y >= 0f) { canvas.drawLine(0f, y, bmpWidth.toFloat(), y, gridPaint); y -= step }
            }

            // 4. Render Geometry
            val visibleLayerIds = if (config.selectedLayerIds.isNotEmpty()) {
                config.selectedLayerIds
            } else {
                document.layers.filter { it.value.isVisible }.keys
            }

            val strokeBaseScale = config.resolution.multiplier

            for (entity in document.entities) {
                if (!visibleLayerIds.contains(entity.layerId)) continue
                val layer = document.layers[entity.layerId]

                val entityColor: Int = if (config.colorMode == CadColorMode.MONOCHROME) {
                    if (isDarkBackground) Color.WHITE else Color.BLACK
                } else {
                    val raw = entity.colorArgb?.toInt() ?: layer?.colorArgb?.toInt() ?: if (isDarkBackground) Color.WHITE else Color.BLACK
                    if (!isDarkBackground && isNearlyWhite(raw)) {
                        Color.BLACK
                    } else if (isDarkBackground && isNearlyBlack(raw)) {
                        Color.WHITE
                    } else {
                        raw
                    }
                }

                val strokeWidth: Float = ((layer?.lineWeight ?: 0.35f) * strokeBaseScale * 1.5f).coerceIn(1.5f * strokeBaseScale, 10f * strokeBaseScale)

                val paint = Paint().apply {
                    color = entityColor
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                    isAntiAlias = true
                }

                when (entity) {
                    is CadEntity.Point -> {
                        val px = worldToScreenX(entity.position.x)
                        val py = worldToScreenY(entity.position.y)
                        val r = 4f * strokeBaseScale
                        val fillPaint = Paint().apply { color = entityColor; style = Paint.Style.FILL; isAntiAlias = true }
                        canvas.drawCircle(px, py, r, fillPaint)
                        canvas.drawLine(px - r * 1.5f, py, px + r * 1.5f, py, paint)
                        canvas.drawLine(px, py - r * 1.5f, px, py + r * 1.5f, paint)
                    }
                    is CadEntity.Line -> {
                        canvas.drawLine(
                            worldToScreenX(entity.start.x),
                            worldToScreenY(entity.start.y),
                            worldToScreenX(entity.end.x),
                            worldToScreenY(entity.end.y),
                            paint
                        )
                    }
                    is CadEntity.Polyline -> {
                        if (entity.points.size >= 2) {
                            val path = Path()
                            val first = entity.points.first()
                            path.moveTo(worldToScreenX(first.x), worldToScreenY(first.y))
                            for (i in 1 until entity.points.size) {
                                val pt = entity.points[i]
                                path.lineTo(worldToScreenX(pt.x), worldToScreenY(pt.y))
                            }
                            if (entity.isClosed) path.close()
                            canvas.drawPath(path, paint)
                        }
                    }
                    is CadEntity.Circle -> {
                        val cx = worldToScreenX(entity.center.x)
                        val cy = worldToScreenY(entity.center.y)
                        val r = entity.radius * scale
                        if (r > 0.5f) canvas.drawCircle(cx, cy, r, paint)
                    }
                    is CadEntity.Arc -> {
                        val cx = worldToScreenX(entity.center.x)
                        val cy = worldToScreenY(entity.center.y)
                        val r = entity.radius * scale
                        if (r > 0.5f) {
                            val oval = RectF(cx - r, cy - r, cx + r, cy + r)
                            canvas.drawArc(oval, -entity.startAngleDeg, -entity.sweepAngleDeg, false, paint)
                        }
                    }
                    is CadEntity.Dimension -> {
                        if (config.includeMeasurements) {
                            val x1 = worldToScreenX(entity.start.x)
                            val y1 = worldToScreenY(entity.start.y)
                            val x2 = worldToScreenX(entity.end.x)
                            val y2 = worldToScreenY(entity.end.y)
                            canvas.drawLine(x1, y1, x2, y2, paint)
                            val tickPaint = Paint().apply { color = entityColor; style = Paint.Style.FILL; isAntiAlias = true }
                            canvas.drawCircle(x1, y1, 4f * strokeBaseScale, tickPaint)
                            canvas.drawCircle(x2, y2, 4f * strokeBaseScale, tickPaint)

                            val midX = (x1 + x2) / 2f
                            val midY = (y1 + y2) / 2f
                            val textPaint = Paint().apply {
                                color = entityColor
                                textSize = 12f * strokeBaseScale
                                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                                isAntiAlias = true
                                textAlign = Paint.Align.CENTER
                            }
                            canvas.drawText(entity.valueText, midX, midY - 6f * strokeBaseScale, textPaint)
                        }
                    }
                    is CadEntity.Text -> {
                        val tx = worldToScreenX(entity.position.x)
                        val ty = worldToScreenY(entity.position.y)
                        val textPaint = Paint().apply {
                            color = entityColor
                            textSize = (entity.textHeight * scale).coerceIn(8f * strokeBaseScale, 180f * strokeBaseScale)
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                            isAntiAlias = true
                        }
                        if (entity.rotationDeg != 0f) {
                            canvas.save()
                            canvas.rotate(-entity.rotationDeg, tx, ty)
                            canvas.drawText(entity.text, tx, ty, textPaint)
                            canvas.restore()
                        } else {
                            canvas.drawText(entity.text, tx, ty, textPaint)
                        }
                    }
                    is CadEntity.Leader -> {
                        val ap = CadPoint2D(worldToScreenX(entity.arrowPoint.x), worldToScreenY(entity.arrowPoint.y))
                        val kp = CadPoint2D(worldToScreenX(entity.kneePoint.x), worldToScreenY(entity.kneePoint.y))
                        val ep = CadPoint2D(worldToScreenX(entity.landingEndPoint.x), worldToScreenY(entity.landingEndPoint.y))
                        canvas.drawLine(ap.x, ap.y, kp.x, kp.y, paint)
                        canvas.drawLine(kp.x, kp.y, ep.x, ep.y, paint)
                        val headPaint = Paint().apply { color = entityColor; style = Paint.Style.FILL; isAntiAlias = true }
                        canvas.drawCircle(ap.x, ap.y, 4f * strokeBaseScale, headPaint)
                        val textPaint = Paint().apply {
                            color = entityColor
                            textSize = (entity.textHeight * scale).coerceIn(8f * strokeBaseScale, 120f * strokeBaseScale)
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                            isAntiAlias = true
                        }
                        canvas.drawText(entity.text, ep.x + 4f, ep.y - 4f, textPaint)
                    }
                    is CadEntity.Arrow -> {
                        val x1 = worldToScreenX(entity.start.x)
                        val y1 = worldToScreenY(entity.start.y)
                        val x2 = worldToScreenX(entity.end.x)
                        val y2 = worldToScreenY(entity.end.y)
                        canvas.drawLine(x1, y1, x2, y2, paint)
                        val headPaint = Paint().apply { color = entityColor; style = Paint.Style.FILL; isAntiAlias = true }
                        canvas.drawCircle(x2, y2, 5f * strokeBaseScale, headPaint)
                    }
                    is CadEntity.RevisionCloud -> {
                        if (entity.vertices.size >= 2) {
                            val path = Path()
                            val first = entity.vertices.first()
                            path.moveTo(worldToScreenX(first.x), worldToScreenY(first.y))
                            for (i in 1 until entity.vertices.size) {
                                val pt = entity.vertices[i]
                                path.lineTo(worldToScreenX(pt.x), worldToScreenY(pt.y))
                            }
                            if (entity.isClosed) path.close()
                            canvas.drawPath(path, paint)
                        }
                    }
                }
            }

            // 5. Active Measurements Overlay
            if (config.includeMeasurements && activeMeasurements.isNotEmpty()) {
                val measurePaint = Paint().apply {
                    color = Color.rgb(255, 180, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * strokeBaseScale
                    pathEffect = DashPathEffect(floatArrayOf(8f * strokeBaseScale, 6f * strokeBaseScale), 0f)
                    isAntiAlias = true
                }
                val labelPaint = Paint().apply {
                    color = Color.rgb(255, 180, 0)
                    textSize = 14f * strokeBaseScale
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    isAntiAlias = true
                }
                for (m in activeMeasurements) {
                    when (m) {
                        is CadMeasurementResult.Distance -> {
                            val p1x = worldToScreenX(m.p1.x)
                            val p1y = worldToScreenY(m.p1.y)
                            val p2x = worldToScreenX(m.p2.x)
                            val p2y = worldToScreenY(m.p2.y)
                            canvas.drawLine(p1x, p1y, p2x, p2y, measurePaint)
                            canvas.drawText(m.primaryFormatted, (p1x + p2x) / 2f, (p1y + p2y) / 2f - 6f, labelPaint)
                        }
                        is CadMeasurementResult.Area -> {
                            if (m.points.size >= 3) {
                                val p = Path()
                                p.moveTo(worldToScreenX(m.points[0].x), worldToScreenY(m.points[0].y))
                                for (i in 1 until m.points.size) {
                                    p.lineTo(worldToScreenX(m.points[i].x), worldToScreenY(m.points[i].y))
                                }
                                p.close()
                                canvas.drawPath(p, measurePaint)
                            }
                        }
                        else -> {}
                    }
                }
            }

            // 6. Compress & Save
            FileOutputStream(targetFile).use { outStream ->
                if (config.format == CadImageFormat.PNG) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, config.jpegQuality.coerceIn(50, 100), outStream)
                }
            }
            bitmap.recycle()

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNearlyWhite(color: Int): Boolean {
        return Color.red(color) > 230 && Color.green(color) > 230 && Color.blue(color) > 230
    }

    private fun isNearlyBlack(color: Int): Boolean {
        return Color.red(color) < 25 && Color.green(color) < 25 && Color.blue(color) < 25
    }
}

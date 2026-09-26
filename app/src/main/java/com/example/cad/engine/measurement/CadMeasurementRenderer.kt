package com.example.cad.engine.measurement

import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * High-precision CAD Measurement Canvas Renderer.
 *
 * Renders technical dimension annotations on top of the drawing canvas:
 * - Linear & Aligned dimension lines with arrows, extension offsets, and centered text
 * - Horizontal & Vertical dimension lines
 * - Angular dimension arcs with arrows and degree badges
 * - Radial and Diametrical leaders with center crosshairs
 * - Coordinate callout target flags
 * - Surface area polygon translucent fills and centroid badges
 * - Polyline segment distance callouts
 * - Bounding box dashed envelopes with orthogonal dimensions
 */
object CadMeasurementRenderer {

    private val DimColor = Color(0xFFFBBF24) // Yellow CAD Dimension
    private val DimColorSemi = Color(0x77FBBF24)
    private val DimFillColor = Color(0x33FBBF24)
    private val DimCyan = Color(0xFF38BDF8)
    private val DimWhite = Color(0xFFFFFFFF)

    private val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 30f
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
    }

    private val subTextPaint = Paint().apply {
        color = android.graphics.Color.rgb(251, 191, 36)
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
    }

    private val badgeBgPaint = Paint().apply {
        color = android.graphics.Color.argb(230, 15, 23, 42) // Slate 900
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint().apply {
        color = android.graphics.Color.argb(220, 251, 191, 36)
        strokeWidth = 2.5f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    fun renderMeasurement(
        scope: DrawScope,
        result: CadMeasurementResult?,
        transform: CadViewportTransform,
        inProgressPoints: List<CadPoint2D> = emptyList(),
        previewPoint: CadPoint2D? = null
    ) {
        // 1. Render in-progress construction points/lines if any
        renderInProgressPoints(scope, inProgressPoints, previewPoint, transform)

        // 2. Render completed or live calculated annotation
        val annotation = result?.annotation ?: return
        when (annotation) {
            is CadMeasurementAnnotation.LinearDimension ->
                renderLinearDimension(scope, annotation, transform)
            is CadMeasurementAnnotation.AngularDimension ->
                renderAngularDimension(scope, annotation, transform)
            is CadMeasurementAnnotation.RadialDimension ->
                renderRadialDimension(scope, annotation, transform)
            is CadMeasurementAnnotation.CoordinateCallout ->
                renderCoordinateCallout(scope, annotation, transform)
            is CadMeasurementAnnotation.PolygonArea ->
                renderPolygonArea(scope, annotation, transform)
            is CadMeasurementAnnotation.PolylinePath ->
                renderPolylinePath(scope, annotation, transform)
            is CadMeasurementAnnotation.BoundingBoxEnvelope ->
                renderBoundingBoxEnvelope(scope, annotation, transform)
            is CadMeasurementAnnotation.MultiRegionArea ->
                renderMultiRegionArea(scope, annotation, transform)
            is CadMeasurementAnnotation.SelectionEnvelope ->
                renderSelectionEnvelope(scope, annotation, transform)
        }
    }

    private fun renderInProgressPoints(
        scope: DrawScope,
        points: List<CadPoint2D>,
        previewPoint: CadPoint2D?,
        transform: CadViewportTransform
    ) {
        val allPoints = if (previewPoint != null) points + previewPoint else points
        if (allPoints.isEmpty()) return

        // Draw connecting dashed construction line
        for (i in 0 until allPoints.size - 1) {
            val s1 = transform.worldToScreen(allPoints[i])
            val s2 = transform.worldToScreen(allPoints[i + 1])
            scope.drawLine(
                color = DimCyan.copy(alpha = 0.7f),
                start = Offset(s1.x, s1.y),
                end = Offset(s2.x, s2.y),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        // Draw picked point markers
        for ((idx, pt) in points.withIndex()) {
            val s = transform.worldToScreen(pt)
            scope.drawCircle(
                color = Color.Black,
                radius = 7f,
                center = Offset(s.x, s.y)
            )
            scope.drawCircle(
                color = DimColor,
                radius = 5f,
                center = Offset(s.x, s.y)
            )
            scope.drawCircle(
                color = Color.White,
                radius = 2f,
                center = Offset(s.x, s.y)
            )
        }
    }

    private fun renderLinearDimension(
        scope: DrawScope,
        dim: CadMeasurementAnnotation.LinearDimension,
        transform: CadViewportTransform
    ) {
        val sP1 = transform.worldToScreen(dim.p1)
        val sP2 = transform.worldToScreen(dim.p2)
        val sDimStart = transform.worldToScreen(dim.dimStart)
        val sDimEnd = transform.worldToScreen(dim.dimEnd)
        val sTextPos = transform.worldToScreen(dim.textPos)

        // 1. Extension lines
        if (dim.ext1Start != dim.dimStart) {
            val sExt1Start = transform.worldToScreen(dim.ext1Start)
            val sExt1End = transform.worldToScreen(dim.ext1End)
            scope.drawLine(
                color = DimColorSemi,
                start = Offset(sExt1Start.x, sExt1Start.y),
                end = Offset(sExt1End.x, sExt1End.y),
                strokeWidth = 1.8f
            )
        }
        if (dim.ext2Start != dim.dimEnd) {
            val sExt2Start = transform.worldToScreen(dim.ext2Start)
            val sExt2End = transform.worldToScreen(dim.ext2End)
            scope.drawLine(
                color = DimColorSemi,
                start = Offset(sExt2Start.x, sExt2Start.y),
                end = Offset(sExt2End.x, sExt2End.y),
                strokeWidth = 1.8f
            )
        }

        // 2. Dimension line
        scope.drawLine(
            color = DimColor,
            start = Offset(sDimStart.x, sDimStart.y),
            end = Offset(sDimEnd.x, sDimEnd.y),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // 3. Arrowheads / Ticks at start and end
        drawArrowhead(scope, Offset(sDimEnd.x, sDimEnd.y), Offset(sDimStart.x, sDimStart.y))
        drawArrowhead(scope, Offset(sDimStart.x, sDimStart.y), Offset(sDimEnd.x, sDimEnd.y))

        // 4. Centered text badge
        drawCenteredBadge(scope, sTextPos.x, sTextPos.y, dim.text)
    }

    private fun renderAngularDimension(
        scope: DrawScope,
        dim: CadMeasurementAnnotation.AngularDimension,
        transform: CadViewportTransform
    ) {
        val sVertex = transform.worldToScreen(dim.vertex)
        val sRay1 = transform.worldToScreen(dim.ray1End)
        val sRay2 = transform.worldToScreen(dim.ray2End)
        val sText = transform.worldToScreen(dim.textPos)

        // Draw ray guidelines
        scope.drawLine(
            color = DimColorSemi,
            start = Offset(sVertex.x, sVertex.y),
            end = Offset(sRay1.x, sRay1.y),
            strokeWidth = 1.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
        scope.drawLine(
            color = DimColorSemi,
            start = Offset(sVertex.x, sVertex.y),
            end = Offset(sRay2.x, sRay2.y),
            strokeWidth = 1.8f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )

        // Draw arc
        val arcRadiusPx = (dim.arcRadius * transform.scale).coerceIn(40f, 250f)
        val ovalLeft = sVertex.x - arcRadiusPx
        val ovalTop = sVertex.y - arcRadiusPx
        val ovalSize = arcRadiusPx * 2f

        val path = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(ovalLeft, ovalTop, ovalLeft + ovalSize, ovalTop + ovalSize),
                startAngleDegrees = dim.startAngleDeg,
                sweepAngleDegrees = dim.sweepAngleDeg,
                forceMoveTo = true
            )
        }
        scope.drawPath(
            path = path,
            color = DimColor,
            style = Stroke(width = 2.5f)
        )

        // Draw degree text badge
        drawCenteredBadge(scope, sText.x, sText.y, dim.text)
    }

    private fun renderRadialDimension(
        scope: DrawScope,
        dim: CadMeasurementAnnotation.RadialDimension,
        transform: CadViewportTransform
    ) {
        val sCenter = transform.worldToScreen(dim.center)
        val sRim = transform.worldToScreen(dim.rimPoint)
        val sLeader = transform.worldToScreen(dim.leaderEnd)

        // Center pip crosshair
        val pipLen = 14f
        scope.drawLine(
            color = DimColor,
            start = Offset(sCenter.x - pipLen, sCenter.y),
            end = Offset(sCenter.x + pipLen, sCenter.y),
            strokeWidth = 2f
        )
        scope.drawLine(
            color = DimColor,
            start = Offset(sCenter.x, sCenter.y - pipLen),
            end = Offset(sCenter.x, sCenter.y + pipLen),
            strokeWidth = 2f
        )

        // Radial line from center through rim to leader elbow
        scope.drawLine(
            color = DimColor,
            start = Offset(sCenter.x, sCenter.y),
            end = Offset(sRim.x, sRim.y),
            strokeWidth = 2.5f
        )
        scope.drawLine(
            color = DimColor,
            start = Offset(sRim.x, sRim.y),
            end = Offset(sLeader.x, sLeader.y),
            strokeWidth = 2.2f
        )

        // If diameter, draw opposite line
        dim.oppositeRimPoint?.let { opp ->
            val sOpp = transform.worldToScreen(opp)
            scope.drawLine(
                color = DimColor,
                start = Offset(sCenter.x, sCenter.y),
                end = Offset(sOpp.x, sOpp.y),
                strokeWidth = 2.5f
            )
            drawArrowhead(scope, Offset(sCenter.x, sCenter.y), Offset(sOpp.x, sOpp.y))
        }

        // Arrow at rim point
        drawArrowhead(scope, Offset(sCenter.x, sCenter.y), Offset(sRim.x, sRim.y))

        // Badge at leader end
        drawCenteredBadge(scope, sLeader.x, sLeader.y, dim.text)
    }

    private fun renderCoordinateCallout(
        scope: DrawScope,
        callout: CadMeasurementAnnotation.CoordinateCallout,
        transform: CadViewportTransform
    ) {
        val sPoint = transform.worldToScreen(callout.point)
        val sLeader = transform.worldToScreen(callout.leaderEnd)

        // Target bulls-eye at picked coordinate
        scope.drawCircle(color = DimColor, radius = 9f, center = Offset(sPoint.x, sPoint.y), style = Stroke(width = 2.5f))
        scope.drawCircle(color = DimColor, radius = 3f, center = Offset(sPoint.x, sPoint.y), style = Fill)
        scope.drawLine(
            color = DimColor,
            start = Offset(sPoint.x - 14f, sPoint.y),
            end = Offset(sPoint.x + 14f, sPoint.y),
            strokeWidth = 1.8f
        )
        scope.drawLine(
            color = DimColor,
            start = Offset(sPoint.x, sPoint.y - 14f),
            end = Offset(sPoint.x, sPoint.y + 14f),
            strokeWidth = 1.8f
        )

        // Leader line to callout flag
        scope.drawLine(
            color = DimColor,
            start = Offset(sPoint.x, sPoint.y),
            end = Offset(sLeader.x, sLeader.y),
            strokeWidth = 2f
        )

        // Multi-line badge
        val canvas = scope.drawContext.canvas.nativeCanvas
        val text1 = callout.line1
        val text2 = callout.line2
        val w1 = textPaint.measureText(text1)
        val w2 = textPaint.measureText(text2)
        val maxW = maxOf(w1, w2)
        val paddingH = 16f
        val paddingV = 10f
        val boxW = maxW + paddingH * 2
        val boxH = 68f

        val left = sLeader.x
        val top = sLeader.y - boxH / 2f
        val rect = RectF(left, top, left + boxW, top + boxH)

        canvas.drawRoundRect(rect, 10f, 10f, badgeBgPaint)
        canvas.drawRoundRect(rect, 10f, 10f, badgeBorderPaint)
        canvas.drawText(text1, left + paddingH, top + 26f, textPaint)
        canvas.drawText(text2, left + paddingH, top + 54f, subTextPaint)
    }

    private fun renderPolygonArea(
        scope: DrawScope,
        area: CadMeasurementAnnotation.PolygonArea,
        transform: CadViewportTransform
    ) {
        if (area.vertices.size < 2) return

        val path = Path()
        val sFirst = transform.worldToScreen(area.vertices.first())
        path.moveTo(sFirst.x, sFirst.y)

        for (i in 1 until area.vertices.size) {
            val s = transform.worldToScreen(area.vertices[i])
            path.lineTo(s.x, s.y)
        }
        if (area.isClosed && area.vertices.size >= 3) {
            path.close()
            // Fill translucent area
            scope.drawPath(path = path, color = DimFillColor, style = Fill)
        }

        // Outline contour
        scope.drawPath(
            path = path,
            color = DimColor,
            style = Stroke(
                width = 2.8f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 6f), 0f)
            )
        )

        // Draw vertex dots
        for (v in area.vertices) {
            val s = transform.worldToScreen(v)
            scope.drawCircle(color = DimColor, radius = 6f, center = Offset(s.x, s.y))
            scope.drawCircle(color = Color.White, radius = 2.5f, center = Offset(s.x, s.y))
        }

        // Draw centroid label
        if (area.areaText.isNotBlank() || area.perimeterText.isNotBlank()) {
            val sCentroid = transform.worldToScreen(area.centroid)
            val canvas = scope.drawContext.canvas.nativeCanvas

            val line1 = if (area.areaText.isNotBlank()) "AREA: ${area.areaText}" else ""
            val line2 = if (area.perimeterText.isNotBlank()) "PERIM: ${area.perimeterText}" else ""

            val w1 = if (line1.isNotBlank()) textPaint.measureText(line1) else 0f
            val w2 = if (line2.isNotBlank()) subTextPaint.measureText(line2) else 0f
            val maxW = maxOf(w1, w2)
            val paddingH = 18f
            val boxW = maxW + paddingH * 2
            val boxH = if (line1.isNotBlank() && line2.isNotBlank()) 72f else 46f

            val left = sCentroid.x - boxW / 2f
            val top = sCentroid.y - boxH / 2f
            val rect = RectF(left, top, left + boxW, top + boxH)

            canvas.drawRoundRect(rect, 10f, 10f, badgeBgPaint)
            canvas.drawRoundRect(rect, 10f, 10f, badgeBorderPaint)

            if (line1.isNotBlank() && line2.isNotBlank()) {
                canvas.drawText(line1, left + paddingH, top + 28f, textPaint)
                canvas.drawText(line2, left + paddingH, top + 58f, subTextPaint)
            } else {
                val singleLine = if (line1.isNotBlank()) line1 else line2
                canvas.drawText(singleLine, left + paddingH, top + 32f, textPaint)
            }
        }
    }

    private fun renderPolylinePath(
        scope: DrawScope,
        pathData: CadMeasurementAnnotation.PolylinePath,
        transform: CadViewportTransform
    ) {
        if (pathData.points.size < 2) return

        for (i in 0 until pathData.points.size - 1) {
            val s1 = transform.worldToScreen(pathData.points[i])
            val s2 = transform.worldToScreen(pathData.points[i + 1])
            scope.drawLine(
                color = DimColor,
                start = Offset(s1.x, s1.y),
                end = Offset(s2.x, s2.y),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        // Draw vertex dots
        for (pt in pathData.points) {
            val s = transform.worldToScreen(pt)
            scope.drawCircle(color = DimColor, radius = 5.5f, center = Offset(s.x, s.y))
        }

        // Total badge at last point
        val sLast = transform.worldToScreen(pathData.points.last())
        drawCenteredBadge(scope, sLast.x + 20f, sLast.y - 30f, pathData.totalLengthText)
    }

    private fun renderBoundingBoxEnvelope(
        scope: DrawScope,
        boxEnv: CadMeasurementAnnotation.BoundingBoxEnvelope,
        transform: CadViewportTransform
    ) {
        val sTopLeft = transform.worldToScreen(CadPoint2D(boxEnv.box.minX, boxEnv.box.maxY))
        val sBottomRight = transform.worldToScreen(CadPoint2D(boxEnv.box.maxX, boxEnv.box.minY))

        val left = minOf(sTopLeft.x, sBottomRight.x)
        val right = maxOf(sTopLeft.x, sBottomRight.x)
        val top = minOf(sTopLeft.y, sBottomRight.y)
        val bottom = maxOf(sTopLeft.y, sBottomRight.y)
        val width = right - left
        val height = bottom - top

        // Dashed envelope rectangle
        scope.drawRect(
            color = DimColor,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(width, height),
            style = Stroke(
                width = 2.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        )

        // Diagonal dashed line
        scope.drawLine(
            color = DimColorSemi,
            start = Offset(left, top),
            end = Offset(right, bottom),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        // Corner L-brackets
        val bracketLen = 14f
        // Top-Left
        scope.drawLine(color = DimColor, start = Offset(left, top), end = Offset(left + bracketLen, top), strokeWidth = 3f)
        scope.drawLine(color = DimColor, start = Offset(left, top), end = Offset(left, top + bracketLen), strokeWidth = 3f)
        // Top-Right
        scope.drawLine(color = DimColor, start = Offset(right, top), end = Offset(right - bracketLen, top), strokeWidth = 3f)
        scope.drawLine(color = DimColor, start = Offset(right, top), end = Offset(right, top + bracketLen), strokeWidth = 3f)
        // Bottom-Left
        scope.drawLine(color = DimColor, start = Offset(left, bottom), end = Offset(left + bracketLen, bottom), strokeWidth = 3f)
        scope.drawLine(color = DimColor, start = Offset(left, bottom), end = Offset(left, bottom - bracketLen), strokeWidth = 3f)
        // Bottom-Right
        scope.drawLine(color = DimColor, start = Offset(right, bottom), end = Offset(right - bracketLen, bottom), strokeWidth = 3f)
        scope.drawLine(color = DimColor, start = Offset(right, bottom), end = Offset(right, bottom - bracketLen), strokeWidth = 3f)

        // Dimension text badges
        drawCenteredBadge(scope, (left + right) / 2f, top - 20f, boxEnv.widthText)
        drawCenteredBadge(scope, right + 40f, (top + bottom) / 2f, boxEnv.heightText)
        drawCenteredBadge(scope, (left + right) / 2f, (top + bottom) / 2f, "${boxEnv.diagonalText} | ${boxEnv.areaText}")
    }

    private fun drawArrowhead(scope: DrawScope, from: Offset, to: Offset) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val len = hypot(dx, dy)
        if (len < 1e-4) return

        val arrowLen = 14f
        val arrowW = 6f

        val uX = dx / len
        val uY = dy / len

        val normalX = -uY
        val normalY = uX

        val baseCenter = Offset(to.x - uX * arrowLen, to.y - uY * arrowLen)
        val p1 = Offset(baseCenter.x + normalX * arrowW, baseCenter.y + normalY * arrowW)
        val p2 = Offset(baseCenter.x - normalX * arrowW, baseCenter.y - normalY * arrowW)

        val arrowPath = Path().apply {
            moveTo(to.x, to.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        scope.drawPath(arrowPath, color = DimColor, style = Fill)
    }

    private fun drawCenteredBadge(
        scope: DrawScope,
        cx: Float,
        cy: Float,
        text: String
    ) {
        val canvas = scope.drawContext.canvas.nativeCanvas
        val textWidth = textPaint.measureText(text)
        val paddingH = 14f
        val paddingV = 7f
        val boxWidth = textWidth + paddingH * 2
        val boxHeight = 44f

        val left = cx - boxWidth / 2f
        val top = cy - boxHeight / 2f
        val rect = RectF(left, top, left + boxWidth, top + boxHeight)

        canvas.drawRoundRect(rect, 8f, 8f, badgeBgPaint)
        canvas.drawRoundRect(rect, 8f, 8f, badgeBorderPaint)
        canvas.drawText(text, left + paddingH, top + 30f, textPaint)
    }

    private fun renderMultiRegionArea(
        scope: DrawScope,
        multi: CadMeasurementAnnotation.MultiRegionArea,
        transform: CadViewportTransform
    ) {
        val canvas = scope.drawContext.canvas.nativeCanvas

        // 1. Render each individual region polygon
        for (region in multi.regions) {
            if (region.vertices.size < 2) continue
            val path = Path()
            val sFirst = transform.worldToScreen(region.vertices.first())
            path.moveTo(sFirst.x, sFirst.y)
            for (i in 1 until region.vertices.size) {
                val s = transform.worldToScreen(region.vertices[i])
                path.lineTo(s.x, s.y)
            }
            if (region.vertices.size >= 3) {
                path.close()
                val fillColor = if (region.isSubtract) Color(0x35EF4444) else Color(0x3510B981)
                scope.drawPath(path = path, color = fillColor, style = Fill)
            }

            val strokeColor = if (region.isSubtract) Color(0xFFEF4444) else Color(0xFF10B981)
            val pathEffect = if (region.isSubtract) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null

            scope.drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = 2.6f,
                    cap = StrokeCap.Round,
                    pathEffect = pathEffect
                )
            )

            // Draw vertex dots
            for (v in region.vertices) {
                val s = transform.worldToScreen(v)
                scope.drawCircle(color = strokeColor, radius = 5f, center = Offset(s.x, s.y))
                scope.drawCircle(color = Color.White, radius = 2f, center = Offset(s.x, s.y))
            }

            // Draw region centroid tag
            val sCentroid = transform.worldToScreen(region.centroid)
            val prefix = if (region.isSubtract) "- " else "+ "
            val suffix = if (region.isSubtract) " (Void)" else ""
            val tagText = "$prefix${region.areaText}$suffix"

            val tagW = textPaint.measureText(tagText) + 20f
            val tagH = 38f
            val tagRect = RectF(
                sCentroid.x - tagW / 2f,
                sCentroid.y - tagH / 2f,
                sCentroid.x + tagW / 2f,
                sCentroid.y + tagH / 2f
            )
            canvas.drawRoundRect(tagRect, 8f, 8f, badgeBgPaint)
            val borderPaint = if (region.isSubtract) {
                Paint().apply {
                    color = android.graphics.Color.rgb(239, 68, 68)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                }
            } else badgeBorderPaint
            canvas.drawRoundRect(tagRect, 8f, 8f, borderPaint)
            canvas.drawText(tagText, sCentroid.x - tagW / 2f + 10f, sCentroid.y + 11f, textPaint)
        }

        // 2. Render composite overview badge at overall centroid
        val sCenter = transform.worldToScreen(multi.overallCentroid)
        val line1 = "NET: ${multi.netAreaText}"
        val line2 = "GROSS: ${multi.grossAreaText} | VOID: -${multi.voidAreaText}"
        val line3 = "PERIM: ${multi.totalPerimeterText}"

        val w1 = textPaint.measureText(line1)
        val w2 = subTextPaint.measureText(line2)
        val w3 = subTextPaint.measureText(line3)
        val maxW = maxOf(w1, maxOf(w2, w3)) + 36f
        val boxH = 92f

        val left = sCenter.x - maxW / 2f
        val top = sCenter.y - boxH / 2f
        val summaryRect = RectF(left, top, left + maxW, top + boxH)

        canvas.drawRoundRect(summaryRect, 12f, 12f, badgeBgPaint)
        canvas.drawRoundRect(summaryRect, 12f, 12f, badgeBorderPaint)
        canvas.drawText(line1, left + 18f, top + 28f, textPaint)
        canvas.drawText(line2, left + 18f, top + 56f, subTextPaint)
        canvas.drawText(line3, left + 18f, top + 80f, subTextPaint)
    }

    private fun renderSelectionEnvelope(
        scope: DrawScope,
        selection: CadMeasurementAnnotation.SelectionEnvelope,
        transform: CadViewportTransform
    ) {
        val box = selection.box
        if (box.isEmpty) return

        val sTopLeft = transform.worldToScreen(CadPoint2D(box.minX, box.maxY))
        val sBottomRight = transform.worldToScreen(CadPoint2D(box.maxX, box.minY))

        val left = minOf(sTopLeft.x, sBottomRight.x) - 10f
        val top = minOf(sTopLeft.y, sBottomRight.y) - 10f
        val right = maxOf(sTopLeft.x, sBottomRight.x) + 10f
        val bottom = maxOf(sTopLeft.y, sBottomRight.y) + 10f
        val width = right - left
        val height = bottom - top

        // Translucent highlight fill
        scope.drawRect(
            color = Color(0x18FBBF24),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )

        // Dashed bounding rectangle
        scope.drawRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(
                width = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        )

        // Corner bracket ticks
        val bracketLen = minOf(20f, width / 4f, height / 4f)
        val tickStroke = Stroke(width = 3.5f, cap = StrokeCap.Square)

        // Top-left corner
        scope.drawLine(Color(0xFFFBBF24), Offset(left, top), Offset(left + bracketLen, top), strokeWidth = 3.5f)
        scope.drawLine(Color(0xFFFBBF24), Offset(left, top), Offset(left, top + bracketLen), strokeWidth = 3.5f)
        // Top-right corner
        scope.drawLine(Color(0xFFFBBF24), Offset(right, top), Offset(right - bracketLen, top), strokeWidth = 3.5f)
        scope.drawLine(Color(0xFFFBBF24), Offset(right, top), Offset(right, top + bracketLen), strokeWidth = 3.5f)
        // Bottom-left corner
        scope.drawLine(Color(0xFFFBBF24), Offset(left, bottom), Offset(left + bracketLen, bottom), strokeWidth = 3.5f)
        scope.drawLine(Color(0xFFFBBF24), Offset(left, bottom), Offset(left, bottom - bracketLen), strokeWidth = 3.5f)
        // Bottom-right corner
        scope.drawLine(Color(0xFFFBBF24), Offset(right, bottom), Offset(right - bracketLen, bottom), strokeWidth = 3.5f)
        scope.drawLine(Color(0xFFFBBF24), Offset(right, bottom), Offset(right, bottom - bracketLen), strokeWidth = 3.5f)

        // Floating Summary Callout
        val canvas = scope.drawContext.canvas.nativeCanvas
        val midX = (left + right) / 2f
        val calloutY = maxOf(40f, top - 30f)

        val line1 = "SELECTED: ${selection.countText} | LEN: ${selection.lengthText}"
        val line2 = "AREA: ${selection.areaText} | PERIM: ${selection.perimeterText}"
        val w1 = textPaint.measureText(line1)
        val w2 = subTextPaint.measureText(line2)
        val maxW = maxOf(w1, w2) + 36f
        val calloutH = 68f

        val cLeft = midX - maxW / 2f
        val cTop = calloutY - calloutH / 2f
        val cRect = RectF(cLeft, cTop, cLeft + maxW, cTop + calloutH)

        canvas.drawRoundRect(cRect, 10f, 10f, badgeBgPaint)
        canvas.drawRoundRect(cRect, 10f, 10f, badgeBorderPaint)
        canvas.drawText(line1, cLeft + 18f, cTop + 26f, textPaint)
        canvas.drawText(line2, cLeft + 18f, cTop + 52f, subTextPaint)
    }
}

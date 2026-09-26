package com.example.cad.engine.snap

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.cad.model.CadPoint2D
import com.example.ui.theme.CadSnapGreen

/**
 * Standard CAD Snap Marker Renderer.
 *
 * Renders distinct geometric OSNAP glyphs according to industry drafting standards:
 * - [CadSnapMode.ENDPOINT]: Square (□)
 * - [CadSnapMode.MIDPOINT]: Equilateral Triangle (△)
 * - [CadSnapMode.CENTER]: Circle with center pip (○)
 * - [CadSnapMode.INTERSECTION]: Diagonal Cross (✕)
 * - [CadSnapMode.QUADRANT]: Diamond (◇)
 * - [CadSnapMode.PERPENDICULAR]: Right Angle (⦜)
 * - [CadSnapMode.NEAREST]: Hourglass (⧖)
 *
 * Features high-contrast dark halo styling for guaranteed visibility on both light and dark backgrounds,
 * plus optional cursor aperture box and tooltip badge.
 */
object CadSnapMarkerRenderer {

    private val MarkerGreen = CadSnapGreen
    private val HaloBlack = Color(0xCC000000)
    private val FillTranslucentGreen = Color(0x2200E676)

    // Android native text paint for tooltip label
    private val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
        style = Paint.Style.FILL
    }

    private val badgeBgPaint = Paint().apply {
        color = android.graphics.Color.argb(220, 18, 22, 28)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val badgeBorderPaint = Paint().apply {
        color = android.graphics.Color.argb(255, 0, 230, 118)
        strokeWidth = 2f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    /**
     * Renders the active snap marker and optional badge/aperture.
     */
    fun renderSnap(
        drawScope: DrawScope,
        snapResult: CadSnapResult,
        settings: CadOsnapSettings,
        cursorScreen: CadPoint2D? = null
    ) {
        val sx = snapResult.screenPoint.x
        val sy = snapResult.screenPoint.y

        // Optional snap aperture box around cursor
        if (settings.showAperture && cursorScreen != null) {
            val halfAperture = settings.apertureSizeScreenPx / 2f
            drawScope.drawRect(
                color = Color(0x6600E676),
                topLeft = Offset(cursorScreen.x - halfAperture, cursorScreen.y - halfAperture),
                size = Size(settings.apertureSizeScreenPx, settings.apertureSizeScreenPx),
                style = Stroke(width = 1.5f)
            )
        }

        if (!settings.showMarker) return

        val markerRadius = 14f
        val strokeWidth = 2.5f
        val haloWidth = 4.5f

        when (snapResult.mode) {
            CadSnapMode.ENDPOINT -> {
                // Square □
                drawSquareMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
            CadSnapMode.MIDPOINT -> {
                // Triangle △
                drawTriangleMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
            CadSnapMode.CENTER -> {
                // Circle ○ with center pip
                drawCenterMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
            CadSnapMode.INTERSECTION -> {
                // Diagonal Cross ✕
                drawIntersectionMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
            CadSnapMode.QUADRANT -> {
                // Diamond ◇
                drawDiamondMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
            CadSnapMode.PERPENDICULAR -> {
                // Right Angle ⦜
                drawPerpendicularMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
            CadSnapMode.NEAREST -> {
                // Hourglass ⧖
                drawNearestMarker(drawScope, sx, sy, markerRadius, strokeWidth, haloWidth)
            }
        }

        // Snap tooltip badge (e.g. "Endpoint")
        if (settings.showTooltip) {
            drawTooltipBadge(drawScope, sx + markerRadius + 8f, sy - markerRadius - 4f, snapResult.description)
        }
    }

    private fun drawSquareMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        val half = radius
        val rect = androidx.compose.ui.geometry.Rect(cx - half, cy - half, cx + half, cy + half)

        // Dark halo
        scope.drawRect(
            color = HaloBlack,
            topLeft = Offset(cx - half, cy - half),
            size = Size(half * 2, half * 2),
            style = Stroke(width = haloWidth)
        )
        // Tinted fill
        scope.drawRect(
            color = FillTranslucentGreen,
            topLeft = Offset(cx - half, cy - half),
            size = Size(half * 2, half * 2),
            style = Fill
        )
        // Green stroke
        scope.drawRect(
            color = MarkerGreen,
            topLeft = Offset(cx - half, cy - half),
            size = Size(half * 2, half * 2),
            style = Stroke(width = strokeWidth)
        )
    }

    private fun drawTriangleMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        val path = Path().apply {
            moveTo(cx, cy - radius)
            lineTo(cx + radius * 1.1f, cy + radius * 0.9f)
            lineTo(cx - radius * 1.1f, cy + radius * 0.9f)
            close()
        }

        scope.drawPath(path, HaloBlack, style = Stroke(width = haloWidth, join = StrokeJoin.Round))
        scope.drawPath(path, FillTranslucentGreen, style = Fill)
        scope.drawPath(path, MarkerGreen, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
    }

    private fun drawCenterMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        // Circle
        scope.drawCircle(HaloBlack, radius = radius, center = Offset(cx, cy), style = Stroke(width = haloWidth))
        scope.drawCircle(FillTranslucentGreen, radius = radius, center = Offset(cx, cy), style = Fill)
        scope.drawCircle(MarkerGreen, radius = radius, center = Offset(cx, cy), style = Stroke(width = strokeWidth))
        // Center cross pip
        val pip = 4f
        scope.drawLine(MarkerGreen, Offset(cx - pip, cy), Offset(cx + pip, cy), strokeWidth = 2f)
        scope.drawLine(MarkerGreen, Offset(cx, cy - pip), Offset(cx, cy + pip), strokeWidth = 2f)
    }

    private fun drawIntersectionMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        val half = radius * 0.85f

        // Dark halo
        scope.drawLine(HaloBlack, Offset(cx - half, cy - half), Offset(cx + half, cy + half), strokeWidth = haloWidth, cap = StrokeCap.Round)
        scope.drawLine(HaloBlack, Offset(cx - half, cy + half), Offset(cx + half, cy - half), strokeWidth = haloWidth, cap = StrokeCap.Round)

        // Green X
        scope.drawLine(MarkerGreen, Offset(cx - half, cy - half), Offset(cx + half, cy + half), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        scope.drawLine(MarkerGreen, Offset(cx - half, cy + half), Offset(cx + half, cy - half), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }

    private fun drawDiamondMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        val path = Path().apply {
            moveTo(cx, cy - radius * 1.15f)
            lineTo(cx + radius * 1.15f, cy)
            lineTo(cx, cy + radius * 1.15f)
            lineTo(cx - radius * 1.15f, cy)
            close()
        }

        scope.drawPath(path, HaloBlack, style = Stroke(width = haloWidth, join = StrokeJoin.Round))
        scope.drawPath(path, FillTranslucentGreen, style = Fill)
        scope.drawPath(path, MarkerGreen, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
    }

    private fun drawPerpendicularMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        val arm = radius * 1.2f
        val pip = radius * 0.6f

        val path = Path().apply {
            // Horizontal baseline
            moveTo(cx - arm * 0.5f, cy + arm * 0.5f)
            lineTo(cx + arm * 0.7f, cy + arm * 0.5f)
            // Vertical leg
            moveTo(cx, cy + arm * 0.5f)
            lineTo(cx, cy - arm * 0.7f)
            // Right-angle square pip
            moveTo(cx, cy + arm * 0.5f - pip)
            lineTo(cx + pip, cy + arm * 0.5f - pip)
            lineTo(cx + pip, cy + arm * 0.5f)
        }

        scope.drawPath(path, HaloBlack, style = Stroke(width = haloWidth, cap = StrokeCap.Square))
        scope.drawPath(path, MarkerGreen, style = Stroke(width = strokeWidth, cap = StrokeCap.Square))
    }

    private fun drawNearestMarker(
        scope: DrawScope,
        cx: Float, cy: Float,
        radius: Float, strokeWidth: Float, haloWidth: Float
    ) {
        val w = radius * 0.9f
        val h = radius * 1.1f

        val path = Path().apply {
            moveTo(cx - w, cy - h)
            lineTo(cx + w, cy - h)
            lineTo(cx - w, cy + h)
            lineTo(cx + w, cy + h)
            close()
        }

        scope.drawPath(path, HaloBlack, style = Stroke(width = haloWidth, join = StrokeJoin.Round))
        scope.drawPath(path, FillTranslucentGreen, style = Fill)
        scope.drawPath(path, MarkerGreen, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
    }

    private fun drawTooltipBadge(
        scope: DrawScope,
        badgeLeft: Float,
        badgeTop: Float,
        text: String
    ) {
        val canvas = scope.drawContext.canvas.nativeCanvas
        val textWidth = textPaint.measureText(text)
        val textHeight = 24f
        val padH = 12f
        val padV = 6f

        val bgRect = android.graphics.RectF(
            badgeLeft,
            badgeTop - textHeight - padV,
            badgeLeft + textWidth + padH * 2,
            badgeTop + padV
        )

        canvas.drawRoundRect(bgRect, 8f, 8f, badgeBgPaint)
        canvas.drawRoundRect(bgRect, 8f, 8f, badgeBorderPaint)
        canvas.drawText(text, badgeLeft + padH, badgeTop - 4f, textPaint)
    }
}

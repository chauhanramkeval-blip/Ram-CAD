package com.example.cad.model

/**
 * Clean domain entity hierarchy for 2D CAD entities.
 * Designed for direct mapping to/from external DWG/DXF engine primitives.
 */
sealed class CadEntity {
    abstract val id: String
    abstract val layerId: String
    abstract val colorArgb: Long?
    abstract val boundingBox: CadBoundingBox

    data class Line(
        override val id: String,
        override val layerId: String = "0",
        override val colorArgb: Long? = null,
        val start: CadPoint2D,
        val end: CadPoint2D,
        val strokeWidth: Float = 1.5f
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = CadBoundingBox(
            minX = minOf(start.x, end.x),
            minY = minOf(start.y, end.y),
            maxX = maxOf(start.x, end.x),
            maxY = maxOf(start.y, end.y)
        )
    }

    data class Polyline(
        override val id: String,
        override val layerId: String = "0",
        override val colorArgb: Long? = null,
        val points: List<CadPoint2D>,
        val isClosed: Boolean = false,
        val strokeWidth: Float = 1.5f
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = if (points.isEmpty()) {
            CadBoundingBox.EMPTY
        } else {
            var minX = points[0].x
            var minY = points[0].y
            var maxX = points[0].x
            var maxY = points[0].y
            for (i in 1 until points.size) {
                val pt = points[i]
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
            CadBoundingBox(minX, minY, maxX, maxY)
        }
    }

    data class Circle(
        override val id: String,
        override val layerId: String = "0",
        override val colorArgb: Long? = null,
        val center: CadPoint2D,
        val radius: Float,
        val strokeWidth: Float = 1.5f
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = CadBoundingBox(
            minX = center.x - radius,
            minY = center.y - radius,
            maxX = center.x + radius,
            maxY = center.y + radius
        )
    }

    data class Arc(
        override val id: String,
        override val layerId: String = "0",
        override val colorArgb: Long? = null,
        val center: CadPoint2D,
        val radius: Float,
        val startAngleDeg: Float,
        val sweepAngleDeg: Float,
        val strokeWidth: Float = 1.5f
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = CadBoundingBox(
            minX = center.x - radius,
            minY = center.y - radius,
            maxX = center.x + radius,
            maxY = center.y + radius
        )
    }

    data class Text(
        override val id: String,
        override val layerId: String = "0",
        override val colorArgb: Long? = null,
        val position: CadPoint2D,
        val text: String,
        val textHeight: Float = 12f,
        val rotationDeg: Float = 0f,
        val isMultiLine: Boolean = false,
        val frameWidth: Float? = null
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = computeTextBounds()

        private fun computeTextBounds(): CadBoundingBox {
            val lines = if (isMultiLine || text.contains('\n')) text.split('\n') else listOf(text)
            val maxChars = lines.maxOfOrNull { it.length } ?: 1
            val w = maxOf(frameWidth ?: 0f, maxChars * textHeight * 0.65f)
            val h = maxOf(textHeight, lines.size * textHeight * 1.35f)
            if (rotationDeg == 0f) {
                return CadBoundingBox(
                    minX = position.x,
                    minY = position.y,
                    maxX = position.x + w,
                    maxY = position.y + h
                )
            }
            val rad = Math.toRadians(-rotationDeg.toDouble())
            val cosA = kotlin.math.cos(rad)
            val sinA = kotlin.math.sin(rad)
            val c1 = position
            val c2 = CadPoint2D((position.x + w * cosA).toFloat(), (position.y + w * sinA).toFloat())
            val c3 = CadPoint2D((position.x + w * cosA - h * sinA).toFloat(), (position.y + w * sinA + h * cosA).toFloat())
            val c4 = CadPoint2D((position.x - h * sinA).toFloat(), (position.y + h * cosA).toFloat())
            return CadBoundingBox().include(c1).include(c2).include(c3).include(c4)
        }
    }

    data class Leader(
        override val id: String,
        override val layerId: String = "ANNOTATIONS",
        override val colorArgb: Long? = null,
        val arrowPoint: CadPoint2D,
        val kneePoint: CadPoint2D,
        val landingEndPoint: CadPoint2D,
        val text: String,
        val textHeight: Float = 12f,
        val arrowSize: Float = 8f,
        val strokeWidth: Float = 1.8f
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = run {
            val textW = text.length * textHeight * 0.65f
            val textH = textHeight * 1.35f
            CadBoundingBox()
                .include(arrowPoint)
                .include(kneePoint)
                .include(landingEndPoint)
                .include(CadPoint2D(landingEndPoint.x + textW, landingEndPoint.y + textH))
                .include(CadPoint2D(landingEndPoint.x - textW, landingEndPoint.y - textH))
        }
    }

    data class Arrow(
        override val id: String,
        override val layerId: String = "ANNOTATIONS",
        override val colorArgb: Long? = null,
        val start: CadPoint2D,
        val end: CadPoint2D,
        val headSize: Float = 10f,
        val strokeWidth: Float = 2.0f,
        val isDoubleHeaded: Boolean = false,
        val label: String? = null
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = CadBoundingBox(
            minX = minOf(start.x, end.x) - headSize,
            minY = minOf(start.y, end.y) - headSize,
            maxX = maxOf(start.x, end.x) + headSize,
            maxY = maxOf(start.y, end.y) + headSize
        )
    }

    data class RevisionCloud(
        override val id: String,
        override val layerId: String = "MARKUP",
        override val colorArgb: Long? = 0xFFFF5722,
        val vertices: List<CadPoint2D>,
        val arcRadius: Float = 16f,
        val strokeWidth: Float = 2.2f,
        val isClosed: Boolean = true,
        val revisionTag: String? = null
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = if (vertices.isEmpty()) {
            CadBoundingBox.EMPTY
        } else {
            var minX = vertices[0].x
            var minY = vertices[0].y
            var maxX = vertices[0].x
            var maxY = vertices[0].y
            for (i in 1 until vertices.size) {
                val pt = vertices[i]
                if (pt.x < minX) minX = pt.x
                if (pt.y < minY) minY = pt.y
                if (pt.x > maxX) maxX = pt.x
                if (pt.y > maxY) maxY = pt.y
            }
            val pad = arcRadius * 1.5f
            CadBoundingBox(minX - pad, minY - pad, maxX + pad, maxY + pad)
        }
    }

    data class Point(
        override val id: String,
        override val layerId: String = "0",
        override val colorArgb: Long? = null,
        val position: CadPoint2D
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = CadBoundingBox(
            minX = position.x - 2f,
            minY = position.y - 2f,
            maxX = position.x + 2f,
            maxY = position.y + 2f
        )
    }

    data class Dimension(
        override val id: String,
        override val layerId: String = "dim",
        override val colorArgb: Long? = 0xFFFFD600,
        val start: CadPoint2D,
        val end: CadPoint2D,
        val textPoint: CadPoint2D,
        val valueText: String
    ) : CadEntity() {
        override val boundingBox: CadBoundingBox = CadBoundingBox(
            minX = minOf(start.x, minOf(end.x, textPoint.x)),
            minY = minOf(start.y, minOf(end.y, textPoint.y)),
            maxX = maxOf(start.x, maxOf(end.x, textPoint.x)),
            maxY = maxOf(start.y, maxOf(end.y, textPoint.y))
        )
    }
}

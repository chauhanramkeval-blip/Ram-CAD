package com.example.cad.model

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * 2D Precision Coordinate in CAD World Space.
 */
data class CadPoint2D(
    val x: Float = 0f,
    val y: Float = 0f
) {
    fun distanceTo(other: CadPoint2D): Float {
        return hypot(other.x - x, other.y - y)
    }

    fun angleTo(other: CadPoint2D): Float {
        val rad = atan2((other.y - y).toDouble(), (other.x - x).toDouble())
        val deg = Math.toDegrees(rad).toFloat()
        return if (deg < 0) deg + 360f else deg
    }

    operator fun plus(other: CadPoint2D) = CadPoint2D(x + other.x, y + other.y)
    operator fun minus(other: CadPoint2D) = CadPoint2D(x - other.x, y - other.y)
    operator fun times(scalar: Float) = CadPoint2D(x * scalar, y * scalar)
}

/**
 * Vector representation for CAD displacement and direction.
 */
data class CadVector2D(
    val dx: Float,
    val dy: Float
) {
    val length: Float get() = hypot(dx, dy)
}

/**
 * World space bounding box for CAD drawing limits and zoom extents.
 */
data class CadBoundingBox(
    val minX: Float = Float.MAX_VALUE,
    val minY: Float = Float.MAX_VALUE,
    val maxX: Float = -Float.MAX_VALUE,
    val maxY: Float = -Float.MAX_VALUE
) {
    val width: Float get() = if (isEmpty) 0f else (maxX - minX)
    val height: Float get() = if (isEmpty) 0f else (maxY - minY)
    val centerX: Float get() = if (isEmpty) 0f else (minX + maxX) * 0.5f
    val centerY: Float get() = if (isEmpty) 0f else (minY + maxY) * 0.5f
    val isEmpty: Boolean get() = minX > maxX || minY > maxY

    fun include(point: CadPoint2D): CadBoundingBox {
        if (isEmpty) return CadBoundingBox(point.x, point.y, point.x, point.y)
        return CadBoundingBox(
            minX = minOf(minX, point.x),
            minY = minOf(minY, point.y),
            maxX = maxOf(maxX, point.x),
            maxY = maxOf(maxY, point.y)
        )
    }

    fun include(other: CadBoundingBox): CadBoundingBox {
        if (other.isEmpty) return this
        if (this.isEmpty) return other
        return CadBoundingBox(
            minX = minOf(minX, other.minX),
            minY = minOf(minY, other.minY),
            maxX = maxOf(maxX, other.maxX),
            maxY = maxOf(maxY, other.maxY)
        )
    }

    fun intersects(other: CadBoundingBox): Boolean {
        if (this.isEmpty || other.isEmpty) return false
        return this.minX <= other.maxX && this.maxX >= other.minX &&
               this.minY <= other.maxY && this.maxY >= other.minY
    }

    fun contains(point: CadPoint2D): Boolean {
        if (isEmpty) return false
        return point.x in minX..maxX && point.y in minY..maxY
    }

    fun contains(other: CadBoundingBox): Boolean {
        if (isEmpty || other.isEmpty) return false
        return other.minX >= minX && other.maxX <= maxX &&
               other.minY >= minY && other.maxY <= maxY
    }

    fun expand(margin: Float): CadBoundingBox {
        if (isEmpty) return this
        return CadBoundingBox(
            minX = minX - margin,
            minY = minY - margin,
            maxX = maxX + margin,
            maxY = maxY + margin
        )
    }

    companion object {
        val EMPTY = CadBoundingBox()
    }
}

/**
 * Viewport transformation state (pan offset and zoom scale).
 *
 * Uses [CadMatrix2D] internally to maintain separation between CAD world coordinates
 * and screen pixels, offering zero-allocation coordinate projection.
 */
data class CadViewportTransform(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val scale: Float = 1.0f
) {
    val matrix: CadMatrix2D by lazy {
        CadMatrix2D.fromViewport(panX, panY, scale)
    }

    fun screenToWorld(screenX: Float, screenY: Float): CadPoint2D {
        return CadPoint2D(
            x = matrix.inverseMapX(screenX, screenY),
            y = matrix.inverseMapY(screenX, screenY)
        )
    }

    fun worldToScreen(worldPoint: CadPoint2D): CadPoint2D {
        return CadPoint2D(
            x = matrix.mapX(worldPoint.x, worldPoint.y),
            y = matrix.mapY(worldPoint.x, worldPoint.y)
        )
    }

    fun worldXToScreenX(worldX: Float, worldY: Float = 0f): Float = matrix.mapX(worldX, worldY)
    fun worldYToScreenY(worldX: Float, worldY: Float): Float = matrix.mapY(worldX, worldY)
}

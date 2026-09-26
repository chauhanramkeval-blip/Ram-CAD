package com.example.cad.model

import kotlin.math.abs

/**
 * High-performance 3x3 2D Affine Transformation Matrix for CAD World <-> Screen mapping.
 *
 * Affine transformation matrix structure:
 * [ m00, m01, m02 ]   [ scaleX, 0,      panX ]
 * [ m10, m11, m12 ] = [ 0,      -scaleY, panY ] (CAD Y inverted)
 * [  0,   0,   1  ]   [ 0,      0,       1   ]
 *
 * Designed to perform zero-allocation forward and inverse transformations,
 * supporting high throughput rendering of thousands of CAD entities.
 */
data class CadMatrix2D(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f,
    val m10: Float = 0f, val m11: Float = 1f, val m12: Float = 0f
) {
    /**
     * Determinant of the 2x2 affine linear part: (m00 * m11 - m01 * m10).
     */
    val determinant: Float get() = m00 * m11 - m01 * m10

    /**
     * Computes the inverse transformation matrix (Screen -> World).
     */
    val inverse: CadMatrix2D by lazy {
        val det = determinant
        if (abs(det) < 1e-9f) {
            IDENTITY
        } else {
            val invDet = 1.0f / det
            val i00 = m11 * invDet
            val i01 = -m01 * invDet
            val i02 = (m01 * m12 - m11 * m02) * invDet

            val i10 = -m10 * invDet
            val i11 = m00 * invDet
            val i12 = (m10 * m02 - m00 * m12) * invDet

            CadMatrix2D(i00, i01, i02, i10, i11, i12)
        }
    }

    /**
     * Maps world coordinates (x, y) to screen coordinates.
     */
    fun mapX(x: Float, y: Float): Float = m00 * x + m01 * y + m02
    fun mapY(x: Float, y: Float): Float = m10 * x + m11 * y + m12

    /**
     * Maps screen coordinates (x, y) to world coordinates using the inverse matrix.
     */
    fun inverseMapX(screenX: Float, screenY: Float): Float = inverse.mapX(screenX, screenY)
    fun inverseMapY(screenX: Float, screenY: Float): Float = inverse.mapY(screenX, screenY)

    companion object {
        val IDENTITY = CadMatrix2D()

        /**
         * Factory for standard CAD viewport transformation:
         * World space has origin (0, 0) with Y pointing UP.
         * Screen space has origin (0, 0) top-left with Y pointing DOWN.
         */
        fun fromViewport(panX: Float, panY: Float, scale: Float): CadMatrix2D {
            return CadMatrix2D(
                m00 = scale,
                m01 = 0f,
                m02 = panX,
                m10 = 0f,
                m11 = -scale, // Invert Y for engineering CAD standard
                m12 = panY
            )
        }
    }
}

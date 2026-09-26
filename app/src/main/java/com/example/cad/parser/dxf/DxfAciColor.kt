package com.example.cad.parser.dxf

/**
 * AutoCAD Color Index (ACI) lookup and conversion utility.
 *
 * Maps standard 1-255 AutoCAD color index codes to 32-bit ARGB values.
 * Code 0 represents ByBlock, Code 256 represents ByLayer.
 */
object DxfAciColor {

    // Standard 1 to 9 AutoCAD primary colors
    val STANDARD_COLORS = longArrayOf(
        0xFF000000, // 0: ByBlock (placeholder)
        0xFFFF0000, // 1: Red
        0xFFFFFF00, // 2: Yellow
        0xFF00FF00, // 3: Green
        0xFF00FFFF, // 4: Cyan
        0xFF0000FF, // 5: Blue
        0xFFFF00FF, // 6: Magenta
        0xFFFFFFFF, // 7: White / Black (adapts to background)
        0xFF808080, // 8: Dark Gray
        0xFFC0C0C0  // 9: Light Gray
    )

    /**
     * Converts an AutoCAD Color Index (ACI) integer to an ARGB Long.
     * Returns null for ByLayer (256) or ByBlock (0), allowing fallback to layer or default color.
     */
    fun toArgb(aci: Int): Long? {
        val index = kotlin.math.abs(aci) // Negative numbers indicate turned off layers in DXF
        if (index == 0 || index == 256) {
            return null // ByBlock or ByLayer
        }

        if (index in 1..9) {
            return STANDARD_COLORS[index]
        }

        if (index in 10..249) {
            // AutoCAD standard 24-step color wheel with 5 shades
            // Hue index 0..23, lightness/saturation level 0..4
            val hueIndex = (index - 10) / 10
            val shadeIndex = (index - 10) % 10

            val hueDeg = hueIndex * 15.0f
            val saturation: Float
            val lightness: Float

            when (shadeIndex) {
                0 -> { saturation = 1.0f; lightness = 0.5f }
                1 -> { saturation = 1.0f; lightness = 0.65f }
                2 -> { saturation = 1.0f; lightness = 0.8f }
                3 -> { saturation = 1.0f; lightness = 0.9f }
                4 -> { saturation = 1.0f; lightness = 0.95f }
                5 -> { saturation = 0.8f; lightness = 0.5f }
                6 -> { saturation = 0.6f; lightness = 0.4f }
                7 -> { saturation = 0.4f; lightness = 0.3f }
                8 -> { saturation = 0.25f; lightness = 0.2f }
                else -> { saturation = 0.15f; lightness = 0.1f }
            }

            return hslToArgb(hueDeg, saturation, lightness)
        }

        // Grayscale ramp (250 to 255)
        if (index in 250..255) {
            val gray = ((index - 250) * 51).coerceIn(0, 255)
            return (0xFF000000L or (gray.toLong() shl 16) or (gray.toLong() shl 8) or gray.toLong())
        }

        return 0xFFFFFFFF
    }

    private fun hslToArgb(hue: Float, sat: Float, light: Float): Long {
        val c = (1f - kotlin.math.abs(2f * light - 1f)) * sat
        val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
        val m = light - c / 2f

        var r = 0f
        var g = 0f
        var b = 0f

        when {
            hue < 60f -> { r = c; g = x; b = 0f }
            hue < 120f -> { r = x; g = c; b = 0f }
            hue < 180f -> { r = 0f; g = c; b = x }
            hue < 240f -> { r = 0f; g = x; b = c }
            hue < 300f -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        val red = ((r + m) * 255f).toInt().coerceIn(0, 255)
        val green = ((g + m) * 255f).toInt().coerceIn(0, 255)
        val blue = ((b + m) * 255f).toInt().coerceIn(0, 255)

        return (0xFF000000L or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong())
    }
}

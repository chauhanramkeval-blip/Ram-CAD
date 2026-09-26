package com.example.cad.model

/**
 * Standard CAD Layer definition, matching industry CAD schemas (AutoCAD/DXF).
 */
data class CadLayer(
    val id: String,
    val name: String,
    val colorArgb: Long = 0xFFFFFFFF,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val lineWeight: Float = 1.0f,
    val lineType: String = "Continuous"
) {
    companion object {
        val DEFAULT_LAYER_0 = CadLayer(
            id = "0",
            name = "0 (Default)",
            colorArgb = 0xFFFFFFFF,
            isVisible = true,
            isLocked = false,
            lineWeight = 1.0f
        )
        val ANNOTATION_LAYER = CadLayer(
            id = "dim",
            name = "Dimensions",
            colorArgb = 0xFFFFD600,
            isVisible = true,
            isLocked = false,
            lineWeight = 0.8f
        )
        val HIDDEN_LAYER = CadLayer(
            id = "hidden",
            name = "Hidden Lines",
            colorArgb = 0xFF00E5FF,
            isVisible = true,
            isLocked = false,
            lineWeight = 0.8f,
            lineType = "Dashed"
        )
        val CENTER_LAYER = CadLayer(
            id = "center",
            name = "Centerlines",
            colorArgb = 0xFFFF5252,
            isVisible = true,
            isLocked = false,
            lineWeight = 0.6f,
            lineType = "Center"
        )
        val MARKUP_LAYER = CadLayer(
            id = "markup",
            name = "Annotations & Markup",
            colorArgb = 0xFFFF7043,
            isVisible = true,
            isLocked = false,
            lineWeight = 1.0f
        )
    }
}

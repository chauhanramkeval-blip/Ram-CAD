package com.example.cad.engine.api

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.cad.model.CadViewportTransform

enum class CadRenderMode {
    WIREFRAME,
    SHADED,
    HIDDEN_LINE,
    HIGH_CONTRAST_MONOCHROME
}

/**
 * Context parameters passed to the CAD engine rendering pipeline.
 *
 * Keeps the rendering engine independent of specific UI widgets while
 * providing necessary viewport dimensions and transformation parameters.
 */
data class CadRenderContext(
    val drawScope: DrawScope,
    val transform: CadViewportTransform,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val selectedEntityId: String? = null,
    val renderMode: CadRenderMode = CadRenderMode.WIREFRAME,
    val showGrid: Boolean = true,
    val showAxes: Boolean = true,
    val fullScreenCrosshair: Boolean = true,
    val antialiasing: Boolean = true
)

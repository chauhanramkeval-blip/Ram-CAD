package com.example.cad.engine

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.cad.model.CadDocument
import com.example.cad.model.CadViewportTransform

/**
 * Modular interface for CAD drawing rendering.
 *
 * This abstraction enables switching between:
 * 1. Jetpack Compose Canvas renderer (current pure-Kotlin baseline)
 * 2. Hardware-accelerated OpenGL ES / Vulkan surface
 * 3. Future Native C++ CAD Rendering Engine (e.g. Teigha / Open Design Alliance / LibreCAD)
 */
interface CadRenderer {
    /**
     * Renders standard CAD drafting grid with major and minor division lines.
     */
    fun renderGrid(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        viewportWidth: Float,
        viewportHeight: Float,
        gridSpacing: Float = 50f,
        subdivisions: Int = 5
    )

    /**
     * Renders CAD world origin crosshair (X in Red, Y in Green).
     */
    fun renderAxes(
        drawScope: DrawScope,
        transform: CadViewportTransform
    )

    /**
     * Renders high-precision CAD drafting crosshair and cursor indicator.
     */
    fun renderCrosshair(
        drawScope: DrawScope,
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        cursorWorldX: Float,
        cursorWorldY: Float,
        unitAbbreviation: String = "mm",
        fullScreenCrosshair: Boolean = true
    )

    /**
     * Renders the complete vector geometry of the document.
     */
    fun renderDocument(
        drawScope: DrawScope,
        document: CadDocument,
        transform: CadViewportTransform,
        selectedEntityId: String? = null,
        selectedEntityIds: Set<String> = emptySet(),
        selectionGrips: List<com.example.cad.engine.selection.SelectionGrip> = emptyList()
    )

    /**
     * Renders an in-progress interactive CAD selection rectangle (Window or Crossing).
     */
    fun renderSelectionBox(
        drawScope: DrawScope,
        box: com.example.cad.engine.selection.SelectionBox
    )
}

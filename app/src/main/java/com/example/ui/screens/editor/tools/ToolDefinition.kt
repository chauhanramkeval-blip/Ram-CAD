package com.example.ui.screens.editor.tools

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Indicates whether a CAD tool is fully functional in the native engine,
 * requires an advanced external CAD engine (e.g. 3D spatial ACIS / OpenNURBS / Teigha kernel),
 * or is coming soon.
 */
enum class ToolAvailability {
    /** Fully implemented and integrated with the active interactive CAD engine */
    AVAILABLE,

    /** Advanced geometric operation requiring specialized native CAD kernel */
    ENGINE_REQUIRED,

    /** Planned feature on the CAD roadmap */
    COMING_SOON
}

/**
 * The 10 canonical mobile CAD categories matching professional AutoCAD Mobile interfaces.
 */
enum class PopupCategory(
    val id: String,
    val title: String,
    val subtitle: String
) {
    ANNOTATION("annotation", "Annotation", "Text, Leaders & Markups"),
    DRAW("draw", "Draw", "Vector Drafting & Geometry"),
    EDIT("edit", "Edit", "Transform & Modify Entities"),
    LAYER("layer", "Layer", "Layers & State Management"),
    MEASURE("measure", "Measure", "Precision Dimensions & Areas"),
    DIMENSION("dimension", "Dimension", "CAD Dimension Lines"),
    COLOR("color", "Color", "Layer & Object Colors"),
    TOOL("tool", "Tool", "Drafting Aids & Snaps"),
    LAYOUT("layout", "Layout", "Viewports & Paper Spaces"),
    VISUAL_STYLE("visual_style", "Visual Style", "Rendering & Display Modes")
}

/**
 * Reusable metadata definition for every CAD tool in the application.
 */
data class ToolDefinition(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val category: PopupCategory,
    val description: String,
    val command: String,
    val availability: ToolAvailability = ToolAvailability.AVAILABLE,
    val requiresSelection: Boolean = false,
    val requiresDrawing: Boolean = false,
    val popupCategory: PopupCategory = category,
    val isToggle: Boolean = false,
    val badgeText: String? = when (availability) {
        ToolAvailability.ENGINE_REQUIRED -> "Engine Required"
        ToolAvailability.COMING_SOON -> "Coming Soon"
        ToolAvailability.AVAILABLE -> null
    }
) {
    val isAvailable: Boolean get() = availability == ToolAvailability.AVAILABLE
}

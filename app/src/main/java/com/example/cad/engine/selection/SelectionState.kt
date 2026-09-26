package com.example.cad.engine.selection

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadPoint2D

/**
 * Type of geometric grip (selection handle) on a CAD entity.
 */
enum class GripType {
    START,
    END,
    MIDPOINT,
    CENTER,
    QUADRANT,
    VERTEX,
    INSERTION
}

/**
 * Grip handle representation in world coordinates.
 */
data class SelectionGrip(
    val entityId: String,
    val type: GripType,
    val position: CadPoint2D,
    val index: Int = 0
)

/**
 * Active interactive selection box on screen and in CAD world space.
 *
 * Left-to-right drag = Window Selection (solid border, selects enclosed entities).
 * Right-to-left drag = Crossing Selection (dashed border, selects enclosed + intersecting entities).
 */
data class SelectionBox(
    val screenStart: CadPoint2D,
    val screenCurrent: CadPoint2D,
    val worldStart: CadPoint2D,
    val worldCurrent: CadPoint2D
) {
    /**
     * In standard CAD, dragging from right to left is Crossing selection.
     */
    val isCrossing: Boolean get() = screenCurrent.x < screenStart.x

    /**
     * In standard CAD, dragging from left to right is Window selection.
     */
    val isWindow: Boolean get() = !isCrossing

    val worldBoundingBox: CadBoundingBox
        get() = CadBoundingBox(
            minX = minOf(worldStart.x, worldCurrent.x),
            minY = minOf(worldStart.y, worldCurrent.y),
            maxX = maxOf(worldStart.x, worldCurrent.x),
            maxY = maxOf(worldStart.y, worldCurrent.y)
        )

    val screenBoundingBox: CadBoundingBox
        get() = CadBoundingBox(
            minX = minOf(screenStart.x, screenCurrent.x),
            minY = minOf(screenStart.y, screenCurrent.y),
            maxX = maxOf(screenStart.x, screenCurrent.x),
            maxY = maxOf(screenStart.y, screenCurrent.y)
        )
}

/**
 * Immutable state representing current CAD object selections.
 */
data class SelectionState(
    val selectedIds: Set<String> = emptySet(),
    val primarySelectedId: String? = null,
    val isMultiSelectEnabled: Boolean = false,
    val activeBox: SelectionBox? = null,
    val grips: List<SelectionGrip> = emptyList()
) {
    val count: Int get() = selectedIds.size
    val hasSelection: Boolean get() = selectedIds.isNotEmpty()

    fun isSelected(id: String): Boolean = selectedIds.contains(id)
}

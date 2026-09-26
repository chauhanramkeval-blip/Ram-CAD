package com.example.cad.engine.snap

/**
 * Supported CAD Object Snap (OSNAP) modes.
 *
 * Each mode corresponds to a specific geometric feature on CAD entities:
 * - [ENDPOINT]: Endpoints of lines, arcs, and polyline vertices.
 * - [MIDPOINT]: Midpoints of lines, arcs, and polyline segments.
 * - [CENTER]: Center point of circles and circular arcs.
 * - [INTERSECTION]: Geometric intersection points between two entities.
 * - [QUADRANT]: Cardinal 0°, 90°, 180°, 270° quadrant points on circles and arcs.
 * - [PERPENDICULAR]: Normal projection point from an anchor/base point onto an entity.
 * - [NEAREST]: Closest point on any entity contour to the cursor.
 */
enum class CadSnapMode(
    val displayName: String,
    val description: String,
    val priority: Int
) {
    ENDPOINT(
        displayName = "Endpoint",
        description = "Snaps to the closest endpoint of a line, arc, or polyline vertex",
        priority = 1
    ),
    MIDPOINT(
        displayName = "Midpoint",
        description = "Snaps to the exact midpoint of a line segment, arc, or polyline edge",
        priority = 2
    ),
    CENTER(
        displayName = "Center",
        description = "Snaps to the center point of a circle or arc",
        priority = 3
    ),
    INTERSECTION(
        displayName = "Intersection",
        description = "Snaps to the intersection point between two crossing entities",
        priority = 4
    ),
    QUADRANT(
        displayName = "Quadrant",
        description = "Snaps to the 0°, 90°, 180°, or 270° quadrant point of a circle or arc",
        priority = 5
    ),
    PERPENDICULAR(
        displayName = "Perpendicular",
        description = "Snaps perpendicular from the current base/anchor point to an entity",
        priority = 6
    ),
    NEAREST(
        displayName = "Nearest",
        description = "Snaps to the nearest point on any entity contour or edge",
        priority = 7
    );

    companion object {
        val DEFAULT_MODES: Set<CadSnapMode> = setOf(
            ENDPOINT,
            MIDPOINT,
            CENTER,
            INTERSECTION,
            QUADRANT,
            PERPENDICULAR,
            NEAREST
        )
    }
}

package com.example.cad.engine.measurement

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 12 Professional CAD Measurement Tool Modes.
 */
enum class CadMeasurementType(
    val id: String,
    val title: String,
    val shortLabel: String,
    val description: String,
    val minPoints: Int,
    val maxPoints: Int?, // null if multi-point
    val stepPrompts: List<String>
) {
    DISTANCE(
        id = "distance",
        title = "Distance",
        shortLabel = "Dist",
        description = "Direct Euclidean distance between 2 points with ΔX, ΔY, and angle",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap first point", "Tap second point to measure distance")
    ),
    ALIGNED_DISTANCE(
        id = "aligned_distance",
        title = "Aligned Distance",
        shortLabel = "Aligned",
        description = "Dimension line aligned parallel with the measured axis",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap first origin point", "Tap second origin point for aligned dimension")
    ),
    HORIZONTAL_DISTANCE(
        id = "horizontal_distance",
        title = "Horizontal Distance",
        shortLabel = "Horiz",
        description = "Linear horizontal distance (|ΔX|) between two points",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap first point", "Tap second point for horizontal distance")
    ),
    VERTICAL_DISTANCE(
        id = "vertical_distance",
        title = "Vertical Distance",
        shortLabel = "Vert",
        description = "Linear vertical distance (|ΔY|) between two points",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap first point", "Tap second point for vertical distance")
    ),
    ANGLE(
        id = "angle",
        title = "Angle",
        shortLabel = "Angle",
        description = "Angular measurement between 3 points (vertex and 2 rays) or 2 lines",
        minPoints = 3,
        maxPoints = 3,
        stepPrompts = listOf("Tap vertex point", "Tap first ray endpoint", "Tap second ray endpoint")
    ),
    RADIUS(
        id = "radius",
        title = "Radius",
        shortLabel = "Radius",
        description = "Measure circle/arc radius or picked center to circumference",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap center point (or circle)", "Tap rim/circumference point")
    ),
    DIAMETER(
        id = "diameter",
        title = "Diameter",
        shortLabel = "Diam",
        description = "Measure diameter passing through center point",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap center or first rim point", "Tap opposite rim point")
    ),
    AREA(
        id = "area",
        title = "Area",
        shortLabel = "Area",
        description = "Calculates enclosed surface area and perimeter using Shoelace formula",
        minPoints = 3,
        maxPoints = null,
        stepPrompts = listOf("Tap first polygon vertex", "Tap next vertex", "Continue tapping vertices, tap Finish when done")
    ),
    PERIMETER(
        id = "perimeter",
        title = "Perimeter",
        shortLabel = "Perim",
        description = "Measure total closed loop boundary perimeter length",
        minPoints = 3,
        maxPoints = null,
        stepPrompts = listOf("Tap first boundary point", "Tap next point", "Continue tapping, tap Finish to close loop")
    ),
    COORDINATE(
        id = "coordinate",
        title = "Coordinate",
        shortLabel = "Coord",
        description = "Absolute CAD world coordinate callout (X, Y) with leader marker",
        minPoints = 1,
        maxPoints = 1,
        stepPrompts = listOf("Tap any location to query world coordinates")
    ),
    POLYLINE_LENGTH(
        id = "polyline_length",
        title = "Polyline Length",
        shortLabel = "Length",
        description = "Cumulative length through multi-point connected segments",
        minPoints = 2,
        maxPoints = null,
        stepPrompts = listOf("Tap path start point", "Tap next path point", "Tap more points, tap Finish when path is complete")
    ),
    BOUNDING_BOX(
        id = "bounding_box",
        title = "Bounding Box",
        shortLabel = "Bounds",
        description = "Orthogonal envelope extent, width, height, and diagonal",
        minPoints = 2,
        maxPoints = 2,
        stepPrompts = listOf("Tap first bounding corner", "Tap diagonal opposite corner")
    ),
    MULTI_AREA(
        id = "multi_area",
        title = "Multi-Area (Add/Subtract)",
        shortLabel = "MultiArea",
        description = "Measure multiple regions, add positive areas, subtract cutouts/voids, calculate net area",
        minPoints = 3,
        maxPoints = null,
        stepPrompts = listOf("Tap vertices for region", "Tap '+ Add' or '- Subtract' for each region", "Finish to calculate Net & Gross area")
    ),
    SELECTION_SUMMARY(
        id = "selection_summary",
        title = "Selection Summary",
        shortLabel = "Selected",
        description = "Calculate total area, perimeter, linear length, and item count for selected objects",
        minPoints = 0,
        maxPoints = 0,
        stepPrompts = listOf("Select objects on canvas or tap Measure Selection to compute metrics")
    );

    fun promptForStep(currentPointCount: Int): String {
        return when {
            currentPointCount < stepPrompts.size -> stepPrompts[currentPointCount]
            else -> stepPrompts.last()
        }
    }
}

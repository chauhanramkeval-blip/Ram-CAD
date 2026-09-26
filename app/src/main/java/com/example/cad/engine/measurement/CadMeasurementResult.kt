package com.example.cad.engine.measurement

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import kotlin.math.abs

/**
 * Result data models for CAD measurements, supporting all 12+ measurement tools,
 * multi-area boolean aggregation, and selection summaries.
 */
sealed class CadMeasurementResult {
    abstract val type: CadMeasurementType
    abstract val unit: CadUnit
    abstract val primaryFormatted: String
    abstract val secondaryDetails: List<Pair<String, String>>
    abstract val annotation: CadMeasurementAnnotation?

    // 4 Key Display Metrics required across all tools
    abstract val displayArea: String?
    abstract val displayPerimeter: String?
    abstract val displayTotalLength: String?
    abstract val displayObjectCount: String?

    open fun formatSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("=== CAD MEASUREMENT: ${type.title.uppercase()} ===")
        sb.appendLine("Primary: $primaryFormatted")
        if (displayArea != null) sb.appendLine("Area: $displayArea")
        if (displayPerimeter != null) sb.appendLine("Perimeter: $displayPerimeter")
        if (displayTotalLength != null) sb.appendLine("Total Length: $displayTotalLength")
        if (displayObjectCount != null) sb.appendLine("Object Count: $displayObjectCount")
        for ((label, value) in secondaryDetails) {
            sb.appendLine("$label: $value")
        }
        sb.appendLine("Unit: ${unit.displayName} (${unit.abbreviation})")
        return sb.toString().trimEnd()
    }

    /**
     * 1. Direct Euclidean Distance
     */
    data class Distance(
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val distance: Double,
        val deltaX: Double,
        val deltaY: Double,
        val angleDeg: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.LinearDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.DISTANCE
        override val primaryFormatted: String get() = unit.format(distance)
        override val displayArea: String? get() = null
        override val displayPerimeter: String? get() = null
        override val displayTotalLength: String get() = unit.format(distance)
        override val displayObjectCount: String get() = "1 segment (2 points)"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "ΔX" to unit.format(deltaX),
            "ΔY" to unit.format(deltaY),
            "Angle" to "%.2f°".format(angleDeg)
        )
    }

    /**
     * 2. Aligned Distance
     */
    data class AlignedDistance(
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val distance: Double,
        val angleDeg: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.LinearDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.ALIGNED_DISTANCE
        override val primaryFormatted: String get() = unit.format(distance)
        override val displayArea: String? get() = null
        override val displayPerimeter: String? get() = null
        override val displayTotalLength: String get() = unit.format(distance)
        override val displayObjectCount: String get() = "1 aligned segment"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Angle" to "%.2f°".format(angleDeg),
            "P1" to "(%.1f, %.1f)".format(p1.x, p1.y),
            "P2" to "(%.1f, %.1f)".format(p2.x, p2.y)
        )
    }

    /**
     * 3. Horizontal Distance
     */
    data class HorizontalDistance(
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val deltaX: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.LinearDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.HORIZONTAL_DISTANCE
        override val primaryFormatted: String get() = "ΔX: ${unit.format(deltaX)}"
        override val displayArea: String? get() = null
        override val displayPerimeter: String? get() = null
        override val displayTotalLength: String get() = unit.format(abs(deltaX))
        override val displayObjectCount: String get() = "1 horizontal span"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "X1" to unit.format(p1.x.toDouble()),
            "X2" to unit.format(p2.x.toDouble()),
            "Absolute ΔX" to unit.format(abs(deltaX))
        )
    }

    /**
     * 4. Vertical Distance
     */
    data class VerticalDistance(
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val deltaY: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.LinearDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.VERTICAL_DISTANCE
        override val primaryFormatted: String get() = "ΔY: ${unit.format(deltaY)}"
        override val displayArea: String? get() = null
        override val displayPerimeter: String? get() = null
        override val displayTotalLength: String get() = unit.format(abs(deltaY))
        override val displayObjectCount: String get() = "1 vertical span"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Y1" to unit.format(p1.y.toDouble()),
            "Y2" to unit.format(p2.y.toDouble()),
            "Absolute ΔY" to unit.format(abs(deltaY))
        )
    }

    /**
     * 5. Angle
     */
    data class Angle(
        val vertex: CadPoint2D,
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val degrees: Double,
        val radians: Double,
        val supplementaryDeg: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.AngularDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.ANGLE
        override val primaryFormatted: String get() = "%.2f°".format(degrees)
        override val displayArea: String? get() = null
        override val displayPerimeter: String? get() = null
        override val displayTotalLength: String? get() = null
        override val displayObjectCount: String get() = "1 vertex, 2 rays"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Radians" to "%.4f rad".format(radians),
            "Supplementary" to "%.2f°".format(supplementaryDeg),
            "Vertex" to "(%.1f, %.1f)".format(vertex.x, vertex.y)
        )
    }

    /**
     * 6. Radius
     */
    data class Radius(
        val center: CadPoint2D,
        val rimPoint: CadPoint2D,
        val radius: Double,
        val diameter: Double,
        val circumference: Double,
        val areaSquareUnits: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.RadialDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.RADIUS
        override val primaryFormatted: String get() = "R ${unit.format(radius)}"
        override val displayArea: String get() = unit.formatArea(areaSquareUnits)
        override val displayPerimeter: String get() = unit.format(circumference)
        override val displayTotalLength: String get() = unit.format(circumference)
        override val displayObjectCount: String get() = "1 radial curve"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Diameter" to "Ø ${unit.format(diameter)}",
            "Circumference" to unit.format(circumference),
            "Area" to unit.formatArea(areaSquareUnits),
            "Center" to "(%.1f, %.1f)".format(center.x, center.y)
        )
    }

    /**
     * 7. Diameter
     */
    data class Diameter(
        val center: CadPoint2D,
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val diameter: Double,
        val radius: Double,
        val circumference: Double,
        val areaSquareUnits: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.RadialDimension? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.DIAMETER
        override val primaryFormatted: String get() = "Ø ${unit.format(diameter)}"
        override val displayArea: String get() = unit.formatArea(areaSquareUnits)
        override val displayPerimeter: String get() = unit.format(circumference)
        override val displayTotalLength: String get() = unit.format(circumference)
        override val displayObjectCount: String get() = "1 circle/arc"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Radius" to "R ${unit.format(radius)}",
            "Circumference" to unit.format(circumference),
            "Area" to unit.formatArea(areaSquareUnits),
            "Center" to "(%.1f, %.1f)".format(center.x, center.y)
        )
    }

    /**
     * 8. Area (Polygon Area & Closed Polyline Area)
     */
    data class Area(
        val points: List<CadPoint2D>,
        val areaSquareUnits: Double,
        val perimeter: Double,
        val isClosedPolyline: Boolean = false,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.PolygonArea? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.AREA
        override val primaryFormatted: String get() = unit.formatArea(areaSquareUnits)
        override val displayArea: String get() = unit.formatArea(areaSquareUnits)
        override val displayPerimeter: String get() = unit.format(perimeter)
        override val displayTotalLength: String get() = unit.format(perimeter)
        override val displayObjectCount: String
            get() = if (isClosedPolyline) "1 closed polyline (${points.size} vertices)"
                    else "1 polygon (${points.size} vertices)"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Perimeter" to unit.format(perimeter),
            "Vertices" to points.size.toString(),
            "Shape" to if (isClosedPolyline) "Closed Polyline" else "Polygon"
        )
    }

    /**
     * 9. Perimeter
     */
    data class Perimeter(
        val points: List<CadPoint2D>,
        val perimeter: Double,
        val segmentCount: Int,
        val enclosedArea: Double = 0.0,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.PolygonArea? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.PERIMETER
        override val primaryFormatted: String get() = unit.format(perimeter)
        override val displayArea: String get() = if (enclosedArea > 0) unit.formatArea(enclosedArea) else "—"
        override val displayPerimeter: String get() = unit.format(perimeter)
        override val displayTotalLength: String get() = unit.format(perimeter)
        override val displayObjectCount: String get() = "$segmentCount segments (1 closed loop)"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Segments" to segmentCount.toString(),
            "Avg Segment" to if (segmentCount > 0) unit.format(perimeter / segmentCount) else "-",
            "Enclosed Area" to if (enclosedArea > 0) unit.formatArea(enclosedArea) else "-"
        )
    }

    /**
     * 10. Coordinate
     */
    data class Coordinate(
        val point: CadPoint2D,
        val xInUnit: Double,
        val yInUnit: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.CoordinateCallout? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.COORDINATE
        override val primaryFormatted: String get() = "X: ${unit.format(xInUnit)}, Y: ${unit.format(yInUnit)}"
        override val displayArea: String? get() = null
        override val displayPerimeter: String? get() = null
        override val displayTotalLength: String? get() = null
        override val displayObjectCount: String get() = "1 world coordinate"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "X" to unit.format(xInUnit),
            "Y" to unit.format(yInUnit),
            "Raw World" to "(%.2f, %.2f)".format(point.x, point.y)
        )
    }

    /**
     * 11. Polyline Length
     */
    data class PolylineLength(
        val points: List<CadPoint2D>,
        val totalLength: Double,
        val segmentLengths: List<Double>,
        val isClosed: Boolean = false,
        val enclosedArea: Double = 0.0,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.PolylinePath? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.POLYLINE_LENGTH
        override val primaryFormatted: String get() = unit.format(totalLength)
        override val displayArea: String? get() = if (isClosed && enclosedArea > 0) unit.formatArea(enclosedArea) else null
        override val displayPerimeter: String? get() = if (isClosed) unit.format(totalLength) else null
        override val displayTotalLength: String get() = unit.format(totalLength)
        override val displayObjectCount: String get() = "1 polyline (${segmentLengths.size} segments)"
        override val secondaryDetails: List<Pair<String, String>> get() = buildList {
            add("Segments" to segmentLengths.size.toString())
            add("Avg Segment" to if (segmentLengths.isNotEmpty()) unit.format(totalLength / segmentLengths.size) else "-")
            if (isClosed) {
                add("Closed Loop" to "Yes")
                if (enclosedArea > 0) add("Enclosed Area" to unit.formatArea(enclosedArea))
            }
        }
    }

    /**
     * 12. Bounding Box
     */
    data class BoundingBox(
        val box: CadBoundingBox,
        val width: Double,
        val height: Double,
        val diagonal: Double,
        val area: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.BoundingBoxEnvelope? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.BOUNDING_BOX
        override val primaryFormatted: String get() = "${unit.format(width)} × ${unit.format(height)}"
        override val displayArea: String get() = unit.formatArea(area)
        override val displayPerimeter: String get() = unit.format(2 * (width + height))
        override val displayTotalLength: String get() = unit.format(2 * (width + height))
        override val displayObjectCount: String get() = "1 envelope (4 bounds)"
        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Width" to unit.format(width),
            "Height" to unit.format(height),
            "Diagonal" to unit.format(diagonal),
            "Area" to unit.formatArea(area),
            "Perimeter" to unit.format(2 * (width + height)),
            "Min (X,Y)" to "(%.1f, %.1f)".format(box.minX, box.minY),
            "Max (X,Y)" to "(%.1f, %.1f)".format(box.maxX, box.maxY)
        )
    }

    /**
     * Individual Region Model for Multi-Area and Add/Subtract calculations.
     */
    data class CadAreaRegion(
        val id: String,
        val name: String,
        val points: List<CadPoint2D>,
        val isSubtract: Boolean = false,
        val areaSquareUnits: Double,
        val perimeter: Double
    )

    /**
     * 13. Multiple-Area Measurement with Add & Subtract Regions.
     */
    data class MultiArea(
        val regions: List<CadAreaRegion>,
        val grossAddedArea: Double,
        val subtractedArea: Double,
        val netArea: Double,
        val totalPerimeter: Double,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.MultiRegionArea? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.MULTI_AREA
        override val primaryFormatted: String get() = "Net: ${unit.formatArea(netArea)}"
        override val displayArea: String get() = unit.formatArea(netArea)
        override val displayPerimeter: String get() = unit.format(totalPerimeter)
        override val displayTotalLength: String get() = unit.format(totalPerimeter)
        override val displayObjectCount: String
            get() = "${regions.size} regions (${regions.count { !it.isSubtract }} Added, ${regions.count { it.isSubtract }} Subtracted)"

        override val secondaryDetails: List<Pair<String, String>> get() = listOf(
            "Gross Area" to unit.formatArea(grossAddedArea),
            "Subtracted Area" to "-${unit.formatArea(subtractedArea)}",
            "Net Area" to unit.formatArea(netArea),
            "Total Perimeter" to unit.format(totalPerimeter),
            "Region Count" to "${regions.size} (${regions.count { !it.isSubtract }} Added, ${regions.count { it.isSubtract }} Subtracted)"
        )
    }

    /**
     * 14. Selection Summary: Total Selected Area, Length of Multiple Selected Lines/Polylines, Quantity Count.
     */
    data class SelectionSummary(
        val selectedEntities: List<CadEntity>,
        val totalAreaSquareUnits: Double,
        val totalPerimeter: Double,
        val totalLength: Double,
        val objectCount: Int,
        val countByType: Map<String, Int>,
        override val unit: CadUnit,
        override val annotation: CadMeasurementAnnotation.SelectionEnvelope? = null
    ) : CadMeasurementResult() {
        override val type: CadMeasurementType get() = CadMeasurementType.SELECTION_SUMMARY
        override val primaryFormatted: String
            get() = "$objectCount items • ${unit.formatArea(totalAreaSquareUnits)}"
        override val displayArea: String get() = unit.formatArea(totalAreaSquareUnits)
        override val displayPerimeter: String get() = unit.format(totalPerimeter)
        override val displayTotalLength: String get() = unit.format(totalLength)
        override val displayObjectCount: String
            get() = "$objectCount items (${countByType.entries.joinToString { "${it.value} ${it.key}" }})"

        override val secondaryDetails: List<Pair<String, String>> get() = buildList {
            add("Total Area" to unit.formatArea(totalAreaSquareUnits))
            add("Total Perimeter" to unit.format(totalPerimeter))
            add("Total Length" to unit.format(totalLength))
            add("Object Count" to "$objectCount items")
            for ((typeName, count) in countByType) {
                add(typeName to "$count item(s)")
            }
        }
    }
}

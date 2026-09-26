package com.example.cad.engine.measurement

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadPoint2D

/**
 * Geometric annotation elements in CAD World Coordinates to be rendered on the drawing canvas.
 */
sealed class CadMeasurementAnnotation {

    /**
     * Standard linear dimension line with extension lines, arrowheads/ticks, and centered label.
     */
    data class LinearDimension(
        val p1: CadPoint2D,
        val p2: CadPoint2D,
        val dimStart: CadPoint2D,
        val dimEnd: CadPoint2D,
        val ext1Start: CadPoint2D = p1,
        val ext1End: CadPoint2D = dimStart,
        val ext2Start: CadPoint2D = p2,
        val ext2End: CadPoint2D = dimEnd,
        val text: String,
        val textPos: CadPoint2D,
        val textAngleDeg: Float = 0f,
        val isHorizontal: Boolean = false,
        val isVertical: Boolean = false
    ) : CadMeasurementAnnotation()

    /**
     * Angular dimension arc with ray lines, arrowheads, and centered angle label.
     */
    data class AngularDimension(
        val vertex: CadPoint2D,
        val ray1End: CadPoint2D,
        val ray2End: CadPoint2D,
        val arcRadius: Float,
        val startAngleDeg: Float,
        val sweepAngleDeg: Float,
        val text: String,
        val textPos: CadPoint2D
    ) : CadMeasurementAnnotation()

    /**
     * Radial or diametrical dimension with center mark and leader line.
     */
    data class RadialDimension(
        val center: CadPoint2D,
        val rimPoint: CadPoint2D,
        val leaderEnd: CadPoint2D,
        val text: String,
        val isDiameter: Boolean,
        val oppositeRimPoint: CadPoint2D? = null
    ) : CadMeasurementAnnotation()

    /**
     * Coordinate target crosshair with leader line and callout box.
     */
    data class CoordinateCallout(
        val point: CadPoint2D,
        val leaderEnd: CadPoint2D,
        val line1: String, // e.g. "X: 1250.00 mm"
        val line2: String  // e.g. "Y: 820.00 mm"
    ) : CadMeasurementAnnotation()

    /**
     * Surface area and perimeter polygon with shaded fill, contour outline, and centroid label.
     */
    data class PolygonArea(
        val vertices: List<CadPoint2D>,
        val centroid: CadPoint2D,
        val areaText: String,
        val perimeterText: String,
        val segmentLengths: List<Pair<CadPoint2D, String>> = emptyList(),
        val isClosed: Boolean = true,
        val isSubtract: Boolean = false
    ) : CadMeasurementAnnotation()

    /**
     * Individual region annotation for composite multi-area measurements.
     */
    data class RegionAnnotation(
        val id: String,
        val name: String,
        val vertices: List<CadPoint2D>,
        val centroid: CadPoint2D,
        val isSubtract: Boolean,
        val areaText: String,
        val perimeterText: String
    )

    /**
     * Composite multiple area measurement with Add/Subtract regions.
     */
    data class MultiRegionArea(
        val regions: List<RegionAnnotation>,
        val netAreaText: String,
        val grossAreaText: String,
        val voidAreaText: String,
        val totalPerimeterText: String,
        val overallCentroid: CadPoint2D
    ) : CadMeasurementAnnotation()

    /**
     * Enclosing summary envelope for multi-selected CAD objects.
     */
    data class SelectionEnvelope(
        val box: CadBoundingBox,
        val countText: String,
        val areaText: String,
        val lengthText: String,
        val perimeterText: String
    ) : CadMeasurementAnnotation()

    /**
     * Polyline cumulative distance path with segment tags and total label.
     */
    data class PolylinePath(
        val points: List<CadPoint2D>,
        val totalLengthText: String,
        val segmentLengths: List<Pair<CadPoint2D, String>>
    ) : CadMeasurementAnnotation()

    /**
     * Orthogonal bounding box envelope with width, height, and diagonal dimension marks.
     */
    data class BoundingBoxEnvelope(
        val box: CadBoundingBox,
        val widthText: String,
        val heightText: String,
        val diagonalText: String,
        val areaText: String
    ) : CadMeasurementAnnotation()
}

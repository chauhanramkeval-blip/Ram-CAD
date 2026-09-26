package com.example.cad.engine.measurement

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Dedicated CAD Calculation Engine independent of the renderer.
 *
 * Implements high-precision world coordinate calculations for all 12 CAD measurement tools,
 * unit transformations (metric: mm, cm, m; imperial: in, ft), and geometric annotation generation.
 */
interface CadMeasurementEngine {
    fun measureDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit): CadMeasurementResult.Distance
    fun measureAlignedDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit, offsetDistance: Float = 20f): CadMeasurementResult.AlignedDistance
    fun measureHorizontalDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit, offsetDistance: Float = 20f): CadMeasurementResult.HorizontalDistance
    fun measureVerticalDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit, offsetDistance: Float = 20f): CadMeasurementResult.VerticalDistance
    fun measureAngle(vertex: CadPoint2D, p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit = CadUnit.MILLIMETERS): CadMeasurementResult.Angle
    fun measureRadius(center: CadPoint2D, rimPoint: CadPoint2D, unit: CadUnit): CadMeasurementResult.Radius
    fun measureDiameter(center: CadPoint2D, p1: CadPoint2D, p2: CadPoint2D? = null, unit: CadUnit): CadMeasurementResult.Diameter
    fun measureArea(points: List<CadPoint2D>, unit: CadUnit): CadMeasurementResult.Area
    fun measureClosedPolylineArea(polyline: CadEntity.Polyline, unit: CadUnit): CadMeasurementResult.Area
    fun measureMultipleAreas(regions: List<CadMeasurementResult.CadAreaRegion>, unit: CadUnit): CadMeasurementResult.MultiArea
    fun measureSelectedEntities(entities: List<CadEntity>, unit: CadUnit): CadMeasurementResult.SelectionSummary
    fun measurePerimeter(points: List<CadPoint2D>, unit: CadUnit): CadMeasurementResult.Perimeter
    fun measureCoordinate(point: CadPoint2D, unit: CadUnit): CadMeasurementResult.Coordinate
    fun measurePolylineLength(points: List<CadPoint2D>, unit: CadUnit): CadMeasurementResult.PolylineLength
    fun measureBoundingBox(box: CadBoundingBox, unit: CadUnit): CadMeasurementResult.BoundingBox
    fun measureBoundingBoxFromPoints(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit): CadMeasurementResult.BoundingBox

    /**
     * Dynamically calculates measurement based on active tool mode and current point stack.
     * If [previewPoint] is provided, it acts as a dynamic live cursor point.
     */
    fun calculate(
        type: CadMeasurementType,
        points: List<CadPoint2D>,
        unit: CadUnit,
        previewPoint: CadPoint2D? = null
    ): CadMeasurementResult?

    /**
     * Direct one-tap measurement of existing CAD entities.
     */
    fun measureEntity(entity: CadEntity, type: CadMeasurementType, unit: CadUnit): CadMeasurementResult?
}

/**
 * Standard pure-Kotlin reference implementation of [CadMeasurementEngine].
 */
class StandardCadMeasurementEngine : CadMeasurementEngine {

    override fun measureDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit): CadMeasurementResult.Distance {
        val dxMm = (p2.x - p1.x).toDouble()
        val dyMm = (p2.y - p1.y).toDouble()
        val distMm = hypot(dxMm, dyMm)
        val distInUnit = unit.convertFromMm(distMm)
        val dxInUnit = unit.convertFromMm(dxMm)
        val dyInUnit = unit.convertFromMm(dyMm)

        val angleRad = atan2(dyMm, dxMm)
        var angleDeg = Math.toDegrees(angleRad)
        if (angleDeg < 0) angleDeg += 360.0

        val midWorld = CadPoint2D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

        // Generate linear dimension annotation
        val text = unit.format(distInUnit)
        val annotation = CadMeasurementAnnotation.LinearDimension(
            p1 = p1,
            p2 = p2,
            dimStart = p1,
            dimEnd = p2,
            text = text,
            textPos = midWorld,
            textAngleDeg = angleDeg.toFloat()
        )

        return CadMeasurementResult.Distance(
            p1 = p1,
            p2 = p2,
            distance = distInUnit,
            deltaX = dxInUnit,
            deltaY = dyInUnit,
            angleDeg = angleDeg,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureAlignedDistance(
        p1: CadPoint2D,
        p2: CadPoint2D,
        unit: CadUnit,
        offsetDistance: Float
    ): CadMeasurementResult.AlignedDistance {
        val dxMm = (p2.x - p1.x).toDouble()
        val dyMm = (p2.y - p1.y).toDouble()
        val distMm = hypot(dxMm, dyMm)
        val distInUnit = unit.convertFromMm(distMm)

        val angleRad = atan2(dyMm, dxMm)
        var angleDeg = Math.toDegrees(angleRad)
        if (angleDeg < 0) angleDeg += 360.0

        // Normal vector perpendicular to p1-p2
        val normalX = if (distMm > 1e-6) (-dyMm / distMm).toFloat() else 0f
        val normalY = if (distMm > 1e-6) (dxMm / distMm).toFloat() else 1f

        val effectiveOffset = if (offsetDistance == 0f) 25f else offsetDistance
        val dimStart = CadPoint2D(p1.x + normalX * effectiveOffset, p1.y + normalY * effectiveOffset)
        val dimEnd = CadPoint2D(p2.x + normalX * effectiveOffset, p2.y + normalY * effectiveOffset)
        val midWorld = CadPoint2D((dimStart.x + dimEnd.x) / 2f, (dimStart.y + dimEnd.y) / 2f)

        val annotation = CadMeasurementAnnotation.LinearDimension(
            p1 = p1,
            p2 = p2,
            dimStart = dimStart,
            dimEnd = dimEnd,
            ext1Start = p1,
            ext1End = dimStart,
            ext2Start = p2,
            ext2End = dimEnd,
            text = unit.format(distInUnit),
            textPos = midWorld,
            textAngleDeg = angleDeg.toFloat()
        )

        return CadMeasurementResult.AlignedDistance(
            p1 = p1,
            p2 = p2,
            distance = distInUnit,
            angleDeg = angleDeg,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureHorizontalDistance(
        p1: CadPoint2D,
        p2: CadPoint2D,
        unit: CadUnit,
        offsetDistance: Float
    ): CadMeasurementResult.HorizontalDistance {
        val dxMm = (p2.x - p1.x).toDouble()
        val deltaX = unit.convertFromMm(dxMm)

        val yMin = minOf(p1.y, p2.y)
        val yMax = maxOf(p1.y, p2.y)
        val dimY = yMax + (if (offsetDistance == 0f) 20f else offsetDistance)

        val dimStart = CadPoint2D(p1.x, dimY)
        val dimEnd = CadPoint2D(p2.x, dimY)
        val textPos = CadPoint2D((p1.x + p2.x) / 2f, dimY + 4f)

        val annotation = CadMeasurementAnnotation.LinearDimension(
            p1 = p1,
            p2 = p2,
            dimStart = dimStart,
            dimEnd = dimEnd,
            ext1Start = p1,
            ext1End = dimStart,
            ext2Start = p2,
            ext2End = dimEnd,
            text = "ΔX: ${unit.format(abs(deltaX))}",
            textPos = textPos,
            isHorizontal = true
        )

        return CadMeasurementResult.HorizontalDistance(
            p1 = p1,
            p2 = p2,
            deltaX = deltaX,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureVerticalDistance(
        p1: CadPoint2D,
        p2: CadPoint2D,
        unit: CadUnit,
        offsetDistance: Float
    ): CadMeasurementResult.VerticalDistance {
        val dyMm = (p2.y - p1.y).toDouble()
        val deltaY = unit.convertFromMm(dyMm)

        val xMax = maxOf(p1.x, p2.x)
        val dimX = xMax + (if (offsetDistance == 0f) 20f else offsetDistance)

        val dimStart = CadPoint2D(dimX, p1.y)
        val dimEnd = CadPoint2D(dimX, p2.y)
        val textPos = CadPoint2D(dimX + 4f, (p1.y + p2.y) / 2f)

        val annotation = CadMeasurementAnnotation.LinearDimension(
            p1 = p1,
            p2 = p2,
            dimStart = dimStart,
            dimEnd = dimEnd,
            ext1Start = p1,
            ext1End = dimStart,
            ext2Start = p2,
            ext2End = dimEnd,
            text = "ΔY: ${unit.format(abs(deltaY))}",
            textPos = textPos,
            isVertical = true
        )

        return CadMeasurementResult.VerticalDistance(
            p1 = p1,
            p2 = p2,
            deltaY = deltaY,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureAngle(
        vertex: CadPoint2D,
        p1: CadPoint2D,
        p2: CadPoint2D,
        unit: CadUnit
    ): CadMeasurementResult.Angle {
        val v1x = (p1.x - vertex.x).toDouble()
        val v1y = (p1.y - vertex.y).toDouble()
        val v2x = (p2.x - vertex.x).toDouble()
        val v2y = (p2.y - vertex.y).toDouble()

        val a1 = atan2(v1y, v1x)
        val a2 = atan2(v2y, v2x)

        var diffRad = abs(a2 - a1)
        if (diffRad > Math.PI) {
            diffRad = 2 * Math.PI - diffRad
        }
        val degrees = Math.toDegrees(diffRad)
        val supplementaryDeg = 180.0 - degrees

        val dist1 = hypot(v1x, v1y).toFloat()
        val dist2 = hypot(v2x, v2y).toFloat()
        val arcRadius = minOf(dist1, dist2).coerceAtLeast(15f) * 0.6f

        val startDeg = Math.toDegrees(minOf(a1, a2)).toFloat()
        val sweepDeg = degrees.toFloat()

        val midAngleRad = a1 + (a2 - a1) / 2.0
        val textPos = CadPoint2D(
            x = vertex.x + (cos(midAngleRad) * arcRadius * 1.3).toFloat(),
            y = vertex.y + (sin(midAngleRad) * arcRadius * 1.3).toFloat()
        )

        val annotation = CadMeasurementAnnotation.AngularDimension(
            vertex = vertex,
            ray1End = p1,
            ray2End = p2,
            arcRadius = arcRadius,
            startAngleDeg = startDeg,
            sweepAngleDeg = sweepDeg,
            text = "%.2f°".format(degrees),
            textPos = textPos
        )

        return CadMeasurementResult.Angle(
            vertex = vertex,
            p1 = p1,
            p2 = p2,
            degrees = degrees,
            radians = diffRad,
            supplementaryDeg = supplementaryDeg,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureRadius(
        center: CadPoint2D,
        rimPoint: CadPoint2D,
        unit: CadUnit
    ): CadMeasurementResult.Radius {
        val dxMm = (rimPoint.x - center.x).toDouble()
        val dyMm = (rimPoint.y - center.y).toDouble()
        val radiusMm = hypot(dxMm, dyMm)
        val radiusInUnit = unit.convertFromMm(radiusMm)
        val diameterInUnit = radiusInUnit * 2.0
        val circumferenceInUnit = 2.0 * Math.PI * radiusInUnit
        val areaMm2 = Math.PI * radiusMm * radiusMm
        val areaInUnit = unit.convertAreaFromMm2(areaMm2)

        val dirX = if (radiusMm > 1e-6) (dxMm / radiusMm).toFloat() else 1f
        val dirY = if (radiusMm > 1e-6) (dyMm / radiusMm).toFloat() else 0f
        val leaderEnd = CadPoint2D(rimPoint.x + dirX * 25f, rimPoint.y + dirY * 25f)

        val annotation = CadMeasurementAnnotation.RadialDimension(
            center = center,
            rimPoint = rimPoint,
            leaderEnd = leaderEnd,
            text = "R ${unit.format(radiusInUnit)}",
            isDiameter = false
        )

        return CadMeasurementResult.Radius(
            center = center,
            rimPoint = rimPoint,
            radius = radiusInUnit,
            diameter = diameterInUnit,
            circumference = circumferenceInUnit,
            areaSquareUnits = areaInUnit,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureDiameter(
        center: CadPoint2D,
        p1: CadPoint2D,
        p2: CadPoint2D?,
        unit: CadUnit
    ): CadMeasurementResult.Diameter {
        val dxMm = (p1.x - center.x).toDouble()
        val dyMm = (p1.y - center.y).toDouble()
        val radiusMm = hypot(dxMm, dyMm)
        val diameterMm = radiusMm * 2.0
        val diameterInUnit = unit.convertFromMm(diameterMm)
        val radiusInUnit = unit.convertFromMm(radiusMm)
        val circumferenceInUnit = Math.PI * diameterInUnit
        val areaMm2 = Math.PI * radiusMm * radiusMm
        val areaInUnit = unit.convertAreaFromMm2(areaMm2)

        val opposite = p2 ?: CadPoint2D(
            x = center.x - (p1.x - center.x),
            y = center.y - (p1.y - center.y)
        )

        val dirX = if (radiusMm > 1e-6) (dxMm / radiusMm).toFloat() else 1f
        val dirY = if (radiusMm > 1e-6) (dyMm / radiusMm).toFloat() else 0f
        val leaderEnd = CadPoint2D(p1.x + dirX * 25f, p1.y + dirY * 25f)

        val annotation = CadMeasurementAnnotation.RadialDimension(
            center = center,
            rimPoint = p1,
            leaderEnd = leaderEnd,
            text = "Ø ${unit.format(diameterInUnit)}",
            isDiameter = true,
            oppositeRimPoint = opposite
        )

        return CadMeasurementResult.Diameter(
            center = center,
            p1 = p1,
            p2 = opposite,
            diameter = diameterInUnit,
            radius = radiusInUnit,
            circumference = circumferenceInUnit,
            areaSquareUnits = areaInUnit,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureArea(points: List<CadPoint2D>, unit: CadUnit): CadMeasurementResult.Area {
        if (points.size < 3) {
            return CadMeasurementResult.Area(points, 0.0, 0.0, isClosedPolyline = false, unit = unit, annotation = null)
        }

        // Surveyor's Shoelace formula
        var sum = 0.0
        var perimeterMm = 0.0
        val n = points.size
        var cx = 0f
        var cy = 0f

        val segmentLengths = mutableListOf<Pair<CadPoint2D, String>>()

        for (i in 0 until n) {
            val j = (i + 1) % n
            sum += (points[i].x * points[j].y - points[j].x * points[i].y).toDouble()
            val segDist = points[i].distanceTo(points[j]).toDouble()
            perimeterMm += segDist
            cx += points[i].x
            cy += points[i].y

            val mid = CadPoint2D((points[i].x + points[j].x) / 2f, (points[i].y + points[j].y) / 2f)
            segmentLengths.add(mid to unit.format(unit.convertFromMm(segDist)))
        }

        val areaMm2 = abs(sum) / 2.0
        val areaInUnit = unit.convertAreaFromMm2(areaMm2)
        val perimeterInUnit = unit.convertFromMm(perimeterMm)

        val centroid = CadPoint2D(cx / n, cy / n)

        val annotation = CadMeasurementAnnotation.PolygonArea(
            vertices = points,
            centroid = centroid,
            areaText = unit.formatArea(areaInUnit),
            perimeterText = unit.format(perimeterInUnit),
            segmentLengths = segmentLengths,
            isClosed = true
        )

        return CadMeasurementResult.Area(
            points = points,
            areaSquareUnits = areaInUnit,
            perimeter = perimeterInUnit,
            isClosedPolyline = false,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureClosedPolylineArea(polyline: CadEntity.Polyline, unit: CadUnit): CadMeasurementResult.Area {
        val points = polyline.points
        if (points.size < 3) {
            return CadMeasurementResult.Area(points, 0.0, 0.0, isClosedPolyline = true, unit = unit, annotation = null)
        }
        val base = measureArea(points, unit)
        return base.copy(isClosedPolyline = true)
    }

    override fun measureMultipleAreas(
        regions: List<CadMeasurementResult.CadAreaRegion>,
        unit: CadUnit
    ): CadMeasurementResult.MultiArea {
        val grossAdded = regions.filter { !it.isSubtract }.sumOf { it.areaSquareUnits }
        val subtracted = regions.filter { it.isSubtract }.sumOf { it.areaSquareUnits }
        val net = maxOf(0.0, grossAdded - subtracted)
        val totalPerim = regions.sumOf { it.perimeter }

        val regionAnnotations = regions.map { r ->
            var cx = 0f
            var cy = 0f
            if (r.points.isNotEmpty()) {
                r.points.forEach { cx += it.x; cy += it.y }
                cx /= r.points.size
                cy /= r.points.size
            }
            CadMeasurementAnnotation.RegionAnnotation(
                id = r.id,
                name = r.name,
                vertices = r.points,
                centroid = CadPoint2D(cx, cy),
                isSubtract = r.isSubtract,
                areaText = unit.formatArea(r.areaSquareUnits),
                perimeterText = unit.format(r.perimeter)
            )
        }

        var overallX = 0f
        var overallY = 0f
        var ptCount = 0
        regions.forEach { r ->
            r.points.forEach { pt ->
                overallX += pt.x
                overallY += pt.y
                ptCount++
            }
        }
        val overallCenter = if (ptCount > 0) CadPoint2D(overallX / ptCount, overallY / ptCount) else CadPoint2D(0f, 0f)

        val annotation = CadMeasurementAnnotation.MultiRegionArea(
            regions = regionAnnotations,
            netAreaText = unit.formatArea(net),
            grossAreaText = unit.formatArea(grossAdded),
            voidAreaText = unit.formatArea(subtracted),
            totalPerimeterText = unit.format(totalPerim),
            overallCentroid = overallCenter
        )

        return CadMeasurementResult.MultiArea(
            regions = regions,
            grossAddedArea = grossAdded,
            subtractedArea = subtracted,
            netArea = net,
            totalPerimeter = totalPerim,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureSelectedEntities(
        entities: List<CadEntity>,
        unit: CadUnit
    ): CadMeasurementResult.SelectionSummary {
        var totalLengthMm = 0.0
        var totalAreaMm2 = 0.0
        var totalPerimeterMm = 0.0
        val countsByType = mutableMapOf<String, Int>()
        var combinedBox = CadBoundingBox()

        for (e in entities) {
            combinedBox = combinedBox.include(e.boundingBox)
            val typeName = when (e) {
                is CadEntity.Line -> "Line"
                is CadEntity.Polyline -> if (e.isClosed) "Closed Polyline" else "Polyline"
                is CadEntity.Circle -> "Circle"
                is CadEntity.Arc -> "Arc"
                is CadEntity.Dimension -> "Dimension"
                is CadEntity.Text -> "Text"
                is CadEntity.Point -> "Point"
                is CadEntity.Leader -> "Leader"
                is CadEntity.Arrow -> "Arrow"
                is CadEntity.RevisionCloud -> "Revision Cloud"
            }
            countsByType[typeName] = (countsByType[typeName] ?: 0) + 1

            when (e) {
                is CadEntity.Line -> {
                    val len = hypot((e.end.x - e.start.x).toDouble(), (e.end.y - e.start.y).toDouble())
                    totalLengthMm += len
                }
                is CadEntity.Polyline -> {
                    var polyLenMm = 0.0
                    for (i in 0 until e.points.size - 1) {
                        polyLenMm += hypot((e.points[i + 1].x - e.points[i].x).toDouble(), (e.points[i + 1].y - e.points[i].y).toDouble())
                    }
                    if (e.isClosed && e.points.size >= 3) {
                        polyLenMm += hypot((e.points.first().x - e.points.last().x).toDouble(), (e.points.first().y - e.points.last().y).toDouble())
                        // Closed polyline area via Shoelace
                        var sum = 0.0
                        val n = e.points.size
                        for (i in 0 until n) {
                            val j = (i + 1) % n
                            sum += (e.points[i].x * e.points[j].y - e.points[j].x * e.points[i].y).toDouble()
                        }
                        totalAreaMm2 += abs(sum) / 2.0
                        totalPerimeterMm += polyLenMm
                    }
                    totalLengthMm += polyLenMm
                }
                is CadEntity.Circle -> {
                    val circMm = 2.0 * Math.PI * e.radius.toDouble()
                    val areaMm = Math.PI * e.radius.toDouble() * e.radius.toDouble()
                    totalLengthMm += circMm
                    totalPerimeterMm += circMm
                    totalAreaMm2 += areaMm
                }
                is CadEntity.Arc -> {
                    val sweepRad = Math.toRadians(abs(e.sweepAngleDeg.toDouble()))
                    val arcLenMm = e.radius.toDouble() * sweepRad
                    totalLengthMm += arcLenMm
                }
                is CadEntity.Dimension -> {
                    val len = hypot((e.end.x - e.start.x).toDouble(), (e.end.y - e.start.y).toDouble())
                    totalLengthMm += len
                }
                is CadEntity.Leader -> {
                    val len = hypot((e.kneePoint.x - e.arrowPoint.x).toDouble(), (e.kneePoint.y - e.arrowPoint.y).toDouble()) +
                            hypot((e.landingEndPoint.x - e.kneePoint.x).toDouble(), (e.landingEndPoint.y - e.kneePoint.y).toDouble())
                    totalLengthMm += len
                }
                is CadEntity.Arrow -> {
                    val len = hypot((e.end.x - e.start.x).toDouble(), (e.end.y - e.start.y).toDouble())
                    totalLengthMm += len
                }
                is CadEntity.RevisionCloud -> {
                    var cloudLen = 0.0
                    for (i in 0 until e.vertices.size - 1) {
                        cloudLen += hypot((e.vertices[i + 1].x - e.vertices[i].x).toDouble(), (e.vertices[i + 1].y - e.vertices[i].y).toDouble())
                    }
                    if (e.isClosed && e.vertices.size >= 3) {
                        cloudLen += hypot((e.vertices.first().x - e.vertices.last().x).toDouble(), (e.vertices.first().y - e.vertices.last().y).toDouble())
                        var sum = 0.0
                        val n = e.vertices.size
                        for (i in 0 until n) {
                            val j = (i + 1) % n
                            sum += (e.vertices[i].x * e.vertices[j].y - e.vertices[j].x * e.vertices[i].y).toDouble()
                        }
                        totalAreaMm2 += abs(sum) / 2.0
                        totalPerimeterMm += cloudLen
                    }
                    totalLengthMm += cloudLen
                }
                is CadEntity.Text, is CadEntity.Point -> {
                    // Non-linear
                }
            }
        }

        val totalLengthInUnit = unit.convertFromMm(totalLengthMm)
        val totalAreaInUnit = unit.convertAreaFromMm2(totalAreaMm2)
        val totalPerimInUnit = unit.convertFromMm(totalPerimeterMm)

        val annotation = CadMeasurementAnnotation.SelectionEnvelope(
            box = combinedBox,
            countText = "${entities.size} items",
            areaText = unit.formatArea(totalAreaInUnit),
            lengthText = unit.format(totalLengthInUnit),
            perimeterText = unit.format(totalPerimInUnit)
        )

        return CadMeasurementResult.SelectionSummary(
            selectedEntities = entities,
            totalAreaSquareUnits = totalAreaInUnit,
            totalPerimeter = totalPerimInUnit,
            totalLength = totalLengthInUnit,
            objectCount = entities.size,
            countByType = countsByType,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measurePerimeter(points: List<CadPoint2D>, unit: CadUnit): CadMeasurementResult.Perimeter {
        if (points.size < 2) {
            return CadMeasurementResult.Perimeter(points, 0.0, 0, 0.0, unit, null)
        }

        var perimeterMm = 0.0
        var sum = 0.0
        val n = points.size
        var cx = 0f
        var cy = 0f
        val segmentLengths = mutableListOf<Pair<CadPoint2D, String>>()

        for (i in 0 until n) {
            val j = (i + 1) % n
            val segDist = points[i].distanceTo(points[j]).toDouble()
            sum += (points[i].x * points[j].y - points[j].x * points[i].y).toDouble()
            perimeterMm += segDist
            cx += points[i].x
            cy += points[i].y
            val mid = CadPoint2D((points[i].x + points[j].x) / 2f, (points[i].y + points[j].y) / 2f)
            segmentLengths.add(mid to unit.format(unit.convertFromMm(segDist)))
        }

        val perimeterInUnit = unit.convertFromMm(perimeterMm)
        val enclosedAreaInUnit = if (n >= 3) unit.convertAreaFromMm2(abs(sum) / 2.0) else 0.0
        val centroid = CadPoint2D(cx / n, cy / n)

        val annotation = CadMeasurementAnnotation.PolygonArea(
            vertices = points,
            centroid = centroid,
            areaText = if (enclosedAreaInUnit > 0) unit.formatArea(enclosedAreaInUnit) else "",
            perimeterText = "P: ${unit.format(perimeterInUnit)}",
            segmentLengths = segmentLengths,
            isClosed = true
        )

        return CadMeasurementResult.Perimeter(
            points = points,
            perimeter = perimeterInUnit,
            segmentCount = n,
            enclosedArea = enclosedAreaInUnit,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureCoordinate(point: CadPoint2D, unit: CadUnit): CadMeasurementResult.Coordinate {
        val xInUnit = unit.convertFromMm(point.x.toDouble())
        val yInUnit = unit.convertFromMm(point.y.toDouble())

        val leaderEnd = CadPoint2D(point.x + 30f, point.y + 30f)

        val annotation = CadMeasurementAnnotation.CoordinateCallout(
            point = point,
            leaderEnd = leaderEnd,
            line1 = "X: ${unit.format(xInUnit)}",
            line2 = "Y: ${unit.format(yInUnit)}"
        )

        return CadMeasurementResult.Coordinate(
            point = point,
            xInUnit = xInUnit,
            yInUnit = yInUnit,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measurePolylineLength(points: List<CadPoint2D>, unit: CadUnit): CadMeasurementResult.PolylineLength {
        if (points.size < 2) {
            return CadMeasurementResult.PolylineLength(points, 0.0, emptyList(), isClosed = false, enclosedArea = 0.0, unit = unit, annotation = null)
        }

        var totalMm = 0.0
        val segmentLengths = mutableListOf<Double>()
        val annotationSegments = mutableListOf<Pair<CadPoint2D, String>>()

        for (i in 0 until points.size - 1) {
            val dist = points[i].distanceTo(points[i + 1]).toDouble()
            totalMm += dist
            val inUnit = unit.convertFromMm(dist)
            segmentLengths.add(inUnit)
            val mid = CadPoint2D((points[i].x + points[i + 1].x) / 2f, (points[i].y + points[i + 1].y) / 2f)
            annotationSegments.add(mid to unit.format(inUnit))
        }

        // Check if closed (first point equals last point or endpoints are close)
        val isClosed = points.size >= 4 && points.first().distanceTo(points.last()) < 1.0f
        var enclosedAreaInUnit = 0.0
        if (isClosed) {
            var sum = 0.0
            val n = points.size - 1
            for (i in 0 until n) {
                val j = (i + 1) % n
                sum += (points[i].x * points[j].y - points[j].x * points[i].y).toDouble()
            }
            enclosedAreaInUnit = unit.convertAreaFromMm2(abs(sum) / 2.0)
        }

        val totalInUnit = unit.convertFromMm(totalMm)

        val annotation = CadMeasurementAnnotation.PolylinePath(
            points = points,
            totalLengthText = "Total: ${unit.format(totalInUnit)}",
            segmentLengths = annotationSegments
        )

        return CadMeasurementResult.PolylineLength(
            points = points,
            totalLength = totalInUnit,
            segmentLengths = segmentLengths,
            isClosed = isClosed,
            enclosedArea = enclosedAreaInUnit,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureBoundingBox(box: CadBoundingBox, unit: CadUnit): CadMeasurementResult.BoundingBox {
        val widthMm = box.width.toDouble()
        val heightMm = box.height.toDouble()
        val diagMm = hypot(widthMm, heightMm)
        val areaMm2 = widthMm * heightMm

        val widthInUnit = unit.convertFromMm(widthMm)
        val heightInUnit = unit.convertFromMm(heightMm)
        val diagInUnit = unit.convertFromMm(diagMm)
        val areaInUnit = unit.convertAreaFromMm2(areaMm2)

        val annotation = CadMeasurementAnnotation.BoundingBoxEnvelope(
            box = box,
            widthText = "W: ${unit.format(widthInUnit)}",
            heightText = "H: ${unit.format(heightInUnit)}",
            diagonalText = "Diag: ${unit.format(diagInUnit)}",
            areaText = unit.formatArea(areaInUnit)
        )

        return CadMeasurementResult.BoundingBox(
            box = box,
            width = widthInUnit,
            height = heightInUnit,
            diagonal = diagInUnit,
            area = areaInUnit,
            unit = unit,
            annotation = annotation
        )
    }

    override fun measureBoundingBoxFromPoints(
        p1: CadPoint2D,
        p2: CadPoint2D,
        unit: CadUnit
    ): CadMeasurementResult.BoundingBox {
        val minX = minOf(p1.x, p2.x)
        val maxX = maxOf(p1.x, p2.x)
        val minY = minOf(p1.y, p2.y)
        val maxY = maxOf(p1.y, p2.y)
        val box = CadBoundingBox(minX = minX, minY = minY, maxX = maxX, maxY = maxY)
        return measureBoundingBox(box, unit)
    }

    override fun calculate(
        type: CadMeasurementType,
        points: List<CadPoint2D>,
        unit: CadUnit,
        previewPoint: CadPoint2D?
    ): CadMeasurementResult? {
        val effectivePoints = if (previewPoint != null) points + previewPoint else points
        if (effectivePoints.isEmpty()) return null

        return when (type) {
            CadMeasurementType.COORDINATE -> {
                measureCoordinate(effectivePoints.last(), unit)
            }
            CadMeasurementType.DISTANCE -> {
                if (effectivePoints.size >= 2) {
                    measureDistance(effectivePoints[0], effectivePoints[1], unit)
                } else null
            }
            CadMeasurementType.ALIGNED_DISTANCE -> {
                if (effectivePoints.size >= 2) {
                    measureAlignedDistance(effectivePoints[0], effectivePoints[1], unit)
                } else null
            }
            CadMeasurementType.HORIZONTAL_DISTANCE -> {
                if (effectivePoints.size >= 2) {
                    measureHorizontalDistance(effectivePoints[0], effectivePoints[1], unit)
                } else null
            }
            CadMeasurementType.VERTICAL_DISTANCE -> {
                if (effectivePoints.size >= 2) {
                    measureVerticalDistance(effectivePoints[0], effectivePoints[1], unit)
                } else null
            }
            CadMeasurementType.ANGLE -> {
                if (effectivePoints.size >= 3) {
                    measureAngle(effectivePoints[0], effectivePoints[1], effectivePoints[2], unit)
                } else if (effectivePoints.size == 2) {
                    // Preview ray from vertex to live cursor relative to horizontal
                    val vertex = effectivePoints[0]
                    val p = effectivePoints[1]
                    val pHoriz = CadPoint2D(vertex.x + 100f, vertex.y)
                    measureAngle(vertex, pHoriz, p, unit)
                } else null
            }
            CadMeasurementType.RADIUS -> {
                if (effectivePoints.size >= 2) {
                    measureRadius(effectivePoints[0], effectivePoints[1], unit)
                } else null
            }
            CadMeasurementType.DIAMETER -> {
                if (effectivePoints.size >= 2) {
                    measureDiameter(effectivePoints[0], effectivePoints[1], null, unit)
                } else null
            }
            CadMeasurementType.AREA -> {
                if (effectivePoints.size >= 3) {
                    measureArea(effectivePoints, unit)
                } else null
            }
            CadMeasurementType.PERIMETER -> {
                if (effectivePoints.size >= 2) {
                    measurePerimeter(effectivePoints, unit)
                } else null
            }
            CadMeasurementType.POLYLINE_LENGTH -> {
                if (effectivePoints.size >= 2) {
                    measurePolylineLength(effectivePoints, unit)
                } else null
            }
            CadMeasurementType.BOUNDING_BOX -> {
                if (effectivePoints.size >= 2) {
                    measureBoundingBoxFromPoints(effectivePoints[0], effectivePoints[1], unit)
                } else null
            }
            CadMeasurementType.MULTI_AREA -> {
                if (effectivePoints.size >= 3) {
                    val reg = CadMeasurementResult.CadAreaRegion(
                        id = "reg_active",
                        name = "Region",
                        points = effectivePoints,
                        isSubtract = false,
                        areaSquareUnits = measureArea(effectivePoints, unit).areaSquareUnits,
                        perimeter = measurePerimeter(effectivePoints, unit).perimeter
                    )
                    measureMultipleAreas(listOf(reg), unit)
                } else null
            }
            CadMeasurementType.SELECTION_SUMMARY -> null
        }
    }

    override fun measureEntity(entity: CadEntity, type: CadMeasurementType, unit: CadUnit): CadMeasurementResult? {
        return when (entity) {
            is CadEntity.Line -> {
                when (type) {
                    CadMeasurementType.DISTANCE, CadMeasurementType.ALIGNED_DISTANCE ->
                        measureAlignedDistance(entity.start, entity.end, unit)
                    CadMeasurementType.HORIZONTAL_DISTANCE ->
                        measureHorizontalDistance(entity.start, entity.end, unit)
                    CadMeasurementType.VERTICAL_DISTANCE ->
                        measureVerticalDistance(entity.start, entity.end, unit)
                    CadMeasurementType.BOUNDING_BOX ->
                        measureBoundingBox(entity.boundingBox, unit)
                    CadMeasurementType.POLYLINE_LENGTH ->
                        measurePolylineLength(listOf(entity.start, entity.end), unit)
                    CadMeasurementType.SELECTION_SUMMARY ->
                        measureSelectedEntities(listOf(entity), unit)
                    else -> measureDistance(entity.start, entity.end, unit)
                }
            }
            is CadEntity.Circle -> {
                val rim = CadPoint2D(entity.center.x + entity.radius, entity.center.y)
                when (type) {
                    CadMeasurementType.RADIUS -> measureRadius(entity.center, rim, unit)
                    CadMeasurementType.DIAMETER -> measureDiameter(entity.center, rim, null, unit)
                    CadMeasurementType.AREA -> {
                        val areaInUnit = unit.convertAreaFromMm2(Math.PI * entity.radius.toDouble() * entity.radius.toDouble())
                        val perimInUnit = unit.convertFromMm(2.0 * Math.PI * entity.radius.toDouble())
                        val annotation = CadMeasurementAnnotation.PolygonArea(
                            vertices = listOf(entity.center, rim),
                            centroid = entity.center,
                            areaText = unit.formatArea(areaInUnit),
                            perimeterText = unit.format(perimInUnit),
                            isClosed = true
                        )
                        CadMeasurementResult.Area(
                            points = listOf(entity.center, rim),
                            areaSquareUnits = areaInUnit,
                            perimeter = perimInUnit,
                            isClosedPolyline = false,
                            unit = unit,
                            annotation = annotation
                        )
                    }
                    CadMeasurementType.PERIMETER -> {
                        val perimInUnit = unit.convertFromMm(2.0 * Math.PI * entity.radius.toDouble())
                        val areaInUnit = unit.convertAreaFromMm2(Math.PI * entity.radius.toDouble() * entity.radius.toDouble())
                        CadMeasurementResult.Perimeter(listOf(entity.center, rim), perimInUnit, 1, areaInUnit, unit, null)
                    }
                    CadMeasurementType.BOUNDING_BOX -> measureBoundingBox(entity.boundingBox, unit)
                    CadMeasurementType.MULTI_AREA -> {
                        val areaInUnit = unit.convertAreaFromMm2(Math.PI * entity.radius.toDouble() * entity.radius.toDouble())
                        val perimInUnit = unit.convertFromMm(2.0 * Math.PI * entity.radius.toDouble())
                        val reg = CadMeasurementResult.CadAreaRegion(
                            id = entity.id,
                            name = "Circle ${entity.id}",
                            points = listOf(entity.center, rim),
                            isSubtract = false,
                            areaSquareUnits = areaInUnit,
                            perimeter = perimInUnit
                        )
                        measureMultipleAreas(listOf(reg), unit)
                    }
                    CadMeasurementType.SELECTION_SUMMARY ->
                        measureSelectedEntities(listOf(entity), unit)
                    else -> measureDiameter(entity.center, rim, null, unit)
                }
            }
            is CadEntity.Arc -> {
                val rim = CadPoint2D(entity.center.x + entity.radius, entity.center.y)
                when (type) {
                    CadMeasurementType.SELECTION_SUMMARY -> measureSelectedEntities(listOf(entity), unit)
                    else -> measureRadius(entity.center, rim, unit)
                }
            }
            is CadEntity.Polyline -> {
                when (type) {
                    CadMeasurementType.AREA -> measureClosedPolylineArea(entity, unit)
                    CadMeasurementType.PERIMETER -> measurePerimeter(entity.points, unit)
                    CadMeasurementType.BOUNDING_BOX -> measureBoundingBox(entity.boundingBox, unit)
                    CadMeasurementType.MULTI_AREA -> {
                        val areaResult = measureClosedPolylineArea(entity, unit)
                        val reg = CadMeasurementResult.CadAreaRegion(
                            id = entity.id,
                            name = if (entity.isClosed) "Closed Polyline ${entity.id}" else "Polyline ${entity.id}",
                            points = entity.points,
                            isSubtract = false,
                            areaSquareUnits = areaResult.areaSquareUnits,
                            perimeter = areaResult.perimeter
                        )
                        measureMultipleAreas(listOf(reg), unit)
                    }
                    CadMeasurementType.SELECTION_SUMMARY -> measureSelectedEntities(listOf(entity), unit)
                    else -> measurePolylineLength(entity.points, unit)
                }
            }
            is CadEntity.Text -> {
                measureCoordinate(entity.position, unit)
            }
            is CadEntity.Point -> {
                measureCoordinate(entity.position, unit)
            }
            is CadEntity.Dimension -> {
                measureAlignedDistance(entity.start, entity.end, unit)
            }
            is CadEntity.Leader -> {
                measurePolylineLength(listOf(entity.arrowPoint, entity.kneePoint, entity.landingEndPoint), unit)
            }
            is CadEntity.Arrow -> {
                measureAlignedDistance(entity.start, entity.end, unit)
            }
            is CadEntity.RevisionCloud -> {
                if (entity.isClosed && entity.vertices.size >= 3) {
                    measureArea(entity.vertices, unit)
                } else {
                    measurePolylineLength(entity.vertices, unit)
                }
            }
        }
    }
}

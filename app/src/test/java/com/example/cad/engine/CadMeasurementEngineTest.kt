package com.example.cad.engine

import com.example.cad.engine.measurement.CadMeasurementEngine
import com.example.cad.engine.measurement.CadMeasurementResult
import com.example.cad.engine.measurement.CadMeasurementType
import com.example.cad.engine.measurement.StandardCadMeasurementEngine
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class CadMeasurementEngineTest {

    private lateinit var engine: CadMeasurementEngine

    @Before
    fun setup() {
        engine = StandardCadMeasurementEngine()
    }

    // 1. Distance
    @Test
    fun testDistanceMeasurement() {
        val p1 = CadPoint2D(0f, 0f)
        val p2 = CadPoint2D(300f, 400f) // 3-4-5 triangle -> 500 mm
        val result = engine.calculate(CadMeasurementType.DISTANCE, listOf(p1, p2), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Distance)
        val dist = result as CadMeasurementResult.Distance
        assertEquals(500.0, dist.distance, 0.001)
        assertEquals(300.0, dist.deltaX, 0.001)
        assertEquals(400.0, dist.deltaY, 0.001)
        assertTrue(dist.primaryFormatted.contains("500.00 mm"))
    }

    // 2. Aligned Distance
    @Test
    fun testAlignedDistanceMeasurement() {
        val p1 = CadPoint2D(100f, 100f)
        val p2 = CadPoint2D(200f, 200f)
        val expected = sqrt(20000.0)
        val result = engine.calculate(CadMeasurementType.ALIGNED_DISTANCE, listOf(p1, p2), CadUnit.CENTIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.AlignedDistance)
        val aligned = result as CadMeasurementResult.AlignedDistance
        // 141.42 mm -> 14.14 cm
        assertEquals(expected / 10.0, aligned.distance, 0.01)
        assertEquals(45.0, aligned.angleDeg, 0.01)
        assertEquals(CadUnit.CENTIMETERS, aligned.unit)
    }

    // 3. Horizontal Distance
    @Test
    fun testHorizontalDistanceMeasurement() {
        val p1 = CadPoint2D(50f, 20f)
        val p2 = CadPoint2D(250f, 180f)
        val result = engine.calculate(CadMeasurementType.HORIZONTAL_DISTANCE, listOf(p1, p2), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.HorizontalDistance)
        val horiz = result as CadMeasurementResult.HorizontalDistance
        assertEquals(200.0, horiz.deltaX, 0.001)
        assertTrue(horiz.primaryFormatted.contains("200.00 mm"))
    }

    // 4. Vertical Distance
    @Test
    fun testVerticalDistanceMeasurement() {
        val p1 = CadPoint2D(50f, 20f)
        val p2 = CadPoint2D(250f, 320f)
        val result = engine.calculate(CadMeasurementType.VERTICAL_DISTANCE, listOf(p1, p2), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.VerticalDistance)
        val vert = result as CadMeasurementResult.VerticalDistance
        assertEquals(300.0, vert.deltaY, 0.001)
        assertTrue(vert.primaryFormatted.contains("300.00 mm"))
    }

    // 5. Angle
    @Test
    fun testAngleMeasurement() {
        // Vertex at (0, 0), ray1 along +X (100, 0), ray2 along +Y (0, 100) -> 90 degrees
        val vertex = CadPoint2D(0f, 0f)
        val r1 = CadPoint2D(100f, 0f)
        val r2 = CadPoint2D(0f, 100f)
        val result = engine.calculate(CadMeasurementType.ANGLE, listOf(vertex, r1, r2), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Angle)
        val angle = result as CadMeasurementResult.Angle
        assertEquals(90.0, angle.degrees, 0.01)
        assertEquals(PI / 2.0, angle.radians, 0.01)
    }

    // 6. Radius
    @Test
    fun testRadiusMeasurement() {
        val center = CadPoint2D(50f, 50f)
        val rim = CadPoint2D(50f, 150f) // R = 100 mm
        val result = engine.calculate(CadMeasurementType.RADIUS, listOf(center, rim), CadUnit.METERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Radius)
        val rad = result as CadMeasurementResult.Radius
        assertEquals(0.1, rad.radius, 0.0001) // 100 mm = 0.1 m
        assertEquals(CadUnit.METERS, rad.unit)
    }

    // 7. Diameter
    @Test
    fun testDiameterMeasurement() {
        val p1 = CadPoint2D(0f, 0f)
        val p2 = CadPoint2D(200f, 0f)
        val result = engine.calculate(CadMeasurementType.DIAMETER, listOf(p1, p2), CadUnit.INCHES)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Diameter)
        val diam = result as CadMeasurementResult.Diameter
        // Center (0,0) to rim (200,0) -> Radius = 200 mm, Diameter = 400 mm
        // 400 mm / 25.4 = 15.748 inches
        assertEquals(400.0 / 25.4, diam.diameter, 0.01)
        assertEquals(CadUnit.INCHES, diam.unit)
    }

    // 8. Area
    @Test
    fun testAreaMeasurement() {
        // 1000mm x 1000mm square = 1,000,000 mm^2 = 1.0 m^2
        val square = listOf(
            CadPoint2D(0f, 0f),
            CadPoint2D(1000f, 0f),
            CadPoint2D(1000f, 1000f),
            CadPoint2D(0f, 1000f)
        )
        val result = engine.calculate(CadMeasurementType.AREA, square, CadUnit.METERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Area)
        val area = result as CadMeasurementResult.Area
        assertEquals(1.0, area.areaSquareUnits, 0.001)
        assertEquals(4.0, area.perimeter, 0.001) // 4000 mm = 4.0 m
    }

    // 9. Perimeter
    @Test
    fun testPerimeterMeasurement() {
        // Rectangle 300 x 400 -> perimeter = 2*(300 + 400) = 1400 mm
        val rect = listOf(
            CadPoint2D(0f, 0f),
            CadPoint2D(300f, 0f),
            CadPoint2D(300f, 400f),
            CadPoint2D(0f, 400f)
        )
        val result = engine.calculate(CadMeasurementType.PERIMETER, rect, CadUnit.FEET)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Perimeter)
        val perim = result as CadMeasurementResult.Perimeter
        // 1400 mm / 304.8 = 4.593 ft
        assertEquals(1400.0 / 304.8, perim.perimeter, 0.01)
        assertEquals(4, perim.segmentCount)
    }

    // 10. Coordinate
    @Test
    fun testCoordinateMeasurement() {
        val pt = CadPoint2D(123.4f, 567.8f)
        val result = engine.calculate(CadMeasurementType.COORDINATE, listOf(pt), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Coordinate)
        val coord = result as CadMeasurementResult.Coordinate
        assertEquals(123.4, coord.xInUnit, 0.01)
        assertEquals(567.8, coord.yInUnit, 0.01)
        assertTrue(coord.primaryFormatted.contains("X: 123.40 mm"))
        assertTrue(coord.primaryFormatted.contains("Y: 567.80 mm"))
    }

    // 11. Polyline Length
    @Test
    fun testPolylineLengthMeasurement() {
        // Segments: (0,0)->(100,0) [100], ->(100,100) [100], ->(100,200) [100] = Total 300 mm
        val path = listOf(
            CadPoint2D(0f, 0f),
            CadPoint2D(100f, 0f),
            CadPoint2D(100f, 100f),
            CadPoint2D(100f, 200f)
        )
        val result = engine.calculate(CadMeasurementType.POLYLINE_LENGTH, path, CadUnit.CENTIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.PolylineLength)
        val poly = result as CadMeasurementResult.PolylineLength
        assertEquals(30.0, poly.totalLength, 0.01) // 300 mm = 30 cm
        assertEquals(3, poly.segmentLengths.size)
        assertEquals(10.0, poly.segmentLengths[0], 0.01)
    }

    // 12. Bounding Box
    @Test
    fun testBoundingBoxMeasurement() {
        val p1 = CadPoint2D(10f, 20f)
        val p2 = CadPoint2D(110f, 80f)
        val result = engine.calculate(CadMeasurementType.BOUNDING_BOX, listOf(p1, p2), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.BoundingBox)
        val bbox = result as CadMeasurementResult.BoundingBox
        assertEquals(100.0, bbox.width, 0.01)
        assertEquals(60.0, bbox.height, 0.01)
        assertEquals(6000.0, bbox.area, 0.01)
        assertEquals(10f, bbox.box.minX, 0.01f)
        assertEquals(110f, bbox.box.maxX, 0.01f)
    }

    // Unit Conversion Suite
    @Test
    fun testMetricAndImperialUnitConversions() {
        val mm = 2540.0 // 2540 mm = 254 cm = 2.54 m = 100 inches = 8.333 ft

        assertEquals(2540.0, CadUnit.MILLIMETERS.convertFromMm(mm), 0.001)
        assertEquals(254.0, CadUnit.CENTIMETERS.convertFromMm(mm), 0.001)
        assertEquals(2.54, CadUnit.METERS.convertFromMm(mm), 0.001)
        assertEquals(100.0, CadUnit.INCHES.convertFromMm(mm), 0.001)
        assertEquals(100.0 / 12.0, CadUnit.FEET.convertFromMm(mm), 0.001)

        // Area conversions: 1,000,000 mm2
        val areaMm2 = 1_000_000.0
        assertEquals(1_000_000.0, CadUnit.MILLIMETERS.convertAreaFromMm2(areaMm2), 0.001)
        assertEquals(10_000.0, CadUnit.CENTIMETERS.convertAreaFromMm2(areaMm2), 0.001)
        assertEquals(1.0, CadUnit.METERS.convertAreaFromMm2(areaMm2), 0.001)
    }

    // Direct Entity Measurement
    @Test
    fun testDirectEntityMeasurement() {
        val line = CadEntity.Line("l1", "0", null, CadPoint2D(0f, 0f), CadPoint2D(200f, 0f))
        val lineResult = engine.measureEntity(line, CadMeasurementType.DISTANCE, CadUnit.MILLIMETERS)
        assertNotNull(lineResult)
        assertTrue(lineResult is CadMeasurementResult.AlignedDistance)
        assertEquals(200.0, (lineResult as CadMeasurementResult.AlignedDistance).distance, 0.001)

        val circle = CadEntity.Circle("c1", "0", null, CadPoint2D(0f, 0f), 50f)
        val radiusResult = engine.measureEntity(circle, CadMeasurementType.RADIUS, CadUnit.MILLIMETERS)
        assertNotNull(radiusResult)
        assertEquals(50.0, (radiusResult as CadMeasurementResult.Radius).radius, 0.001)

        val diamResult = engine.measureEntity(circle, CadMeasurementType.DIAMETER, CadUnit.MILLIMETERS)
        assertNotNull(diamResult)
        assertEquals(100.0, (diamResult as CadMeasurementResult.Diameter).diameter, 0.001)
    }

    // 13. Closed Polyline Area & Perimeter
    @Test
    fun testClosedPolylineArea() {
        val polyline = CadEntity.Polyline(
            id = "pl1",
            layerId = "0",
            colorArgb = null,
            points = listOf(
                CadPoint2D(0f, 0f),
                CadPoint2D(100f, 0f),
                CadPoint2D(100f, 50f),
                CadPoint2D(0f, 50f)
            ),
            isClosed = true
        )
        val result = engine.measureEntity(polyline, CadMeasurementType.AREA, CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertTrue(result is CadMeasurementResult.Area)
        val areaRes = result as CadMeasurementResult.Area
        assertEquals(5000.0, areaRes.areaSquareUnits, 0.01)
        assertEquals(300.0, areaRes.perimeter, 0.01)
        assertTrue(areaRes.primaryFormatted.contains("5000.00 mm²"))
    }

    // 14. Multiple-Area Measurement with Add and Subtract Regions
    @Test
    fun testMultiAreaAddAndSubtract() {
        // Main region: 100 x 100 square -> 10,000 mm2
        val r1 = CadMeasurementResult.CadAreaRegion(
            id = "reg1",
            name = "Outer Room",
            points = listOf(
                CadPoint2D(0f, 0f),
                CadPoint2D(100f, 0f),
                CadPoint2D(100f, 100f),
                CadPoint2D(0f, 100f)
            ),
            isSubtract = false,
            areaSquareUnits = 10000.0,
            perimeter = 400.0
        )
        // Void/cutout region: 20 x 20 square -> 400 mm2
        val r2 = CadMeasurementResult.CadAreaRegion(
            id = "reg2",
            name = "Column Cutout",
            points = listOf(
                CadPoint2D(40f, 40f),
                CadPoint2D(60f, 40f),
                CadPoint2D(60f, 60f),
                CadPoint2D(40f, 60f)
            ),
            isSubtract = true,
            areaSquareUnits = 400.0,
            perimeter = 80.0
        )

        val result = engine.measureMultipleAreas(listOf(r1, r2), CadUnit.MILLIMETERS)
        assertNotNull(result)
        assertEquals(10000.0, result.grossAddedArea, 0.01)
        assertEquals(400.0, result.subtractedArea, 0.01)
        assertEquals(9600.0, result.netArea, 0.01)
        assertEquals(480.0, result.totalPerimeter, 0.01)
        assertEquals(2, result.regions.size)
        assertTrue(result.primaryFormatted.contains("9600.00 mm²"))
        assertEquals("9600.00 mm²", result.displayArea)
        assertEquals("480.00 mm", result.displayPerimeter)
        assertTrue(result.displayObjectCount.contains("2 regions"))
    }

    // 15. Selection Summary (Total Area, Length of lines/polylines, Object Count)
    @Test
    fun testSelectionSummaryMeasurement() {
        val line1 = CadEntity.Line("l1", "0", null, CadPoint2D(0f, 0f), CadPoint2D(100f, 0f)) // len 100
        val line2 = CadEntity.Line("l2", "0", null, CadPoint2D(0f, 0f), CadPoint2D(0f, 50f)) // len 50
        val circle = CadEntity.Circle("c1", "0", null, CadPoint2D(0f, 0f), 10f) // area pi*100, perim 2*pi*10
        val closedPoly = CadEntity.Polyline(
            id = "pl1",
            layerId = "0",
            colorArgb = null,
            points = listOf(
                CadPoint2D(0f, 0f),
                CadPoint2D(40f, 0f),
                CadPoint2D(40f, 30f),
                CadPoint2D(0f, 30f)
            ),
            isClosed = true
        ) // area 1200, perim 140

        val entities = listOf(line1, line2, circle, closedPoly)
        val result = engine.measureSelectedEntities(entities, CadUnit.MILLIMETERS)

        assertNotNull(result)
        assertEquals(4, result.objectCount)
        val expectedLength = 100.0 + 50.0 + (2 * PI * 10) + 140.0
        val expectedArea = (PI * 100) + 1200.0

        assertEquals(expectedLength, result.totalLength, 0.1)
        assertEquals(expectedArea, result.totalAreaSquareUnits, 0.1)
        assertEquals(4, result.objectCount)
        assertTrue(result.displayObjectCount.contains("4 items"))
        assertNotNull(result.displayArea)
        assertNotNull(result.displayTotalLength)
        assertNotNull(result.displayPerimeter)
    }
}

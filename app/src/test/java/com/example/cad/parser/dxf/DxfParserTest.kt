package com.example.cad.parser.dxf

import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class DxfParserTest {

    private lateinit var parser: DxfParser

    @Before
    fun setUp() {
        parser = DxfParser()
    }

    @Test
    fun parse_lineEntity_preservesCoordinatesAndLayer() {
        val dxfData = """
0
SECTION
2
ENTITIES
0
LINE
8
WALLS
10
15.5
20
25.25
11
115.5
21
225.25
62
3
0
ENDSEC
0
EOF
""".trimIndent()

        val result = parser.parse(ByteArrayInputStream(dxfData.toByteArray(Charsets.UTF_8)), "test_line.dxf")
        assertTrue("Parsing LINE entity should succeed", result.isSuccess)

        val doc = result.getOrThrow()
        val line = doc.entities.filterIsInstance<CadEntity.Line>().firstOrNull()

        assertNotNull("Line entity should exist in parsed document", line)
        assertEquals(15.5f, line!!.start.x, 0.001f)
        assertEquals(25.25f, line.start.y, 0.001f)
        assertEquals(115.5f, line.end.x, 0.001f)
        assertEquals(225.25f, line.end.y, 0.001f)
        assertEquals("WALLS", line.layerId)
    }

    @Test
    fun parse_circleEntity_preservesCenterAndRadius() {
        val dxfData = """
0
SECTION
2
ENTITIES
0
CIRCLE
8
FOUNDATION
10
100.0
20
200.0
40
50.0
62
4
0
ENDSEC
0
EOF
""".trimIndent()

        val result = parser.parse(ByteArrayInputStream(dxfData.toByteArray(Charsets.UTF_8)), "test_circle.dxf")
        assertTrue("Parsing CIRCLE entity should succeed", result.isSuccess)

        val doc = result.getOrThrow()
        val circle = doc.entities.filterIsInstance<CadEntity.Circle>().firstOrNull()

        assertNotNull("Circle entity should exist in parsed document", circle)
        assertEquals(100.0f, circle!!.center.x, 0.001f)
        assertEquals(200.0f, circle.center.y, 0.001f)
        assertEquals(50.0f, circle.radius, 0.001f)
        assertEquals("FOUNDATION", circle.layerId)
    }

    @Test
    fun parse_arcEntity_preservesCenterRadiusAndAngles() {
        val dxfData = """
0
SECTION
2
ENTITIES
0
ARC
8
DOOR_SWING
10
50.0
20
60.0
40
30.0
50
45.0
51
135.0
0
ENDSEC
0
EOF
""".trimIndent()

        val result = parser.parse(ByteArrayInputStream(dxfData.toByteArray(Charsets.UTF_8)), "test_arc.dxf")
        assertTrue("Parsing ARC entity should succeed", result.isSuccess)

        val doc = result.getOrThrow()
        val arc = doc.entities.filterIsInstance<CadEntity.Arc>().firstOrNull()

        assertNotNull("Arc entity should exist in parsed document", arc)
        assertEquals(50.0f, arc!!.center.x, 0.001f)
        assertEquals(60.0f, arc.center.y, 0.001f)
        assertEquals(30.0f, arc.radius, 0.001f)
        assertEquals(45.0f, arc.startAngleDeg, 0.001f)
        assertEquals(90.0f, arc.sweepAngleDeg, 0.001f)
    }

    @Test
    fun parse_lwpolylineRectangle_preservesClosedVerticesAndBounds() {
        // Rectangle: 4 vertices from (10, 10) to (110, 60), closed flag 70 = 1
        val dxfData = """
0
SECTION
2
ENTITIES
0
LWPOLYLINE
8
ROOM_OUTLINE
90
4
70
1
10
10.0
20
10.0
10
110.0
20
10.0
10
110.0
20
60.0
10
10.0
20
60.0
0
ENDSEC
0
EOF
""".trimIndent()

        val result = parser.parse(ByteArrayInputStream(dxfData.toByteArray(Charsets.UTF_8)), "test_rect.dxf")
        assertTrue("Parsing closed LWPOLYLINE rectangle should succeed", result.isSuccess)

        val doc = result.getOrThrow()
        val polyline = doc.entities.filterIsInstance<CadEntity.Polyline>().firstOrNull()

        assertNotNull("Polyline should exist", polyline)
        assertTrue("Rectangle should be closed", polyline!!.isClosed)
        assertEquals("Should have 4 vertices", 4, polyline.points.size)
        assertEquals(CadPoint2D(10f, 10f), polyline.points[0])
        assertEquals(CadPoint2D(110f, 10f), polyline.points[1])
        assertEquals(CadPoint2D(110f, 60f), polyline.points[2])
        assertEquals(CadPoint2D(10f, 60f), polyline.points[3])

        // Verify bounding box calculation for rectangle
        val bounds = polyline.boundingBox
        assertEquals(10f, bounds.minX, 0.001f)
        assertEquals(10f, bounds.minY, 0.001f)
        assertEquals(110f, bounds.maxX, 0.001f)
        assertEquals(60f, bounds.maxY, 0.001f)
        assertEquals(100f, bounds.width, 0.001f)
        assertEquals(50f, bounds.height, 0.001f)
    }

    @Test
    fun parse_polylineWithVerticesAndSeqend_parsesAllVertexPoints() {
        val dxfData = """
0
SECTION
2
ENTITIES
0
POLYLINE
8
PATH
70
0
0
VERTEX
10
0.0
20
0.0
0
VERTEX
10
50.0
20
75.0
0
VERTEX
10
100.0
20
50.0
0
SEQEND
0
ENDSEC
0
EOF
""".trimIndent()

        val result = parser.parse(ByteArrayInputStream(dxfData.toByteArray(Charsets.UTF_8)), "test_polyline.dxf")
        assertTrue("Parsing POLYLINE with VERTEX stream should succeed", result.isSuccess)

        val doc = result.getOrThrow()
        val polyline = doc.entities.filterIsInstance<CadEntity.Polyline>().firstOrNull()

        assertNotNull("Polyline entity should exist", polyline)
        assertEquals(3, polyline!!.points.size)
        assertEquals(CadPoint2D(0f, 0f), polyline.points[0])
        assertEquals(CadPoint2D(50f, 75f), polyline.points[1])
        assertEquals(CadPoint2D(100f, 50f), polyline.points[2])
        assertFalse(polyline.isClosed)
    }

    @Test
    fun parse_textEntity_preservesStringPositionHeightAndRotation() {
        val dxfData = """
0
SECTION
2
ENTITIES
0
TEXT
8
ANNOTATION
10
35.0
20
70.0
40
12.5
1
BEARING BEAM W12x26
50
45.0
0
ENDSEC
0
EOF
""".trimIndent()

        val result = parser.parse(ByteArrayInputStream(dxfData.toByteArray(Charsets.UTF_8)), "test_text.dxf")
        assertTrue("Parsing TEXT entity should succeed", result.isSuccess)

        val doc = result.getOrThrow()
        val textEntity = doc.entities.filterIsInstance<CadEntity.Text>().firstOrNull()

        assertNotNull("Text entity should exist", textEntity)
        assertEquals("BEARING BEAM W12x26", textEntity!!.text)
        assertEquals(35.0f, textEntity.position.x, 0.001f)
        assertEquals(70.0f, textEntity.position.y, 0.001f)
        assertEquals(12.5f, textEntity.textHeight, 0.001f)
        assertEquals(45.0f, textEntity.rotationDeg, 0.001f)
    }

    @Test
    fun parse_fullSampleDxf_calculatesAccurateDrawingBounds() {
        val inputStream = ByteArrayInputStream(SampleDxfContent.VALID_FULL_DXF.toByteArray(Charsets.UTF_8))
        val result = parser.parse(inputStream, "full_sample.dxf")

        assertTrue("Full technical DXF should parse successfully", result.isSuccess)
        val doc = result.getOrThrow()

        // Verify entities count: LINE (2), CIRCLE (1), ARC (1), LWPOLYLINE (1), POLYLINE (1), TEXT (1) = 7 entities
        assertEquals(7, doc.entities.size)

        // Automatically calculate drawing bounds
        val extents = doc.computeExtents()
        assertFalse("Drawing bounds should not be empty", extents.isEmpty)

        // Bounds should encompass from minX <= 0f to maxX >= 300f
        assertTrue("Bounds minX should be <= 0f: ${extents.minX}", extents.minX <= 0f)
        assertTrue("Bounds maxX should be >= 300f: ${extents.maxX}", extents.maxX >= 300f)
        assertTrue("Bounds minY should be <= 0f: ${extents.minY}", extents.minY <= 0f)
        assertTrue("Bounds maxY should be >= 200f: ${extents.maxY}", extents.maxY >= 200f)
    }

    @Test
    fun parse_malformedDxf_doesNotCrashAndParsesValidEntities() {
        val inputStream = ByteArrayInputStream(SampleDxfContent.MALFORMED_DXF_WITH_JUNK_TOKENS.toByteArray(Charsets.UTF_8))
        val result = parser.parse(inputStream, "malformed.dxf")

        // Parser should not crash
        assertTrue("Malformed DXF should be handled safely without crashing", result.isSuccess)
        val doc = result.getOrThrow()

        // Valid entities (one line and one circle) should still be parsed
        val hasLine = doc.entities.any { it is CadEntity.Line }
        val hasCircle = doc.entities.any { it is CadEntity.Circle }
        assertTrue("Parser should recover and extract valid LINE", hasLine)
        assertTrue("Parser should recover and extract valid CIRCLE", hasCircle)
    }

    @Test
    fun parse_emptyStream_returnsFailureWithoutCrashing() {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        val result = parser.parse(emptyStream, "empty.dxf")

        assertTrue("Empty stream should return failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should be provided", exception)
        assertTrue("Exception message should indicate empty file", exception?.message?.contains("empty", ignoreCase = true) == true)
    }
}

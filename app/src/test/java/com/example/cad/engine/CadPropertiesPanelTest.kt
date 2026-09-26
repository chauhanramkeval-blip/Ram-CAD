package com.example.cad.engine

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class CadPropertiesPanelTest {

    private lateinit var cadEngine: DefaultCadEngine
    private lateinit var sampleDoc: CadDocument
    private lateinit var line: CadEntity.Line
    private lateinit var circle: CadEntity.Circle
    private lateinit var arc: CadEntity.Arc
    private lateinit var polyline: CadEntity.Polyline
    private lateinit var text: CadEntity.Text

    @Before
    fun setup() {
        cadEngine = DefaultCadEngine()
        line = CadEntity.Line(
            id = "line_101",
            layerId = "0",
            start = CadPoint2D(10f, 20f),
            end = CadPoint2D(110f, 20f)
        )
        circle = CadEntity.Circle(
            id = "circle_101",
            layerId = "0",
            center = CadPoint2D(50f, 50f),
            radius = 30f
        )
        arc = CadEntity.Arc(
            id = "arc_101",
            layerId = "0",
            center = CadPoint2D(0f, 0f),
            radius = 45f,
            startAngleDeg = 30f,
            sweepAngleDeg = 60f
        )
        polyline = CadEntity.Polyline(
            id = "poly_101",
            layerId = "0",
            points = listOf(
                CadPoint2D(0f, 0f),
                CadPoint2D(100f, 0f),
                CadPoint2D(100f, 100f)
            ),
            isClosed = false
        )
        text = CadEntity.Text(
            id = "text_101",
            layerId = "0",
            position = CadPoint2D(25f, 35f),
            text = "SECTION A-A",
            textHeight = 14f,
            rotationDeg = 0f
        )

        val layer0 = CadLayer("0", "0", 0xFFFFFFFF, isVisible = true, isLocked = false)
        val layerHidden = CadLayer("HIDDEN", "HIDDEN", 0xFFFF9800, isVisible = true, isLocked = false)
        val layerLocked = CadLayer("LOCKED_LAYER", "LOCKED_LAYER", 0xFF9E9E9E, isVisible = true, isLocked = true)

        sampleDoc = CadDocument(
            title = "Properties Panel Test Drawing",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            layers = mapOf(
                layer0.id to layer0,
                layerHidden.id to layerHidden,
                layerLocked.id to layerLocked
            ),
            entities = listOf(line, circle, arc, polyline, text)
        )
    }

    @Test
    fun testLinePropertyEdits_updatesStartEndLengthAndLayer() {
        val editor = cadEngine.editor

        // Edit Start X/Y
        val updatedStart = line.copy(start = CadPoint2D(15f, 25f))
        var doc = editor.updateEntity(sampleDoc, updatedStart)
        var fetched = doc.entities.find { it.id == line.id } as CadEntity.Line
        assertEquals(15f, fetched.start.x, 0.001f)
        assertEquals(25f, fetched.start.y, 0.001f)
        assertTrue(editor.canUndo())

        // Edit End X/Y
        val updatedEnd = fetched.copy(end = CadPoint2D(125f, 25f))
        doc = editor.updateEntity(doc, updatedEnd)
        fetched = doc.entities.find { it.id == line.id } as CadEntity.Line
        assertEquals(125f, fetched.end.x, 0.001f)
        assertEquals(25f, fetched.end.y, 0.001f)

        // Edit Length (scaled along direction vector)
        val desiredLength = 200f
        val dx = fetched.end.x - fetched.start.x
        val dy = fetched.end.y - fetched.start.y
        val len = sqrt(dx * dx + dy * dy)
        val ux = dx / len
        val uy = dy / len
        val newEnd = CadPoint2D(fetched.start.x + ux * desiredLength, fetched.start.y + uy * desiredLength)
        val lengthUpdated = fetched.copy(end = newEnd)
        doc = editor.updateEntity(doc, lengthUpdated)
        fetched = doc.entities.find { it.id == line.id } as CadEntity.Line
        val computedLen = fetched.start.distanceTo(fetched.end)
        assertEquals(200f, computedLen, 0.001f)

        // Edit Layer
        val layerUpdated = fetched.copy(layerId = "HIDDEN")
        doc = editor.updateEntity(doc, layerUpdated)
        fetched = doc.entities.find { it.id == line.id } as CadEntity.Line
        assertEquals("HIDDEN", fetched.layerId)
    }

    @Test
    fun testCirclePropertyEdits_updatesCenterRadiusAndLayer() {
        val editor = cadEngine.editor

        // Edit Center X/Y
        val updatedCenter = circle.copy(center = CadPoint2D(75f, 85f))
        var doc = editor.updateEntity(sampleDoc, updatedCenter)
        var fetched = doc.entities.find { it.id == circle.id } as CadEntity.Circle
        assertEquals(75f, fetched.center.x, 0.001f)
        assertEquals(85f, fetched.center.y, 0.001f)

        // Edit Radius
        val updatedRadius = fetched.copy(radius = 55.5f)
        doc = editor.updateEntity(doc, updatedRadius)
        fetched = doc.entities.find { it.id == circle.id } as CadEntity.Circle
        assertEquals(55.5f, fetched.radius, 0.001f)

        // Edit Layer
        val updatedLayer = fetched.copy(layerId = "HIDDEN")
        doc = editor.updateEntity(doc, updatedLayer)
        fetched = doc.entities.find { it.id == circle.id } as CadEntity.Circle
        assertEquals("HIDDEN", fetched.layerId)
    }

    @Test
    fun testArcPropertyEdits_updatesCenterRadiusAnglesAndLayer() {
        val editor = cadEngine.editor

        // Edit Center X/Y and Radius
        val updatedCenter = arc.copy(center = CadPoint2D(12f, 18f), radius = 50f)
        var doc = editor.updateEntity(sampleDoc, updatedCenter)
        var fetched = doc.entities.find { it.id == arc.id } as CadEntity.Arc
        assertEquals(12f, fetched.center.x, 0.001f)
        assertEquals(18f, fetched.center.y, 0.001f)
        assertEquals(50f, fetched.radius, 0.001f)

        // Edit Start Angle and End Angle
        // User sets Start Angle = 45°, End Angle = 135° -> sweep = 90°
        val newStart = 45f
        val newEnd = 135f
        val newSweep = newEnd - newStart
        val updatedAngles = fetched.copy(startAngleDeg = newStart, sweepAngleDeg = newSweep)
        doc = editor.updateEntity(doc, updatedAngles)
        fetched = doc.entities.find { it.id == arc.id } as CadEntity.Arc
        assertEquals(45f, fetched.startAngleDeg, 0.001f)
        assertEquals(90f, fetched.sweepAngleDeg, 0.001f)
        assertEquals(135f, fetched.startAngleDeg + fetched.sweepAngleDeg, 0.001f)
    }

    @Test
    fun testPolylinePropertyEdits_updatesVertexCountCoordinatesClosedAndLayer() {
        val editor = cadEngine.editor

        assertEquals(3, polyline.points.size)
        assertFalse(polyline.isClosed)

        // Edit Vertex Coordinates (modify vertex #2)
        val modifiedPoints = polyline.points.toMutableList()
        modifiedPoints[1] = CadPoint2D(120f, -10f)
        val updatedVertex = polyline.copy(points = modifiedPoints)
        var doc = editor.updateEntity(sampleDoc, updatedVertex)
        var fetched = doc.entities.find { it.id == polyline.id } as CadEntity.Polyline
        assertEquals(120f, fetched.points[1].x, 0.001f)
        assertEquals(-10f, fetched.points[1].y, 0.001f)

        // Add Vertex
        val withAddedVertex = fetched.copy(points = fetched.points + CadPoint2D(0f, 100f))
        doc = editor.updateEntity(doc, withAddedVertex)
        fetched = doc.entities.find { it.id == polyline.id } as CadEntity.Polyline
        assertEquals(4, fetched.points.size)

        // Edit Closed/Open
        val closedPoly = fetched.copy(isClosed = true)
        doc = editor.updateEntity(doc, closedPoly)
        fetched = doc.entities.find { it.id == polyline.id } as CadEntity.Polyline
        assertTrue(fetched.isClosed)
    }

    @Test
    fun testTextPropertyEdits_updatesContentPositionHeightRotationAndLayer() {
        val editor = cadEngine.editor

        // Edit Text Content
        val updatedText = text.copy(text = "DETAIL B (SCALE 2:1)")
        var doc = editor.updateEntity(sampleDoc, updatedText)
        var fetched = doc.entities.find { it.id == text.id } as CadEntity.Text
        assertEquals("DETAIL B (SCALE 2:1)", fetched.text)

        // Edit Position X/Y
        val updatedPos = fetched.copy(position = CadPoint2D(40f, 60f))
        doc = editor.updateEntity(doc, updatedPos)
        fetched = doc.entities.find { it.id == text.id } as CadEntity.Text
        assertEquals(40f, fetched.position.x, 0.001f)
        assertEquals(60f, fetched.position.y, 0.001f)

        // Edit Height
        val updatedHeight = fetched.copy(textHeight = 20f)
        doc = editor.updateEntity(doc, updatedHeight)
        fetched = doc.entities.find { it.id == text.id } as CadEntity.Text
        assertEquals(20f, fetched.textHeight, 0.001f)

        // Edit Rotation
        val updatedRotation = fetched.copy(rotationDeg = 45f)
        doc = editor.updateEntity(doc, updatedRotation)
        fetched = doc.entities.find { it.id == text.id } as CadEntity.Text
        assertEquals(45f, fetched.rotationDeg, 0.001f)
    }

    @Test
    fun testLockedLayer_preventsModifications() {
        val editor = cadEngine.editor

        // Create entity in locked layer
        val lockedLine = CadEntity.Line(
            id = "locked_line",
            layerId = "LOCKED_LAYER",
            start = CadPoint2D(0f, 0f),
            end = CadPoint2D(50f, 50f)
        )
        val docWithLocked = sampleDoc.copy(entities = sampleDoc.entities + lockedLine)

        // Try to update entity on locked layer
        val attemptUpdate = lockedLine.copy(end = CadPoint2D(999f, 999f))
        val resultDoc = editor.updateEntity(docWithLocked, attemptUpdate)
        val fetched = resultDoc.entities.find { it.id == lockedLine.id } as CadEntity.Line

        // Must NOT have modified end point
        assertEquals(50f, fetched.end.x, 0.001f)
        assertEquals(50f, fetched.end.y, 0.001f)
    }
}

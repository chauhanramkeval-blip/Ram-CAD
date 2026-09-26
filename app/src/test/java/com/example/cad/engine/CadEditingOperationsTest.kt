package com.example.cad.engine

import com.example.cad.engine.edit.CadEditMath
import com.example.cad.engine.edit.EditCommandState
import com.example.cad.engine.edit.EditOperationType
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class CadEditingOperationsTest {

    private lateinit var cadEngine: DefaultCadEngine
    private lateinit var sampleDoc: CadDocument
    private lateinit var line1: CadEntity.Line
    private lateinit var line2: CadEntity.Line
    private lateinit var circle1: CadEntity.Circle

    @Before
    fun setup() {
        cadEngine = DefaultCadEngine()
        line1 = CadEntity.Line(
            id = "line_1",
            layerId = "0",
            start = CadPoint2D(0f, 0f),
            end = CadPoint2D(100f, 0f)
        )
        line2 = CadEntity.Line(
            id = "line_2",
            layerId = "0",
            start = CadPoint2D(50f, -50f),
            end = CadPoint2D(50f, 50f)
        )
        circle1 = CadEntity.Circle(
            id = "circle_1",
            layerId = "0",
            center = CadPoint2D(0f, 0f),
            radius = 25f
        )
        sampleDoc = CadDocument(
            title = "Test Doc",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            entities = listOf(line1, line2, circle1)
        )
    }

    @Test
    fun testMoveOperation_maintainsExactWorldCoordinates() {
        val deltaX = 42.5f
        val deltaY = -18.25f

        val resultDoc = cadEngine.editor.moveEntities(
            document = sampleDoc,
            entityIds = setOf("line_1"),
            deltaX = deltaX,
            deltaY = deltaY
        )

        val movedLine = resultDoc.entities.find { it.id == "line_1" } as CadEntity.Line
        assertEquals(42.5f, movedLine.start.x, 0.001f)
        assertEquals(-18.25f, movedLine.start.y, 0.001f)
        assertEquals(142.5f, movedLine.end.x, 0.001f)
        assertEquals(-18.25f, movedLine.end.y, 0.001f)

        // Unselected entities remain unchanged
        val unselectedLine = resultDoc.entities.find { it.id == "line_2" } as CadEntity.Line
        assertEquals(50f, unselectedLine.start.x, 0.001f)
        assertEquals(-50f, unselectedLine.start.y, 0.001f)
    }

    @Test
    fun testCopyOperation_duplicatesWithOffsetAndPreservesOriginal() {
        val (resultDoc, newIds) = cadEngine.editor.copyEntities(
            document = sampleDoc,
            entityIds = setOf("line_1"),
            deltaX = 200f,
            deltaY = 100f
        )

        // Original count was 3, should now be 4
        assertEquals(4, resultDoc.entities.size)
        assertEquals(1, newIds.size)

        // Original line1 unchanged
        val orig = resultDoc.entities.find { it.id == "line_1" } as CadEntity.Line
        assertEquals(0f, orig.start.x, 0.001f)
        assertEquals(0f, orig.start.y, 0.001f)

        // Copied line exists with offset
        val copied = resultDoc.entities.find { newIds.contains(it.id) } as CadEntity.Line
        assertEquals(200f, copied.start.x, 0.001f)
        assertEquals(100f, copied.start.y, 0.001f)
        assertEquals(300f, copied.end.x, 0.001f)
        assertEquals(100f, copied.end.y, 0.001f)
    }

    @Test
    fun testDeleteOperation_removesEntity() {
        val resultDoc = cadEngine.editor.deleteEntities(
            document = sampleDoc,
            entityIds = setOf("circle_1")
        )

        assertEquals(2, resultDoc.entities.size)
        assertNull(resultDoc.entities.find { it.id == "circle_1" })
    }

    @Test
    fun testRotateOperation_rotates90DegreesAroundOrigin() {
        val resultDoc = cadEngine.editor.rotateEntities(
            document = sampleDoc,
            entityIds = setOf("line_1"),
            center = CadPoint2D(0f, 0f),
            angleDegrees = 90f
        )

        val rotatedLine = resultDoc.entities.find { it.id == "line_1" } as CadEntity.Line
        assertEquals(0f, rotatedLine.start.x, 0.01f)
        assertEquals(0f, rotatedLine.start.y, 0.01f)
        assertEquals(0f, rotatedLine.end.x, 0.01f)
        assertEquals(100f, rotatedLine.end.y, 0.01f)
    }

    @Test
    fun testScaleOperation_scalesEntityFromCenter() {
        val resultDoc = cadEngine.editor.scaleEntities(
            document = sampleDoc,
            entityIds = setOf("circle_1"),
            basePoint = CadPoint2D(0f, 0f),
            factor = 2.0f
        )

        val scaledCircle = resultDoc.entities.find { it.id == "circle_1" } as CadEntity.Circle
        assertEquals(0f, scaledCircle.center.x, 0.001f)
        assertEquals(0f, scaledCircle.center.y, 0.001f)
        assertEquals(50f, scaledCircle.radius, 0.001f)
    }

    @Test
    fun testTrimOperation_trimsSegmentAtCrossingIntersection() {
        // line1: (0,0) to (100,0)
        // line2: (50,-50) to (50,50) - crosses line1 at (50,0)
        // Tapping at (25,0) should trim the left segment (0,0)-(50,0), leaving (50,0)-(100,0)
        val trimResult = CadEditMath.trimLine(line1, CadPoint2D(25f, 0f), listOf(line2))
        assertNotNull(trimResult)

        val remaining = trimResult!!.replacementEntities.filterIsInstance<CadEntity.Line>()
        assertEquals(1, remaining.size)
        assertEquals(50f, remaining[0].start.x, 0.001f)
        assertEquals(100f, remaining[0].end.x, 0.001f)
    }

    @Test
    fun testExtendOperation_extendsLineToNearestBoundary() {
        // Line extending towards x = 200 boundary
        val lineToExtend = CadEntity.Line(
            id = "line_to_ext",
            layerId = "0",
            start = CadPoint2D(0f, 50f),
            end = CadPoint2D(100f, 50f)
        )
        val boundaryLine = CadEntity.Line(
            id = "boundary",
            layerId = "0",
            start = CadPoint2D(200f, 0f),
            end = CadPoint2D(200f, 100f)
        )

        val extendResult = CadEditMath.extendLine(lineToExtend, CadPoint2D(90f, 50f), listOf(boundaryLine))
        assertNotNull(extendResult)

        val extended = extendResult!!.extendedEntity as CadEntity.Line
        assertEquals(0f, extended.start.x, 0.001f)
        assertEquals(200f, extended.end.x, 0.001f)
        assertEquals(50f, extended.end.y, 0.001f)
    }

    @Test
    fun testUndoRedoIntegration_forRotateAndMove() {
        val initialDoc = sampleDoc
        val movedDoc = cadEngine.editor.moveEntities(initialDoc, setOf("line_1"), 50f, 50f)

        assertTrue(cadEngine.editor.canUndo())

        // Perform undo
        val undoneDoc = cadEngine.editor.undo(movedDoc)
        val revertedLine = undoneDoc.entities.find { it.id == "line_1" } as CadEntity.Line
        assertEquals(0f, revertedLine.start.x, 0.001f)
        assertEquals(0f, revertedLine.start.y, 0.001f)

        // Perform redo
        assertTrue(cadEngine.editor.canRedo())
        val redoneDoc = cadEngine.editor.redo(undoneDoc)
        val redoneLine = redoneDoc.entities.find { it.id == "line_1" } as CadEntity.Line
        assertEquals(50f, redoneLine.start.x, 0.001f)
        assertEquals(50f, redoneLine.start.y, 0.001f)
    }

    @Test
    fun testCommandStateIsolation_neverModifiesOriginalUntilConfirmed() {
        val activeState = EditCommandState.initialize(
            operation = EditOperationType.ROTATE,
            targetEntities = listOf(line1),
            allDocumentEntities = sampleDoc.entities,
            initialPoint = CadPoint2D(0f, 0f)
        ) as EditCommandState.Active

        // Rotate in preview state
        val preview = CadEditMath.rotateEntity(line1, CadPoint2D(0f, 0f), 45f)
        val updatedActive = activeState.copy(
            angleDeg = 45f,
            previewEntities = listOf(preview)
        )

        // Preview entity is rotated
        val previewLine = updatedActive.previewEntities[0] as CadEntity.Line
        assertTrue(abs(previewLine.end.x - 70.71f) < 0.5f)

        // Original document entity MUST remain completely untouched!
        val docLine = sampleDoc.entities.find { it.id == "line_1" } as CadEntity.Line
        assertEquals(100f, docLine.end.x, 0.001f)
        assertEquals(0f, docLine.end.y, 0.001f)
    }
}

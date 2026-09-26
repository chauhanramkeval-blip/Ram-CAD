package com.example.cad.engine

import com.example.cad.engine.selection.GripType
import com.example.cad.engine.selection.HitTestEngine
import com.example.cad.engine.selection.SelectionBox
import com.example.cad.engine.selection.SelectionManager
import com.example.cad.engine.selection.SelectionState
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.cad.model.CadViewportTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CadSelectionSystemTest {

    private lateinit var hitTestEngine: HitTestEngine
    private lateinit var selectionManager: SelectionManager
    private lateinit var cadEngine: DefaultCadEngine
    private lateinit var sampleDocument: CadDocument
    private lateinit var viewportTransform: CadViewportTransform

    private lateinit var lineEntity: CadEntity.Line
    private lateinit var polylineEntity: CadEntity.Polyline
    private lateinit var circleEntity: CadEntity.Circle
    private lateinit var arcEntity: CadEntity.Arc
    private lateinit var textEntity: CadEntity.Text

    @Before
    fun setup() {
        hitTestEngine = HitTestEngine()
        selectionManager = SelectionManager(hitTestEngine)
        cadEngine = DefaultCadEngine()
        viewportTransform = CadViewportTransform(
            scale = 1.0f,
            panX = 0f,
            panY = 0f
        )

        val layerVisible = CadLayer(
            id = "layer_vis",
            name = "VISIBLE",
            colorArgb = 0xFFFFFFFFL,
            isVisible = true,
            isLocked = false
        )
        val layerLocked = CadLayer(
            id = "layer_locked",
            name = "LOCKED",
            colorArgb = 0xFFEF4444L,
            isVisible = true,
            isLocked = true
        )

        // 1. Line: (0, 0) to (100, 0)
        lineEntity = CadEntity.Line(
            id = "line_test",
            layerId = "layer_vis",
            start = CadPoint2D(0f, 0f),
            end = CadPoint2D(100f, 0f)
        )

        // 2. Polyline: (0, 100) -> (50, 150) -> (100, 100)
        polylineEntity = CadEntity.Polyline(
            id = "poly_test",
            layerId = "layer_vis",
            points = listOf(
                CadPoint2D(0f, 100f),
                CadPoint2D(50f, 150f),
                CadPoint2D(100f, 100f)
            ),
            isClosed = false
        )

        // 3. Circle: center (200, 200), radius 50
        circleEntity = CadEntity.Circle(
            id = "circle_test",
            layerId = "layer_vis",
            center = CadPoint2D(200f, 200f),
            radius = 50f
        )

        // 4. Arc: center (300, 300), radius 50, start 0 deg, end 90 deg
        arcEntity = CadEntity.Arc(
            id = "arc_test",
            layerId = "layer_vis",
            center = CadPoint2D(300f, 300f),
            radius = 50f,
            startAngleDeg = 0f,
            sweepAngleDeg = 90f
        )

        // 5. Text: origin (400, 400), text "ELEVATION"
        textEntity = CadEntity.Text(
            id = "text_test",
            layerId = "layer_vis",
            position = CadPoint2D(400f, 400f),
            text = "ELEVATION",
            textHeight = 20f
        )

        val lockedLine = CadEntity.Line(
            id = "line_locked",
            layerId = "layer_locked",
            start = CadPoint2D(500f, 500f),
            end = CadPoint2D(600f, 500f)
        )

        sampleDocument = CadDocument(
            title = "Test Drawing",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            layers = mapOf(
                layerVisible.id to layerVisible,
                layerLocked.id to layerLocked
            ),
            entities = listOf(lineEntity, polylineEntity, circleEntity, arcEntity, textEntity, lockedLine)
        )
    }

    @Test
    fun testHitTestLine() {
        // Direct hit on line
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(50f, 0f), lineEntity, 5f))
        // Near hit within tolerance (5f)
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(50f, 3f), lineEntity, 5f))
        // Miss beyond tolerance
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(50f, 10f), lineEntity, 5f))
        // Miss past start/end
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(120f, 0f), lineEntity, 5f))
    }

    @Test
    fun testHitTestPolyline() {
        // Hit on first segment (0,100) -> (50,150) at (25, 125)
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(25f, 125f), polylineEntity, 5f))
        // Hit on second segment (50,150) -> (100,100) at (75, 125)
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(75f, 125f), polylineEntity, 5f))
        // Miss outside tolerance
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(25f, 140f), polylineEntity, 5f))
    }

    @Test
    fun testHitTestCircle() {
        // Hit on circumference: center is (200, 200), radius is 50 -> (250, 200) is on edge
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(250f, 200f), circleEntity, 5f))
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(200f, 250f), circleEntity, 5f))
        // Hit near edge
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(252f, 200f), circleEntity, 5f))
        // Miss at center (CAD circles are hollow outlines, not solid discs)
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(200f, 200f), circleEntity, 5f))
        // Miss outside circle
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(300f, 200f), circleEntity, 5f))
    }

    @Test
    fun testHitTestArc() {
        // Center (300, 300), radius 50, angle 0..90 deg
        // (350, 300) is at 0 deg (hit)
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(350f, 300f), arcEntity, 5f))
        // (300, 350) is at 90 deg (hit)
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(300f, 350f), arcEntity, 5f))
        // (250, 300) is at 180 deg (on full circle, but NOT within 0..90 deg arc span -> miss)
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(250f, 300f), arcEntity, 5f))
    }

    @Test
    fun testHitTestText() {
        // Position (400, 400), height 20, approx width ~ 9 * 12 = 108
        // Hit inside bounding area
        assertTrue(hitTestEngine.isEntityHit(CadPoint2D(420f, 410f), textEntity, 5f))
        // Miss far outside
        assertFalse(hitTestEngine.isEntityHit(CadPoint2D(400f, 500f), textEntity, 5f))
    }

    @Test
    fun testSingleTapSelectionAndDeselect() {
        var state = SelectionState()

        // Tap on line at world (50, 0)
        state = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(50f, 0f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = state
        )
        assertEquals(1, state.count)
        assertTrue(state.isSelected("line_test"))

        // Tap on circle circumference at world (250, 200) without multi-select replaces selection
        state = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(250f, 200f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = state
        )
        assertEquals(1, state.count)
        assertFalse(state.isSelected("line_test"))
        assertTrue(state.isSelected("circle_test"))

        // Tap empty space deselects
        state = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(999f, 999f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = state
        )
        assertEquals(0, state.count)
        assertFalse(state.hasSelection)
    }

    @Test
    fun testMultiSelectToggle() {
        var state = SelectionState(isMultiSelectEnabled = true)

        // Select line
        state = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(50f, 0f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = state,
            toggle = true
        )
        assertEquals(1, state.count)

        // Add circle
        state = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(250f, 200f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = state,
            toggle = true
        )
        assertEquals(2, state.count)
        assertTrue(state.isSelected("line_test"))
        assertTrue(state.isSelected("circle_test"))

        // Tap line again to deselect it in multi-select mode
        state = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(50f, 0f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = state,
            toggle = true
        )
        assertEquals(1, state.count)
        assertFalse(state.isSelected("line_test"))
        assertTrue(state.isSelected("circle_test"))
    }

    @Test
    fun testSelectAllAndClear() {
        val stateAll = selectionManager.selectAll(
            document = sampleDocument,
            currentState = SelectionState(),
            visibleOnly = true
        )
        // All 6 entities are visible
        assertEquals(sampleDocument.entities.size, stateAll.count)

        val stateCleared = selectionManager.clearSelection(stateAll)
        assertEquals(0, stateCleared.count)
        assertFalse(stateCleared.hasSelection)
    }

    @Test
    fun testWindowSelection() {
        // Window selection: dragged left to right -> startX < endX
        // Box covering (-10, -10) to (120, 50) strictly contains line (0,0)-(100,0)
        // but does NOT contain circle (200, 200)
        val windowBox = SelectionBox(
            screenStart = CadPoint2D(-10f, -10f),
            screenCurrent = CadPoint2D(120f, 50f),
            worldStart = CadPoint2D(-10f, -10f),
            worldCurrent = CadPoint2D(120f, 50f)
        )
        assertTrue(windowBox.isWindow)
        assertFalse(windowBox.isCrossing)

        val selected = selectionManager.applyBoxSelection(
            box = windowBox,
            document = sampleDocument,
            currentState = SelectionState(),
            additive = false
        )
        assertEquals(1, selected.count)
        assertTrue(selected.isSelected("line_test"))
        assertFalse(selected.isSelected("circle_test"))
    }

    @Test
    fun testCrossingSelection() {
        // Crossing selection: dragged right to left -> screenCurrent.x < screenStart.x
        // Box partially overlapping circle at (200, 200) r=50 (e.g. from 260 down to 180)
        val crossingBox = SelectionBox(
            screenStart = CadPoint2D(260f, 220f),
            screenCurrent = CadPoint2D(180f, 180f),
            worldStart = CadPoint2D(260f, 220f),
            worldCurrent = CadPoint2D(180f, 180f)
        )
        assertTrue(crossingBox.isCrossing)
        assertFalse(crossingBox.isWindow)

        val selected = selectionManager.applyBoxSelection(
            box = crossingBox,
            document = sampleDocument,
            currentState = SelectionState(),
            additive = false
        )
        assertTrue(selected.isSelected("circle_test"))
    }

    @Test
    fun testSelectionGripsGeneration() {
        // Select line
        val lineState = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(50f, 0f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = SelectionState()
        )
        val grips = lineState.grips

        // Line should have START, END, MIDPOINT grips
        assertTrue(grips.any { it.type == GripType.START })
        assertTrue(grips.any { it.type == GripType.END })
        assertTrue(grips.any { it.type == GripType.MIDPOINT })

        // Select circle
        val circleState = selectionManager.selectAt(
            screenPoint = viewportTransform.worldToScreen(CadPoint2D(250f, 200f)),
            transform = viewportTransform,
            document = sampleDocument,
            currentState = SelectionState()
        )
        val circleGrips = circleState.grips

        // Circle should have Center and 4 Quadrants
        assertTrue(circleGrips.any { it.type == GripType.CENTER })
        assertEquals(4, circleGrips.count { it.type == GripType.QUADRANT })
    }

    @Test
    fun testBulkOperationsOnSelectedEntities() {
        val editEngine = cadEngine.editor

        // 1. Move Line by (10, 20)
        val movedDoc = editEngine.moveEntities(sampleDocument, setOf("line_test"), 10f, 20f)
        val movedLine = movedDoc.entities.find { it.id == "line_test" } as CadEntity.Line
        assertEquals(10f, movedLine.start.x, 0.01f)
        assertEquals(20f, movedLine.start.y, 0.01f)
        assertEquals(110f, movedLine.end.x, 0.01f)
        assertEquals(20f, movedLine.end.y, 0.01f)

        // 2. Cannot move entity on locked layer
        val lockedAttempt = editEngine.moveEntities(sampleDocument, setOf("line_locked"), 50f, 50f)
        val lockedLine = lockedAttempt.entities.find { it.id == "line_locked" } as CadEntity.Line
        assertEquals(500f, lockedLine.start.x, 0.01f)

        // 3. Copy circle by (100, 100)
        val (copiedDoc, copiedIds) = editEngine.copyEntities(sampleDocument, setOf("circle_test"), 100f, 100f)
        assertEquals(sampleDocument.entities.size + 1, copiedDoc.entities.size)
        assertEquals(1, copiedIds.size)

        // 4. Delete line
        val deletedDoc = editEngine.deleteEntities(sampleDocument, setOf("line_test"))
        assertNull(deletedDoc.entities.find { it.id == "line_test" })
    }
}

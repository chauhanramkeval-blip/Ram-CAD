package com.example.cad.engine.snap

import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.cad.model.CadViewportTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CadObjectSnapTest {

    private lateinit var snapEngine: CadSnapEngine
    private lateinit var defaultSettings: CadOsnapSettings
    private val transform = CadViewportTransform(panX = 0f, panY = 0f, scale = 1.0f)

    @Before
    fun setup() {
        snapEngine = CadSnapEngine()
        defaultSettings = CadOsnapSettings(
            isEnabled = true,
            snapToleranceScreenPx = 30f
        )
    }

    private fun createDoc(entities: List<CadEntity>): CadDocument {
        return CadDocument(
            title = "SnapTest",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            entities = entities,
            layers = mapOf("0" to CadLayer("0", "0", 0xFFFFFFFF, true, false))
        )
    }

    @Test
    fun testEndpointSnapOnLine() {
        val line = CadEntity.Line(id = "line1", layerId = "0", start = CadPoint2D(100f, 100f), end = CadPoint2D(200f, 200f))
        val doc = createDoc(listOf(line))

        // Near start point (100, 100)
        val cursorStart = CadPoint2D(105f, 103f)
        val snapStart = snapEngine.findSnap(
            cursorWorld = cursorStart,
            cursorScreen = transform.worldToScreen(cursorStart),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapStart)
        assertEquals(CadSnapMode.ENDPOINT, snapStart!!.mode)
        assertEquals(100f, snapStart.point.x, 0.001f)
        assertEquals(100f, snapStart.point.y, 0.001f)

        // Near end point (200, 200)
        val cursorEnd = CadPoint2D(195f, 202f)
        val snapEnd = snapEngine.findSnap(
            cursorWorld = cursorEnd,
            cursorScreen = transform.worldToScreen(cursorEnd),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapEnd)
        assertEquals(CadSnapMode.ENDPOINT, snapEnd!!.mode)
        assertEquals(200f, snapEnd.point.x, 0.001f)
        assertEquals(200f, snapEnd.point.y, 0.001f)
    }

    @Test
    fun testMidpointSnapOnLine() {
        val line = CadEntity.Line(id = "line1", layerId = "0", start = CadPoint2D(0f, 0f), end = CadPoint2D(100f, 0f))
        val doc = createDoc(listOf(line))

        // Near midpoint (50, 0)
        val cursorMid = CadPoint2D(52f, 3f)
        val snapMid = snapEngine.findSnap(
            cursorWorld = cursorMid,
            cursorScreen = transform.worldToScreen(cursorMid),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapMid)
        assertEquals(CadSnapMode.MIDPOINT, snapMid!!.mode)
        assertEquals(50f, snapMid.point.x, 0.001f)
        assertEquals(0f, snapMid.point.y, 0.001f)
    }

    @Test
    fun testCenterAndQuadrantSnapOnCircle() {
        val circle = CadEntity.Circle(id = "circle1", layerId = "0", center = CadPoint2D(100f, 100f), radius = 50f)
        val doc = createDoc(listOf(circle))

        // Center snap near (100, 100)
        val cursorCenter = CadPoint2D(102f, 98f)
        val snapCenter = snapEngine.findSnap(
            cursorWorld = cursorCenter,
            cursorScreen = transform.worldToScreen(cursorCenter),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapCenter)
        assertEquals(CadSnapMode.CENTER, snapCenter!!.mode)
        assertEquals(100f, snapCenter.point.x, 0.001f)
        assertEquals(100f, snapCenter.point.y, 0.001f)

        // Quadrant snap near 0 degrees (150, 100)
        val cursorQuad0 = CadPoint2D(148f, 101f)
        val snapQuad0 = snapEngine.findSnap(
            cursorWorld = cursorQuad0,
            cursorScreen = transform.worldToScreen(cursorQuad0),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapQuad0)
        assertEquals(CadSnapMode.QUADRANT, snapQuad0!!.mode)
        assertEquals(150f, snapQuad0.point.x, 0.001f)
        assertEquals(100f, snapQuad0.point.y, 0.001f)

        // Quadrant snap near 90 degrees (100, 150)
        val cursorQuad90 = CadPoint2D(99f, 152f)
        val snapQuad90 = snapEngine.findSnap(
            cursorWorld = cursorQuad90,
            cursorScreen = transform.worldToScreen(cursorQuad90),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapQuad90)
        assertEquals(CadSnapMode.QUADRANT, snapQuad90!!.mode)
        assertEquals(100f, snapQuad90.point.x, 0.001f)
        assertEquals(150f, snapQuad90.point.y, 0.001f)
    }

    @Test
    fun testIntersectionSnap() {
        val line1 = CadEntity.Line(id = "l1", layerId = "0", start = CadPoint2D(0f, 50f), end = CadPoint2D(200f, 50f))
        val line2 = CadEntity.Line(id = "l2", layerId = "0", start = CadPoint2D(50f, 0f), end = CadPoint2D(50f, 200f))
        val doc = createDoc(listOf(line1, line2))

        // Near intersection (50, 50)
        val cursorInter = CadPoint2D(51f, 49f)
        val snapInter = snapEngine.findSnap(
            cursorWorld = cursorInter,
            cursorScreen = transform.worldToScreen(cursorInter),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        assertNotNull(snapInter)
        assertEquals(CadSnapMode.INTERSECTION, snapInter!!.mode)
        assertEquals(50f, snapInter.point.x, 0.001f)
        assertEquals(50f, snapInter.point.y, 0.001f)
    }

    @Test
    fun testPerpendicularSnapWithAnchor() {
        val line = CadEntity.Line(id = "l1", layerId = "0", start = CadPoint2D(0f, 100f), end = CadPoint2D(200f, 100f))
        val doc = createDoc(listOf(line))
        val anchor = CadPoint2D(50f, 0f)

        // Normal drop from (50, 0) onto y=100 is (50, 100)
        val cursorPerp = CadPoint2D(52f, 98f)
        val snapPerp = snapEngine.findSnap(
            cursorWorld = cursorPerp,
            cursorScreen = transform.worldToScreen(cursorPerp),
            document = doc,
            transform = transform,
            settings = defaultSettings,
            anchorPoint = anchor
        )
        assertNotNull(snapPerp)
        assertEquals(CadSnapMode.PERPENDICULAR, snapPerp!!.mode)
        assertEquals(50f, snapPerp.point.x, 0.001f)
        assertEquals(100f, snapPerp.point.y, 0.001f)
    }

    @Test
    fun testNearestSnapOnSegment() {
        val line = CadEntity.Line(id = "l1", layerId = "0", start = CadPoint2D(0f, 0f), end = CadPoint2D(100f, 0f))
        val doc = createDoc(listOf(line))

        val settingsNearestOnly = defaultSettings.copy(
            enabledModes = setOf(CadSnapMode.NEAREST)
        )
        val cursorNearest = CadPoint2D(23.4f, 4f)
        val snapNearest = snapEngine.findSnap(
            cursorWorld = cursorNearest,
            cursorScreen = transform.worldToScreen(cursorNearest),
            document = doc,
            transform = transform,
            settings = settingsNearestOnly
        )
        assertNotNull(snapNearest)
        assertEquals(CadSnapMode.NEAREST, snapNearest!!.mode)
        assertEquals(23.4f, snapNearest.point.x, 0.01f)
        assertEquals(0f, snapNearest.point.y, 0.001f)
    }

    @Test
    fun testIndividualSnapModeDisabling() {
        val line = CadEntity.Line(id = "l1", layerId = "0", start = CadPoint2D(0f, 0f), end = CadPoint2D(100f, 0f))
        val doc = createDoc(listOf(line))

        // Disable ENDPOINT and MIDPOINT, only allow CENTER
        val settingsNoEndpoint = defaultSettings.copy(
            enabledModes = setOf(CadSnapMode.CENTER)
        )

        val cursor = CadPoint2D(1f, 1f)
        val snapResult = snapEngine.findSnap(
            cursorWorld = cursor,
            cursorScreen = transform.worldToScreen(cursor),
            document = doc,
            transform = transform,
            settings = settingsNoEndpoint
        )
        assertNull(snapResult)
    }

    @Test
    fun testZoomAwareSnapTolerance() {
        val line = CadEntity.Line(id = "l1", layerId = "0", start = CadPoint2D(1000f, 1000f), end = CadPoint2D(2000f, 2000f))
        val doc = createDoc(listOf(line))

        // When zoomed way out (scale = 0.1f)
        // 30 screen pixels equals 30 / 0.1 = 300 drawing world units!
        val zoomedOutTransform = CadViewportTransform(panX = 0f, panY = 0f, scale = 0.1f)
        val cursorWorld = CadPoint2D(1150f, 1100f) // 180 units away from start in world
        val cursorScreen = zoomedOutTransform.worldToScreen(cursorWorld)

        val snapZoomedOut = snapEngine.findSnap(
            cursorWorld = cursorWorld,
            cursorScreen = cursorScreen,
            document = doc,
            transform = zoomedOutTransform,
            settings = defaultSettings // 30 px tolerance
        )
        assertNotNull(snapZoomedOut)
        assertEquals(CadSnapMode.ENDPOINT, snapZoomedOut!!.mode)
        assertEquals(1000f, snapZoomedOut.point.x, 0.001f)
        assertEquals(1000f, snapZoomedOut.point.y, 0.001f)

        // When zoomed in (scale = 5.0f)
        // 30 screen pixels equals 30 / 5.0 = 6 drawing world units!
        val zoomedInTransform = CadViewportTransform(panX = 0f, panY = 0f, scale = 5.0f)
        // Off the line by (50, 50) units -> 250px away in screen space
        val cursorFarZoomedIn = CadPoint2D(1050f, 1150f)
        val cursorScreenFar = zoomedInTransform.worldToScreen(cursorFarZoomedIn)

        val snapTooFar = snapEngine.findSnap(
            cursorWorld = cursorFarZoomedIn,
            cursorScreen = cursorScreenFar,
            document = doc,
            transform = zoomedInTransform,
            settings = defaultSettings // 30 px tolerance
        )
        assertNull(snapTooFar)
    }

    @Test
    fun testSpatialIndexPerformanceOnLargeDrawing() {
        // Create 200 lines across a 5000x5000 drawing area
        val lines: List<CadEntity> = (0 until 200).map { i ->
            val x = (i % 20) * 200f
            val y = (i / 20) * 200f
            CadEntity.Line(id = "l$i", layerId = "0", start = CadPoint2D(x, y), end = CadPoint2D(x + 100f, y + 100f))
        }
        val doc = createDoc(lines)

        val cursor = CadPoint2D(602f, 403f)
        val startTime = System.nanoTime()
        val snap = snapEngine.findSnap(
            cursorWorld = cursor,
            cursorScreen = transform.worldToScreen(cursor),
            document = doc,
            transform = transform,
            settings = defaultSettings
        )
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertNotNull(snap)
        assertEquals(CadSnapMode.ENDPOINT, snap!!.mode)
        assertEquals(600f, snap.point.x, 0.001f)
        assertEquals(400f, snap.point.y, 0.001f)
        assertTrue("Snap query on large drawing took $elapsedMs ms", elapsedMs < 50.0)
    }
}

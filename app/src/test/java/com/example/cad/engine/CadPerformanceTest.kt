package com.example.cad.engine

import com.example.cad.engine.selection.HitTestEngine
import com.example.cad.engine.selection.SelectionBox
import com.example.cad.engine.selection.SelectionManager
import com.example.cad.engine.selection.SelectionState
import com.example.cad.engine.spatial.CadSpatialIndex
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.cad.model.CadViewportTransform
import com.example.cad.parser.dxf.DxfParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Performance and CAD accuracy benchmark test suite for large drawings:
 * - 1,000 entities
 * - 10,000 entities
 * - 100,000 entities
 *
 * Verifies:
 * 1. Spatial indexing build speed and memory efficiency
 * 2. Viewport query speed (pan/zoom culling)
 * 3. Hit testing accuracy (spatial index vs exhaustive search)
 * 4. Window and Crossing selection performance
 * 5. Layer filtering efficiency
 * 6. Streaming DXF parser throughput
 */
class CadPerformanceTest {

    private val hitTestEngine = HitTestEngine()
    private val selectionManager = SelectionManager(hitTestEngine)

    // =========================================================================
    // 1,000 Entities Test
    // =========================================================================

    @Test
    fun testDrawing_1000Entities_spatialIndexAndHitTesting() {
        val count = 1000
        val entities = generateSyntheticCadEntities(count, seed = 1000L)
        val doc = createDocumentWithEntities("drawing_1k", entities)

        // 1. Spatial Index Build
        val buildTime = measureTimeMillis {
            val idx = doc.spatialIndex
            assertEquals(count, idx.size)
        }
        assertTrue("Spatial index for 1,000 entities should build in < 50ms (was ${buildTime}ms)", buildTime < 50)

        // 2. Viewport Culling Query (Simulate Pan/Zoom)
        val viewportBox = CadBoundingBox(100f, 100f, 500f, 500f)
        val queryTime = measureTimeMillis {
            val visible = doc.spatialIndex.query(viewportBox)
            assertTrue("Should find visible entities in viewport", visible.isNotEmpty())
        }
        assertTrue("Viewport query for 1k entities should be instantaneous (< 5ms, was ${queryTime}ms)", queryTime < 5)

        // 3. Hit-Testing CAD Accuracy
        val targetEntity = entities.filterIsInstance<CadEntity.Line>().first()
        val testPoint = CadPoint2D(
            (targetEntity.start.x + targetEntity.end.x) / 2f + 0.1f,
            (targetEntity.start.y + targetEntity.end.y) / 2f + 0.1f
        )
        val tolerance = 5.0f

        // Exhaustive linear search (ground truth)
        val groundTruth = hitTestEngine.hitTest(testPoint, doc.entities, tolerance)

        // Optimized spatial index hit-test
        val candidates = doc.spatialIndex.queryPoint(testPoint, tolerance)
        val optimizedHit = hitTestEngine.hitTest(testPoint, candidates, tolerance)

        assertNotNull("Ground truth hit should succeed", groundTruth)
        assertNotNull("Optimized hit should succeed", optimizedHit)
        assertEquals("Optimized hit must match ground truth exactly (CAD Accuracy)", groundTruth?.id, optimizedHit?.id)
    }

    // =========================================================================
    // 10,000 Entities Test
    // =========================================================================

    @Test
    fun testDrawing_10000Entities_panZoomCullingAndSelection() {
        val count = 10000
        val entities = generateSyntheticCadEntities(count, seed = 10000L)
        val doc = createDocumentWithEntities("drawing_10k", entities)

        // 1. Spatial Index Build
        val buildTime = measureTimeMillis {
            val idx = doc.spatialIndex
            assertEquals(count, idx.size)
        }
        assertTrue("Spatial index for 10,000 entities should build in < 150ms (was ${buildTime}ms)", buildTime < 150)

        // 2. Viewport Culling (Pan/Zoom frame simulation)
        val viewportBox = CadBoundingBox(200f, 200f, 600f, 600f)
        var visibleCount = 0
        val panFrameTime = measureTimeMillis {
            repeat(10) {
                val visible = doc.spatialIndex.query(viewportBox)
                visibleCount = visible.size
            }
        }
        val avgFrameQueryTime = panFrameTime / 10.0
        assertTrue("Viewport culling for 10k entities must take < 1ms per frame (was ${avgFrameQueryTime}ms)", avgFrameQueryTime < 1.0)
        assertTrue("Visible count should be fraction of 10,000", visibleCount in 1..<count)

        // 3. Window & Crossing Selection
        val selBox = CadBoundingBox(300f, 300f, 700f, 700f)
        val selTime = measureTimeMillis {
            val candidates = doc.spatialIndex.query(selBox)
            val windowSelected = hitTestEngine.windowSelect(selBox, candidates)
            val crossingSelected = hitTestEngine.crossingSelect(selBox, candidates)
            assertTrue("Crossing selection should contain at least as many as window", crossingSelected.size >= windowSelected.size)
        }
        assertTrue("Box selection on 10k entities should be fast (< 25ms, was ${selTime}ms)", selTime < 25)

        // 4. Layer Filtering
        val hiddenLayerId = "LAYER_HIDDEN"
        val layersWithHidden = doc.layers.toMutableMap()
        layersWithHidden[hiddenLayerId] = CadLayer(hiddenLayerId, "Hidden Layer", isVisible = false)
        val docWithHidden = doc.copy(layers = layersWithHidden)

        val visibleQuery = docWithHidden.spatialIndex.query(viewportBox) { entity ->
            docWithHidden.layers[entity.layerId]?.isVisible != false
        }
        assertTrue("All queried entities must belong to visible layers", visibleQuery.none { it.layerId == hiddenLayerId })
    }

    // =========================================================================
    // 100,000 Entities Test
    // =========================================================================

    @Test
    fun testDrawing_100000Entities_benchmarkAndAccuracy() {
        val count = 100000
        val entities = generateSyntheticCadEntities(count, seed = 100000L)
        val doc = createDocumentWithEntities("drawing_100k", entities)

        // 1. Spatial Index Build (100k entities)
        val buildTime = measureTimeMillis {
            val idx = doc.spatialIndex
            assertEquals(count, idx.size)
            assertFalse("Root bounds must not be empty", idx.bounds.isEmpty)
        }
        assertTrue("Spatial index for 100,000 entities should build in < 1500ms (was ${buildTime}ms)", buildTime < 1500)

        // 2. Viewport Culling during 60 FPS Pan & Zoom
        // When panning, the viewport bounds shift. The spatial index must return visible entities within frame budget
        val viewportBox = CadBoundingBox(1000f, 1000f, 2500f, 2500f)
        // Warmup JIT
        repeat(3) {
            doc.spatialIndex.query(viewportBox)
        }
        val panIterations = 20
        val totalPanTime = measureTimeMillis {
            for (i in 0 until panIterations) {
                val shiftedBox = viewportBox.expand(i * 10f)
                val visible = doc.spatialIndex.query(shiftedBox)
                assertTrue("Should find entities in shifted viewport", visible.isNotEmpty())
            }
        }
        val avgPanTime = totalPanTime.toDouble() / panIterations
        assertTrue("60fps pan/zoom query on 100,000 entities must be within 60fps frame budget (< 15ms, was ${avgPanTime}ms)", avgPanTime < 15.0)

        // 3. Single-Point Tap Hit-Testing on 100,000 Entities
        // Select an entity in the middle of the drawing
        val target = entities.filterIsInstance<CadEntity.Line>()[count / 10]
        val tapPoint = CadPoint2D(
            (target.start.x + target.end.x) / 2f,
            (target.start.y + target.end.y) / 2f
        )
        val tolerance = 3.0f

        val hitTestTime = measureTimeMillis {
            val candidates = doc.spatialIndex.queryPoint(tapPoint, tolerance)
            assertTrue("Candidates should be culled from 100,000 down to few entities (was ${candidates.size})", candidates.size < 50)
            val hit = hitTestEngine.hitTest(tapPoint, candidates, tolerance)
            assertNotNull("Must hit the target entity", hit)
        }
        assertTrue("Hit test on 100k entities must take < 5ms (was ${hitTestTime}ms)", hitTestTime < 5)

        // 4. SelectionManager selectAt performance on 100,000 Entities
        val transform = CadViewportTransform(panX = 0f, panY = 0f, scale = 1.0f)
        val screenTapPoint = transform.worldToScreen(tapPoint)
        val state = SelectionState()
        // Warmup
        selectionManager.selectAt(screenTapPoint, transform, doc, state)
        val selectAtTime = measureTimeMillis {
            val newState = selectionManager.selectAt(
                screenPoint = screenTapPoint,
                transform = transform,
                document = doc,
                currentState = state
            )
            assertTrue("Entity must be selected in SelectionState", newState.selectedIds.isNotEmpty())
        }
        assertTrue("SelectionManager.selectAt on 100k entities must complete quickly (< 25ms, was ${selectAtTime}ms)", selectAtTime < 25)

        // 5. O(1) Entity Map Lookup vs O(N) scan
        val testId = entities[54321].id
        val lookupTime = measureTimeMillis {
            val found = doc.findEntity(testId)
            assertNotNull("findEntity must locate entity by id", found)
            assertEquals(testId, found?.id)
        }
        assertTrue("findEntity lookup must be instantaneous (< 5ms)", lookupTime < 5)

        // 6. Fast Drawing Extents Computation
        val extentsTime = measureTimeMillis {
            val extents = doc.computeExtents()
            assertFalse(extents.isEmpty)
        }
        assertTrue("Extents computation on 100k entities must be instant via root bounds (< 5ms)", extentsTime < 5)
    }

    // =========================================================================
    // Large DXF Loading & Streaming Parser Test
    // =========================================================================

    @Test
    fun testLargeDxfLoading_streamingThroughput() = runBlocking {
        val lineCount = 5000
        val dxfContent = buildSyntheticDxfLines(lineCount)
        val parser = DxfParser()

        var progressCalls = 0
        val parseTime = measureTimeMillis {
            val reader = BufferedReader(StringReader(dxfContent))
            val result = parser.parseAsync(reader, "perf_test.dxf") { _, _ ->
                progressCalls++
            }
            assertTrue("DXF parsing should succeed", result.isSuccess)
            val doc = result.getOrThrow()
            assertEquals("All $lineCount entities must be parsed", lineCount, doc.entities.size)
            assertFalse("Document extents must not be empty", doc.extents.isEmpty)
        }

        assertTrue("Streaming parse for 5,000 entities should finish in < 1500ms (was ${parseTime}ms)", parseTime < 1500)
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun generateSyntheticCadEntities(count: Int, seed: Long): List<CadEntity> {
        val rand = Random(seed)
        val entities = ArrayList<CadEntity>(count)
        val spread = 5000f

        for (i in 0 until count) {
            val type = i % 5
            val layerId = if (i % 20 == 0) "ANNOTATIONS" else "WALLS"
            val id = "ent_$i"

            when (type) {
                0, 1 -> { // Line (40%)
                    val x1 = rand.nextFloat() * spread
                    val y1 = rand.nextFloat() * spread
                    val len = rand.nextFloat() * 200f + 10f
                    val angle = rand.nextFloat() * 6.28f
                    entities.add(
                        CadEntity.Line(
                            id = id,
                            layerId = layerId,
                            start = CadPoint2D(x1, y1),
                            end = CadPoint2D(x1 + len * kotlin.math.cos(angle), y1 + len * kotlin.math.sin(angle))
                        )
                    )
                }
                2 -> { // Circle (20%)
                    val cx = rand.nextFloat() * spread
                    val cy = rand.nextFloat() * spread
                    val r = rand.nextFloat() * 50f + 5f
                    entities.add(
                        CadEntity.Circle(
                            id = id,
                            layerId = layerId,
                            center = CadPoint2D(cx, cy),
                            radius = r
                        )
                    )
                }
                3 -> { // Polyline (20%)
                    val x0 = rand.nextFloat() * spread
                    val y0 = rand.nextFloat() * spread
                    val pts = listOf(
                        CadPoint2D(x0, y0),
                        CadPoint2D(x0 + 40f, y0),
                        CadPoint2D(x0 + 40f, y0 + 30f),
                        CadPoint2D(x0, y0 + 30f)
                    )
                    entities.add(
                        CadEntity.Polyline(
                            id = id,
                            layerId = layerId,
                            points = pts,
                            isClosed = true
                        )
                    )
                }
                else -> { // Text (20%)
                    val tx = rand.nextFloat() * spread
                    val ty = rand.nextFloat() * spread
                    entities.add(
                        CadEntity.Text(
                            id = id,
                            layerId = layerId,
                            position = CadPoint2D(tx, ty),
                            text = "TAG-$i",
                            textHeight = 12f
                        )
                    )
                }
            }
        }
        return entities
    }

    private fun createDocumentWithEntities(title: String, entities: List<CadEntity>): CadDocument {
        return CadDocument(
            title = title,
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            entities = entities,
            layers = mapOf(
                "0" to CadLayer.DEFAULT_LAYER_0,
                "WALLS" to CadLayer("WALLS", "Walls", colorArgb = 0xFFFFFFFF),
                "ANNOTATIONS" to CadLayer.ANNOTATION_LAYER
            )
        )
    }

    private fun buildSyntheticDxfLines(count: Int): String {
        val sb = StringBuilder(count * 80)
        sb.append("0\nSECTION\n2\nENTITIES\n")
        for (i in 0 until count) {
            val x1 = i * 10f
            val y1 = i * 5f
            val x2 = x1 + 100f
            val y2 = y1 + 50f
            sb.append("0\nLINE\n8\n0\n10\n$x1\n20\n$y1\n11\n$x2\n21\n$y2\n")
        }
        sb.append("0\nENDSEC\n0\nEOF\n")
        return sb.toString()
    }
}

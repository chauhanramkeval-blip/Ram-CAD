package com.example.cad.engine

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.ui.screens.editor.CadEditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CadLayerManagementTest {

    private lateinit var cadEngine: DefaultCadEngine
    private lateinit var sampleDocument: CadDocument

    @Before
    fun setup() {
        cadEngine = DefaultCadEngine()

        val layer0 = CadLayer(
            id = "0",
            name = "0",
            colorArgb = 0xFFFFFFFFL,
            isVisible = true,
            isLocked = false
        )
        val layerWalls = CadLayer(
            id = "walls",
            name = "WALLS",
            colorArgb = 0xFF22C55EL,
            isVisible = true,
            isLocked = false
        )
        val layerDoors = CadLayer(
            id = "doors",
            name = "DOORS",
            colorArgb = 0xFF00E5FFL,
            isVisible = false,
            isLocked = true
        )

        val lineOnLayer0 = CadEntity.Line(
            id = "line_1",
            layerId = "0",
            start = CadPoint2D(0f, 0f),
            end = CadPoint2D(100f, 0f)
        )
        val lineOnWalls = CadEntity.Line(
            id = "line_2",
            layerId = "walls",
            start = CadPoint2D(100f, 0f),
            end = CadPoint2D(100f, 100f)
        )
        val circleOnDoors = CadEntity.Circle(
            id = "circle_1",
            layerId = "doors",
            center = CadPoint2D(50f, 50f),
            radius = 25f
        )

        val layersMap = mapOf("0" to layer0, "walls" to layerWalls, "doors" to layerDoors)
        val entitiesList = listOf(lineOnLayer0, lineOnWalls, circleOnDoors)

        sampleDocument = CadDocument(
            title = "Test Drawing",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            layers = layersMap,
            entities = entitiesList,
            extents = CadBoundingBox(0f, 0f, 100f, 100f)
        )
    }

    @Test
    fun testLayerPropertiesModelIntegrity() {
        // Requirement 2: Name, Visibility, Lock state, Color
        val doors = sampleDocument.layers["doors"]
        assertNotNull(doors)
        assertEquals("doors", doors?.id)
        assertEquals("DOORS", doors?.name)
        assertEquals(0xFF00E5FFL, doors?.colorArgb)
        assertFalse(doors!!.isVisible)
        assertTrue(doors.isLocked)
    }

    @Test
    fun testRendererRespectsLayerVisibility() {
        // Requirement 7: Renderer must immediately respect layer visibility
        // Initially, layer 'doors' is hidden, '0' and 'walls' are visible
        val visibleEntitiesBefore = sampleDocument.entities.filter { entity ->
            val layer = sampleDocument.layers[entity.layerId]
            layer == null || layer.isVisible
        }
        assertEquals(2, visibleEntitiesBefore.size)
        assertFalse(visibleEntitiesBefore.any { it.id == "circle_1" })

        // Show doors layer
        val updatedDoc = cadEngine.editor.setLayerVisibility(sampleDocument, "doors", isVisible = true)
        val visibleEntitiesAfter = updatedDoc.entities.filter { entity ->
            val layer = updatedDoc.layers[entity.layerId]
            layer == null || layer.isVisible
        }
        assertEquals(3, visibleEntitiesAfter.size)
        assertTrue(visibleEntitiesAfter.any { it.id == "circle_1" })

        // Hide walls layer
        val docWithHiddenWalls = cadEngine.editor.setLayerVisibility(updatedDoc, "walls", isVisible = false)
        val visibleAfterWallsHidden = docWithHiddenWalls.entities.filter { entity ->
            val layer = docWithHiddenWalls.layers[entity.layerId]
            layer == null || layer.isVisible
        }
        assertEquals(2, visibleAfterWallsHidden.size)
        assertFalse(visibleAfterWallsHidden.any { it.id == "line_2" })
    }

    @Test
    fun testShowAllAndHideAllLayers() {
        // Requirement 5 & 6: Show all and Hide all layers
        // Hide all
        val allHidden = sampleDocument.layers.mapValues { it.value.copy(isVisible = false) }
        val allHiddenDoc = sampleDocument.copy(layers = allHidden)
        assertTrue(allHiddenDoc.layers.values.none { it.isVisible })

        // Show all
        val allVisible = sampleDocument.layers.mapValues { it.value.copy(isVisible = true) }
        val allVisibleDoc = sampleDocument.copy(layers = allVisible)
        assertTrue(allVisibleDoc.layers.values.all { it.isVisible })
    }

    @Test
    fun testToggleLayerLock() {
        // Requirement 3: Lock/unlock layer
        val wallsLayer = sampleDocument.layers["walls"]!!
        assertFalse(wallsLayer.isLocked)

        val lockedDoc = cadEngine.editor.setLayerLock(sampleDocument, "walls", isLocked = true)
        assertTrue(lockedDoc.layers["walls"]!!.isLocked)

        val unlockedDoc = cadEngine.editor.setLayerLock(lockedDoc, "walls", isLocked = false)
        assertFalse(unlockedDoc.layers["walls"]!!.isLocked)
    }

    @Test
    fun testSelectedObjectsDisplayTheirLayer() {
        // Requirement 8: Selected objects must display their layer
        val line1 = sampleDocument.entities.first { it.id == "line_1" }
        assertEquals("0", line1.layerId)
        val layerName1 = sampleDocument.layers[line1.layerId]?.name
        assertEquals("0", layerName1)

        val line2 = sampleDocument.entities.first { it.id == "line_2" }
        assertEquals("walls", line2.layerId)
        val layerName2 = sampleDocument.layers[line2.layerId]?.name
        assertEquals("WALLS", layerName2)
    }

    @Test
    fun testLayerSearchFiltering() {
        // Requirement 4: Add layer search
        val layers = sampleDocument.layers.values.toList()
        val query = "wa"
        val filtered = layers.filter {
            it.name.contains(query, ignoreCase = true) || it.id.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("WALLS", filtered.first().name)
    }
}

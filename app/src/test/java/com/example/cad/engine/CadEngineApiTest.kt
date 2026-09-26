package com.example.cad.engine

import com.example.cad.engine.api.CadEntityFilter
import com.example.cad.engine.api.CadOpenOptions
import com.example.cad.engine.api.CadRenderContext
import com.example.cad.engine.core.StandardCadEngineApi
import com.example.cad.engine.mock.MockCadEngine
import com.example.cad.engine.nativebridge.NativeEntityDto
import com.example.cad.engine.nativebridge.NativeLayerDto
import com.example.cad.model.CadEntity
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CadEngineApiTest {

    @Test
    fun testMockCadEngineLifecycle() = runBlocking {
        val mockEngine = MockCadEngine()

        // 1. openDrawing
        val sessionResult = mockEngine.openDrawing(
            path = "virtual/MechanicalFlange.dwg",
            options = CadOpenOptions()
        )
        assertTrue(sessionResult.isSuccess)
        val session = sessionResult.getOrThrow()
        assertNotNull(session.sessionId)
        assertEquals(0xDEADBEEFL, session.nativeHandle)

        // 2. getLayers
        val layersResult = mockEngine.getLayers(session)
        assertTrue(layersResult.isSuccess)
        val layers = layersResult.getOrThrow()
        assertTrue(layers.isNotEmpty())

        // 3. getEntities
        val entitiesResult = mockEngine.getEntities(session, CadEntityFilter.All)
        assertTrue(entitiesResult.isSuccess)
        val initialEntities = entitiesResult.getOrThrow()
        assertTrue(initialEntities.isNotEmpty())

        // 4. createEntity
        val newLine = CadEntity.Line(
            id = "",
            layerId = layers.first().id,
            start = CadPoint2D(0f, 0f),
            end = CadPoint2D(100f, 100f)
        )
        val createResult = mockEngine.createEntity(session, newLine)
        assertTrue(createResult.isSuccess)
        val createdLine = createResult.getOrThrow()
        assertTrue(createdLine.id.isNotBlank())

        // Verify entity count increased
        val updatedEntities = mockEngine.getEntities(session, CadEntityFilter.All).getOrThrow()
        assertEquals(initialEntities.size + 1, updatedEntities.size)

        // 5. modifyEntity
        val modifiedLine = (createdLine as CadEntity.Line).copy(end = CadPoint2D(200f, 200f))
        val modifyResult = mockEngine.modifyEntity(session, modifiedLine)
        assertTrue(modifyResult.isSuccess)

        // 6. deleteEntity
        val deleteResult = mockEngine.deleteEntity(session, createdLine.id)
        assertTrue(deleteResult.isSuccess)
        assertTrue(deleteResult.getOrThrow())

        // 7. saveDrawing
        val saveResult = mockEngine.saveDrawing(session)
        assertTrue(saveResult.isSuccess)

        // 8. closeDrawing
        val closeResult = mockEngine.closeDrawing(session)
        assertTrue(closeResult.isSuccess)
    }

    @Test
    fun testNativeLayerDtoRoundTrip() {
        val layer = CadLayer(
            id = "layer_outline",
            name = "WALLS",
            colorArgb = 0xFF00FF00L,
            isVisible = true,
            isLocked = false,
            lineWeight = 0.5f
        )
        val dto = NativeLayerDto.fromCadLayer(layer)
        assertEquals("layer_outline", dto.id)
        assertEquals("WALLS", dto.name)
        assertTrue(dto.isVisible)

        val restored = dto.toCadLayer()
        assertEquals(layer.id, restored.id)
        assertEquals(layer.name, restored.name)
        assertEquals(layer.colorArgb, restored.colorArgb)
    }

    @Test
    fun testNativeEntityDtoRoundTrip() {
        val line = CadEntity.Line(
            id = "line_42",
            layerId = "0",
            start = CadPoint2D(10.5f, 20.5f),
            end = CadPoint2D(100.0f, 200.0f),
            colorArgb = 0xFFFF0000L,
            strokeWidth = 2.0f
        )

        val dto = NativeEntityDto.fromCadEntity(line)
        assertEquals("line_42", dto.id)
        assertEquals(NativeEntityDto.TYPE_LINE, dto.type)
        assertEquals("0", dto.layerName)
        assertEquals(4, dto.coordinates.size)

        val restored = dto.toCadEntity()
        assertNotNull(restored)
        assertTrue(restored is CadEntity.Line)
        val restoredLine = restored as CadEntity.Line
        assertEquals(10.5f, restoredLine.start.x, 0.001f)
        assertEquals(20.5f, restoredLine.start.y, 0.001f)
        assertEquals(100.0f, restoredLine.end.x, 0.001f)
        assertEquals(200.0f, restoredLine.end.y, 0.001f)
    }
}

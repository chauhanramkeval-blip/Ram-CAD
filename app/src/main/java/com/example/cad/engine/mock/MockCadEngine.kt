package com.example.cad.engine.mock

import com.example.cad.engine.SampleCadDrawings
import com.example.cad.engine.api.CadDrawingSession
import com.example.cad.engine.api.CadEngineApi
import com.example.cad.engine.api.CadEngineBackendType
import com.example.cad.engine.api.CadEntityFilter
import com.example.cad.engine.api.CadEntityNotFoundException
import com.example.cad.engine.api.CadOpenOptions
import com.example.cad.engine.api.CadRenderContext
import com.example.cad.engine.api.CadSessionClosedException
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Mock CAD Engine implementation specifically designed for development and automated testing.
 *
 * Fulfills Requirement 7 ("Create mock implementations only for development/testing")
 * and Requirement 6 ("Do NOT pretend that DWG support works without a real DWG engine").
 *
 * This engine explicitly declares itself as a development mock and provides deterministic
 * in-memory geometry without connecting to proprietary C++ DWG libraries.
 */
class MockCadEngine(
    override val engineName: String = "Mock CAD Engine (Development & Testing Only)"
) : CadEngineApi {

    override val supportedFormats: Set<CadFormat> = setOf(CadFormat.DWG, CadFormat.DXF, CadFormat.SVG)
    override val backendType: CadEngineBackendType = CadEngineBackendType.MOCK_TESTING

    private val sessions = ConcurrentHashMap<String, CadDrawingSession>()

    override suspend fun openDrawing(path: String, options: CadOpenOptions): Result<CadDrawingSession> {
        val fileName = File(path).name
        val format = when {
            fileName.endsWith(".dwg", ignoreCase = true) -> CadFormat.DWG
            fileName.endsWith(".svg", ignoreCase = true) -> CadFormat.SVG
            else -> CadFormat.DXF
        }

        // Generate synthetic mock CAD document for testing
        val doc = when {
            fileName.contains("Mech", ignoreCase = true) || fileName.contains("Flange", ignoreCase = true) -> {
                SampleCadDrawings.createMechanicalFlangeAssembly()
            }
            fileName.contains("Elec", ignoreCase = true) || fileName.contains("Substation", ignoreCase = true) -> {
                SampleCadDrawings.createElectricalSchematic()
            }
            else -> {
                SampleCadDrawings.createArchitecturalFloorPlan()
            }
        }.copy(
            title = "[DEV MOCK] $fileName",
            format = format,
            units = options.targetUnits ?: CadUnit.MILLIMETERS,
            isReadOnly = options.readOnly
        )

        val session = CadDrawingSession(
            sessionId = UUID.randomUUID().toString(),
            filePath = path,
            title = doc.title,
            format = format,
            units = doc.units,
            isReadOnly = options.readOnly,
            nativeHandle = 0xDEADBEEFL, // Distinctive mock handle
            engineBackend = CadEngineBackendType.MOCK_TESTING,
            documentState = doc
        )

        sessions[session.sessionId] = session
        return Result.success(session)
    }

    override suspend fun closeDrawing(session: CadDrawingSession): Result<Unit> {
        sessions.remove(session.sessionId)
        return Result.success(Unit)
    }

    override fun render(session: CadDrawingSession, context: CadRenderContext) {
        // Mock rendering passes through to Compose Canvas
    }

    override suspend fun getLayers(session: CadDrawingSession): Result<List<CadLayer>> {
        val current = sessions[session.sessionId] ?: return Result.failure(CadSessionClosedException(session.sessionId))
        return Result.success(current.documentState.layers.values.toList())
    }

    override suspend fun getEntities(
        session: CadDrawingSession,
        filter: CadEntityFilter
    ): Result<List<CadEntity>> {
        val current = sessions[session.sessionId] ?: return Result.failure(CadSessionClosedException(session.sessionId))
        val all = current.documentState.entities
        val filtered = when (filter) {
            is CadEntityFilter.All -> all
            is CadEntityFilter.ByLayer -> all.filter { it.layerId == filter.layerId }
            is CadEntityFilter.ByType -> all.filter { it::class.simpleName.equals(filter.entityType, ignoreCase = true) }
            is CadEntityFilter.ByBoundingBox -> all.filter { filter.bounds.intersects(it.boundingBox) }
            is CadEntityFilter.VisibleOnly -> all.filter {
                val layer = current.documentState.layers[it.layerId]
                layer?.isVisible != false
            }
        }
        return Result.success(filtered)
    }

    override suspend fun createEntity(
        session: CadDrawingSession,
        entity: CadEntity
    ): Result<CadEntity> {
        val current = sessions[session.sessionId] ?: return Result.failure(CadSessionClosedException(session.sessionId))
        val assignedId = if (entity.id.isBlank()) "mock_ent_${UUID.randomUUID()}" else entity.id
        val committed = when (entity) {
            is CadEntity.Line -> entity.copy(id = assignedId)
            is CadEntity.Polyline -> entity.copy(id = assignedId)
            is CadEntity.Circle -> entity.copy(id = assignedId)
            is CadEntity.Arc -> entity.copy(id = assignedId)
            is CadEntity.Text -> entity.copy(id = assignedId)
            is CadEntity.Point -> entity.copy(id = assignedId)
            is CadEntity.Dimension -> entity.copy(id = assignedId)
            is CadEntity.Leader -> entity.copy(id = assignedId)
            is CadEntity.Arrow -> entity.copy(id = assignedId)
            is CadEntity.RevisionCloud -> entity.copy(id = assignedId)
        }

        val updatedEntities = current.documentState.entities + committed
        val updatedDoc = current.documentState.copy(
            entities = updatedEntities,
            extents = current.documentState.extents.include(committed.boundingBox)
        )
        sessions[session.sessionId] = current.copy(
            isDirty = true,
            documentState = updatedDoc
        )
        return Result.success(committed)
    }

    override suspend fun modifyEntity(
        session: CadDrawingSession,
        entity: CadEntity
    ): Result<CadEntity> {
        val current = sessions[session.sessionId] ?: return Result.failure(CadSessionClosedException(session.sessionId))
        val index = current.documentState.entities.indexOfFirst { it.id == entity.id }
        if (index == -1) {
            return Result.failure(CadEntityNotFoundException(entity.id))
        }

        val list = current.documentState.entities.toMutableList()
        list[index] = entity
        val updatedDoc = current.documentState.copy(entities = list)
        val withExtents = updatedDoc.copy(extents = updatedDoc.computeExtents())
        sessions[session.sessionId] = current.copy(
            isDirty = true,
            documentState = withExtents
        )
        return Result.success(entity)
    }

    override suspend fun deleteEntity(
        session: CadDrawingSession,
        entityId: String
    ): Result<Boolean> {
        val current = sessions[session.sessionId] ?: return Result.failure(CadSessionClosedException(session.sessionId))
        val exists = current.documentState.entities.any { it.id == entityId }
        if (!exists) return Result.success(false)

        val updatedEntities = current.documentState.entities.filterNot { it.id == entityId }
        val updatedDoc = current.documentState.copy(entities = updatedEntities)
        val withExtents = updatedDoc.copy(extents = updatedDoc.computeExtents())
        sessions[session.sessionId] = current.copy(
            isDirty = true,
            documentState = withExtents
        )
        return Result.success(true)
    }

    override suspend fun saveDrawing(
        session: CadDrawingSession,
        targetPath: String?,
        format: CadFormat?
    ): Result<Unit> {
        val current = sessions[session.sessionId] ?: return Result.failure(CadSessionClosedException(session.sessionId))
        sessions[session.sessionId] = current.copy(isDirty = false)
        return Result.success(Unit)
    }
}

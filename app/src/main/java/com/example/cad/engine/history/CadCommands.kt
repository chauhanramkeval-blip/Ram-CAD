package com.example.cad.engine.history

import com.example.cad.engine.edit.CadEditMath
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D

/**
 * Command for creating/adding a new entity to the CAD document.
 */
data class CreateEntityCommand(
    val entity: CadEntity,
    override val name: String = "Create ${entity.javaClass.simpleName.removePrefix("CadEntity$")}"
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 128 + entity.id.length * 2

    override fun execute(document: CadDocument): CadDocument {
        // Prevent duplicate addition
        if (document.entities.any { it.id == entity.id }) return document
        val newEntities = document.entities + entity
        return document.copy(
            entities = newEntities,
            extents = document.extents.include(entity.boundingBox)
        )
    }

    override fun undo(document: CadDocument): CadDocument {
        val newEntities = document.entities.filterNot { it.id == entity.id }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for creating/adding multiple entities at once (e.g. paste or multi-shape creation).
 */
data class CreateEntitiesCommand(
    val entities: List<CadEntity>,
    override val name: String = "Create ${entities.size} Entities"
) : CadCommand {
    private val entityIds = entities.map { it.id }.toSet()
    override val estimatedMemoryBytes: Int
        get() = 256 + entities.size * 128

    override fun execute(document: CadDocument): CadDocument {
        val existingIds = document.entities.map { it.id }.toSet()
        val toAdd = entities.filterNot { existingIds.contains(it.id) }
        val newEntities = document.entities + toAdd
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }

    override fun undo(document: CadDocument): CadDocument {
        val newEntities = document.entities.filterNot { entityIds.contains(it.id) }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for deleting entities from the CAD document.
 * Stores previous entities and their indices for precise restoration on undo.
 */
data class DeleteEntitiesCommand(
    val deletedEntitiesWithIndices: List<Pair<Int, CadEntity>>,
    override val name: String = if (deletedEntitiesWithIndices.size == 1) "Delete ${deletedEntitiesWithIndices.first().second.javaClass.simpleName}" else "Delete ${deletedEntitiesWithIndices.size} Entities"
) : CadCommand {
    private val deletedIds = deletedEntitiesWithIndices.map { it.second.id }.toSet()
    override val estimatedMemoryBytes: Int
        get() = 256 + deletedEntitiesWithIndices.size * 128

    override fun execute(document: CadDocument): CadDocument {
        val newEntities = document.entities.filterNot { deletedIds.contains(it.id) }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }

    override fun undo(document: CadDocument): CadDocument {
        val currentEntities = document.entities.toMutableList()
        // Restore in order of original index
        val sorted = deletedEntitiesWithIndices.sortedBy { it.first }
        for ((origIdx, entity) in sorted) {
            val insertIdx = origIdx.coerceIn(0, currentEntities.size)
            currentEntities.add(insertIdx, entity)
        }
        val newDoc = document.copy(entities = currentEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for moving entities by (deltaX, deltaY).
 * Pure delta math: undo moves by (-deltaX, -deltaY), redo moves by (+deltaX, +deltaY).
 * Extremely lightweight and memory safe.
 */
data class MoveEntitiesCommand(
    val entityIds: Set<String>,
    val deltaX: Float,
    val deltaY: Float,
    override val name: String = "Move ${entityIds.size} Entities"
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 128 + entityIds.size * 32

    override fun execute(document: CadDocument): CadDocument = applyDelta(document, deltaX, deltaY)

    override fun undo(document: CadDocument): CadDocument = applyDelta(document, -deltaX, -deltaY)

    private fun applyDelta(document: CadDocument, dx: Float, dy: Float): CadDocument {
        if (entityIds.isEmpty() || (dx == 0f && dy == 0f)) return document
        val delta = CadPoint2D(dx, dy)
        val newEntities = document.entities.map { entity ->
            if (!entityIds.contains(entity.id)) return@map entity
            val layer = document.layers[entity.layerId]
            if (layer?.isLocked == true) return@map entity
            when (entity) {
                is CadEntity.Line -> entity.copy(start = entity.start + delta, end = entity.end + delta)
                is CadEntity.Polyline -> entity.copy(points = entity.points.map { it + delta })
                is CadEntity.Circle -> entity.copy(center = entity.center + delta)
                is CadEntity.Arc -> entity.copy(center = entity.center + delta)
                is CadEntity.Text -> entity.copy(position = entity.position + delta)
                is CadEntity.Point -> entity.copy(position = entity.position + delta)
                is CadEntity.Dimension -> entity.copy(
                    start = entity.start + delta,
                    end = entity.end + delta,
                    textPoint = entity.textPoint + delta
                )
                is CadEntity.Leader -> entity.copy(
                    arrowPoint = entity.arrowPoint + delta,
                    kneePoint = entity.kneePoint + delta,
                    landingEndPoint = entity.landingEndPoint + delta
                )
                is CadEntity.Arrow -> entity.copy(
                    start = entity.start + delta,
                    end = entity.end + delta
                )
                is CadEntity.RevisionCloud -> entity.copy(
                    vertices = entity.vertices.map { it + delta }
                )
            }
        }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for rotating entities around a pivot center by angleDegrees.
 * Invertible operation: undo rotates by -angleDegrees.
 */
data class RotateEntitiesCommand(
    val entityIds: Set<String>,
    val center: CadPoint2D,
    val angleDegrees: Float,
    override val name: String = "Rotate ${entityIds.size} Entities (%.1f°)".format(angleDegrees)
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 128 + entityIds.size * 32

    override fun execute(document: CadDocument): CadDocument = applyRotation(document, angleDegrees)

    override fun undo(document: CadDocument): CadDocument = applyRotation(document, -angleDegrees)

    private fun applyRotation(document: CadDocument, angle: Float): CadDocument {
        if (entityIds.isEmpty() || angle % 360f == 0f) return document
        val newEntities = document.entities.map { entity ->
            if (!entityIds.contains(entity.id)) return@map entity
            val layer = document.layers[entity.layerId]
            if (layer?.isLocked == true) return@map entity
            CadEditMath.rotateEntity(entity, center, angle)
        }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for scaling entities around a base point by a scale factor.
 * Invertible operation: undo scales by (1.0f / factor).
 */
data class ScaleEntitiesCommand(
    val entityIds: Set<String>,
    val basePoint: CadPoint2D,
    val factor: Float,
    override val name: String = "Scale ${entityIds.size} Entities (%.2fx)".format(factor)
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 128 + entityIds.size * 32

    override fun execute(document: CadDocument): CadDocument = applyScale(document, factor)

    override fun undo(document: CadDocument): CadDocument {
        if (factor == 0f) return document
        return applyScale(document, 1.0f / factor)
    }

    private fun applyScale(document: CadDocument, scale: Float): CadDocument {
        if (entityIds.isEmpty() || scale == 1.0f) return document
        val newEntities = document.entities.map { entity ->
            if (!entityIds.contains(entity.id)) return@map entity
            val layer = document.layers[entity.layerId]
            if (layer?.isLocked == true) return@map entity
            CadEditMath.scaleEntity(entity, basePoint, scale)
        }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for modifying entity properties (color, strokeWidth, style, text content, etc.).
 */
data class ModifyPropertyCommand(
    val entityId: String,
    val oldEntity: CadEntity,
    val newEntity: CadEntity,
    val propertyDescription: String = "Property Modification",
    override val name: String = propertyDescription
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 256

    override fun execute(document: CadDocument): CadDocument = replaceEntity(document, newEntity)

    override fun undo(document: CadDocument): CadDocument = replaceEntity(document, oldEntity)

    private fun replaceEntity(document: CadDocument, entity: CadEntity): CadDocument {
        val newEntities = document.entities.map { if (it.id == entityId) entity else it }
        val newDoc = document.copy(entities = newEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for modifying an entity's assigned layer.
 */
data class ChangeEntityLayerCommand(
    val entityId: String,
    val oldLayerId: String,
    val newLayerId: String,
    override val name: String = "Change Layer ($oldLayerId → $newLayerId)"
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 128

    override fun execute(document: CadDocument): CadDocument = setLayer(document, newLayerId)

    override fun undo(document: CadDocument): CadDocument = setLayer(document, oldLayerId)

    private fun setLayer(document: CadDocument, layerId: String): CadDocument {
        val newEntities = document.entities.map { entity ->
            if (entity.id == entityId) {
                when (entity) {
                    is CadEntity.Point -> entity.copy(layerId = layerId)
                    is CadEntity.Line -> entity.copy(layerId = layerId)
                    is CadEntity.Polyline -> entity.copy(layerId = layerId)
                    is CadEntity.Circle -> entity.copy(layerId = layerId)
                    is CadEntity.Arc -> entity.copy(layerId = layerId)
                    is CadEntity.Text -> entity.copy(layerId = layerId)
                    is CadEntity.Dimension -> entity.copy(layerId = layerId)
                    is CadEntity.Leader -> entity.copy(layerId = layerId)
                    is CadEntity.Arrow -> entity.copy(layerId = layerId)
                    is CadEntity.RevisionCloud -> entity.copy(layerId = layerId)
                }
            } else entity
        }
        return document.copy(entities = newEntities)
    }
}

/**
 * Command for modifying layer states (visibility, lock, color, etc.).
 */
data class ModifyLayerStateCommand(
    val layerId: String,
    val oldLayer: CadLayer,
    val newLayer: CadLayer,
    override val name: String = "Layer '${oldLayer.name}' state changed"
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = 128

    override fun execute(document: CadDocument): CadDocument {
        val updatedLayers = document.layers + (layerId to newLayer)
        val newDoc = document.copy(layers = updatedLayers)
        return newDoc.copy(extents = newDoc.computeExtents())
    }

    override fun undo(document: CadDocument): CadDocument {
        val updatedLayers = document.layers + (layerId to oldLayer)
        val newDoc = document.copy(layers = updatedLayers)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Command for trimming or extending an entity (replacing original entity with new geometry).
 */
data class GeometryEditCommand(
    val originalEntity: CadEntity,
    val replacementEntities: List<CadEntity>,
    override val name: String = "Trim/Extend"
) : CadCommand {
    private val replacementIds = replacementEntities.map { it.id }.toSet()
    override val estimatedMemoryBytes: Int
        get() = 256 + replacementEntities.size * 128

    override fun execute(document: CadDocument): CadDocument {
        val filtered = document.entities.filterNot { it.id == originalEntity.id }
        val newDoc = document.copy(entities = filtered + replacementEntities)
        return newDoc.copy(extents = newDoc.computeExtents())
    }

    override fun undo(document: CadDocument): CadDocument {
        val filtered = document.entities.filterNot { replacementIds.contains(it.id) }
        val newDoc = document.copy(entities = filtered + originalEntity)
        return newDoc.copy(extents = newDoc.computeExtents())
    }
}

/**
 * Composite Command: Groups multiple related CAD operations into a single atomic undo/redo command.
 * Satisfies Requirement 3: "Group related operations into one undo command".
 */
data class CompositeCadCommand(
    val commands: List<CadCommand>,
    override val name: String = commands.firstOrNull()?.name ?: "Grouped Command"
) : CadCommand {
    override val estimatedMemoryBytes: Int
        get() = commands.sumOf { it.estimatedMemoryBytes } + 128

    override fun execute(document: CadDocument): CadDocument {
        var current = document
        for (cmd in commands) {
            current = cmd.execute(current)
        }
        return current
    }

    override fun undo(document: CadDocument): CadDocument {
        var current = document
        // Undo commands in reverse order
        for (cmd in commands.asReversed()) {
            current = cmd.undo(current)
        }
        return current
    }

    override fun redo(document: CadDocument): CadDocument {
        var current = document
        for (cmd in commands) {
            current = cmd.redo(current)
        }
        return current
    }
}

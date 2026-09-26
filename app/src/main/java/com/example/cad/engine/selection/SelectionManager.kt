package com.example.cad.engine.selection

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-level manager coordinating CAD entity selection, box selection modes,
 * grip handle generation, and bulk transformations (Move, Copy, Delete).
 */
class SelectionManager(
    val hitTestEngine: HitTestEngine = HitTestEngine()
) {

    /**
     * Executes tap selection given a screen coordinate and viewport transform.
     */
    fun selectAt(
        screenPoint: CadPoint2D,
        transform: CadViewportTransform,
        document: CadDocument,
        currentState: SelectionState,
        screenToleranceDp: Float = 24f,
        toggle: Boolean = currentState.isMultiSelectEnabled
    ): SelectionState {
        val worldPoint = transform.screenToWorld(screenPoint.x, screenPoint.y)
        val worldTolerance = (screenToleranceDp / transform.scale).coerceAtLeast(0.01f)

        val allLayersVisible = document.layers.values.all { it.isVisible }
        val hiddenLayers = if (allLayersVisible) emptySet() else document.layers.values.filter { !it.isVisible }.map { it.id }.toSet()

        val candidateEntities = if (document.entities.size > 32) {
            document.spatialIndex.queryPoint(worldPoint, worldTolerance) { entity ->
                allLayersVisible || entity.layerId !in hiddenLayers
            }
        } else {
            if (allLayersVisible) document.entities else document.entities.filter { it.layerId !in hiddenLayers }
        }

        val hit = hitTestEngine.hitTest(worldPoint, candidateEntities, worldTolerance)

        return if (hit != null) {
            val newSelectedIds = if (toggle) {
                if (currentState.selectedIds.contains(hit.id)) {
                    currentState.selectedIds - hit.id
                } else {
                    currentState.selectedIds + hit.id
                }
            } else {
                setOf(hit.id)
            }
            val primaryId = if (newSelectedIds.contains(hit.id)) hit.id else newSelectedIds.lastOrNull()
            val grips = computeGrips(document, newSelectedIds)

            currentState.copy(
                selectedIds = newSelectedIds,
                primarySelectedId = primaryId,
                activeBox = null,
                grips = grips
            )
        } else {
            // Tap on empty space: clear selection if not multi-select toggle mode
            if (toggle) {
                currentState.copy(activeBox = null)
            } else {
                clearSelection(currentState)
            }
        }
    }

    /**
     * Evaluates a completed selection box (Window or Crossing) against document entities.
     */
    fun applyBoxSelection(
        box: SelectionBox,
        document: CadDocument,
        currentState: SelectionState,
        additive: Boolean = currentState.isMultiSelectEnabled
    ): SelectionState {
        val allLayersVisible = document.layers.values.all { it.isVisible }
        val hiddenLayers = if (allLayersVisible) emptySet() else document.layers.values.filter { !it.isVisible }.map { it.id }.toSet()
        val boxBounds = box.worldBoundingBox

        val candidateEntities = if (document.entities.size > 32) {
            document.spatialIndex.query(boxBounds) { entity ->
                allLayersVisible || entity.layerId !in hiddenLayers
            }
        } else {
            if (allLayersVisible) document.entities else document.entities.filter { it.layerId !in hiddenLayers }
        }

        val matchedEntities = if (box.isCrossing) {
            hitTestEngine.crossingSelect(boxBounds, candidateEntities)
        } else {
            hitTestEngine.windowSelect(boxBounds, candidateEntities)
        }

        val matchedIds = matchedEntities.map { it.id }.toSet()
        val newSelectedIds = if (additive) {
            currentState.selectedIds + matchedIds
        } else {
            matchedIds
        }

        val primaryId = if (newSelectedIds.contains(currentState.primarySelectedId)) {
            currentState.primarySelectedId
        } else {
            newSelectedIds.lastOrNull()
        }

        val grips = computeGrips(document, newSelectedIds)

        return currentState.copy(
            selectedIds = newSelectedIds,
            primarySelectedId = primaryId,
            activeBox = null,
            grips = grips
        )
    }

    /**
     * Selects all visible entities in the document.
     */
    fun selectAll(
        document: CadDocument,
        currentState: SelectionState,
        visibleOnly: Boolean = true
    ): SelectionState {
        val eligibleEntities = document.entities.filter { entity ->
            if (!visibleOnly) return@filter true
            val layer = document.layers[entity.layerId]
            layer == null || layer.isVisible
        }

        val allIds = eligibleEntities.map { it.id }.toSet()
        val grips = computeGrips(document, allIds)

        return currentState.copy(
            selectedIds = allIds,
            primarySelectedId = allIds.firstOrNull(),
            activeBox = null,
            grips = grips
        )
    }

    /**
     * Clears all selections.
     */
    fun clearSelection(currentState: SelectionState = SelectionState()): SelectionState {
        return currentState.copy(
            selectedIds = emptySet(),
            primarySelectedId = null,
            activeBox = null,
            grips = emptyList()
        )
    }

    /**
     * Deselects a specific entity.
     */
    fun deselect(currentState: SelectionState, document: CadDocument, entityId: String): SelectionState {
        val newSelectedIds = currentState.selectedIds - entityId
        val primaryId = if (currentState.primarySelectedId == entityId) {
            newSelectedIds.lastOrNull()
        } else {
            currentState.primarySelectedId
        }
        val grips = computeGrips(document, newSelectedIds)
        return currentState.copy(
            selectedIds = newSelectedIds,
            primarySelectedId = primaryId,
            grips = grips
        )
    }

    /**
     * Explicitly sets the selected entity IDs.
     */
    fun setSelection(
        selectedIds: Set<String>,
        document: CadDocument,
        currentState: SelectionState
    ): SelectionState {
        val validIds = selectedIds.filter { document.findEntity(it) != null }.toSet()
        val grips = computeGrips(document, validIds)
        return currentState.copy(
            selectedIds = validIds,
            primarySelectedId = validIds.firstOrNull(),
            activeBox = null,
            grips = grips
        )
    }

    /**
     * Computes CAD selection handles (grips) for the selected entities.
     */
    fun computeGrips(document: CadDocument, selectedIds: Set<String>): List<SelectionGrip> {
        if (selectedIds.isEmpty()) return emptyList()

        val grips = mutableListOf<SelectionGrip>()
        val selectedEntities = selectedIds.mapNotNull { document.findEntity(it) }

        // Limit grips to at most 100 entities to ensure high rendering performance on mobile
        val entitiesToGrip = if (selectedEntities.size > 100) selectedEntities.take(100) else selectedEntities

        for (entity in entitiesToGrip) {
            when (entity) {
                is CadEntity.Point -> {
                    grips.add(SelectionGrip(entity.id, GripType.CENTER, entity.position))
                }
                is CadEntity.Line -> {
                    grips.add(SelectionGrip(entity.id, GripType.START, entity.start))
                    val mid = CadPoint2D((entity.start.x + entity.end.x) / 2f, (entity.start.y + entity.end.y) / 2f)
                    grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, mid))
                    grips.add(SelectionGrip(entity.id, GripType.END, entity.end))
                }
                is CadEntity.Polyline -> {
                    entity.points.forEachIndexed { index, pt ->
                        grips.add(SelectionGrip(entity.id, GripType.VERTEX, pt, index))
                    }
                    for (i in 0 until entity.points.size - 1) {
                        val mid = CadPoint2D(
                            (entity.points[i].x + entity.points[i + 1].x) / 2f,
                            (entity.points[i].y + entity.points[i + 1].y) / 2f
                        )
                        grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, mid, i))
                    }
                    if (entity.isClosed && entity.points.size > 2) {
                        val mid = CadPoint2D(
                            (entity.points.last().x + entity.points.first().x) / 2f,
                            (entity.points.last().y + entity.points.first().y) / 2f
                        )
                        grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, mid, entity.points.size - 1))
                    }
                }
                is CadEntity.Circle -> {
                    grips.add(SelectionGrip(entity.id, GripType.CENTER, entity.center))
                    val r = entity.radius
                    grips.add(SelectionGrip(entity.id, GripType.QUADRANT, CadPoint2D(entity.center.x + r, entity.center.y)))
                    grips.add(SelectionGrip(entity.id, GripType.QUADRANT, CadPoint2D(entity.center.x, entity.center.y + r)))
                    grips.add(SelectionGrip(entity.id, GripType.QUADRANT, CadPoint2D(entity.center.x - r, entity.center.y)))
                    grips.add(SelectionGrip(entity.id, GripType.QUADRANT, CadPoint2D(entity.center.x, entity.center.y - r)))
                }
                is CadEntity.Arc -> {
                    grips.add(SelectionGrip(entity.id, GripType.CENTER, entity.center))
                    val r = entity.radius
                    val startRad = Math.toRadians(entity.startAngleDeg.toDouble())
                    val midAngle = entity.startAngleDeg + entity.sweepAngleDeg / 2f
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val endAngle = entity.startAngleDeg + entity.sweepAngleDeg
                    val endRad = Math.toRadians(endAngle.toDouble())

                    grips.add(SelectionGrip(entity.id, GripType.START, CadPoint2D(
                        entity.center.x + r * cos(startRad).toFloat(),
                        entity.center.y + r * sin(startRad).toFloat()
                    )))
                    grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, CadPoint2D(
                        entity.center.x + r * cos(midRad).toFloat(),
                        entity.center.y + r * sin(midRad).toFloat()
                    )))
                    grips.add(SelectionGrip(entity.id, GripType.END, CadPoint2D(
                        entity.center.x + r * cos(endRad).toFloat(),
                        entity.center.y + r * sin(endRad).toFloat()
                    )))
                }
                is CadEntity.Text -> {
                    grips.add(SelectionGrip(entity.id, GripType.INSERTION, entity.position))
                    grips.add(SelectionGrip(entity.id, GripType.END, CadPoint2D(
                        entity.boundingBox.maxX,
                        entity.boundingBox.maxY
                    )))
                }
                is CadEntity.Dimension -> {
                    grips.add(SelectionGrip(entity.id, GripType.START, entity.start))
                    grips.add(SelectionGrip(entity.id, GripType.END, entity.end))
                    grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, entity.textPoint))
                }
                is CadEntity.Leader -> {
                    grips.add(SelectionGrip(entity.id, GripType.START, entity.arrowPoint))
                    grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, entity.kneePoint))
                    grips.add(SelectionGrip(entity.id, GripType.END, entity.landingEndPoint))
                }
                is CadEntity.Arrow -> {
                    grips.add(SelectionGrip(entity.id, GripType.START, entity.start))
                    grips.add(SelectionGrip(entity.id, GripType.END, entity.end))
                    val mid = CadPoint2D((entity.start.x + entity.end.x) / 2f, (entity.start.y + entity.end.y) / 2f)
                    grips.add(SelectionGrip(entity.id, GripType.MIDPOINT, mid))
                }
                is CadEntity.RevisionCloud -> {
                    entity.vertices.forEachIndexed { index, pt ->
                        grips.add(SelectionGrip(entity.id, GripType.VERTEX, pt, index))
                    }
                }
            }
        }
        return grips
    }

    // =========================================================================
    // Selection Mutation Transformations (Move, Copy, Delete)
    // =========================================================================

    /**
     * Translates selected entities by (deltaX, deltaY).
     */
    fun moveEntities(
        entities: List<CadEntity>,
        deltaX: Float,
        deltaY: Float,
        targetIds: Set<String>
    ): List<CadEntity> {
        val delta = CadPoint2D(deltaX, deltaY)
        return entities.map { entity ->
            if (!targetIds.contains(entity.id)) return@map entity
            when (entity) {
                is CadEntity.Point -> entity.copy(position = entity.position + delta)
                is CadEntity.Line -> entity.copy(start = entity.start + delta, end = entity.end + delta)
                is CadEntity.Polyline -> entity.copy(points = entity.points.map { it + delta })
                is CadEntity.Circle -> entity.copy(center = entity.center + delta)
                is CadEntity.Arc -> entity.copy(center = entity.center + delta)
                is CadEntity.Text -> entity.copy(position = entity.position + delta)
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
    }

    /**
     * Duplicates selected entities offset by (deltaX, deltaY), returning new entities list
     * along with the Set of newly created entity IDs.
     */
    fun copyEntities(
        entities: List<CadEntity>,
        deltaX: Float,
        deltaY: Float,
        targetIds: Set<String>
    ): Pair<List<CadEntity>, Set<String>> {
        val delta = CadPoint2D(deltaX, deltaY)
        val copiedIds = mutableSetOf<String>()
        val newEntities = mutableListOf<CadEntity>()

        for (entity in entities) {
            if (targetIds.contains(entity.id)) {
                val newId = "copy_${UUID.randomUUID().toString().take(8)}"
                copiedIds.add(newId)
                val cloned = when (entity) {
                    is CadEntity.Point -> entity.copy(id = newId, position = entity.position + delta)
                    is CadEntity.Line -> entity.copy(id = newId, start = entity.start + delta, end = entity.end + delta)
                    is CadEntity.Polyline -> entity.copy(id = newId, points = entity.points.map { it + delta })
                    is CadEntity.Circle -> entity.copy(id = newId, center = entity.center + delta)
                    is CadEntity.Arc -> entity.copy(id = newId, center = entity.center + delta)
                    is CadEntity.Text -> entity.copy(id = newId, position = entity.position + delta)
                    is CadEntity.Dimension -> entity.copy(
                        id = newId,
                        start = entity.start + delta,
                        end = entity.end + delta,
                        textPoint = entity.textPoint + delta
                    )
                    is CadEntity.Leader -> entity.copy(
                        id = newId,
                        arrowPoint = entity.arrowPoint + delta,
                        kneePoint = entity.kneePoint + delta,
                        landingEndPoint = entity.landingEndPoint + delta
                    )
                    is CadEntity.Arrow -> entity.copy(
                        id = newId,
                        start = entity.start + delta,
                        end = entity.end + delta
                    )
                    is CadEntity.RevisionCloud -> entity.copy(
                        id = newId,
                        vertices = entity.vertices.map { it + delta }
                    )
                }
                newEntities.add(cloned)
            }
        }

        return Pair(entities + newEntities, copiedIds)
    }

    /**
     * Deletes the target selected entities from the list.
     */
    fun deleteEntities(
        entities: List<CadEntity>,
        targetIds: Set<String>
    ): List<CadEntity> {
        return entities.filterNot { targetIds.contains(it.id) }
    }
}

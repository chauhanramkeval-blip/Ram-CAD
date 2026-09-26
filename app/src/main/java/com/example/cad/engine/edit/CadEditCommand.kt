package com.example.cad.engine.edit

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Supported CAD Interactive Editing Command Types.
 */
enum class EditOperationType(val displayName: String, val prompt: String) {
    MOVE("Move", "Drag or tap to place target displacement"),
    COPY("Copy", "Drag or tap to place copied entities"),
    ROTATE("Rotate", "Drag to rotate around pivot or enter angle"),
    SCALE("Scale", "Drag or enter factor to scale entities"),
    TRIM("Trim", "Tap segment between cutting intersections to trim"),
    EXTEND("Extend", "Tap near endpoint to extend to nearest boundary"),
    DELETE("Delete", "Delete selected entities")
}

/**
 * Command state management for active CAD editing operations.
 *
 * Guarantees that the underlying [CadDocument] is never modified until
 * the user confirms the command.
 */
sealed class EditCommandState {

    /** No editing operation is currently active. */
    object Idle : EditCommandState()

    /**
     * An editing operation is in-progress with live interactive parameters and dynamic previews.
     */
    data class Active(
        val operation: EditOperationType,
        val targetEntityIds: Set<String>,
        val basePoint: CadPoint2D,
        val currentPoint: CadPoint2D,
        val deltaX: Float = 0f,
        val deltaY: Float = 0f,
        val angleDeg: Float = 0f,
        val scaleFactor: Float = 1.0f,
        val previewEntities: List<CadEntity> = emptyList(),
        val trimRemovedSegment: CadEntity.Line? = null,
        val extendNewSegment: CadEntity.Line? = null,
        val statusPrompt: String = ""
    ) : EditCommandState() {

        val displacementLength: Float
            get() = basePoint.distanceTo(currentPoint)
    }

    companion object {

        /**
         * Initializes an active editing command from selected entities.
         */
        fun initialize(
            operation: EditOperationType,
            targetEntities: List<CadEntity>,
            allDocumentEntities: List<CadEntity>,
            initialPoint: CadPoint2D? = null
        ): EditCommandState {
            if (targetEntities.isEmpty()) return Idle

            // Compute center of bounding box for base point
            val bbox = targetEntities.fold(CadBoundingBox()) { box, entity ->
                box.include(entity.boundingBox)
            }
            val basePt = initialPoint ?: if (!bbox.isEmpty) {
                CadPoint2D(bbox.centerX, bbox.centerY)
            } else {
                when (val first = targetEntities.first()) {
                    is CadEntity.Line -> first.start
                    is CadEntity.Circle -> first.center
                    is CadEntity.Arc -> first.center
                    is CadEntity.Text -> first.position
                    is CadEntity.Point -> first.position
                    is CadEntity.Polyline -> first.points.firstOrNull() ?: CadPoint2D(0f, 0f)
                    is CadEntity.Dimension -> first.start
                    is CadEntity.Leader -> first.arrowPoint
                    is CadEntity.Arrow -> first.start
                    is CadEntity.RevisionCloud -> first.vertices.firstOrNull() ?: CadPoint2D(0f, 0f)
                }
            }

            val targetIds = targetEntities.map { it.id }.toSet()

            return when (operation) {
                EditOperationType.MOVE -> {
                    Active(
                        operation = operation,
                        targetEntityIds = targetIds,
                        basePoint = basePt,
                        currentPoint = basePt,
                        previewEntities = targetEntities,
                        statusPrompt = "Base: (%.1f, %.1f) - Drag or tap destination".format(basePt.x, basePt.y)
                    )
                }
                EditOperationType.COPY -> {
                    Active(
                        operation = operation,
                        targetEntityIds = targetIds,
                        basePoint = basePt,
                        currentPoint = basePt,
                        previewEntities = targetEntities,
                        statusPrompt = "Base: (%.1f, %.1f) - Drag or tap copy placement".format(basePt.x, basePt.y)
                    )
                }
                EditOperationType.ROTATE -> {
                    Active(
                        operation = operation,
                        targetEntityIds = targetIds,
                        basePoint = basePt,
                        currentPoint = basePt,
                        angleDeg = 0f,
                        previewEntities = targetEntities,
                        statusPrompt = "Pivot: (%.1f, %.1f) - Drag to rotate (θ = 0.0°)".format(basePt.x, basePt.y)
                    )
                }
                EditOperationType.SCALE -> {
                    Active(
                        operation = operation,
                        targetEntityIds = targetIds,
                        basePoint = basePt,
                        currentPoint = basePt,
                        scaleFactor = 1.0f,
                        previewEntities = targetEntities,
                        statusPrompt = "Base: (%.1f, %.1f) - Drag or enter scale factor (1.0x)".format(basePt.x, basePt.y)
                    )
                }
                EditOperationType.TRIM -> {
                    // Precompute initial trim if possible on first target line
                    val targetLine = targetEntities.firstOrNull { it is CadEntity.Line } as? CadEntity.Line
                    val trimResult = if (targetLine != null) {
                        CadEditMath.trimLine(targetLine, basePt, allDocumentEntities)
                    } else null

                    Active(
                        operation = operation,
                        targetEntityIds = targetIds,
                        basePoint = basePt,
                        currentPoint = basePt,
                        previewEntities = trimResult?.replacementEntities ?: targetEntities,
                        trimRemovedSegment = trimResult?.removedSegment,
                        statusPrompt = if (trimResult != null) {
                            "Trim segment previewed - Tap Confirm (✓) to apply"
                        } else {
                            "Tap along line segment crossing other entities to trim"
                        }
                    )
                }
                EditOperationType.EXTEND -> {
                    // Precompute initial extend if possible on first target line
                    val targetLine = targetEntities.firstOrNull { it is CadEntity.Line } as? CadEntity.Line
                    val extendResult = if (targetLine != null) {
                        CadEditMath.extendLine(targetLine, basePt, allDocumentEntities)
                    } else null

                    Active(
                        operation = operation,
                        targetEntityIds = targetIds,
                        basePoint = basePt,
                        currentPoint = basePt,
                        previewEntities = if (extendResult != null) listOf(extendResult.extendedEntity) else targetEntities,
                        extendNewSegment = extendResult?.extensionSegment,
                        statusPrompt = if (extendResult != null) {
                            "Extension boundary hit! Tap Confirm (✓) to apply"
                        } else {
                            "Tap near line endpoint towards a boundary entity"
                        }
                    )
                }
                EditOperationType.DELETE -> Idle
            }
        }
    }
}

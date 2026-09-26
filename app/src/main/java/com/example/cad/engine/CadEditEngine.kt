package com.example.cad.engine

import com.example.cad.engine.history.CadCommand
import com.example.cad.engine.history.CadCommandHistoryState
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import kotlinx.coroutines.flow.StateFlow

/**
 * Modular command-based editing engine for CAD document mutation and professional undo/redo history.
 */
interface CadEditEngine {
    val historyState: StateFlow<CadCommandHistoryState>

    fun canUndo(): Boolean
    fun canRedo(): Boolean
    fun getLastCommandName(): String?
    fun getNextRedoCommandName(): String?

    fun undo(document: CadDocument): CadDocument
    fun redo(document: CadDocument): CadDocument

    // Command Pattern execution directly
    fun executeCommand(document: CadDocument, command: CadCommand): CadDocument

    // Command grouping methods
    fun beginCommandGroup(groupName: String? = null)
    fun commitCommandGroup(groupName: String? = null)
    fun cancelCommandGroup()

    // Core CAD operations
    fun addEntity(document: CadDocument, entity: CadEntity): CadDocument
    fun addEntities(document: CadDocument, entities: List<CadEntity>): CadDocument
    fun updateEntity(document: CadDocument, entity: CadEntity): CadDocument
    fun deleteEntity(document: CadDocument, entityId: String): CadDocument
    fun deleteEntities(document: CadDocument, entityIds: Set<String>): CadDocument
    fun moveEntities(document: CadDocument, entityIds: Set<String>, deltaX: Float, deltaY: Float): CadDocument
    fun copyEntities(document: CadDocument, entityIds: Set<String>, deltaX: Float, deltaY: Float): Pair<CadDocument, Set<String>>
    fun rotateEntities(document: CadDocument, entityIds: Set<String>, center: CadPoint2D, angleDegrees: Float): CadDocument
    fun scaleEntities(document: CadDocument, entityIds: Set<String>, basePoint: CadPoint2D, factor: Float): CadDocument
    fun trimEntity(document: CadDocument, targetEntityId: String, clickPoint: CadPoint2D): CadDocument?
    fun extendEntity(document: CadDocument, targetEntityId: String, clickPoint: CadPoint2D): CadDocument?
    fun setLayerVisibility(document: CadDocument, layerId: String, isVisible: Boolean): CadDocument
    fun setLayerLock(document: CadDocument, layerId: String, isLocked: Boolean): CadDocument
    fun clearHistory()
}

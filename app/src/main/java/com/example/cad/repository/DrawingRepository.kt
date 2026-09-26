package com.example.cad.repository

import com.example.cad.data.ProjectEntity
import com.example.cad.model.CadDocument
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.cad.model.CadUnit
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Local CAD File Management repository interface.
 * Implements offline-first storage, favorites, recent tracking, renaming, duplicating,
 * project scoping, auto-save, and crash recovery.
 */
interface DrawingRepository {
    fun getRecentDrawings(): Flow<List<CadDrawing>>
    fun getAllDrawings(): Flow<List<CadDrawing>>
    fun getFavoriteDrawings(): Flow<List<CadDrawing>>
    fun getCrashRecoveryDrawings(): Flow<List<CadDrawing>>
    fun getAllProjects(): Flow<List<ProjectEntity>>
    fun getDrawingsForProject(projectName: String): Flow<List<CadDrawing>>

    suspend fun getDrawingById(id: String): CadDrawing?
    suspend fun toggleFavorite(id: String)

    // File Management CUJs:
    suspend fun renameDrawing(id: String, newName: String): Result<CadDrawing>
    suspend fun deleteDrawing(id: String): Result<Unit>
    suspend fun duplicateDrawing(id: String): Result<CadDrawing>
    suspend fun createProject(name: String, description: String = ""): Result<ProjectEntity>
    suspend fun deleteProject(projectId: String): Result<Unit>

    // Import & Create:
    suspend fun importDrawing(
        name: String,
        format: CadFormat,
        filePath: String,
        sizeBytes: Long,
        entityCount: Int = 0,
        layerCount: Int = 1,
        units: CadUnit = CadUnit.MILLIMETERS,
        projectName: String? = null
    ): CadDrawing

    suspend fun createNewDrawing(
        name: String,
        format: CadFormat,
        templateTag: String,
        projectName: String? = null
    ): CadDrawing

    // Save Architecture (DXF, Internal CADPROJ, DWG through future SDK):
    suspend fun saveDrawing(
        drawingId: String,
        document: CadDocument,
        format: CadFormat
    ): Result<File>

    // Auto-Save & Crash Recovery:
    suspend fun autoSaveDrawing(drawingId: String, document: CadDocument): Result<File>
    suspend fun recordCrashSnapshot(drawingId: String, document: CadDocument): Result<File>
    suspend fun recoverDrawing(drawingId: String): Result<CadDocument>
    suspend fun discardCrashRecovery(drawingId: String): Result<Unit>
    fun hasCrashRecovery(drawingId: String): Boolean
}

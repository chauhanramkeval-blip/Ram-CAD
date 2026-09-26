package com.example.cad.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Query("SELECT * FROM cad_drawings ORDER BY lastModifiedTimestamp DESC")
    fun getAllDrawings(): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM cad_drawings ORDER BY lastModifiedTimestamp DESC LIMIT :limit")
    fun getRecentDrawings(limit: Int = 30): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM cad_drawings WHERE isFavorite = 1 ORDER BY lastModifiedTimestamp DESC")
    fun getFavoriteDrawings(): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM cad_drawings WHERE projectName = :projectName ORDER BY lastModifiedTimestamp DESC")
    fun getDrawingsForProject(projectName: String): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM cad_drawings WHERE hasCrashRecovery = 1 ORDER BY recoveryTimestamp DESC")
    fun getCrashRecoveryDrawings(): Flow<List<DrawingEntity>>

    @Query("SELECT * FROM cad_drawings WHERE id = :id LIMIT 1")
    suspend fun getDrawingById(id: String): DrawingEntity?

    @Query("SELECT COUNT(*) FROM cad_drawings")
    suspend fun getDrawingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawing(drawing: DrawingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrawings(drawings: List<DrawingEntity>)

    @Update
    suspend fun updateDrawing(drawing: DrawingEntity)

    @Query("UPDATE cad_drawings SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("UPDATE cad_drawings SET name = :newName, filePath = :newFilePath, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun renameDrawing(id: String, newName: String, newFilePath: String, timestamp: Long)

    @Query("UPDATE cad_drawings SET hasCrashRecovery = :hasRecovery, recoveryFilePath = :recoveryPath, recoveryTimestamp = :recoveryTime WHERE id = :id")
    suspend fun updateCrashRecovery(id: String, hasRecovery: Boolean, recoveryPath: String?, recoveryTime: Long?)

    @Query("UPDATE cad_drawings SET fileSizeBytes = :size, lastModifiedTimestamp = :timestamp, entityCount = :entityCount, layerCount = :layerCount WHERE id = :id")
    suspend fun updateMetadata(id: String, size: Long, timestamp: Long, entityCount: Int, layerCount: Int)

    @Query("DELETE FROM cad_drawings WHERE id = :id")
    suspend fun deleteDrawingById(id: String)
}

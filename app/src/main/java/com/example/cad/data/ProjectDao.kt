package com.example.cad.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM cad_projects ORDER BY lastModifiedTimestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM cad_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM cad_projects WHERE name = :name LIMIT 1")
    suspend fun getProjectByName(name: String): ProjectEntity?

    @Query("SELECT COUNT(*) FROM cad_projects")
    suspend fun getProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE cad_projects SET name = :newName, folderPath = :newPath, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun renameProject(id: String, newName: String, newPath: String, timestamp: Long)

    @Query("UPDATE cad_projects SET drawingsCount = :count, lastModifiedTimestamp = :timestamp WHERE name = :projectName")
    suspend fun updateProjectDrawingsCount(projectName: String, count: Int, timestamp: Long)

    @Query("DELETE FROM cad_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

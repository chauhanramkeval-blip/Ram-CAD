package com.example.cad.io

import android.content.Context
import com.example.cad.model.CadDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages auto-save snapshots and crash recovery journals for offline-first CAD document editing.
 * Ensures unsaved drawing modifications can be fully recovered after an unexpected app closure or crash.
 */
class CrashRecoveryManager(
    private val context: Context
) {
    val recoveryDir: File = File(context.filesDir, "cad_crash_recovery").apply { mkdirs() }
    val autoSaveDir: File = File(context.filesDir, "cad_autosave").apply { mkdirs() }

    private val internalExporter = InternalProjectExporter()
    private val internalParser = InternalProjectParser()

    /**
     * Saves an auto-save snapshot of the active document.
     */
    suspend fun saveAutoSaveSnapshot(drawingId: String, document: CadDocument): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedId = drawingId.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
                val targetFile = File(autoSaveDir, "${sanitizedId}_autosave.cadproj")
                internalExporter.export(document, targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Saves a crash recovery snapshot journal marking an active editing session with unsaved state.
     */
    suspend fun recordCrashSnapshot(drawingId: String, document: CadDocument): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val sanitizedId = drawingId.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
                val targetFile = File(recoveryDir, "${sanitizedId}_recovery.cadproj")
                internalExporter.export(document, targetFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Checks if a valid crash recovery snapshot exists for the given drawing.
     */
    fun hasCrashSnapshot(drawingId: String): Boolean {
        val sanitizedId = drawingId.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
        val file = File(recoveryDir, "${sanitizedId}_recovery.cadproj")
        return file.exists() && file.length() > 0
    }

    /**
     * Retrieves the crash snapshot file if it exists.
     */
    fun getCrashSnapshotFile(drawingId: String): File? {
        val sanitizedId = drawingId.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
        val file = File(recoveryDir, "${sanitizedId}_recovery.cadproj")
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Loads and reconstitutes the recovered [CadDocument] from the crash recovery snapshot.
     */
    suspend fun loadCrashSnapshot(drawingId: String): Result<CadDocument> {
        return withContext(Dispatchers.IO) {
            val file = getCrashSnapshotFile(drawingId)
                ?: return@withContext Result.failure(IllegalStateException("No recovery file found for drawing '$drawingId'"))
            internalParser.parse(file)
        }
    }

    /**
     * Clears and purges the crash recovery snapshot (called on clean save or user discard).
     */
    suspend fun clearCrashSnapshot(drawingId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val sanitizedId = drawingId.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
            val file = File(recoveryDir, "${sanitizedId}_recovery.cadproj")
            if (file.exists()) file.delete() else true
        }
    }

    /**
     * Scans and returns all drawing IDs that have unrecovered crash recovery snapshots available.
     */
    fun listUnrecoveredDrawingIds(): List<String> {
        val files = recoveryDir.listFiles { f -> f.name.endsWith("_recovery.cadproj") } ?: return emptyList()
        return files.map { it.name.removeSuffix("_recovery.cadproj") }
    }
}

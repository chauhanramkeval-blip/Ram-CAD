package com.example.cad.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.cad.model.CadUnit

@Entity(tableName = "cad_drawings")
data class DrawingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val format: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val lastModifiedTimestamp: Long,
    val units: String = "MILLIMETERS",
    val entityCount: Int = 0,
    val layerCount: Int = 1,
    val isFavorite: Boolean = false,
    val description: String = "",
    val previewTag: String = "ARCH",
    val projectName: String? = null,
    val hasCrashRecovery: Boolean = false,
    val recoveryFilePath: String? = null,
    val recoveryTimestamp: Long? = null
) {
    fun toDomain(): CadDrawing {
        val resolvedFormat = CadFormat.fromExtension(format)
        val resolvedUnits = try {
            CadUnit.valueOf(units)
        } catch (_: Exception) {
            CadUnit.MILLIMETERS
        }
        return CadDrawing(
            id = id,
            name = name,
            format = resolvedFormat,
            filePath = filePath,
            fileSizeBytes = fileSizeBytes,
            lastModifiedTimestamp = lastModifiedTimestamp,
            units = resolvedUnits,
            entityCount = entityCount,
            layerCount = layerCount,
            isFavorite = isFavorite,
            description = description,
            previewTag = previewTag,
            projectName = projectName,
            hasCrashRecovery = hasCrashRecovery,
            recoveryFilePath = recoveryFilePath,
            recoveryTimestamp = recoveryTimestamp
        )
    }

    companion object {
        fun fromDomain(drawing: CadDrawing): DrawingEntity {
            return DrawingEntity(
                id = drawing.id,
                name = drawing.name,
                format = drawing.format.extension,
                filePath = drawing.filePath,
                fileSizeBytes = drawing.fileSizeBytes,
                lastModifiedTimestamp = drawing.lastModifiedTimestamp,
                units = drawing.units.name,
                entityCount = drawing.entityCount,
                layerCount = drawing.layerCount,
                isFavorite = drawing.isFavorite,
                description = drawing.description,
                previewTag = drawing.previewTag,
                projectName = drawing.projectName,
                hasCrashRecovery = drawing.hasCrashRecovery,
                recoveryFilePath = drawing.recoveryFilePath,
                recoveryTimestamp = drawing.recoveryTimestamp
            )
        }
    }
}

package com.example.cad.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata record for a CAD drawing file displayed in Home, Recent, and File Browser.
 */
data class CadDrawing(
    val id: String,
    val name: String,
    val format: CadFormat,
    val filePath: String,
    val fileSizeBytes: Long,
    val lastModifiedTimestamp: Long,
    val units: CadUnit = CadUnit.MILLIMETERS,
    val entityCount: Int = 0,
    val layerCount: Int = 1,
    val isFavorite: Boolean = false,
    val description: String = "",
    val previewTag: String = "ARCH", // e.g. "ARCH", "MECH", "ELEC", "SAMPLE"
    val projectName: String? = null,
    val hasCrashRecovery: Boolean = false,
    val recoveryFilePath: String? = null,
    val recoveryTimestamp: Long? = null
) {
    val drawingType: String
        get() = when (format) {
            CadFormat.DWG -> "AutoCAD Drawing (DWG)"
            CadFormat.DXF -> "Drawing Exchange Format (DXF)"
            CadFormat.CADPROJ -> "Native CAD Project (.cadproj)"
            CadFormat.STEP -> "3D STEP Model"
            CadFormat.DWF -> "Autodesk DWF Design"
            CadFormat.SVG -> "Scalable Vector Graphics (SVG)"
        }

    val formattedSize: String
        get() = when {
            fileSizeBytes < 1024 -> "$fileSizeBytes B"
            fileSizeBytes < 1024 * 1024 -> "%.1f KB".format(fileSizeBytes / 1024.0)
            else -> "%.2f MB".format(fileSizeBytes / (1024.0 * 1024.0))
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModifiedTimestamp))
        }
}

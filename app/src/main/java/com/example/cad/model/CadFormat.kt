package com.example.cad.model

/**
 * Supported CAD file formats.
 * Prepared for future DWG and DXF engine decoding/encoding.
 */
enum class CadFormat(
    val extension: String,
    val displayName: String,
    val description: String,
    val mimeType: String
) {
    DWG("dwg", "AutoCAD Drawing (DWG)", "Industry standard binary CAD drawing format", "image/vnd.dwg"),
    DXF("dxf", "Drawing Exchange Format (DXF)", "Open ASCII/Binary exchange format", "image/vnd.dxf"),
    DWF("dwf", "Design Web Format (DWF)", "Autodesk compressed 2D/3D design package", "model/vnd.dwf"),
    STEP("step", "STEP / STP", "Standard for the Exchange of Product model data", "application/step"),
    SVG("svg", "Scalable Vector Graphics", "Vector exchange export format", "image/svg+xml"),
    CADPROJ("cadproj", "CAD Project (Internal)", "Internal native offline CAD project package", "application/json");

    companion object {
        fun fromExtension(ext: String?): CadFormat {
            return entries.firstOrNull { it.extension.equals(ext, ignoreCase = true) } ?: DXF
        }

        fun fromFilename(filename: String): CadFormat {
            val ext = filename.substringAfterLast('.', "")
            return fromExtension(ext)
        }
    }
}

package com.example.cad.io

import com.example.cad.model.CadDocument
import com.example.cad.model.CadFormat
import java.io.File

/**
 * Common abstraction for exporting CAD documents to external and internal file formats.
 * Part of the CAD Save Architecture for DXF, DWG (via future CAD SDK), and Internal Project Format.
 */
interface CadExporter {
    val format: CadFormat

    /**
     * Serializes the [document] into the given [destination] file.
     * Returns the saved [File] or a failure result.
     */
    suspend fun export(document: CadDocument, destination: File): Result<File>
}

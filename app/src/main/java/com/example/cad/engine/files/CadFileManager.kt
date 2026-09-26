package com.example.cad.engine.files

import com.example.cad.model.CadFormat
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * File management abstraction for CAD projects, storage caching, format detection,
 * and binary DWG header validation.
 */
interface CadFileManager {

    /** Base project directory where CAD documents are stored */
    val projectsDir: File

    /** Temporary cache directory for imports and export buffering */
    val cacheDir: File

    /**
     * Detects CAD file format based on extension and internal binary magic header.
     */
    fun detectFormat(file: File): CadFormat

    /**
     * Inspects the binary header of a .DWG file to determine the AutoCAD release version
     * (e.g. AC1032 for AutoCAD 2018+, AC1027 for AutoCAD 2013).
     */
    fun inspectDwgHeader(file: File): DwgHeaderInfo

    /**
     * Reads a stream from Android Storage Access Framework (SAF) into a local temporary file.
     */
    fun copyStreamToTemp(inputStream: InputStream, suggestedName: String): File

    /**
     * Creates an empty or initialized CAD file in the projects directory.
     */
    fun createProjectFile(fileName: String, format: CadFormat): File

    /**
     * Lists all saved drawing files in the project storage.
     */
    fun listProjectFiles(): List<File>

    /**
     * Deletes a drawing file from storage.
     */
    fun deleteDrawing(filePath: String): Boolean
}

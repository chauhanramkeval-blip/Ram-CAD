package com.example.cad.engine

import com.example.cad.model.CadDocument
import com.example.cad.model.CadFormat
import java.io.InputStream
import java.io.OutputStream

/**
 * Modular parser and exporter for CAD file formats.
 *
 * Designed to integrate future native C++ libraries (NDK / JNI):
 * - DWG decoding via Open Design Alliance Teigha or LibreDWG
 * - DXF decoding via libdxfrw or dxflib
 */
interface CadFileParser {
    val supportedFormats: Set<CadFormat>

    /**
     * Parses raw file stream into [CadDocument].
     */
    suspend fun parse(
        inputStream: InputStream,
        format: CadFormat,
        title: String
    ): Result<CadDocument>

    /**
     * Serializes [CadDocument] into the target CAD format.
     */
    suspend fun export(
        document: CadDocument,
        format: CadFormat,
        outputStream: OutputStream
    ): Result<Unit>
}

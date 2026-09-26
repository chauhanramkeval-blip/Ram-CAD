package com.example.cad.engine

import com.example.cad.engine.api.CadEngineApi
import com.example.cad.engine.files.CadFileManager
import com.example.cad.model.CadDocument
import com.example.cad.model.CadDrawing

/**
 * Root facade for the CAD subsystem.
 *
 * Exposes core API, rendering, file management, measurement, and editing capabilities.
 * Allows transparently swapping out the default pure-Kotlin engine with
 * a native C++/NDK DWG/DXF engine when integrated.
 */
interface CadEngine {
    val name: String
    val version: String
    val isNativeEngineAvailable: Boolean
    val isDwgSdkLinked: Boolean
    val dwgEngineStatusDescription: String

    val api: CadEngineApi
    val fileManager: CadFileManager
    val parser: CadFileParser
    val renderer: CadRenderer
    val measurement: CadMeasurementEngine
    val editor: CadEditEngine

    suspend fun loadDrawing(drawing: CadDrawing): Result<CadDocument>
    fun createEmptyDocument(title: String): CadDocument
    fun createFromTemplate(templateName: String): CadDocument
}

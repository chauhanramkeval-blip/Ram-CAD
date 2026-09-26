package com.example.cad.engine.api

import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer

/**
 * Primary CAD Engine abstraction contract.
 *
 * Defines the vendor-neutral API for CAD drawing management, geometry manipulation,
 * and graphics rendering.
 *
 * Implementations of this interface can be:
 * 1. [com.example.cad.engine.core.StandardCadEngineApi]: Pure-Kotlin baseline supporting DXF
 * 2. [com.example.cad.engine.nativebridge.NativeCadEngine]: Production bridge to native C++ ODA SDK
 * 3. [com.example.cad.engine.mock.MockCadEngine]: Explicit mock implementation for development & tests
 */
interface CadEngineApi {

    /**
     * Unique identifier and display name for this CAD engine backend.
     */
    val engineName: String

    /**
     * Supported file formats for this engine implementation.
     */
    val supportedFormats: Set<CadFormat>

    /**
     * Engine backend classification (Kotlin baseline, Native ODA, or Mock).
     */
    val backendType: CadEngineBackendType

    /**
     * Opens a CAD drawing from the specified file path or URI.
     *
     * @param path Absolute file system path or content URI of the CAD file (.dwg, .dxf).
     * @param options Open options including read-only flag, password, and recovery flags.
     * @return [Result] wrapping the active [CadDrawingSession] or failure exception.
     */
    suspend fun openDrawing(
        path: String,
        options: CadOpenOptions = CadOpenOptions()
    ): Result<CadDrawingSession>

    /**
     * Closes an active drawing session and releases native resources or cached geometry.
     *
     * @param session Active session to close.
     * @return [Result] indicating success or failure.
     */
    suspend fun closeDrawing(session: CadDrawingSession): Result<Unit>

    /**
     * Renders the drawing session geometry into the provided render context.
     *
     * @param session Active drawing session whose entities should be rendered.
     * @param context Viewport transform, draw scope, and visual rendering options.
     */
    fun render(session: CadDrawingSession, context: CadRenderContext)

    /**
     * Retrieves all layers present in the specified drawing session.
     *
     * @param session Active drawing session.
     * @return List of [CadLayer] objects defining layer names, colors, and visibility.
     */
    suspend fun getLayers(session: CadDrawingSession): Result<List<CadLayer>>

    /**
     * Queries drawing entities matching the optional filter criteria.
     *
     * @param session Active drawing session.
     * @param filter Filter criteria (e.g. by layer, type, or bounding box).
     * @return List of matching [CadEntity] geometric primitives.
     */
    suspend fun getEntities(
        session: CadDrawingSession,
        filter: CadEntityFilter = CadEntityFilter.All
    ): Result<List<CadEntity>>

    /**
     * Creates and adds a new CAD entity to the drawing.
     *
     * @param session Active drawing session.
     * @param entity The geometric entity to add (Line, Circle, Polyline, Arc, Text, etc.).
     * @return The committed [CadEntity] with assigned unique ID and bounding box.
     */
    suspend fun createEntity(
        session: CadDrawingSession,
        entity: CadEntity
    ): Result<CadEntity>

    /**
     * Modifies an existing entity in the drawing session.
     *
     * @param session Active drawing session.
     * @param entity Updated entity definition.
     * @return The updated [CadEntity].
     */
    suspend fun modifyEntity(
        session: CadDrawingSession,
        entity: CadEntity
    ): Result<CadEntity>

    /**
     * Deletes an entity from the drawing session by its unique ID.
     *
     * @param session Active drawing session.
     * @param entityId Unique identifier of the entity to remove.
     * @return True if the entity was found and removed, false otherwise.
     */
    suspend fun deleteEntity(
        session: CadDrawingSession,
        entityId: String
    ): Result<Boolean>

    /**
     * Saves the drawing session to disk in native format (.dwg or .dxf).
     *
     * @param session Active drawing session to persist.
     * @param targetPath Target destination path. If null, overwrites original file.
     * @param format Target CAD format. If null, preserves the session's active format.
     */
    suspend fun saveDrawing(
        session: CadDrawingSession,
        targetPath: String? = null,
        format: CadFormat? = null
    ): Result<Unit>
}

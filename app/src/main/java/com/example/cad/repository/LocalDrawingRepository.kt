package com.example.cad.repository

import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.cad.model.CadUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalDrawingRepository : DrawingRepository {

    private val now = System.currentTimeMillis()

    private val initialDrawings = listOf(
        CadDrawing(
            id = "draw_1",
            name = "Architectural_Floor_Plan_A101.dwg",
            format = CadFormat.DWG,
            filePath = "/storage/emulated/0/CAD/Projects/Architectural_Floor_Plan_A101.dwg",
            fileSizeBytes = 2_840_000L,
            lastModifiedTimestamp = now - 1000L * 60 * 42, // 42 mins ago
            units = CadUnit.MILLIMETERS,
            entityCount = 342,
            layerCount = 14,
            isFavorite = true,
            description = "Commercial Level 2 Office Layout with partition walls and dimensions",
            previewTag = "ARCH"
        ),
        CadDrawing(
            id = "draw_2",
            name = "Flange_Housing_Assembly_M24.dxf",
            format = CadFormat.DXF,
            filePath = "/storage/emulated/0/CAD/Mechanical/Flange_Housing_Assembly_M24.dxf",
            fileSizeBytes = 845_200L,
            lastModifiedTimestamp = now - 1000L * 60 * 60 * 5, // 5 hours ago
            units = CadUnit.MILLIMETERS,
            entityCount = 186,
            layerCount = 8,
            isFavorite = true,
            description = "High-pressure hydraulic pipe flange mounting with 8 bolt PCD holes",
            previewTag = "MECH"
        ),
        CadDrawing(
            id = "draw_3",
            name = "Substation_Distribution_Diagram.dwg",
            format = CadFormat.DWG,
            filePath = "/storage/emulated/0/CAD/Electrical/Substation_Distribution_Diagram.dwg",
            fileSizeBytes = 1_420_000L,
            lastModifiedTimestamp = now - 1000L * 60 * 60 * 26, // 1 day ago
            units = CadUnit.MILLIMETERS,
            entityCount = 215,
            layerCount = 6,
            isFavorite = false,
            description = "Three-phase distribution schematic with transformer isolation",
            previewTag = "ELEC"
        ),
        CadDrawing(
            id = "draw_4",
            name = "Foundation_Reinforcement_Detail.dxf",
            format = CadFormat.DXF,
            filePath = "/storage/emulated/0/CAD/Civil/Foundation_Reinforcement_Detail.dxf",
            fileSizeBytes = 3_150_000L,
            lastModifiedTimestamp = now - 1000L * 60 * 60 * 72, // 3 days ago
            units = CadUnit.MILLIMETERS,
            entityCount = 512,
            layerCount = 18,
            isFavorite = false,
            description = "Reinforced concrete footing with rebar schedules and sectional callouts",
            previewTag = "ARCH"
        ),
        CadDrawing(
            id = "draw_5",
            name = "Gearbox_Shaft_Profile_RevC.step",
            format = CadFormat.STEP,
            filePath = "/storage/emulated/0/CAD/Mechanical/Gearbox_Shaft_Profile_RevC.step",
            fileSizeBytes = 4_920_000L,
            lastModifiedTimestamp = now - 1000L * 60 * 60 * 120, // 5 days ago
            units = CadUnit.MILLIMETERS,
            entityCount = 98,
            layerCount = 4,
            isFavorite = false,
            description = "Precision machined transmission input shaft with splines and keyways",
            previewTag = "MECH"
        )
    )

    private val _drawings = MutableStateFlow(initialDrawings)

    override fun getRecentDrawings(): Flow<List<CadDrawing>> {
        return _drawings.map { list ->
            list.sortedByDescending { it.lastModifiedTimestamp }
        }
    }

    override fun getAllDrawings(): Flow<List<CadDrawing>> {
        return _drawings.asStateFlow()
    }

    override suspend fun getDrawingById(id: String): CadDrawing? {
        return _drawings.value.firstOrNull { it.id == id }
    }

    override suspend fun toggleFavorite(id: String) {
        _drawings.value = _drawings.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    override fun getFavoriteDrawings(): Flow<List<CadDrawing>> {
        return _drawings.map { list -> list.filter { it.isFavorite } }
    }

    override fun getCrashRecoveryDrawings(): Flow<List<CadDrawing>> {
        return _drawings.map { list -> list.filter { it.hasCrashRecovery } }
    }

    private val _projects = MutableStateFlow(
        listOf(
            com.example.cad.data.ProjectEntity(
                id = "proj_1",
                name = "Commercial Office Park",
                description = "Office layouts and interior partitions",
                folderPath = "/storage/emulated/0/CAD/Projects/Commercial_Office_Park",
                createdTimestamp = now - 1000L * 3600 * 48,
                lastModifiedTimestamp = now - 1000L * 3600 * 2,
                drawingsCount = 2,
                isFavorite = true
            )
        )
    )

    override fun getAllProjects(): Flow<List<com.example.cad.data.ProjectEntity>> {
        return _projects.asStateFlow()
    }

    override fun getDrawingsForProject(projectName: String): Flow<List<CadDrawing>> {
        return _drawings.map { list -> list.filter { it.projectName == projectName } }
    }

    override suspend fun renameDrawing(id: String, newName: String): Result<CadDrawing> {
        val target = _drawings.value.firstOrNull { it.id == id }
            ?: return Result.failure(IllegalArgumentException("Drawing not found"))
        val updated = target.copy(name = newName, lastModifiedTimestamp = System.currentTimeMillis())
        _drawings.value = _drawings.value.map { if (it.id == id) updated else it }
        return Result.success(updated)
    }

    override suspend fun duplicateDrawing(id: String): Result<CadDrawing> {
        val target = _drawings.value.firstOrNull { it.id == id }
            ?: return Result.failure(IllegalArgumentException("Drawing not found"))
        val copy = target.copy(
            id = "dup_${UUID.randomUUID().toString().take(8)}",
            name = "${target.name.substringBeforeLast('.')}_Copy.${target.format.extension}",
            lastModifiedTimestamp = System.currentTimeMillis(),
            isFavorite = false
        )
        _drawings.value = listOf(copy) + _drawings.value
        return Result.success(copy)
    }

    override suspend fun createProject(name: String, description: String): Result<com.example.cad.data.ProjectEntity> {
        val proj = com.example.cad.data.ProjectEntity(
            id = "proj_${UUID.randomUUID().toString().take(8)}",
            name = name,
            description = description.ifEmpty { "Project workspace" },
            folderPath = "/storage/emulated/0/CAD/Projects/$name",
            createdTimestamp = System.currentTimeMillis(),
            lastModifiedTimestamp = System.currentTimeMillis(),
            drawingsCount = 0,
            isFavorite = false
        )
        _projects.value = listOf(proj) + _projects.value
        return Result.success(proj)
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> {
        _projects.value = _projects.value.filterNot { it.id == projectId }
        return Result.success(Unit)
    }

    override suspend fun importDrawing(
        name: String,
        format: CadFormat,
        filePath: String,
        sizeBytes: Long,
        entityCount: Int,
        layerCount: Int,
        units: CadUnit,
        projectName: String?
    ): CadDrawing {
        val newDrawing = CadDrawing(
            id = "import_${UUID.randomUUID().toString().take(8)}",
            name = name,
            format = format,
            filePath = filePath,
            fileSizeBytes = sizeBytes,
            lastModifiedTimestamp = System.currentTimeMillis(),
            units = units,
            entityCount = entityCount,
            layerCount = layerCount,
            isFavorite = false,
            description = "Imported external CAD file (${format.extension.uppercase()})",
            previewTag = if (format == CadFormat.DWG) "ARCH" else "MECH",
            projectName = projectName
        )
        _drawings.value = listOf(newDrawing) + _drawings.value
        return newDrawing
    }

    override suspend fun createNewDrawing(
        name: String,
        format: CadFormat,
        templateTag: String,
        projectName: String?
    ): CadDrawing {
        val filename = if (name.endsWith(".${format.extension}", ignoreCase = true)) name else "$name.${format.extension}"
        val newDrawing = CadDrawing(
            id = "new_${UUID.randomUUID().toString().take(8)}",
            name = filename,
            format = format,
            filePath = "/storage/emulated/0/CAD/Drawings/$filename",
            fileSizeBytes = 124_000L,
            lastModifiedTimestamp = System.currentTimeMillis(),
            units = CadUnit.MILLIMETERS,
            entityCount = if (templateTag.isNotEmpty()) 24 else 0,
            layerCount = 4,
            isFavorite = false,
            description = "New CAD drawing created from template",
            previewTag = templateTag.ifEmpty { "ARCH" },
            projectName = projectName
        )
        _drawings.value = listOf(newDrawing) + _drawings.value
        return newDrawing
    }

    override suspend fun deleteDrawing(id: String): Result<Unit> {
        _drawings.value = _drawings.value.filterNot { it.id == id }
        return Result.success(Unit)
    }

    override suspend fun saveDrawing(
        drawingId: String,
        document: com.example.cad.model.CadDocument,
        format: CadFormat
    ): Result<java.io.File> {
        val file = java.io.File(System.getProperty("java.io.tmpdir", "/tmp"), "${document.title}.${format.extension}")
        _drawings.value = _drawings.value.map {
            if (it.id == drawingId) it.copy(lastModifiedTimestamp = System.currentTimeMillis(), hasCrashRecovery = false) else it
        }
        return Result.success(file)
    }

    override suspend fun autoSaveDrawing(
        drawingId: String,
        document: com.example.cad.model.CadDocument
    ): Result<java.io.File> {
        val file = java.io.File(System.getProperty("java.io.tmpdir", "/tmp"), "${drawingId}_autosave.cadproj")
        _drawings.value = _drawings.value.map {
            if (it.id == drawingId) it.copy(hasCrashRecovery = true, recoveryTimestamp = System.currentTimeMillis()) else it
        }
        return Result.success(file)
    }

    override suspend fun recordCrashSnapshot(
        drawingId: String,
        document: com.example.cad.model.CadDocument
    ): Result<java.io.File> {
        val file = java.io.File(System.getProperty("java.io.tmpdir", "/tmp"), "${drawingId}_recovery.cadproj")
        _drawings.value = _drawings.value.map {
            if (it.id == drawingId) it.copy(hasCrashRecovery = true, recoveryTimestamp = System.currentTimeMillis()) else it
        }
        return Result.success(file)
    }

    override suspend fun recoverDrawing(drawingId: String): Result<com.example.cad.model.CadDocument> {
        val target = _drawings.value.firstOrNull { it.id == drawingId }
        return if (target != null) {
            Result.success(com.example.cad.engine.SampleCadDrawings.createArchitecturalFloorPlan().copy(title = target.name))
        } else {
            Result.failure(IllegalArgumentException("Drawing not found"))
        }
    }

    override suspend fun discardCrashRecovery(drawingId: String): Result<Unit> {
        _drawings.value = _drawings.value.map {
            if (it.id == drawingId) it.copy(hasCrashRecovery = false, recoveryFilePath = null, recoveryTimestamp = null) else it
        }
        return Result.success(Unit)
    }

    override fun hasCrashRecovery(drawingId: String): Boolean {
        return _drawings.value.firstOrNull { it.id == drawingId }?.hasCrashRecovery == true
    }
}

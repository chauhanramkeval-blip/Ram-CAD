package com.example.ui.screens.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cad.engine.CadEngine
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.cad.parser.dxf.SampleDxfContent
import com.example.cad.repository.DrawingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class CadFolder(
    val name: String,
    val path: String,
    val itemCount: Int
)

data class FileBrowserUiState(
    val currentPath: String = "/storage/emulated/0/CAD",
    val folders: List<CadFolder> = emptyList(),
    val drawingsInFolder: List<CadDrawing> = emptyList(),
    val selectedDrawing: CadDrawing? = null,
    val isImporting: Boolean = false,
    val showNewFolderDialog: Boolean = false,
    val renamingDrawing: CadDrawing? = null,
    val deletingDrawing: CadDrawing? = null,
    val propertiesDrawing: CadDrawing? = null,
    val showCreateProjectDialog: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class FileBrowserViewModel(
    private val repository: DrawingRepository,
    private val cadEngine: CadEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var allDrawings: List<CadDrawing> = emptyList()

    init {
        val defaultFolders = listOf(
            CadFolder("Projects", "/storage/emulated/0/CAD/Projects", 1),
            CadFolder("Mechanical", "/storage/emulated/0/CAD/Mechanical", 2),
            CadFolder("Electrical", "/storage/emulated/0/CAD/Electrical", 1),
            CadFolder("Civil", "/storage/emulated/0/CAD/Civil", 1)
        )
        _uiState.value = _uiState.value.copy(folders = defaultFolders)

        repository.getAllProjects().onEach { projectsList ->
            val dynamicFolders = projectsList.map { p ->
                CadFolder(p.name, p.folderPath, p.drawingsCount)
            }
            _uiState.value = _uiState.value.copy(folders = defaultFolders + dynamicFolders)
        }.launchIn(viewModelScope)

        repository.getAllDrawings().onEach { list ->
            allDrawings = list
            updateDrawingsForCurrentPath()
        }.launchIn(viewModelScope)
    }

    private fun updateDrawingsForCurrentPath() {
        val curr = _uiState.value.currentPath
        val filtered = allDrawings.filter { drawing ->
            drawing.filePath.startsWith(curr) || curr == "/storage/emulated/0/CAD"
        }
        _uiState.value = _uiState.value.copy(drawingsInFolder = filtered)
    }

    fun navigateToFolder(folder: CadFolder) {
        _uiState.value = _uiState.value.copy(currentPath = folder.path)
        updateDrawingsForCurrentPath()
    }

    fun navigateUp() {
        val curr = _uiState.value.currentPath
        val parent = if (curr.contains('/')) curr.substringBeforeLast('/') else "/storage/emulated/0/CAD"
        _uiState.value = _uiState.value.copy(
            currentPath = if (parent.length < 20) "/storage/emulated/0/CAD" else parent
        )
        updateDrawingsForCurrentPath()
    }

    fun selectDrawing(drawing: CadDrawing?) {
        _uiState.value = _uiState.value.copy(selectedDrawing = drawing)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissStatus() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun importDxfFromUri(context: Context, uri: Uri, onImported: (String) -> Unit) =
        importCadFileFromUri(context, uri, onImported)

    fun importCadFileFromUri(context: Context, uri: Uri, onImported: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
            try {
                // 1. Resolve file name from content resolver
                var displayName = "Imported_Drawing.dxf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }

                // 2. Validate file extension for DWG or DXF
                val isDxf = displayName.endsWith(".dxf", ignoreCase = true)
                val isDwg = displayName.endsWith(".dwg", ignoreCase = true)
                if (!isDxf && !isDwg) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Selected file '$displayName' is not a supported AutoCAD file (.dwg or .dxf). Please select a valid DWG or DXF document."
                    )
                    return@launch
                }

                val targetFormat = if (isDwg) CadFormat.DWG else CadFormat.DXF

                // 3. Copy file to internal storage
                val localFile = withContext(Dispatchers.IO) {
                    val importDir = File(context.filesDir, "imported_cad").apply { mkdirs() }
                    val sanitizedName = displayName.replace(Regex("""[^a-zA-Z0-9._-]"""), "_")
                    val targetFile = File(importDir, "${System.currentTimeMillis()}_$sanitizedName")

                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw IllegalStateException("Could not open input stream for selected file URI")

                    targetFile
                }

                // 4. Parse the CAD document with CadEngine parser
                val parseResult = withContext(Dispatchers.IO) {
                    localFile.inputStream().use { stream ->
                        cadEngine.parser.parse(stream, targetFormat, displayName)
                    }
                }

                if (parseResult.isFailure) {
                    val exception = parseResult.exceptionOrNull()
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Failed to parse ${targetFormat.displayName}: ${exception?.message ?: "Unknown corruption"}"
                    )
                    return@launch
                }

                val doc = parseResult.getOrThrow()

                // 5. Register in Drawing Repository
                val importedDrawing = repository.importDrawing(
                    name = displayName,
                    format = targetFormat,
                    filePath = localFile.absolutePath,
                    sizeBytes = localFile.length(),
                    entityCount = doc.entityCount,
                    layerCount = doc.layerCount,
                    units = doc.units
                )

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    statusMessage = "Successfully opened '${displayName}' (${doc.entityCount} entities, ${doc.layerCount} layers)"
                )

                onImported(importedDrawing.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "Error opening CAD file: ${e.message}"
                )
            }
        }
    }

    fun importSampleDxf(context: Context, onImported: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
            try {
                val fileName = "Sample_Workshop_Layout.dxf"
                val localFile = withContext(Dispatchers.IO) {
                    val importDir = File(context.filesDir, "imported_dxf").apply { mkdirs() }
                    val targetFile = File(importDir, "${System.currentTimeMillis()}_$fileName")
                    targetFile.writeText(SampleDxfContent.VALID_FULL_DXF)
                    targetFile
                }

                val parseResult = withContext(Dispatchers.IO) {
                    localFile.inputStream().use { stream ->
                        cadEngine.parser.parse(stream, CadFormat.DXF, fileName)
                    }
                }

                if (parseResult.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Failed to parse sample DXF: ${parseResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                val doc = parseResult.getOrThrow()
                val imported = repository.importDrawing(
                    name = fileName,
                    format = CadFormat.DXF,
                    filePath = localFile.absolutePath,
                    sizeBytes = localFile.length(),
                    entityCount = doc.entityCount,
                    layerCount = doc.layerCount,
                    units = doc.units
                )

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    statusMessage = "Sample DXF loaded successfully (${doc.entityCount} entities)"
                )

                onImported(imported.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "Error loading sample DXF: ${e.message}"
                )
            }
        }
    }

    fun importSampleFile(format: CadFormat, onImported: (String) -> Unit) {
        viewModelScope.launch {
            val name = "Imported_${format.name}_${System.currentTimeMillis() % 1000}.${format.extension}"
            val path = "${_uiState.value.currentPath}/$name"
            val imported = repository.importDrawing(
                name = name,
                format = format,
                filePath = path,
                sizeBytes = 1_850_000L
            )
            onImported(imported.id)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun requestRename(drawing: CadDrawing) {
        _uiState.value = _uiState.value.copy(renamingDrawing = drawing)
    }

    fun dismissRename() {
        _uiState.value = _uiState.value.copy(renamingDrawing = null)
    }

    fun confirmRename(id: String, newName: String) {
        viewModelScope.launch {
            val result = repository.renameDrawing(id, newName)
            _uiState.value = _uiState.value.copy(renamingDrawing = null)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Renamed to '${it.name}'")
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to rename: ${it.message}")
            }
        }
    }

    fun requestDelete(drawing: CadDrawing) {
        _uiState.value = _uiState.value.copy(deletingDrawing = drawing)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(deletingDrawing = null)
    }

    fun confirmDelete(id: String) {
        viewModelScope.launch {
            val result = repository.deleteDrawing(id)
            _uiState.value = _uiState.value.copy(deletingDrawing = null, selectedDrawing = null)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Drawing deleted successfully")
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to delete: ${it.message}")
            }
        }
    }

    fun duplicateDrawing(id: String) {
        viewModelScope.launch {
            val result = repository.duplicateDrawing(id)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Duplicated as '${it.name}'")
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to duplicate: ${it.message}")
            }
        }
    }

    fun showProperties(drawing: CadDrawing) {
        _uiState.value = _uiState.value.copy(propertiesDrawing = drawing)
    }

    fun dismissProperties() {
        _uiState.value = _uiState.value.copy(propertiesDrawing = null)
    }

    fun showCreateProject() {
        _uiState.value = _uiState.value.copy(showCreateProjectDialog = true)
    }

    fun dismissCreateProject() {
        _uiState.value = _uiState.value.copy(showCreateProjectDialog = false)
    }

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showCreateProjectDialog = false)
            val result = repository.createProject(name, description)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Project '${it.name}' created")
            }.onFailure {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create project: ${it.message}")
            }
        }
    }
}

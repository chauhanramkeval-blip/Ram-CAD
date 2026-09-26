package com.example.ui.screens.home

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

data class HomeUiState(
    val recentDrawings: List<CadDrawing> = emptyList(),
    val totalDrawingsCount: Int = 0,
    val dwgCount: Int = 0,
    val dxfCount: Int = 0,
    val engineName: String = "",
    val engineVersion: String = "",
    val isNativeEngineAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val renamingDrawing: CadDrawing? = null,
    val deletingDrawing: CadDrawing? = null,
    val propertiesDrawing: CadDrawing? = null,
    val showCreateProjectDialog: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class HomeViewModel(
    private val repository: DrawingRepository,
    private val cadEngine: CadEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            engineName = cadEngine.name,
            engineVersion = cadEngine.version,
            isNativeEngineAvailable = cadEngine.isNativeEngineAvailable
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        repository.getAllDrawings().onEach { drawings ->
            _uiState.value = _uiState.value.copy(
                recentDrawings = drawings.sortedByDescending { it.lastModifiedTimestamp }.take(4),
                totalDrawingsCount = drawings.size,
                dwgCount = drawings.count { it.format == CadFormat.DWG },
                dxfCount = drawings.count { it.format == CadFormat.DXF }
            )
        }.launchIn(viewModelScope)
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
                var displayName = "Imported_Drawing.dxf"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                    }
                }

                val isDxf = displayName.endsWith(".dxf", ignoreCase = true)
                val isDwg = displayName.endsWith(".dwg", ignoreCase = true)
                if (!isDxf && !isDwg) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Selected file '$displayName' is not a supported CAD drawing (.dwg or .dxf). Please choose a valid DWG or DXF document."
                    )
                    return@launch
                }

                val targetFormat = if (isDwg) CadFormat.DWG else CadFormat.DXF

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
            _uiState.value = _uiState.value.copy(deletingDrawing = null)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(statusMessage = "Drawing deleted")
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

    fun createDrawingFromTemplate(templateTag: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val name = when (templateTag) {
                "ARCH" -> "Architectural_Project_New"
                "MECH" -> "Mechanical_Component_New"
                "ELEC" -> "Schematic_Diagram_New"
                else -> "Untitled_Drawing"
            }
            val format = if (templateTag == "MECH") CadFormat.DXF else CadFormat.DWG
            val created = repository.createNewDrawing(name, format, templateTag)
            onCreated(created.id)
        }
    }
}

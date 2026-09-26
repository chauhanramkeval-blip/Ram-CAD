package com.example.ui.screens.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cad.data.ProjectEntity
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.cad.repository.DrawingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RecentFilter(val label: String) {
    ALL("All"),
    FAVORITES("Bookmarked"),
    DWG("DWG"),
    DXF("DXF"),
    CADPROJ("Projects"),
    RECOVERY("Crash Recovery")
}

data class RecentDrawingsUiState(
    val drawings: List<CadDrawing> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: RecentFilter = RecentFilter.ALL,
    val isLoading: Boolean = false,
    val renamingDrawing: CadDrawing? = null,
    val deletingDrawing: CadDrawing? = null,
    val propertiesDrawing: CadDrawing? = null,
    val showCreateProjectDialog: Boolean = false,
    val statusMessage: String? = null
)

class RecentDrawingsViewModel(
    private val repository: DrawingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(RecentFilter.ALL)
    private val _renamingDrawing = MutableStateFlow<CadDrawing?>(null)
    private val _deletingDrawing = MutableStateFlow<CadDrawing?>(null)
    private val _propertiesDrawing = MutableStateFlow<CadDrawing?>(null)
    private val _showCreateProjectDialog = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RecentDrawingsUiState> = combine(
        repository.getAllDrawings(),
        repository.getAllProjects(),
        _searchQuery,
        _activeFilter,
        _renamingDrawing,
        _deletingDrawing,
        _propertiesDrawing,
        _showCreateProjectDialog,
        _statusMessage
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allDrawings = args[0] as List<CadDrawing>
        @Suppress("UNCHECKED_CAST")
        val projects = args[1] as List<ProjectEntity>
        val query = args[2] as String
        val filter = args[3] as RecentFilter
        val renaming = args[4] as? CadDrawing
        val deleting = args[5] as? CadDrawing
        val properties = args[6] as? CadDrawing
        val showCreateProj = args[7] as Boolean
        val status = args[8] as? String

        val filtered = allDrawings.filter { drawing ->
            val matchesQuery = query.isEmpty() ||
                drawing.name.contains(query, ignoreCase = true) ||
                drawing.description.contains(query, ignoreCase = true) ||
                (drawing.projectName?.contains(query, ignoreCase = true) == true)

            val matchesFilter = when (filter) {
                RecentFilter.ALL -> true
                RecentFilter.FAVORITES -> drawing.isFavorite
                RecentFilter.DWG -> drawing.format == CadFormat.DWG
                RecentFilter.DXF -> drawing.format == CadFormat.DXF
                RecentFilter.CADPROJ -> drawing.format == CadFormat.CADPROJ || drawing.projectName != null
                RecentFilter.RECOVERY -> drawing.hasCrashRecovery
            }

            matchesQuery && matchesFilter
        }

        RecentDrawingsUiState(
            drawings = filtered,
            projects = projects,
            searchQuery = query,
            activeFilter = filter,
            renamingDrawing = renaming,
            deletingDrawing = deleting,
            propertiesDrawing = properties,
            showCreateProjectDialog = showCreateProj,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecentDrawingsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: RecentFilter) {
        _activeFilter.value = filter
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun requestRename(drawing: CadDrawing) {
        _renamingDrawing.value = drawing
    }

    fun dismissRename() {
        _renamingDrawing.value = null
    }

    fun confirmRename(id: String, newName: String) {
        viewModelScope.launch {
            val result = repository.renameDrawing(id, newName)
            _renamingDrawing.value = null
            result.onSuccess {
                _statusMessage.value = "Renamed to '${it.name}'"
            }.onFailure {
                _statusMessage.value = "Failed to rename: ${it.message}"
            }
        }
    }

    fun requestDelete(drawing: CadDrawing) {
        _deletingDrawing.value = drawing
    }

    fun dismissDelete() {
        _deletingDrawing.value = null
    }

    fun confirmDelete(id: String) {
        viewModelScope.launch {
            val result = repository.deleteDrawing(id)
            _deletingDrawing.value = null
            result.onSuccess {
                _statusMessage.value = "Drawing deleted successfully"
            }.onFailure {
                _statusMessage.value = "Failed to delete: ${it.message}"
            }
        }
    }

    fun duplicateDrawing(id: String) {
        viewModelScope.launch {
            val result = repository.duplicateDrawing(id)
            result.onSuccess {
                _statusMessage.value = "Duplicated as '${it.name}'"
            }.onFailure {
                _statusMessage.value = "Failed to duplicate: ${it.message}"
            }
        }
    }

    fun showProperties(drawing: CadDrawing) {
        _propertiesDrawing.value = drawing
    }

    fun dismissProperties() {
        _propertiesDrawing.value = null
    }

    fun showCreateProject() {
        _showCreateProjectDialog.value = true
    }

    fun dismissCreateProject() {
        _showCreateProjectDialog.value = false
    }

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            _showCreateProjectDialog.value = false
            val result = repository.createProject(name, description)
            result.onSuccess {
                _statusMessage.value = "Project '${it.name}' created"
            }.onFailure {
                _statusMessage.value = "Failed to create project: ${it.message}"
            }
        }
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }
}

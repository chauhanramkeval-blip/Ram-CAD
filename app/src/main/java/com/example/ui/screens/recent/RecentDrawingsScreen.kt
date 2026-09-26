package com.example.ui.screens.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CadCreateProjectDialog
import com.example.ui.components.CadDeleteConfirmDialog
import com.example.ui.components.CadDrawingCard
import com.example.ui.components.CadEmptyState
import com.example.ui.components.CadFileMetadataDialog
import com.example.ui.components.CadRenameDialog
import com.example.ui.components.CadTopAppBar
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark

@Composable
fun RecentDrawingsScreen(
    viewModel: RecentDrawingsViewModel,
    onOpenDrawing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CadTopAppBar(
                title = "Drawings & Projects",
                subtitle = "${uiState.drawings.size} drawings · ${uiState.projects.size} projects"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Search input + Create Project button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("recent_search_input"),
                        placeholder = {
                            Text(
                                text = "Search name, project or type...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CadSurfaceDark,
                            unfocusedContainerColor = CadSurfaceDark,
                            focusedBorderColor = CadCyan,
                            unfocusedBorderColor = CadBorderDark
                        )
                    )

                    Button(
                        onClick = { viewModel.showCreateProject() },
                        colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("create_project_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CreateNewFolder,
                                contentDescription = "New Project",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Project",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips (All, Favorites, Projects, DWG, DXF, Recovery)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RecentFilter.entries) { filter ->
                        val isSelected = uiState.activeFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CadCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CadCyan,
                                containerColor = CadSurfaceDark,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) CadCyan else CadBorderDark,
                                selectedBorderColor = CadCyan,
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Drawings List or Empty State
                if (uiState.drawings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CadEmptyState(
                            title = "No CAD Drawings Found",
                            message = if (uiState.searchQuery.isNotEmpty()) {
                                "No drawings match \"${uiState.searchQuery}\". Try clearing your search query."
                            } else {
                                "No CAD drawings found in this category. You can create a new project or import a drawing."
                            },
                            actionLabel = if (uiState.searchQuery.isNotEmpty()) "Clear Filter" else "View All Drawings",
                            onActionClick = {
                                viewModel.onSearchQueryChanged("")
                                viewModel.onFilterSelected(RecentFilter.ALL)
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("recent_drawings_list"),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.drawings, key = { it.id }) { drawing ->
                            CadDrawingCard(
                                drawing = drawing,
                                onClick = { onOpenDrawing(drawing.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(drawing.id) },
                                onRename = { viewModel.requestRename(drawing) },
                                onDuplicate = { viewModel.duplicateDrawing(drawing.id) },
                                onDelete = { viewModel.requestDelete(drawing) },
                                onShowProperties = { viewModel.showProperties(drawing) },
                                onExport = { onOpenDrawing(drawing.id) },
                                onRestoreCrashRecovery = { onOpenDrawing(drawing.id) }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Dialogs
        uiState.renamingDrawing?.let { drawing ->
            CadRenameDialog(
                drawing = drawing,
                onDismiss = viewModel::dismissRename,
                onConfirmRename = { newName ->
                    viewModel.confirmRename(drawing.id, newName)
                }
            )
        }

        uiState.deletingDrawing?.let { drawing ->
            CadDeleteConfirmDialog(
                drawing = drawing,
                onDismiss = viewModel::dismissDelete,
                onConfirmDelete = {
                    viewModel.confirmDelete(drawing.id)
                }
            )
        }

        uiState.propertiesDrawing?.let { drawing ->
            CadFileMetadataDialog(
                drawing = drawing,
                onDismiss = viewModel::dismissProperties
            )
        }

        if (uiState.showCreateProjectDialog) {
            CadCreateProjectDialog(
                onDismiss = viewModel::dismissCreateProject,
                onConfirmCreate = { name, desc ->
                    viewModel.createProject(name, desc)
                }
            )
        }
    }
}

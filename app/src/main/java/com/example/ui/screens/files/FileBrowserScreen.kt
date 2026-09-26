package com.example.ui.screens.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.ui.components.CadDrawingCard
import com.example.ui.components.CadFormatChip
import com.example.ui.components.CadTopAppBar
import com.example.ui.permissions.rememberCadStoragePermissionState
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onOpenDrawing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }

    // Android Storage Permission Request Flow
    val storagePermissionState = rememberCadStoragePermissionState()

    // Android Storage Access Framework File Picker for .DWG and .DXF files
    val cadFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCadFileFromUri(context, uri, onOpenDrawing)
        }
    }

    // Error Dialog for corrupted or unsupported DXF files
    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Import Error",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "CAD Import Error",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = uiState.errorMessage ?: "Unknown error while opening CAD drawing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::dismissError,
                    colors = ButtonDefaults.buttonColors(containerColor = CadCyan)
                ) {
                    Text("OK", color = Color.Black)
                }
            },
            containerColor = CadSurfaceDark
        )
    }

    // Loading indicator overlay during CAD parsing and bounds calculation
    if (uiState.isImporting) {
        AlertDialog(
            onDismissRequest = { /* non-cancelable during parse */ },
            title = {
                Text(
                    text = "Parsing CAD Drawing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = CadCyan,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Reading CAD entities, converting geometry, and calculating bounds...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { },
            containerColor = CadSurfaceDark
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CadTopAppBar(
            title = "CAD File Browser",
            subtitle = uiState.currentPath.replace("/storage/emulated/0", "Device Storage"),
            actions = {
                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.testTag("import_cad_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.UploadFile,
                        contentDescription = "Import CAD File",
                        tint = CadCyan
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("file_browser_content"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Path Navigation breadcrumb
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CadSurfaceDark)
                        .border(1.dp, CadBorderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentPath != "/storage/emulated/0/CAD") {
                        IconButton(
                            onClick = viewModel::navigateUp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate Up",
                                tint = CadCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = uiState.currentPath.replace("/storage/emulated/0/CAD", "📁 Root"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Folder Categories
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Project Folders",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = viewModel::showCreateProject,
                        modifier = Modifier.testTag("browser_create_project_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Project",
                            tint = CadCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.folders) { folder ->
                        CadFolderCard(
                            folder = folder,
                            isSelected = uiState.currentPath == folder.path,
                            onClick = { viewModel.navigateToFolder(folder) }
                        )
                    }
                }
            }

            // Drawings in Current Directory
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Drawings in Folder (${uiState.drawingsInFolder.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(uiState.drawingsInFolder, key = { it.id }) { drawing ->
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

    // Action dialogs
    uiState.renamingDrawing?.let { drawing ->
        com.example.ui.components.CadRenameDialog(
            drawing = drawing,
            onDismiss = viewModel::dismissRename,
            onConfirmRename = { newName ->
                viewModel.confirmRename(drawing.id, newName)
            }
        )
    }

    uiState.deletingDrawing?.let { drawing ->
        com.example.ui.components.CadDeleteConfirmDialog(
            drawing = drawing,
            onDismiss = viewModel::dismissDelete,
            onConfirmDelete = {
                viewModel.confirmDelete(drawing.id)
            }
        )
    }

    uiState.propertiesDrawing?.let { drawing ->
        com.example.ui.components.CadFileMetadataDialog(
            drawing = drawing,
            onDismiss = viewModel::dismissProperties
        )
    }

    if (uiState.showCreateProjectDialog) {
        com.example.ui.components.CadCreateProjectDialog(
            onDismiss = viewModel::dismissCreateProject,
            onConfirmCreate = { name, desc ->
                viewModel.createProject(name, desc)
            }
        )
    }

    // Import CAD Modal Dialog
    if (showImportDialog) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showImportDialog = false },
            sheetState = sheetState,
            containerColor = CadSurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Import CAD Drawing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Open an AutoCAD .DWG or .DXF drawing from device storage or load a technical sample:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Storage Permission Flow & File Picker for .DWG and .DXF files
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showImportDialog = false
                            storagePermissionState.requestAccess {
                                cadFilePickerLauncher.launch(
                                    arrayOf(
                                        "application/dxf",
                                        "image/vnd.dxf",
                                        "application/x-dwg",
                                        "image/vnd.dwg",
                                        "application/acad",
                                        "application/x-acad",
                                        "application/autocad_dwg",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        }
                        .testTag("import_pick_dxf_btn"),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadCyan)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CadFormatChip(format = CadFormat.DWG)
                            CadFormatChip(format = CadFormat.DXF)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open DWG / DXF from Device Storage",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CadCyan
                            )
                            Text(
                                text = "Browse device storage and SD card with permission check to open 2D CAD files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.FileOpen,
                            contentDescription = null,
                            tint = CadCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Load complete DXF sample with all 7 supported entities
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showImportDialog = false
                            viewModel.importSampleDxf(context, onOpenDrawing)
                        }
                        .testTag("import_sample_dxf_btn"),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(com.example.ui.theme.CadBorderDark)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CadFormatChip(format = CadFormat.DXF)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Load Sample Technical DXF",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Parses LINE, POLYLINE, LWPOLYLINE, CIRCLE, ARC, RECTANGLE, and TEXT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                CadImportOptionItem(
                    format = CadFormat.DWG,
                    description = "Load sample native AutoCAD DWG drawing",
                    onClick = {
                        showImportDialog = false
                        viewModel.importSampleFile(CadFormat.DWG, onOpenDrawing)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CadFolderCard(
    folder: CadFolder,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("folder_${folder.name.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) CadCyan.copy(alpha = 0.15f) else CadSurfaceDark
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CadCyan else CadBorderDark)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = "Folder",
                tint = if (isSelected) CadCyan else Color(0xFFFFD600),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${folder.itemCount} files",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CadImportOptionItem(
    format: CadFormat,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("import_option_${format.extension}"),
        colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CadFormatChip(format = format)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = format.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.example.ui.screens.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadFormat
import com.example.ui.components.CadDrawingCard
import com.example.ui.components.CadFormatChip
import com.example.ui.components.CadMiniBlueprintThumbnail
import com.example.ui.components.CadTopAppBar
import com.example.ui.permissions.rememberCadStoragePermissionState
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSnapGreen
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawing: (String) -> Unit,
    onNavigateToRecent: () -> Unit,
    onNavigateToFiles: () -> Unit,
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
                    text = uiState.errorMessage ?: "Unknown error while importing CAD drawing.",
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

    // Loading indicator overlay during DXF import and parse
    if (uiState.isImporting) {
        AlertDialog(
            onDismissRequest = { /* non-cancelable during parse */ },
            title = {
                Text(
                    text = "Importing CAD Drawing",
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
                        text = "Parsing CAD entities, geometry, and computing bounds...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { },
            containerColor = CadSurfaceDark
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
                    text = "Open AutoCAD .DWG or .DXF drawings from device storage or load technical samples:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Storage Permission Flow & File Picker for .DWG and .DXF
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
                        .testTag("home_pick_dxf_btn"),
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
                                text = "Open DWG / DXF from Device",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = CadCyan
                            )
                            Text(
                                text = "Browse device storage and SD card with permission check",
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

                // Option 2: Sample DXF with all supported entities
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showImportDialog = false
                            viewModel.importSampleDxf(context, onOpenDrawing)
                        }
                        .testTag("home_sample_dxf_btn"),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
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
                                text = "Contains LINE, POLYLINE, LWPOLYLINE, CIRCLE, ARC, RECTANGLE, TEXT",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Navigate to file browser
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            showImportDialog = false
                            onNavigateToFiles()
                        }
                        .testTag("home_browse_files_btn"),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CadFormatChip(format = CadFormat.DWG)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open CAD File Browser",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Explore directory trees and manage stored drawings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CadTopAppBar(
            title = "CAD Mobile Viewer & Editor",
            subtitle = "2D Drafting & Vector Engineering"
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_screen_content"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Engine Architecture Status Banner
                item {
                    CadEngineStatusBanner(
                        engineName = uiState.engineName,
                        version = uiState.engineVersion,
                        isNative = uiState.isNativeEngineAvailable
                    )
                }

                // Quick Action Cards
                item {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isTablet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CadQuickActionCard(
                                title = "New Drawing",
                                subtitle = "Start blank CAD project",
                                icon = Icons.Filled.Add,
                                accentColor = CadCyan,
                                onClick = { viewModel.createDrawingFromTemplate("ARCH", onOpenDrawing) },
                                modifier = Modifier.weight(1f)
                            )
                            CadQuickActionCard(
                                title = "Import CAD",
                                subtitle = "Pick .DXF or browse",
                                icon = Icons.Filled.FileOpen,
                                accentColor = Color(0xFF2979FF),
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                            CadQuickActionCard(
                                title = "Quick Measure",
                                subtitle = "Inspect & measure drawing",
                                icon = Icons.Filled.Straighten,
                                accentColor = CadSnapGreen,
                                onClick = {
                                    val first = uiState.recentDrawings.firstOrNull()?.id
                                    onOpenDrawing(first ?: "")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CadQuickActionCard(
                                title = "New CAD",
                                subtitle = "Blank Project",
                                icon = Icons.Filled.Add,
                                accentColor = CadCyan,
                                onClick = { viewModel.createDrawingFromTemplate("ARCH", onOpenDrawing) },
                                modifier = Modifier.weight(1f)
                            )
                            CadQuickActionCard(
                                title = "Import CAD",
                                subtitle = "Pick .DXF or browse",
                                icon = Icons.Filled.FileOpen,
                                accentColor = Color(0xFF2979FF),
                                onClick = { showImportDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Project Overview Stats
                item {
                    CadOverviewStatsRow(
                        total = uiState.totalDrawingsCount,
                        dwg = uiState.dwgCount,
                        dxf = uiState.dxfCount
                    )
                }

                // Engineering Templates Quick Start
                item {
                    Text(
                        text = "Engineering Templates",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            CadTemplateItem(
                                title = "Architectural Plan",
                                tag = "ARCH",
                                format = "DWG",
                                description = "Floor plan, walls, columns, door swings",
                                onClick = { viewModel.createDrawingFromTemplate("ARCH", onOpenDrawing) }
                            )
                        }
                        item {
                            CadTemplateItem(
                                title = "Mechanical Assembly",
                                tag = "MECH",
                                format = "DXF",
                                description = "Flange bore, bolt PCD, centerlines",
                                onClick = { viewModel.createDrawingFromTemplate("MECH", onOpenDrawing) }
                            )
                        }
                        item {
                            CadTemplateItem(
                                title = "Electrical Diagram",
                                tag = "ELEC",
                                format = "DWG",
                                description = "3-Phase busbars, transformer symbols",
                                onClick = { viewModel.createDrawingFromTemplate("ELEC", onOpenDrawing) }
                            )
                        }
                    }
                }

                // Recent Drawings Header & List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Drawings",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onNavigateToRecent,
                            modifier = Modifier.testTag("view_all_recent_btn")
                        ) {
                            Text(
                                text = "View All (${uiState.totalDrawingsCount})",
                                color = CadCyan,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                items(uiState.recentDrawings, key = { it.id }) { drawing ->
                    CadDrawingCard(
                        drawing = drawing,
                        onClick = { onOpenDrawing(drawing.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(drawing.id) },
                        onRename = { viewModel.requestRename(drawing) },
                        onDuplicate = { viewModel.duplicateDrawing(drawing.id) },
                        onDelete = { viewModel.requestDelete(drawing) },
                        onShowProperties = { viewModel.showProperties(drawing) },
                        onRestoreCrashRecovery = { onOpenDrawing(drawing.id) }
                    )
                }

                // Bottom padding spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
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
    }
}

@Composable
fun CadEngineStatusBanner(
    engineName: String,
    version: String,
    isNative: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("engine_status_banner"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.dp, Color(0x6600E5FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = "CAD Engine",
                    tint = CadCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Modular CAD Core Engine",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x2200E676))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DWG / DXF READY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = CadSnapGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$engineName · v$version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CadQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("action_card_${title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CadOverviewStatsRow(
    total: Int,
    dwg: Int,
    dxf: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CadStatBox(
            label = "Total Drawings",
            value = total.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        CadStatBox(
            label = "DWG Projects",
            value = dwg.toString(),
            color = Color(0xFF2979FF),
            modifier = Modifier.weight(1f)
        )
        CadStatBox(
            label = "DXF Exchange",
            value = dxf.toString(),
            color = CadCyan,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CadStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CadSurfaceVariantDark)
            .border(1.dp, CadBorderDark, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CadTemplateItem(
    title: String,
    tag: String,
    format: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("template_${tag.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CadMiniBlueprintThumbnail(
                    tag = tag,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CadSurfaceVariantDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = format,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CadCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

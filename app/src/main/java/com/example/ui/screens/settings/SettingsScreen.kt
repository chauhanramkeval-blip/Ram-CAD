package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadUnit
import com.example.ui.components.CadTopAppBar
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSnapGreen
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CadTopAppBar(
            title = "CAD Settings",
            subtitle = "Drafting environment & engine configuration"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("settings_content"),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Architecture Information Card
            item {
                CadEngineArchitectureCard(
                    engineName = uiState.engineName,
                    version = uiState.engineVersion,
                    isNative = uiState.isNativeEngineAvailable
                )
            }

            // Units & Measurements Section
            item {
                Text(
                    text = "Drawing Units & Precision",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Standard Drafting Unit",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CadUnit.entries.forEach { unit ->
                                val isSelected = uiState.selectedUnit == unit
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setUnit(unit) },
                                    label = { Text(unit.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CadCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CadCyan,
                                        containerColor = CadSurfaceVariantDark,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) CadCyan else CadBorderDark,
                                        selectedBorderColor = CadCyan,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    modifier = Modifier.testTag("unit_chip_${unit.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Coordinate Precision",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0, 1, 2, 3).forEach { precision ->
                                val isSelected = uiState.coordinatePrecision == precision
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setCoordinatePrecision(precision) },
                                    label = { Text("$precision Decimals") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CadCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CadCyan,
                                        containerColor = CadSurfaceVariantDark,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) CadCyan else CadBorderDark,
                                        selectedBorderColor = CadCyan,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Drafting & Grid Snapping
            item {
                Text(
                    text = "Drafting & Snapping",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Object Snap (OSNAP)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Snap cursor to endpoints, midpoints and centers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isAutoSnapEnabled,
                                onCheckedChange = viewModel::setAutoSnap,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CadCyan,
                                    checkedTrackColor = CadCyan.copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Grid Spacing",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100f, 500f, 1000f).forEach { spacing ->
                                val isSelected = uiState.gridSpacingMm == spacing
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setGridSpacing(spacing) },
                                    label = { Text("%.0f mm".format(spacing)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CadCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CadCyan,
                                        containerColor = CadSurfaceVariantDark,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) CadCyan else CadBorderDark,
                                        selectedBorderColor = CadCyan,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Canvas Workspace Style
            item {
                Text(
                    text = "Canvas Background Style",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        listOf("AutoCAD Dark (#0C0E14)", "Blueprint Navy (#0B1933)", "Slate Drafting (#1A1F2C)").forEach { themeName ->
                            val isSelected = uiState.canvasBackgroundTheme == themeName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) CadCyan.copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setCanvasBackground(themeName) },
                                    label = { Text(themeName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CadCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = CadCyan,
                                        containerColor = CadSurfaceVariantDark,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) CadCyan else CadBorderDark,
                                        selectedBorderColor = CadCyan,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CadEngineArchitectureCard(
    engineName: String,
    version: String,
    isNative: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("engine_architecture_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CadSurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2200E5FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = "Engine Architecture",
                        tint = CadCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "CAD Modular Subsystem Architecture",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$engineName · v$version",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem modules list
            CadArchitectureModuleRow("CadRenderer", "Decoupled rendering abstraction (Compose Canvas -> OpenGL / NDK)")
            CadArchitectureModuleRow("CadFileParser", "Prepared for native DWG (LibreDWG/Teigha) and DXF (libdxfrw)")
            CadArchitectureModuleRow("CadMeasurementEngine", "Independent distance, angle, polyline & Shoelace polygon area")
            CadArchitectureModuleRow("CadEditEngine", "Immutable document mutations with undo/redo command stack")
        }
    }
}

@Composable
fun CadArchitectureModuleRow(
    name: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium.copy(color = CadCyan),
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = CadCyan
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

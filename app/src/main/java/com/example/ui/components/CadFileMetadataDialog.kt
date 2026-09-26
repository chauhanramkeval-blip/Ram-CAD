package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadDrawing
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

/**
 * Dialog displaying exhaustive metadata for a CAD drawing.
 * Satisfies Requirement 10:
 * - Name
 * - Size
 * - Modified date
 * - Drawing type
 * Plus entity count, layer count, units, storage path, and crash recovery status.
 */
@Composable
fun CadFileMetadataDialog(
    drawing: CadDrawing,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("file_metadata_dialog"),
        shape = RoundedCornerShape(16.dp),
        containerColor = CadSurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Drawing Properties",
                        tint = CadCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drawing Properties",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header highlight card with Name & Type
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CadSurfaceVariantDark)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = drawing.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CadCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CadFormatChip(format = drawing.format)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = drawing.drawingType,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = CadBorderDark, thickness = 1.dp)

                // Property rows
                MetadataPropertyRow(
                    icon = Icons.Filled.Storage,
                    label = "File Size",
                    value = "${drawing.formattedSize} (${drawing.fileSizeBytes} bytes)"
                )

                MetadataPropertyRow(
                    icon = Icons.Filled.Schedule,
                    label = "Modified Date",
                    value = drawing.formattedDate
                )

                MetadataPropertyRow(
                    icon = Icons.Filled.Layers,
                    label = "Layers & Units",
                    value = "${drawing.layerCount} Layers · ${drawing.units.displayName} (${drawing.units.abbreviation})"
                )

                MetadataPropertyRow(
                    icon = Icons.Filled.Description,
                    label = "Entities Count",
                    value = "${drawing.entityCount} 2D CAD Primitives"
                )

                MetadataPropertyRow(
                    icon = Icons.Filled.Folder,
                    label = "Project Workspace",
                    value = drawing.projectName ?: "Standalone Drawing (Root)"
                )

                MetadataPropertyRow(
                    icon = Icons.Filled.Storage,
                    label = "Physical Storage Path",
                    value = drawing.filePath,
                    isMonospace = true
                )

                if (drawing.hasCrashRecovery) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF332000))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Crash Snapshot",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Unsaved recovery snapshot available on disk",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MetadataPropertyRow(
    icon: ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CadCyan.copy(alpha = 0.8f),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

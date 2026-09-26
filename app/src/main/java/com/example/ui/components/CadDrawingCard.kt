package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadFormat
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

@Composable
fun CadDrawingCard(
    drawing: CadDrawing,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onRename: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShowProperties: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onRestoreCrashRecovery: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("drawing_card_${drawing.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Preview blueprint banner + Favorite Button + Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Technical Blueprint Mini Thumbnail
                CadMiniBlueprintThumbnail(
                    tag = drawing.previewTag,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = drawing.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CadFormatChip(format = drawing.format)
                        Text(
                            text = drawing.formattedSize,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${drawing.entityCount} entities",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bookmark action
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_btn_${drawing.id}")
                ) {
                    Icon(
                        imageVector = if (drawing.isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (drawing.isFavorite) "Remove Bookmark" else "Bookmark Drawing",
                        tint = if (drawing.isFavorite) CadCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Overflow Actions Menu (Rename, Duplicate, Properties, Delete)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("overflow_btn_${drawing.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CadSurfaceDark)
                    ) {
                        if (onShowProperties != null) {
                            DropdownMenuItem(
                                text = { Text("Properties (Metadata)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = CadCyan
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onShowProperties()
                                },
                                modifier = Modifier.testTag("menu_properties_${drawing.id}")
                            )
                        }

                        if (onExport != null) {
                            DropdownMenuItem(
                                text = { Text("Export & Print...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        tint = CadCyan
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onExport()
                                },
                                modifier = Modifier.testTag("menu_export_${drawing.id}")
                            )
                        }

                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                },
                                modifier = Modifier.testTag("menu_rename_${drawing.id}")
                            )
                        }

                        if (onDuplicate != null) {
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDuplicate()
                                },
                                modifier = Modifier.testTag("menu_duplicate_${drawing.id}")
                            )
                        }

                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color(0xFFFF5252)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                modifier = Modifier.testTag("menu_delete_${drawing.id}")
                            )
                        }
                    }
                }
            }

            // Crash Recovery Alert Banner
            if (drawing.hasCrashRecovery) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF332000))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Crash Recovery",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Crash Recovery Available",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD54F)
                            )
                        }

                        if (onRestoreCrashRecovery != null) {
                            Button(
                                onClick = onRestoreCrashRecovery,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 2.dp
                                ),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("restore_btn_${drawing.id}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Restore",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Project Scope Tag
            if (drawing.projectName != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Project",
                        tint = CadCyan.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = drawing.projectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = CadCyan.copy(alpha = 0.85f)
                    )
                }
            }

            if (drawing.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = drawing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(CadSurfaceVariantDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = "Layers",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${drawing.layerCount} Layers (${drawing.units.abbreviation})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Last Modified",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = drawing.formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CadMiniBlueprintThumbnail(tag: String, modifier: Modifier = Modifier) {
    val (gridColor, elemColor) = when (tag) {
        "ARCH" -> Pair(Color(0xFF0D253A), Color(0xFF4FC3F7))
        "MECH" -> Pair(Color(0xFF1C2A1E), Color(0xFF81C784))
        "ELEC" -> Pair(Color(0xFF2E1A35), Color(0xFFBA68C8))
        else -> Pair(Color(0xFF1A1F2C), Color(0xFF00E5FF))
    }

    Canvas(modifier = modifier.background(gridColor)) {
        val w = size.width
        val h = size.height
        // Drafting grid lines
        val step = w / 4f
        for (i in 1..3) {
            drawLine(
                color = elemColor.copy(alpha = 0.2f),
                start = Offset(i * step, 0f),
                end = Offset(i * step, h),
                strokeWidth = 1f
            )
            drawLine(
                color = elemColor.copy(alpha = 0.2f),
                start = Offset(0f, i * step),
                end = Offset(w, i * step),
                strokeWidth = 1f
            )
        }

        when (tag) {
            "ARCH" -> {
                // Architectural walls
                drawRect(
                    color = elemColor.copy(alpha = 0.7f),
                    topLeft = Offset(w * 0.2f, h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                )
                drawLine(
                    color = elemColor,
                    start = Offset(w * 0.2f, h * 0.5f),
                    end = Offset(w * 0.6f, h * 0.5f),
                    strokeWidth = 2f
                )
            }
            "MECH" -> {
                // Mechanical circle flange
                drawCircle(
                    color = elemColor.copy(alpha = 0.7f),
                    radius = w * 0.32f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                )
                drawCircle(
                    color = elemColor,
                    radius = w * 0.12f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            else -> {
                // Electrical / General schematic line
                drawLine(
                    color = elemColor,
                    start = Offset(w * 0.2f, h * 0.8f),
                    end = Offset(w * 0.8f, h * 0.2f),
                    strokeWidth = 2.5f
                )
                drawCircle(
                    color = elemColor,
                    radius = 3.5f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
        }
    }
}

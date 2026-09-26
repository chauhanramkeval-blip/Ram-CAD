package com.example.ui.screens.editor

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.engine.snap.CadOsnapSettings
import com.example.cad.engine.snap.CadSnapMode
import com.example.ui.theme.CadSnapGreen

/**
 * Modal Bottom Sheet for CAD Object Snap (OSNAP) settings and configuration.
 *
 * Allows drafting engineers to toggle the OSNAP master switch, enable/disable individual
 * snap modes (Endpoint, Midpoint, Center, Intersection, Quadrant, Perpendicular, Nearest),
 * configure zoom-aware snap aperture tolerance, and customize marker visualization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadOsnapSettingsSheet(
    settings: CadOsnapSettings,
    onSettingsChanged: (CadOsnapSettings) -> Unit,
    onToggleMode: (CadSnapMode) -> Unit,
    onSetAllModes: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.testTag("cad_osnap_settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (settings.isEnabled) CadSnapGreen else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Object Snap (OSNAP)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_osnap_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close OSNAP Settings")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Master OSNAP Switch Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Object Snap (F3)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (settings.isEnabled) "${settings.enabledModes.size} of 7 snap modes active"
                            else "All object snaps temporarily paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.isEnabled) CadSnapGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(isEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CadSnapGreen
                        ),
                        modifier = Modifier.testTag("osnap_master_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Batch Mode Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSetAllModes(true) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("osnap_select_all_button"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Select All", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onSetAllModes(false) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("osnap_clear_all_button"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "Clear All", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("osnap_reset_defaults_button"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Defaults", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Individual Modes & Parameters
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Section Title: Snap Modes
                item {
                    Text(
                        text = "OBJECT SNAP MODES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(CadSnapMode.values().toList(), key = { it.name }) { mode ->
                    val isChecked = settings.enabledModes.contains(mode)
                    OsnapModeRow(
                        mode = mode,
                        isChecked = isChecked,
                        isOsnapEnabled = settings.isEnabled,
                        onToggle = { onToggleMode(mode) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SNAP TOLERANCE & APERTURE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Zoom-Aware Snap Tolerance Slider
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Capture Tolerance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${settings.snapToleranceScreenPx.toInt()} px",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CadSnapGreen
                                )
                            }
                            Slider(
                                value = settings.snapToleranceScreenPx,
                                onValueChange = { onSettingsChanged(settings.copy(snapToleranceScreenPx = it)) },
                                valueRange = 10f..48f,
                                steps = 18,
                                colors = SliderDefaults.colors(
                                    thumbColor = CadSnapGreen,
                                    activeTrackColor = CadSnapGreen
                                ),
                                modifier = Modifier.testTag("osnap_tolerance_slider")
                            )
                            Text(
                                text = "Zoom-Aware: The screen capture radius is constant (px) while the world-space threshold scales dynamically with zoom level for precision drafting.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Display Options
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Show Tooltip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Snap Label Tooltip",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Displays badge (e.g. [Endpoint], [Center]) next to marker",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.showTooltip,
                                    onCheckedChange = { onSettingsChanged(settings.copy(showTooltip = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CadSnapGreen
                                    ),
                                    modifier = Modifier.testTag("osnap_tooltip_switch")
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // Show Aperture
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Crosshair Aperture Box",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Shows the active pick aperture box around the cursor",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.showAperture,
                                    onCheckedChange = { onSettingsChanged(settings.copy(showAperture = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = CadSnapGreen
                                    ),
                                    modifier = Modifier.testTag("osnap_aperture_switch")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Row for an individual OSNAP mode with geometric icon preview.
 */
@Composable
private fun OsnapModeRow(
    mode: CadSnapMode,
    isChecked: Boolean,
    isOsnapEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(8.dp),
        color = if (isChecked && isOsnapEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("osnap_mode_row_${mode.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Geometric Marker Icon Preview
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF141820), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = 7f
                    val color = if (isChecked && isOsnapEnabled) CadSnapGreen else Color(0xFF6B7280)

                    when (mode) {
                        CadSnapMode.ENDPOINT -> {
                            drawRect(
                                color = color,
                                topLeft = Offset(cx - r, cy - r),
                                size = Size(r * 2, r * 2),
                                style = Stroke(width = 2f)
                            )
                        }
                        CadSnapMode.MIDPOINT -> {
                            val path = Path().apply {
                                moveTo(cx, cy - r)
                                lineTo(cx + r * 1.1f, cy + r * 0.9f)
                                lineTo(cx - r * 1.1f, cy + r * 0.9f)
                                close()
                            }
                            drawPath(path, color, style = Stroke(width = 2f, join = StrokeJoin.Round))
                        }
                        CadSnapMode.CENTER -> {
                            drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = 2f))
                            drawLine(color, Offset(cx - 2.5f, cy), Offset(cx + 2.5f, cy), strokeWidth = 1.5f)
                            drawLine(color, Offset(cx, cy - 2.5f), Offset(cx, cy + 2.5f), strokeWidth = 1.5f)
                        }
                        CadSnapMode.INTERSECTION -> {
                            val h = r * 0.85f
                            drawLine(color, Offset(cx - h, cy - h), Offset(cx + h, cy + h), strokeWidth = 2f, cap = StrokeCap.Round)
                            drawLine(color, Offset(cx - h, cy + h), Offset(cx + h, cy - h), strokeWidth = 2f, cap = StrokeCap.Round)
                        }
                        CadSnapMode.QUADRANT -> {
                            val path = Path().apply {
                                moveTo(cx, cy - r * 1.15f)
                                lineTo(cx + r * 1.15f, cy)
                                lineTo(cx, cy + r * 1.15f)
                                lineTo(cx - r * 1.15f, cy)
                                close()
                            }
                            drawPath(path, color, style = Stroke(width = 2f, join = StrokeJoin.Round))
                        }
                        CadSnapMode.PERPENDICULAR -> {
                            val arm = r * 1.1f
                            val pip = r * 0.5f
                            drawLine(color, Offset(cx - arm * 0.5f, cy + arm * 0.5f), Offset(cx + arm * 0.7f, cy + arm * 0.5f), strokeWidth = 2f)
                            drawLine(color, Offset(cx, cy + arm * 0.5f), Offset(cx, cy - arm * 0.7f), strokeWidth = 2f)
                            drawLine(color, Offset(cx, cy + arm * 0.5f - pip), Offset(cx + pip, cy + arm * 0.5f - pip), strokeWidth = 1.5f)
                            drawLine(color, Offset(cx + pip, cy + arm * 0.5f - pip), Offset(cx + pip, cy + arm * 0.5f), strokeWidth = 1.5f)
                        }
                        CadSnapMode.NEAREST -> {
                            val path = Path().apply {
                                moveTo(cx - r * 0.8f, cy - r)
                                lineTo(cx + r * 0.8f, cy - r)
                                lineTo(cx - r * 0.8f, cy + r)
                                lineTo(cx + r * 0.8f, cy + r)
                                close()
                            }
                            drawPath(path, color, style = Stroke(width = 2f, join = StrokeJoin.Round))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                enabled = isOsnapEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CadSnapGreen
                ),
                modifier = Modifier.testTag("osnap_mode_switch_${mode.name.lowercase()}")
            )
        }
    }
}

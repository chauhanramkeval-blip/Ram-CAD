package com.example.ui.screens.editor.tools

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadDimensionYellow
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

/**
 * Professional AutoCAD-style popup toolbox panel.
 * Opens above the bottom toolbar with smooth animation, rounded corners,
 * multi-row tool layout, tool badges, and favorite pinning.
 */
@Composable
fun PopupToolPanel(
    category: PopupCategory?,
    toolManager: ToolManager,
    commandManager: CommandManager,
    isTablet: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = category != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeOut(),
        modifier = modifier
    ) {
        if (category == null) return@AnimatedVisibility

        val tools = ToolRegistry.getToolsByCategory(category)
        val title = category.title
        val subtitle = category.subtitle

        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            color = Color(0xF5131823),
            tonalElevation = 10.dp,
            shadowElevation = 12.dp,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.verticalGradient(
                    colors = listOf(CadCyan.copy(alpha = 0.5f), CadBorderDark)
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isTablet) 800.dp else 600.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("cad_popup_tool_panel")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                // Header Bar: Category Title, Subtitle, Pin Mode Toggle & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CadCyan.copy(alpha = 0.15f))
                                .border(1.dp, CadCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (category) {
                                    PopupCategory.ANNOTATION -> Icons.Filled.EditNote
                                    PopupCategory.DRAW -> Icons.Filled.Brush
                                    PopupCategory.EDIT -> Icons.Filled.Transform
                                    PopupCategory.LAYER -> Icons.Filled.Layers
                                    PopupCategory.MEASURE -> Icons.Filled.Straighten
                                    PopupCategory.DIMENSION -> Icons.Filled.SquareFoot
                                    PopupCategory.COLOR -> Icons.Filled.Palette
                                    PopupCategory.TOOL -> Icons.Filled.Build
                                    PopupCategory.LAYOUT -> Icons.Filled.Dashboard
                                    PopupCategory.VISUAL_STYLE -> Icons.Filled.Visibility
                                },
                                contentDescription = title,
                                tint = CadCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title.uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${tools.size} tools)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CadCyan.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Action buttons: Keep-Open Pin Toggle and Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Pin popup open toggle
                        IconButton(
                            onClick = { toolManager.toggleKeepOpen() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_pin_popup")
                        ) {
                            Icon(
                                imageVector = if (toolManager.isKeepPopupOpen) Icons.Filled.PushPin else Icons.Filled.VerticalAlignBottom,
                                contentDescription = if (toolManager.isKeepPopupOpen) "Keep Panel Pinned" else "Auto-close on Select",
                                tint = if (toolManager.isKeepPopupOpen) CadDimensionYellow else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close button
                        IconButton(
                            onClick = { toolManager.closePopup() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_close_popup")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Tool Panel",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tools Multi-row Display
                // Split tools into 2 balanced rows for clean horizontal scroll on phones and tablets
                val row1 = tools.filterIndexed { index, _ -> index % 2 == 0 }
                val row2 = tools.filterIndexed { index, _ -> index % 2 != 0 }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("popup_tools_scrollable_container"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1 of tools
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row1.forEach { tool ->
                            PopupToolItem(
                                tool = tool,
                                isSelected = toolManager.activeToolId == tool.id,
                                isPinned = toolManager.isToolPinned(tool.id),
                                onSelect = {
                                    toolManager.executeTool(tool, commandManager)
                                },
                                onTogglePin = {
                                    toolManager.togglePinTool(tool.id)
                                }
                            )
                        }
                    }

                    // Row 2 of tools (if any)
                    if (row2.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row2.forEach { tool ->
                                PopupToolItem(
                                    tool = tool,
                                    isSelected = toolManager.activeToolId == tool.id,
                                    isPinned = toolManager.isToolPinned(tool.id),
                                    onSelect = {
                                        toolManager.executeTool(tool, commandManager)
                                    },
                                    onTogglePin = {
                                        toolManager.togglePinTool(tool.id)
                                    }
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
 * Individual tool item chip inside the popup tool panel.
 */
@Composable
fun PopupToolItem(
    tool: ToolDefinition,
    isSelected: Boolean,
    isPinned: Boolean,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEngineRequired = tool.availability == ToolAvailability.ENGINE_REQUIRED
    val isComingSoon = tool.availability == ToolAvailability.COMING_SOON

    val backgroundColor = when {
        isSelected -> CadCyan.copy(alpha = 0.22f)
        isEngineRequired -> Color(0xFF1E1E28)
        else -> CadSurfaceVariantDark
    }

    val borderColor = when {
        isSelected -> CadCyan
        isEngineRequired -> Color(0xFFE65100).copy(alpha = 0.6f)
        isComingSoon -> Color(0xFF64748B).copy(alpha = 0.6f)
        else -> CadBorderDark
    }

    val iconTint = when {
        isSelected -> CadCyan
        isEngineRequired -> Color(0xFFFFB74D)
        isComingSoon -> Color.Gray
        else -> Color.White
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
        modifier = modifier
            .widthIn(min = 100.dp, max = 135.dp)
            .height(56.dp)
            .testTag("tool_item_${tool.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Label column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CadCyan.copy(alpha = 0.2f) else Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.name,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tool.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) CadCyan else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Status Badge if Engine Required or Coming Soon
                    if (isEngineRequired) {
                        Text(
                            text = "Engine Req",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFFFFB74D),
                            maxLines = 1
                        )
                    } else if (isComingSoon) {
                        Text(
                            text = "Coming Soon",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    } else if (tool.requiresSelection) {
                        Text(
                            text = "Select Req",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = CadDimensionYellow.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            // Pin / Favorite Star Button
            IconButton(
                onClick = onTogglePin,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("pin_${tool.id}")
            ) {
                Icon(
                    imageVector = if (isPinned) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (isPinned) "Unpin Tool" else "Pin Tool to Favorites",
                    tint = if (isPinned) CadDimensionYellow else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

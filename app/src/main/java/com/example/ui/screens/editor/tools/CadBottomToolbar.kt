package com.example.ui.screens.editor.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadDimensionYellow
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark

/**
 * Main CAD bottom navigation and tool category toolbar.
 * Displays the 10 canonical mobile CAD categories in order:
 * 1. Annotation
 * 2. Draw
 * 3. Edit
 * 4. Layer
 * 5. Measure
 * 6. Dimension
 * 7. Color
 * 8. Tool
 * 9. Layout
 * 10. Visual Style
 *
 * Also provides a quick-access strip for pinned/favorite tools.
 */
@Composable
fun CadBottomToolbar(
    toolManager: ToolManager,
    commandManager: CommandManager,
    modifier: Modifier = Modifier
) {
    val pinnedTools = toolManager.getPinnedTools()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("cad_bottom_main_toolbar"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF50D1117)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pinned / Favorites Quick Strip (if any tools are pinned)
            AnimatedVisibility(
                visible = pinnedTools.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B26))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite Tools",
                        tint = CadDimensionYellow,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp)
                    )

                    Text(
                        text = "PINNED:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.Gray
                    )

                    pinnedTools.forEach { tool ->
                        val isSelected = toolManager.activeToolId == tool.id
                        Surface(
                            onClick = {
                                toolManager.executeTool(tool, commandManager)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CadCyan.copy(alpha = 0.2f) else CadSurfaceVariantDark,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) CadCyan else CadBorderDark)
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("pinned_tool_${tool.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.name,
                                    tint = if (isSelected) CadCyan else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) CadCyan else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Main 10-Item CAD Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolRegistry.categories.forEachIndexed { index, category ->
                    val isActive = toolManager.activeCategory == category

                    val animatedBgColor by animateColorAsState(
                        targetValue = if (isActive) CadCyan.copy(alpha = 0.2f) else Color.Transparent,
                        animationSpec = spring(),
                        label = "toolbar_bg_anim_${category.id}"
                    )

                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (isActive) CadCyan else Color.Transparent,
                        animationSpec = spring(),
                        label = "toolbar_border_anim_${category.id}"
                    )

                    val icon = when (category) {
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
                    }

                    Surface(
                        onClick = {
                            toolManager.toggleCategory(category)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = animatedBgColor,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(animatedBorderColor)
                        ),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 64.dp, minHeight = 52.dp)
                            .testTag("toolbar_btn_${category.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = category.title,
                                    tint = if (isActive) CadCyan else Color(0xFFCAD1DC),
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(CadCyan)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isActive) CadCyan else Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

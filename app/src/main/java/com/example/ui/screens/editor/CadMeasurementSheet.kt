package com.example.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.cad.engine.measurement.CadMeasurementResult
import com.example.cad.engine.measurement.CadMeasurementType
import com.example.cad.model.CadUnit
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadDimensionYellow
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom Sheet modal for CAD Measurement Tools, Units, Multi-Area Manager, and History.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadMeasurementSheet(
    activeType: CadMeasurementType?,
    activeResult: CadMeasurementResult?,
    currentUnit: CadUnit,
    multiAreaRegions: List<CadMeasurementResult.CadAreaRegion> = emptyList(),
    isSubtractMode: Boolean = false,
    measurementHistory: List<CadMeasurementHistoryItem> = emptyList(),
    selectedCount: Int = 0,
    onSelectTool: (CadMeasurementType) -> Unit,
    onChangeUnit: (CadUnit) -> Unit,
    onCopyResult: () -> Unit,
    onClearResult: () -> Unit,
    onAddRegion: (Boolean) -> Unit = {},
    onToggleRegionSubtract: (String) -> Unit = {},
    onRemoveRegion: (String) -> Unit = {},
    onClearRegions: () -> Unit = {},
    onMeasureSelection: () -> Unit = {},
    onRecallHistoryItem: (CadMeasurementHistoryItem) -> Unit = {},
    onDeleteHistoryItem: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember {
        mutableStateOf(
            when {
                activeType == CadMeasurementType.MULTI_AREA -> 1
                activeType == CadMeasurementType.SELECTION_SUMMARY -> 2
                else -> 0
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CadSurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("cad_measurement_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CadDimensionYellow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Straighten,
                            contentDescription = "Measurement",
                            tint = CadDimensionYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CAD Measurement Suite",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "14 Precision Tools • World Coordinates • Multi-Unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_measurement_sheet_btn")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Unit Selector Section (Metric: mm, cm, m; Imperial: in, ft)
            Text(
                text = "MEASUREMENT UNIT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CadCyan
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CadUnit.values().forEach { unit ->
                    val isSelected = unit == currentUnit
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CadDimensionYellow else CadSurfaceVariantDark)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CadDimensionYellow else Color(0xFF334155),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onChangeUnit(unit) }
                            .padding(vertical = 6.dp)
                            .testTag("unit_selector_${unit.abbreviation}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = unit.abbreviation,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isSelected) Color.Black else Color.White
                            )
                            Text(
                                text = if (unit == CadUnit.INCHES || unit == CadUnit.FEET) "Imp" else "Met",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Measurement Summary Card (if present)
            if (activeResult != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_measurement_summary_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2433)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(CadDimensionYellow)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getMeasurementTypeIcon(activeResult.type),
                                    contentDescription = null,
                                    tint = CadDimensionYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeResult.type.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = CadDimensionYellow
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = onCopyResult,
                                    modifier = Modifier.size(28.dp).testTag("sheet_copy_measurement_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy Result",
                                        tint = CadCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onClearResult,
                                    modifier = Modifier.size(28.dp).testTag("sheet_clear_measurement_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear Result",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = activeResult.primaryFormatted,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )

                        // Prominent 4-Metric Display Grid (Area, Perimeter, Total Length, Object Count)
                        val metrics = listOfNotNull(
                            activeResult.displayArea?.let { "Area" to it },
                            activeResult.displayPerimeter?.let { "Perimeter" to it },
                            activeResult.displayTotalLength?.let { "Total Length" to it },
                            activeResult.displayObjectCount?.let { "Count" to it }
                        )

                        if (metrics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                metrics.forEach { (label, value) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F172A))
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = label.uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                color = CadCyan
                                            )
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Secondary Details
                        if (activeResult.secondaryDetails.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                activeResult.secondaryDetails.take(3).forEach { (k, v) ->
                                    Text(
                                        text = "$k: $v",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp
                                        ),
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Navigation Tabs (Tools, Multi-Area, Selection, History)
            val tabs = listOf(
                "Tools (14)",
                "Multi-Area (${multiAreaRegions.size})",
                "Selection ($selectedCount)",
                "History (${measurementHistory.size})"
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E2430),
                contentColor = CadDimensionYellow,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = CadDimensionYellow
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (selectedTab == index) CadDimensionYellow else Color.Gray
                            )
                        },
                        modifier = Modifier.testTag("measurement_tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: 14 Precision Tools List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .testTag("measurement_tools_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(CadMeasurementType.values()) { type ->
                            val isSelected = type == activeType
                            MeasurementToolItem(
                                type = type,
                                isSelected = isSelected,
                                onClick = {
                                    onSelectTool(type)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // TAB 1: Multi-Area Manager (Add / Subtract Regions)
                    MultiAreaManagerTab(
                        regions = multiAreaRegions,
                        isSubtractMode = isSubtractMode,
                        currentUnit = currentUnit,
                        activeResult = activeResult as? CadMeasurementResult.MultiArea,
                        onAddRegion = { onAddRegion(false) },
                        onAddVoidRegion = { onAddRegion(true) },
                        onToggleSubtract = onToggleRegionSubtract,
                        onRemoveRegion = onRemoveRegion,
                        onClearAll = onClearRegions,
                        onActivateMultiAreaTool = {
                            onSelectTool(CadMeasurementType.MULTI_AREA)
                            onDismiss()
                        }
                    )
                }
                2 -> {
                    // TAB 2: Selection Measurement (Total area, length, object count)
                    SelectionMeasurementTab(
                        selectedCount = selectedCount,
                        activeResult = activeResult as? CadMeasurementResult.SelectionSummary,
                        onMeasureSelection = onMeasureSelection,
                        onActivateTool = {
                            onSelectTool(CadMeasurementType.SELECTION_SUMMARY)
                            onDismiss()
                        }
                    )
                }
                3 -> {
                    // TAB 3: Measurement History
                    MeasurementHistoryTab(
                        history = measurementHistory,
                        onRecall = { item ->
                            onRecallHistoryItem(item)
                            onDismiss()
                        },
                        onDelete = onDeleteHistoryItem,
                        onClearAll = onClearHistory
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MultiAreaManagerTab(
    regions: List<CadMeasurementResult.CadAreaRegion>,
    isSubtractMode: Boolean,
    currentUnit: CadUnit,
    activeResult: CadMeasurementResult.MultiArea?,
    onAddRegion: () -> Unit,
    onAddVoidRegion: () -> Unit,
    onToggleSubtract: (String) -> Unit,
    onRemoveRegion: (String) -> Unit,
    onClearAll: () -> Unit,
    onActivateMultiAreaTool: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
            .testTag("multi_area_manager_tab"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Action Row: Add Region (+) / Void Region (-)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddRegion,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.weight(1f).testTag("btn_add_area_region"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Region (+)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onAddVoidRegion,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.weight(1f).testTag("btn_void_area_region"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Void Region (-)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (regions.isNotEmpty()) {
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.size(36.dp).testTag("btn_clear_multi_area")
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear Regions", tint = Color.LightGray)
                }
            }
        }

        if (regions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Area Regions Added",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 3+ points on canvas or tap a closed polyline/circle, then press '+ Add' to union or '- Void' to subtract.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onActivateMultiAreaTool,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Start Multi-Area Tool", color = CadDimensionYellow, fontSize = 12.sp)
                    }
                }
            }
        } else {
            // Region list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(regions) { region ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (region.isSubtract) Color(0x28EF4444) else Color(0x2810B981),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (region.isSubtract) Color(0xFFEF4444) else Color(0xFF10B981)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("region_item_${region.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (region.isSubtract) "[-] VOID" else "[+] ADD",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (region.isSubtract) Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = region.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Area: ${currentUnit.formatArea(region.areaSquareUnits)} | Perim: ${currentUnit.format(region.perimeter)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = Color.LightGray
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Toggle Add / Void
                                IconButton(
                                    onClick = { onToggleSubtract(region.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "Toggle Add/Subtract",
                                        tint = CadCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                // Delete
                                IconButton(
                                    onClick = { onRemoveRegion(region.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove Region",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionMeasurementTab(
    selectedCount: Int,
    activeResult: CadMeasurementResult.SelectionSummary?,
    onMeasureSelection: () -> Unit,
    onActivateTool: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
            .testTag("selection_measurement_tab"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SELECTION SUMMARY TOOL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = CadDimensionYellow
                        )
                        Text(
                            text = "$selectedCount entities currently selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }

                    Button(
                        onClick = onMeasureSelection,
                        colors = ButtonDefaults.buttonColors(containerColor = CadDimensionYellow, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_measure_current_selection")
                    ) {
                        Text("Calculate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (activeResult != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CadDimensionYellow),
                modifier = Modifier.fillMaxWidth().testTag("selection_summary_result_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COMBINED SELECTION METRICS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CadCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(modifier = Modifier.weight(1f), label = "TOTAL AREA", value = activeResult.displayArea ?: "0.0")
                        MetricCard(modifier = Modifier.weight(1f), label = "TOTAL LENGTH", value = activeResult.displayTotalLength ?: "0.0")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(modifier = Modifier.weight(1f), label = "PERIMETER", value = activeResult.displayPerimeter ?: "0.0")
                        MetricCard(modifier = Modifier.weight(1f), label = "OBJECT COUNT", value = "${activeResult.objectCount} items")
                    }
                }
            }
        } else {
            Text(
                text = "Tap any CAD entities (lines, polylines, circles, arcs) on the canvas to inspect combined total length, area, perimeter, and object quantity count.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Button(
                onClick = onActivateTool,
                colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariantDark),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.SelectAll, contentDescription = null, tint = CadDimensionYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Activate Selection Measurement Mode", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier = Modifier, label: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E2430))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = CadCyan)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = Color.White
            )
        }
    }
}

@Composable
fun MeasurementHistoryTab(
    history: List<CadMeasurementHistoryItem>,
    onRecall: (CadMeasurementHistoryItem) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
            .testTag("measurement_history_tab"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED MEASUREMENTS (${history.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CadCyan
            )
            if (history.isNotEmpty()) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .clickable(onClick = onClearAll)
                        .testTag("clear_history_btn")
                )
            }
        }

        if (history.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No Measurement History",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Measurements calculated on canvas will be automatically saved here with full metrics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(history) { item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2430),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E384D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRecall(item) }
                            .testTag("history_item_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getMeasurementTypeIcon(item.type),
                                        contentDescription = null,
                                        tint = CadDimensionYellow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.type.title.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = CadDimensionYellow
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormat.format(Date(item.timestamp)),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.Gray
                                    )
                                }

                                IconButton(
                                    onClick = { onDelete(item.id) },
                                    modifier = Modifier.size(24.dp).testTag("delete_history_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Delete",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.primaryValue,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )

                            // Metric pills
                            val pills = listOfNotNull(
                                item.area?.let { "Area: $it" },
                                item.perimeter?.let { "Perim: $it" },
                                item.totalLength?.let { "Len: $it" },
                                item.objectCount?.let { "Count: $it" }
                            )

                            if (pills.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pills.forEach { pill ->
                                        Text(
                                            text = pill,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementToolItem(
    type: CadMeasurementType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = getMeasurementTypeIcon(type)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CadDimensionYellow.copy(alpha = 0.15f) else Color(0xFF1E2430),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) CadDimensionYellow else Color(0xFF2E384D)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("measurement_tool_${type.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) CadDimensionYellow else CadSurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = type.title,
                    tint = if (isSelected) Color.Black else CadDimensionYellow,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = type.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) CadDimensionYellow else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (type.maxPoints != null) "${type.minPoints} pts" else "${type.minPoints}+ pts",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
                Text(
                    text = type.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = Color.LightGray,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = CadDimensionYellow,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Active CAD Measurement HUD Banner rendered at the top of the canvas.
 * Shows dynamic real-time values, secondary metrics (Area, Perimeter, Total Length, Count),
 * unit selector chips, copy, clear, and multi-area add/subtract buttons.
 */
@Composable
fun CadMeasurementActiveBanner(
    type: CadMeasurementType,
    result: CadMeasurementResult?,
    currentUnit: CadUnit,
    pointCount: Int,
    multiAreaRegions: List<CadMeasurementResult.CadAreaRegion> = emptyList(),
    isSubtractMode: Boolean = false,
    onSelectUnit: (CadUnit) -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    onAddRegion: ((Boolean) -> Unit)? = null,
    onFinishMultiPoint: (() -> Unit)? = null,
    onOpenToolSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("cad_measurement_active_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadDimensionYellow)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Tool info, Add/Void buttons (if multi-area), Copy, Clear, Sheet button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = onOpenToolSheet)
                        .testTag("measurement_hud_title_row")
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CadDimensionYellow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getMeasurementTypeIcon(type),
                            contentDescription = type.title,
                            tint = CadDimensionYellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = type.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = CadDimensionYellow
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Multi-Area Quick Buttons in HUD
                    if (type == CadMeasurementType.MULTI_AREA && onAddRegion != null) {
                        Button(
                            onClick = { onAddRegion(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("hud_add_region_btn")
                        ) {
                            Text("+ Add", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Button(
                            onClick = { onAddRegion(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("hud_void_region_btn")
                        ) {
                            Text("- Void", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (onFinishMultiPoint != null) {
                        Button(
                            onClick = onFinishMultiPoint,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CadDimensionYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("measurement_finish_btn")
                        ) {
                            Text("Finish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Copy Button
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp).testTag("measurement_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Measurement",
                            tint = CadCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clear / Close Button
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(28.dp).testTag("measurement_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear Measurement",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Row 2: Dynamic Primary measurement value
            if (result != null) {
                Text(
                    text = result.primaryFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp
                    ),
                    color = Color.White
                )

                // Row of 4 Core Metrics: Area, Perimeter, Total Length, Object Count
                val coreMetrics = listOfNotNull(
                    result.displayArea?.let { "Area: $it" },
                    result.displayPerimeter?.let { "Perim: $it" },
                    result.displayTotalLength?.let { "Len: $it" },
                    result.displayObjectCount?.let { "Count: $it" }
                )

                if (coreMetrics.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        coreMetrics.forEach { metric ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = metric,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = CadCyan
                                )
                            }
                        }
                    }
                }

                // Secondary details line
                if (result.secondaryDetails.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        result.secondaryDetails.take(3).forEach { (k, v) ->
                            Text(
                                text = "$k: $v",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = type.promptForStep(pointCount),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = Color(0xFF94A3B8)
                )
            }

            // Row 3: Unit selector chips row (mm, cm, m, in, ft)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Unit:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.Gray
                )
                CadUnit.values().forEach { u ->
                    val isSelected = u == currentUnit
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CadDimensionYellow else Color(0xFF1E293B))
                            .clickable { onSelectUnit(u) }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                            .testTag("hud_unit_${u.abbreviation}")
                    ) {
                        Text(
                            text = u.abbreviation,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color.Black else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

fun getMeasurementTypeIcon(type: CadMeasurementType): ImageVector {
    return when (type) {
        CadMeasurementType.DISTANCE -> Icons.Filled.Straighten
        CadMeasurementType.ALIGNED_DISTANCE -> Icons.Filled.LinearScale
        CadMeasurementType.HORIZONTAL_DISTANCE -> Icons.Filled.VerticalAlignBottom
        CadMeasurementType.VERTICAL_DISTANCE -> Icons.Filled.VerticalAlignTop
        CadMeasurementType.ANGLE -> Icons.Filled.FormatShapes
        CadMeasurementType.RADIUS -> Icons.Filled.RadioButtonUnchecked
        CadMeasurementType.DIAMETER -> Icons.Filled.AspectRatio
        CadMeasurementType.AREA -> Icons.Filled.CropFree
        CadMeasurementType.PERIMETER -> Icons.Filled.CropFree
        CadMeasurementType.COORDINATE -> Icons.Filled.MyLocation
        CadMeasurementType.POLYLINE_LENGTH -> Icons.Filled.Timeline
        CadMeasurementType.BOUNDING_BOX -> Icons.Filled.OpenWith
        CadMeasurementType.MULTI_AREA -> Icons.Filled.Layers
        CadMeasurementType.SELECTION_SUMMARY -> Icons.Filled.SelectAll
    }
}

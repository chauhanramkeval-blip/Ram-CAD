package com.example.ui.screens.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.export.CadColorMode
import com.example.cad.export.CadDrawingScale
import com.example.cad.export.CadImageBackgroundMode
import com.example.cad.export.CadImageExportConfig
import com.example.cad.export.CadImageExporter
import com.example.cad.export.CadImageFormat
import com.example.cad.export.CadImageResolution
import com.example.cad.export.CadLineweightStyle
import com.example.cad.export.CadMeasurementReportConfig
import com.example.cad.export.CadMeasurementReportExporter
import com.example.cad.export.CadPageOrientation
import com.example.cad.export.CadPdfExportConfig
import com.example.cad.export.CadPdfExporter
import com.example.cad.export.CadPdfPageSize
import com.example.cad.export.CadPrintShareManager
import com.example.cad.export.CadReportFormat
import com.example.cad.export.ExportResult
import com.example.cad.model.CadDocument
import com.example.cad.model.CadLayer
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadDimensionYellow
import com.example.ui.theme.CadSnapGreen
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.hypot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadExportBottomSheet(
    document: CadDocument,
    measurementHistory: List<CadMeasurementHistoryItem> = emptyList(),
    activeMeasurement: com.example.cad.engine.measurement.CadMeasurementResult? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: PDF, 1: Image, 2: Report

    // PDF Config State
    var pdfTitle by remember { mutableStateOf(document.title.removeSuffix(".dxf").removeSuffix(".dwg").removeSuffix(".cadproj")) }
    var pdfPageSize by remember { mutableStateOf(CadPdfPageSize.A4) }
    var pdfOrientation by remember { mutableStateOf(CadPageOrientation.LANDSCAPE) }
    var pdfScale by remember { mutableStateOf(CadDrawingScale.FIT_TO_PAGE) }
    var customScaleRatio by remember { mutableStateOf("100") }
    var pdfIncludeGrid by remember { mutableStateOf(false) }
    var pdfIncludeMeasurements by remember { mutableStateOf(true) }
    var pdfIncludeTitleBlock by remember { mutableStateOf(true) }
    var pdfIncludeBorder by remember { mutableStateOf(true) }
    var pdfColorMode by remember { mutableStateOf(CadColorMode.LAYER_COLORS) }
    var pdfLineweight by remember { mutableStateOf(CadLineweightStyle.WYSIWYG) }
    var pdfAuthor by remember { mutableStateOf("CAD Mobile") }

    // Image Config State
    var imageFormat by remember { mutableStateOf(CadImageFormat.PNG) }
    var imageResolution by remember { mutableStateOf(CadImageResolution.HIGH_RES_2X) }
    var imageBgMode by remember { mutableStateOf(CadImageBackgroundMode.WHITE_PLOT) }
    var imageIncludeGrid by remember { mutableStateOf(false) }
    var imageIncludeMeasurements by remember { mutableStateOf(true) }

    // Report Config State
    var reportFormat by remember { mutableStateOf(CadReportFormat.PDF_REPORT) }
    var reportIncludeInventory by remember { mutableStateOf(true) }
    var reportIncludeLayers by remember { mutableStateOf(true) }

    // Layer Filtering (Shared across exports)
    var selectedLayerIds by remember {
        mutableStateOf(document.layers.filter { it.value.isVisible }.keys.toSet())
    }
    var showLayerSelector by remember { mutableStateOf(false) }

    // Execution & Feedback State
    var isExporting by remember { mutableStateOf(false) }
    var exportStatusText by remember { mutableStateOf<String?>(null) }
    var completedExportResult by remember { mutableStateOf<ExportResult?>(null) }

    // Exporters
    val pdfExporter = remember { CadPdfExporter(context) }
    val imageExporter = remember { CadImageExporter(context) }
    val reportExporter = remember { CadMeasurementReportExporter(context) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("cad_export_bottom_sheet")
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Export & Print Drawing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Professional PDF, PNG/JPEG, and Measurement Takeoffs",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("export_close_btn")) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs: PDF, Image, Report
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = CadSurfaceVariantDark,
            contentColor = CadCyan,
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("PDF Document", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_export_pdf")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Image", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_export_image")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Takeoff Report", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_export_report")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> {
                    // ==================== PDF TAB ====================
                    Text("PAGE SETUP & SCALE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CadCyan)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Title Field
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        label = { Text("Drawing Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CadCyan,
                            unfocusedBorderColor = CadBorderDark,
                            focusedContainerColor = CadSurfaceVariantDark,
                            unfocusedContainerColor = CadSurfaceVariantDark
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Page Size Selector
                    Text("Page Size", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CadPdfPageSize.entries.forEach { size ->
                            FilterChip(
                                selected = pdfPageSize == size,
                                onClick = { pdfPageSize = size },
                                label = { Text(size.name) },
                                leadingIcon = if (pdfPageSize == size) { { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) } } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CadCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = CadCyan,
                                    containerColor = CadSurfaceVariantDark,
                                    labelColor = Color(0xFFCCCCCC)
                                ),
                                modifier = Modifier.testTag("chip_size_${size.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Orientation Selector
                    Text("Orientation", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CadPageOrientation.entries.forEach { orient ->
                            FilterChip(
                                selected = pdfOrientation == orient,
                                onClick = { pdfOrientation = orient },
                                label = { Text(orient.displayName) },
                                modifier = Modifier.weight(1f).testTag("chip_orient_${orient.name}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CadCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = CadCyan,
                                    containerColor = CadSurfaceVariantDark,
                                    labelColor = Color(0xFFCCCCCC)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Drawing Scale Selector
                    Text("Drawing Scale", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CadDrawingScale.entries.forEach { sc ->
                            FilterChip(
                                selected = pdfScale == sc,
                                onClick = { pdfScale = sc },
                                label = { Text(if (sc == CadDrawingScale.FIT_TO_PAGE) "Fit to Page" else sc.name.replace("SCALE_", "").replace("_", ":")) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CadDimensionYellow.copy(alpha = 0.25f),
                                    selectedLabelColor = CadDimensionYellow,
                                    containerColor = CadSurfaceVariantDark,
                                    labelColor = Color(0xFFCCCCCC)
                                ),
                                modifier = Modifier.testTag("chip_scale_${sc.name}")
                            )
                        }
                    }

                    if (pdfScale == CadDrawingScale.CUSTOM) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customScaleRatio,
                            onValueChange = { customScaleRatio = it },
                            label = { Text("Custom Ratio (1 : X)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CadCyan,
                                unfocusedBorderColor = CadBorderDark,
                                focusedContainerColor = CadSurfaceVariantDark,
                                unfocusedContainerColor = CadSurfaceVariantDark
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("export_custom_scale_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Line Visibility & Styling
                    Text("LINE VISIBILITY & PLOT STYLE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CadCyan)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = pdfColorMode == CadColorMode.LAYER_COLORS,
                            onClick = { pdfColorMode = CadColorMode.LAYER_COLORS },
                            label = { Text("Color Plot") },
                            modifier = Modifier.weight(1f).testTag("chip_color_mode")
                        )
                        FilterChip(
                            selected = pdfColorMode == CadColorMode.MONOCHROME,
                            onClick = { pdfColorMode = CadColorMode.MONOCHROME },
                            label = { Text("Monochrome B&W") },
                            modifier = Modifier.weight(1f).testTag("chip_mono_mode")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Lineweight style
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CadLineweightStyle.entries.forEach { lw ->
                            FilterChip(
                                selected = pdfLineweight == lw,
                                onClick = { pdfLineweight = lw },
                                label = { Text(lw.displayName.split(" ").first()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CadCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CadCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Options Switch Toggles
                    ExportToggleRow(label = "Title Block & Legend", checked = pdfIncludeTitleBlock, onCheckedChange = { pdfIncludeTitleBlock = it })
                    ExportToggleRow(label = "Sheet Margin Border", checked = pdfIncludeBorder, onCheckedChange = { pdfIncludeBorder = it })
                    ExportToggleRow(label = "Optional Drafting Grid", checked = pdfIncludeGrid, onCheckedChange = { pdfIncludeGrid = it })
                    ExportToggleRow(label = "Optional Measurements & Dimensions", checked = pdfIncludeMeasurements, onCheckedChange = { pdfIncludeMeasurements = it })
                }

                1 -> {
                    // ==================== IMAGE TAB ====================
                    Text("IMAGE FORMAT & RESOLUTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CadCyan)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Format Toggle: PNG vs JPEG
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = imageFormat == CadImageFormat.PNG,
                            onClick = { imageFormat = CadImageFormat.PNG },
                            label = { Text("PNG (Lossless Vector Raster)") },
                            modifier = Modifier.weight(1f).testTag("chip_format_png")
                        )
                        FilterChip(
                            selected = imageFormat == CadImageFormat.JPEG,
                            onClick = {
                                imageFormat = CadImageFormat.JPEG
                                if (imageBgMode == CadImageBackgroundMode.TRANSPARENT_PNG) {
                                    imageBgMode = CadImageBackgroundMode.WHITE_PLOT
                                }
                            },
                            label = { Text("JPEG (Compact)") },
                            modifier = Modifier.weight(1f).testTag("chip_format_jpeg")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Resolution
                    Text("Resolution & Quality", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CadImageResolution.entries.forEach { res ->
                            FilterChip(
                                selected = imageResolution == res,
                                onClick = { imageResolution = res },
                                label = { Text(res.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CadCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = CadCyan
                                ),
                                modifier = Modifier.testTag("chip_res_${res.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Background Mode
                    Text("Canvas Background", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = imageBgMode == CadImageBackgroundMode.WHITE_PLOT,
                            onClick = { imageBgMode = CadImageBackgroundMode.WHITE_PLOT },
                            label = { Text("White (Plot Style)") }
                        )
                        FilterChip(
                            selected = imageBgMode == CadImageBackgroundMode.DARK_CANVAS,
                            onClick = { imageBgMode = CadImageBackgroundMode.DARK_CANVAS },
                            label = { Text("Dark Canvas (CAD)") }
                        )
                        if (imageFormat == CadImageFormat.PNG) {
                            FilterChip(
                                selected = imageBgMode == CadImageBackgroundMode.TRANSPARENT_PNG,
                                onClick = { imageBgMode = CadImageBackgroundMode.TRANSPARENT_PNG },
                                label = { Text("Transparent") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ExportToggleRow(label = "Include Grid", checked = imageIncludeGrid, onCheckedChange = { imageIncludeGrid = it })
                    ExportToggleRow(label = "Include Measurements & Dimensions", checked = imageIncludeMeasurements, onCheckedChange = { imageIncludeMeasurements = it })
                }

                2 -> {
                    // ==================== TAKEOFF REPORT TAB ====================
                    Text("QUANTITY TAKEOFF & MEASUREMENT REPORT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CadCyan)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Report Format
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CadReportFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = reportFormat == fmt,
                                onClick = { reportFormat = fmt },
                                label = { Text(fmt.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CadSnapGreen.copy(alpha = 0.25f),
                                    selectedLabelColor = CadSnapGreen
                                ),
                                modifier = Modifier.testTag("chip_report_${fmt.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document Inventory Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Drawing Takeoff Summary", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Spacer(modifier = Modifier.height(6.dp))

                            var totalLength = 0.0
                            var totalArea = 0.0
                            for (e in document.entities) {
                                when (e) {
                                    is com.example.cad.model.CadEntity.Line -> totalLength += hypot(e.end.x - e.start.x, e.end.y - e.start.y)
                                    is com.example.cad.model.CadEntity.Polyline -> {
                                        for (i in 0 until e.points.size - 1) {
                                            totalLength += hypot(e.points[i + 1].x - e.points[i].x, e.points[i + 1].y - e.points[i].y)
                                        }
                                        if (e.isClosed && e.points.size >= 3) {
                                            totalLength += hypot(e.points.first().x - e.points.last().x, e.points.first().y - e.points.last().y)
                                            var sum = 0.0
                                            val n = e.points.size
                                            for (i in 0 until n) {
                                                val j = (i + 1) % n
                                                sum += (e.points[i].x * e.points[j].y - e.points[j].x * e.points[i].y).toDouble()
                                            }
                                            totalArea += kotlin.math.abs(sum) / 2.0
                                        }
                                    }
                                    is com.example.cad.model.CadEntity.Circle -> {
                                        totalLength += 2 * PI * e.radius
                                        totalArea += PI * e.radius * e.radius
                                    }
                                    is com.example.cad.model.CadEntity.Arc -> totalLength += (kotlin.math.abs(e.sweepAngleDeg) / 360.0) * 2 * PI * e.radius
                                    else -> {}
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Linear Length:", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Text("%.2f %s".format(totalLength, document.units.abbreviation), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CadCyan)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Closed Area:", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Text("%.2f sq %s".format(totalArea, document.units.abbreviation), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = CadDimensionYellow)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Entities:", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Text("${document.entities.size} (${document.layers.size} layers)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Recorded Measurements:", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                Text("${measurementHistory.size} items", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = CadSnapGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    ExportToggleRow(label = "Include Entity-by-Entity Inventory", checked = reportIncludeInventory, onCheckedChange = { reportIncludeInventory = it })
                    ExportToggleRow(label = "Include Layer Breakdown Statistics", checked = reportIncludeLayers, onCheckedChange = { reportIncludeLayers = it })
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==================== LAYER SELECTION BAR ====================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CadSurfaceVariantDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Layers, contentDescription = null, tint = CadCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Selected Layers (${selectedLayerIds.size}/${document.layers.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                        TextButton(onClick = { showLayerSelector = !showLayerSelector }) {
                            Text(if (showLayerSelector) "Collapse" else "Filter Layers", color = CadCyan)
                        }
                    }

                    if (showLayerSelector) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedLayerIds = document.layers.keys.toSet() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Select All", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { selectedLayerIds = emptySet() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Deselect All", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        document.layers.values.forEach { layer ->
                            val isChecked = selectedLayerIds.contains(layer.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLayerIds = if (isChecked) {
                                            selectedLayerIds - layer.id
                                        } else {
                                            selectedLayerIds + layer.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color(layer.colorArgb))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = layer.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isChecked) Color.White else Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isChecked) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = CadCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Completed Result Banner
            completedExportResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("export_success_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14301A)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CadSnapGreen))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = CadSnapGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Successful!", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CadSnapGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Saved: ${res.file.name} (${res.fileSizeFormatted})", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    CadPrintShareManager.shareFile(context, res.file, res.mimeType)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CadSnapGreen),
                                modifier = Modifier.weight(1f).testTag("btn_share_exported_file")
                            ) {
                                Icon(Icons.Filled.Share, null, Modifier.size(16.dp), tint = Color.Black)
                                Spacer(Modifier.width(6.dp))
                                Text("Share", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            if (res.mimeType == "application/pdf") {
                                Button(
                                    onClick = {
                                        CadPrintShareManager.printPdf(context, res.file)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                                    modifier = Modifier.weight(1f).testTag("btn_print_exported_file")
                                ) {
                                    Icon(Icons.Filled.Print, null, Modifier.size(16.dp), tint = Color.Black)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Print", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    CadPrintShareManager.openFile(context, res.file, res.mimeType)
                                },
                                modifier = Modifier.weight(1f).testTag("btn_open_exported_file")
                            ) {
                                Icon(Icons.Filled.OpenInNew, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ==================== BOTTOM ACTION BAR ====================
        if (isExporting) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = CadCyan, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(exportStatusText ?: "Generating export...", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Secondary Print Action for PDF
                if (selectedTab == 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatusText = "Preparing PDF for Print..."
                                val customRatio = customScaleRatio.toFloatOrNull() ?: 100f
                                val config = CadPdfExportConfig(
                                    title = pdfTitle,
                                    pageSize = pdfPageSize,
                                    orientation = pdfOrientation,
                                    scale = pdfScale,
                                    customScaleRatio = customRatio,
                                    includeGrid = pdfIncludeGrid,
                                    includeMeasurements = pdfIncludeMeasurements,
                                    includeTitleBlock = pdfIncludeTitleBlock,
                                    includeBorder = pdfIncludeBorder,
                                    selectedLayerIds = selectedLayerIds,
                                    colorMode = pdfColorMode,
                                    lineweightStyle = pdfLineweight,
                                    author = pdfAuthor
                                )
                                val activeList = listOfNotNull(activeMeasurement)
                                val result = pdfExporter.exportPdf(document, config, activeList)
                                isExporting = false
                                result.onSuccess { file ->
                                    completedExportResult = ExportResult(
                                        file = file,
                                        formatName = "PDF Document",
                                        mimeType = "application/pdf",
                                        fileSizeFormatted = formatFileSize(file.length())
                                    )
                                    CadPrintShareManager.printPdf(context, file)
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CadCyan),
                        modifier = Modifier.weight(1f).testTag("btn_export_and_print")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print / Spool")
                    }
                }

                // Primary Export / Share Action
                Button(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            when (selectedTab) {
                                0 -> {
                                    exportStatusText = "Rendering vector PDF..."
                                    val customRatio = customScaleRatio.toFloatOrNull() ?: 100f
                                    val config = CadPdfExportConfig(
                                        title = pdfTitle,
                                        pageSize = pdfPageSize,
                                        orientation = pdfOrientation,
                                        scale = pdfScale,
                                        customScaleRatio = customRatio,
                                        includeGrid = pdfIncludeGrid,
                                        includeMeasurements = pdfIncludeMeasurements,
                                        includeTitleBlock = pdfIncludeTitleBlock,
                                        includeBorder = pdfIncludeBorder,
                                        selectedLayerIds = selectedLayerIds,
                                        colorMode = pdfColorMode,
                                        lineweightStyle = pdfLineweight,
                                        author = pdfAuthor
                                    )
                                    val activeList = listOfNotNull(activeMeasurement)
                                    val res = pdfExporter.exportPdf(document, config, activeList)
                                    isExporting = false
                                    res.onSuccess { f ->
                                        val er = ExportResult(
                                            file = f,
                                            formatName = "PDF Drawing",
                                            mimeType = "application/pdf",
                                            fileSizeFormatted = formatFileSize(f.length())
                                        )
                                        completedExportResult = er
                                        CadPrintShareManager.shareFile(context, f, "application/pdf")
                                    }
                                }
                                1 -> {
                                    exportStatusText = "Rendering raster image..."
                                    val config = CadImageExportConfig(
                                        format = imageFormat,
                                        resolution = imageResolution,
                                        backgroundMode = imageBgMode,
                                        includeGrid = imageIncludeGrid,
                                        includeMeasurements = imageIncludeMeasurements,
                                        selectedLayerIds = selectedLayerIds
                                    )
                                    val activeList = listOfNotNull(activeMeasurement)
                                    val res = imageExporter.exportImage(document, config, activeList)
                                    isExporting = false
                                    res.onSuccess { f ->
                                        val er = ExportResult(
                                            file = f,
                                            formatName = imageFormat.name,
                                            mimeType = imageFormat.mimeType,
                                            fileSizeFormatted = formatFileSize(f.length())
                                        )
                                        completedExportResult = er
                                        CadPrintShareManager.shareFile(context, f, imageFormat.mimeType)
                                    }
                                }
                                2 -> {
                                    exportStatusText = "Compiling takeoff report..."
                                    val config = CadMeasurementReportConfig(
                                        format = reportFormat,
                                        includeEntityBreakdown = reportIncludeInventory,
                                        includeLayerSummary = reportIncludeLayers
                                    )
                                    val res = reportExporter.exportReport(document, config, measurementHistory, activeMeasurement)
                                    isExporting = false
                                    res.onSuccess { f ->
                                        val er = ExportResult(
                                            file = f,
                                            formatName = reportFormat.displayName,
                                            mimeType = reportFormat.mimeType,
                                            fileSizeFormatted = formatFileSize(f.length())
                                        )
                                        completedExportResult = er
                                        CadPrintShareManager.shareFile(context, f, reportFormat.mimeType)
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                    modifier = Modifier.weight(1.2f).testTag("btn_export_primary")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (selectedTab) {
                            0 -> "Export & Share PDF"
                            1 -> "Export Image"
                            else -> "Export Report"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CadCyan,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = CadSurfaceVariantDark
            )
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
    }
}

package com.example.ui.screens.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.cad.engine.edit.CadEditMath
import com.example.cad.engine.edit.EditCommandState
import com.example.cad.engine.edit.EditOperationType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.engine.MeasurementResult
import com.example.cad.engine.measurement.CadMeasurementRenderer
import com.example.cad.engine.measurement.CadMeasurementType
import com.example.cad.engine.snap.CadSnapMarkerRenderer
import com.example.cad.model.CadEntity
import com.example.cad.model.CadLayer
import com.example.ui.components.CadFormatChip
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCanvasBlack
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadDimensionYellow
import com.example.ui.theme.CadSnapGreen
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark
import com.example.ui.screens.editor.tools.CadActionDispatcher
import com.example.ui.screens.editor.tools.CadBottomToolbar
import com.example.ui.screens.editor.tools.CommandManager
import com.example.ui.screens.editor.tools.PopupCategory
import com.example.ui.screens.editor.tools.PopupToolPanel
import com.example.ui.screens.editor.tools.ToolManager
import com.example.ui.screens.editor.tools.ToolRegistry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadEditorScreen(
    viewModel: CadEditorViewModel,
    drawingId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var viewportSize by remember { mutableStateOf(IntSize(1080, 1920)) }
    var showMoveDialog by remember { mutableStateOf(false) }

    val actionDispatcher = remember(viewModel, uiState) {
        object : CadActionDispatcher {
            override val hasSelection: Boolean
                get() = uiState.selectionState.hasSelection
            override val selectedCount: Int
                get() = uiState.selectionState.count
            override val activeTool: CadTool
                get() = uiState.activeTool

            override fun selectCadTool(tool: CadTool) = viewModel.selectTool(tool)
            override fun startEditCommand(operation: EditOperationType) = viewModel.startEditCommand(operation)
            override fun deleteSelectedEntities() = viewModel.deleteSelectedEntities()
            override fun openLayersSheet() = viewModel.toggleLayersSheet(true)
            override fun createNewLayer(name: String, colorArgb: Long) = viewModel.addNewLayer(name, colorArgb)
            override fun deleteCurrentLayer() = viewModel.deleteCurrentLayer()
            override fun cycleCurrentLayer() = viewModel.cycleCurrentLayer()
            override fun toggleCurrentLayerVisibility() = viewModel.toggleCurrentLayerVisibility()
            override fun toggleCurrentLayerLock() = viewModel.toggleCurrentLayerLock()
            override fun changeCurrentLayerColor(colorArgb: Long) = viewModel.changeCurrentLayerColor(colorArgb)
            override fun setSelectionColor(colorArgb: Long?) = viewModel.changeSelectedEntitiesColor(colorArgb)
            override fun openPropertiesSheet() = viewModel.togglePropertiesSheet(true)
            override fun toggleGrid() = viewModel.toggleGrid()
            override fun toggleSnap() = viewModel.toggleSnap()
            override fun openOsnapSheet() = viewModel.openOsnapSheet()
            override fun toggleOrtho() = viewModel.toggleOrtho()
            override fun toggleCoordinates() = viewModel.toggleCrosshair()
            override fun cycleUnits() = viewModel.cycleUnits()
            override fun cyclePrecision() = viewModel.cyclePrecision()
            override fun toggleLineweights() = viewModel.toggleLineweights()
            override fun cycleCanvasBackground() = viewModel.cycleCanvasBackground()
            override fun openMeasurementSheet() = viewModel.toggleMeasurementSheet(true)
            override fun showStatusMessage(message: String) = viewModel.showStatusMessage(message)
        }
    }
    val commandManager = remember(actionDispatcher) { CommandManager(actionDispatcher) }
    val toolManager = remember { ToolManager() }

    LaunchedEffect(drawingId) {
        viewModel.loadDrawing(drawingId)
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(uiState.canvasBackgroundColorArgb))
    ) {
        val isTablet = maxWidth >= 600.dp
        val isLandscape = maxWidth > maxHeight

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Toolbar with Open, Save, Undo, Redo, Layers, Measure, Settings
            CadEditorTopBar(
                title = uiState.drawingTitle,
                format = uiState.format,
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                hasUnsavedChanges = uiState.hasUnsavedChanges,
                isAutoSaving = uiState.isAutoSaving,
                onBack = onNavigateBack,
                onOpen = onNavigateBack, // Returns to file list/browser to select
                onSave = { viewModel.saveDocument() },
                onSaveFormat = { format -> viewModel.saveDocument(format) },
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onLayersClick = { viewModel.toggleLayersSheet(true) },
                onMeasureClick = { viewModel.toggleMeasurementSheet(true) },
                onExportClick = { viewModel.toggleExportSheet(true) },
                onZoomFit = { viewModel.zoomExtents(viewportSize.width.toFloat(), viewportSize.height.toFloat()) }
            )

            // Crash Recovery Prompt Banner
            if (uiState.showCrashRecoveryPrompt) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("editor_crash_recovery_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF332000)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Crash Recovery Available", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFD54F))
                                Text("Unsaved session from unexpected close found on disk", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFE082))
                            }
                        }
                        Row {
                            TextButton(onClick = { viewModel.dismissCrashRecovery(discard = true) }) {
                                Text("Discard", color = Color(0xFFFF8A80))
                            }
                            Button(
                                onClick = { viewModel.restoreCrashRecovery() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                            ) {
                                Text("Restore", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Viewport and Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Interactive CAD Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("cad_viewport_canvas")
                        .onSizeChanged { size ->
                            viewportSize = size
                        }
                        .pointerInput(uiState.activeTool, uiState.activeEditCommand) {
                            if (uiState.activeEditCommand is EditCommandState.Active) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    viewModel.updateCursorPos(startPos.x, startPos.y)
                                    viewModel.updateEditCommandPlacement(startPos.x, startPos.y)

                                    var isMultiTouch = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size > 1) {
                                            isMultiTouch = true
                                            break
                                        }
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            break
                                        }
                                        val currentPos = change.position
                                        viewModel.updateCursorPos(currentPos.x, currentPos.y)
                                        viewModel.updateEditCommandPlacement(currentPos.x, currentPos.y)
                                        change.consume()
                                    }
                                }
                            } else if (uiState.activeTool == CadTool.SELECT) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    viewModel.updateCursorPos(startPos.x, startPos.y)

                                    var isDrag = false
                                    var isMultiTouch = false

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size > 1) {
                                            isMultiTouch = true
                                            viewModel.cancelSelectionBox()
                                            break
                                        }
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            break
                                        }

                                        val currentPos = change.position
                                        val distance = (currentPos - startPos).getDistance()

                                        if (!isDrag && distance > 12f) {
                                            isDrag = true
                                            viewModel.startSelectionBox(startPos.x, startPos.y)
                                        }

                                        if (isDrag) {
                                            viewModel.updateSelectionBox(currentPos.x, currentPos.y)
                                            change.consume()
                                        }
                                    }

                                    if (!isMultiTouch) {
                                        if (isDrag) {
                                            viewModel.finishSelectionBox()
                                        } else {
                                            viewModel.onCanvasTapped(startPos.x, startPos.y)
                                        }
                                    }
                                }
                            } else if (viewModel.activeDrawingTool != null) {
                                // Interactive touch-based drawing tool gesture loop
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    viewModel.updateCursorPos(startPos.x, startPos.y)

                                    var isDrag = false
                                    var isMultiTouch = false

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size > 1) {
                                            isMultiTouch = true
                                            break
                                        }
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) {
                                            break
                                        }

                                        val currentPos = change.position
                                        val distance = (currentPos - startPos).getDistance()

                                        if (!isDrag && distance > 14f) {
                                            isDrag = true
                                            viewModel.onDrawingPointerDown(startPos.x, startPos.y)
                                        }

                                        if (isDrag) {
                                            viewModel.onDrawingPointerMove(currentPos.x, currentPos.y)
                                            change.consume()
                                        }
                                    }

                                    if (!isMultiTouch) {
                                        if (isDrag) {
                                            val lastPos = viewModel.uiState.value.cursorScreenPos
                                            viewModel.onDrawingPointerUp(lastPos.x, lastPos.y)
                                        } else {
                                            viewModel.onCanvasTapped(startPos.x, startPos.y)
                                        }
                                    }
                                }
                            } else if (uiState.activeTool.isMeasurementTool) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val startPos = down.position
                                    viewModel.updateCursorPos(startPos.x, startPos.y)

                                    var isMultiTouch = false
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size > 1) {
                                            isMultiTouch = true
                                            break
                                        }
                                        val change = event.changes.firstOrNull() ?: break
                                        if (!change.pressed) break
                                        val currentPos = change.position
                                        viewModel.updateCursorPos(currentPos.x, currentPos.y)
                                        change.consume()
                                    }

                                    if (!isMultiTouch) {
                                        val lastPos = viewModel.uiState.value.cursorScreenPos
                                        viewModel.onCanvasTapped(lastPos.x, lastPos.y)
                                    }
                                }
                            } else {
                                detectTapGestures(
                                    onTap = { offset ->
                                        viewModel.updateCursorPos(offset.x, offset.y)
                                        viewModel.onCanvasTapped(offset.x, offset.y)
                                    },
                                    onDoubleTap = { offset ->
                                        viewModel.onDoubleTapZoom(offset.x, offset.y)
                                    },
                                    onPress = { offset ->
                                        viewModel.updateCursorPos(offset.x, offset.y)
                                    }
                                )
                            }
                        }
                        .pointerInput(uiState.activeTool) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                viewModel.onViewportTransformed(
                                    panDeltaX = pan.x,
                                    panDeltaY = pan.y,
                                    zoomFactor = zoom,
                                    centroidX = centroid.x,
                                    centroidY = centroid.y
                                )
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 2.1 Technical Drafting Grid
                    if (uiState.isGridVisible) {
                        viewModel.cadEngine.renderer.renderGrid(
                            drawScope = this,
                            transform = uiState.viewportTransform,
                            viewportWidth = w,
                            viewportHeight = h,
                            gridSpacing = 500f,
                            subdivisions = 5
                        )
                    }

                    // 2.2 CAD World Origin Axes
                    viewModel.cadEngine.renderer.renderAxes(
                        drawScope = this,
                        transform = uiState.viewportTransform
                    )

                    // 2.3 Document Vector Geometry
                    uiState.document?.let { doc ->
                        viewModel.cadEngine.renderer.renderDocument(
                            drawScope = this,
                            document = doc,
                            transform = uiState.viewportTransform,
                            selectedEntityId = uiState.selectedEntityId,
                            selectedEntityIds = uiState.selectionState.selectedIds,
                            selectionGrips = uiState.selectionState.grips
                        )
                    }

                    // 2.3.1 Active CAD Selection Box (Window = Blue, Crossing = Green)
                    uiState.selectionState.activeBox?.let { box ->
                        viewModel.cadEngine.renderer.renderSelectionBox(
                            drawScope = this,
                            box = box
                        )
                    }

                    // 2.4 Active Drawing Tool Dynamic Preview in progress
                    viewModel.activeDrawingTool?.let { tool ->
                        if (tool.isInProgress) {
                            tool.renderPreview(
                                drawScope = this,
                                transform = uiState.viewportTransform,
                                cursorWorldPos = uiState.cursorWorldPos
                            )
                        }
                    }

                    // 2.4.1 Active Polyline Preview in progress (fallback / backward compatibility)
                    if (viewModel.activeDrawingTool == null && uiState.activePolylinePoints.isNotEmpty()) {
                        val screenPts = uiState.activePolylinePoints.map { pt ->
                            val screenPt = uiState.viewportTransform.worldToScreen(pt)
                            Offset(screenPt.x, screenPt.y)
                        }
                        for (i in 0 until screenPts.size - 1) {
                            drawLine(
                                color = CadCyan,
                                start = screenPts[i],
                                end = screenPts[i + 1],
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                            )
                        }
                        screenPts.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = pt
                            )
                        }
                    }

                    // 2.4.2 Active CAD Editing Command Dynamic Preview (Move, Copy, Rotate, Scale, Trim, Extend)
                    (uiState.activeEditCommand as? EditCommandState.Active)?.let { activeCmd ->
                        if (activeCmd.previewEntities.isNotEmpty()) {
                            val previewDoc = com.example.cad.model.CadDocument(
                                title = "preview",
                                format = uiState.format,
                                units = uiState.units,
                                entities = activeCmd.previewEntities,
                                layers = uiState.document?.layers ?: emptyMap()
                            )
                            viewModel.cadEngine.renderer.renderDocument(
                                drawScope = this,
                                document = previewDoc,
                                transform = uiState.viewportTransform,
                                selectedEntityId = null,
                                selectedEntityIds = emptySet(),
                                selectionGrips = emptyList()
                            )
                        }

                        val baseScreen = uiState.viewportTransform.worldToScreen(activeCmd.basePoint)
                        val currentScreen = uiState.viewportTransform.worldToScreen(activeCmd.currentPoint)

                        // Base point pivot indicator
                        drawCircle(
                            color = CadCyan,
                            radius = 8f,
                            center = Offset(baseScreen.x, baseScreen.y),
                            style = Stroke(width = 2f)
                        )
                        drawCircle(
                            color = CadCyan,
                            radius = 2f,
                            center = Offset(baseScreen.x, baseScreen.y)
                        )

                        // Guide rubberband line for Move & Copy
                        if (activeCmd.operation == EditOperationType.MOVE || activeCmd.operation == EditOperationType.COPY) {
                            drawLine(
                                color = CadCyan,
                                start = Offset(baseScreen.x, baseScreen.y),
                                end = Offset(currentScreen.x, currentScreen.y),
                                strokeWidth = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                            drawCircle(
                                color = CadDimensionYellow,
                                radius = 5f,
                                center = Offset(currentScreen.x, currentScreen.y)
                            )
                        } else if (activeCmd.operation == EditOperationType.ROTATE) {
                            drawLine(
                                color = CadDimensionYellow,
                                start = Offset(baseScreen.x, baseScreen.y),
                                end = Offset(currentScreen.x, currentScreen.y),
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                            )
                        } else if (activeCmd.operation == EditOperationType.TRIM && activeCmd.trimRemovedSegment != null) {
                            val seg = activeCmd.trimRemovedSegment
                            val s1 = uiState.viewportTransform.worldToScreen(seg.start)
                            val s2 = uiState.viewportTransform.worldToScreen(seg.end)
                            drawLine(
                                color = Color(0xFFEF4444),
                                start = Offset(s1.x, s1.y),
                                end = Offset(s2.x, s2.y),
                                strokeWidth = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                            )
                        } else if (activeCmd.operation == EditOperationType.EXTEND && activeCmd.extendNewSegment != null) {
                            val seg = activeCmd.extendNewSegment
                            val s1 = uiState.viewportTransform.worldToScreen(seg.start)
                            val s2 = uiState.viewportTransform.worldToScreen(seg.end)
                            drawLine(
                                color = CadCyan,
                                start = Offset(s1.x, s1.y),
                                end = Offset(s2.x, s2.y),
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                            )
                        }
                    }

                    // 2.4.3 CAD Object Snap (OSNAP) Marker and Pick Aperture
                    if (uiState.isSnapEnabled && uiState.osnapSettings.isEnabled) {
                        uiState.activeSnapResult?.let { snap ->
                            val snapScreen = uiState.viewportTransform.worldToScreen(snap.point)
                            CadSnapMarkerRenderer.renderSnap(
                                drawScope = this,
                                snapResult = snap.copy(screenPoint = snapScreen),
                                settings = uiState.osnapSettings,
                                cursorScreen = uiState.cursorScreenPos
                            )
                        }
                    }

                    // 2.4.4 CAD Measurement Annotation (Dimension lines, extension lines, tick marks, angles, areas)
                    if (uiState.activeMeasurementResult != null || uiState.measurementPoints.isNotEmpty()) {
                        CadMeasurementRenderer.renderMeasurement(
                            scope = this,
                            result = uiState.activeMeasurementResult,
                            transform = uiState.viewportTransform,
                            inProgressPoints = uiState.measurementPoints,
                            previewPoint = if (uiState.measurementPoints.isNotEmpty()) uiState.cursorWorldPos else null
                        )
                    }

                    // 2.5 Dynamic Drafting Crosshair with Coordinate Callout
                    if (uiState.isCrosshairVisible) {
                        viewModel.cadEngine.renderer.renderCrosshair(
                            drawScope = this,
                            screenX = uiState.cursorScreenPos.x,
                            screenY = uiState.cursorScreenPos.y,
                            viewportWidth = w,
                            viewportHeight = h,
                            cursorWorldX = uiState.cursorWorldPos.x,
                            cursorWorldY = uiState.cursorWorldPos.y,
                            unitAbbreviation = uiState.units.abbreviation,
                            fullScreenCrosshair = uiState.isFullScreenCrosshair
                        )
                    }
                }

                // Coordinate & Zoom HUD (Top Left)
                CadCoordinateHud(
                    cursorPos = uiState.cursorWorldPos,
                    scale = uiState.viewportTransform.scale,
                    units = uiState.units,
                    activeTool = uiState.activeTool,
                    currentLayerId = uiState.currentLayerId,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )

                // Dynamic CAD Measurement HUD Banner
                val activeMeasureType = uiState.activeMeasurementType ?: uiState.activeTool.toMeasurementType()
                if (activeMeasureType != null && (uiState.activeMeasurementResult != null || uiState.measurementPoints.isNotEmpty() || uiState.multiAreaRegions.isNotEmpty())) {
                    CadMeasurementActiveBanner(
                        type = activeMeasureType,
                        result = uiState.activeMeasurementResult,
                        currentUnit = uiState.measurementUnit,
                        pointCount = uiState.measurementPoints.size,
                        multiAreaRegions = uiState.multiAreaRegions,
                        isSubtractMode = uiState.isSubtractMode,
                        onSelectUnit = { unit -> viewModel.setMeasurementUnit(unit) },
                        onCopy = { viewModel.copyMeasurementResult(context) },
                        onClear = { viewModel.clearMeasurement() },
                        onAddRegion = if (activeMeasureType == CadMeasurementType.MULTI_AREA) {
                            { isSubtract -> viewModel.addCurrentPointsAsRegion(isSubtract) }
                        } else null,
                        onFinishMultiPoint = if (activeMeasureType.minPoints > 2 && uiState.measurementPoints.size >= activeMeasureType.minPoints) {
                            { viewModel.finishMultiPointMeasurement() }
                        } else null,
                        onOpenToolSheet = { viewModel.toggleMeasurementSheet(true) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                } else if (uiState.measurementResult != null) {
                    CadMeasurementBanner(
                        result = uiState.measurementResult!!,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }

                // Active CAD Editing Operation HUD Banner (Move, Copy, Rotate, Scale, Trim, Extend)
                (uiState.activeEditCommand as? EditCommandState.Active)?.let { activeCmd ->
                    CadEditCommandActiveBanner(
                        activeCommand = activeCmd,
                        onConfirm = viewModel::confirmEditCommand,
                        onCancel = viewModel::cancelEditCommand,
                        onNudgeDelta = viewModel::setEditCommandDelta,
                        onSetAngle = viewModel::setEditCommandAngle,
                        onSetScale = viewModel::setEditCommandScale,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (uiState.measurementResult != null) 70.dp else 12.dp)
                    )
                }

                // CAD Selection Controls Banner (when tool is SELECT or entities are selected, and not in active edit command)
                if (uiState.activeEditCommand !is EditCommandState.Active && (uiState.activeTool == CadTool.SELECT || uiState.selectionState.hasSelection)) {
                    CadSelectionToolbarBanner(
                        selectionState = uiState.selectionState,
                        document = uiState.document,
                        onSelectAll = viewModel::selectAll,
                        onClearSelection = viewModel::clearSelection,
                        onToggleMultiSelect = { viewModel.toggleMultiSelect() },
                        onDelete = viewModel::deleteSelectedEntities,
                        onCopy = { viewModel.startEditCommand(EditOperationType.COPY) },
                        onOpenMoveDialog = { showMoveDialog = true },
                        onOpenProperties = { viewModel.togglePropertiesSheet(true) },
                        onStartMove = { viewModel.startEditCommand(EditOperationType.MOVE) },
                        onStartCopy = { viewModel.startEditCommand(EditOperationType.COPY) },
                        onStartRotate = { viewModel.startEditCommand(EditOperationType.ROTATE) },
                        onStartScale = { viewModel.startEditCommand(EditOperationType.SCALE) },
                        onStartTrim = { viewModel.startEditCommand(EditOperationType.TRIM) },
                        onStartExtend = { viewModel.startEditCommand(EditOperationType.EXTEND) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = if (uiState.measurementResult != null) 70.dp else 12.dp)
                    )
                }

                // In-progress Drawing Tool Action Banner (Polyline, Arc, Rectangle, Line, Circle)
                if (uiState.isToolInProgress || uiState.activePolylinePoints.isNotEmpty()) {
                    DrawingToolControlsBanner(
                        toolName = uiState.activeTool.displayName,
                        pointCount = uiState.activePolylinePoints.size,
                        showPolygonClose = (uiState.activeTool == CadTool.POLYLINE || uiState.activeTool == CadTool.CLOUD) && uiState.activePolylinePoints.size >= 3,
                        onFinish = { viewModel.confirmActiveTool(isClosed = false) },
                        onClosePolygon = { viewModel.confirmActiveTool(isClosed = true) },
                        onCancel = { viewModel.cancelActiveTool() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 70.dp)
                    )
                }

                // Properties Panel (Tablet: Floating Right Pane)
                if (isTablet && uiState.selectedEntityId != null && uiState.document != null) {
                    val selectedEntity = uiState.document!!.entities.find { it.id == uiState.selectedEntityId }
                    if (selectedEntity != null) {
                        CadPropertiesPanel(
                            entity = selectedEntity,
                            layers = uiState.document!!.layers.values.toList(),
                            onDelete = viewModel::deleteSelectedEntity,
                            onUpdateEntity = viewModel::updateSelectedEntity,
                            onUpdateText = viewModel::updateSelectedEntityText,
                            onUpdateLayer = viewModel::updateSelectedEntityLayer,
                            onClose = { viewModel.togglePropertiesSheet(false) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .width(320.dp)
                        )
                    }
                }

                // Floating Zoom & View Navigation Controls
                CadFloatingZoomControls(
                    onZoomIn = { viewModel.zoomIn(viewportSize.width.toFloat(), viewportSize.height.toFloat()) },
                    onZoomOut = { viewModel.zoomOut(viewportSize.width.toFloat(), viewportSize.height.toFloat()) },
                    onZoomFit = { viewModel.zoomExtents(viewportSize.width.toFloat(), viewportSize.height.toFloat()) },
                    onPan = { viewModel.selectTool(CadTool.PAN) },
                    isPanActive = uiState.activeTool == CadTool.PAN,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                )

                // Mobile CAD Popup Tool Panel above the bottom toolbar
                PopupToolPanel(
                    category = toolManager.activeCategory,
                    toolManager = toolManager,
                    commandManager = commandManager,
                    isTablet = isTablet,
                    isLandscape = isLandscape,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (toolManager.activeCategory != null) 160.dp else 40.dp)
                )
            }

            // 3. Bottom Status Bar (X, Y, Zoom, Current Layer)
            CadStatusBar(
                cursorPos = uiState.cursorWorldPos,
                zoomScale = uiState.viewportTransform.scale,
                currentLayerId = uiState.currentLayerId,
                units = uiState.units,
                activeTool = uiState.activeTool,
                isSnapOn = uiState.isSnapEnabled,
                isOrthoOn = uiState.isOrthoEnabled,
                selectedEntityId = uiState.selectedEntityId,
                onOpenProperties = { viewModel.togglePropertiesSheet(true) },
                onOpenOsnap = viewModel::openOsnapSheet
            )

            // 4. Main 10-Item CAD Bottom Toolbar with Pinned Favorites
            CadBottomToolbar(
                toolManager = toolManager,
                commandManager = commandManager
            )
        }

        // CAD Object Snap (OSNAP) Settings Sheet
        if (uiState.showOsnapSheet) {
            CadOsnapSettingsSheet(
                settings = uiState.osnapSettings,
                onSettingsChanged = viewModel::setOsnapSettings,
                onToggleMode = viewModel::toggleOsnapMode,
                onSetAllModes = viewModel::setAllOsnapModes,
                onResetDefaults = viewModel::resetOsnapDefaults,
                onDismiss = viewModel::closeOsnapSheet
            )
        }

        // Layer Management Bottom Sheet
        if (uiState.showLayersSheet && uiState.document != null) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleLayersSheet(false) },
                sheetState = sheetState,
                containerColor = CadSurfaceDark
            ) {
                CadLayerManagementSheet(
                    layers = uiState.document!!.layers.values.toList(),
                    currentLayerId = uiState.currentLayerId,
                    searchQuery = uiState.layerSearchQuery,
                    onSearchQueryChange = viewModel::setLayerSearchQuery,
                    onSelectActiveLayer = viewModel::setCurrentLayer,
                    onToggleVisibility = viewModel::toggleLayerVisibility,
                    onToggleLock = viewModel::toggleLayerLock,
                    onShowAll = viewModel::showAllLayers,
                    onHideAll = viewModel::hideAllLayers,
                    onClose = { viewModel.toggleLayersSheet(false) }
                )
            }
        }

        // Mobile Properties Bottom Sheet (When an entity is selected on phone)
        if (!isTablet && uiState.showPropertiesSheet && uiState.selectedEntityId != null && uiState.document != null) {
            val selectedEntity = uiState.document!!.entities.find { it.id == uiState.selectedEntityId }
            if (selectedEntity != null) {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    onDismissRequest = { viewModel.togglePropertiesSheet(false) },
                    sheetState = sheetState,
                    containerColor = CadSurfaceDark
                ) {
                    CadPropertiesPanel(
                        entity = selectedEntity,
                        layers = uiState.document!!.layers.values.toList(),
                        onDelete = viewModel::deleteSelectedEntity,
                        onUpdateEntity = viewModel::updateSelectedEntity,
                        onUpdateText = viewModel::updateSelectedEntityText,
                        onUpdateLayer = viewModel::updateSelectedEntityLayer,
                        onClose = { viewModel.togglePropertiesSheet(false) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()
                    )
                }
            }
        }

        // CAD Move Displacement Dialog
        if (showMoveDialog && uiState.selectionState.hasSelection) {
            CadMoveDialog(
                count = uiState.selectionState.count,
                onApply = { dx, dy ->
                    viewModel.moveSelectedEntities(dx, dy)
                    showMoveDialog = false
                },
                onDismiss = { showMoveDialog = false }
            )
        }

        // CAD Measurement Bottom Sheet for selecting any of the 14 tools, multi-area & units
        if (uiState.showMeasurementSheet) {
            CadMeasurementSheet(
                activeType = uiState.activeMeasurementType ?: uiState.activeTool.toMeasurementType(),
                activeResult = uiState.activeMeasurementResult,
                currentUnit = uiState.measurementUnit,
                multiAreaRegions = uiState.multiAreaRegions,
                isSubtractMode = uiState.isSubtractMode,
                measurementHistory = uiState.measurementHistory,
                selectedCount = uiState.selectionState.count,
                onSelectTool = { type -> viewModel.selectMeasurementTool(type) },
                onChangeUnit = { unit -> viewModel.setMeasurementUnit(unit) },
                onCopyResult = { viewModel.copyMeasurementResult(context) },
                onClearResult = { viewModel.clearMeasurement() },
                onAddRegion = { isSub -> viewModel.addCurrentPointsAsRegion(isSub) },
                onToggleRegionSubtract = { id -> viewModel.toggleRegionSubtract(id) },
                onRemoveRegion = { id -> viewModel.removeRegion(id) },
                onClearRegions = { viewModel.clearMultiAreaRegions() },
                onMeasureSelection = { viewModel.measureSelectedEntities() },
                onRecallHistoryItem = { item -> viewModel.recallMeasurementHistoryItem(item) },
                onDeleteHistoryItem = { id -> viewModel.deleteMeasurementHistoryItem(id) },
                onClearHistory = { viewModel.clearMeasurementHistory() },
                onDismiss = { viewModel.toggleMeasurementSheet(false) }
            )
        }

        // CAD Export & Print Bottom Sheet (PDF, PNG/JPEG, Takeoff Reports, Print)
        if (uiState.showExportSheet && uiState.document != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleExportSheet(false) },
                sheetState = sheetState,
                containerColor = CadSurfaceDark
            ) {
                CadExportBottomSheet(
                    document = uiState.document!!,
                    measurementHistory = uiState.measurementHistory,
                    activeMeasurement = uiState.activeMeasurementResult,
                    onDismiss = { viewModel.toggleExportSheet(false) }
                )
            }
        }
    }
}

@Composable
fun CadEditorTopBar(
    title: String,
    format: com.example.cad.model.CadFormat,
    canUndo: Boolean,
    canRedo: Boolean,
    hasUnsavedChanges: Boolean = false,
    isAutoSaving: Boolean = false,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onSaveFormat: (com.example.cad.model.CadFormat) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onLayersClick: () -> Unit,
    onMeasureClick: () -> Unit,
    onExportClick: () -> Unit = {},
    onZoomFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CadSurfaceDark)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("editor_back_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        IconButton(
            onClick = onOpen,
            modifier = Modifier.testTag("editor_open_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = "Open Drawing",
                tint = Color(0xFFCBD5E1)
            )
        }

        Box {
            IconButton(
                onClick = { showSaveMenu = true },
                modifier = Modifier.testTag("editor_save_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save Drawing Options",
                    tint = if (hasUnsavedChanges) Color(0xFFFFB74D) else CadCyan
                )
            }

            DropdownMenu(
                expanded = showSaveMenu,
                onDismissRequest = { showSaveMenu = false },
                modifier = Modifier.background(CadSurfaceDark)
            ) {
                DropdownMenuItem(
                    text = { Text("Quick Save") },
                    leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null, tint = CadCyan) },
                    onClick = {
                        showSaveMenu = false
                        onSave()
                    },
                    modifier = Modifier.testTag("save_quick_btn")
                )
                DropdownMenuItem(
                    text = { Text("Save as DXF (.dxf)") },
                    leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, tint = Color(0xFF81C784)) },
                    onClick = {
                        showSaveMenu = false
                        onSaveFormat(com.example.cad.model.CadFormat.DXF)
                    },
                    modifier = Modifier.testTag("save_dxf_btn")
                )
                DropdownMenuItem(
                    text = { Text("Save as CAD Project (.cadproj)") },
                    leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = Color(0xFFFFB74D)) },
                    onClick = {
                        showSaveMenu = false
                        onSaveFormat(com.example.cad.model.CadFormat.CADPROJ)
                    },
                    modifier = Modifier.testTag("save_cadproj_btn")
                )
                DropdownMenuItem(
                    text = { Text("Export & Print (PDF, Image, Report)...") },
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = CadCyan) },
                    onClick = {
                        showSaveMenu = false
                        onExportClick()
                    },
                    modifier = Modifier.testTag("menu_export_print_btn")
                )
                DropdownMenuItem(
                    text = { Text("DWG Export (Commercial ODA SDK Required)") },
                    leadingIcon = { Icon(Icons.Filled.Architecture, contentDescription = null, tint = Color(0xFF64B5F6)) },
                    onClick = {
                        showSaveMenu = false
                        onSaveFormat(com.example.cad.model.CadFormat.DWG)
                    },
                    modifier = Modifier.testTag("export_dwg_btn")
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CadFormatChip(format = format)
                Text(
                    text = when {
                        isAutoSaving -> "Auto-saving..."
                        hasUnsavedChanges -> "Unsaved edits"
                        else -> "Saved"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = when {
                        isAutoSaving -> Color(0xFFFFB74D)
                        hasUnsavedChanges -> Color(0xFFFFB74D)
                        else -> Color(0xFF81C784)
                    }
                )
            }
        }

        // Action icons
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.testTag("editor_undo_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) Color.White else Color(0xFF555555)
            )
        }

        IconButton(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier.testTag("editor_redo_btn")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                tint = if (canRedo) Color.White else Color(0xFF555555)
            )
        }

        IconButton(
            onClick = onZoomFit,
            modifier = Modifier.testTag("editor_zoom_fit_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = "Zoom Extents",
                tint = CadCyan
            )
        }

        IconButton(
            onClick = onMeasureClick,
            modifier = Modifier.testTag("editor_measure_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.Straighten,
                contentDescription = "Measure",
                tint = CadDimensionYellow
            )
        }

        IconButton(
            onClick = onLayersClick,
            modifier = Modifier.testTag("editor_layers_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.Layers,
                contentDescription = "Layers",
                tint = CadCyan
            )
        }

        IconButton(
            onClick = onExportClick,
            modifier = Modifier.testTag("editor_export_btn")
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Export & Print Drawing",
                tint = CadCyan
            )
        }
    }
}

@Composable
fun CadCoordinateHud(
    cursorPos: com.example.cad.model.CadPoint2D,
    scale: Float,
    units: com.example.cad.model.CadUnit,
    activeTool: CadTool,
    currentLayerId: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC121620))
            .border(1.dp, CadBorderDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TOOL: ${activeTool.displayName.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = CadCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LYR: $currentLayerId",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = CadSnapGreen
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "X: %.1f %s   Y: %.1f %s".format(cursorPos.x, units.abbreviation, cursorPos.y, units.abbreviation),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = Color.White
            )
            Text(
                text = "ZOOM: %.0f%%".format(scale * 1000f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = Color(0xFFAAAAAA)
            )
        }
    }
}

@Composable
fun DrawingToolControlsBanner(
    toolName: String,
    pointCount: Int,
    showPolygonClose: Boolean = false,
    onFinish: () -> Unit,
    onClosePolygon: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("drawing_tool_controls_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE161B26)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadCyan)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val labelText = if (pointCount > 0) "$toolName ($pointCount pts)" else toolName
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan),
                modifier = Modifier.height(32.dp).testTag("btn_finish_drawing_tool")
            ) {
                Text("Confirm", color = Color.Black, fontSize = 11.sp)
            }
            if (showPolygonClose) {
                Button(
                    onClick = onClosePolygon,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.height(32.dp).testTag("btn_close_polygon")
                ) {
                    Text("Close Loop", color = Color.White, fontSize = 11.sp)
                }
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(32.dp).testTag("btn_cancel_drawing_tool")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel Operation",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CadSelectionToolbarBanner(
    selectionState: com.example.cad.engine.selection.SelectionState,
    document: com.example.cad.model.CadDocument?,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onOpenMoveDialog: () -> Unit,
    onOpenProperties: () -> Unit,
    onStartMove: () -> Unit = onOpenMoveDialog,
    onStartCopy: () -> Unit = onCopy,
    onStartRotate: () -> Unit = {},
    onStartScale: () -> Unit = {},
    onStartTrim: () -> Unit = {},
    onStartExtend: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("cad_selection_toolbar_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF0121620)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadCyan.copy(alpha = 0.6f))
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status Tag / Badge
            if (selectionState.hasSelection) {
                val primaryEntity = selectionState.primarySelectedId?.let { id ->
                    document?.entities?.find { it.id == id }
                } ?: selectionState.selectedIds.firstOrNull()?.let { id ->
                    document?.entities?.find { it.id == id }
                }
                val layerName = primaryEntity?.let {
                    document?.layers?.get(it.layerId)?.name ?: it.layerId
                }

                Column {
                    Text(
                        text = "${selectionState.count} SELECTED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = CadCyan
                    )
                    if (layerName != null) {
                        Text(
                            text = "LAYER: $layerName",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = CadSnapGreen
                        )
                    }
                }
            } else {
                Text(
                    text = "SELECT MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFFAAAAAA)
                )
            }

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(CadBorderDark)
            )

            // Select All
            IconButton(
                onClick = onSelectAll,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("selection_select_all_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = "Select All",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Deselect / Clear
            IconButton(
                onClick = onClearSelection,
                enabled = selectionState.hasSelection,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("selection_clear_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "Deselect",
                    tint = if (selectionState.hasSelection) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Multi-Select Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (selectionState.isMultiSelectEnabled) CadCyan.copy(alpha = 0.25f)
                        else Color.Transparent
                    )
                    .clickable { onToggleMultiSelect() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("selection_multi_toggle_btn")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Checklist,
                        contentDescription = "Multi-Select",
                        tint = if (selectionState.isMultiSelectEnabled) CadCyan else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MULTI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = if (selectionState.isMultiSelectEnabled) CadCyan else Color.Gray
                    )
                }
            }

            if (selectionState.hasSelection) {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(CadBorderDark)
                )

                // Move Button
                IconButton(
                    onClick = onStartMove,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_move_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenWith,
                        contentDescription = "Move Selected",
                        tint = CadCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy Button
                IconButton(
                    onClick = onStartCopy,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_copy_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy Selected",
                        tint = CadDimensionYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Rotate Button
                IconButton(
                    onClick = onStartRotate,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_rotate_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RotateRight,
                        contentDescription = "Rotate Selected",
                        tint = CadCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Scale Button
                IconButton(
                    onClick = onStartScale,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_scale_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomOutMap,
                        contentDescription = "Scale Selected",
                        tint = CadCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Trim Button
                IconButton(
                    onClick = onStartTrim,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_trim_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = "Trim Selected",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Extend Button
                IconButton(
                    onClick = onStartExtend,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_extend_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.East,
                        contentDescription = "Extend Selected",
                        tint = CadCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_delete_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Selected",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Properties Button
                IconButton(
                    onClick = onOpenProperties,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("selection_properties_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "View Properties",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CadEditCommandActiveBanner(
    activeCommand: EditCommandState.Active,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onNudgeDelta: (Float, Float) -> Unit,
    onSetAngle: (Float) -> Unit,
    onSetScale: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("cad_edit_command_active_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xF50D1117)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadCyan)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CadCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (activeCommand.operation) {
                            EditOperationType.MOVE -> Icons.Filled.OpenWith
                            EditOperationType.COPY -> Icons.Filled.ContentCopy
                            EditOperationType.ROTATE -> Icons.Filled.RotateRight
                            EditOperationType.SCALE -> Icons.Filled.ZoomOutMap
                            EditOperationType.TRIM -> Icons.Filled.ContentCut
                            EditOperationType.EXTEND -> Icons.Filled.East
                            EditOperationType.DELETE -> Icons.Filled.Delete
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = activeCommand.operation.displayName,
                            tint = CadCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "COMMAND: ${activeCommand.operation.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = CadCyan
                        )
                        Text(
                            text = activeCommand.statusPrompt,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Cancel Button
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF374151),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("edit_command_cancel_btn")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                    }

                    // Confirm Button
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CadCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("edit_command_confirm_btn")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Confirm", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                    }
                }
            }

            // Quick adjustment chips row for precision placement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (activeCommand.operation) {
                    EditOperationType.MOVE, EditOperationType.COPY -> {
                        CadQuickValueChip("+100 mm X") { onNudgeDelta(activeCommand.deltaX + 100f, activeCommand.deltaY) }
                        CadQuickValueChip("+100 mm Y") { onNudgeDelta(activeCommand.deltaX, activeCommand.deltaY + 100f) }
                        CadQuickValueChip("-100 mm X") { onNudgeDelta(activeCommand.deltaX - 100f, activeCommand.deltaY) }
                        CadQuickValueChip("-100 mm Y") { onNudgeDelta(activeCommand.deltaX, activeCommand.deltaY - 100f) }
                        CadQuickValueChip("+500 mm X") { onNudgeDelta(activeCommand.deltaX + 500f, activeCommand.deltaY) }
                    }
                    EditOperationType.ROTATE -> {
                        CadQuickValueChip("45°") { onSetAngle(45f) }
                        CadQuickValueChip("90°") { onSetAngle(90f) }
                        CadQuickValueChip("180°") { onSetAngle(180f) }
                        CadQuickValueChip("270°") { onSetAngle(270f) }
                        CadQuickValueChip("+15°") { onSetAngle((activeCommand.angleDeg + 15f) % 360f) }
                        CadQuickValueChip("-15°") { onSetAngle((activeCommand.angleDeg - 15f + 360f) % 360f) }
                    }
                    EditOperationType.SCALE -> {
                        CadQuickValueChip("0.5x") { onSetScale(0.5f) }
                        CadQuickValueChip("1.5x") { onSetScale(1.5f) }
                        CadQuickValueChip("2.0x") { onSetScale(2.0f) }
                        CadQuickValueChip("3.0x") { onSetScale(3.0f) }
                        CadQuickValueChip("0.1x") { onSetScale(0.1f) }
                    }
                    EditOperationType.TRIM -> {
                        Text(
                            text = "Tap on the line segment crossing other objects to trim it away",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.LightGray
                        )
                    }
                    EditOperationType.EXTEND -> {
                        Text(
                            text = "Tap near the endpoint you want to extend to the nearest boundary edge",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.LightGray
                        )
                    }
                    EditOperationType.DELETE -> {}
                }
            }
        }
    }
}

@Composable
fun CadQuickValueChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = CadCyan
        )
    }
}

@Composable
fun CadMoveDialog(
    count: Int,
    onApply: (deltaX: Float, deltaY: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var deltaX by remember { mutableStateOf("100") }
    var deltaY by remember { mutableStateOf("0") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CadSurfaceDark,
        title = {
            Text(
                text = "Move $count Selected Object(s)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Specify displacement in drawing units (mm):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
                androidx.compose.material3.OutlinedTextField(
                    value = deltaX,
                    onValueChange = { deltaX = it },
                    label = { Text("Delta X (mm)") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CadCyan,
                        unfocusedBorderColor = CadBorderDark
                    )
                )
                androidx.compose.material3.OutlinedTextField(
                    value = deltaY,
                    onValueChange = { deltaY = it },
                    label = { Text("Delta Y (mm)") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CadCyan,
                        unfocusedBorderColor = CadBorderDark
                    )
                )
                // Quick Nudge Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onApply(-100f, 0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariantDark),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("← -100", fontSize = 10.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onApply(100f, 0f) },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariantDark),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+100 →", fontSize = 10.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onApply(0f, 100f) },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariantDark),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("↑ +100", fontSize = 10.sp, color = Color.White)
                    }
                    Button(
                        onClick = { onApply(0f, -100f) },
                        colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariantDark),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("↓ -100", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dx = deltaX.toFloatOrNull() ?: 0f
                    val dy = deltaY.toFloatOrNull() ?: 0f
                    onApply(dx, dy)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CadCyan)
            ) {
                Text("Apply Move", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun CadMeasurementBanner(
    result: MeasurementResult,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .testTag("cad_measurement_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE1E2433)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadDimensionYellow)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Straighten,
                contentDescription = "Measurement",
                tint = CadDimensionYellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            when (result) {
                is MeasurementResult.Distance -> {
                    Column {
                        Text(
                            text = "DISTANCE: ${result.formatted}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CadDimensionYellow
                        )
                        Text(
                            text = "ΔX: %.2f %s   ΔY: %.2f %s".format(
                                result.deltaX, result.unit.abbreviation,
                                result.deltaY, result.unit.abbreviation
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
                is MeasurementResult.Angle -> {
                    Text(
                        text = "ANGLE: ${result.formatted}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = CadDimensionYellow
                    )
                }
                is MeasurementResult.Area -> {
                    Column {
                        Text(
                            text = "AREA: ${result.formattedArea}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CadDimensionYellow
                        )
                        Text(
                            text = "PERIMETER: ${result.formattedPerimeter}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CadFloatingZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomFit: () -> Unit,
    onPan: () -> Unit,
    isPanActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .testTag("cad_floating_zoom_controls"),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE161B26)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onPan,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("floating_pan_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.PanTool,
                    contentDescription = "Pan Viewport",
                    tint = if (isPanActive) CadCyan else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("floating_zoom_in_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Zoom In",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("floating_zoom_out_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Zoom Out",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onZoomFit,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("floating_zoom_fit_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.ZoomOutMap,
                    contentDescription = "Zoom Extents",
                    tint = CadCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CadBottomToolBar(
    activeTool: CadTool,
    isGridOn: Boolean,
    isSnapOn: Boolean,
    isOrthoOn: Boolean,
    isCrosshairOn: Boolean,
    onSelectTool: (CadTool) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleSnap: () -> Unit,
    onOpenOsnap: () -> Unit = {},
    onOpenMeasure: () -> Unit = {},
    onToggleOrtho: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .testTag("cad_bottom_toolbar"),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE161B26)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Select Tool
            CadToolIconBtn(
                icon = Icons.Filled.NearMe,
                label = "Select",
                isSelected = activeTool == CadTool.SELECT,
                onClick = { onSelectTool(CadTool.SELECT) }
            )
            // Pan Tool
            CadToolIconBtn(
                icon = Icons.Filled.PanTool,
                label = "Pan",
                isSelected = activeTool == CadTool.PAN,
                onClick = { onSelectTool(CadTool.PAN) }
            )
            // Line Tool
            CadToolIconBtn(
                icon = Icons.Filled.ShowChart,
                label = "Line",
                isSelected = activeTool == CadTool.LINE,
                onClick = { onSelectTool(CadTool.LINE) }
            )
            // Polyline Tool
            CadToolIconBtn(
                icon = Icons.Filled.Timeline,
                label = "Polyline",
                isSelected = activeTool == CadTool.POLYLINE,
                onClick = { onSelectTool(CadTool.POLYLINE) }
            )
            // Circle Tool
            CadToolIconBtn(
                icon = Icons.Filled.Circle,
                label = "Circle",
                isSelected = activeTool == CadTool.CIRCLE,
                onClick = { onSelectTool(CadTool.CIRCLE) }
            )
            // Arc Tool
            CadToolIconBtn(
                icon = Icons.Filled.LinearScale,
                label = "Arc",
                isSelected = activeTool == CadTool.ARC,
                onClick = { onSelectTool(CadTool.ARC) }
            )
            // Rectangle Tool
            CadToolIconBtn(
                icon = Icons.Filled.CropSquare,
                label = "Rectangle",
                isSelected = activeTool == CadTool.RECTANGLE,
                onClick = { onSelectTool(CadTool.RECTANGLE) }
            )
            // Point Tool
            CadToolIconBtn(
                icon = Icons.Filled.CenterFocusStrong,
                label = "Point",
                isSelected = activeTool == CadTool.POINT,
                onClick = { onSelectTool(CadTool.POINT) }
            )
            // Single-line Text Tool
            CadToolIconBtn(
                icon = Icons.Filled.TextFields,
                label = "Text",
                isSelected = activeTool == CadTool.TEXT,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.TEXT) }
            )
            // Multi-line Text Tool
            CadToolIconBtn(
                icon = Icons.Filled.Notes,
                label = "MText",
                isSelected = activeTool == CadTool.MTEXT,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.MTEXT) }
            )
            // Leader Note Tool
            CadToolIconBtn(
                icon = Icons.Filled.CallMade,
                label = "Leader",
                isSelected = activeTool == CadTool.LEADER,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.LEADER) }
            )
            // Arrow Annotation Tool
            CadToolIconBtn(
                icon = Icons.Filled.East,
                label = "Arrow",
                isSelected = activeTool == CadTool.ARROW,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.ARROW) }
            )
            // Revision Cloud Tool
            CadToolIconBtn(
                icon = Icons.Filled.Cloud,
                label = "Cloud",
                isSelected = activeTool == CadTool.CLOUD,
                activeColor = Color(0xFFFF7043),
                onClick = { onSelectTool(CadTool.CLOUD) }
            )
            // Eraser Tool
            CadToolIconBtn(
                icon = Icons.Filled.Delete,
                label = "Eraser",
                isSelected = activeTool == CadTool.ERASER,
                activeColor = Color(0xFFEF4444),
                onClick = { onSelectTool(CadTool.ERASER) }
            )
            // Measure Tool
            CadToolIconBtn(
                icon = Icons.Filled.Straighten,
                label = "Measure",
                isSelected = activeTool.isMeasurementTool,
                activeColor = CadDimensionYellow,
                onClick = onOpenMeasure
            )
            // Move Tool
            CadToolIconBtn(
                icon = Icons.Filled.OpenWith,
                label = "Move",
                isSelected = activeTool == CadTool.MOVE,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.MOVE) }
            )
            // Copy Tool
            CadToolIconBtn(
                icon = Icons.Filled.ContentCopy,
                label = "Copy",
                isSelected = activeTool == CadTool.COPY,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.COPY) }
            )
            // Rotate Tool
            CadToolIconBtn(
                icon = Icons.Filled.RotateRight,
                label = "Rotate",
                isSelected = activeTool == CadTool.ROTATE,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.ROTATE) }
            )
            // Scale Tool
            CadToolIconBtn(
                icon = Icons.Filled.ZoomOutMap,
                label = "Scale",
                isSelected = activeTool == CadTool.SCALE,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.SCALE) }
            )
            // Trim Tool
            CadToolIconBtn(
                icon = Icons.Filled.ContentCut,
                label = "Trim",
                isSelected = activeTool == CadTool.TRIM,
                activeColor = Color(0xFFEF4444),
                onClick = { onSelectTool(CadTool.TRIM) }
            )
            // Extend Tool
            CadToolIconBtn(
                icon = Icons.Filled.East,
                label = "Extend",
                isSelected = activeTool == CadTool.EXTEND,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.EXTEND) }
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(CadBorderDark)
            )

            // Grid toggle
            CadToolIconBtn(
                icon = Icons.Filled.GridOn,
                label = "Grid",
                isSelected = isGridOn,
                activeColor = CadSnapGreen,
                onClick = onToggleGrid
            )
            // Snap toggle
            CadToolIconBtn(
                icon = Icons.Filled.SquareFoot,
                label = "Snap",
                isSelected = isSnapOn,
                activeColor = CadSnapGreen,
                onClick = onToggleSnap
            )
            // OSNAP Modes Settings
            CadToolIconBtn(
                icon = Icons.Filled.Tune,
                label = "OSNAP",
                isSelected = isSnapOn,
                activeColor = CadSnapGreen,
                onClick = onOpenOsnap
            )
            // Ortho toggle
            CadToolIconBtn(
                icon = Icons.Filled.AutoFixHigh,
                label = "Ortho",
                isSelected = isOrthoOn,
                activeColor = CadCyan,
                onClick = onToggleOrtho
            )
            // Crosshair toggle
            CadToolIconBtn(
                icon = Icons.Filled.CenterFocusStrong,
                label = "Crosshair",
                isSelected = isCrosshairOn,
                activeColor = CadCyan,
                onClick = onToggleCrosshair
            )

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(CadBorderDark)
            )

            // Zoom In
            CadToolIconBtn(
                icon = Icons.Filled.Add,
                label = "Zoom +",
                isSelected = false,
                onClick = onZoomIn
            )
            // Zoom Out
            CadToolIconBtn(
                icon = Icons.Filled.Remove,
                label = "Zoom -",
                isSelected = false,
                onClick = onZoomOut
            )
            // Zoom Fit
            CadToolIconBtn(
                icon = Icons.Filled.CenterFocusStrong,
                label = "Fit",
                isSelected = false,
                activeColor = CadDimensionYellow,
                onClick = onZoomFit
            )
        }
    }
}

@Composable
fun CadVerticalToolRail(
    activeTool: CadTool,
    isGridOn: Boolean,
    isSnapOn: Boolean,
    isOrthoOn: Boolean,
    isCrosshairOn: Boolean,
    onSelectTool: (CadTool) -> Unit,
    onToggleGrid: () -> Unit,
    onToggleSnap: () -> Unit,
    onOpenOsnap: () -> Unit = {},
    onOpenMeasure: () -> Unit = {},
    onToggleOrtho: () -> Unit,
    onToggleCrosshair: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onZoomFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .testTag("cad_vertical_tool_rail"),
        colors = CardDefaults.cardColors(containerColor = Color(0xEE161B26)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CadToolIconBtn(
                icon = Icons.Filled.NearMe,
                label = "Select",
                isSelected = activeTool == CadTool.SELECT,
                onClick = { onSelectTool(CadTool.SELECT) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.PanTool,
                label = "Pan",
                isSelected = activeTool == CadTool.PAN,
                onClick = { onSelectTool(CadTool.PAN) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.ShowChart,
                label = "Line",
                isSelected = activeTool == CadTool.LINE,
                onClick = { onSelectTool(CadTool.LINE) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.Timeline,
                label = "Polyline",
                isSelected = activeTool == CadTool.POLYLINE,
                onClick = { onSelectTool(CadTool.POLYLINE) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.Circle,
                label = "Circle",
                isSelected = activeTool == CadTool.CIRCLE,
                onClick = { onSelectTool(CadTool.CIRCLE) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.LinearScale,
                label = "Arc",
                isSelected = activeTool == CadTool.ARC,
                onClick = { onSelectTool(CadTool.ARC) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.CropSquare,
                label = "Rectangle",
                isSelected = activeTool == CadTool.RECTANGLE,
                onClick = { onSelectTool(CadTool.RECTANGLE) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.CenterFocusStrong,
                label = "Point",
                isSelected = activeTool == CadTool.POINT,
                onClick = { onSelectTool(CadTool.POINT) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.TextFields,
                label = "Text",
                isSelected = activeTool == CadTool.TEXT,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.TEXT) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.Notes,
                label = "MText",
                isSelected = activeTool == CadTool.MTEXT,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.MTEXT) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.CallMade,
                label = "Leader",
                isSelected = activeTool == CadTool.LEADER,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.LEADER) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.East,
                label = "Arrow",
                isSelected = activeTool == CadTool.ARROW,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.ARROW) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.Cloud,
                label = "Cloud",
                isSelected = activeTool == CadTool.CLOUD,
                activeColor = Color(0xFFFF7043),
                onClick = { onSelectTool(CadTool.CLOUD) }
            )
            CadToolIconBtn(
                icon = Icons.Filled.Delete,
                label = "Eraser",
                isSelected = activeTool == CadTool.ERASER,
                activeColor = Color(0xFFEF4444),
                onClick = { onSelectTool(CadTool.ERASER) }
            )
            // Measure Tool
            CadToolIconBtn(
                icon = Icons.Filled.Straighten,
                label = "Measure",
                isSelected = activeTool.isMeasurementTool,
                activeColor = CadDimensionYellow,
                onClick = onOpenMeasure
            )
            // Move Tool
            CadToolIconBtn(
                icon = Icons.Filled.OpenWith,
                label = "Move",
                isSelected = activeTool == CadTool.MOVE,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.MOVE) }
            )
            // Copy Tool
            CadToolIconBtn(
                icon = Icons.Filled.ContentCopy,
                label = "Copy",
                isSelected = activeTool == CadTool.COPY,
                activeColor = CadDimensionYellow,
                onClick = { onSelectTool(CadTool.COPY) }
            )
            // Rotate Tool
            CadToolIconBtn(
                icon = Icons.Filled.RotateRight,
                label = "Rotate",
                isSelected = activeTool == CadTool.ROTATE,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.ROTATE) }
            )
            // Scale Tool
            CadToolIconBtn(
                icon = Icons.Filled.ZoomOutMap,
                label = "Scale",
                isSelected = activeTool == CadTool.SCALE,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.SCALE) }
            )
            // Trim Tool
            CadToolIconBtn(
                icon = Icons.Filled.ContentCut,
                label = "Trim",
                isSelected = activeTool == CadTool.TRIM,
                activeColor = Color(0xFFEF4444),
                onClick = { onSelectTool(CadTool.TRIM) }
            )
            // Extend Tool
            CadToolIconBtn(
                icon = Icons.Filled.East,
                label = "Extend",
                isSelected = activeTool == CadTool.EXTEND,
                activeColor = CadCyan,
                onClick = { onSelectTool(CadTool.EXTEND) }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CadBorderDark)
            )

            CadToolIconBtn(
                icon = Icons.Filled.GridOn,
                label = "Grid",
                isSelected = isGridOn,
                activeColor = CadSnapGreen,
                onClick = onToggleGrid
            )
            CadToolIconBtn(
                icon = Icons.Filled.SquareFoot,
                label = "Snap",
                isSelected = isSnapOn,
                activeColor = CadSnapGreen,
                onClick = onToggleSnap
            )
            CadToolIconBtn(
                icon = Icons.Filled.Tune,
                label = "OSNAP",
                isSelected = isSnapOn,
                activeColor = CadSnapGreen,
                onClick = onOpenOsnap
            )
            CadToolIconBtn(
                icon = Icons.Filled.AutoFixHigh,
                label = "Ortho",
                isSelected = isOrthoOn,
                activeColor = CadCyan,
                onClick = onToggleOrtho
            )
            CadToolIconBtn(
                icon = Icons.Filled.CenterFocusStrong,
                label = "Crosshair",
                isSelected = isCrosshairOn,
                activeColor = CadCyan,
                onClick = onToggleCrosshair
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CadBorderDark)
            )

            CadToolIconBtn(
                icon = Icons.Filled.Add,
                label = "Zoom +",
                isSelected = false,
                onClick = onZoomIn
            )
            CadToolIconBtn(
                icon = Icons.Filled.Remove,
                label = "Zoom -",
                isSelected = false,
                onClick = onZoomOut
            )
            CadToolIconBtn(
                icon = Icons.Filled.CenterFocusStrong,
                label = "Fit",
                isSelected = false,
                activeColor = CadDimensionYellow,
                onClick = onZoomFit
            )
        }
    }
}

@Composable
fun CadToolIconBtn(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    activeColor: Color = CadCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) activeColor else Color.Transparent,
                CircleShape
            )
            .testTag("tool_btn_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CadStatusBar(
    cursorPos: com.example.cad.model.CadPoint2D,
    zoomScale: Float,
    currentLayerId: String,
    units: com.example.cad.model.CadUnit,
    activeTool: CadTool,
    isSnapOn: Boolean,
    isOrthoOn: Boolean,
    selectedEntityId: String?,
    onOpenProperties: () -> Unit,
    onOpenOsnap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F131C))
            .border(1.dp, CadBorderDark)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Coordinates X, Y
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "X: %.1f".format(cursorPos.x),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CadCyan
            )
            Text(
                text = "Y: %.1f".format(cursorPos.y),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = CadCyan
            )
            Text(
                text = "ZOOM: %.0f%%".format(zoomScale * 1000f),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFFAAAAAA)
            )
        }

        // Right side: Current Layer, Modes, and Properties affordance
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "LAYER: $currentLayerId",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = CadSnapGreen
            )

            if (isSnapOn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CadSnapGreen.copy(alpha = 0.2f))
                        .clickable { onOpenOsnap() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "OSNAP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CadSnapGreen
                    )
                }
            }
            if (isOrthoOn) {
                Text(
                    text = "ORTHO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CadCyan
                )
            }

            if (selectedEntityId != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CadCyan.copy(alpha = 0.2f))
                        .clickable { onOpenProperties() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PROP [1]",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CadCyan
                    )
                }
            }
        }
    }
}



@Composable
fun CadLayerManagementSheet(
    layers: List<CadLayer>,
    currentLayerId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectActiveLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLayers = remember(layers, searchQuery) {
        if (searchQuery.isBlank()) {
            layers
        } else {
            layers.filter {
                it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.id.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CAD Layer Manager",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${layers.size} layer(s) · Tap to set active layer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search layers by name or ID...", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = CadCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CadCyan,
                unfocusedBorderColor = CadBorderDark,
                focusedContainerColor = CadSurfaceVariantDark,
                unfocusedContainerColor = CadSurfaceVariantDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("layer_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bulk Controls Bar: Show All & Hide All Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onShowAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CadSurfaceVariantDark,
                    contentColor = CadCyan
                ),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CadCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("btn_show_all_layers")
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = "Show All Layers",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Show All", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }

            Button(
                onClick = onHideAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CadSurfaceVariantDark,
                    contentColor = Color(0xFFEF4444)
                ),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .testTag("btn_hide_all_layers")
            ) {
                Icon(
                    imageVector = Icons.Filled.VisibilityOff,
                    contentDescription = "Hide All Layers",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hide All", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Layer List
        if (filteredLayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No layers match '$searchQuery'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredLayers, key = { it.id }) { layer ->
                    val isActive = layer.id == currentLayerId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) CadCyan.copy(alpha = 0.15f) else CadSurfaceVariantDark)
                            .border(1.dp, if (isActive) CadCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onSelectActiveLayer(layer.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("layer_row_${layer.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Color dot + Layer details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(layer.colorArgb))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = layer.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (layer.isVisible) MaterialTheme.colorScheme.onSurface else Color.Gray
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = CadCyan
                                        )
                                    }
                                }
                                Text(
                                    text = "${layer.lineType} · ${layer.lineWeight} mm" + if (layer.isLocked) " · LOCKED" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (layer.isLocked) Color(0xFFFFB74D) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Action Controls: Lock Toggle & Visibility Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Lock toggle button
                            IconButton(
                                onClick = { onToggleLock(layer.id) },
                                modifier = Modifier.testTag("layer_lock_${layer.id}")
                            ) {
                                Icon(
                                    imageVector = if (layer.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                    contentDescription = if (layer.isLocked) "Unlock Layer" else "Lock Layer",
                                    tint = if (layer.isLocked) Color(0xFFFFB74D) else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Visibility toggle button
                            IconButton(
                                onClick = { onToggleVisibility(layer.id) },
                                modifier = Modifier.testTag("layer_toggle_${layer.id}")
                            ) {
                                Icon(
                                    imageVector = if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (layer.isVisible) "Hide Layer" else "Show Layer",
                                    tint = if (layer.isVisible) CadCyan else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

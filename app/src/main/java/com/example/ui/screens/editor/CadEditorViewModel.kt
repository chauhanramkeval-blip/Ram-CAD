package com.example.ui.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cad.engine.CadEngine
import com.example.cad.engine.MeasurementResult
import com.example.cad.engine.selection.HitTestEngine
import com.example.cad.engine.selection.SelectionBox
import com.example.cad.engine.selection.SelectionGrip
import com.example.cad.engine.selection.SelectionManager
import com.example.cad.engine.selection.SelectionState
import com.example.cad.engine.tools.ArcTool
import com.example.cad.engine.tools.ArrowTool
import com.example.cad.engine.tools.CadToolContext
import com.example.cad.engine.tools.CircleTool
import com.example.cad.engine.tools.DrawingTool
import com.example.cad.engine.tools.LeaderTool
import com.example.cad.engine.tools.LineTool
import com.example.cad.engine.tools.MultiLineTextTool
import com.example.cad.engine.tools.PointTool
import com.example.cad.engine.tools.PolylineTool
import com.example.cad.engine.tools.RectangleTool
import com.example.cad.engine.tools.RevisionCloudTool
import com.example.cad.engine.tools.SingleLineTextTool
import com.example.cad.engine.tools.ToolContext
import com.example.cad.engine.tools.ToolResult
import com.example.cad.engine.edit.CadEditMath
import com.example.cad.engine.edit.EditCommandState
import com.example.cad.engine.edit.EditOperationType
import com.example.cad.engine.snap.CadOsnapSettings
import com.example.cad.engine.snap.CadSnapEngine
import com.example.cad.engine.snap.CadSnapMode
import com.example.cad.engine.snap.CadSnapResult
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.cad.model.CadViewportTransform
import com.example.cad.repository.DrawingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.cad.engine.measurement.CadMeasurementEngine
import com.example.cad.engine.measurement.CadMeasurementResult
import com.example.cad.engine.measurement.CadMeasurementType
import com.example.cad.engine.measurement.StandardCadMeasurementEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

enum class CadTool(val displayName: String) {
    SELECT("Select"),
    PAN("Pan Viewport"),
    LINE("Line"),
    POLYLINE("Polyline"),
    CIRCLE("Circle"),
    ARC("Arc"),
    RECTANGLE("Rectangle"),
    TEXT("Single-line Text"),
    MTEXT("Multi-line Text"),
    LEADER("Leader Note"),
    ARROW("Arrow"),
    CLOUD("Revision Cloud"),
    ERASER("Eraser/Delete"),
    POINT("Point"),
    MOVE("Move"),
    COPY("Copy"),
    ROTATE("Rotate"),
    SCALE("Scale"),
    TRIM("Trim"),
    EXTEND("Extend"),
    MEASURE_DISTANCE("Distance"),
    MEASURE_ALIGNED("Aligned Distance"),
    MEASURE_HORIZONTAL("Horizontal Distance"),
    MEASURE_VERTICAL("Vertical Distance"),
    MEASURE_ANGLE("Angle"),
    MEASURE_RADIUS("Radius"),
    MEASURE_DIAMETER("Diameter"),
    MEASURE_AREA("Area"),
    MEASURE_PERIMETER("Perimeter"),
    MEASURE_COORDINATE("Coordinate"),
    MEASURE_POLYLINE("Polyline Length"),
    MEASURE_BOUNDING_BOX("Bounding Box"),
    MEASURE_MULTI_AREA("Multi-Area"),
    MEASURE_SELECTION("Selection Summary")
}

val CadTool.isMeasurementTool: Boolean
    get() = when (this) {
        CadTool.MEASURE_DISTANCE,
        CadTool.MEASURE_ALIGNED,
        CadTool.MEASURE_HORIZONTAL,
        CadTool.MEASURE_VERTICAL,
        CadTool.MEASURE_ANGLE,
        CadTool.MEASURE_RADIUS,
        CadTool.MEASURE_DIAMETER,
        CadTool.MEASURE_AREA,
        CadTool.MEASURE_PERIMETER,
        CadTool.MEASURE_COORDINATE,
        CadTool.MEASURE_POLYLINE,
        CadTool.MEASURE_BOUNDING_BOX,
        CadTool.MEASURE_MULTI_AREA,
        CadTool.MEASURE_SELECTION -> true
        else -> false
    }

val CadTool.isAnnotationTool: Boolean
    get() = when (this) {
        CadTool.TEXT,
        CadTool.MTEXT,
        CadTool.LEADER,
        CadTool.ARROW,
        CadTool.CLOUD -> true
        else -> false
    }

fun CadTool.toMeasurementType(): CadMeasurementType? = when (this) {
    CadTool.MEASURE_DISTANCE -> CadMeasurementType.DISTANCE
    CadTool.MEASURE_ALIGNED -> CadMeasurementType.ALIGNED_DISTANCE
    CadTool.MEASURE_HORIZONTAL -> CadMeasurementType.HORIZONTAL_DISTANCE
    CadTool.MEASURE_VERTICAL -> CadMeasurementType.VERTICAL_DISTANCE
    CadTool.MEASURE_ANGLE -> CadMeasurementType.ANGLE
    CadTool.MEASURE_RADIUS -> CadMeasurementType.RADIUS
    CadTool.MEASURE_DIAMETER -> CadMeasurementType.DIAMETER
    CadTool.MEASURE_AREA -> CadMeasurementType.AREA
    CadTool.MEASURE_PERIMETER -> CadMeasurementType.PERIMETER
    CadTool.MEASURE_COORDINATE -> CadMeasurementType.COORDINATE
    CadTool.MEASURE_POLYLINE -> CadMeasurementType.POLYLINE_LENGTH
    CadTool.MEASURE_BOUNDING_BOX -> CadMeasurementType.BOUNDING_BOX
    CadTool.MEASURE_MULTI_AREA -> CadMeasurementType.MULTI_AREA
    CadTool.MEASURE_SELECTION -> CadMeasurementType.SELECTION_SUMMARY
    else -> null
}

fun CadMeasurementType.toCadTool(): CadTool = when (this) {
    CadMeasurementType.DISTANCE -> CadTool.MEASURE_DISTANCE
    CadMeasurementType.ALIGNED_DISTANCE -> CadTool.MEASURE_ALIGNED
    CadMeasurementType.HORIZONTAL_DISTANCE -> CadTool.MEASURE_HORIZONTAL
    CadMeasurementType.VERTICAL_DISTANCE -> CadTool.MEASURE_VERTICAL
    CadMeasurementType.ANGLE -> CadTool.MEASURE_ANGLE
    CadMeasurementType.RADIUS -> CadTool.MEASURE_RADIUS
    CadMeasurementType.DIAMETER -> CadTool.MEASURE_DIAMETER
    CadMeasurementType.AREA -> CadTool.MEASURE_AREA
    CadMeasurementType.PERIMETER -> CadTool.MEASURE_PERIMETER
    CadMeasurementType.COORDINATE -> CadTool.MEASURE_COORDINATE
    CadMeasurementType.POLYLINE_LENGTH -> CadTool.MEASURE_POLYLINE
    CadMeasurementType.BOUNDING_BOX -> CadTool.MEASURE_BOUNDING_BOX
    CadMeasurementType.MULTI_AREA -> CadTool.MEASURE_MULTI_AREA
    CadMeasurementType.SELECTION_SUMMARY -> CadTool.MEASURE_SELECTION
}

data class CadMeasurementHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: CadMeasurementType,
    val primaryValue: String,
    val area: String? = null,
    val perimeter: String? = null,
    val totalLength: String? = null,
    val objectCount: String? = null,
    val unit: CadUnit,
    val result: CadMeasurementResult
)

data class CadEditorUiState(
    val drawingId: String? = null,
    val drawingTitle: String = "Untitled_Drawing.dxf",
    val format: CadFormat = CadFormat.DXF,
    val units: CadUnit = CadUnit.MILLIMETERS,
    val document: CadDocument? = null,
    val viewportTransform: CadViewportTransform = CadViewportTransform(panX = 300f, panY = 500f, scale = 0.05f),
    val activeTool: CadTool = CadTool.PAN,
    val isGridVisible: Boolean = true,
    val isSnapEnabled: Boolean = true,
    val isOrthoEnabled: Boolean = false,
    val cursorWorldPos: CadPoint2D = CadPoint2D(0f, 0f),
    val selectionState: SelectionState = SelectionState(),
    val selectedEntityId: String? = null,
    val currentLayerId: String = "0",
    val measurementResult: MeasurementResult? = null,
    val activeMeasurementType: CadMeasurementType? = null,
    val activeMeasurementResult: CadMeasurementResult? = null,
    val measurementPoints: List<CadPoint2D> = emptyList(),
    val measurementUnit: CadUnit = CadUnit.MILLIMETERS,
    val multiAreaRegions: List<CadMeasurementResult.CadAreaRegion> = emptyList(),
    val isSubtractMode: Boolean = false,
    val measurementHistory: List<CadMeasurementHistoryItem> = emptyList(),
    val showMeasurementSheet: Boolean = false,
    val showLayersSheet: Boolean = false,
    val showPropertiesSheet: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isLoading: Boolean = false,
    val activePolylinePoints: List<CadPoint2D> = emptyList(),
    val isToolInProgress: Boolean = false,
    val cursorScreenPos: CadPoint2D = CadPoint2D(400f, 600f),
    val isCrosshairVisible: Boolean = true,
    val isFullScreenCrosshair: Boolean = true,
    val layerSearchQuery: String = "",
    val statusMessage: String? = null,
    val activeEditCommand: EditCommandState = EditCommandState.Idle,
    val osnapSettings: CadOsnapSettings = CadOsnapSettings(),
    val activeSnapResult: CadSnapResult? = null,
    val showOsnapSheet: Boolean = false,
    val showLineweights: Boolean = true,
    val canvasBackgroundColorArgb: Long = 0xFF0C0E14,
    val precisionDecimals: Int = 2,
    val hasUnsavedChanges: Boolean = false,
    val isAutoSaving: Boolean = false,
    val lastAutoSavedTimestamp: Long? = null,
    val hasCrashRecoveryAvailable: Boolean = false,
    val showCrashRecoveryPrompt: Boolean = false,
    val showSaveFormatMenu: Boolean = false,
    val showExportSheet: Boolean = false
)

class CadEditorViewModel(
    private val repository: DrawingRepository,
    val cadEngine: CadEngine
) : ViewModel() {

    val selectionManager: SelectionManager = SelectionManager()
    val snapEngine: CadSnapEngine = CadSnapEngine()
    val measurementEngine: CadMeasurementEngine = StandardCadMeasurementEngine()

    // Active interactive drawing tool instance
    var activeDrawingTool: DrawingTool? = null
        private set

    private val _uiState = MutableStateFlow(CadEditorUiState())
    val uiState: StateFlow<CadEditorUiState> = _uiState.asStateFlow()

    private var measurePoint1: CadPoint2D? = null
    private var arcPoint1: CadPoint2D? = null
    private var arcPoint2: CadPoint2D? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(20_000)
                if (_uiState.value.hasUnsavedChanges) {
                    triggerAutoSave()
                }
            }
        }
    }

    /**
     * Resolves the current base/anchor point for perpendicular object snapping and ortho constraints.
     */
    fun getActiveAnchorPoint(): CadPoint2D? {
        val tool = activeDrawingTool
        if (tool != null && tool.isInProgress) {
            return tool.activePoints.firstOrNull() ?: tool.activePoints.lastOrNull()
        }
        val activeCmd = _uiState.value.activeEditCommand as? EditCommandState.Active
        if (activeCmd != null) {
            return activeCmd.basePoint
        }
        if (measurePoint1 != null) {
            return measurePoint1
        }
        return null
    }

    fun loadDrawing(drawingId: String?) {
        if (drawingId == null) {
            val doc = cadEngine.createEmptyDocument("New_Draft.dxf")
            _uiState.value = _uiState.value.copy(
                drawingTitle = doc.title,
                format = doc.format,
                units = doc.units,
                document = doc,
                currentLayerId = doc.layers.keys.firstOrNull() ?: "0",
                hasUnsavedChanges = false,
                hasCrashRecoveryAvailable = false,
                showCrashRecoveryPrompt = false
            )
            zoomExtents(doc)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, drawingId = drawingId)
            val drawing = repository.getDrawingById(drawingId)
            val hasRecovery = repository.hasCrashRecovery(drawingId)
            if (drawing != null) {
                val result = cadEngine.loadDrawing(drawing)
                result.onSuccess { doc ->
                    _uiState.value = _uiState.value.copy(
                        drawingTitle = drawing.name,
                        format = drawing.format,
                        units = drawing.units,
                        document = doc,
                        isLoading = false,
                        currentLayerId = doc.layers.keys.firstOrNull() ?: "0",
                        canUndo = cadEngine.editor.canUndo(),
                        canRedo = cadEngine.editor.canRedo(),
                        hasUnsavedChanges = false,
                        hasCrashRecoveryAvailable = hasRecovery,
                        showCrashRecoveryPrompt = hasRecovery
                    )
                    zoomExtents(doc)
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Error loading drawing: ${error.message ?: "Unsupported or corrupted file"}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onViewportTransformed(panDeltaX: Float, panDeltaY: Float, zoomFactor: Float, centroidX: Float? = null, centroidY: Float? = null) {
        val current = _uiState.value.viewportTransform
        val newScale = (current.scale * zoomFactor).coerceIn(0.002f, 50f)

        val (newPanX, newPanY) = if (centroidX != null && centroidY != null && zoomFactor != 1.0f) {
            // Pivot zoom around centroid point (finger gesture anchor)
            val focusX = centroidX
            val focusY = centroidY
            val panX = focusX - (focusX - current.panX) * (newScale / current.scale) + panDeltaX
            val panY = focusY - (focusY - current.panY) * (newScale / current.scale) + panDeltaY
            Pair(panX, panY)
        } else {
            Pair(current.panX + panDeltaX, current.panY + panDeltaY)
        }

        val updatedTransform = CadViewportTransform(newPanX, newPanY, newScale)
        val currentScreen = _uiState.value.cursorScreenPos
        val updatedWorld = updatedTransform.screenToWorld(currentScreen.x, currentScreen.y)

        _uiState.value = _uiState.value.copy(
            viewportTransform = updatedTransform,
            cursorWorldPos = updatedWorld
        )
    }

    fun onDoubleTapZoom(tapX: Float, tapY: Float) {
        // Double-tap zoom: Zoom in 1.8x centered around the tapped location
        val current = _uiState.value.viewportTransform
        val factor = 1.8f
        val newScale = (current.scale * factor).coerceIn(0.002f, 50f)
        val newPanX = tapX - (tapX - current.panX) * (newScale / current.scale)
        val newPanY = tapY - (tapY - current.panY) * (newScale / current.scale)

        val updatedTransform = CadViewportTransform(newPanX, newPanY, newScale)
        val updatedWorld = updatedTransform.screenToWorld(tapX, tapY)

        _uiState.value = _uiState.value.copy(
            viewportTransform = updatedTransform,
            cursorScreenPos = CadPoint2D(tapX, tapY),
            cursorWorldPos = updatedWorld
        )
    }

    fun zoomIn(centerX: Float = 400f, centerY: Float = 600f) {
        onViewportTransformed(panDeltaX = 0f, panDeltaY = 0f, zoomFactor = 1.25f, centroidX = centerX, centroidY = centerY)
    }

    fun zoomOut(centerX: Float = 400f, centerY: Float = 600f) {
        onViewportTransformed(panDeltaX = 0f, panDeltaY = 0f, zoomFactor = 0.8f, centroidX = centerX, centroidY = centerY)
    }

    fun onCursorMoved(screenX: Float, screenY: Float) {
        val rawWorld = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val doc = _uiState.value.document

        val snap = if (_uiState.value.isSnapEnabled && _uiState.value.osnapSettings.isEnabled && doc != null) {
            val anchor = getActiveAnchorPoint()
            snapEngine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = CadPoint2D(screenX, screenY),
                document = doc,
                transform = _uiState.value.viewportTransform,
                settings = _uiState.value.osnapSettings,
                anchorPoint = anchor
            )
        } else null

        val effectiveWorld = snap?.point ?: rawWorld

        _uiState.value = _uiState.value.copy(
            cursorScreenPos = CadPoint2D(screenX, screenY),
            cursorWorldPos = effectiveWorld,
            activeSnapResult = snap
        )
        // Dispatch to active drawing tool if in progress
        val tool = activeDrawingTool
        if (tool != null && tool.isInProgress) {
            val result = tool.onPointerMove(effectiveWorld, createToolContext())
            handleToolResult(result)
        }

        // Live dynamic measurement calculation as cursor moves
        val activeMeasType = _uiState.value.activeTool.toMeasurementType()
        if (activeMeasType != null && _uiState.value.measurementPoints.isNotEmpty()) {
            val liveResult = measurementEngine.calculate(
                activeMeasType,
                _uiState.value.measurementPoints,
                _uiState.value.measurementUnit,
                previewPoint = effectiveWorld
            )
            if (liveResult != null) {
                _uiState.value = _uiState.value.copy(activeMeasurementResult = liveResult)
            }
        }
    }

    fun updateCursorPos(screenX: Float, screenY: Float) = onCursorMoved(screenX, screenY)

    fun onDrawingPointerDown(screenX: Float, screenY: Float) {
        val rawWorld = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val doc = _uiState.value.document

        val snap = if (_uiState.value.isSnapEnabled && _uiState.value.osnapSettings.isEnabled && doc != null) {
            val anchor = getActiveAnchorPoint()
            snapEngine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = CadPoint2D(screenX, screenY),
                document = doc,
                transform = _uiState.value.viewportTransform,
                settings = _uiState.value.osnapSettings,
                anchorPoint = anchor
            )
        } else null

        val effectiveWorld = snap?.point ?: rawWorld

        _uiState.value = _uiState.value.copy(
            cursorScreenPos = CadPoint2D(screenX, screenY),
            cursorWorldPos = effectiveWorld,
            activeSnapResult = snap
        )
        val tool = activeDrawingTool ?: return
        val result = tool.onPointerDown(effectiveWorld, createToolContext())
        handleToolResult(result)
    }

    fun onDrawingPointerMove(screenX: Float, screenY: Float) {
        val rawWorld = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val doc = _uiState.value.document

        val snap = if (_uiState.value.isSnapEnabled && _uiState.value.osnapSettings.isEnabled && doc != null) {
            val anchor = getActiveAnchorPoint()
            snapEngine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = CadPoint2D(screenX, screenY),
                document = doc,
                transform = _uiState.value.viewportTransform,
                settings = _uiState.value.osnapSettings,
                anchorPoint = anchor
            )
        } else null

        val effectiveWorld = snap?.point ?: rawWorld

        _uiState.value = _uiState.value.copy(
            cursorScreenPos = CadPoint2D(screenX, screenY),
            cursorWorldPos = effectiveWorld,
            activeSnapResult = snap
        )
        val tool = activeDrawingTool ?: return
        val result = tool.onPointerMove(effectiveWorld, createToolContext())
        handleToolResult(result)
    }

    fun onDrawingPointerUp(screenX: Float, screenY: Float) {
        val rawWorld = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val doc = _uiState.value.document

        val snap = if (_uiState.value.isSnapEnabled && _uiState.value.osnapSettings.isEnabled && doc != null) {
            val anchor = getActiveAnchorPoint()
            snapEngine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = CadPoint2D(screenX, screenY),
                document = doc,
                transform = _uiState.value.viewportTransform,
                settings = _uiState.value.osnapSettings,
                anchorPoint = anchor
            )
        } else null

        val effectiveWorld = snap?.point ?: rawWorld

        _uiState.value = _uiState.value.copy(
            cursorScreenPos = CadPoint2D(screenX, screenY),
            cursorWorldPos = effectiveWorld,
            activeSnapResult = snap
        )
        val tool = activeDrawingTool ?: return
        val result = tool.onPointerUp(effectiveWorld, createToolContext())
        handleToolResult(result)
    }

    fun confirmActiveTool(isClosed: Boolean = false) {
        val tool = activeDrawingTool
        if (tool is PolylineTool && isClosed) {
            val result = tool.confirmClosedPolygon(createToolContext())
            handleToolResult(result)
            return
        }
        if (tool is RevisionCloudTool) {
            val result = tool.confirmClosedCloud(createToolContext())
            handleToolResult(result)
            return
        }
        if (tool != null) {
            val result = tool.confirmOperation(createToolContext())
            handleToolResult(result)
        }
    }

    fun cancelActiveTool() {
        val tool = activeDrawingTool
        if (tool != null) {
            val result = tool.cancelOperation()
            handleToolResult(result)
        } else {
            _uiState.value = _uiState.value.copy(
                activePolylinePoints = emptyList(),
                isToolInProgress = false,
                statusMessage = "Operation cancelled"
            )
        }
    }

    fun createToolContext(): ToolContext {
        val state = _uiState.value
        return CadToolContext(
            document = state.document,
            currentLayerId = state.currentLayerId,
            viewportTransform = state.viewportTransform,
            isSnapEnabled = state.isSnapEnabled,
            isOrthoEnabled = state.isOrthoEnabled,
            snapEngine = snapEngine,
            osnapSettings = state.osnapSettings,
            activeSnapResult = state.activeSnapResult
        )
    }

    private fun handleToolResult(result: ToolResult) {
        val doc = _uiState.value.document ?: return
        when (result) {
            is ToolResult.EntityCreated -> {
                val updated = cadEngine.editor.addEntity(doc, result.entity)
                val tool = activeDrawingTool
                val isAnnotation = result.entity is CadEntity.Text ||
                        result.entity is CadEntity.Leader ||
                        result.entity is CadEntity.Arrow ||
                        result.entity is CadEntity.RevisionCloud
                val grips = selectionManager.computeGrips(updated, setOf(result.entity.id))
                val newSelection = _uiState.value.selectionState.copy(
                    selectedIds = setOf(result.entity.id),
                    primarySelectedId = result.entity.id,
                    grips = grips
                )
                _uiState.value = _uiState.value.copy(
                    document = updated,
                    selectedEntityId = result.entity.id,
                    selectionState = newSelection,
                    showPropertiesSheet = isAnnotation || _uiState.value.showPropertiesSheet,
                    activePolylinePoints = emptyList(),
                    isToolInProgress = tool?.isInProgress == true,
                    canUndo = cadEngine.editor.canUndo(),
                    canRedo = cadEngine.editor.canRedo(),
                    statusMessage = result.message
                )
            }
            is ToolResult.InProgress -> {
                val tool = activeDrawingTool
                _uiState.value = _uiState.value.copy(
                    activePolylinePoints = result.activePoints,
                    isToolInProgress = tool?.isInProgress == true,
                    statusMessage = result.message
                )
            }
            is ToolResult.Cancelled -> {
                val tool = activeDrawingTool
                _uiState.value = _uiState.value.copy(
                    activePolylinePoints = emptyList(),
                    isToolInProgress = tool?.isInProgress == true,
                    statusMessage = result.message
                )
            }
            is ToolResult.None -> {
                val tool = activeDrawingTool
                _uiState.value = _uiState.value.copy(
                    isToolInProgress = tool?.isInProgress == true
                )
            }
        }
    }

    fun toggleCrosshair() {
        _uiState.value = _uiState.value.copy(isCrosshairVisible = !_uiState.value.isCrosshairVisible)
    }

    fun toggleFullScreenCrosshair() {
        _uiState.value = _uiState.value.copy(isFullScreenCrosshair = !_uiState.value.isFullScreenCrosshair)
    }

    fun onCanvasTapped(screenX: Float, screenY: Float) {
        val rawWorld = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val doc = _uiState.value.document ?: return

        // 1. Detect or use active snap
        var world = if (_uiState.value.isSnapEnabled) {
            val anchor = getActiveAnchorPoint()
            val snap = _uiState.value.activeSnapResult ?: snapEngine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = CadPoint2D(screenX, screenY),
                document = doc,
                transform = _uiState.value.viewportTransform,
                settings = _uiState.value.osnapSettings,
                anchorPoint = anchor
            )
            if (snap != null) {
                snap.point
            } else if (!_uiState.value.osnapSettings.isEnabled) {
                val snapInterval = 50f
                val snappedX = kotlin.math.round(rawWorld.x / snapInterval) * snapInterval
                val snappedY = kotlin.math.round(rawWorld.y / snapInterval) * snapInterval
                CadPoint2D(snappedX, snappedY)
            } else {
                rawWorld
            }
        } else {
            rawWorld
        }

        // Handle Ortho mode if start point exists for linear measurement
        if (_uiState.value.isOrthoEnabled && measurePoint1 != null) {
            val p1 = measurePoint1!!
            val dx = abs(world.x - p1.x)
            val dy = abs(world.y - p1.y)
            world = if (dx > dy) {
                CadPoint2D(world.x, p1.y)
            } else {
                CadPoint2D(p1.x, world.y)
            }
        }

        _uiState.value = _uiState.value.copy(cursorWorldPos = world)
        val currentLayer = _uiState.value.currentLayerId

        // Delegate to activeDrawingTool if one is active
        val drawingTool = activeDrawingTool
        if (drawingTool != null) {
            val result = drawingTool.onCanvasTapped(world, createToolContext())
            handleToolResult(result)
            return
        }

        // Handle active edit command touch placement
        if (_uiState.value.activeEditCommand is EditCommandState.Active) {
            updateEditCommandPlacement(screenX, screenY)
            return
        }

        when (_uiState.value.activeTool) {
            CadTool.SELECT -> {
                val newSelection = selectionManager.selectAt(
                    screenPoint = CadPoint2D(screenX, screenY),
                    transform = _uiState.value.viewportTransform,
                    document = doc,
                    currentState = _uiState.value.selectionState,
                    screenToleranceDp = 24f,
                    toggle = _uiState.value.selectionState.isMultiSelectEnabled
                )
                val primaryId = newSelection.primarySelectedId ?: newSelection.selectedIds.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    selectionState = newSelection,
                    selectedEntityId = primaryId,
                    showPropertiesSheet = newSelection.hasSelection,
                    statusMessage = if (newSelection.hasSelection) {
                        "${newSelection.count} entity selected"
                    } else {
                        "Selection cleared"
                    }
                )
            }
            CadTool.ERASER -> {
                val tolerance = 24f / _uiState.value.viewportTransform.scale
                val nearest = selectionManager.hitTestEngine.hitTest(world, doc.entities, tolerance)
                if (nearest != null) {
                    val entityLayer = doc.layers[nearest.layerId]
                    if (entityLayer?.isLocked == true) {
                        _uiState.value = _uiState.value.copy(
                            statusMessage = "Cannot delete: Layer '${entityLayer.name}' is locked"
                        )
                    } else {
                        val updated = cadEngine.editor.deleteEntity(doc, nearest.id)
                        val newSelection = selectionManager.deselect(_uiState.value.selectionState, updated, nearest.id)
                        _uiState.value = _uiState.value.copy(
                            document = updated,
                            selectionState = newSelection,
                            selectedEntityId = newSelection.primarySelectedId,
                            canUndo = cadEngine.editor.canUndo(),
                            canRedo = cadEngine.editor.canRedo(),
                            statusMessage = "Deleted entity ${nearest.id}"
                        )
                    }
                }
            }
            CadTool.MEASURE_DISTANCE,
            CadTool.MEASURE_ALIGNED,
            CadTool.MEASURE_HORIZONTAL,
            CadTool.MEASURE_VERTICAL,
            CadTool.MEASURE_ANGLE,
            CadTool.MEASURE_RADIUS,
            CadTool.MEASURE_DIAMETER,
            CadTool.MEASURE_AREA,
            CadTool.MEASURE_PERIMETER,
            CadTool.MEASURE_COORDINATE,
            CadTool.MEASURE_POLYLINE,
            CadTool.MEASURE_BOUNDING_BOX,
            CadTool.MEASURE_MULTI_AREA,
            CadTool.MEASURE_SELECTION -> {
                val measureType = _uiState.value.activeTool.toMeasurementType() ?: CadMeasurementType.DISTANCE
                val currentPoints = _uiState.value.measurementPoints
                val tolerance = 24f / _uiState.value.viewportTransform.scale

                // Selection Summary tool: tap objects to toggle inclusion and measure
                if (measureType == CadMeasurementType.SELECTION_SUMMARY) {
                    val hit = selectionManager.hitTestEngine.hitTest(world, doc.entities, tolerance)
                    if (hit != null) {
                        val currentSelected = _uiState.value.selectionState.selectedIds
                        val newSelected = if (hit.id in currentSelected) currentSelected - hit.id else currentSelected + hit.id
                        val newSelectionState = _uiState.value.selectionState.copy(
                            selectedIds = newSelected,
                            primarySelectedId = newSelected.firstOrNull()
                        )
                        val entities = doc.entities.filter { it.id in newSelected }
                        val summary = measurementEngine.measureSelectedEntities(entities, _uiState.value.measurementUnit)
                        recordMeasurementToHistory(summary)
                        _uiState.value = _uiState.value.copy(
                            selectionState = newSelectionState,
                            selectedEntityId = newSelected.firstOrNull(),
                            activeMeasurementType = measureType,
                            activeMeasurementResult = summary,
                            statusMessage = "Selected ${summary.objectCount} items: Area=${summary.displayArea}, Length=${summary.displayTotalLength}"
                        )
                    }
                    return
                }

                // 1. One-tap direct entity measurement if first point and tapped near entity
                if (currentPoints.isEmpty()) {
                    val hit = selectionManager.hitTestEngine.hitTest(world, doc.entities, tolerance)
                    if (hit != null) {
                        if (measureType == CadMeasurementType.MULTI_AREA) {
                            // Convert tapped entity (e.g. circle or closed polyline) into a region!
                            val unit = _uiState.value.measurementUnit
                            val regionResult = measurementEngine.measureEntity(hit, CadMeasurementType.AREA, unit)
                            if (regionResult is CadMeasurementResult.Area) {
                                val regIndex = _uiState.value.multiAreaRegions.size + 1
                                val typeName = hit.javaClass.simpleName
                                val reg = CadMeasurementResult.CadAreaRegion(
                                    id = hit.id,
                                    name = if (_uiState.value.isSubtractMode) "Void $regIndex ($typeName)" else "Region $regIndex ($typeName)",
                                    points = regionResult.points,
                                    isSubtract = _uiState.value.isSubtractMode,
                                    areaSquareUnits = regionResult.areaSquareUnits,
                                    perimeter = regionResult.perimeter
                                )
                                val newRegions = _uiState.value.multiAreaRegions + reg
                                val multiResult = measurementEngine.measureMultipleAreas(newRegions, unit)
                                recordMeasurementToHistory(multiResult)
                                _uiState.value = _uiState.value.copy(
                                    activeMeasurementType = CadMeasurementType.MULTI_AREA,
                                    multiAreaRegions = newRegions,
                                    activeMeasurementResult = multiResult,
                                    statusMessage = "Added ${if (reg.isSubtract) "Void" else "Add"} Region $regIndex from $typeName"
                                )
                                return
                            }
                        }

                        val entityResult = measurementEngine.measureEntity(hit, measureType, _uiState.value.measurementUnit)
                        if (entityResult != null) {
                            recordMeasurementToHistory(entityResult)
                            val legacyResult = when (entityResult) {
                                is CadMeasurementResult.Distance -> MeasurementResult.Distance(entityResult.distance, entityResult.unit, entityResult.deltaX, entityResult.deltaY)
                                is CadMeasurementResult.Angle -> MeasurementResult.Angle(entityResult.degrees, entityResult.radians)
                                is CadMeasurementResult.Area -> MeasurementResult.Area(entityResult.areaSquareUnits, entityResult.unit, entityResult.perimeter)
                                else -> null
                            }
                            _uiState.value = _uiState.value.copy(
                                activeMeasurementType = measureType,
                                activeMeasurementResult = entityResult,
                                measurementResult = legacyResult,
                                measurementPoints = emptyList(),
                                statusMessage = "${measureType.title}: ${entityResult.primaryFormatted}"
                            )
                            return
                        }
                    }
                }

                // 2. Interactive point-based measurement
                val newPoints = currentPoints + world
                val maxPts = measureType.maxPoints

                if (maxPts != null && newPoints.size >= maxPts) {
                    val result = measurementEngine.calculate(measureType, newPoints, _uiState.value.measurementUnit)
                    if (result != null) {
                        recordMeasurementToHistory(result)
                    }
                    val legacyResult = when (result) {
                        is CadMeasurementResult.Distance -> MeasurementResult.Distance(result.distance, result.unit, result.deltaX, result.deltaY)
                        is CadMeasurementResult.Angle -> MeasurementResult.Angle(result.degrees, result.radians)
                        is CadMeasurementResult.Area -> MeasurementResult.Area(result.areaSquareUnits, result.unit, result.perimeter)
                        else -> null
                    }
                    _uiState.value = _uiState.value.copy(
                        activeMeasurementType = measureType,
                        activeMeasurementResult = result,
                        measurementResult = legacyResult,
                        measurementPoints = newPoints,
                        statusMessage = "${measureType.title}: ${result?.primaryFormatted ?: ""}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        activeMeasurementType = measureType,
                        measurementPoints = newPoints,
                        statusMessage = measureType.promptForStep(newPoints.size)
                    )
                }
            }
            CadTool.TEXT -> {
                val newText = CadEntity.Text(
                    id = "text_${UUID.randomUUID().toString().take(6)}",
                    layerId = currentLayer,
                    position = world,
                    text = "NOTE @ (%.0f, %.0f)".format(world.x, world.y)
                )
                val updated = cadEngine.editor.addEntity(doc, newText)
                _uiState.value = _uiState.value.copy(
                    document = updated,
                    selectedEntityId = newText.id,
                    showPropertiesSheet = true,
                    canUndo = cadEngine.editor.canUndo(),
                    canRedo = cadEngine.editor.canRedo(),
                    statusMessage = "Text entity placed"
                )
            }
            CadTool.MOVE, CadTool.COPY, CadTool.ROTATE, CadTool.SCALE, CadTool.TRIM, CadTool.EXTEND -> {
                val tolerance = 24f / _uiState.value.viewportTransform.scale
                val nearest = selectionManager.hitTestEngine.hitTest(world, doc.entities, tolerance)
                if (nearest != null) {
                    val grips = selectionManager.computeGrips(doc, setOf(nearest.id))
                    val newSelection = _uiState.value.selectionState.copy(
                        selectedIds = setOf(nearest.id),
                        primarySelectedId = nearest.id,
                        grips = grips
                    )
                    _uiState.value = _uiState.value.copy(
                        selectionState = newSelection,
                        selectedEntityId = nearest.id
                    )
                    val op = when (_uiState.value.activeTool) {
                        CadTool.MOVE -> EditOperationType.MOVE
                        CadTool.COPY -> EditOperationType.COPY
                        CadTool.ROTATE -> EditOperationType.ROTATE
                        CadTool.SCALE -> EditOperationType.SCALE
                        CadTool.TRIM -> EditOperationType.TRIM
                        CadTool.EXTEND -> EditOperationType.EXTEND
                        else -> EditOperationType.MOVE
                    }
                    startEditCommand(op, explicitBasePoint = world)
                } else {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Tap an entity to ${_uiState.value.activeTool.displayName.lowercase()}"
                    )
                }
            }
            else -> {
                // Pan or other tools
            }
        }
    }

    fun finishPolyline(isClosed: Boolean = false) {
        confirmActiveTool(isClosed = isClosed)
    }

    fun selectTool(tool: CadTool) {
        measurePoint1 = null
        arcPoint1 = null
        arcPoint2 = null
        // Cancel any active drawing tool before switching
        activeDrawingTool?.cancelOperation()

        // Instantiate corresponding DrawingTool
        activeDrawingTool = when (tool) {
            CadTool.LINE -> LineTool()
            CadTool.POLYLINE -> PolylineTool()
            CadTool.CIRCLE -> CircleTool()
            CadTool.ARC -> ArcTool()
            CadTool.RECTANGLE -> RectangleTool()
            CadTool.POINT -> PointTool()
            CadTool.TEXT -> SingleLineTextTool()
            CadTool.MTEXT -> MultiLineTextTool()
            CadTool.LEADER -> LeaderTool()
            CadTool.ARROW -> ArrowTool()
            CadTool.CLOUD -> RevisionCloudTool()
            else -> null
        }

        _uiState.value = _uiState.value.copy(
            activeTool = tool,
            activePolylinePoints = emptyList(),
            isToolInProgress = false,
            statusMessage = when (tool) {
                CadTool.SELECT -> "Select mode: Tap any object to inspect and edit properties"
                CadTool.PAN -> "Pan mode: Drag with one or two fingers to navigate"
                CadTool.LINE -> "Line tool: Tap two points or drag to draw a line segment"
                CadTool.POLYLINE -> "Polyline: Tap vertices, then press 'Complete' or 'Close'"
                CadTool.CIRCLE -> "Circle: Tap center point, then tap or drag radius boundary"
                CadTool.ARC -> "Arc: Tap start, intermediate point, and endpoint"
                CadTool.RECTANGLE -> "Rectangle: Tap or drag two opposite diagonal corners"
                CadTool.POINT -> "Point: Tap canvas to place precision coordinate marker"
                CadTool.TEXT -> "Single-line Text: Tap canvas to place text note"
                CadTool.MTEXT -> "Multi-line Text: Tap canvas to place formatted multi-line note"
                CadTool.LEADER -> "Leader: Tap arrow target point, then tap knee/landing point"
                CadTool.ARROW -> "Arrow: Tap start point, then tap tip/head point"
                CadTool.CLOUD -> "Revision Cloud: Tap boundary vertices, then tap Finish/Close"
                CadTool.ERASER -> "Eraser: Tap any entity to delete it"
                CadTool.MOVE -> "Move: Tap or drag to place target displacement"
                CadTool.COPY -> "Copy: Tap or drag to place copied entities"
                CadTool.ROTATE -> "Rotate: Drag or enter angle to rotate around pivot"
                CadTool.SCALE -> "Scale: Drag or enter factor to scale entities"
                CadTool.TRIM -> "Trim: Tap segment between intersections to trim"
                CadTool.EXTEND -> "Extend: Tap near endpoint to extend to nearest boundary"
                CadTool.MEASURE_DISTANCE -> "Distance: Tap two points to measure distance"
                CadTool.MEASURE_ALIGNED -> "Aligned: Tap two points for aligned dimension"
                CadTool.MEASURE_HORIZONTAL -> "Horizontal: Tap two points to measure ΔX"
                CadTool.MEASURE_VERTICAL -> "Vertical: Tap two points to measure ΔY"
                CadTool.MEASURE_ANGLE -> "Angle: Tap vertex, then two ray endpoints"
                CadTool.MEASURE_RADIUS -> "Radius: Tap circle or center and rim point"
                CadTool.MEASURE_DIAMETER -> "Diameter: Tap circle or two opposite points"
                CadTool.MEASURE_AREA -> "Area: Tap vertices, then tap Finish"
                CadTool.MEASURE_PERIMETER -> "Perimeter: Tap boundary points, then tap Finish"
                CadTool.MEASURE_COORDINATE -> "Coordinate: Tap any location for coordinates"
                CadTool.MEASURE_POLYLINE -> "Polyline: Tap path points, then tap Finish"
                CadTool.MEASURE_BOUNDING_BOX -> "Bounding Box: Tap two corners or an entity"
                CadTool.MEASURE_MULTI_AREA -> "Multi-Area: Tap points then tap Add Region (+) or Void Region (-)"
                CadTool.MEASURE_SELECTION -> "Selection: Tap objects to measure combined area, length and count"
            },
            activeMeasurementType = tool.toMeasurementType(),
            activeMeasurementResult = if (tool.isMeasurementTool) _uiState.value.activeMeasurementResult else null,
            measurementResult = if (tool.isMeasurementTool) _uiState.value.measurementResult else null,
            measurementPoints = emptyList()
        )

        if (tool == CadTool.MEASURE_SELECTION) {
            measureSelectedEntities()
        }

        when (tool) {
            CadTool.MOVE -> startEditCommand(EditOperationType.MOVE)
            CadTool.COPY -> startEditCommand(EditOperationType.COPY)
            CadTool.ROTATE -> startEditCommand(EditOperationType.ROTATE)
            CadTool.SCALE -> startEditCommand(EditOperationType.SCALE)
            CadTool.TRIM -> startEditCommand(EditOperationType.TRIM)
            CadTool.EXTEND -> startEditCommand(EditOperationType.EXTEND)
            else -> {}
        }
    }

    fun selectMeasurementTool(type: CadMeasurementType) {
        val tool = type.toCadTool()
        selectTool(tool)
        _uiState.value = _uiState.value.copy(
            activeMeasurementType = type,
            activeMeasurementResult = null,
            measurementPoints = emptyList(),
            statusMessage = type.promptForStep(0)
        )
    }

    fun setMeasurementUnit(unit: CadUnit) {
        val currentPoints = _uiState.value.measurementPoints
        val currentType = _uiState.value.activeMeasurementType
        val recalculatedResult = if (currentType != null && currentPoints.isNotEmpty()) {
            measurementEngine.calculate(currentType, currentPoints, unit)
        } else if (_uiState.value.activeMeasurementResult != null && currentType != null) {
            when (val r = _uiState.value.activeMeasurementResult) {
                is CadMeasurementResult.Distance -> measurementEngine.measureDistance(r.p1, r.p2, unit)
                is CadMeasurementResult.AlignedDistance -> measurementEngine.measureAlignedDistance(r.p1, r.p2, unit)
                is CadMeasurementResult.HorizontalDistance -> measurementEngine.measureHorizontalDistance(r.p1, r.p2, unit)
                is CadMeasurementResult.VerticalDistance -> measurementEngine.measureVerticalDistance(r.p1, r.p2, unit)
                is CadMeasurementResult.Angle -> measurementEngine.measureAngle(r.vertex, r.p1, r.p2, unit)
                is CadMeasurementResult.Radius -> measurementEngine.measureRadius(r.center, r.rimPoint, unit)
                is CadMeasurementResult.Diameter -> measurementEngine.measureDiameter(r.center, r.p1, r.p2, unit)
                is CadMeasurementResult.Area -> measurementEngine.measureArea(r.points, unit)
                is CadMeasurementResult.Perimeter -> measurementEngine.measurePerimeter(r.points, unit)
                is CadMeasurementResult.Coordinate -> measurementEngine.measureCoordinate(r.point, unit)
                is CadMeasurementResult.PolylineLength -> measurementEngine.measurePolylineLength(r.points, unit)
                is CadMeasurementResult.BoundingBox -> measurementEngine.measureBoundingBox(r.box, unit)
                is CadMeasurementResult.MultiArea -> measurementEngine.measureMultipleAreas(r.regions, unit)
                is CadMeasurementResult.SelectionSummary -> {
                    val entities = _uiState.value.document?.entities?.filter { it.id in _uiState.value.selectionState.selectedIds } ?: emptyList()
                    measurementEngine.measureSelectedEntities(entities, unit)
                }
                null -> null
            }
        } else null

        val legacyResult = when (recalculatedResult) {
            is CadMeasurementResult.Distance -> MeasurementResult.Distance(recalculatedResult.distance, recalculatedResult.unit, recalculatedResult.deltaX, recalculatedResult.deltaY)
            is CadMeasurementResult.Angle -> MeasurementResult.Angle(recalculatedResult.degrees, recalculatedResult.radians)
            is CadMeasurementResult.Area -> MeasurementResult.Area(recalculatedResult.areaSquareUnits, recalculatedResult.unit, recalculatedResult.perimeter)
            else -> _uiState.value.measurementResult
        }

        _uiState.value = _uiState.value.copy(
            units = unit,
            measurementUnit = unit,
            activeMeasurementResult = recalculatedResult ?: _uiState.value.activeMeasurementResult,
            measurementResult = legacyResult
        )
    }

    fun clearMeasurement() {
        measurePoint1 = null
        _uiState.value = _uiState.value.copy(
            measurementResult = null,
            activeMeasurementResult = null,
            measurementPoints = emptyList(),
            multiAreaRegions = emptyList(),
            statusMessage = "Measurement cleared"
        )
    }

    fun copyMeasurementResult(context: android.content.Context) {
        val result = _uiState.value.activeMeasurementResult
        val textToCopy = result?.formatSummary() ?: run {
            val r = _uiState.value.measurementResult
            when (r) {
                is MeasurementResult.Distance -> "Distance: ${r.formatted} (ΔX: %.2f, ΔY: %.2f)".format(r.deltaX, r.deltaY)
                is MeasurementResult.Angle -> "Angle: ${r.formatted}"
                is MeasurementResult.Area -> "Area: ${r.formattedArea} (Perimeter: ${r.formattedPerimeter})"
                null -> "No active measurement"
            }
        }

        try {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("CAD Measurement", textToCopy)
            clipboard.setPrimaryClip(clip)
            _uiState.value = _uiState.value.copy(statusMessage = "Measurement copied to clipboard!")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(statusMessage = "Copied: $textToCopy")
        }
    }

    fun finishMultiPointMeasurement() {
        val type = _uiState.value.activeMeasurementType ?: return
        val points = _uiState.value.measurementPoints
        if (points.size >= type.minPoints) {
            val finalResult = measurementEngine.calculate(type, points, _uiState.value.measurementUnit)
            val legacyResult = when (finalResult) {
                is CadMeasurementResult.Distance -> MeasurementResult.Distance(finalResult.distance, finalResult.unit, finalResult.deltaX, finalResult.deltaY)
                is CadMeasurementResult.Angle -> MeasurementResult.Angle(finalResult.degrees, finalResult.radians)
                is CadMeasurementResult.Area -> MeasurementResult.Area(finalResult.areaSquareUnits, finalResult.unit, finalResult.perimeter)
                else -> null
            }
            if (finalResult != null) {
                recordMeasurementToHistory(finalResult)
            }
            _uiState.value = _uiState.value.copy(
                activeMeasurementResult = finalResult,
                measurementResult = legacyResult,
                statusMessage = "${type.title}: ${finalResult?.primaryFormatted ?: "Completed"}"
            )
        }
    }

    fun recordMeasurementToHistory(result: CadMeasurementResult) {
        val item = CadMeasurementHistoryItem(
            type = result.type,
            primaryValue = result.primaryFormatted,
            area = result.displayArea,
            perimeter = result.displayPerimeter,
            totalLength = result.displayTotalLength,
            objectCount = result.displayObjectCount,
            unit = result.unit,
            result = result
        )
        val updated = (listOf(item) + _uiState.value.measurementHistory.filterNot { it.id == item.id }).take(50)
        _uiState.value = _uiState.value.copy(measurementHistory = updated)
    }

    fun clearMeasurementHistory() {
        _uiState.value = _uiState.value.copy(
            measurementHistory = emptyList(),
            statusMessage = "Measurement history cleared"
        )
    }

    fun deleteMeasurementHistoryItem(id: String) {
        _uiState.value = _uiState.value.copy(
            measurementHistory = _uiState.value.measurementHistory.filterNot { it.id == id }
        )
    }

    fun recallMeasurementHistoryItem(item: CadMeasurementHistoryItem) {
        _uiState.value = _uiState.value.copy(
            activeMeasurementType = item.type,
            activeMeasurementResult = item.result,
            measurementUnit = item.unit,
            statusMessage = "Recalled: ${item.type.title} - ${item.primaryValue}"
        )
    }

    fun addCurrentPointsAsRegion(isSubtract: Boolean = _uiState.value.isSubtractMode) {
        val points = _uiState.value.measurementPoints
        if (points.size < 3) {
            _uiState.value = _uiState.value.copy(statusMessage = "Tap at least 3 points to form an area region")
            return
        }

        val unit = _uiState.value.measurementUnit
        val areaResult = measurementEngine.measureArea(points, unit)
        val perimResult = measurementEngine.measurePerimeter(points, unit)

        val nextIndex = _uiState.value.multiAreaRegions.size + 1
        val regionName = if (isSubtract) "Void Region $nextIndex" else "Add Region $nextIndex"
        val newRegion = CadMeasurementResult.CadAreaRegion(
            id = "region_${System.currentTimeMillis()}",
            name = regionName,
            points = points,
            isSubtract = isSubtract,
            areaSquareUnits = areaResult.areaSquareUnits,
            perimeter = perimResult.perimeter
        )

        val newRegions = _uiState.value.multiAreaRegions + newRegion
        val multiResult = measurementEngine.measureMultipleAreas(newRegions, unit)

        recordMeasurementToHistory(multiResult)

        _uiState.value = _uiState.value.copy(
            activeMeasurementType = CadMeasurementType.MULTI_AREA,
            multiAreaRegions = newRegions,
            measurementPoints = emptyList(),
            activeMeasurementResult = multiResult,
            statusMessage = "Added ${if (isSubtract) "Void" else "Add"} Region $nextIndex: ${unit.formatArea(newRegion.areaSquareUnits)}"
        )
    }

    fun toggleRegionSubtract(regionId: String) {
        val unit = _uiState.value.measurementUnit
        val updated = _uiState.value.multiAreaRegions.map { reg ->
            if (reg.id == regionId) {
                val newSub = !reg.isSubtract
                reg.copy(
                    isSubtract = newSub,
                    name = if (newSub) reg.name.replace("Add", "Void") else reg.name.replace("Void", "Add")
                )
            } else reg
        }
        val multiResult = measurementEngine.measureMultipleAreas(updated, unit)
        recordMeasurementToHistory(multiResult)
        _uiState.value = _uiState.value.copy(
            multiAreaRegions = updated,
            activeMeasurementResult = multiResult,
            statusMessage = "Updated multi-area regions (Net: ${multiResult.displayArea})"
        )
    }

    fun removeRegion(regionId: String) {
        val unit = _uiState.value.measurementUnit
        val updated = _uiState.value.multiAreaRegions.filterNot { it.id == regionId }
        val multiResult = measurementEngine.measureMultipleAreas(updated, unit)
        _uiState.value = _uiState.value.copy(
            multiAreaRegions = updated,
            activeMeasurementResult = if (updated.isNotEmpty()) multiResult else null,
            statusMessage = "Removed region"
        )
    }

    fun clearMultiAreaRegions() {
        _uiState.value = _uiState.value.copy(
            multiAreaRegions = emptyList(),
            measurementPoints = emptyList(),
            activeMeasurementResult = null,
            statusMessage = "Multi-area regions cleared"
        )
    }

    fun setSubtractMode(isSubtract: Boolean) {
        _uiState.value = _uiState.value.copy(isSubtractMode = isSubtract)
    }

    fun measureSelectedEntities() {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds
        val entities = if (selectedIds.isNotEmpty()) {
            doc.entities.filter { it.id in selectedIds }
        } else {
            _uiState.value.selectedEntityId?.let { id -> doc.entities.filter { it.id == id } } ?: emptyList()
        }

        if (entities.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "No entities selected to measure. Tap objects to select them."
            )
            return
        }

        val unit = _uiState.value.measurementUnit
        val result = measurementEngine.measureSelectedEntities(entities, unit)
        recordMeasurementToHistory(result)

        _uiState.value = _uiState.value.copy(
            activeMeasurementType = CadMeasurementType.SELECTION_SUMMARY,
            activeMeasurementResult = result,
            statusMessage = "Selected ${result.objectCount} objects: Area = ${result.displayArea}, Length = ${result.displayTotalLength}"
        )
    }

    fun toggleMeasurementSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showMeasurementSheet = show)
    }

    fun setCurrentLayer(layerId: String) {
        _uiState.value = _uiState.value.copy(currentLayerId = layerId)
    }

    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(isGridVisible = !_uiState.value.isGridVisible)
    }

    fun toggleSnap() {
        val newEnabled = !_uiState.value.isSnapEnabled
        _uiState.value = _uiState.value.copy(
            isSnapEnabled = newEnabled,
            osnapSettings = _uiState.value.osnapSettings.copy(isEnabled = newEnabled),
            activeSnapResult = if (!newEnabled) null else _uiState.value.activeSnapResult
        )
    }

    fun openOsnapSheet() {
        _uiState.value = _uiState.value.copy(showOsnapSheet = true)
    }

    fun closeOsnapSheet() {
        _uiState.value = _uiState.value.copy(showOsnapSheet = false)
    }

    fun setOsnapSettings(settings: CadOsnapSettings) {
        _uiState.value = _uiState.value.copy(
            osnapSettings = settings,
            isSnapEnabled = settings.isEnabled
        )
    }

    fun toggleOsnapMode(mode: CadSnapMode) {
        val current = _uiState.value.osnapSettings
        val updated = current.withModeToggled(mode, !current.enabledModes.contains(mode))
        setOsnapSettings(updated)
    }

    fun setAllOsnapModes(enableAll: Boolean) {
        val current = _uiState.value.osnapSettings
        val updated = current.withAllModes(enableAll)
        setOsnapSettings(updated)
    }

    fun resetOsnapDefaults() {
        setOsnapSettings(CadOsnapSettings())
    }

    fun setOsnapTolerance(tolerancePx: Float) {
        val current = _uiState.value.osnapSettings
        setOsnapSettings(current.copy(snapToleranceScreenPx = tolerancePx))
    }

    fun toggleOrtho() {
        _uiState.value = _uiState.value.copy(isOrthoEnabled = !_uiState.value.isOrthoEnabled)
    }

    fun toggleLayersSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLayersSheet = show)
    }

    fun setLayerSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(layerSearchQuery = query)
    }

    fun togglePropertiesSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPropertiesSheet = show)
    }

    fun toggleLayerVisibility(layerId: String) {
        val doc = _uiState.value.document ?: return
        val layer = doc.layers[layerId] ?: return
        val updated = cadEngine.editor.setLayerVisibility(doc, layerId, !layer.isVisible)
        _uiState.value = _uiState.value.copy(document = updated)
    }

    fun toggleLayerLock(layerId: String) {
        val doc = _uiState.value.document ?: return
        val layer = doc.layers[layerId] ?: return
        val updated = cadEngine.editor.setLayerLock(doc, layerId, !layer.isLocked)
        _uiState.value = _uiState.value.copy(document = updated)
    }

    fun showAllLayers() {
        val doc = _uiState.value.document ?: return
        val updatedLayers = doc.layers.mapValues { (_, layer) ->
            layer.copy(isVisible = true)
        }
        val updatedDoc = doc.copy(layers = updatedLayers)
        _uiState.value = _uiState.value.copy(
            document = updatedDoc.copy(extents = updatedDoc.computeExtents()),
            statusMessage = "All layers visible"
        )
    }

    fun hideAllLayers() {
        val doc = _uiState.value.document ?: return
        val updatedLayers = doc.layers.mapValues { (_, layer) ->
            layer.copy(isVisible = false)
        }
        val updatedDoc = doc.copy(layers = updatedLayers)
        _uiState.value = _uiState.value.copy(
            document = updatedDoc.copy(extents = updatedDoc.computeExtents()),
            statusMessage = "All layers hidden"
        )
    }

    fun addNewLayer(name: String, colorArgb: Long) {
        val doc = _uiState.value.document ?: return
        val newLayerId = "layer_${System.currentTimeMillis() % 10000}"
        val newLayer = CadLayer(
            id = newLayerId,
            name = name,
            colorArgb = colorArgb,
            isVisible = true,
            isLocked = false,
            lineWeight = 1.0f
        )
        val updatedLayers = doc.layers + (newLayerId to newLayer)
        val updatedDoc = doc.copy(layers = updatedLayers)
        _uiState.value = _uiState.value.copy(
            document = updatedDoc,
            currentLayerId = newLayerId,
            statusMessage = "Created new layer: $name"
        )
    }

    fun deleteCurrentLayer() {
        val doc = _uiState.value.document ?: return
        val currentId = _uiState.value.currentLayerId
        if (currentId == "0") {
            _uiState.value = _uiState.value.copy(statusMessage = "Default layer '0' cannot be deleted")
            return
        }
        val targetLayer = doc.layers[currentId] ?: return
        val updatedLayers = doc.layers - currentId
        val updatedEntities = doc.entities.map { entity ->
            if (entity.layerId == currentId) {
                when (entity) {
                    is CadEntity.Line -> entity.copy(layerId = "0")
                    is CadEntity.Polyline -> entity.copy(layerId = "0")
                    is CadEntity.Circle -> entity.copy(layerId = "0")
                    is CadEntity.Arc -> entity.copy(layerId = "0")
                    is CadEntity.Text -> entity.copy(layerId = "0")
                    is CadEntity.Leader -> entity.copy(layerId = "0")
                    is CadEntity.Arrow -> entity.copy(layerId = "0")
                    is CadEntity.RevisionCloud -> entity.copy(layerId = "0")
                    is CadEntity.Point -> entity.copy(layerId = "0")
                    is CadEntity.Dimension -> entity.copy(layerId = "0")
                }
            } else entity
        }
        val updatedDoc = doc.copy(layers = updatedLayers, entities = updatedEntities)
        _uiState.value = _uiState.value.copy(
            document = updatedDoc,
            currentLayerId = "0",
            statusMessage = "Deleted layer '${targetLayer.name}'"
        )
    }

    fun cycleCurrentLayer() {
        val doc = _uiState.value.document ?: return
        val layerIds = doc.layers.keys.toList()
        if (layerIds.isEmpty()) return
        val currentIndex = layerIds.indexOf(_uiState.value.currentLayerId)
        val nextIndex = (currentIndex + 1) % layerIds.size
        val nextId = layerIds[nextIndex]
        val layerName = doc.layers[nextId]?.name ?: nextId
        _uiState.value = _uiState.value.copy(
            currentLayerId = nextId,
            statusMessage = "Active Layer: $layerName"
        )
    }

    fun toggleCurrentLayerVisibility() {
        toggleLayerVisibility(_uiState.value.currentLayerId)
    }

    fun toggleCurrentLayerLock() {
        toggleLayerLock(_uiState.value.currentLayerId)
    }

    fun changeCurrentLayerColor(colorArgb: Long) {
        val doc = _uiState.value.document ?: return
        val currentId = _uiState.value.currentLayerId
        val currentLayer = doc.layers[currentId] ?: return
        val updatedLayer = currentLayer.copy(colorArgb = colorArgb)
        val updatedDoc = doc.copy(layers = doc.layers + (currentId to updatedLayer))
        _uiState.value = _uiState.value.copy(
            document = updatedDoc,
            statusMessage = "Updated layer '${currentLayer.name}' color"
        )
    }

    fun changeSelectedEntitiesColor(colorArgb: Long?) {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds
        if (selectedIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = if (colorArgb == null) "Default color: ByLayer" else "Default draw color updated"
            )
            return
        }
        var updatedDoc = doc
        selectedIds.forEach { id ->
            val entity = updatedDoc.entities.find { it.id == id }
            if (entity != null) {
                val colored = when (entity) {
                    is CadEntity.Line -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Polyline -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Circle -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Arc -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Text -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Leader -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Arrow -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.RevisionCloud -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Point -> entity.copy(colorArgb = colorArgb)
                    is CadEntity.Dimension -> entity.copy(colorArgb = colorArgb)
                }
                updatedDoc = cadEngine.editor.updateEntity(updatedDoc, colored)
            }
        }
        _uiState.value = _uiState.value.copy(
            document = updatedDoc,
            statusMessage = if (colorArgb == null) "Selected entities set to ByLayer" else "Selected entities color updated"
        )
    }

    fun cycleUnits() {
        val unitsList = CadUnit.values()
        val currentIndex = unitsList.indexOf(_uiState.value.units)
        val nextUnit = unitsList[(currentIndex + 1) % unitsList.size]
        _uiState.value = _uiState.value.copy(
            units = nextUnit,
            measurementUnit = nextUnit,
            statusMessage = "Units changed to: ${nextUnit.displayName} (${nextUnit.abbreviation})"
        )
    }

    fun cyclePrecision() {
        val nextPrecision = when (_uiState.value.precisionDecimals) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            else -> 2
        }
        _uiState.value = _uiState.value.copy(
            precisionDecimals = nextPrecision,
            statusMessage = "Precision set to $nextPrecision decimal places (0.${"0".repeat(nextPrecision)})"
        )
    }

    fun toggleLineweights() {
        val nextVal = !_uiState.value.showLineweights
        _uiState.value = _uiState.value.copy(
            showLineweights = nextVal,
            statusMessage = if (nextVal) "Lineweights display ON" else "Lineweights display OFF (Hairline)"
        )
    }

    fun cycleCanvasBackground() {
        val current = _uiState.value.canvasBackgroundColorArgb
        val nextColor = when (current) {
            0xFF0C0E14 -> 0xFF181C24
            0xFF181C24 -> 0xFF0F172A
            0xFF0F172A -> 0xFF1E2638
            else -> 0xFF0C0E14
        }
        val name = when (nextColor) {
            0xFF181C24 -> "Dark Charcoal"
            0xFF0F172A -> "Deep Navy Blue"
            0xFF1E2638 -> "AutoCAD Slate"
            else -> "Pure Canvas Black"
        }
        _uiState.value = _uiState.value.copy(
            canvasBackgroundColorArgb = nextColor,
            statusMessage = "Canvas background: $name"
        )
    }

    fun showStatusMessage(message: String) {
        _uiState.value = _uiState.value.copy(statusMessage = message)
    }

    // =========================================================================
    // Professional CAD Selection System Workflows
    // =========================================================================

    fun startSelectionBox(screenX: Float, screenY: Float) {
        val world = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val screenPt = CadPoint2D(screenX, screenY)
        val box = SelectionBox(
            screenStart = screenPt,
            screenCurrent = screenPt,
            worldStart = world,
            worldCurrent = world
        )
        _uiState.value = _uiState.value.copy(
            cursorScreenPos = screenPt,
            cursorWorldPos = world,
            selectionState = _uiState.value.selectionState.copy(activeBox = box)
        )
    }

    fun updateSelectionBox(screenX: Float, screenY: Float) {
        val currentBox = _uiState.value.selectionState.activeBox ?: return
        val world = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)
        val screenPt = CadPoint2D(screenX, screenY)
        val updatedBox = currentBox.copy(
            screenCurrent = screenPt,
            worldCurrent = world
        )
        _uiState.value = _uiState.value.copy(
            cursorScreenPos = screenPt,
            cursorWorldPos = world,
            selectionState = _uiState.value.selectionState.copy(activeBox = updatedBox)
        )
    }

    fun finishSelectionBox() {
        val box = _uiState.value.selectionState.activeBox
        val doc = _uiState.value.document
        if (box != null && doc != null) {
            val dragDist = box.screenStart.distanceTo(box.screenCurrent)
            if (dragDist > 12f) {
                // Apply Box Selection
                val newSelection = selectionManager.applyBoxSelection(
                    box = box,
                    document = doc,
                    currentState = _uiState.value.selectionState,
                    additive = _uiState.value.selectionState.isMultiSelectEnabled
                )
                val primaryId = newSelection.primarySelectedId ?: newSelection.selectedIds.firstOrNull()
                val modeLabel = if (box.isCrossing) "Crossing (Right-to-Left)" else "Window (Left-to-Right)"
                _uiState.value = _uiState.value.copy(
                    selectionState = newSelection,
                    selectedEntityId = primaryId,
                    showPropertiesSheet = newSelection.hasSelection,
                    statusMessage = "${newSelection.count} entity/entities selected via $modeLabel"
                )
                return
            }
        }
        // If drag distance is small, perform tap selection at the touch point
        if (box != null) {
            onCanvasTapped(box.screenStart.x, box.screenStart.y)
        } else {
            _uiState.value = _uiState.value.copy(
                selectionState = _uiState.value.selectionState.copy(activeBox = null)
            )
        }
    }

    fun cancelSelectionBox() {
        _uiState.value = _uiState.value.copy(
            selectionState = _uiState.value.selectionState.copy(activeBox = null)
        )
    }

    fun selectAll() {
        val doc = _uiState.value.document ?: return
        val newSelection = selectionManager.selectAll(doc, _uiState.value.selectionState, visibleOnly = true)
        _uiState.value = _uiState.value.copy(
            selectionState = newSelection,
            selectedEntityId = newSelection.primarySelectedId,
            showPropertiesSheet = newSelection.hasSelection,
            statusMessage = "Selected all ${newSelection.count} entities"
        )
    }

    fun clearSelection() {
        val newSelection = selectionManager.clearSelection(_uiState.value.selectionState)
        _uiState.value = _uiState.value.copy(
            selectionState = newSelection,
            selectedEntityId = null,
            showPropertiesSheet = false,
            statusMessage = "Selection cleared"
        )
    }

    fun toggleMultiSelect(enabled: Boolean? = null) {
        val current = _uiState.value.selectionState.isMultiSelectEnabled
        val newState = enabled ?: !current
        _uiState.value = _uiState.value.copy(
            selectionState = _uiState.value.selectionState.copy(isMultiSelectEnabled = newState),
            statusMessage = if (newState) "Multi-selection mode enabled" else "Multi-selection mode disabled"
        )
    }

    fun deleteSelectedEntities() {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds.ifEmpty {
            _uiState.value.selectedEntityId?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return

        val lockedEntities = doc.entities.filter { selectedIds.contains(it.id) && doc.layers[it.layerId]?.isLocked == true }
        if (lockedEntities.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Warning: ${lockedEntities.size} entity/entities are on locked layers and cannot be deleted"
            )
        }

        val deletableIds = selectedIds - lockedEntities.map { it.id }.toSet()
        if (deletableIds.isEmpty()) return

        val updated = cadEngine.editor.deleteEntities(doc, deletableIds)
        val newSelection = selectionManager.clearSelection(_uiState.value.selectionState)
        _uiState.value = _uiState.value.copy(
            document = updated,
            selectionState = newSelection,
            selectedEntityId = null,
            showPropertiesSheet = false,
            canUndo = cadEngine.editor.canUndo(),
            canRedo = cadEngine.editor.canRedo(),
            statusMessage = "Deleted ${deletableIds.size} entity/entities"
        )
    }

    fun deleteSelectedEntity() {
        deleteSelectedEntities()
    }

    fun moveSelectedEntities(deltaX: Float, deltaY: Float) {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds.ifEmpty {
            _uiState.value.selectedEntityId?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return

        val lockedEntities = doc.entities.filter { selectedIds.contains(it.id) && doc.layers[it.layerId]?.isLocked == true }
        val moveableIds = selectedIds - lockedEntities.map { it.id }.toSet()
        if (moveableIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusMessage = "Cannot move: Selected entities are on locked layers")
            return
        }

        val updated = cadEngine.editor.moveEntities(doc, moveableIds, deltaX, deltaY)
        val newGrips = selectionManager.computeGrips(updated, selectedIds)
        _uiState.value = _uiState.value.copy(
            document = updated,
            selectionState = _uiState.value.selectionState.copy(grips = newGrips),
            canUndo = cadEngine.editor.canUndo(),
            canRedo = cadEngine.editor.canRedo(),
            statusMessage = "Moved ${moveableIds.size} entity/entities"
        )
    }

    fun copySelectedEntities(deltaX: Float, deltaY: Float) {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds.ifEmpty {
            _uiState.value.selectedEntityId?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return

        val (updated, newCopiedIds) = cadEngine.editor.copyEntities(doc, selectedIds, deltaX, deltaY)
        val newGrips = selectionManager.computeGrips(updated, newCopiedIds)
        _uiState.value = _uiState.value.copy(
            document = updated,
            selectionState = _uiState.value.selectionState.copy(
                selectedIds = newCopiedIds,
                primarySelectedId = newCopiedIds.firstOrNull(),
                grips = newGrips
            ),
            selectedEntityId = newCopiedIds.firstOrNull(),
            canUndo = cadEngine.editor.canUndo(),
            canRedo = cadEngine.editor.canRedo(),
            statusMessage = "Copied ${newCopiedIds.size} entity/entities"
        )
    }

    /**
     * Starts an interactive editing command with command state management and live dynamic preview.
     * Original entities are NOT modified until confirmed by the user.
     */
    fun startEditCommand(operation: EditOperationType, explicitBasePoint: CadPoint2D? = null) {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds.ifEmpty {
            _uiState.value.selectedEntityId?.let { setOf(it) } ?: emptySet()
        }

        val targetEntities = doc.entities.filter { selectedIds.contains(it.id) }
        if (targetEntities.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Select an entity first to ${operation.displayName.lowercase()}"
            )
            return
        }

        // Check if all selected are on locked layers
        val lockedCount = targetEntities.count { doc.layers[it.layerId]?.isLocked == true }
        if (lockedCount == targetEntities.size) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Cannot ${operation.displayName.lowercase()}: Target entity layer is locked"
            )
            return
        }

        val cmdState = EditCommandState.initialize(
            operation = operation,
            targetEntities = targetEntities,
            allDocumentEntities = doc.entities,
            initialPoint = explicitBasePoint
        )

        _uiState.value = _uiState.value.copy(
            activeEditCommand = cmdState,
            statusMessage = (cmdState as? EditCommandState.Active)?.statusPrompt ?: operation.prompt
        )
    }

    /**
     * Updates interactive touch placement for active edit command.
     * Computes dynamic preview entities while maintaining exact CAD world coordinates.
     */
    fun updateEditCommandPlacement(screenX: Float, screenY: Float) {
        val active = _uiState.value.activeEditCommand as? EditCommandState.Active ?: return
        val doc = _uiState.value.document ?: return
        val rawWorld = _uiState.value.viewportTransform.screenToWorld(screenX, screenY)

        val snap = if (_uiState.value.isSnapEnabled && _uiState.value.osnapSettings.isEnabled) {
            snapEngine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = CadPoint2D(screenX, screenY),
                document = doc,
                transform = _uiState.value.viewportTransform,
                settings = _uiState.value.osnapSettings,
                anchorPoint = active.basePoint
            )
        } else null

        val world = if (snap != null) {
            snap.point
        } else if (_uiState.value.isSnapEnabled && !_uiState.value.osnapSettings.isEnabled) {
            val snapInterval = 50f
            CadPoint2D(
                (kotlin.math.round(rawWorld.x / snapInterval) * snapInterval),
                (kotlin.math.round(rawWorld.y / snapInterval) * snapInterval)
            )
        } else rawWorld

        _uiState.value = _uiState.value.copy(
            cursorScreenPos = CadPoint2D(screenX, screenY),
            cursorWorldPos = world,
            activeSnapResult = snap
        )

        when (active.operation) {
            EditOperationType.MOVE, EditOperationType.COPY -> {
                var dx = world.x - active.basePoint.x
                var dy = world.y - active.basePoint.y
                if (_uiState.value.isOrthoEnabled) {
                    if (abs(dx) > abs(dy)) dy = 0f else dx = 0f
                }
                val targetEntities = doc.entities.filter { active.targetEntityIds.contains(it.id) }
                val previews = targetEntities.map { entity ->
                    CadEditMath.moveEntity(entity, dx, dy, "preview_${entity.id}")
                }
                val length = hypot(dx, dy)
                _uiState.value = _uiState.value.copy(
                    activeEditCommand = active.copy(
                        currentPoint = CadPoint2D(active.basePoint.x + dx, active.basePoint.y + dy),
                        deltaX = dx,
                        deltaY = dy,
                        previewEntities = previews,
                        statusPrompt = "Δ(%.1f, %.1f) mm  |  Dist = %.1f mm".format(dx, dy, length)
                    )
                )
            }
            EditOperationType.ROTATE -> {
                val angleRad = atan2((world.y - active.basePoint.y).toDouble(), (world.x - active.basePoint.x).toDouble())
                var angleDeg = Math.toDegrees(angleRad).toFloat()
                if (angleDeg < 0f) angleDeg += 360f
                if (_uiState.value.isOrthoEnabled) {
                    angleDeg = (kotlin.math.round(angleDeg / 90f) * 90f) % 360f
                } else if (_uiState.value.isSnapEnabled) {
                    angleDeg = (kotlin.math.round(angleDeg / 15f) * 15f) % 360f
                }
                val targetEntities = doc.entities.filter { active.targetEntityIds.contains(it.id) }
                val previews = targetEntities.map { entity ->
                    CadEditMath.rotateEntity(entity, active.basePoint, angleDeg, "preview_${entity.id}")
                }
                _uiState.value = _uiState.value.copy(
                    activeEditCommand = active.copy(
                        currentPoint = world,
                        angleDeg = angleDeg,
                        previewEntities = previews,
                        statusPrompt = "Rotation: %.1f° around (%.1f, %.1f)".format(angleDeg, active.basePoint.x, active.basePoint.y)
                    )
                )
            }
            EditOperationType.SCALE -> {
                val baseDist = active.basePoint.distanceTo(active.currentPoint).coerceAtLeast(60f)
                val currentDist = active.basePoint.distanceTo(world)
                var factor = (currentDist / baseDist).coerceIn(0.05f, 20f)
                if (_uiState.value.isSnapEnabled) {
                    factor = (kotlin.math.round(factor * 10f) / 10f).coerceIn(0.1f, 10f)
                }
                val targetEntities = doc.entities.filter { active.targetEntityIds.contains(it.id) }
                val previews = targetEntities.map { entity ->
                    CadEditMath.scaleEntity(entity, active.basePoint, factor, "preview_${entity.id}")
                }
                _uiState.value = _uiState.value.copy(
                    activeEditCommand = active.copy(
                        currentPoint = world,
                        scaleFactor = factor,
                        previewEntities = previews,
                        statusPrompt = "Scale: %.2fx from (%.1f, %.1f)".format(factor, active.basePoint.x, active.basePoint.y)
                    )
                )
            }
            EditOperationType.TRIM -> {
                val targetLine: CadEntity.Line? = (doc.entities.filterIsInstance<CadEntity.Line>().find { active.targetEntityIds.contains(it.id) }
                    ?: doc.entities.filterIsInstance<CadEntity.Line>().minByOrNull { CadEditMath.distanceToSegment(world, it.start, it.end) })
                if (targetLine != null) {
                    val trimResult = CadEditMath.trimLine(targetLine, world, doc.entities)
                    if (trimResult != null) {
                        _uiState.value = _uiState.value.copy(
                            activeEditCommand = active.copy(
                                targetEntityIds = setOf(targetLine.id),
                                currentPoint = world,
                                previewEntities = trimResult.replacementEntities,
                                trimRemovedSegment = trimResult.removedSegment,
                                statusPrompt = "Trim preview: cutting segment under cursor. Tap Confirm (✓) to apply"
                            )
                        )
                    }
                }
            }
            EditOperationType.EXTEND -> {
                val targetLine: CadEntity.Line? = (doc.entities.filterIsInstance<CadEntity.Line>().find { active.targetEntityIds.contains(it.id) }
                    ?: doc.entities.filterIsInstance<CadEntity.Line>().minByOrNull { CadEditMath.distanceToSegment(world, it.start, it.end) })
                if (targetLine != null) {
                    val extendResult = CadEditMath.extendLine(targetLine, world, doc.entities)
                    if (extendResult != null) {
                        _uiState.value = _uiState.value.copy(
                            activeEditCommand = active.copy(
                                targetEntityIds = setOf(targetLine.id),
                                currentPoint = world,
                                previewEntities = listOf(extendResult.extendedEntity),
                                extendNewSegment = extendResult.extensionSegment,
                                statusPrompt = "Extend preview: boundary hit! Tap Confirm (✓) to apply"
                            )
                        )
                    }
                }
            }
            EditOperationType.DELETE -> {}
        }
    }

    fun setEditCommandDelta(deltaX: Float, deltaY: Float) {
        val active = _uiState.value.activeEditCommand as? EditCommandState.Active ?: return
        val doc = _uiState.value.document ?: return
        val targetEntities = doc.entities.filter { active.targetEntityIds.contains(it.id) }
        val previews = targetEntities.map { entity ->
            CadEditMath.moveEntity(entity, deltaX, deltaY, "preview_${entity.id}")
        }
        val length = hypot(deltaX, deltaY)
        _uiState.value = _uiState.value.copy(
            activeEditCommand = active.copy(
                currentPoint = CadPoint2D(active.basePoint.x + deltaX, active.basePoint.y + deltaY),
                deltaX = deltaX,
                deltaY = deltaY,
                previewEntities = previews,
                statusPrompt = "Δ(%.1f, %.1f) mm  |  Dist = %.1f mm".format(deltaX, deltaY, length)
            )
        )
    }

    fun setEditCommandAngle(angleDeg: Float) {
        val active = _uiState.value.activeEditCommand as? EditCommandState.Active ?: return
        val doc = _uiState.value.document ?: return
        val targetEntities = doc.entities.filter { active.targetEntityIds.contains(it.id) }
        val previews = targetEntities.map { entity ->
            CadEditMath.rotateEntity(entity, active.basePoint, angleDeg, "preview_${entity.id}")
        }
        _uiState.value = _uiState.value.copy(
            activeEditCommand = active.copy(
                angleDeg = angleDeg,
                previewEntities = previews,
                statusPrompt = "Rotation: %.1f° around (%.1f, %.1f)".format(angleDeg, active.basePoint.x, active.basePoint.y)
            )
        )
    }

    fun setEditCommandScale(factor: Float) {
        val active = _uiState.value.activeEditCommand as? EditCommandState.Active ?: return
        val doc = _uiState.value.document ?: return
        val targetEntities = doc.entities.filter { active.targetEntityIds.contains(it.id) }
        val previews = targetEntities.map { entity ->
            CadEditMath.scaleEntity(entity, active.basePoint, factor, "preview_${entity.id}")
        }
        _uiState.value = _uiState.value.copy(
            activeEditCommand = active.copy(
                scaleFactor = factor,
                previewEntities = previews,
                statusPrompt = "Scale: %.2fx from (%.1f, %.1f)".format(factor, active.basePoint.x, active.basePoint.y)
            )
        )
    }

    /**
     * Confirms the active CAD edit command, updating the document with full Undo/Redo integration.
     */
    fun confirmEditCommand() {
        val active = _uiState.value.activeEditCommand as? EditCommandState.Active ?: return
        val doc = _uiState.value.document ?: return

        when (active.operation) {
            EditOperationType.MOVE -> {
                if (active.deltaX == 0f && active.deltaY == 0f) {
                    cancelEditCommand()
                    return
                }
                val updated = cadEngine.editor.moveEntities(doc, active.targetEntityIds, active.deltaX, active.deltaY)
                val newGrips = selectionManager.computeGrips(updated, active.targetEntityIds)
                _uiState.value = _uiState.value.copy(
                    document = updated,
                    activeEditCommand = EditCommandState.Idle,
                    selectionState = _uiState.value.selectionState.copy(grips = newGrips),
                    canUndo = cadEngine.editor.canUndo(),
                    canRedo = cadEngine.editor.canRedo(),
                    statusMessage = "Moved ${active.targetEntityIds.size} entity/entities"
                )
            }
            EditOperationType.COPY -> {
                val (updated, newCopiedIds) = cadEngine.editor.copyEntities(doc, active.targetEntityIds, active.deltaX, active.deltaY)
                val newGrips = selectionManager.computeGrips(updated, newCopiedIds)
                _uiState.value = _uiState.value.copy(
                    document = updated,
                    activeEditCommand = EditCommandState.Idle,
                    selectionState = _uiState.value.selectionState.copy(
                        selectedIds = newCopiedIds,
                        primarySelectedId = newCopiedIds.firstOrNull(),
                        grips = newGrips
                    ),
                    selectedEntityId = newCopiedIds.firstOrNull(),
                    canUndo = cadEngine.editor.canUndo(),
                    canRedo = cadEngine.editor.canRedo(),
                    statusMessage = "Copied ${newCopiedIds.size} entity/entities"
                )
            }
            EditOperationType.ROTATE -> {
                if (active.angleDeg % 360f == 0f) {
                    cancelEditCommand()
                    return
                }
                val updated = cadEngine.editor.rotateEntities(doc, active.targetEntityIds, active.basePoint, active.angleDeg)
                val newGrips = selectionManager.computeGrips(updated, active.targetEntityIds)
                _uiState.value = _uiState.value.copy(
                    document = updated,
                    activeEditCommand = EditCommandState.Idle,
                    selectionState = _uiState.value.selectionState.copy(grips = newGrips),
                    canUndo = cadEngine.editor.canUndo(),
                    canRedo = cadEngine.editor.canRedo(),
                    statusMessage = "Rotated ${active.targetEntityIds.size} entity/entities by %.1f°".format(active.angleDeg)
                )
            }
            EditOperationType.SCALE -> {
                if (active.scaleFactor == 1.0f) {
                    cancelEditCommand()
                    return
                }
                val updated = cadEngine.editor.scaleEntities(doc, active.targetEntityIds, active.basePoint, active.scaleFactor)
                val newGrips = selectionManager.computeGrips(updated, active.targetEntityIds)
                _uiState.value = _uiState.value.copy(
                    document = updated,
                    activeEditCommand = EditCommandState.Idle,
                    selectionState = _uiState.value.selectionState.copy(grips = newGrips),
                    canUndo = cadEngine.editor.canUndo(),
                    canRedo = cadEngine.editor.canRedo(),
                    statusMessage = "Scaled ${active.targetEntityIds.size} entity/entities by %.2fx".format(active.scaleFactor)
                )
            }
            EditOperationType.TRIM -> {
                val targetId = active.targetEntityIds.firstOrNull()
                if (targetId != null && active.previewEntities.isNotEmpty()) {
                    val updated = cadEngine.editor.trimEntity(doc, targetId, active.currentPoint) ?: doc
                    val newSelection = selectionManager.clearSelection(_uiState.value.selectionState)
                    _uiState.value = _uiState.value.copy(
                        document = updated,
                        activeEditCommand = EditCommandState.Idle,
                        selectionState = newSelection,
                        selectedEntityId = null,
                        canUndo = cadEngine.editor.canUndo(),
                        canRedo = cadEngine.editor.canRedo(),
                        statusMessage = "Trim applied successfully"
                    )
                } else {
                    cancelEditCommand()
                }
            }
            EditOperationType.EXTEND -> {
                val targetId = active.targetEntityIds.firstOrNull()
                if (targetId != null && active.previewEntities.isNotEmpty()) {
                    val updated = cadEngine.editor.extendEntity(doc, targetId, active.currentPoint) ?: doc
                    val newSelection = selectionManager.computeGrips(updated, setOf(targetId))
                    _uiState.value = _uiState.value.copy(
                        document = updated,
                        activeEditCommand = EditCommandState.Idle,
                        selectionState = _uiState.value.selectionState.copy(grips = newSelection),
                        canUndo = cadEngine.editor.canUndo(),
                        canRedo = cadEngine.editor.canRedo(),
                        statusMessage = "Extend applied successfully"
                    )
                } else {
                    cancelEditCommand()
                }
            }
            EditOperationType.DELETE -> {
                deleteSelectedEntities()
                _uiState.value = _uiState.value.copy(activeEditCommand = EditCommandState.Idle)
            }
        }
    }

    /**
     * Cancels the active CAD edit command, leaving original document completely unmodified.
     */
    fun cancelEditCommand() {
        _uiState.value = _uiState.value.copy(
            activeEditCommand = EditCommandState.Idle,
            statusMessage = "Operation cancelled"
        )
    }

    fun rotateSelectedEntities(center: CadPoint2D, angleDegrees: Float) {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds.ifEmpty {
            _uiState.value.selectedEntityId?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return
        val updated = cadEngine.editor.rotateEntities(doc, selectedIds, center, angleDegrees)
        val newGrips = selectionManager.computeGrips(updated, selectedIds)
        _uiState.value = _uiState.value.copy(
            document = updated,
            selectionState = _uiState.value.selectionState.copy(grips = newGrips),
            canUndo = cadEngine.editor.canUndo(),
            canRedo = cadEngine.editor.canRedo(),
            statusMessage = "Rotated ${selectedIds.size} entity/entities by %.1f°".format(angleDegrees)
        )
    }

    fun scaleSelectedEntities(basePoint: CadPoint2D, factor: Float) {
        val doc = _uiState.value.document ?: return
        val selectedIds = _uiState.value.selectionState.selectedIds.ifEmpty {
            _uiState.value.selectedEntityId?.let { setOf(it) } ?: emptySet()
        }
        if (selectedIds.isEmpty()) return
        val updated = cadEngine.editor.scaleEntities(doc, selectedIds, basePoint, factor)
        val newGrips = selectionManager.computeGrips(updated, selectedIds)
        _uiState.value = _uiState.value.copy(
            document = updated,
            selectionState = _uiState.value.selectionState.copy(grips = newGrips),
            canUndo = cadEngine.editor.canUndo(),
            canRedo = cadEngine.editor.canRedo(),
            statusMessage = "Scaled ${selectedIds.size} entity/entities by %.2fx".format(factor)
        )
    }

    fun trimSelectedEntity(targetEntityId: String, clickPoint: CadPoint2D) {
        val doc = _uiState.value.document ?: return
        val updated = cadEngine.editor.trimEntity(doc, targetEntityId, clickPoint)
        if (updated != null) {
            _uiState.value = _uiState.value.copy(
                document = updated,
                canUndo = cadEngine.editor.canUndo(),
                canRedo = cadEngine.editor.canRedo(),
                statusMessage = "Trim applied"
            )
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "No cutting intersection found to trim")
        }
    }

    fun extendSelectedEntity(targetEntityId: String, clickPoint: CadPoint2D) {
        val doc = _uiState.value.document ?: return
        val updated = cadEngine.editor.extendEntity(doc, targetEntityId, clickPoint)
        if (updated != null) {
            _uiState.value = _uiState.value.copy(
                document = updated,
                canUndo = cadEngine.editor.canUndo(),
                canRedo = cadEngine.editor.canRedo(),
                statusMessage = "Extend applied"
            )
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "No boundary edge found to extend towards")
        }
    }

    fun updateSelectedEntity(updatedEntity: CadEntity) {
        val doc = _uiState.value.document ?: return
        val currentEntity = doc.entities.find { it.id == updatedEntity.id } ?: return
        val currentLayer = doc.layers[currentEntity.layerId]
        if (currentLayer?.isLocked == true) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Cannot modify: Layer '${currentLayer.name}' is locked"
            )
            return
        }
        val targetLayer = doc.layers[updatedEntity.layerId]
        if (targetLayer?.isLocked == true) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Cannot assign: Target layer '${targetLayer.name}' is locked"
            )
            return
        }
        val updatedDoc = cadEngine.editor.updateEntity(doc, updatedEntity)
        val grips = selectionManager.computeGrips(updatedDoc, _uiState.value.selectionState.selectedIds)
        _uiState.value = _uiState.value.copy(
            document = updatedDoc,
            selectionState = _uiState.value.selectionState.copy(grips = grips),
            canUndo = cadEngine.editor.canUndo(),
            canRedo = cadEngine.editor.canRedo()
        )
    }

    fun updateSelectedEntityText(newText: String) {
        val doc = _uiState.value.document ?: return
        val id = _uiState.value.selectedEntityId ?: return
        val entity = doc.entities.find { it.id == id } as? CadEntity.Text ?: return
        updateSelectedEntity(entity.copy(text = newText))
    }

    fun updateSelectedEntityLayer(layerId: String) {
        val doc = _uiState.value.document ?: return
        val id = _uiState.value.selectedEntityId ?: return
        val entity = doc.entities.find { it.id == id } ?: return
        val updatedEntity = when (entity) {
            is CadEntity.Point -> entity.copy(layerId = layerId)
            is CadEntity.Line -> entity.copy(layerId = layerId)
            is CadEntity.Polyline -> entity.copy(layerId = layerId)
            is CadEntity.Circle -> entity.copy(layerId = layerId)
            is CadEntity.Arc -> entity.copy(layerId = layerId)
            is CadEntity.Text -> entity.copy(layerId = layerId)
            is CadEntity.Dimension -> entity.copy(layerId = layerId)
            is CadEntity.Leader -> entity.copy(layerId = layerId)
            is CadEntity.Arrow -> entity.copy(layerId = layerId)
            is CadEntity.RevisionCloud -> entity.copy(layerId = layerId)
        }
        updateSelectedEntity(updatedEntity)
    }

    fun saveDocument(format: CadFormat = _uiState.value.format): Boolean {
        val doc = _uiState.value.document ?: return false
        val currentDrawingId = _uiState.value.drawingId ?: "new_drawing"
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.saveDrawing(currentDrawingId, doc, format)
            result.onSuccess { savedFile ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasUnsavedChanges = false,
                    hasCrashRecoveryAvailable = false,
                    showCrashRecoveryPrompt = false,
                    format = format,
                    drawingTitle = savedFile.name,
                    statusMessage = "Drawing saved successfully (${format.name})"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Save notice: ${error.message ?: "Saved"}"
                )
            }
        }
        return true
    }

    fun triggerAutoSave() {
        val doc = _uiState.value.document ?: return
        val currentDrawingId = _uiState.value.drawingId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoSaving = true)
            val result = repository.autoSaveDrawing(currentDrawingId, doc)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isAutoSaving = false,
                    lastAutoSavedTimestamp = System.currentTimeMillis()
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isAutoSaving = false)
            }
        }
    }

    fun restoreCrashRecovery() {
        val currentDrawingId = _uiState.value.drawingId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = repository.recoverDrawing(currentDrawingId)
            result.onSuccess { recoveredDoc ->
                _uiState.value = _uiState.value.copy(
                    document = recoveredDoc,
                    isLoading = false,
                    hasUnsavedChanges = true,
                    showCrashRecoveryPrompt = false,
                    hasCrashRecoveryAvailable = false,
                    statusMessage = "Restored unsaved session snapshot successfully!"
                )
                zoomExtents(recoveredDoc)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to restore recovery data: ${error.message}"
                )
            }
        }
    }

    fun dismissCrashRecovery(discard: Boolean = true) {
        val currentDrawingId = _uiState.value.drawingId
        if (discard && currentDrawingId != null) {
            viewModelScope.launch {
                repository.discardCrashRecovery(currentDrawingId)
            }
        }
        _uiState.value = _uiState.value.copy(
            showCrashRecoveryPrompt = false,
            hasCrashRecoveryAvailable = false
        )
    }

    fun toggleSaveFormatMenu(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSaveFormatMenu = show)
    }

    fun toggleExportSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportSheet = show)
    }

    fun undo() {
        val doc = _uiState.value.document ?: return
        if (cadEngine.editor.canUndo()) {
            val updated = cadEngine.editor.undo(doc)
            _uiState.value = _uiState.value.copy(
                document = updated,
                canUndo = cadEngine.editor.canUndo(),
                canRedo = cadEngine.editor.canRedo(),
                statusMessage = "Undo action"
            )
        }
    }

    fun redo() {
        val doc = _uiState.value.document ?: return
        if (cadEngine.editor.canRedo()) {
            val updated = cadEngine.editor.redo(doc)
            _uiState.value = _uiState.value.copy(
                document = updated,
                canUndo = cadEngine.editor.canUndo(),
                canRedo = cadEngine.editor.canRedo(),
                statusMessage = "Redo action"
            )
        }
    }

    fun zoomExtents(viewportWidth: Float, viewportHeight: Float) {
        zoomExtents(document = _uiState.value.document, viewportWidth = viewportWidth, viewportHeight = viewportHeight)
    }

    fun zoomExtents(
        document: CadDocument? = _uiState.value.document,
        viewportWidth: Float = 720f,
        viewportHeight: Float = 1280f
    ) {
        val doc = document ?: return
        val extents = doc.computeExtents()
        if (extents.isEmpty) {
            _uiState.value = _uiState.value.copy(
                viewportTransform = CadViewportTransform(panX = viewportWidth / 2f, panY = viewportHeight / 2f, scale = 0.05f)
            )
            return
        }

        // Fit extents into viewport with 15% margin padding
        val margin = 0.85f
        val availW = viewportWidth * margin
        val availH = viewportHeight * margin
        val scaleX = availW / extents.width.coerceAtLeast(10f)
        val scaleY = availH / extents.height.coerceAtLeast(10f)
        val fitScale = minOf(scaleX, scaleY).coerceIn(0.005f, 25f)

        // Center the drawing in the viewport
        val panX = (viewportWidth / 2f) - extents.centerX * fitScale
        val panY = (viewportHeight / 2f) + extents.centerY * fitScale // Since Y is inverted in CAD

        _uiState.value = _uiState.value.copy(
            viewportTransform = CadViewportTransform(panX = panX, panY = panY, scale = fitScale)
        )
    }
}

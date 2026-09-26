package com.example.cad.engine.tools

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.cad.engine.selection.SelectionState
import com.example.cad.engine.snap.CadOsnapSettings
import com.example.cad.engine.snap.CadSnapEngine
import com.example.cad.engine.snap.CadSnapResult
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform

/**
 * Interface representing the context provided to a [DrawingTool] during execution.
 */
interface ToolContext {
    val document: CadDocument?
    val currentLayerId: String
    val viewportTransform: CadViewportTransform
    val isSnapEnabled: Boolean
    val isOrthoEnabled: Boolean
    val snapInterval: Float
        get() = 50f
    val snapEngine: CadSnapEngine?
        get() = null
    val osnapSettings: CadOsnapSettings?
        get() = null
    val activeSnapResult: CadSnapResult?
        get() = null

    /**
     * Resolves snapping (Object Snap / OSNAP prioritized, falling back to grid snap)
     * and ortho constraints for a target world coordinate.
     */
    fun resolveCoordinates(
        rawWorld: CadPoint2D,
        anchorPoint: CadPoint2D? = null
    ): CadPoint2D {
        var pt = rawWorld

        // 1. Check Object Snap (OSNAP) first if enabled
        val osnap = osnapSettings
        val engine = snapEngine
        val doc = document
        if (isSnapEnabled && osnap != null && osnap.isEnabled && doc != null && engine != null) {
            val screenPt = viewportTransform.worldToScreen(rawWorld)
            val detectedSnap = engine.findSnap(
                cursorWorld = rawWorld,
                cursorScreen = screenPt,
                document = doc,
                transform = viewportTransform,
                settings = osnap,
                anchorPoint = anchorPoint
            )
            if (detectedSnap != null) {
                return detectedSnap.point
            }
        } else if (activeSnapResult != null && isSnapEnabled) {
            return activeSnapResult!!.point
        }

        // 2. Grid Snapping fallback if enabled
        if (isSnapEnabled && (osnap == null || !osnap.isEnabled)) {
            val sx = kotlin.math.round(pt.x / snapInterval) * snapInterval
            val sy = kotlin.math.round(pt.y / snapInterval) * snapInterval
            pt = CadPoint2D(sx, sy)
        }

        // 3. Ortho mode constraint
        if (isOrthoEnabled && anchorPoint != null) {
            val dx = kotlin.math.abs(pt.x - anchorPoint.x)
            val dy = kotlin.math.abs(pt.y - anchorPoint.y)
            pt = if (dx > dy) {
                CadPoint2D(pt.x, anchorPoint.y)
            } else {
                CadPoint2D(anchorPoint.x, pt.y)
            }
        }
        return pt
    }
}

/**
 * Concrete implementation of [ToolContext] for CAD operations.
 */
data class CadToolContext(
    override val document: CadDocument? = null,
    override val currentLayerId: String = "0",
    override val viewportTransform: CadViewportTransform = CadViewportTransform(),
    override val isSnapEnabled: Boolean = true,
    override val isOrthoEnabled: Boolean = false,
    override val snapInterval: Float = 50f,
    override val snapEngine: CadSnapEngine? = null,
    override val osnapSettings: CadOsnapSettings? = null,
    override val activeSnapResult: CadSnapResult? = null
) : ToolContext

/**
 * Result returned when a tool handles a touch interaction (tap or drag).
 */
sealed class ToolResult {
    /** The tool is still actively gathering inputs / drawing (e.g. first point set, dragging) */
    data class InProgress(
        val prompt: String,
        val intermediatePoints: List<CadPoint2D> = emptyList()
    ) : ToolResult() {
        val message: String get() = prompt
        val activePoints: List<CadPoint2D> get() = intermediatePoints
    }

    /** The tool created or committed an entity to the document */
    data class EntityCreated(
        val entity: CadEntity,
        val message: String = "Entity created"
    ) : ToolResult()

    /** The tool cancelled its active operation or reset state */
    data class Cancelled(
        val message: String = "Operation cancelled"
    ) : ToolResult()

    /** No state change or pass-through */
    object None : ToolResult()
}

/**
 * Reusable DrawingTool interface.
 * Decouples interactive CAD drawing logic from the ViewModel,
 * enabling pluggable tools with start/end coordinates, dynamic dragging preview,
 * snap/ortho calculation, cancel, confirm, and undo support.
 */
interface DrawingTool {
    /** Unique identifier or tool type name */
    val name: String

    /** Short prompt or guidance text for user */
    val prompt: String

    /** Points gathered in current operation */
    val activePoints: List<CadPoint2D>

    /** Whether the tool is currently in the middle of a multi-step operation */
    val isInProgress: Boolean

    /**
     * Handles pointer down/drag start event.
     */
    fun onPointerDown(worldPoint: CadPoint2D, context: ToolContext): ToolResult = ToolResult.None

    /**
     * Handles dynamic pointer motion (dragging or cursor hovering) for real-time preview.
     */
    fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult = ToolResult.None

    /**
     * Handles pointer up/tap release event.
     */
    fun onPointerUp(worldPoint: CadPoint2D, context: ToolContext): ToolResult = ToolResult.None

    /**
     * Handles discrete canvas tap.
     */
    fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult

    /**
     * Confirms or finalizes the active multi-step operation (e.g. polyline completion).
     */
    fun confirmOperation(context: ToolContext): ToolResult = ToolResult.None

    /**
     * Explicitly cancels the active drawing operation.
     */
    fun cancelOperation(): ToolResult

    /**
     * Renders real-time interactive preview graphics to the canvas during dragging or input.
     */
    fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    )
}

package com.example.cad.engine.tools

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadViewportTransform
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private fun resolveAnnotationLayer(context: ToolContext): String {
    return if (context.currentLayerId == "0" && context.document?.layers?.containsKey("markup") == true) {
        "markup"
    } else {
        context.currentLayerId
    }
}

/**
 * Single-line Text Placement Tool
 */
class SingleLineTextTool : DrawingTool {
    override val name: String = "Text"
    override val prompt: String = "Text: Tap canvas to place single-line note"

    private var previewPoint: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(previewPoint)

    override val isInProgress: Boolean = false

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        previewPoint = context.resolveCoordinates(worldPoint)
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint)
        val entity = CadEntity.Text(
            id = "text_${UUID.randomUUID().toString().take(6)}",
            layerId = resolveAnnotationLayer(context),
            position = resolved,
            text = "NOTE @ (%.0f, %.0f)".format(resolved.x, resolved.y),
            textHeight = 12f,
            rotationDeg = 0f,
            isMultiLine = false
        )
        previewPoint = null
        return ToolResult.EntityCreated(entity, "Text placed at (%.1f, %.1f)".format(resolved.x, resolved.y))
    }

    override fun cancelOperation(): ToolResult {
        previewPoint = null
        return ToolResult.Cancelled("Text cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val pt = previewPoint ?: cursorWorldPos
        val s = transform.worldToScreen(pt)
        val h = 12f * transform.scale
        val w = 60f * transform.scale

        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(s.x, s.y),
            end = Offset(s.x + w, s.y),
            strokeWidth = 2f
        )
        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(s.x, s.y - h),
            end = Offset(s.x, s.y),
            strokeWidth = 2f
        )
    }
}

/**
 * Multi-line Text (MText) Placement Tool
 */
class MultiLineTextTool : DrawingTool {
    override val name: String = "MText"
    override val prompt: String = "MText: Tap canvas to place multi-line text note"

    private var previewPoint: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(previewPoint)

    override val isInProgress: Boolean = false

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        previewPoint = context.resolveCoordinates(worldPoint)
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint)
        val entity = CadEntity.Text(
            id = "mtext_${UUID.randomUUID().toString().take(6)}",
            layerId = resolveAnnotationLayer(context),
            position = resolved,
            text = "1. VERIFY IN FIELD\n2. ALL DIMS IN MM\n3. DO NOT SCALE DRAWING",
            textHeight = 12f,
            rotationDeg = 0f,
            isMultiLine = true,
            frameWidth = 120f
        )
        previewPoint = null
        return ToolResult.EntityCreated(entity, "Multi-line text placed at (%.1f, %.1f)".format(resolved.x, resolved.y))
    }

    override fun cancelOperation(): ToolResult {
        previewPoint = null
        return ToolResult.Cancelled("MText cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val pt = previewPoint ?: cursorWorldPos
        val s = transform.worldToScreen(pt)
        val h = 36f * transform.scale
        val w = 100f * transform.scale

        drawScope.drawRect(
            color = PreviewCyan.copy(alpha = 0.4f),
            topLeft = Offset(s.x, s.y - h),
            size = androidx.compose.ui.geometry.Size(w, h),
            style = Stroke(width = 1.5f, pathEffect = PreviewDashedStroke)
        )
    }
}

/**
 * Leader Note Tool:
 * Step 1: Tap arrow tip (feature being annotated)
 * Step 2: Tap landing knee point
 */
class LeaderTool : DrawingTool {
    override val name: String = "Leader"
    override val prompt: String
        get() = if (arrowPoint == null) "Leader: Tap arrow target point" else "Leader: Tap knee/landing point"

    private var arrowPoint: CadPoint2D? = null
    private var currentCursor: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(arrowPoint, currentCursor)

    override val isInProgress: Boolean
        get() = arrowPoint != null

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (arrowPoint != null) {
            currentCursor = context.resolveCoordinates(worldPoint, arrowPoint)
            return ToolResult.InProgress("Placing landing knee...", activePoints)
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint, arrowPoint)
        if (arrowPoint == null) {
            arrowPoint = resolved
            currentCursor = resolved
            return ToolResult.InProgress("Arrow point set: (%.1f, %.1f)".format(resolved.x, resolved.y), activePoints)
        } else {
            val pArrow = arrowPoint!!
            val knee = resolved
            val landingDir = if (knee.x >= pArrow.x) 1f else -1f
            val landingEnd = CadPoint2D(knee.x + landingDir * 25f, knee.y)

            val leader = CadEntity.Leader(
                id = "leader_${UUID.randomUUID().toString().take(6)}",
                layerId = resolveAnnotationLayer(context),
                arrowPoint = pArrow,
                kneePoint = knee,
                landingEndPoint = landingEnd,
                text = "NOTE @ (%.0f, %.0f)".format(pArrow.x, pArrow.y),
                textHeight = 12f,
                arrowSize = 8f
            )
            arrowPoint = null
            currentCursor = null
            return ToolResult.EntityCreated(leader, "Leader note placed")
        }
    }

    override fun cancelOperation(): ToolResult {
        arrowPoint = null
        currentCursor = null
        return ToolResult.Cancelled("Leader cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val pArrow = arrowPoint ?: return
        val knee = currentCursor ?: cursorWorldPos

        val sArrow = transform.worldToScreen(pArrow)
        val sKnee = transform.worldToScreen(knee)

        // Draw leader line from arrow to knee
        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(sArrow.x, sArrow.y),
            end = Offset(sKnee.x, sKnee.y),
            strokeWidth = 2.5f
        )

        // Draw horizontal landing line
        val landingDir = if (knee.x >= pArrow.x) 1f else -1f
        val landingEnd = CadPoint2D(knee.x + landingDir * 25f, knee.y)
        val sLandingEnd = transform.worldToScreen(landingEnd)
        drawScope.drawLine(
            color = PreviewYellow,
            start = Offset(sKnee.x, sKnee.y),
            end = Offset(sLandingEnd.x, sLandingEnd.y),
            strokeWidth = 2.5f
        )

        // Draw arrowhead at arrow tip
        val angle = atan2((knee.y - pArrow.y).toDouble(), (knee.x - pArrow.x).toDouble())
        val arrowLen = 14f
        val wingAngle = 0.4
        val leftX = sArrow.x + (arrowLen * cos(angle + wingAngle)).toFloat()
        val leftY = sArrow.y + (arrowLen * sin(angle + wingAngle)).toFloat()
        val rightX = sArrow.x + (arrowLen * cos(angle - wingAngle)).toFloat()
        val rightY = sArrow.y + (arrowLen * sin(angle - wingAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(sArrow.x, sArrow.y)
            lineTo(leftX, leftY)
            lineTo(rightX, rightY)
            close()
        }
        drawScope.drawPath(headPath, color = PreviewCyan)
    }
}

/**
 * Arrow Drawing Tool:
 * Step 1: Tap arrow start / tail
 * Step 2: Tap arrow tip / head
 */
class ArrowTool : DrawingTool {
    override val name: String = "Arrow"
    override val prompt: String
        get() = if (startPoint == null) "Arrow: Tap start / tail point" else "Arrow: Tap tip / head point"

    private var startPoint: CadPoint2D? = null
    private var currentEndPoint: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = listOfNotNull(startPoint, currentEndPoint)

    override val isInProgress: Boolean
        get() = startPoint != null

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (startPoint != null) {
            currentEndPoint = context.resolveCoordinates(worldPoint, startPoint)
            val dist = startPoint!!.distanceTo(currentEndPoint!!)
            return ToolResult.InProgress("Length: %.1f mm".format(dist), activePoints)
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val resolved = context.resolveCoordinates(worldPoint, startPoint)
        if (startPoint == null) {
            startPoint = resolved
            currentEndPoint = resolved
            return ToolResult.InProgress("Start point set: (%.1f, %.1f)".format(resolved.x, resolved.y), activePoints)
        } else {
            val pStart = startPoint!!
            val pEnd = resolved
            val arrow = CadEntity.Arrow(
                id = "arrow_${UUID.randomUUID().toString().take(6)}",
                layerId = resolveAnnotationLayer(context),
                start = pStart,
                end = pEnd,
                headSize = 10f,
                isDoubleHeaded = false,
                label = null
            )
            startPoint = null
            currentEndPoint = null
            return ToolResult.EntityCreated(arrow, "Arrow placed (length: %.1f mm)".format(pStart.distanceTo(pEnd)))
        }
    }

    override fun cancelOperation(): ToolResult {
        startPoint = null
        currentEndPoint = null
        return ToolResult.Cancelled("Arrow cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        val p1 = startPoint ?: return
        val p2 = currentEndPoint ?: cursorWorldPos
        val s1 = transform.worldToScreen(p1)
        val s2 = transform.worldToScreen(p2)

        // Draw shaft
        drawScope.drawLine(
            color = PreviewCyan,
            start = Offset(s1.x, s1.y),
            end = Offset(s2.x, s2.y),
            strokeWidth = 3f
        )

        // Draw arrowhead at p2
        val angle = atan2((s2.y - s1.y).toDouble(), (s2.x - s1.x).toDouble())
        val arrowLen = 16f
        val wingAngle = 0.45
        val leftX = s2.x - (arrowLen * cos(angle + wingAngle)).toFloat()
        val leftY = s2.y - (arrowLen * sin(angle + wingAngle)).toFloat()
        val rightX = s2.x - (arrowLen * cos(angle - wingAngle)).toFloat()
        val rightY = s2.y - (arrowLen * sin(angle - wingAngle)).toFloat()

        val headPath = Path().apply {
            moveTo(s2.x, s2.y)
            lineTo(leftX, leftY)
            lineTo(rightX, rightY)
            close()
        }
        drawScope.drawPath(headPath, color = PreviewCyan)
    }
}

/**
 * Revision Cloud (Markup) Tool:
 * Tap consecutive boundary points around the change/revision area.
 * Press confirm/close polygon to finish the cloud bubble.
 */
class RevisionCloudTool : DrawingTool {
    override val name: String = "Cloud"
    override val prompt: String
        get() = if (points.isEmpty()) "Cloud: Tap first boundary point" else "Cloud: Tap vertex ${points.size + 1} or Finish"

    private val points = mutableListOf<CadPoint2D>()
    private var previewNext: CadPoint2D? = null

    override val activePoints: List<CadPoint2D>
        get() = points + listOfNotNull(previewNext)

    override val isInProgress: Boolean
        get() = points.isNotEmpty()

    override fun onPointerMove(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        if (points.isNotEmpty()) {
            previewNext = context.resolveCoordinates(worldPoint, points.last())
            return ToolResult.InProgress("Vertex ${points.size + 1}", activePoints)
        }
        return ToolResult.None
    }

    override fun onCanvasTapped(worldPoint: CadPoint2D, context: ToolContext): ToolResult {
        val anchor = points.lastOrNull()
        val resolved = context.resolveCoordinates(worldPoint, anchor)

        // If user taps close to the start point and has >= 3 points, close it automatically!
        if (points.size >= 3 && resolved.distanceTo(points.first()) < 20f / context.viewportTransform.scale) {
            return confirmOperation(context)
        }

        points.add(resolved)
        previewNext = resolved
        return ToolResult.InProgress("Vertex ${points.size} added", activePoints)
    }

    override fun confirmOperation(context: ToolContext): ToolResult {
        if (points.size < 3) {
            return ToolResult.InProgress("Need at least 3 points for a revision cloud", activePoints)
        }
        val entity = CadEntity.RevisionCloud(
            id = "cloud_${UUID.randomUUID().toString().take(6)}",
            layerId = resolveAnnotationLayer(context),
            vertices = points.toList(),
            arcRadius = 15f,
            strokeWidth = 2f,
            isClosed = true,
            revisionTag = "REV 1"
        )
        points.clear()
        previewNext = null
        return ToolResult.EntityCreated(entity, "Revision Cloud markup created with ${entity.vertices.size} vertices")
    }

    fun confirmClosedCloud(context: ToolContext): ToolResult = confirmOperation(context)

    override fun cancelOperation(): ToolResult {
        points.clear()
        previewNext = null
        return ToolResult.Cancelled("Revision cloud cancelled")
    }

    override fun renderPreview(
        drawScope: DrawScope,
        transform: CadViewportTransform,
        cursorWorldPos: CadPoint2D
    ) {
        if (points.isEmpty()) return
        val allPts = points + listOfNotNull(previewNext)
        if (allPts.size < 2) {
            val s = transform.worldToScreen(points.first())
            drawScope.drawCircle(color = PreviewYellow, radius = 6f, center = Offset(s.x, s.y))
            return
        }

        // Draw preview scalloped arcs or segments
        for (i in 0 until allPts.size - 1) {
            val p1 = transform.worldToScreen(allPts[i])
            val p2 = transform.worldToScreen(allPts[i + 1])
            drawScope.drawLine(
                color = PreviewYellow,
                start = Offset(p1.x, p1.y),
                end = Offset(p2.x, p2.y),
                strokeWidth = 2f,
                pathEffect = PreviewDashedStroke
            )
        }
        // Closing segment to first point
        if (points.size >= 3) {
            val last = transform.worldToScreen(allPts.last())
            val first = transform.worldToScreen(allPts.first())
            drawScope.drawLine(
                color = PreviewYellow.copy(alpha = 0.5f),
                start = Offset(last.x, last.y),
                end = Offset(first.x, first.y),
                strokeWidth = 1.5f,
                pathEffect = PreviewDashedStroke
            )
        }
    }
}

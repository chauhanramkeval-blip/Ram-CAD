package com.example.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadEntity
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.ui.theme.CadBorderDark
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadDimensionYellow
import com.example.ui.theme.CadSnapGreen
import com.example.ui.theme.CadSurfaceDark
import com.example.ui.theme.CadSurfaceVariantDark
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive CAD Properties Panel.
 *
 * Displays live, editable geometric properties for the currently selected entity:
 * - Line: Start X/Y, End X/Y, Length, Layer
 * - Circle: Center X/Y, Radius, Layer
 * - Arc: Center X/Y, Radius, Start Angle, End Angle, Layer
 * - Polyline: Vertex Count, Coordinates (per-vertex X/Y), Closed/Open, Layer
 * - Text: Text content, Position X/Y, Height, Rotation, Layer
 *
 * All numeric inputs allow safe editing (handling typing transitions without crashes)
 * and update the CAD document and canvas immediately.
 */
@Composable
fun CadPropertiesPanel(
    entity: CadEntity,
    layers: List<CadLayer>,
    onDelete: () -> Unit,
    onUpdateEntity: (CadEntity) -> Unit,
    onUpdateText: (String) -> Unit = { newText ->
        if (entity is CadEntity.Text) {
            onUpdateEntity(entity.copy(text = newText))
        }
    },
    onUpdateLayer: (String) -> Unit = { newLayerId ->
        val updated = when (entity) {
            is CadEntity.Line -> entity.copy(layerId = newLayerId)
            is CadEntity.Circle -> entity.copy(layerId = newLayerId)
            is CadEntity.Arc -> entity.copy(layerId = newLayerId)
            is CadEntity.Polyline -> entity.copy(layerId = newLayerId)
            is CadEntity.Text -> entity.copy(layerId = newLayerId)
            is CadEntity.Point -> entity.copy(layerId = newLayerId)
            is CadEntity.Dimension -> entity.copy(layerId = newLayerId)
            is CadEntity.Leader -> entity.copy(layerId = newLayerId)
            is CadEntity.Arrow -> entity.copy(layerId = newLayerId)
            is CadEntity.RevisionCloud -> entity.copy(layerId = newLayerId)
        }
        onUpdateEntity(updated)
    },
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .testTag("cad_properties_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFA141923)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CadBorderDark)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CadCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Properties",
                            tint = CadCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Properties",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = when (entity) {
                                is CadEntity.Line -> "Line Segment"
                                is CadEntity.Circle -> "Circle"
                                is CadEntity.Arc -> "Circular Arc"
                                is CadEntity.Polyline -> if (entity.isClosed) "Closed Polygon" else "Open Polyline"
                                is CadEntity.Text -> if (entity.isMultiLine) "Multi-line Text (MText)" else "Single-line Text"
                                is CadEntity.Point -> "Point Marker"
                                is CadEntity.Dimension -> "Dimension Annotation"
                                is CadEntity.Leader -> "Leader Note"
                                is CadEntity.Arrow -> "Arrow Annotation"
                                is CadEntity.RevisionCloud -> "Revision Cloud (Markup)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = CadCyan
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp).testTag("btn_close_properties")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Properties",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CadBorderDark.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Object-Specific Editable Properties
            when (entity) {
                is CadEntity.Line -> {
                    LinePropertiesSection(
                        line = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Circle -> {
                    CirclePropertiesSection(
                        circle = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Arc -> {
                    ArcPropertiesSection(
                        arc = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Polyline -> {
                    PolylinePropertiesSection(
                        polyline = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Text -> {
                    TextPropertiesSection(
                        textEntity = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Point -> {
                    PointPropertiesSection(
                        point = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Dimension -> {
                    DimensionPropertiesSection(
                        dimension = entity
                    )
                }
                is CadEntity.Leader -> {
                    LeaderPropertiesSection(
                        leader = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.Arrow -> {
                    ArrowPropertiesSection(
                        arrow = entity,
                        onUpdate = onUpdateEntity
                    )
                }
                is CadEntity.RevisionCloud -> {
                    RevisionCloudPropertiesSection(
                        cloud = entity,
                        onUpdate = onUpdateEntity
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CadBorderDark.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Editable Layer Assignment Section
            Text(
                text = "Layer",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                layers.forEach { layer ->
                    val isSelected = entity.layerId == layer.id
                    val isLocked = layer.isLocked
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) CadCyan.copy(alpha = 0.25f)
                                else if (isLocked) Color(0x33262626)
                                else CadSurfaceVariantDark
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) CadCyan else CadBorderDark,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isLocked) {
                                onUpdateLayer(layer.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("prop_layer_${layer.name}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(layer.colorArgb))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = layer.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSelected) CadCyan else if (isLocked) Color.Gray else Color.LightGray
                            )
                            if (isLocked) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Layer Locked",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Delete Object Action Button
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_delete_selected_entity")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Entity",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Entity", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Line Properties Editor:
 * - Start X/Y
 * - End X/Y
 * - Length (editable: scales end along vector)
 */
@Composable
private fun LinePropertiesSection(
    line: CadEntity.Line,
    onUpdate: (CadEntity) -> Unit
) {
    var startXText by remember(line.id, line.start.x) { mutableStateOf(formatCoord(line.start.x)) }
    var startYText by remember(line.id, line.start.y) { mutableStateOf(formatCoord(line.start.y)) }
    var endXText by remember(line.id, line.end.x) { mutableStateOf(formatCoord(line.end.x)) }
    var endYText by remember(line.id, line.end.y) { mutableStateOf(formatCoord(line.end.y)) }

    val currentLength = line.start.distanceTo(line.end)
    var lengthText by remember(line.id, currentLength) { mutableStateOf(formatCoord(currentLength)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Geometry",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )

        // Start Coordinates X / Y
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Start X",
                value = startXText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_line_start_x"),
                onValueChange = { str, num ->
                    startXText = str
                    if (num != null) {
                        onUpdate(line.copy(start = line.start.copy(x = num)))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Start Y",
                value = startYText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_line_start_y"),
                onValueChange = { str, num ->
                    startYText = str
                    if (num != null) {
                        onUpdate(line.copy(start = line.start.copy(y = num)))
                    }
                }
            )
        }

        // End Coordinates X / Y
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "End X",
                value = endXText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_line_end_x"),
                onValueChange = { str, num ->
                    endXText = str
                    if (num != null) {
                        onUpdate(line.copy(end = line.end.copy(x = num)))
                    }
                }
            )
            CadPropertyNumericField(
                label = "End Y",
                value = endYText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_line_end_y"),
                onValueChange = { str, num ->
                    endYText = str
                    if (num != null) {
                        onUpdate(line.copy(end = line.end.copy(y = num)))
                    }
                }
            )
        }

        // Length (editable)
        CadPropertyNumericField(
            label = "Length",
            value = lengthText,
            suffix = "mm",
            modifier = Modifier.fillMaxWidth().testTag("prop_line_length"),
            onValueChange = { str, newLength ->
                lengthText = str
                if (newLength != null && newLength > 0.001f) {
                    val dx = line.end.x - line.start.x
                    val dy = line.end.y - line.start.y
                    val len = sqrt(dx * dx + dy * dy)
                    val (ux, uy) = if (len > 0.0001f) {
                        Pair(dx / len, dy / len)
                    } else {
                        Pair(1.0f, 0.0f)
                    }
                    val newEnd = CadPoint2D(line.start.x + ux * newLength, line.start.y + uy * newLength)
                    onUpdate(line.copy(end = newEnd))
                }
            }
        )
    }
}

/**
 * Circle Properties Editor:
 * - Center X/Y
 * - Radius
 */
@Composable
private fun CirclePropertiesSection(
    circle: CadEntity.Circle,
    onUpdate: (CadEntity) -> Unit
) {
    var centerXText by remember(circle.id, circle.center.x) { mutableStateOf(formatCoord(circle.center.x)) }
    var centerYText by remember(circle.id, circle.center.y) { mutableStateOf(formatCoord(circle.center.y)) }
    var radiusText by remember(circle.id, circle.radius) { mutableStateOf(formatCoord(circle.radius)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Geometry",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )

        // Center Coordinates X / Y
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Center X",
                value = centerXText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_circle_center_x"),
                onValueChange = { str, num ->
                    centerXText = str
                    if (num != null) {
                        onUpdate(circle.copy(center = circle.center.copy(x = num)))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Center Y",
                value = centerYText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_circle_center_y"),
                onValueChange = { str, num ->
                    centerYText = str
                    if (num != null) {
                        onUpdate(circle.copy(center = circle.center.copy(y = num)))
                    }
                }
            )
        }

        // Radius (editable, positive)
        CadPropertyNumericField(
            label = "Radius",
            value = radiusText,
            suffix = "mm",
            modifier = Modifier.fillMaxWidth().testTag("prop_circle_radius"),
            onValueChange = { str, num ->
                radiusText = str
                if (num != null && num > 0.001f) {
                    onUpdate(circle.copy(radius = num))
                }
            }
        )

        // Read-only reference metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PropertyItemRow("Diameter", "%.2f mm".format(circle.radius * 2f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PropertyItemRow("Circumference", "%.2f mm".format(2 * Math.PI * circle.radius))
        }
    }
}

/**
 * Arc Properties Editor:
 * - Center X/Y
 * - Radius
 * - Start angle
 * - End angle
 */
@Composable
private fun ArcPropertiesSection(
    arc: CadEntity.Arc,
    onUpdate: (CadEntity) -> Unit
) {
    var centerXText by remember(arc.id, arc.center.x) { mutableStateOf(formatCoord(arc.center.x)) }
    var centerYText by remember(arc.id, arc.center.y) { mutableStateOf(formatCoord(arc.center.y)) }
    var radiusText by remember(arc.id, arc.radius) { mutableStateOf(formatCoord(arc.radius)) }

    var startAngleText by remember(arc.id, arc.startAngleDeg) { mutableStateOf(formatCoord(arc.startAngleDeg)) }
    val endAngleCalc = (arc.startAngleDeg + arc.sweepAngleDeg)
    var endAngleText by remember(arc.id, endAngleCalc) { mutableStateOf(formatCoord(endAngleCalc)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Geometry",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )

        // Center Coordinates X / Y
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Center X",
                value = centerXText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_arc_center_x"),
                onValueChange = { str, num ->
                    centerXText = str
                    if (num != null) {
                        onUpdate(arc.copy(center = arc.center.copy(x = num)))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Center Y",
                value = centerYText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_arc_center_y"),
                onValueChange = { str, num ->
                    centerYText = str
                    if (num != null) {
                        onUpdate(arc.copy(center = arc.center.copy(y = num)))
                    }
                }
            )
        }

        // Radius
        CadPropertyNumericField(
            label = "Radius",
            value = radiusText,
            suffix = "mm",
            modifier = Modifier.fillMaxWidth().testTag("prop_arc_radius"),
            onValueChange = { str, num ->
                radiusText = str
                if (num != null && num > 0.001f) {
                    onUpdate(arc.copy(radius = num))
                }
            }
        )

        // Start Angle / End Angle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Start Angle",
                value = startAngleText,
                suffix = "°",
                modifier = Modifier.weight(1f).testTag("prop_arc_start_angle"),
                onValueChange = { str, newStart ->
                    startAngleText = str
                    if (newStart != null) {
                        val currentEnd = arc.startAngleDeg + arc.sweepAngleDeg
                        val newSweep = currentEnd - newStart
                        onUpdate(arc.copy(startAngleDeg = newStart, sweepAngleDeg = newSweep))
                    }
                }
            )
            CadPropertyNumericField(
                label = "End Angle",
                value = endAngleText,
                suffix = "°",
                modifier = Modifier.weight(1f).testTag("prop_arc_end_angle"),
                onValueChange = { str, newEnd ->
                    endAngleText = str
                    if (newEnd != null) {
                        val newSweep = newEnd - arc.startAngleDeg
                        onUpdate(arc.copy(sweepAngleDeg = newSweep))
                    }
                }
            )
        }

        // Sweep Angle Info
        PropertyItemRow("Sweep Angle", "%.1f°".format(arc.sweepAngleDeg))
    }
}

/**
 * Polyline Properties Editor:
 * - Vertex count
 * - Coordinates (per-vertex X/Y coordinates)
 * - Closed/open
 */
@Composable
private fun PolylinePropertiesSection(
    polyline: CadEntity.Polyline,
    onUpdate: (CadEntity) -> Unit
) {
    var expandedVertices by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Geometry",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CadCyan
            )
            Text(
                text = "${polyline.points.size} vertices",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = CadDimensionYellow
            )
        }

        // Closed / Open Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CadSurfaceVariantDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (polyline.isClosed) "Closed Polygon" else "Open Polyline",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = if (polyline.isClosed) "Connects end to start vertex" else "Endpoints remain unjoined",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = polyline.isClosed,
                onCheckedChange = { isClosed ->
                    onUpdate(polyline.copy(isClosed = isClosed))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CadCyan,
                    checkedTrackColor = CadCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF262626)
                ),
                modifier = Modifier.testTag("prop_polyline_closed_switch")
            )
        }

        // Expandable Vertex Coordinates List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedVertices = !expandedVertices }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vertex Coordinates",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.LightGray
            )
            Icon(
                imageVector = if (expandedVertices) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = "Toggle Vertices",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = expandedVertices) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                polyline.points.forEachIndexed { index, pt ->
                    var vxText by remember(polyline.id, index, pt.x) { mutableStateOf(formatCoord(pt.x)) }
                    var vyText by remember(polyline.id, index, pt.y) { mutableStateOf(formatCoord(pt.y)) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = CadSnapGreen,
                            modifier = Modifier.width(26.dp)
                        )
                        CadPropertyNumericField(
                            label = "X",
                            value = vxText,
                            suffix = "mm",
                            modifier = Modifier.weight(1f).testTag("prop_vertex_${index}_x"),
                            onValueChange = { str, num ->
                                vxText = str
                                if (num != null) {
                                    val newPoints = polyline.points.toMutableList()
                                    newPoints[index] = pt.copy(x = num)
                                    onUpdate(polyline.copy(points = newPoints))
                                }
                            }
                        )
                        CadPropertyNumericField(
                            label = "Y",
                            value = vyText,
                            suffix = "mm",
                            modifier = Modifier.weight(1f).testTag("prop_vertex_${index}_y"),
                            onValueChange = { str, num ->
                                vyText = str
                                if (num != null) {
                                    val newPoints = polyline.points.toMutableList()
                                    newPoints[index] = pt.copy(y = num)
                                    onUpdate(polyline.copy(points = newPoints))
                                }
                            }
                        )
                        if (polyline.points.size > 2) {
                            IconButton(
                                onClick = {
                                    val newPoints = polyline.points.toMutableList()
                                    newPoints.removeAt(index)
                                    onUpdate(polyline.copy(points = newPoints))
                                },
                                modifier = Modifier.size(28.dp).testTag("btn_remove_vertex_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove Vertex",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Add Vertex Button
                Button(
                    onClick = {
                        val lastPt = polyline.points.lastOrNull() ?: CadPoint2D(0f, 0f)
                        val newPt = CadPoint2D(lastPt.x + 50f, lastPt.y)
                        onUpdate(polyline.copy(points = polyline.points + newPt))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CadSurfaceVariantDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_add_vertex")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Vertex",
                        tint = CadCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Vertex", color = CadCyan, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Text Properties Editor:
 * - Text content
 * - Position X/Y
 * - Height
 * - Rotation
 */
@Composable
private fun TextPropertiesSection(
    textEntity: CadEntity.Text,
    onUpdate: (CadEntity) -> Unit
) {
    var contentText by remember(textEntity.id, textEntity.text) { mutableStateOf(textEntity.text) }
    var posXText by remember(textEntity.id, textEntity.position.x) { mutableStateOf(formatCoord(textEntity.position.x)) }
    var posYText by remember(textEntity.id, textEntity.position.y) { mutableStateOf(formatCoord(textEntity.position.y)) }
    var heightText by remember(textEntity.id, textEntity.textHeight) { mutableStateOf(formatCoord(textEntity.textHeight)) }
    var rotationText by remember(textEntity.id, textEntity.rotationDeg) { mutableStateOf(formatCoord(textEntity.rotationDeg)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Text Content",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )

        // Text string input
        OutlinedTextField(
            value = contentText,
            onValueChange = {
                contentText = it
                onUpdate(textEntity.copy(text = it))
            },
            label = { Text("Content") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CadCyan,
                unfocusedBorderColor = CadBorderDark
            ),
            singleLine = false,
            modifier = Modifier.fillMaxWidth().testTag("prop_text_content")
        )

        Text(
            text = "Geometry & Sizing",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )

        // Position X / Y
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Position X",
                value = posXText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_text_pos_x"),
                onValueChange = { str, num ->
                    posXText = str
                    if (num != null) {
                        onUpdate(textEntity.copy(position = textEntity.position.copy(x = num)))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Position Y",
                value = posYText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_text_pos_y"),
                onValueChange = { str, num ->
                    posYText = str
                    if (num != null) {
                        onUpdate(textEntity.copy(position = textEntity.position.copy(y = num)))
                    }
                }
            )
        }

        // Height & Rotation
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Height",
                value = heightText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_text_height"),
                onValueChange = { str, num ->
                    heightText = str
                    if (num != null && num > 0.1f) {
                        onUpdate(textEntity.copy(textHeight = num))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Rotation",
                value = rotationText,
                suffix = "°",
                modifier = Modifier.weight(1f).testTag("prop_text_rotation"),
                onValueChange = { str, num ->
                    rotationText = str
                    if (num != null) {
                        onUpdate(textEntity.copy(rotationDeg = num))
                    }
                }
            )
        }

        // Quick rotation presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0f, 90f, 180f, 270f).forEach { angle ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CadSurfaceVariantDark)
                        .border(1.dp, CadBorderDark, RoundedCornerShape(6.dp))
                        .clickable {
                            rotationText = formatCoord(angle)
                            onUpdate(textEntity.copy(rotationDeg = angle))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "%.0f°".format(angle),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (abs(textEntity.rotationDeg - angle) < 0.1f) CadCyan else Color.LightGray
                    )
                }
            }
        }
    }
}

/**
 * Point Properties Editor:
 * - Position X/Y
 */
@Composable
private fun PointPropertiesSection(
    point: CadEntity.Point,
    onUpdate: (CadEntity) -> Unit
) {
    var posXText by remember(point.id, point.position.x) { mutableStateOf(formatCoord(point.position.x)) }
    var posYText by remember(point.id, point.position.y) { mutableStateOf(formatCoord(point.position.y)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Geometry",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Position X",
                value = posXText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_point_pos_x"),
                onValueChange = { str, num ->
                    posXText = str
                    if (num != null) {
                        onUpdate(point.copy(position = point.position.copy(x = num)))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Position Y",
                value = posYText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_point_pos_y"),
                onValueChange = { str, num ->
                    posYText = str
                    if (num != null) {
                        onUpdate(point.copy(position = point.position.copy(y = num)))
                    }
                }
            )
        }
    }
}

/**
 * Dimension Properties Readout
 */
@Composable
private fun DimensionPropertiesSection(
    dimension: CadEntity.Dimension
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Measurement",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )
        PropertyItemRow("Value", dimension.valueText)
        PropertyItemRow("Start", "(%.1f, %.1f)".format(dimension.start.x, dimension.start.y))
        PropertyItemRow("End", "(%.1f, %.1f)".format(dimension.end.x, dimension.end.y))
    }
}

/**
 * Leader Note Properties Editor:
 * - Content Text
 * - Text Height
 * - Arrow Size
 */
@Composable
private fun LeaderPropertiesSection(
    leader: CadEntity.Leader,
    onUpdate: (CadEntity) -> Unit
) {
    var contentText by remember(leader.id, leader.text) { mutableStateOf(leader.text) }
    var heightText by remember(leader.id, leader.textHeight) { mutableStateOf(formatCoord(leader.textHeight)) }
    var arrowSizeText by remember(leader.id, leader.arrowSize) { mutableStateOf(formatCoord(leader.arrowSize)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Leader Note",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )
        OutlinedTextField(
            value = contentText,
            onValueChange = {
                contentText = it
                onUpdate(leader.copy(text = it))
            },
            label = { Text("Note Text") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CadCyan,
                unfocusedBorderColor = CadBorderDark
            ),
            singleLine = false,
            modifier = Modifier.fillMaxWidth().testTag("prop_leader_text")
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CadPropertyNumericField(
                label = "Text Height",
                value = heightText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_leader_height"),
                onValueChange = { str, num ->
                    heightText = str
                    if (num != null && num > 0.5f) {
                        onUpdate(leader.copy(textHeight = num))
                    }
                }
            )
            CadPropertyNumericField(
                label = "Arrow Size",
                value = arrowSizeText,
                suffix = "mm",
                modifier = Modifier.weight(1f).testTag("prop_leader_arrow_size"),
                onValueChange = { str, num ->
                    arrowSizeText = str
                    if (num != null && num > 1f) {
                        onUpdate(leader.copy(arrowSize = num))
                    }
                }
            )
        }
        PropertyItemRow("Tip Coord", "(%.1f, %.1f)".format(leader.arrowPoint.x, leader.arrowPoint.y))
        PropertyItemRow("Knee Coord", "(%.1f, %.1f)".format(leader.kneePoint.x, leader.kneePoint.y))
    }
}

/**
 * Arrow Properties Editor
 */
@Composable
private fun ArrowPropertiesSection(
    arrow: CadEntity.Arrow,
    onUpdate: (CadEntity) -> Unit
) {
    var labelText by remember(arrow.id, arrow.label) { mutableStateOf(arrow.label ?: "") }
    var headSizeText by remember(arrow.id, arrow.headSize) { mutableStateOf(formatCoord(arrow.headSize)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Arrow Properties",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )
        OutlinedTextField(
            value = labelText,
            onValueChange = {
                labelText = it
                onUpdate(arrow.copy(label = it.takeIf { it.isNotBlank() }))
            },
            label = { Text("Label (Optional)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CadCyan,
                unfocusedBorderColor = CadBorderDark
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("prop_arrow_label")
        )
        CadPropertyNumericField(
            label = "Head Size",
            value = headSizeText,
            suffix = "mm",
            modifier = Modifier.fillMaxWidth().testTag("prop_arrow_head_size"),
            onValueChange = { str, num ->
                headSizeText = str
                if (num != null && num > 1f) {
                    onUpdate(arrow.copy(headSize = num))
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Double-Headed", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Switch(
                checked = arrow.isDoubleHeaded,
                onCheckedChange = { onUpdate(arrow.copy(isDoubleHeaded = it)) },
                modifier = Modifier.testTag("switch_double_headed"),
                colors = SwitchDefaults.colors(checkedThumbColor = CadCyan, checkedTrackColor = CadCyan.copy(alpha = 0.5f))
            )
        }
    }
}

/**
 * Revision Cloud Properties Editor
 */
@Composable
private fun RevisionCloudPropertiesSection(
    cloud: CadEntity.RevisionCloud,
    onUpdate: (CadEntity) -> Unit
) {
    var tagText by remember(cloud.id, cloud.revisionTag) { mutableStateOf(cloud.revisionTag ?: "") }
    var arcRadiusText by remember(cloud.id, cloud.arcRadius) { mutableStateOf(formatCoord(cloud.arcRadius)) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Revision Cloud (Markup)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = CadCyan
        )
        OutlinedTextField(
            value = tagText,
            onValueChange = {
                tagText = it
                onUpdate(cloud.copy(revisionTag = it.takeIf { it.isNotBlank() }))
            },
            label = { Text("Revision Tag (e.g. REV A, Δ 1)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CadCyan,
                unfocusedBorderColor = CadBorderDark
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("prop_cloud_tag")
        )
        CadPropertyNumericField(
            label = "Scallop Arc Radius",
            value = arcRadiusText,
            suffix = "mm",
            modifier = Modifier.fillMaxWidth().testTag("prop_cloud_radius"),
            onValueChange = { str, num ->
                arcRadiusText = str
                if (num != null && num > 2f) {
                    onUpdate(cloud.copy(arcRadius = num))
                }
            }
        )
        PropertyItemRow("Vertices Count", "${cloud.vertices.size}")
        PropertyItemRow("Boundary Status", if (cloud.isClosed) "Closed Bubble" else "Open Arc Path")
    }
}

/**
 * Reusable Numeric Input Field with safe parsing and immediate live update callback.
 * Prevents crashes during in-progress typing (e.g. "-", ".", empty while backspacing).
 */
@Composable
fun CadPropertyNumericField(
    label: String,
    value: String,
    suffix: String? = null,
    modifier: Modifier = Modifier,
    onValueChange: (rawText: String, parsedValue: Float?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isValid = value.isEmpty() || value == "-" || value == "." || value == "-." || value.toFloatOrNull() != null

    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Allow numbers, negative sign, and decimal point
            val sanitized = input.filter { it.isDigit() || it == '.' || it == '-' }
            val parsed = sanitized.toFloatOrNull()
            onValueChange(sanitized, parsed)
        },
        label = {
            Text(label, style = MaterialTheme.typography.labelSmall)
        },
        trailingIcon = suffix?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        isError = !isValid,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = CadCyan,
            unfocusedBorderColor = CadBorderDark,
            focusedLabelColor = CadCyan,
            unfocusedLabelColor = Color.LightGray,
            errorBorderColor = Color(0xFFEF4444)
        ),
        modifier = modifier
    )
}

/**
 * Standard Read-Only Property Key-Value Row.
 */
@Composable
fun PropertyItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White
        )
    }
}

private fun formatCoord(value: Float): String {
    return if (value % 1.0f == 0.0f) {
        "%.0f".format(Locale.US, value)
    } else {
        "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')
    }
}

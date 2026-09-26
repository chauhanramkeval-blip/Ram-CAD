package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CadCyan

@Composable
fun CadEmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Geometric Drafting Compass & Caliper Canvas graphic
        Canvas(modifier = Modifier.size(100.dp)) {
            val w = size.width
            val h = size.height

            // Outer technical dashed circle
            drawCircle(
                color = Color(0x3300E5FF),
                radius = w * 0.45f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 1.5f)
            )

            // Compass apex hinge
            drawCircle(
                color = CadCyan,
                radius = 7f,
                center = Offset(w * 0.5f, h * 0.25f)
            )

            // Left leg
            drawLine(
                color = CadCyan,
                start = Offset(w * 0.5f, h * 0.25f),
                end = Offset(w * 0.25f, h * 0.78f),
                strokeWidth = 3f
            )

            // Right leg
            drawLine(
                color = Color(0xFF2979FF),
                start = Offset(w * 0.5f, h * 0.25f),
                end = Offset(w * 0.75f, h * 0.78f),
                strokeWidth = 3f
            )

            // Measuring arc
            drawArc(
                color = Color.White,
                startAngle = 30f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(w * 0.3f, h * 0.38f),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, w * 0.4f),
                style = Stroke(width = 1.5f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CadCyan,
                    contentColor = Color(0xFF00363D)
                ),
                modifier = Modifier.testTag("empty_state_action_btn")
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

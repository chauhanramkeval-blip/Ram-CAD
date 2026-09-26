package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cad.model.CadFormat
import com.example.ui.theme.CadAlertRed
import com.example.ui.theme.CadBlue
import com.example.ui.theme.CadCyan
import com.example.ui.theme.CadOrthoOrange
import com.example.ui.theme.CadSnapGreen

@Composable
fun CadFormatChip(
    format: CadFormat,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (format) {
        CadFormat.DWG -> Triple(Color(0x222979FF), CadBlue, Color(0x662979FF))
        CadFormat.DXF -> Triple(Color(0x2200E5FF), CadCyan, Color(0x6600E5FF))
        CadFormat.DWF -> Triple(Color(0x2200E676), CadSnapGreen, Color(0x6600E676))
        CadFormat.STEP -> Triple(Color(0x22FF9100), CadOrthoOrange, Color(0x66FF9100))
        CadFormat.SVG -> Triple(Color(0x22FF5252), CadAlertRed, Color(0x66FF5252))
        CadFormat.CADPROJ -> Triple(Color(0x22FFB74D), Color(0xFFFFB74D), Color(0x66FFB74D))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = format.extension.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            ),
            color = textColor
        )
    }
}

@Composable
fun CadTagChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = color
        )
    }
}

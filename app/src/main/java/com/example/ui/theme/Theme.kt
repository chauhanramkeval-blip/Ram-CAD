package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Default to Dark Color Scheme for CAD precision drafting aesthetic
private val CadDarkColorScheme = darkColorScheme(
    primary = CadCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = CadCyanLight,
    secondary = CadBlue,
    onSecondary = Color.White,
    secondaryContainer = CadBlueDark,
    onSecondaryContainer = CadBlueLight,
    tertiary = CadDimensionYellow,
    onTertiary = Color(0xFF3B2F00),
    background = CadBackgroundDark,
    onBackground = CadTextPrimary,
    surface = CadSurfaceDark,
    onSurface = CadTextPrimary,
    surfaceVariant = CadSurfaceVariantDark,
    onSurfaceVariant = CadTextSecondary,
    outline = CadBorderDark,
    outlineVariant = CadGridLineDark
)

private val CadLightColorScheme = lightColorScheme(
    primary = CadBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = CadCyanDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEF5FF),
    onSecondaryContainer = Color(0xFF001F26),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    background = CadBackgroundLight,
    onBackground = CadTextPrimaryLight,
    surface = CadSurfaceLight,
    onSurface = CadTextPrimaryLight,
    surfaceVariant = CadSurfaceVariantLight,
    onSurfaceVariant = CadTextSecondaryLight,
    outline = CadBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // CAD apps are best suited with dark theme by default
    dynamicColor: Boolean = false, // Keep CAD branded precision palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CadDarkColorScheme
        else -> CadLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

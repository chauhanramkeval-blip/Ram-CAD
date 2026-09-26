package com.example.cad.engine.snap

/**
 * Settings configuration for CAD Object Snap (OSNAP).
 *
 * @property isEnabled Master toggle for Object Snapping.
 * @property enabledModes Set of currently enabled individual snap modes.
 * @property snapToleranceScreenPx Screen-space radius in pixels for snap capture (zoom-aware).
 * @property showMarker Whether to render geometric snap markers (square, triangle, etc.).
 * @property showTooltip Whether to display the snap mode label pill next to the marker.
 * @property showAperture Whether to render a visual aperture capture box around cursor.
 * @property apertureSizeScreenPx Size of the aperture capture box in screen pixels.
 */
data class CadOsnapSettings(
    val isEnabled: Boolean = true,
    val enabledModes: Set<CadSnapMode> = CadSnapMode.DEFAULT_MODES,
    val snapToleranceScreenPx: Float = 24f,
    val showMarker: Boolean = true,
    val showTooltip: Boolean = true,
    val showAperture: Boolean = false,
    val apertureSizeScreenPx: Float = 28f
) {
    fun isModeEnabled(mode: CadSnapMode): Boolean = isEnabled && enabledModes.contains(mode)

    fun withModeToggled(mode: CadSnapMode, enabled: Boolean): CadOsnapSettings {
        val newModes = if (enabled) enabledModes + mode else enabledModes - mode
        return copy(enabledModes = newModes)
    }

    fun withAllModes(enabled: Boolean): CadOsnapSettings {
        return copy(enabledModes = if (enabled) CadSnapMode.values().toSet() else emptySet())
    }
}

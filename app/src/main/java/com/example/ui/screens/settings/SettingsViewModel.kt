package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.example.cad.engine.CadEngine
import com.example.cad.model.CadUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val selectedUnit: CadUnit = CadUnit.MILLIMETERS,
    val coordinatePrecision: Int = 2,
    val gridSpacingMm: Float = 500f,
    val isAutoSnapEnabled: Boolean = true,
    val canvasBackgroundTheme: String = "AutoCAD Dark",
    val engineName: String = "",
    val engineVersion: String = "",
    val isNativeEngineAvailable: Boolean = false,
    val supportedFormats: List<String> = listOf("DWG", "DXF", "DWF", "STEP", "SVG")
)

class SettingsViewModel(
    private val cadEngine: CadEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            engineName = cadEngine.name,
            engineVersion = cadEngine.version,
            isNativeEngineAvailable = cadEngine.isNativeEngineAvailable
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setUnit(unit: CadUnit) {
        _uiState.value = _uiState.value.copy(selectedUnit = unit)
    }

    fun setCoordinatePrecision(decimals: Int) {
        _uiState.value = _uiState.value.copy(coordinatePrecision = decimals)
    }

    fun setGridSpacing(spacing: Float) {
        _uiState.value = _uiState.value.copy(gridSpacingMm = spacing)
    }

    fun setAutoSnap(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoSnapEnabled = enabled)
    }

    fun setCanvasBackground(theme: String) {
        _uiState.value = _uiState.value.copy(canvasBackgroundTheme = theme)
    }
}

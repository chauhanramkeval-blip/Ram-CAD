package com.example.cad.engine.api

import com.example.cad.model.CadDocument
import com.example.cad.model.CadFormat
import com.example.cad.model.CadUnit
import java.util.UUID

enum class CadEngineBackendType(val displayName: String, val isProductionReady: Boolean) {
    /** Pure-Kotlin baseline engine with integrated DXF parser */
    KOTLIN_BASELINE("Kotlin Baseline Engine (DXF)", true),

    /** Native C++ engine backed by commercial/licensed ODA SDK */
    NATIVE_ODA_SDK("Open Design Alliance (ODA) Native SDK", true),

    /** Explicit mock implementation used for unit testing and offline development */
    MOCK_TESTING("Mock Engine (Development & Testing Only)", false)
}

/**
 * Handle representing an actively opened CAD drawing session in the engine.
 *
 * Encapsulates native pointers (e.g. OdDbDatabase*) or pure-Kotlin memory state
 * without exposing internal SDK implementation details to the UI layer.
 */
data class CadDrawingSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val filePath: String,
    val title: String,
    val format: CadFormat,
    val units: CadUnit = CadUnit.MILLIMETERS,
    val isReadOnly: Boolean = false,
    val isDirty: Boolean = false,
    val nativeHandle: Long = 0L,
    val engineBackend: CadEngineBackendType = CadEngineBackendType.KOTLIN_BASELINE,
    val documentState: CadDocument
) {
    val isValid: Boolean get() = sessionId.isNotBlank()
}

package com.example.cad.engine.api

import com.example.cad.model.CadUnit

/**
 * Configuration options for opening a CAD drawing.
 */
data class CadOpenOptions(
    val readOnly: Boolean = false,
    val password: String? = null,
    val auditAndRecover: Boolean = false,
    val targetUnits: CadUnit? = null,
    val loadPartial: Boolean = false,
    val spatialFilter: List<Float>? = null
)

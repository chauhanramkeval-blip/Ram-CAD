package com.example.cad.engine.snap

import com.example.cad.model.CadPoint2D

/**
 * Result representing an active Object Snap candidate.
 *
 * @property point Snapped 2D coordinate in CAD world space.
 * @property mode The geometric snap mode detected.
 * @property screenPoint Position of the snap marker on the screen in pixels.
 * @property primaryEntityId Identifier of the entity being snapped to.
 * @property secondaryEntityId Identifier of the second entity (for [CadSnapMode.INTERSECTION]).
 * @property distanceScreenPx Distance in screen pixels between raw cursor and snap marker.
 * @property description Human-readable label displayed in the snap marker tooltip.
 */
data class CadSnapResult(
    val point: CadPoint2D,
    val mode: CadSnapMode,
    val screenPoint: CadPoint2D,
    val primaryEntityId: String,
    val secondaryEntityId: String? = null,
    val distanceScreenPx: Float = 0f,
    val description: String = mode.displayName
)

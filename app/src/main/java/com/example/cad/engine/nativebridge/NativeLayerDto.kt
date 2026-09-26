package com.example.cad.engine.nativebridge

import com.example.cad.model.CadLayer

/**
 * DTO for layer definition crossing the native JNI boundary.
 */
data class NativeLayerDto(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val isVisible: Boolean,
    val isLocked: Boolean,
    val lineWeight: Float
) {
    fun toCadLayer(): CadLayer {
        return CadLayer(
            id = id,
            name = name,
            colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
            isVisible = isVisible,
            isLocked = isLocked,
            lineWeight = lineWeight
        )
    }

    companion object {
        fun fromCadLayer(layer: CadLayer): NativeLayerDto {
            return NativeLayerDto(
                id = layer.id,
                name = layer.name,
                colorArgb = layer.colorArgb.toInt(),
                isVisible = layer.isVisible,
                isLocked = layer.isLocked,
                lineWeight = layer.lineWeight
            )
        }
    }
}

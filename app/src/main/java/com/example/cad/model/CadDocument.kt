package com.example.cad.model

import com.example.cad.engine.spatial.CadSpatialIndex

/**
 * Representation of an active CAD Drawing Document in memory.
 */
data class CadDocument(
    val title: String,
    val format: CadFormat,
    val units: CadUnit = CadUnit.MILLIMETERS,
    val layers: Map<String, CadLayer> = mapOf(
        CadLayer.DEFAULT_LAYER_0.id to CadLayer.DEFAULT_LAYER_0,
        CadLayer.ANNOTATION_LAYER.id to CadLayer.ANNOTATION_LAYER,
        CadLayer.HIDDEN_LAYER.id to CadLayer.HIDDEN_LAYER,
        CadLayer.CENTER_LAYER.id to CadLayer.CENTER_LAYER,
        CadLayer.MARKUP_LAYER.id to CadLayer.MARKUP_LAYER
    ),
    val entities: List<CadEntity> = emptyList(),
    val extents: CadBoundingBox = CadBoundingBox.EMPTY,
    val isReadOnly: Boolean = false
) {
    val entityCount: Int get() = entities.size
    val layerCount: Int get() = layers.size

    val spatialIndex: CadSpatialIndex by lazy {
        CadSpatialIndex.build(entities)
    }

    val entityMap: Map<String, CadEntity> by lazy {
        entities.associateBy { it.id }
    }

    fun findEntity(id: String): CadEntity? = entityMap[id]

    fun computeExtents(): CadBoundingBox {
        if (entities.isEmpty()) {
            return CadBoundingBox(minX = -100f, minY = -100f, maxX = 100f, maxY = 100f)
        }

        val allLayersVisible = layers.values.all { it.isVisible }
        if (allLayersVisible) {
            val rootBounds = spatialIndex.bounds
            return if (rootBounds.isEmpty) {
                CadBoundingBox(minX = -100f, minY = -100f, maxX = 100f, maxY = 100f)
            } else {
                rootBounds
            }
        }

        var box = CadBoundingBox()
        for (entity in entities) {
            val layer = layers[entity.layerId]
            if (layer?.isVisible != false) {
                box = box.include(entity.boundingBox)
            }
        }
        return if (box.isEmpty) {
            CadBoundingBox(minX = -100f, minY = -100f, maxX = 100f, maxY = 100f)
        } else {
            box
        }
    }
}

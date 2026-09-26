package com.example.cad.engine.snap

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadEntity
import kotlin.math.floor

/**
 * High-performance 2D spatial grid index for CAD entities.
 *
 * Ensures smooth 60fps cursor movement and snapping on large drawings (thousands of entities)
 * by culling non-candidate entities in O(1) grid cell lookups rather than O(N) full document scans.
 */
class CadSnapSpatialIndex(
    private val cellSize: Float = 150f
) {
    private val grid = HashMap<Long, MutableList<CadEntity>>()
    private var indexedEntities: List<CadEntity> = emptyList()
    private var lastEntityCount: Int = -1

    /**
     * Rebuilds the spatial index if the entity list has changed.
     */
    fun sync(entities: List<CadEntity>) {
        if (entities === indexedEntities && entities.size == lastEntityCount) {
            return
        }
        grid.clear()
        indexedEntities = entities
        lastEntityCount = entities.size

        for (entity in entities) {
            val bounds = entity.boundingBox
            if (bounds.isEmpty) continue

            val minCellX = floor(bounds.minX / cellSize).toInt()
            val maxCellX = floor(bounds.maxX / cellSize).toInt()
            val minCellY = floor(bounds.minY / cellSize).toInt()
            val maxCellY = floor(bounds.maxY / cellSize).toInt()

            // Cap the cell span to prevent pathological bounding boxes from flooding the grid
            val clampedMaxCellX = minOf(maxCellX, minCellX + 15)
            val clampedMaxCellY = minOf(maxCellY, minCellY + 15)

            for (cx in minCellX..clampedMaxCellX) {
                for (cy in minCellY..clampedMaxCellY) {
                    val key = cellKey(cx, cy)
                    grid.getOrPut(key) { ArrayList() }.add(entity)
                }
            }
        }
    }

    /**
     * Queries all candidate entities overlapping [queryBox].
     */
    fun query(queryBox: CadBoundingBox): List<CadEntity> {
        if (queryBox.isEmpty) return emptyList()

        if (indexedEntities.size <= 30) {
            // For small drawings, direct bounding box intersection is faster than hash lookups
            return indexedEntities.filter { it.boundingBox.intersects(queryBox) }
        }

        val minCellX = floor(queryBox.minX / cellSize).toInt()
        val maxCellX = floor(queryBox.maxX / cellSize).toInt()
        val minCellY = floor(queryBox.minY / cellSize).toInt()
        val maxCellY = floor(queryBox.maxY / cellSize).toInt()

        val resultSet = LinkedHashSet<CadEntity>()
        for (cx in minCellX..maxCellX) {
            for (cy in minCellY..maxCellY) {
                val key = cellKey(cx, cy)
                grid[key]?.let { bucket ->
                    for (entity in bucket) {
                        if (entity.boundingBox.intersects(queryBox)) {
                            resultSet.add(entity)
                        }
                    }
                }
            }
        }
        return resultSet.toList()
    }

    private fun cellKey(cx: Int, cy: Int): Long {
        return (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
    }
}

package com.example.cad.engine.nativebridge

import com.example.cad.model.CadEntity
import com.example.cad.model.CadPoint2D

/**
 * Lightweight Data Transfer Object (DTO) for transferring CAD entity geometry across
 * the JNI (Java Native Interface) boundary between C++ (ODA / Teigha) and Kotlin.
 */
data class NativeEntityDto(
    val id: String,
    val type: Int, // 1=LINE, 2=POLYLINE, 3=CIRCLE, 4=ARC, 5=TEXT, 6=DIMENSION
    val layerName: String,
    val colorArgb: Int,
    val strokeWidth: Float,
    val coordinates: FloatArray, // Packed coordinates: [x1, y1, x2, y2, ...] or [cx, cy, r, ...]
    val textContent: String? = null,
    val isClosed: Boolean = false,
    val rotationDeg: Float = 0f
) {
    companion object {
        const val TYPE_LINE = 1
        const val TYPE_POLYLINE = 2
        const val TYPE_CIRCLE = 3
        const val TYPE_ARC = 4
        const val TYPE_TEXT = 5
        const val TYPE_DIMENSION = 6
        const val TYPE_POINT = 7
        const val TYPE_LEADER = 8
        const val TYPE_ARROW = 9
        const val TYPE_REVISION_CLOUD = 10

        fun fromCadEntity(entity: CadEntity): NativeEntityDto {
            return when (entity) {
                is CadEntity.Line -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_LINE,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = entity.strokeWidth,
                    coordinates = floatArrayOf(entity.start.x, entity.start.y, entity.end.x, entity.end.y)
                )
                is CadEntity.Polyline -> {
                    val coords = FloatArray(entity.points.size * 2)
                    entity.points.forEachIndexed { i, pt ->
                        coords[i * 2] = pt.x
                        coords[i * 2 + 1] = pt.y
                    }
                    NativeEntityDto(
                        id = entity.id,
                        type = TYPE_POLYLINE,
                        layerName = entity.layerId,
                        colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                        strokeWidth = entity.strokeWidth,
                        coordinates = coords,
                        isClosed = entity.isClosed
                    )
                }
                is CadEntity.Circle -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_CIRCLE,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = entity.strokeWidth,
                    coordinates = floatArrayOf(entity.center.x, entity.center.y, entity.radius)
                )
                is CadEntity.Arc -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_ARC,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = entity.strokeWidth,
                    coordinates = floatArrayOf(
                        entity.center.x,
                        entity.center.y,
                        entity.radius,
                        entity.startAngleDeg,
                        entity.sweepAngleDeg
                    )
                )
                is CadEntity.Text -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_TEXT,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = 1f,
                    coordinates = floatArrayOf(entity.position.x, entity.position.y, entity.textHeight),
                    textContent = entity.text,
                    rotationDeg = entity.rotationDeg
                )
                is CadEntity.Dimension -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_DIMENSION,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = 1.2f,
                    coordinates = floatArrayOf(
                        entity.start.x, entity.start.y,
                        entity.end.x, entity.end.y,
                        entity.textPoint.x, entity.textPoint.y
                    ),
                    textContent = entity.valueText
                )
                is CadEntity.Point -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_POINT,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = 1f,
                    coordinates = floatArrayOf(entity.position.x, entity.position.y)
                )
                is CadEntity.Leader -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_LEADER,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = entity.strokeWidth,
                    coordinates = floatArrayOf(
                        entity.arrowPoint.x, entity.arrowPoint.y,
                        entity.kneePoint.x, entity.kneePoint.y,
                        entity.landingEndPoint.x, entity.landingEndPoint.y,
                        entity.textHeight, entity.arrowSize
                    ),
                    textContent = entity.text
                )
                is CadEntity.Arrow -> NativeEntityDto(
                    id = entity.id,
                    type = TYPE_ARROW,
                    layerName = entity.layerId,
                    colorArgb = (entity.colorArgb ?: 0xFFFFFFFF).toInt(),
                    strokeWidth = entity.strokeWidth,
                    coordinates = floatArrayOf(
                        entity.start.x, entity.start.y,
                        entity.end.x, entity.end.y,
                        entity.headSize
                    ),
                    isClosed = entity.isDoubleHeaded,
                    textContent = entity.label
                )
                is CadEntity.RevisionCloud -> {
                    val coords = FloatArray(2 + entity.vertices.size * 2)
                    coords[0] = entity.arcRadius
                    coords[1] = entity.strokeWidth
                    entity.vertices.forEachIndexed { i, pt ->
                        coords[2 + i * 2] = pt.x
                        coords[2 + i * 2 + 1] = pt.y
                    }
                    NativeEntityDto(
                        id = entity.id,
                        type = TYPE_REVISION_CLOUD,
                        layerName = entity.layerId,
                        colorArgb = (entity.colorArgb ?: 0xFFFF5722).toInt(),
                        strokeWidth = entity.strokeWidth,
                        coordinates = coords,
                        isClosed = entity.isClosed,
                        textContent = entity.revisionTag
                    )
                }
            }
        }
    }

    fun toCadEntity(): CadEntity? {
        return when (type) {
            TYPE_LINE -> {
                if (coordinates.size >= 4) {
                    CadEntity.Line(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        start = CadPoint2D(coordinates[0], coordinates[1]),
                        end = CadPoint2D(coordinates[2], coordinates[3]),
                        strokeWidth = strokeWidth
                    )
                } else null
            }
            TYPE_POLYLINE -> {
                val pts = mutableListOf<CadPoint2D>()
                for (i in 0 until coordinates.size step 2) {
                    if (i + 1 < coordinates.size) {
                        pts.add(CadPoint2D(coordinates[i], coordinates[i + 1]))
                    }
                }
                CadEntity.Polyline(
                    id = id,
                    layerId = layerName,
                    colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                    points = pts,
                    isClosed = isClosed,
                    strokeWidth = strokeWidth
                )
            }
            TYPE_CIRCLE -> {
                if (coordinates.size >= 3) {
                    CadEntity.Circle(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        center = CadPoint2D(coordinates[0], coordinates[1]),
                        radius = coordinates[2],
                        strokeWidth = strokeWidth
                    )
                } else null
            }
            TYPE_ARC -> {
                if (coordinates.size >= 5) {
                    CadEntity.Arc(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        center = CadPoint2D(coordinates[0], coordinates[1]),
                        radius = coordinates[2],
                        startAngleDeg = coordinates[3],
                        sweepAngleDeg = coordinates[4],
                        strokeWidth = strokeWidth
                    )
                } else null
            }
            TYPE_TEXT -> {
                if (coordinates.size >= 3) {
                    CadEntity.Text(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        position = CadPoint2D(coordinates[0], coordinates[1]),
                        textHeight = coordinates[2],
                        text = textContent ?: "",
                        rotationDeg = rotationDeg
                    )
                } else null
            }
            TYPE_DIMENSION -> {
                if (coordinates.size >= 6) {
                    CadEntity.Dimension(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        start = CadPoint2D(coordinates[0], coordinates[1]),
                        end = CadPoint2D(coordinates[2], coordinates[3]),
                        textPoint = CadPoint2D(coordinates[4], coordinates[5]),
                        valueText = textContent ?: ""
                    )
                } else null
            }
            TYPE_POINT -> {
                if (coordinates.size >= 2) {
                    CadEntity.Point(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        position = CadPoint2D(coordinates[0], coordinates[1])
                    )
                } else null
            }
            TYPE_LEADER -> {
                if (coordinates.size >= 6) {
                    CadEntity.Leader(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        arrowPoint = CadPoint2D(coordinates[0], coordinates[1]),
                        kneePoint = CadPoint2D(coordinates[2], coordinates[3]),
                        landingEndPoint = CadPoint2D(coordinates[4], coordinates[5]),
                        text = textContent ?: "",
                        textHeight = if (coordinates.size >= 7) coordinates[6] else 12f,
                        arrowSize = if (coordinates.size >= 8) coordinates[7] else 8f,
                        strokeWidth = strokeWidth
                    )
                } else null
            }
            TYPE_ARROW -> {
                if (coordinates.size >= 4) {
                    CadEntity.Arrow(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        start = CadPoint2D(coordinates[0], coordinates[1]),
                        end = CadPoint2D(coordinates[2], coordinates[3]),
                        headSize = if (coordinates.size >= 5) coordinates[4] else 10f,
                        strokeWidth = strokeWidth,
                        isDoubleHeaded = isClosed,
                        label = textContent
                    )
                } else null
            }
            TYPE_REVISION_CLOUD -> {
                if (coordinates.size >= 2) {
                    val radius = coordinates[0]
                    val sWidth = coordinates[1]
                    val pts = mutableListOf<CadPoint2D>()
                    for (i in 2 until coordinates.size step 2) {
                        if (i + 1 < coordinates.size) {
                            pts.add(CadPoint2D(coordinates[i], coordinates[i + 1]))
                        }
                    }
                    CadEntity.RevisionCloud(
                        id = id,
                        layerId = layerName,
                        colorArgb = colorArgb.toLong() and 0xFFFFFFFFL,
                        vertices = pts,
                        arcRadius = radius,
                        strokeWidth = sWidth,
                        isClosed = isClosed,
                        revisionTag = textContent
                    )
                } else null
            }
            else -> null
        }
    }
}

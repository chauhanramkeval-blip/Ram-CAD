package com.example.cad.io

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import org.json.JSONObject
import java.io.File
import java.io.InputStream

/**
 * Parser for the internal native CAD project format (.cadproj).
 * Reconstitutes full [CadDocument] hierarchy with all entity and layer fidelity.
 */
class InternalProjectParser {

    fun parse(file: File): Result<CadDocument> {
        return try {
            val content = file.readText()
            parseJson(content, file.name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parse(stream: InputStream, title: String): Result<CadDocument> {
        return try {
            val content = stream.bufferedReader().use { it.readText() }
            parseJson(content, title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseJson(jsonString: String, fallbackTitle: String): Result<CadDocument> {
        return try {
            val root = JSONObject(jsonString)
            val title = root.optString("title", fallbackTitle)
            val unitsStr = root.optString("units", "MILLIMETERS")
            val units = try { CadUnit.valueOf(unitsStr) } catch (_: Exception) { CadUnit.MILLIMETERS }

            // Parse layers
            val layersMap = mutableMapOf<String, CadLayer>()
            val layersArray = root.optJSONArray("layers")
            if (layersArray != null) {
                for (i in 0 until layersArray.length()) {
                    val lObj = layersArray.getJSONObject(i)
                    val id = lObj.getString("id")
                    val name = lObj.optString("name", id)
                    val colorArgb = lObj.optLong("colorArgb", 0xFFFFFFFF)
                    val isVisible = lObj.optBoolean("isVisible", true)
                    val isLocked = lObj.optBoolean("isLocked", false)
                    val lineWeight = lObj.optDouble("lineWeight", 1.5).toFloat()
                    val lineType = lObj.optString("lineType", "Continuous")

                    layersMap[id] = CadLayer(
                        id = id,
                        name = name,
                        colorArgb = colorArgb,
                        isVisible = isVisible,
                        isLocked = isLocked,
                        lineWeight = lineWeight,
                        lineType = lineType
                    )
                }
            }

            if (layersMap.isEmpty()) {
                layersMap[CadLayer.DEFAULT_LAYER_0.id] = CadLayer.DEFAULT_LAYER_0
            }

            // Parse entities
            val entitiesList = mutableListOf<CadEntity>()
            val entitiesArray = root.optJSONArray("entities")
            if (entitiesArray != null) {
                for (i in 0 until entitiesArray.length()) {
                    val eObj = entitiesArray.getJSONObject(i)
                    val id = eObj.getString("id")
                    val layerId = eObj.optString("layerId", "0")
                    val colorArgb = if (eObj.has("colorArgb")) eObj.getLong("colorArgb") else null
                    val type = eObj.getString("type")

                    val entity: CadEntity? = when (type) {
                        "LINE" -> CadEntity.Line(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            start = CadPoint2D(eObj.getDouble("startX").toFloat(), eObj.getDouble("startY").toFloat()),
                            end = CadPoint2D(eObj.getDouble("endX").toFloat(), eObj.getDouble("endY").toFloat()),
                            strokeWidth = eObj.optDouble("strokeWidth", 1.5).toFloat()
                        )

                        "POLYLINE" -> {
                            val pts = mutableListOf<CadPoint2D>()
                            val ptsArray = eObj.optJSONArray("points")
                            if (ptsArray != null) {
                                for (p in 0 until ptsArray.length()) {
                                    val ptObj = ptsArray.getJSONObject(p)
                                    pts.add(CadPoint2D(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                                }
                            }
                            CadEntity.Polyline(
                                id = id,
                                layerId = layerId,
                                colorArgb = colorArgb,
                                points = pts,
                                isClosed = eObj.optBoolean("isClosed", false),
                                strokeWidth = eObj.optDouble("strokeWidth", 1.5).toFloat()
                            )
                        }

                        "CIRCLE" -> CadEntity.Circle(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            center = CadPoint2D(eObj.getDouble("centerX").toFloat(), eObj.getDouble("centerY").toFloat()),
                            radius = eObj.getDouble("radius").toFloat(),
                            strokeWidth = eObj.optDouble("strokeWidth", 1.5).toFloat()
                        )

                        "ARC" -> CadEntity.Arc(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            center = CadPoint2D(eObj.getDouble("centerX").toFloat(), eObj.getDouble("centerY").toFloat()),
                            radius = eObj.getDouble("radius").toFloat(),
                            startAngleDeg = eObj.getDouble("startAngleDeg").toFloat(),
                            sweepAngleDeg = eObj.getDouble("sweepAngleDeg").toFloat(),
                            strokeWidth = eObj.optDouble("strokeWidth", 1.5).toFloat()
                        )

                        "TEXT" -> CadEntity.Text(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            position = CadPoint2D(eObj.getDouble("posX").toFloat(), eObj.getDouble("posY").toFloat()),
                            text = eObj.getString("text"),
                            textHeight = eObj.optDouble("textHeight", 12.0).toFloat(),
                            rotationDeg = eObj.optDouble("rotationDeg", 0.0).toFloat(),
                            isMultiLine = eObj.optBoolean("isMultiLine", false)
                        )

                        "LEADER" -> CadEntity.Leader(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            arrowPoint = CadPoint2D(eObj.getDouble("arrowX").toFloat(), eObj.getDouble("arrowY").toFloat()),
                            kneePoint = CadPoint2D(eObj.getDouble("kneeX").toFloat(), eObj.getDouble("kneeY").toFloat()),
                            landingEndPoint = CadPoint2D(eObj.getDouble("landingX").toFloat(), eObj.getDouble("landingY").toFloat()),
                            text = eObj.getString("text"),
                            textHeight = eObj.optDouble("textHeight", 12.0).toFloat(),
                            arrowSize = eObj.optDouble("arrowSize", 8.0).toFloat(),
                            strokeWidth = eObj.optDouble("strokeWidth", 1.8).toFloat()
                        )

                        "ARROW" -> CadEntity.Arrow(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            start = CadPoint2D(eObj.getDouble("startX").toFloat(), eObj.getDouble("startY").toFloat()),
                            end = CadPoint2D(eObj.getDouble("endX").toFloat(), eObj.getDouble("endY").toFloat()),
                            headSize = eObj.optDouble("headSize", 10.0).toFloat(),
                            strokeWidth = eObj.optDouble("strokeWidth", 2.0).toFloat()
                        )

                        "CLOUD" -> {
                            val vts = mutableListOf<CadPoint2D>()
                            val vtsArray = eObj.optJSONArray("vertices")
                            if (vtsArray != null) {
                                for (p in 0 until vtsArray.length()) {
                                    val ptObj = vtsArray.getJSONObject(p)
                                    vts.add(CadPoint2D(ptObj.getDouble("x").toFloat(), ptObj.getDouble("y").toFloat()))
                                }
                            }
                            CadEntity.RevisionCloud(
                                id = id,
                                layerId = layerId,
                                colorArgb = colorArgb ?: 0xFFFF5722,
                                vertices = vts,
                                arcRadius = eObj.optDouble("arcRadius", 16.0).toFloat(),
                                strokeWidth = eObj.optDouble("strokeWidth", 2.2).toFloat(),
                                isClosed = eObj.optBoolean("isClosed", true)
                            )
                        }

                        "POINT" -> CadEntity.Point(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb,
                            position = CadPoint2D(eObj.getDouble("x").toFloat(), eObj.getDouble("y").toFloat())
                        )

                        "DIMENSION" -> CadEntity.Dimension(
                            id = id,
                            layerId = layerId,
                            colorArgb = colorArgb ?: 0xFFFFD600,
                            start = CadPoint2D(eObj.getDouble("startX").toFloat(), eObj.getDouble("startY").toFloat()),
                            end = CadPoint2D(eObj.getDouble("endX").toFloat(), eObj.getDouble("endY").toFloat()),
                            textPoint = CadPoint2D(eObj.getDouble("textX").toFloat(), eObj.getDouble("textY").toFloat()),
                            valueText = eObj.getString("valueText")
                        )

                        else -> null
                    }

                    if (entity != null) {
                        entitiesList.add(entity)
                    }
                }
            }

            val doc = CadDocument(
                title = title,
                format = CadFormat.CADPROJ,
                units = units,
                layers = layersMap,
                entities = entitiesList
            )
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package com.example.cad.io

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Exporter for the internal native CAD project format (.cadproj).
 * Stores comprehensive CAD vector hierarchies, layers, units, and styling losslessly.
 */
class InternalProjectExporter : CadExporter {

    override val format: CadFormat = CadFormat.CADPROJ

    override suspend fun export(document: CadDocument, destination: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                if (destination.parentFile != null && !destination.parentFile!!.exists()) {
                    destination.parentFile!!.mkdirs()
                }

                val json = serializeToJson(document)
                destination.writeText(json.toString(2))
                Result.success(destination)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun serializeToJson(doc: CadDocument): JSONObject {
        val root = JSONObject()
        root.put("format", "cadproj")
        root.put("version", 1)
        root.put("title", doc.title)
        root.put("units", doc.units.name)
        root.put("savedTimestamp", System.currentTimeMillis())

        val extents = doc.computeExtents()
        val extentsObj = JSONObject()
        extentsObj.put("minX", extents.minX.toDouble())
        extentsObj.put("minY", extents.minY.toDouble())
        extentsObj.put("maxX", extents.maxX.toDouble())
        extentsObj.put("maxY", extents.maxY.toDouble())
        root.put("extents", extentsObj)

        // Layers
        val layersArray = JSONArray()
        for ((_, layer) in doc.layers) {
            val layerObj = JSONObject()
            layerObj.put("id", layer.id)
            layerObj.put("name", layer.name)
            layerObj.put("colorArgb", layer.colorArgb)
            layerObj.put("isVisible", layer.isVisible)
            layerObj.put("isLocked", layer.isLocked)
            layerObj.put("lineWeight", layer.lineWeight.toDouble())
            layerObj.put("lineType", layer.lineType)
            layersArray.put(layerObj)
        }
        root.put("layers", layersArray)

        // Entities
        val entitiesArray = JSONArray()
        for (entity in doc.entities) {
            val entityObj = JSONObject()
            entityObj.put("id", entity.id)
            entityObj.put("layerId", entity.layerId)
            entity.colorArgb?.let { entityObj.put("colorArgb", it) }

            when (entity) {
                is CadEntity.Line -> {
                    entityObj.put("type", "LINE")
                    entityObj.put("startX", entity.start.x.toDouble())
                    entityObj.put("startY", entity.start.y.toDouble())
                    entityObj.put("endX", entity.end.x.toDouble())
                    entityObj.put("endY", entity.end.y.toDouble())
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                }

                is CadEntity.Polyline -> {
                    entityObj.put("type", "POLYLINE")
                    entityObj.put("isClosed", entity.isClosed)
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                    val ptsArray = JSONArray()
                    for (pt in entity.points) {
                        val ptObj = JSONObject()
                        ptObj.put("x", pt.x.toDouble())
                        ptObj.put("y", pt.y.toDouble())
                        ptsArray.put(ptObj)
                    }
                    entityObj.put("points", ptsArray)
                }

                is CadEntity.Circle -> {
                    entityObj.put("type", "CIRCLE")
                    entityObj.put("centerX", entity.center.x.toDouble())
                    entityObj.put("centerY", entity.center.y.toDouble())
                    entityObj.put("radius", entity.radius.toDouble())
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                }

                is CadEntity.Arc -> {
                    entityObj.put("type", "ARC")
                    entityObj.put("centerX", entity.center.x.toDouble())
                    entityObj.put("centerY", entity.center.y.toDouble())
                    entityObj.put("radius", entity.radius.toDouble())
                    entityObj.put("startAngleDeg", entity.startAngleDeg.toDouble())
                    entityObj.put("sweepAngleDeg", entity.sweepAngleDeg.toDouble())
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                }

                is CadEntity.Text -> {
                    entityObj.put("type", "TEXT")
                    entityObj.put("posX", entity.position.x.toDouble())
                    entityObj.put("posY", entity.position.y.toDouble())
                    entityObj.put("text", entity.text)
                    entityObj.put("textHeight", entity.textHeight.toDouble())
                    entityObj.put("rotationDeg", entity.rotationDeg.toDouble())
                    entityObj.put("isMultiLine", entity.isMultiLine)
                }

                is CadEntity.Leader -> {
                    entityObj.put("type", "LEADER")
                    entityObj.put("arrowX", entity.arrowPoint.x.toDouble())
                    entityObj.put("arrowY", entity.arrowPoint.y.toDouble())
                    entityObj.put("kneeX", entity.kneePoint.x.toDouble())
                    entityObj.put("kneeY", entity.kneePoint.y.toDouble())
                    entityObj.put("landingX", entity.landingEndPoint.x.toDouble())
                    entityObj.put("landingY", entity.landingEndPoint.y.toDouble())
                    entityObj.put("text", entity.text)
                    entityObj.put("textHeight", entity.textHeight.toDouble())
                    entityObj.put("arrowSize", entity.arrowSize.toDouble())
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                }

                is CadEntity.Arrow -> {
                    entityObj.put("type", "ARROW")
                    entityObj.put("startX", entity.start.x.toDouble())
                    entityObj.put("startY", entity.start.y.toDouble())
                    entityObj.put("endX", entity.end.x.toDouble())
                    entityObj.put("endY", entity.end.y.toDouble())
                    entityObj.put("headSize", entity.headSize.toDouble())
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                }

                is CadEntity.RevisionCloud -> {
                    entityObj.put("type", "CLOUD")
                    entityObj.put("arcRadius", entity.arcRadius.toDouble())
                    entityObj.put("strokeWidth", entity.strokeWidth.toDouble())
                    entityObj.put("isClosed", entity.isClosed)
                    val ptsArray = JSONArray()
                    for (pt in entity.vertices) {
                        val ptObj = JSONObject()
                        ptObj.put("x", pt.x.toDouble())
                        ptObj.put("y", pt.y.toDouble())
                        ptsArray.put(ptObj)
                    }
                    entityObj.put("vertices", ptsArray)
                }

                is CadEntity.Point -> {
                    entityObj.put("type", "POINT")
                    entityObj.put("x", entity.position.x.toDouble())
                    entityObj.put("y", entity.position.y.toDouble())
                }

                is CadEntity.Dimension -> {
                    entityObj.put("type", "DIMENSION")
                    entityObj.put("startX", entity.start.x.toDouble())
                    entityObj.put("startY", entity.start.y.toDouble())
                    entityObj.put("endX", entity.end.x.toDouble())
                    entityObj.put("endY", entity.end.y.toDouble())
                    entityObj.put("textX", entity.textPoint.x.toDouble())
                    entityObj.put("textY", entity.textPoint.y.toDouble())
                    entityObj.put("valueText", entity.valueText)
                }
            }
            entitiesArray.put(entityObj)
        }
        root.put("entities", entitiesArray)

        return root
    }
}

package com.example.cad.io

import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.parser.dxf.DxfAciColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Locale

/**
 * Pure-Kotlin AutoCAD DXF ASCII Exporter.
 * Generates standards-compliant DXF files (AC1015 / AutoCAD 2000 format)
 * with HEADER, TABLES (LAYERS), and ENTITIES sections.
 */
class DxfExporter : CadExporter {

    override val format: CadFormat = CadFormat.DXF

    override suspend fun export(document: CadDocument, destination: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                if (destination.parentFile != null && !destination.parentFile!!.exists()) {
                    destination.parentFile!!.mkdirs()
                }

                BufferedWriter(FileWriter(destination)).use { writer ->
                    writeDxf(document, writer)
                }
                Result.success(destination)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun writeDxf(doc: CadDocument, writer: BufferedWriter) {
        fun writePair(code: Int, value: Any) {
            writer.write(code.toString())
            writer.newLine()
            writer.write(value.toString())
            writer.newLine()
        }

        val extents = doc.computeExtents()

        // 1. HEADER SECTION
        writePair(0, "SECTION")
        writePair(2, "HEADER")

        writePair(9, "\$ACADVER")
        writePair(1, "AC1015") // AutoCAD 2000 ASCII format

        writePair(9, "\$INSUNITS")
        writePair(70, doc.units.ordinal + 1) // 4 = Millimeters, 1 = Inches

        writePair(9, "\$EXTMIN")
        writePair(10, "%.4f".format(Locale.US, extents.minX))
        writePair(20, "%.4f".format(Locale.US, extents.minY))
        writePair(30, "0.0")

        writePair(9, "\$EXTMAX")
        writePair(10, "%.4f".format(Locale.US, extents.maxX))
        writePair(20, "%.4f".format(Locale.US, extents.maxY))
        writePair(30, "0.0")

        writePair(0, "ENDSEC")

        // 2. TABLES SECTION (Layers)
        writePair(0, "SECTION")
        writePair(2, "TABLES")

        writePair(0, "TABLE")
        writePair(2, "LAYER")
        writePair(70, doc.layers.size)

        for ((_, layer) in doc.layers) {
            writePair(0, "LAYER")
            writePair(2, layer.name)
            writePair(70, if (layer.isLocked) 4 else 0) // Flags: 4 = locked
            val aciColor = findClosestAciColor(layer.colorArgb)
            // In DXF, negative color indicates layer is OFF / hidden
            val signedColor = if (layer.isVisible) aciColor else -aciColor
            writePair(62, signedColor)
            writePair(6, layer.lineType)
        }

        writePair(0, "ENDTAB")
        writePair(0, "ENDSEC")

        // 3. BLOCKS SECTION
        writePair(0, "SECTION")
        writePair(2, "BLOCKS")
        writePair(0, "ENDSEC")

        // 4. ENTITIES SECTION
        writePair(0, "SECTION")
        writePair(2, "ENTITIES")

        for (entity in doc.entities) {
            val layerName = doc.layers[entity.layerId]?.name ?: "0"
            val entityAci = entity.colorArgb?.let { findClosestAciColor(it) }

            when (entity) {
                is CadEntity.Line -> {
                    writePair(0, "LINE")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(10, "%.4f".format(Locale.US, entity.start.x))
                    writePair(20, "%.4f".format(Locale.US, entity.start.y))
                    writePair(30, "0.0")
                    writePair(11, "%.4f".format(Locale.US, entity.end.x))
                    writePair(21, "%.4f".format(Locale.US, entity.end.y))
                    writePair(31, "0.0")
                }

                is CadEntity.Circle -> {
                    writePair(0, "CIRCLE")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(10, "%.4f".format(Locale.US, entity.center.x))
                    writePair(20, "%.4f".format(Locale.US, entity.center.y))
                    writePair(30, "0.0")
                    writePair(40, "%.4f".format(Locale.US, entity.radius))
                }

                is CadEntity.Arc -> {
                    writePair(0, "ARC")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(10, "%.4f".format(Locale.US, entity.center.x))
                    writePair(20, "%.4f".format(Locale.US, entity.center.y))
                    writePair(30, "0.0")
                    writePair(40, "%.4f".format(Locale.US, entity.radius))
                    writePair(50, "%.4f".format(Locale.US, entity.startAngleDeg))
                    val endAngle = (entity.startAngleDeg + entity.sweepAngleDeg) % 360f
                    writePair(51, "%.4f".format(Locale.US, endAngle))
                }

                is CadEntity.Polyline -> {
                    writePair(0, "LWPOLYLINE")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(90, entity.points.size)
                    writePair(70, if (entity.isClosed) 1 else 0)
                    for (pt in entity.points) {
                        writePair(10, "%.4f".format(Locale.US, pt.x))
                        writePair(20, "%.4f".format(Locale.US, pt.y))
                    }
                }

                is CadEntity.Text -> {
                    writePair(0, if (entity.isMultiLine) "MTEXT" else "TEXT")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(10, "%.4f".format(Locale.US, entity.position.x))
                    writePair(20, "%.4f".format(Locale.US, entity.position.y))
                    writePair(30, "0.0")
                    writePair(40, "%.4f".format(Locale.US, entity.textHeight))
                    writePair(1, entity.text)
                    if (entity.rotationDeg != 0f) {
                        writePair(50, "%.4f".format(Locale.US, entity.rotationDeg))
                    }
                }

                is CadEntity.Point -> {
                    writePair(0, "POINT")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(10, "%.4f".format(Locale.US, entity.position.x))
                    writePair(20, "%.4f".format(Locale.US, entity.position.y))
                    writePair(30, "0.0")
                }

                is CadEntity.Leader -> {
                    writePair(0, "LEADER")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(76, 3) // 3 vertices
                    writePair(10, "%.4f".format(Locale.US, entity.arrowPoint.x))
                    writePair(20, "%.4f".format(Locale.US, entity.arrowPoint.y))
                    writePair(10, "%.4f".format(Locale.US, entity.kneePoint.x))
                    writePair(20, "%.4f".format(Locale.US, entity.kneePoint.y))
                    writePair(10, "%.4f".format(Locale.US, entity.landingEndPoint.x))
                    writePair(20, "%.4f".format(Locale.US, entity.landingEndPoint.y))
                    // Also write accompanying text note
                    writePair(0, "TEXT")
                    writePair(8, layerName)
                    writePair(10, "%.4f".format(Locale.US, entity.landingEndPoint.x + 4f))
                    writePair(20, "%.4f".format(Locale.US, entity.landingEndPoint.y))
                    writePair(30, "0.0")
                    writePair(40, "%.4f".format(Locale.US, entity.textHeight))
                    writePair(1, entity.text)
                }

                is CadEntity.Arrow -> {
                    writePair(0, "LINE")
                    writePair(8, layerName)
                    if (entityAci != null) writePair(62, entityAci)
                    writePair(10, "%.4f".format(Locale.US, entity.start.x))
                    writePair(20, "%.4f".format(Locale.US, entity.start.y))
                    writePair(11, "%.4f".format(Locale.US, entity.end.x))
                    writePair(21, "%.4f".format(Locale.US, entity.end.y))
                }

                is CadEntity.RevisionCloud -> {
                    writePair(0, "LWPOLYLINE")
                    writePair(8, layerName)
                    writePair(62, 1) // Red for revision cloud
                    writePair(90, entity.vertices.size)
                    writePair(70, if (entity.isClosed) 1 else 0)
                    for (pt in entity.vertices) {
                        writePair(10, "%.4f".format(Locale.US, pt.x))
                        writePair(20, "%.4f".format(Locale.US, pt.y))
                    }
                }

                is CadEntity.Dimension -> {
                    writePair(0, "LINE")
                    writePair(8, layerName)
                    writePair(62, 2) // Yellow
                    writePair(10, "%.4f".format(Locale.US, entity.start.x))
                    writePair(20, "%.4f".format(Locale.US, entity.start.y))
                    writePair(11, "%.4f".format(Locale.US, entity.end.x))
                    writePair(21, "%.4f".format(Locale.US, entity.end.y))

                    writePair(0, "TEXT")
                    writePair(8, layerName)
                    writePair(62, 2)
                    writePair(10, "%.4f".format(Locale.US, entity.textPoint.x))
                    writePair(20, "%.4f".format(Locale.US, entity.textPoint.y))
                    writePair(30, "0.0")
                    writePair(40, "10.0")
                    writePair(1, entity.valueText)
                }
            }
        }

        writePair(0, "ENDSEC")
        writePair(0, "EOF")
    }

    private fun findClosestAciColor(argb: Long): Int {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()

        // Quick check against standard primary colors
        var bestAci = 7
        var minDiff = Int.MAX_VALUE

        for (i in 1..9) {
            val std = DxfAciColor.STANDARD_COLORS[i]
            val sr = ((std shr 16) and 0xFF).toInt()
            val sg = ((std shr 8) and 0xFF).toInt()
            val sb = (std and 0xFF).toInt()

            val diff = (r - sr) * (r - sr) + (g - sg) * (g - sg) + (b - sb) * (b - sb)
            if (diff < minDiff) {
                minDiff = diff
                bestAci = i
            }
        }
        return bestAci
    }
}

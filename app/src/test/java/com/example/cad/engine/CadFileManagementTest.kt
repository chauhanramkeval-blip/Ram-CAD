package com.example.cad.engine

import com.example.cad.io.DxfExporter
import com.example.cad.io.DwgSdkExporter
import com.example.cad.io.InternalProjectExporter
import com.example.cad.io.InternalProjectParser
import com.example.cad.model.CadBoundingBox
import com.example.cad.model.CadDocument
import com.example.cad.model.CadDrawing
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.cad.repository.LocalDrawingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CadFileManagementTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSampleDoc(title: String = "Test_Floor_Plan"): CadDocument {
        return CadDocument(
            title = title,
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            layers = mapOf(
                "0" to CadLayer.DEFAULT_LAYER_0,
                "WALLS" to CadLayer("WALLS", "WALLS", 0xFF00E5FF, true, false, 2.0f)
            ),
            entities = listOf(
                CadEntity.Line(
                    id = "line_1",
                    layerId = "WALLS",
                    colorArgb = 0xFF00E5FF,
                    start = CadPoint2D(0f, 0f),
                    end = CadPoint2D(100f, 0f),
                    strokeWidth = 2.0f
                ),
                CadEntity.Circle(
                    id = "circle_1",
                    layerId = "0",
                    colorArgb = null,
                    center = CadPoint2D(50f, 50f),
                    radius = 25f
                ),
                CadEntity.Text(
                    id = "text_1",
                    layerId = "0",
                    position = CadPoint2D(10f, 20f),
                    text = "Room A101"
                )
            )
        )
    }

    @Test
    fun testRecentDrawingsAndFavorites() = runBlocking {
        val repo = LocalDrawingRepository()
        val recents = repo.getRecentDrawings().first()
        assertTrue(recents.isNotEmpty())

        val firstId = recents.first().id
        val wasFavorite = recents.first().isFavorite
        repo.toggleFavorite(firstId)

        val updated = repo.getDrawingById(firstId)
        assertNotNull(updated)
        assertEquals(!wasFavorite, updated!!.isFavorite)
    }

    @Test
    fun testRenameDrawing() = runBlocking {
        val repo = LocalDrawingRepository()
        val all = repo.getAllDrawings().first()
        val target = all.first()

        val renameResult = repo.renameDrawing(target.id, "Renamed_Drawing_Plan")
        assertTrue(renameResult.isSuccess)
        val renamed = renameResult.getOrThrow()
        assertTrue(renamed.name.contains("Renamed_Drawing_Plan"))

        val fromDb = repo.getDrawingById(target.id)
        assertNotNull(fromDb)
        assertEquals(renamed.name, fromDb!!.name)
    }

    @Test
    fun testDuplicateDrawing() = runBlocking {
        val repo = LocalDrawingRepository()
        val allBefore = repo.getAllDrawings().first()
        val target = allBefore.first()

        val duplicateResult = repo.duplicateDrawing(target.id)
        assertTrue(duplicateResult.isSuccess)
        val copy = duplicateResult.getOrThrow()

        assertTrue(copy.name.contains("Copy"))
        assertFalse(copy.id == target.id)

        val allAfter = repo.getAllDrawings().first()
        assertEquals(allBefore.size + 1, allAfter.size)
    }

    @Test
    fun testDeleteDrawing() = runBlocking {
        val repo = LocalDrawingRepository()
        val allBefore = repo.getAllDrawings().first()
        val target = allBefore.first()

        val deleteResult = repo.deleteDrawing(target.id)
        assertTrue(deleteResult.isSuccess)

        val fromDb = repo.getDrawingById(target.id)
        assertEquals(null, fromDb)

        val allAfter = repo.getAllDrawings().first()
        assertEquals(allBefore.size - 1, allAfter.size)
    }

    @Test
    fun testCreateProject() = runBlocking {
        val repo = LocalDrawingRepository()
        val result = repo.createProject("Skyline Towers", "Phase 1 structural plans")
        assertTrue(result.isSuccess)

        val project = result.getOrThrow()
        assertEquals("Skyline Towers", project.name)
        assertEquals("Phase 1 structural plans", project.description)

        val allProjects = repo.getAllProjects().first()
        assertTrue(allProjects.any { it.name == "Skyline Towers" })
    }

    @Test
    fun testDxfExporterArchitecture() = runBlocking {
        val doc = createSampleDoc("Sample_Export")
        val destination = File(tempFolder.root, "sample_export.dxf")
        val exporter = DxfExporter()

        val result = exporter.export(doc, destination)
        assertTrue(result.isSuccess)
        assertTrue(destination.exists())
        assertTrue(destination.length() > 100)

        val content = destination.readText()
        assertTrue(content.contains("HEADER"))
        assertTrue(content.contains("AC1015"))
        assertTrue(content.contains("TABLES"))
        assertTrue(content.contains("LAYER"))
        assertTrue(content.contains("WALLS"))
        assertTrue(content.contains("ENTITIES"))
        assertTrue(content.contains("LINE"))
        assertTrue(content.contains("CIRCLE"))
        assertTrue(content.contains("EOF"))
    }

    @Test
    fun testInternalProjectExporterAndParser() = runBlocking {
        val doc = createSampleDoc("Internal_Arch_Bundle")
        val destination = File(tempFolder.root, "arch_bundle.cadproj")
        val exporter = InternalProjectExporter()

        val result = exporter.export(doc, destination)
        assertTrue(result.isSuccess)
        assertTrue(destination.exists())

        val parser = InternalProjectParser()
        val parsedResult = parser.parse(destination)
        assertTrue(parsedResult.isSuccess)

        val parsedDoc = parsedResult.getOrThrow()
        assertEquals("Internal_Arch_Bundle", parsedDoc.title)
        assertEquals(doc.units, parsedDoc.units)
        assertEquals(doc.layers.size, parsedDoc.layers.size)
        assertEquals(doc.entities.size, parsedDoc.entities.size)
    }

    @Test
    fun testDwgSdkExporterPreparedArchitecture() = runBlocking {
        val doc = createSampleDoc("Bridge_Design")
        val destination = File(tempFolder.root, "bridge_design.dwg")
        val exporter = DwgSdkExporter()

        val result = exporter.export(doc, destination)
        // Since native DWG SDK (e.g. ODA) is unlinked in baseline JVM tests,
        // it must report CadDwgEngineRequiredException without crashing,
        // while safely saving a companion internal backup.
        assertFalse(result.isSuccess)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)

        val companionBackup = File(tempFolder.root, "bridge_design.cadproj")
        assertTrue(companionBackup.exists())
        assertTrue(companionBackup.length() > 50)
    }

    @Test
    fun testFileMetadataFields() {
        val drawing = CadDrawing(
            id = "draw_meta_1",
            name = "Office_Interior_A202.dxf",
            format = CadFormat.DXF,
            filePath = "/storage/emulated/0/CAD/Office_Interior_A202.dxf",
            fileSizeBytes = 1_540_000L,
            lastModifiedTimestamp = 1700000000000L,
            units = CadUnit.MILLIMETERS,
            entityCount = 120,
            layerCount = 5,
            isFavorite = true,
            projectName = "Corporate Campus"
        )

        assertEquals("Drawing Exchange Format (DXF)", drawing.drawingType)
        assertTrue(drawing.formattedSize.contains("MB") || drawing.formattedSize.contains("KB"))
        assertTrue(drawing.formattedDate.isNotEmpty())
        assertEquals("Corporate Campus", drawing.projectName)
    }
}

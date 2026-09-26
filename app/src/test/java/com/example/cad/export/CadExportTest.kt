package com.example.cad.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cad.engine.measurement.CadMeasurementResult
import com.example.cad.engine.measurement.CadMeasurementType
import com.example.cad.model.CadDocument
import com.example.cad.model.CadEntity
import com.example.cad.model.CadFormat
import com.example.cad.model.CadLayer
import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit
import com.example.ui.screens.editor.CadMeasurementHistoryItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CadExportTest {

    private lateinit var context: Context
    private lateinit var sampleDoc: CadDocument

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val layerWalls = CadLayer(id = "WALLS", name = "Walls", colorArgb = 0xFFFFFFFFL, lineWeight = 0.5f)
        val layerDimensions = CadLayer(id = "DIMS", name = "Dimensions", colorArgb = 0xFFFFD54FL, lineWeight = 0.25f)
        val layerPlumbing = CadLayer(id = "PLUMB", name = "Plumbing", colorArgb = 0xFF00E5FFL, lineWeight = 0.35f)

        sampleDoc = CadDocument(
            title = "Architectural_Ground_Floor.dxf",
            format = CadFormat.DXF,
            units = CadUnit.MILLIMETERS,
            layers = mapOf(
                layerWalls.id to layerWalls,
                layerDimensions.id to layerDimensions,
                layerPlumbing.id to layerPlumbing
            ),
            entities = listOf(
                CadEntity.Line(
                    id = "L1",
                    layerId = "WALLS",
                    start = CadPoint2D(0f, 0f),
                    end = CadPoint2D(5000f, 0f),
                    strokeWidth = 2f
                ),
                CadEntity.Line(
                    id = "L2",
                    layerId = "WALLS",
                    start = CadPoint2D(5000f, 0f),
                    end = CadPoint2D(5000f, 3000f),
                    strokeWidth = 2f
                ),
                CadEntity.Polyline(
                    id = "PL1",
                    layerId = "WALLS",
                    points = listOf(
                        CadPoint2D(0f, 0f),
                        CadPoint2D(0f, 3000f),
                        CadPoint2D(5000f, 3000f)
                    ),
                    isClosed = false,
                    strokeWidth = 2f
                ),
                CadEntity.Circle(
                    id = "C1",
                    layerId = "PLUMB",
                    center = CadPoint2D(2500f, 1500f),
                    radius = 400f,
                    strokeWidth = 1.5f
                ),
                CadEntity.Arc(
                    id = "A1",
                    layerId = "PLUMB",
                    center = CadPoint2D(1000f, 1000f),
                    radius = 300f,
                    startAngleDeg = 0f,
                    sweepAngleDeg = 90f,
                    strokeWidth = 1.5f
                ),
                CadEntity.Dimension(
                    id = "D1",
                    layerId = "DIMS",
                    start = CadPoint2D(0f, -200f),
                    end = CadPoint2D(5000f, -200f),
                    textPoint = CadPoint2D(2500f, -300f),
                    valueText = "5000 mm"
                ),
                CadEntity.Text(
                    id = "T1",
                    layerId = "WALLS",
                    position = CadPoint2D(2500f, 1500f),
                    text = "LIVING ROOM\nAREA: 15.0 m²",
                    textHeight = 180f,
                    isMultiLine = true
                )
            )
        )
    }

    @Test
    fun testPdfExport_FitToPage_A4Landscape() = runBlocking {
        val exporter = CadPdfExporter(context)
        val config = CadPdfExportConfig(
            title = "Test_Architectural_Floor_Plan",
            pageSize = CadPdfPageSize.A4,
            orientation = CadPageOrientation.LANDSCAPE,
            scale = CadDrawingScale.FIT_TO_PAGE,
            includeTitleBlock = true,
            includeBorder = true,
            includeMeasurements = true,
            includeGrid = false,
            colorMode = CadColorMode.LAYER_COLORS
        )

        val result = exporter.exportPdf(sampleDoc, config)
        assertTrue("PDF export should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue("Exported PDF file must exist", file.exists())
        assertTrue("Exported PDF must not be empty (${file.length()} bytes)", file.length() > 500)
    }

    @Test
    fun testPdfExport_Scale1to100_Monochrome_Grid() = runBlocking {
        val exporter = CadPdfExporter(context)
        val config = CadPdfExportConfig(
            title = "Floor_Plan_1_100_Mono",
            pageSize = CadPdfPageSize.A3,
            orientation = CadPageOrientation.PORTRAIT,
            scale = CadDrawingScale.SCALE_1_100,
            includeTitleBlock = true,
            includeBorder = true,
            includeGrid = true,
            includeMeasurements = true,
            colorMode = CadColorMode.MONOCHROME,
            lineweightStyle = CadLineweightStyle.STANDARD
        )

        val result = exporter.exportPdf(sampleDoc, config)
        assertTrue("Monochrome A3 PDF export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue("File exists", file.exists())
        assertTrue("File size > 500 bytes", file.length() > 500)
    }

    @Test
    fun testPdfExport_SelectedLayersOnly() = runBlocking {
        val exporter = CadPdfExporter(context)
        val config = CadPdfExportConfig(
            title = "Walls_Only_Export",
            pageSize = CadPdfPageSize.LETTER,
            orientation = CadPageOrientation.LANDSCAPE,
            scale = CadDrawingScale.FIT_TO_PAGE,
            selectedLayerIds = setOf("WALLS") // Excludes DIMS and PLUMB
        )

        val result = exporter.exportPdf(sampleDoc, config)
        assertTrue("Selected layer PDF export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertTrue(file.length() > 500)
    }

    @Test
    fun testImageExport_Png_WhiteAndTransparent() = runBlocking {
        val exporter = CadImageExporter(context)

        // 1. White Background Plot PNG
        val whiteConfig = CadImageExportConfig(
            format = CadImageFormat.PNG,
            resolution = CadImageResolution.DRAFT_1X,
            backgroundMode = CadImageBackgroundMode.WHITE_PLOT,
            includeGrid = false,
            includeMeasurements = true
        )
        val whiteResult = exporter.exportImage(sampleDoc, whiteConfig)
        assertTrue("PNG White Plot export should succeed", whiteResult.isSuccess)
        val whiteFile = whiteResult.getOrThrow()
        assertTrue(whiteFile.exists())
        assertTrue(whiteFile.length() > 1000)

        // 2. Transparent Background PNG
        val transConfig = CadImageExportConfig(
            format = CadImageFormat.PNG,
            resolution = CadImageResolution.DRAFT_1X,
            backgroundMode = CadImageBackgroundMode.TRANSPARENT_PNG
        )
        val transResult = exporter.exportImage(sampleDoc, transConfig)
        assertTrue("PNG Transparent export should succeed", transResult.isSuccess)
        val transFile = transResult.getOrThrow()
        assertTrue(transFile.exists())
        assertTrue(transFile.length() > 1000)
    }

    @Test
    fun testImageExport_Jpeg_DarkCanvas() = runBlocking {
        val exporter = CadImageExporter(context)
        val jpegConfig = CadImageExportConfig(
            format = CadImageFormat.JPEG,
            resolution = CadImageResolution.DRAFT_1X,
            backgroundMode = CadImageBackgroundMode.DARK_CANVAS,
            includeGrid = true,
            jpegQuality = 90
        )
        val result = exporter.exportImage(sampleDoc, jpegConfig)
        assertTrue("JPEG Dark Canvas export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertTrue(file.length() > 1000)
    }

    @Test
    fun testMeasurementReportExport_Csv() = runBlocking {
        val exporter = CadMeasurementReportExporter(context)
        val history = listOf(
            CadMeasurementHistoryItem(
                type = CadMeasurementType.DISTANCE,
                primaryValue = "5000.00 mm",
                unit = CadUnit.MILLIMETERS,
                result = CadMeasurementResult.Distance(
                    p1 = CadPoint2D(0f, 0f),
                    p2 = CadPoint2D(5000f, 0f),
                    distance = 5000.0,
                    deltaX = 5000.0,
                    deltaY = 0.0,
                    angleDeg = 0.0,
                    unit = CadUnit.MILLIMETERS
                )
            ),
            CadMeasurementHistoryItem(
                type = CadMeasurementType.AREA,
                primaryValue = "15.00 m²",
                area = "15.00 m²",
                perimeter = "16.00 m",
                unit = CadUnit.METERS,
                result = CadMeasurementResult.Area(
                    points = listOf(CadPoint2D(0f, 0f), CadPoint2D(5000f, 0f), CadPoint2D(5000f, 3000f), CadPoint2D(0f, 3000f)),
                    areaSquareUnits = 15.0,
                    perimeter = 16.0,
                    unit = CadUnit.METERS
                )
            )
        )

        val config = CadMeasurementReportConfig(format = CadReportFormat.CSV_SPREADSHEET)
        val result = exporter.exportReport(sampleDoc, config, history)
        assertTrue("CSV Report export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())

        val content = file.readText()
        assertTrue("CSV should contain title", content.contains("Architectural_Ground_Floor.dxf"))
        assertTrue("CSV should contain LAYER BREAKDOWN", content.contains("LAYER BREAKDOWN"))
        assertTrue("CSV should contain WALLS layer", content.contains("WALLS"))
        assertTrue("CSV should contain ENTITY INVENTORY", content.contains("ENTITY INVENTORY"))
        assertTrue("CSV should contain L1 Line", content.contains("Line"))
        assertTrue("CSV should contain Circle", content.contains("Circle"))
        assertTrue("CSV should contain TOTAL COMBINED LINEAR LENGTH", content.contains("TOTAL COMBINED LINEAR LENGTH"))
        assertTrue("CSV should contain RECORDED MEASUREMENTS", content.contains("RECORDED MEASUREMENTS"))
        assertTrue("CSV should contain 5000.00 mm", content.contains("5000.00 mm"))
    }

    @Test
    fun testMeasurementReportExport_Text() = runBlocking {
        val exporter = CadMeasurementReportExporter(context)
        val config = CadMeasurementReportConfig(format = CadReportFormat.TEXT_SUMMARY)
        val result = exporter.exportReport(sampleDoc, config)
        assertTrue("Text report export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())

        val content = file.readText()
        assertTrue("Text report should contain header", content.contains("CAD MEASUREMENT & INSPECTION REPORT"))
        assertTrue("Text report should contain Layer Summary", content.contains("1. LAYER SUMMARY"))
        assertTrue("Text report should contain Quantity Takeoff", content.contains("2. QUANTITY TAKEOFF TOTALS"))
    }

    @Test
    fun testMeasurementReportExport_Pdf() = runBlocking {
        val exporter = CadMeasurementReportExporter(context)
        val config = CadMeasurementReportConfig(format = CadReportFormat.PDF_REPORT)
        val result = exporter.exportReport(sampleDoc, config)
        assertTrue("PDF report export should succeed", result.isSuccess)
        val file = result.getOrThrow()
        assertTrue(file.exists())
        assertTrue("PDF report must have content", file.length() > 500)
    }

    @Test
    fun testPageSizesAndScales_Definitions() {
        assertEquals("A4 (210 × 297 mm)", CadPdfPageSize.A4.displayName)
        assertEquals(595, CadPdfPageSize.A4.widthPt)
        assertEquals(842, CadPdfPageSize.A4.heightPt)
        assertEquals(612, CadPdfPageSize.LETTER.widthPt)

        assertEquals("Fit to Page", CadDrawingScale.FIT_TO_PAGE.displayName)
        assertEquals(0.01f, CadDrawingScale.SCALE_1_100.ratioToWorld)
        assertEquals(1.0f, CadDrawingScale.SCALE_1_1.ratioToWorld)
    }
}

package com.example.cad.export

import com.example.cad.model.CadDocument
import com.example.cad.model.CadUnit

/**
 * Standard CAD Page Sizes with PostScript points (72 pt/inch) and mm dimensions.
 */
enum class CadPdfPageSize(
    val displayName: String,
    val widthPt: Int,
    val heightPt: Int,
    val widthMm: Float,
    val heightMm: Float
) {
    A4("A4 (210 × 297 mm)", 595, 842, 210f, 297f),
    A3("A3 (297 × 420 mm)", 842, 1191, 297f, 420f),
    A2("A2 (420 × 594 mm)", 1191, 1684, 420f, 594f),
    A1("A1 (594 × 841 mm)", 1684, 2384, 594f, 841f),
    LETTER("US Letter (8.5 × 11 in)", 612, 792, 215.9f, 279.4f),
    LEGAL("US Legal (8.5 × 14 in)", 612, 1008, 215.9f, 355.6f),
    TABLOID("US Tabloid / Ledger (11 × 17 in)", 792, 1224, 279.4f, 431.8f)
}

enum class CadPageOrientation(val displayName: String) {
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}

enum class CadDrawingScale(
    val displayName: String,
    val ratioToWorld: Float? // e.g. 0.01 for 1:100 where 1 mm drawing = 100 mm world
) {
    FIT_TO_PAGE("Fit to Page", null),
    SCALE_1_1("1:1 (Full Scale)", 1.0f),
    SCALE_1_2("1:2", 0.5f),
    SCALE_1_5("1:5", 0.2f),
    SCALE_1_10("1:10", 0.1f),
    SCALE_1_20("1:20", 0.05f),
    SCALE_1_50("1:50", 0.02f),
    SCALE_1_100("1:100 (Architectural)", 0.01f),
    SCALE_1_200("1:200", 0.005f),
    SCALE_1_500("1:500 (Civil / Site)", 0.002f),
    CUSTOM("Custom Scale...", null)
}

enum class CadColorMode(val displayName: String) {
    LAYER_COLORS("CAD Layer Colors"),
    MONOCHROME("Monochrome (Black & White)")
}

enum class CadLineweightStyle(val displayName: String) {
    WYSIWYG("CAD Lineweights (By Layer/Entity)"),
    STANDARD("Standard (0.5 pt)"),
    HAIRLINE("Hairline (0.25 pt)"),
    BOLD("Bold / Heavy (1.0 pt)")
}

data class CadPdfExportConfig(
    val title: String = "",
    val pageSize: CadPdfPageSize = CadPdfPageSize.A4,
    val orientation: CadPageOrientation = CadPageOrientation.LANDSCAPE,
    val scale: CadDrawingScale = CadDrawingScale.FIT_TO_PAGE,
    val customScaleRatio: Float = 100f, // 1:customScaleRatio
    val includeGrid: Boolean = false,
    val includeMeasurements: Boolean = true,
    val includeTitleBlock: Boolean = true,
    val includeBorder: Boolean = true,
    val selectedLayerIds: Set<String> = emptySet(), // empty means all visible layers
    val colorMode: CadColorMode = CadColorMode.LAYER_COLORS,
    val lineweightStyle: CadLineweightStyle = CadLineweightStyle.WYSIWYG,
    val author: String = "CAD Mobile",
    val notes: String = ""
)

enum class CadImageFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    JPEG("jpg", "image/jpeg")
}

enum class CadImageBackgroundMode(val displayName: String) {
    WHITE_PLOT("White (Plot Style)"),
    DARK_CANVAS("Dark (CAD Model Space)"),
    TRANSPARENT_PNG("Transparent (PNG Only)")
}

enum class CadImageResolution(val displayName: String, val multiplier: Float, val baseTargetPx: Int) {
    DRAFT_1X("Standard (1080p)", 1f, 1920),
    HIGH_RES_2X("High Res 2K (2160p)", 2f, 3840),
    ULTRA_4X("Ultra HD 4K (4320p)", 4f, 7680)
}

data class CadImageExportConfig(
    val format: CadImageFormat = CadImageFormat.PNG,
    val resolution: CadImageResolution = CadImageResolution.HIGH_RES_2X,
    val backgroundMode: CadImageBackgroundMode = CadImageBackgroundMode.WHITE_PLOT,
    val includeGrid: Boolean = false,
    val includeMeasurements: Boolean = true,
    val selectedLayerIds: Set<String> = emptySet(),
    val colorMode: CadColorMode = CadColorMode.LAYER_COLORS,
    val jpegQuality: Int = 92
)

enum class CadReportFormat(val displayName: String, val extension: String, val mimeType: String) {
    PDF_REPORT("Formatted PDF Report", "pdf", "application/pdf"),
    CSV_SPREADSHEET("CSV Spreadsheet (Excel compatible)", "csv", "text/csv"),
    TEXT_SUMMARY("Plain Text Inspection Report", "txt", "text/plain")
}

data class CadMeasurementReportConfig(
    val format: CadReportFormat = CadReportFormat.PDF_REPORT,
    val includeEntityBreakdown: Boolean = true,
    val includeLayerSummary: Boolean = true,
    val includeRecordedMeasurements: Boolean = true,
    val includeGeometryDetails: Boolean = true
)

data class ExportResult(
    val file: java.io.File,
    val formatName: String,
    val mimeType: String,
    val fileSizeFormatted: String,
    val shareIntent: android.content.Intent? = null
)

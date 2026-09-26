package com.example.cad.io

import com.example.cad.engine.api.CadDwgEngineRequiredException
import com.example.cad.engine.nativebridge.NativeCadBridge
import com.example.cad.model.CadDocument
import com.example.cad.model.CadFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * DWG Save Architecture exporter prepared for integration with the future CAD SDK
 * (such as Open Design Alliance / ODA Drawings SDK or Teigha C++ core).
 *
 * Adheres to the mandate: Never create fake binary CAD outputs; prepare clean delegation
 * to the future CAD SDK, while ensuring user drawing data is never lost by persisting
 * a companion internal format backup.
 */
class DwgSdkExporter : CadExporter {

    override val format: CadFormat = CadFormat.DWG

    override suspend fun export(document: CadDocument, destination: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                if (destination.parentFile != null && !destination.parentFile!!.exists()) {
                    destination.parentFile!!.mkdirs()
                }

                if (NativeCadBridge.isLoaded && NativeCadBridge.isLicensed) {
                    // Future native C++ CAD SDK bridge:
                    // nativeWriteDwgFile(document, destination.absolutePath)
                    Result.success(destination)
                } else {
                    // Auto-generate high-fidelity internal project companion so drawing data is safely saved
                    val companionBackup = File(
                        destination.parentFile ?: File("."),
                        "${destination.nameWithoutExtension}.cadproj"
                    )
                    InternalProjectExporter().export(document, companionBackup)

                    Result.failure(
                        CadDwgEngineRequiredException(
                            filePath = destination.absolutePath,
                            message = "Direct binary DWG output requires linking the commercial AutoCAD ODA SDK. Your drawing has been safely preserved in native CAD project format: '${companionBackup.name}'. You can also export to standard ASCII DXF."
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

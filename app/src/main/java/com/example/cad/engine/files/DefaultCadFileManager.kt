package com.example.cad.engine.files

import com.example.cad.model.CadFormat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class DefaultCadFileManager(
    baseDir: File = File(System.getProperty("java.io.tmpdir", "/tmp"), "cad_mobile_storage")
) : CadFileManager {

    override val projectsDir: File = File(baseDir, "projects").apply { mkdirs() }
    override val cacheDir: File = File(baseDir, "cache").apply { mkdirs() }

    override fun detectFormat(file: File): CadFormat {
        val extension = file.extension.lowercase()
        return when (extension) {
            "dwg" -> CadFormat.DWG
            "dxf" -> CadFormat.DXF
            "svg" -> CadFormat.SVG
            else -> {
                // If extension is ambiguous, check magic bytes
                if (file.exists() && file.length() >= 6) {
                    try {
                        FileInputStream(file).use { stream ->
                            val header = ByteArray(6)
                            val read = stream.read(header)
                            if (read == 6 && String(header, Charsets.US_ASCII).startsWith("AC")) {
                                return CadFormat.DWG
                            }
                        }
                    } catch (_: Exception) { }
                }
                CadFormat.DXF
            }
        }
    }

    override fun inspectDwgHeader(file: File): DwgHeaderInfo {
        if (!file.exists() || file.length() < 6) {
            return DwgHeaderInfo("UNKNOWN", "File missing or too small", false, false, file.length())
        }
        return try {
            FileInputStream(file).use { stream ->
                val header = ByteArray(6)
                val bytesRead = stream.read(header)
                if (bytesRead == 6) {
                    DwgHeaderInfo.fromHeaderBytes(header, file.length())
                } else {
                    DwgHeaderInfo("UNKNOWN", "Unreadable header", false, false, file.length())
                }
            }
        } catch (e: Exception) {
            DwgHeaderInfo("ERROR", "Error reading file: ${e.message}", false, false, file.length())
        }
    }

    override fun copyStreamToTemp(inputStream: InputStream, suggestedName: String): File {
        val sanitized = suggestedName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val tempFile = File(cacheDir, "${UUID.randomUUID()}_$sanitized")
        FileOutputStream(tempFile).use { out ->
            inputStream.copyTo(out)
        }
        return tempFile
    }

    override fun createProjectFile(fileName: String, format: CadFormat): File {
        val sanitized = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val file = File(projectsDir, sanitized)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    override fun listProjectFiles(): List<File> {
        return projectsDir.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
    }

    override fun deleteDrawing(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }
}

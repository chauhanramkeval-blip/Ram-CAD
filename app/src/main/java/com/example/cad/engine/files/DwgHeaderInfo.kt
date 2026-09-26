package com.example.cad.engine.files

/**
 * Information extracted from a DWG binary file header.
 *
 * All binary AutoCAD DWG files begin with a 6-byte sentinel header string (e.g., "AC1032").
 */
data class DwgHeaderInfo(
    val versionString: String,
    val autocadRelease: String,
    val isSupportedByOda: Boolean,
    val isBinaryDwg: Boolean,
    val fileSizeBytes: Long
) {
    companion object {
        fun fromHeaderBytes(headerBytes: ByteArray, fileSizeBytes: Long): DwgHeaderInfo {
            if (headerBytes.size < 6) {
                return DwgHeaderInfo("UNKNOWN", "Unknown Format", false, false, fileSizeBytes)
            }
            val magic = String(headerBytes.copyOfRange(0, 6), Charsets.US_ASCII)
            val release = when (magic) {
                "AC1015" -> "AutoCAD 2000 / 2000i / 2002 (R15)"
                "AC1018" -> "AutoCAD 2004 / 2005 / 2006 (R18)"
                "AC1021" -> "AutoCAD 2007 / 2008 / 2009 (R21)"
                "AC1024" -> "AutoCAD 2010 / 2011 / 2012 (R24)"
                "AC1027" -> "AutoCAD 2013 / 2014 / 2015 / 2016 / 2017 (R27)"
                "AC1032" -> "AutoCAD 2018 / 2021 / 2024 / 2025 (R32)"
                else -> if (magic.startsWith("AC")) "AutoCAD Format ($magic)" else "Non-DWG Data"
            }
            val isDwg = magic.startsWith("AC")
            return DwgHeaderInfo(
                versionString = magic,
                autocadRelease = release,
                isSupportedByOda = isDwg, // All AC1015-AC1032 versions supported by modern ODA Drawings SDK
                isBinaryDwg = isDwg,
                fileSizeBytes = fileSizeBytes
            )
        }
    }
}

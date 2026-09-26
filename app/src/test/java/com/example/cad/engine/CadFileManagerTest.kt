package com.example.cad.engine

import com.example.cad.engine.files.DefaultCadFileManager
import com.example.cad.engine.files.DwgHeaderInfo
import com.example.cad.model.CadFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CadFileManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDetectDxfFormat() {
        val file = tempFolder.newFile("floor_plan.dxf")
        file.writeText("0\nSECTION\n2\nHEADER\n0\nENDSEC\n0\nEOF\n")
        val fileManager = DefaultCadFileManager(tempFolder.root)
        val format = fileManager.detectFormat(file)
        assertEquals(CadFormat.DXF, format)
    }

    @Test
    fun testDetectDwgFormat() {
        val file = tempFolder.newFile("structural_foundation.dwg")
        file.writeBytes("AC1032".toByteArray(Charsets.US_ASCII) + ByteArray(20))
        val fileManager = DefaultCadFileManager(tempFolder.root)
        val format = fileManager.detectFormat(file)
        assertEquals(CadFormat.DWG, format)
    }

    @Test
    fun testInspectValidDwgHeader_2018() {
        val headerBytes = "AC1032".toByteArray(Charsets.US_ASCII) + ByteArray(10)
        val info = DwgHeaderInfo.fromHeaderBytes(headerBytes, headerBytes.size.toLong())

        assertEquals("AC1032", info.versionString)
        assertTrue(info.autocadRelease.contains("2018"))
        assertTrue(info.isBinaryDwg)
        assertTrue(info.isSupportedByOda)
    }

    @Test
    fun testInspectValidDwgHeader_2013() {
        val headerBytes = "AC1027".toByteArray(Charsets.US_ASCII) + ByteArray(10)
        val info = DwgHeaderInfo.fromHeaderBytes(headerBytes, headerBytes.size.toLong())

        assertEquals("AC1027", info.versionString)
        assertTrue(info.autocadRelease.contains("2013"))
        assertTrue(info.isBinaryDwg)
        assertTrue(info.isSupportedByOda)
    }

    @Test
    fun testInspectInvalidDwgHeader() {
        val headerBytes = "NOTADWG".toByteArray(Charsets.US_ASCII)
        val info = DwgHeaderInfo.fromHeaderBytes(headerBytes, headerBytes.size.toLong())

        assertFalse(info.isBinaryDwg)
        assertFalse(info.isSupportedByOda)
    }
}

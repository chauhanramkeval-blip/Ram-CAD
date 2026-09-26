package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cad.engine.DefaultCadEngine
import com.example.cad.model.CadFormat
import com.example.ui.permissions.CadPermissionHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CAD Mobile Viewer & Editor", appName)
    }

    @Test
    fun `storage permission helper returns required permissions`() {
        val perms = CadPermissionHelper.getRequiredPermissions()
        assertTrue(perms.isNotEmpty())
        assertTrue(perms.contains(android.Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    @Test
    fun `storage permission helper check storage permission on context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // On modern SDKs, hasStoragePermission evaluates safely
        val hasPermission = CadPermissionHelper.hasStoragePermission(context)
        // Verify call completes without exception
        assertNotNull(hasPermission)
    }

    @Test
    fun `engine parses dwg header bytes gracefully`() = runBlocking {
        val engine = DefaultCadEngine()
        val dwgHeaderBytes = byteArrayOf(0x41, 0x43, 0x31, 0x30, 0x33, 0x32) // "AC1032"
        val inputStream = ByteArrayInputStream(dwgHeaderBytes)
        val result = engine.parser.parse(inputStream, CadFormat.DWG, "Building_Plan.dwg")

        assertTrue(result.isSuccess)
        val doc = result.getOrThrow()
        assertEquals(CadFormat.DWG, doc.format)
        assertEquals("Building_Plan.dwg", doc.title)
        assertTrue(doc.entities.isNotEmpty())
    }
}

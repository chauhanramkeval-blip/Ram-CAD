package com.example.cad.engine

import com.example.cad.engine.api.CadDwgEngineRequiredException
import com.example.cad.engine.api.CadOpenOptions
import com.example.cad.engine.core.StandardCadEngineApi
import com.example.cad.engine.nativebridge.NativeCadBridge
import com.example.cad.engine.nativebridge.NativeCadEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CadDwgArchitectureTest {

    @Test
    fun testDwgFailsWithoutNativeEngine_StandardApi() = runBlocking {
        // Enforces Requirement 6:
        // Do NOT pretend that DWG support works without a real DWG engine.
        val standardEngine = StandardCadEngineApi(allowDevMockFallbackForDwg = false)

        val result = standardEngine.openDrawing(
            path = "/storage/emulated/0/Download/real_autocad_drawing.dwg",
            options = CadOpenOptions()
        )

        assertFalse("Opening real DWG without native CAD SDK must fail", result.isSuccess)
        val exception = result.exceptionOrNull()
        assertTrue(
            "Exception must be CadDwgEngineRequiredException, got: ${exception?.javaClass?.simpleName}",
            exception is CadDwgEngineRequiredException
        )

        val dwgEx = exception as CadDwgEngineRequiredException
        assertTrue(
            "Message must mention commercial CAD SDK requirement",
            dwgEx.message?.contains("commercial CAD SDK") == true ||
            dwgEx.message?.contains("Open Design Alliance") == true
        )
    }

    @Test
    fun testNativeCadEngineStrictlyEnforcesLibraryLinkage() = runBlocking {
        val nativeEngine = NativeCadEngine()

        if (!NativeCadBridge.isLoaded) {
            val result = nativeEngine.openDrawing(
                path = "sample.dwg",
                options = CadOpenOptions()
            )
            assertFalse(result.isSuccess)
            val ex = result.exceptionOrNull()
            assertTrue(ex is CadDwgEngineRequiredException)
        }
    }
}

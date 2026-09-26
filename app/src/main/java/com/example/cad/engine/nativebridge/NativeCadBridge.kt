package com.example.cad.engine.nativebridge

import java.util.logging.Level
import java.util.logging.Logger

/**
 * JNI Bridge interface to the native C++ CAD Engine (e.g. Open Design Alliance ODA SDK).
 *
 * This class isolates all `external` C++ calls from the rest of the application.
 * If the native binary `.so` has not been compiled or linked, it safely reports
 * [isLoaded] as false without crashing the JVM, and rejects calls with structured exceptions.
 */
object NativeCadBridge {

    private val logger = Logger.getLogger(NativeCadBridge::class.java.name)

    const val NATIVE_LIB_NAME = "cad_dwg_native"

    @Volatile
    private var libraryLoaded: Boolean = false

    @Volatile
    private var sdkLicensed: Boolean = false

    init {
        try {
            System.loadLibrary(NATIVE_LIB_NAME)
            libraryLoaded = true
            logger.info("Successfully loaded native CAD library: $NATIVE_LIB_NAME")
        } catch (e: UnsatisfiedLinkError) {
            libraryLoaded = false
            logger.info("Native CAD library '$NATIVE_LIB_NAME' not found in path. Operating in Kotlin baseline/mock mode.")
        } catch (e: Exception) {
            libraryLoaded = false
            logger.log(Level.WARNING, "Unexpected exception loading '$NATIVE_LIB_NAME': ${e.message}", e)
        }
    }

    /**
     * Checks if the native C++ library (.so) is compiled and loaded into the process.
     */
    val isLoaded: Boolean get() = libraryLoaded

    /**
     * Checks if the commercial CAD SDK (e.g. ODA) has been activated with a valid license.
     */
    val isLicensed: Boolean get() = sdkLicensed

    /**
     * Initializes the native commercial CAD engine with an activation/license key.
     *
     * @param licenseKey Commercial license key (e.g. from Open Design Alliance membership).
     * @return 0 on success, or non-zero error code.
     */
    fun initializeSdk(licenseKey: String): Int {
        if (!libraryLoaded) return -1
        return try {
            val result = nativeInitialize(licenseKey)
            sdkLicensed = (result == 0)
            result
        } catch (e: UnsatisfiedLinkError) {
            -2
        }
    }

    // =========================================================================
    // JNI External Declarations implemented in C++ (app/src/main/cpp/)
    // =========================================================================

    @JvmStatic
    private external fun nativeInitialize(licenseKey: String): Int

    @JvmStatic
    external fun nativeRelease(): Unit

    @JvmStatic
    external fun nativeOpenDrawing(filePath: String, readOnly: Boolean, auditAndRecover: Boolean): Long

    @JvmStatic
    external fun nativeCloseDrawing(nativeHandle: Long): Int

    @JvmStatic
    external fun nativeGetLayers(nativeHandle: Long): Array<NativeLayerDto>

    @JvmStatic
    external fun nativeGetEntities(nativeHandle: Long, filterType: Int): Array<NativeEntityDto>

    @JvmStatic
    external fun nativeCreateEntity(
        nativeHandle: Long,
        type: Int,
        coordinates: FloatArray,
        colorArgb: Int,
        layerName: String,
        strokeWidth: Float,
        textContent: String?,
        isClosed: Boolean,
        rotationDeg: Float
    ): String?

    @JvmStatic
    external fun nativeModifyEntity(
        nativeHandle: Long,
        entityId: String,
        coordinates: FloatArray,
        colorArgb: Int,
        layerName: String
    ): Int

    @JvmStatic
    external fun nativeDeleteEntity(nativeHandle: Long, entityId: String): Int

    @JvmStatic
    external fun nativeSaveDrawing(nativeHandle: Long, targetPath: String, dwgVersionCode: Int): Int

    @JvmStatic
    external fun nativeComputeExtents(nativeHandle: Long, outBounds: FloatArray): Int
}

/**
 * ============================================================================
 * CAD Mobile - Native C++ JNI Bridge Architecture
 * ============================================================================
 *
 * This header defines the JNI entry points for the native CAD Engine.
 * It translates between Android JNI types and C++ CAD SDK structures.
 *
 * Designed to link against professional CAD SDKs:
 * - Open Design Alliance (ODA) Drawings SDK (formerly Teigha)
 * - Autodesk RealDWG (Windows/Server) / ODA Kernel for Android NDK
 *
 * Package: com.example.cad.engine.nativebridge.NativeCadBridge
 */

#ifndef CAD_NATIVE_BRIDGE_H
#define CAD_NATIVE_BRIDGE_H

#include <jni.h>
#include <string>
#include <vector>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeInitialize
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeInitialize(
    JNIEnv* env,
    jclass clazz,
    jstring licenseKey
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeRelease
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeRelease(
    JNIEnv* env,
    jclass clazz
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeOpenDrawing
 * Signature: (Ljava/lang/String;ZZ)J
 */
JNIEXPORT jlong JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeOpenDrawing(
    JNIEnv* env,
    jclass clazz,
    jstring filePath,
    jboolean readOnly,
    jboolean auditAndRecover
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeCloseDrawing
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeCloseDrawing(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeGetLayers
 * Signature: (J)[Lcom/example/cad/engine/nativebridge/NativeLayerDto;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeGetLayers(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeGetEntities
 * Signature: (JI)[Lcom/example/cad/engine/nativebridge/NativeEntityDto;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeGetEntities(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jint filterType
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeCreateEntity
 * Signature: (JI[FILjava/lang/String;FLjava/lang/String;ZF)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeCreateEntity(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jint type,
    jfloatArray coordinates,
    jint colorArgb,
    jstring layerName,
    jfloat strokeWidth,
    jstring textContent,
    jboolean isClosed,
    jfloat rotationDeg
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeModifyEntity
 * Signature: (JLjava/lang/String;[FILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeModifyEntity(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jstring entityId,
    jfloatArray coordinates,
    jint colorArgb,
    jstring layerName
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeDeleteEntity
 * Signature: (JLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeDeleteEntity(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jstring entityId
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeSaveDrawing
 * Signature: (JLjava/lang/String;I)I
 */
JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeSaveDrawing(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jstring targetPath,
    jint dwgVersionCode
);

/*
 * Class:     com_example_cad_engine_nativebridge_NativeCadBridge
 * Method:    nativeComputeExtents
 * Signature: (J[F)I
 */
JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeComputeExtents(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jfloatArray outBounds
);

#ifdef __cplusplus
}
#endif

#endif // CAD_NATIVE_BRIDGE_H

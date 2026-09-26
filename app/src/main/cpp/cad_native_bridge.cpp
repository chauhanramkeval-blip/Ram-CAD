/**
 * ============================================================================
 * CAD Mobile - JNI Bridge Implementation
 * ============================================================================
 */

#include "cad_native_bridge.h"
#include "oda_sdk_adapter.h"

JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeInitialize(
    JNIEnv* env,
    jclass clazz,
    jstring licenseKey
) {
    if (!licenseKey) return -1;
    const char* keyChars = env->GetStringUTFChars(licenseKey, nullptr);
    std::string keyStr(keyChars);
    env->ReleaseStringUTFChars(licenseKey, keyChars);

    return getOdaSdkAdapterInstance()->initialize(keyStr);
}

JNIEXPORT void JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeRelease(
    JNIEnv* env,
    jclass clazz
) {
    getOdaSdkAdapterInstance()->release();
}

JNIEXPORT jlong JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeOpenDrawing(
    JNIEnv* env,
    jclass clazz,
    jstring filePath,
    jboolean readOnly,
    jboolean auditAndRecover
) {
    if (!filePath) return 0;
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    std::string pathStr(pathChars);
    env->ReleaseStringUTFChars(filePath, pathChars);

    return getOdaSdkAdapterInstance()->openDrawing(pathStr, readOnly, auditAndRecover);
}

JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeCloseDrawing(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle
) {
    return getOdaSdkAdapterInstance()->closeDrawing(nativeHandle);
}

JNIEXPORT jobjectArray JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeGetLayers(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle
) {
    jclass layerClass = env->FindClass("com/example/cad/engine/nativebridge/NativeLayerDto");
    if (!layerClass) return nullptr;

    jmethodID constructor = env->GetMethodID(
        layerClass,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/String;IZZFL)V"
    );

    std::vector<NativeCadLayerData> layers = getOdaSdkAdapterInstance()->getLayers(nativeHandle);
    jobjectArray array = env->NewObjectArray(layers.size(), layerClass, nullptr);

    for (size_t i = 0; i < layers.size(); ++i) {
        jstring jId = env->NewStringUTF(layers[i].id.c_str());
        jstring jName = env->NewStringUTF(layers[i].name.c_str());
        jobject layerObj = env->NewObject(
            layerClass,
            constructor,
            jId,
            jName,
            (jint)layers[i].colorArgb,
            (jboolean)layers[i].isVisible,
            (jboolean)layers[i].isLocked,
            (jfloat)layers[i].lineWeight
        );
        env->SetObjectArrayElement(array, i, layerObj);
        env->DeleteLocalRef(jId);
        env->DeleteLocalRef(jName);
        env->DeleteLocalRef(layerObj);
    }
    return array;
}

JNIEXPORT jobjectArray JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeGetEntities(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jint filterType
) {
    jclass entityClass = env->FindClass("com/example/cad/engine/nativebridge/NativeEntityDto");
    if (!entityClass) return nullptr;

    jmethodID constructor = env->GetMethodID(
        entityClass,
        "<init>",
        "(Ljava/lang/String;ILjava/lang/String;IF[FLjava/lang/String;ZF)V"
    );

    std::vector<NativeCadEntityData> entities = getOdaSdkAdapterInstance()->getEntities(nativeHandle, filterType);
    jobjectArray array = env->NewObjectArray(entities.size(), entityClass, nullptr);

    for (size_t i = 0; i < entities.size(); ++i) {
        jstring jId = env->NewStringUTF(entities[i].id.c_str());
        jstring jLayer = env->NewStringUTF(entities[i].layerName.c_str());
        jstring jText = entities[i].textContent.empty() ? nullptr : env->NewStringUTF(entities[i].textContent.c_str());

        jfloatArray jCoords = env->NewFloatArray(entities[i].coordinates.size());
        env->SetFloatArrayRegion(jCoords, 0, entities[i].coordinates.size(), entities[i].coordinates.data());

        jobject entityObj = env->NewObject(
            entityClass,
            constructor,
            jId,
            (jint)entities[i].type,
            jLayer,
            (jint)entities[i].colorArgb,
            (jfloat)entities[i].strokeWidth,
            jCoords,
            jText,
            (jboolean)entities[i].isClosed,
            (jfloat)entities[i].rotationDeg
        );

        env->SetObjectArrayElement(array, i, entityObj);
        env->DeleteLocalRef(jId);
        env->DeleteLocalRef(jLayer);
        if (jText) env->DeleteLocalRef(jText);
        env->DeleteLocalRef(jCoords);
        env->DeleteLocalRef(entityObj);
    }
    return array;
}

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
) {
    NativeCadEntityData data;
    data.type = type;
    data.colorArgb = colorArgb;
    data.strokeWidth = strokeWidth;
    data.isClosed = isClosed;
    data.rotationDeg = rotationDeg;

    if (layerName) {
        const char* lChars = env->GetStringUTFChars(layerName, nullptr);
        data.layerName = lChars;
        env->ReleaseStringUTFChars(layerName, lChars);
    }
    if (textContent) {
        const char* tChars = env->GetStringUTFChars(textContent, nullptr);
        data.textContent = tChars;
        env->ReleaseStringUTFChars(textContent, tChars);
    }
    if (coordinates) {
        jsize len = env->GetArrayLength(coordinates);
        data.coordinates.resize(len);
        env->GetFloatArrayRegion(coordinates, 0, len, data.coordinates.data());
    }

    std::string assignedId = getOdaSdkAdapterInstance()->createEntity(nativeHandle, data);
    return env->NewStringUTF(assignedId.c_str());
}

JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeModifyEntity(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jstring entityId,
    jfloatArray coordinates,
    jint colorArgb,
    jstring layerName
) {
    if (!entityId) return -1;
    const char* idChars = env->GetStringUTFChars(entityId, nullptr);
    std::string idStr(idChars);
    env->ReleaseStringUTFChars(entityId, idChars);

    NativeCadEntityData data;
    data.id = idStr;
    data.colorArgb = colorArgb;

    if (layerName) {
        const char* lChars = env->GetStringUTFChars(layerName, nullptr);
        data.layerName = lChars;
        env->ReleaseStringUTFChars(layerName, lChars);
    }
    if (coordinates) {
        jsize len = env->GetArrayLength(coordinates);
        data.coordinates.resize(len);
        env->GetFloatArrayRegion(coordinates, 0, len, data.coordinates.data());
    }

    return getOdaSdkAdapterInstance()->modifyEntity(nativeHandle, idStr, data);
}

JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeDeleteEntity(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jstring entityId
) {
    if (!entityId) return -1;
    const char* idChars = env->GetStringUTFChars(entityId, nullptr);
    std::string idStr(idChars);
    env->ReleaseStringUTFChars(entityId, idChars);

    return getOdaSdkAdapterInstance()->deleteEntity(nativeHandle, idStr);
}

JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeSaveDrawing(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jstring targetPath,
    jint dwgVersionCode
) {
    if (!targetPath) return -1;
    const char* pathChars = env->GetStringUTFChars(targetPath, nullptr);
    std::string pathStr(pathChars);
    env->ReleaseStringUTFChars(targetPath, pathChars);

    return getOdaSdkAdapterInstance()->saveDrawing(nativeHandle, pathStr, dwgVersionCode);
}

JNIEXPORT jint JNICALL
Java_com_example_cad_engine_nativebridge_NativeCadBridge_nativeComputeExtents(
    JNIEnv* env,
    jclass clazz,
    jlong nativeHandle,
    jfloatArray outBounds
) {
    if (!outBounds || env->GetArrayLength(outBounds) < 4) return -1;
    float bounds[4];
    int res = getOdaSdkAdapterInstance()->computeExtents(nativeHandle, bounds);
    if (res == 0) {
        env->SetFloatArrayRegion(outBounds, 0, 4, bounds);
    }
    return res;
}

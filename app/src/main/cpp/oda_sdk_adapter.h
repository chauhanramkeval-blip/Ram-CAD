/**
 * ============================================================================
 * Open Design Alliance (ODA) Drawings SDK Adapter Specification
 * ============================================================================
 *
 * This adapter encapsulates all direct interactions with the commercial ODA
 * C++ SDK (formerly Teigha / OpenDWG).
 *
 * When compiling with a valid ODA SDK license, define `-DUSE_ODA_SDK=1` in CMake.
 *
 * ODA Header References:
 * - OdaCommon.h: Core definitions and memory allocators
 * - DbDatabase.h: OdDbDatabase representing the AutoCAD drawing database
 * - DbHostAppServices.h: Platform host application services for file I/O
 * - DbBlockTableRecord.h: ModelSpace (*MODEL_SPACE) block record holding entities
 * - DbLine.h, DbCircle.h, DbArc.h, DbPolyline.h, DbText.h: Entity classes
 * - Gi/GiWorldDraw.h: Vectorization and rendering pipelines
 */

#ifndef ODA_SDK_ADAPTER_H
#define ODA_SDK_ADAPTER_H

#include <string>
#include <vector>
#include <cstdint>

struct NativeCadEntityData {
    std::string id;
    int type; // 1=Line, 2=Polyline, 3=Circle, 4=Arc, 5=Text, 6=Dimension
    std::string layerName;
    uint32_t colorArgb;
    float strokeWidth;
    std::vector<float> coordinates;
    std::string textContent;
    bool isClosed;
    float rotationDeg;
};

struct NativeCadLayerData {
    std::string id;
    std::string name;
    uint32_t colorArgb;
    bool isVisible;
    bool isLocked;
    float lineWeight;
};

class IOdaSdkAdapter {
public:
    virtual ~IOdaSdkAdapter() = default;

    virtual int initialize(const std::string& activationKey) = 0;
    virtual void release() = 0;

    virtual int64_t openDrawing(const std::string& filePath, bool readOnly, bool audit) = 0;
    virtual int closeDrawing(int64_t handle) = 0;

    virtual std::vector<NativeCadLayerData> getLayers(int64_t handle) = 0;
    virtual std::vector<NativeCadEntityData> getEntities(int64_t handle, int filterType) = 0;

    virtual std::string createEntity(int64_t handle, const NativeCadEntityData& entity) = 0;
    virtual int modifyEntity(int64_t handle, const std::string& entityId, const NativeCadEntityData& entity) = 0;
    virtual int deleteEntity(int64_t handle, const std::string& entityId) = 0;

    virtual int saveDrawing(int64_t handle, const std::string& targetPath, int dwgVersion) = 0;
    virtual int computeExtents(int64_t handle, float outBounds[4]) = 0;
};

/**
 * Factory method to obtain the active ODA SDK Adapter instance.
 */
IOdaSdkAdapter* getOdaSdkAdapterInstance();

#endif // ODA_SDK_ADAPTER_H

# Production DWG Support Architecture & Commercial CAD SDK Integration Guide

## 1. Architectural Overview

This CAD application uses a strictly decoupled, vendor-neutral multi-layer architecture.
The UI and application code never reference proprietary CAD SDK headers, JNI pointers, or vendor-specific data structures.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│   (CadEditorScreen, FileBrowserScreen, HomeScreen, CadEditorViewModel) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          CAD Engine Abstraction                        │
│   (CadEngineApi, CadDrawingSession, CadOpenOptions, CadEntityFilter)   │
└───────────┬───────────────────────┬───────────────────────┬────────────┘
            │                       │                       │
            ▼                       ▼                       ▼
┌───────────────────────┐ ┌──────────────────┐ ┌─────────────────────────┐
│  StandardCadEngineApi │ │   MockCadEngine  │ │      NativeCadEngine    │
│  - ASCII DXF Parser   │ │ (Dev & Test Only)│ │  (Production DWG SDK)   │
│  - Compose Renderer   │ │                  │ └────────────┬────────────┘
└───────────────────────┘ └──────────────────┘              │
                                                            ▼
                                              ┌───────────────────────────┐
                                              │    NativeCadBridge (JNI)  │
                                              │   (libcad_dwg_native.so)  │
                                              └─────────────┬─────────────┘
                                                            │
                                                            ▼
                                              ┌───────────────────────────┐
                                              │    Commercial CAD SDK     │
                                              │(Open Design Alliance ODA) │
                                              │(Teigha / RealDWG / Libre) │
                                              └───────────────────────────┘
```

---

## 2. Separation of Concerns (Requirement 5)

The CAD architecture is explicitly organized into distinct modules:

| Subsystem | Package / Path | Responsibility |
| :--- | :--- | :--- |
| **UI Layer** | `com.example.ui.*` | Jetpack Compose presentation, gestures, sheets, dialogs. Zero CAD SDK knowledge. |
| **CAD Engine Core** | `com.example.cad.engine.api.*`, `core.*` | Drawing sessions, lifecycle, layer query, entity CRUD, DWG licensing validation. |
| **Renderer** | `com.example.cad.engine.renderer.*` | Drafting grid, origin axes, crosshair coordinate callouts, Compose Vector geometry. |
| **File Manager** | `com.example.cad.engine.files.*` | Projects storage, cache, SAF URI streams, DWG magic byte header inspection. |
| **Measurement Engine** | `com.example.cad.engine.measurement.*` | Distance, delta X/Y, polyline length, angles, Shoelace polygon area calculation. |
| **Editing Engine** | `com.example.cad.engine.editor.*` | Transaction history, Undo/Redo stack, layer visibility/locking state. |
| **Native Bridge** | `com.example.cad.engine.nativebridge.*` | JNI interface bindings, DTO marshaling across C++/Kotlin boundary. |
| **Dev Mock Engine** | `com.example.cad.engine.mock.*` | Synthetic mock drawings for unit testing and offline development. |

---

## 3. Strict Realism Rule (Requirement 6)

Binary `.dwg` files are proprietary binary database streams (AC1015 through AC1032) encrypted with Autodesk bit-packing and cyclic redundancy checks. 
**The engine does NOT pretend that binary DWG files can be parsed without a licensed CAD SDK.**

- When an actual binary `.dwg` file is opened without the native SDK linked, the engine raises:
  `com.example.cad.engine.api.CadDwgEngineRequiredException`
- In development/test mode, the app uses `MockCadEngine` with explicit `[DEV MOCK]` labeling for simulated geometry.

---

## 4. Step-by-Step Integration with Open Design Alliance (ODA) SDK

The Open Design Alliance (ODA) provides the industry-standard C++ SDK for reading and writing AutoCAD `.dwg` files on mobile and desktop platforms.

### Step 4.1: Obtain ODA Membership & Android SDK
1. Register at [opendesign.com](https://www.opendesign.com/) and obtain an ODA Drawings SDK license.
2. Download the Android NDK package (e.g. `ODA_Drawings_Android_arm64-v8a_release.tar.gz`).
3. Place header files into:
   ```
   app/src/main/cpp/oda_sdk/include/
   ```
4. Place precompiled shared libraries into:
   ```
   app/src/main/cpp/oda_sdk/lib/arm64-v8a/
   app/src/main/cpp/oda_sdk/lib/armeabi-v7a/
   app/src/main/cpp/oda_sdk/lib/x86_64/
   ```

### Step 4.2: Enable CMake in `app/build.gradle.kts`
Uncomment the external native build configuration in `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += "-DUSE_ODA_SDK=ON"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
```

### Step 4.3: Implement ODA C++ Lifecycle in `oda_sdk_adapter.cpp`
In `app/src/main/cpp/oda_sdk_adapter.cpp`:
1. Include ODA headers:
   ```cpp
   #include "OdaCommon.h"
   #include "DbDatabase.h"
   #include "DbHostAppServices.h"
   #include "DbBlockTableRecord.h"
   #include "DbLine.h"
   #include "DbCircle.h"
   #include "DbArc.h"
   #include "DbPolyline.h"
   #include "DbText.h"
   ```
2. Initialize ODA on application launch:
   ```cpp
   static MyHostAppServices g_hostAppServices;
   odInitialize(&g_hostAppServices);
   odActivate("YOUR_ODA_ACTIVATION_KEY");
   ```
3. Open DWG:
   ```cpp
   OdDbDatabasePtr pDb = g_hostAppServices.readFile(filePath.c_str(), !readOnly);
   return reinterpret_cast<int64_t>(pDb.detach());
   ```
4. Query Entities:
   ```cpp
   OdDbBlockTableRecordPtr pModelSpace = pDb->getModelSpaceId().safeOpenObject();
   OdDbBlockTableRecordIteratorPtr pIter = pModelSpace->newIterator();
   for (; !pIter->done(); pIter->step()) {
       OdDbEntityPtr pEnt = pIter->getEntityId().safeOpenObject();
       if (pEnt->isKindOf(OdDbLine::desc())) {
           OdDbLinePtr pLine = pEnt;
           // Extract pLine->startPoint() and pLine->endPoint()
       }
   }
   ```
5. Save DWG:
   ```cpp
   pDb->writeFile(targetPath.c_str(), OdDb::kDwgVer2018);
   ```

### Step 4.4: Activate via `NativeCadBridge`
In Kotlin:
```kotlin
val result = NativeCadBridge.initializeSdk("YOUR_ODA_ACTIVATION_KEY")
if (result == 0) {
    Log.i("CAD", "ODA SDK successfully activated.")
}
```

---

## 5. Touchpoints File Checklist

| File | Purpose |
| :--- | :--- |
| `app/src/main/java/com/example/cad/engine/api/CadEngineApi.kt` | Vendor-neutral abstraction with all 9 core methods |
| `app/src/main/java/com/example/cad/engine/nativebridge/NativeCadBridge.kt` | JNI dynamic loader and external C++ method declarations |
| `app/src/main/java/com/example/cad/engine/nativebridge/NativeCadEngine.kt` | Kotlin implementation delegating directly to native C++ |
| `app/src/main/cpp/CMakeLists.txt` | CMake build script linking `cad_dwg_native` & ODA `.so` libraries |
| `app/src/main/cpp/cad_native_bridge.h` / `.cpp` | JNI native entry points |
| `app/src/main/cpp/oda_sdk_adapter.h` / `.cpp` | Commercial ODA SDK adapter classes |
| `app/src/main/java/com/example/cad/engine/mock/MockCadEngine.kt` | Safe mock engine for testing without ODA license |
| `app/src/main/java/com/example/cad/engine/files/CadFileManager.kt` | File storage and DWG binary header inspection |

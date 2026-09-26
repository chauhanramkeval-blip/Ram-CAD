# ==============================================================================
# CAD Mobile - CMake Configuration for Native CAD Engine (ODA SDK Bridge)
# ==============================================================================
cmake_minimum_required(VERSION 3.22.1)

project("cad_dwg_native")

# Set C++20 standard
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ------------------------------------------------------------------------------
# Commercial ODA (Open Design Alliance) SDK Configuration
# ------------------------------------------------------------------------------
# To link the licensed ODA Drawings SDK:
# 1. Place the ODA Android SDK archive under `app/libs/oda_sdk/`
# 2. Set -DUSE_ODA_SDK=ON when invoking CMake
# ------------------------------------------------------------------------------
option(USE_ODA_SDK "Build with licensed Open Design Alliance ODA SDK" OFF)

set(ODA_INCLUDE_DIRS
    "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/include"
    "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/include/Kernel"
    "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/include/Drawing"
)

if (USE_ODA_SDK)
    message(STATUS "Configuring build with licensed Open Design Alliance (ODA) SDK")
    add_definitions(-DUSE_ODA_SDK=1)
    include_directories(${ODA_INCLUDE_DIRS})
else()
    message(STATUS "Configuring build with Native CAD Engine Bridge Stub (Safe Fallback)")
endif()

# Primary JNI bridge library
add_library(
    cad_dwg_native
    SHARED
    cad_native_bridge.cpp
    oda_sdk_adapter.cpp
)

# Standard Android NDK libraries
find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    cad_dwg_native
    ${log-lib}
    ${android-lib}
)

if (USE_ODA_SDK)
    # Link ODA precompiled shared libraries (.so)
    target_link_libraries(
        cad_dwg_native
        "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/lib/${ANDROID_ABI}/libTD_Db.so"
        "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/lib/${ANDROID_ABI}/libTD_Root.so"
        "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/lib/${ANDROID_ABI}/libTD_Ge.so"
        "${CMAKE_CURRENT_SOURCE_DIR}/oda_sdk/lib/${ANDROID_ABI}/libTD_Gi.so"
    )
endif()

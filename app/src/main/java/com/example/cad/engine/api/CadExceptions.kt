package com.example.cad.engine.api

/**
 * Base exception for CAD engine subsystem failures.
 */
open class CadEngineException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception explicitly raised when an operation requires a commercial/native DWG SDK
 * (such as Open Design Alliance ODA Drawings SDK) which has not yet been linked or licensed.
 *
 * This ensures the application never silently pretends that proprietary binary AutoCAD DWG files
 * are decoded when only pure-Kotlin or mock layers are active.
 */
class CadDwgEngineRequiredException(
    val filePath: String,
    message: String = "Commercial CAD SDK (such as Open Design Alliance / ODA Drawings SDK) is required to decode binary AutoCAD DWG files ('$filePath'). In development mode, mock sample drawings can be loaded for testing, or standard ASCII DXF drawings can be opened natively."
) : CadEngineException(message)

/**
 * Exception thrown when an operation is attempted on an invalid or closed drawing session.
 */
class CadSessionClosedException(sessionId: String) :
    CadEngineException("CAD Drawing session '$sessionId' is closed or invalid.")

/**
 * Exception thrown when an entity is not found in the drawing.
 */
class CadEntityNotFoundException(entityId: String) :
    CadEngineException("CAD entity with ID '$entityId' was not found in the drawing.")

/**
 * Exception thrown when a file format is not supported by the active engine.
 */
class CadUnsupportedFormatException(format: String, engineName: String) :
    CadEngineException("Format '$format' is not supported by active engine '$engineName'.")

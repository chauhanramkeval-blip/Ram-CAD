package com.example.cad.parser.dxf

/**
 * Exception thrown when a DXF file cannot be parsed due to invalid format or corruption.
 */
class DxfParseException(
    message: String,
    cause: Throwable? = null,
    val lineNumber: Int = -1
) : Exception(
    if (lineNumber > 0) "Line $lineNumber: $message" else message,
    cause
)

package com.example.cad.engine.api

import com.example.cad.model.CadBoundingBox

/**
 * Filter criteria for querying CAD entities from an active drawing session.
 */
sealed class CadEntityFilter {
    /** Retrieve all entities in the active space (ModelSpace) */
    object All : CadEntityFilter()

    /** Retrieve entities belonging to a specific layer */
    data class ByLayer(val layerId: String) : CadEntityFilter()

    /** Retrieve entities of a specific primitive type (Line, Polyline, Circle, Arc, etc.) */
    data class ByType(val entityType: String) : CadEntityFilter()

    /** Retrieve entities intersecting or contained within a bounding box */
    data class ByBoundingBox(val bounds: CadBoundingBox) : CadEntityFilter()

    /** Retrieve visible entities only (excludes hidden layers) */
    object VisibleOnly : CadEntityFilter()
}

package com.example.cad.engine

import com.example.cad.model.CadPoint2D
import com.example.cad.model.CadUnit

sealed class MeasurementResult {
    data class Distance(
        val value: Double,
        val unit: CadUnit,
        val deltaX: Double,
        val deltaY: Double
    ) : MeasurementResult() {
        val formatted: String get() = unit.format(value)
    }

    data class Angle(
        val degrees: Double,
        val radians: Double
    ) : MeasurementResult() {
        val formatted: String get() = "%.2f°".format(degrees)
    }

    data class Area(
        val valueSquareUnits: Double,
        val unit: CadUnit,
        val perimeter: Double
    ) : MeasurementResult() {
        val formattedArea: String get() = "%.2f %s²".format(valueSquareUnits, unit.abbreviation)
        val formattedPerimeter: String get() = unit.format(perimeter)
    }
}

/**
 * Modular CAD calculation engine for geometric measurement, snapping, and dimensioning.
 */
interface CadMeasurementEngine {
    fun measureDistance(p1: CadPoint2D, p2: CadPoint2D, unit: CadUnit): MeasurementResult.Distance
    fun measureAngle(vertex: CadPoint2D, p1: CadPoint2D, p2: CadPoint2D): MeasurementResult.Angle
    fun measurePolylineLength(points: List<CadPoint2D>, unit: CadUnit): MeasurementResult.Distance
    fun measurePolygonArea(points: List<CadPoint2D>, unit: CadUnit): MeasurementResult.Area
}

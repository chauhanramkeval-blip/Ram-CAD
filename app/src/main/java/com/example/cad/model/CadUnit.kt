package com.example.cad.model

/**
 * Standard measurement units for technical CAD drawings.
 */
enum class CadUnit(
    val abbreviation: String,
    val displayName: String,
    val conversionToMm: Double
) {
    MILLIMETERS("mm", "Millimeters", 1.0),
    CENTIMETERS("cm", "Centimeters", 10.0),
    METERS("m", "Meters", 1000.0),
    INCHES("in", "Inches", 25.4),
    FEET("ft", "Feet", 304.8);

    val areaAbbreviation: String
        get() = when (this) {
            MILLIMETERS -> "mm²"
            CENTIMETERS -> "cm²"
            METERS -> "m²"
            INCHES -> "in²"
            FEET -> "ft²"
        }

    fun format(value: Double, decimals: Int = 2): String {
        return "%.${decimals}f %s".format(value, abbreviation)
    }

    fun formatArea(value: Double, decimals: Int = 2): String {
        return "%.${decimals}f %s".format(value, areaAbbreviation)
    }

    fun convertFromMm(valueInMm: Double): Double {
        return valueInMm / conversionToMm
    }

    fun convertToMm(valueInUnit: Double): Double {
        return valueInUnit * conversionToMm
    }

    fun convertAreaFromMm2(areaInMm2: Double): Double {
        return areaInMm2 / (conversionToMm * conversionToMm)
    }

    fun convertAreaToMm2(areaInUnit: Double): Double {
        return areaInUnit * (conversionToMm * conversionToMm)
    }
}

package com.example.locus.utils

// PostgreSQL POINT columns come back as "(lat, lon)" — strip parens and spaces before parsing.
fun String?.parseLatLon(): Pair<Double, Double>? {
    if (isNullOrBlank()) return null
    val clean = trim().removePrefix("(").removeSuffix(")")
    val parts = clean.split(",")
    val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: return null
    val lon = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: return null
    return Pair(lat, lon)
}

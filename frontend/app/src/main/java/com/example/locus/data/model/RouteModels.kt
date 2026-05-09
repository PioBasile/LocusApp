package com.example.locus.data.model

enum class ActivityCategory(val label: String) {
    DINING("Dining"),
    LEISURE("Leisure"),
    DISCOVERY("Discovery"),
    CULTURE("Culture")
}

enum class EffortLevel(val label: String) {
    LOW("Low"),
    MEDIUM("Moderate"),
    HIGH("Intense")
}

enum class RouteType(val label: String) {
    ECONOMIC("Budget"),
    BALANCED("Balanced"),
    COMFORT("Comfort")
}

enum class TimeSlot(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening")
}

data class RouteStep(
    val id: Int,
    val name: String,
    val address: String,
    val category: ActivityCategory,
    val timeSlot: TimeSlot,
    val distanceFromPrev: String,
    val durationMinutes: Int,
    val openingHours: String,
    val imageUrl: String = "",
    val price: Int
)

data class RouteOption(
    val id: Int,
    val type: RouteType,
    val steps: List<RouteStep>,
    val totalBudget: Int,
    val totalDurationMinutes: Int,
    val effortScore: Int,
    val weatherSensitive: Boolean
)

data class RoutePlanPreferences(
    val activities: Set<ActivityCategory> = setOf(ActivityCategory.DINING, ActivityCategory.LEISURE),
    val favoritePlaces: List<String> = emptyList(),
    val maxBudget: Int = 100,
    val durationHours: Int = 4,
    val effort: EffortLevel = EffortLevel.MEDIUM,
    val anyWeather: Boolean = false
)

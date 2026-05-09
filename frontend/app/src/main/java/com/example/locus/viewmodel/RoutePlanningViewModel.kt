package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlanningState { IDLE, PLANNING, GENERATING, OPTIONS, DETAIL }

class RoutePlanningViewModel : ViewModel() {

    var state by mutableStateOf(PlanningState.IDLE)
        private set

    var preferences by mutableStateOf(RoutePlanPreferences())

    var routes by mutableStateOf<List<RouteOption>>(emptyList())
        private set

    var selectedRoute by mutableStateOf<RouteOption?>(null)
        private set

    var likedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var dislikedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var isSaved by mutableStateOf(false)
        private set

    fun open() { state = PlanningState.PLANNING }

    fun generate(prefs: RoutePlanPreferences) {
        preferences = prefs
        state = PlanningState.GENERATING
        viewModelScope.launch {
            delay(2000)
            routes = buildMockRoutes()
            state = PlanningState.OPTIONS
        }
    }

    fun regenerate() {
        state = PlanningState.GENERATING
        viewModelScope.launch {
            delay(1500)
            routes = buildMockRoutes()
            state = PlanningState.OPTIONS
        }
    }

    fun select(route: RouteOption) { selectedRoute = route; state = PlanningState.DETAIL }
    fun like(id: Int) { likedIds = likedIds + id; dislikedIds = dislikedIds - id }
    fun dislike(id: Int) { dislikedIds = dislikedIds + id; likedIds = likedIds - id }
    fun toggleSave() { isSaved = !isSaved }
    fun backToOptions() { selectedRoute = null; state = PlanningState.OPTIONS }
    fun close() { selectedRoute = null; state = PlanningState.IDLE }

    private fun buildMockRoutes() = listOf(
        RouteOption(
            id = 1, type = RouteType.ECONOMIC,
            steps = listOf(
                RouteStep(1, "Local Market", "Republic Square", ActivityCategory.DISCOVERY, TimeSlot.MORNING, "Start", 45, "8am–1pm", price = 0),
                RouteStep(2, "City Park", "Garden Lane", ActivityCategory.LEISURE, TimeSlot.MORNING, "800m · 10 min", 60, "Open 24/7", price = 0),
                RouteStep(3, "Corner Deli", "12 Flower Street", ActivityCategory.DINING, TimeSlot.AFTERNOON, "300m · 4 min", 30, "7am–7pm", price = 7),
                RouteStep(4, "City Museum", "1 Town Hall Square", ActivityCategory.CULTURE, TimeSlot.AFTERNOON, "1.2km · 15 min", 90, "10am–6pm, closed Mon", price = 0),
            ),
            totalBudget = 7, totalDurationMinutes = 225, effortScore = 2, weatherSensitive = true
        ),
        RouteOption(
            id = 2, type = RouteType.BALANCED,
            steps = listOf(
                RouteStep(5, "Terrace Café", "3 Main Boulevard", ActivityCategory.DINING, TimeSlot.MORNING, "Start", 45, "8am–10pm", price = 12),
                RouteStep(6, "Contemporary Art Gallery", "8 Peace Street", ActivityCategory.CULTURE, TimeSlot.MORNING, "500m · 7 min", 75, "10am–7pm", price = 10),
                RouteStep(7, "Market Brasserie", "22 Market Street", ActivityCategory.DINING, TimeSlot.AFTERNOON, "400m · 5 min", 60, "12pm–2:30pm", price = 22),
                RouteStep(8, "Pottery Workshop", "15 Arts Lane", ActivityCategory.LEISURE, TimeSlot.AFTERNOON, "700m · 9 min", 90, "2pm–6:30pm", price = 18),
                RouteStep(9, "Tapas Bar", "6 Central Square", ActivityCategory.DINING, TimeSlot.EVENING, "600m · 8 min", 60, "5pm–midnight", price = 20),
            ),
            totalBudget = 82, totalDurationMinutes = 330, effortScore = 3, weatherSensitive = false
        ),
        RouteOption(
            id = 3, type = RouteType.COMFORT,
            steps = listOf(
                RouteStep(10, "Gourmet Brunch", "The Grand Hotel", ActivityCategory.DINING, TimeSlot.MORNING, "Start", 90, "10am–2pm", price = 45),
                RouteStep(11, "Private Museum Tour", "Fine Arts Museum", ActivityCategory.CULTURE, TimeSlot.AFTERNOON, "1km · 5 min taxi", 120, "By appointment", price = 35),
                RouteStep(12, "Spa & Wellness", "Royal Spa", ActivityCategory.LEISURE, TimeSlot.AFTERNOON, "800m · 4 min taxi", 90, "10am–8pm", price = 60),
                RouteStep(13, "Fine Dining ⭐", "La Maison", ActivityCategory.DINING, TimeSlot.EVENING, "500m · 3 min taxi", 120, "7pm–11pm", price = 85),
            ),
            totalBudget = 225, totalDurationMinutes = 420, effortScore = 1, weatherSensitive = false
        )
    )
}

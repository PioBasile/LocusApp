package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.*
import com.example.locus.data.remote.*
import com.example.locus.data.remote.RetrofitClient
import com.example.locus.data.repository.TravelPathRepository
import kotlinx.coroutines.launch

enum class PlanningState { IDLE, PLANNING, GENERATING, OPTIONS, DETAIL, PLACE_DETAIL }

class RoutePlanningViewModel : ViewModel() {

    private val repo = TravelPathRepository(RetrofitClient.api)

    var state by mutableStateOf(PlanningState.IDLE)
        private set

    var preferences by mutableStateOf(RoutePlanPreferences())

    var routes by mutableStateOf<List<RouteOption>>(emptyList())
        private set

    private var rawRoutes = listOf<ItineraireResponse>()

    var selectedRoute by mutableStateOf<RouteOption?>(null)
        private set

    private var selectedRawIndex = -1

    var likedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var dislikedIds by mutableStateOf<Set<Int>>(emptySet())
        private set

    var isSaved by mutableStateOf(false)
        private set

    var savedItineraries by mutableStateOf<List<SavedItinResponse>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var selectedLieuId by mutableStateOf(-1)
        private set

    // Injected from MapScreen once location + auth are available
    var gps: String = ""
    var token: String = ""

    fun open() { state = PlanningState.PLANNING }

    fun generate(prefs: RoutePlanPreferences) {
        preferences = prefs
        state = PlanningState.GENERATING
        error = null
        viewModelScope.launch {
            val request = ItineraireRequest(
                gps = gps.ifEmpty { "43.6088,3.8783" },
                categories = prefs.activities.flatMap { it.toBackendCategories() },
                budget_max = prefs.maxBudget,
                duree_heures = prefs.durationHours,
                effort = prefs.effort.toBackend(),
                tout_temps = prefs.anyWeather,
                lieux_favoris = prefs.favoritePlaces
            )
            Log.d("RoutePlanning", "generate request: gps=${request.gps} categories=${request.categories} budget=${request.budget_max} duration=${request.duree_heures}h effort=${request.effort}")
            repo.generateItineraires(request).fold(
                onSuccess = { response ->
                    Log.d("RoutePlanning", "generate success: ${response.itineraires.size} routes")
                    rawRoutes = response.itineraires
                    routes = response.itineraires.mapIndexed { i, itin -> itin.toRouteOption(i + 1) }
                    state = PlanningState.OPTIONS
                },
                onFailure = {
                    Log.e("RoutePlanning", "generate failed: ${it.javaClass.simpleName} — ${it.message}", it)
                    error = it.message ?: "Failed to generate routes."
                    routes = emptyList()
                    state = PlanningState.OPTIONS
                }
            )
        }
    }

    fun regenerate() {
        state = PlanningState.GENERATING
        error = null
        viewModelScope.launch {
            val request = ItineraireRequest(
                gps = gps.ifEmpty { "43.6088,3.8783" },
                categories = preferences.activities.flatMap { it.toBackendCategories() },
                budget_max = preferences.maxBudget,
                duree_heures = preferences.durationHours,
                effort = preferences.effort.toBackend(),
                tout_temps = preferences.anyWeather,
                lieux_favoris = preferences.favoritePlaces
            )
            repo.generateItineraires(request).fold(
                onSuccess = { response ->
                    rawRoutes = response.itineraires
                    routes = response.itineraires.mapIndexed { i, itin -> itin.toRouteOption(i + 1) }
                    state = PlanningState.OPTIONS
                },
                onFailure = {
                    Log.e("RoutePlanning", "regenerate failed", it)
                    error = it.message ?: "Failed to regenerate routes."
                    state = PlanningState.OPTIONS
                }
            )
        }
    }

    fun select(route: RouteOption) {
        selectedRoute = route
        selectedRawIndex = routes.indexOf(route)
        isSaved = false
        state = PlanningState.DETAIL
    }

    fun like(id: Int) { likedIds = likedIds + id; dislikedIds = dislikedIds - id }
    fun dislike(id: Int) { dislikedIds = dislikedIds + id; likedIds = likedIds - id }

    fun toggleSave() {
        if (isSaved) { isSaved = false; return }
        val idx = selectedRawIndex
        if (token.isBlank() || idx < 0 || idx >= rawRoutes.size) { isSaved = true; return }
        viewModelScope.launch {
            repo.saveItineraire(token, rawRoutes[idx]).fold(
                onSuccess = { isSaved = true },
                onFailure = { isSaved = false }
            )
        }
    }

    fun loadSavedItineraries() {
        if (token.isBlank()) return
        viewModelScope.launch {
            repo.getSavedItineraires(token).fold(
                onSuccess = { savedItineraries = it },
                onFailure = {}
            )
        }
    }

    fun selectPlace(lieuId: Int) { selectedLieuId = lieuId; state = PlanningState.PLACE_DETAIL }
    fun backFromPlace() { selectedLieuId = -1; state = PlanningState.DETAIL }
    fun addToFavorites(placeName: String) {
        if (placeName.isNotBlank() && placeName !in preferences.favoritePlaces) {
            preferences = preferences.copy(favoritePlaces = preferences.favoritePlaces + placeName)
        }
    }

    fun backToOptions() { selectedRoute = null; state = PlanningState.OPTIONS }
    fun close() { selectedRoute = null; state = PlanningState.IDLE; error = null }
}

// -- Mapping helpers -----------------------------------------------------------

private fun ActivityCategory.toBackendCategories(): List<String> = when (this) {
    ActivityCategory.DINING    -> listOf("restaurant", "bar", "cafe")
    ActivityCategory.LEISURE   -> listOf("sport", "shopping", "plage", "hotel")
    ActivityCategory.DISCOVERY -> listOf("monument", "parc", "autre")
    ActivityCategory.CULTURE   -> listOf("musee")
}

private fun EffortLevel.toBackend(): String = when (this) {
    EffortLevel.LOW    -> "faible"
    EffortLevel.MEDIUM -> "modere"
    EffortLevel.HIGH   -> "eleve"
}

private fun String.toActivityCategory(): ActivityCategory = when (this) {
    "restaurant", "bar", "cafe" -> ActivityCategory.DINING
    "musee"                     -> ActivityCategory.CULTURE
    "monument", "parc", "autre" -> ActivityCategory.DISCOVERY
    else                        -> ActivityCategory.LEISURE
}

private fun String.toRouteType(): RouteType = when (this) {
    "economique" -> RouteType.ECONOMIC
    "equilibre"  -> RouteType.BALANCED
    else         -> RouteType.COMFORT
}

private fun String.toTimeSlot(): TimeSlot = when (this) {
    "matin"      -> TimeSlot.MORNING
    "apres-midi" -> TimeSlot.AFTERNOON
    else         -> TimeSlot.EVENING
}

private fun ItineraireResponse.toRouteOption(fallbackId: Int): RouteOption = RouteOption(
    id = if (id != 0) id else fallbackId,
    type = type.toRouteType(),
    nom = nom,
    steps = etapes.mapIndexed { idx, etape ->
        val dist = if (idx == 0) "Start"
        else "%.1fkm · %dmin".format(etape.distancePrevKm, etape.tempsTrajMin)
        RouteStep(
            id = etape.idLieu * 100 + etape.ordre,
            lieuId = etape.idLieu,
            name = etape.nomLieu,
            address = etape.adresseLieu,
            category = etape.categorie.toActivityCategory(),
            timeSlot = etape.creneau.toTimeSlot(),
            distanceFromPrev = dist,
            durationMinutes = etape.dureeMinutes,
            openingHours = etape.horaires.ifEmpty { "Open" },
            imageUrl = etape.urlImage,
            price = etape.prix,
            rating = etape.note,
            gps = etape.gpsLieu
        )
    },
    totalBudget = budgetTotal,
    totalDurationMinutes = dureeMinutes,
    effortScore = effortScore,
    weatherSensitive = meteoSensible,
    resume = resume
)

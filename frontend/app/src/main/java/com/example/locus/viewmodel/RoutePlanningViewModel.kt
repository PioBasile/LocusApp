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
import com.example.locus.utils.parseLatLon
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.locus.data.remote.CreateLieuRequest
import org.json.JSONObject

enum class PlanningState { IDLE, PLANNING, GENERATING, OPTIONS, DETAIL, PLACE_DETAIL, SAVED_ROUTES, EXPLORE_ITINS, PLACES }

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

    var savedItineraireId by mutableStateOf(-1)
        private set

    var isCurrentItinLiked by mutableStateOf(false)
        private set

    var savedItineraries by mutableStateOf<List<SavedItinResponse>>(emptyList())
        private set

    var weather by mutableStateOf<WeatherResponse?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var selectedLieuId by mutableStateOf(-1)
        private set

    var places by mutableStateOf<List<LieuResponse>>(emptyList())
        private set
    var isLoadingPlaces by mutableStateOf(false)
        private set

    var itinSearchResults by mutableStateOf<List<SavedItinResponse>>(emptyList())
        private set
    var isSearchingItins by mutableStateOf(false)
        private set

    var nearbyPosts by mutableStateOf<List<SearchPostResult>>(emptyList())
        private set

    var focusedPlaceGps: String? by mutableStateOf(null)
        private set

    // lieuId → "lat,lon" populated when a route is selected
    var stepGpsMap by mutableStateOf<Map<Int, String>>(emptyMap())
        private set

    // Navigation (Directions API result)
    var showNavigationStart by mutableStateOf(false)
        private set
    var navigationPoints by mutableStateOf<List<Point>>(emptyList())
        private set
    var isLoadingNavRoute by mutableStateOf(false)
        private set
    var navRouteError by mutableStateOf<String?>(null)
        private set
    var startSuggestions by mutableStateOf<List<LieuResponse>>(emptyList())
        private set

    // Map search bar
    var mapSearchQuery by mutableStateOf("")
        private set
    var mapSearchResults by mutableStateOf<List<LieuResponse>>(emptyList())
        private set
    var isSearchingMap by mutableStateOf(false)
        private set
    private var mapSearchJob: Job? = null

    private var backFromDetailState = PlanningState.OPTIONS
    private var backFromPlaceState = PlanningState.DETAIL

    // Injected from MapScreen once location + auth are available
    var gps: String = ""
    var token: String = ""
    var mapboxToken: String = ""

    val currentLat: Double get() = gps.split(",").getOrNull(0)?.toDoubleOrNull() ?: 0.0
    val currentLon: Double get() = gps.split(",").getOrNull(1)?.toDoubleOrNull() ?: 0.0
    val canRegenerate: Boolean get() = selectedRawIndex >= 0

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
        savedItineraireId = -1
        isCurrentItinLiked = false
        backFromDetailState = PlanningState.OPTIONS
        backFromPlaceState = PlanningState.DETAIL
        state = PlanningState.DETAIL
        loadStepGps(route)
    }

    fun selectSavedRoute(saved: SavedItinResponse) {
        val route = saved.itineraire.copy(nom = saved.nom).toRouteOption(saved.id)
        selectedRoute = route
        selectedRawIndex = -1
        isSaved = true
        savedItineraireId = saved.id
        isCurrentItinLiked = false
        backFromDetailState = PlanningState.SAVED_ROUTES
        backFromPlaceState = PlanningState.DETAIL
        state = PlanningState.DETAIL
        loadStepGps(route)
    }

    private fun loadStepGps(route: RouteOption) {
        stepGpsMap = emptyMap()
        viewModelScope.launch {
            val result = mutableMapOf<Int, String>()
            // Use GPS already in the step if present, otherwise fetch from API
            route.steps.filter { it.lieuId > 0 }.forEach { step ->
                val gps = step.gps.ifBlank {
                    repo.getLieu(step.lieuId).getOrNull()?.gps
                }
                if (!gps.isNullOrBlank()) result[step.lieuId] = gps
            }
            stepGpsMap = result
        }
    }

    fun like(id: Int) { likedIds = likedIds + id; dislikedIds = dislikedIds - id }
    fun dislike(id: Int) { dislikedIds = dislikedIds + id; likedIds = likedIds - id }

    fun toggleSave() {
        if (isSaved) { isSaved = false; savedItineraireId = -1; isCurrentItinLiked = false; return }
        val idx = selectedRawIndex
        if (token.isBlank() || idx < 0 || idx >= rawRoutes.size) { isSaved = true; return }
        viewModelScope.launch {
            repo.saveItineraire(token, rawRoutes[idx]).fold(
                onSuccess = { response -> isSaved = true; savedItineraireId = response.id },
                onFailure = { isSaved = false }
            )
        }
    }

    fun toggleItineraireLike() {
        if (token.isBlank() || savedItineraireId < 0) return
        viewModelScope.launch {
            if (isCurrentItinLiked) {
                repo.unlikeItineraire(token, savedItineraireId).fold(
                    onSuccess = { isCurrentItinLiked = false },
                    onFailure = {}
                )
            } else {
                repo.likeItineraire(token, savedItineraireId).fold(
                    onSuccess = { isCurrentItinLiked = true },
                    onFailure = {}
                )
            }
        }
    }

    fun shareItineraire(onUrl: (String) -> Unit) {
        if (savedItineraireId < 0) return
        viewModelScope.launch {
            repo.getShareItineraire(savedItineraireId).fold(
                onSuccess = { response -> onUrl(response.publicUrl) },
                onFailure = {}
            )
        }
    }

    fun loadWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            repo.getWeather(lat, lon).fold(
                onSuccess = { weather = it },
                onFailure = {}
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

    fun selectPlace(lieuId: Int, gps: String = "") {
        selectedLieuId = lieuId
        if (gps.isNotBlank()) focusedPlaceGps = gps
        backFromPlaceState = PlanningState.DETAIL
        state = PlanningState.PLACE_DETAIL
    }

    fun clearFocusedPlaceGps() { focusedPlaceGps = null }
    fun setFocusedGps(gps: String) { focusedPlaceGps = gps }

    fun openNavigationStart() { showNavigationStart = true; navRouteError = null }
    fun closeNavigationStart() { showNavigationStart = false; startSuggestions = emptyList() }
    fun clearNavigation() { navigationPoints = emptyList() }

    fun searchStartLocations(q: String) {
        if (q.isBlank()) { startSuggestions = emptyList(); return }
        viewModelScope.launch {
            repo.getLieux(lat = currentLat.takeIf { it != 0.0 }, lon = currentLon.takeIf { it != 0.0 }, radiusKm = null, categorie = null, q = q, limit = 6).fold(
                onSuccess = { startSuggestions = it },
                onFailure = { startSuggestions = emptyList() }
            )
        }
    }

    fun fetchDirectionsRoute(startGps: String, mapboxToken: String) {
        val route = selectedRoute ?: return
        viewModelScope.launch {
            isLoadingNavRoute = true
            navRouteError = null
            try {
                // Resolve GPS for every step, fetching from API if not cached
                val resolvedStepGps = mutableListOf<String>()
                for (step in route.steps) {
                    val gps = stepGpsMap[step.lieuId]
                        ?: step.gps.ifBlank { null }
                        ?: if (step.lieuId > 0) repo.getLieu(step.lieuId).getOrNull()?.gps else null
                    if (!gps.isNullOrBlank()) {
                        resolvedStepGps.add(gps)
                        if (step.lieuId > 0) stepGpsMap = stepGpsMap + (step.lieuId to gps)
                    }
                }

                val (startLat, startLon) = startGps.parseLatLon() ?: run {
                    navRouteError = "Invalid starting location"
                    isLoadingNavRoute = false
                    return@launch
                }

                val waypointCoords = mutableListOf("$startLon,$startLat")
                for (gps in resolvedStepGps) {
                    val (lat, lon) = gps.parseLatLon() ?: continue
                    waypointCoords.add("$lon,$lat")
                }

                if (waypointCoords.size < 2) {
                    navRouteError = "Could not find GPS coordinates for the route steps"
                    isLoadingNavRoute = false
                    return@launch
                }

                // Mapbox Directions allows max 25 waypoints
                val coords = waypointCoords.take(25).joinToString(";")
                val url = "https://api.mapbox.com/directions/v5/mapbox/walking/$coords" +
                    "?geometries=geojson&overview=full&access_token=$mapboxToken"
                val json = withContext(Dispatchers.IO) { java.net.URL(url).readText() }
                val routesArr = JSONObject(json).getJSONArray("routes")
                if (routesArr.length() == 0) {
                    navRouteError = "No walking route found between these locations"
                    isLoadingNavRoute = false
                    return@launch
                }
                val coordsArr = routesArr.getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")
                navigationPoints = (0 until coordsArr.length()).map { i ->
                    val c = coordsArr.getJSONArray(i)
                    Point.fromLngLat(c.getDouble(0), c.getDouble(1))
                }
                showNavigationStart = false
                startSuggestions = emptyList()
            } catch (e: Exception) {
                navRouteError = e.localizedMessage ?: "Failed to load route"
            }
            isLoadingNavRoute = false
        }
    }

    fun startNavigation() {
        if (gps.isBlank()) { navRouteError = "GPS not available — enable location first"; return }
        state = PlanningState.IDLE
        navRouteError = null
        fetchDirectionsRoute(gps, mapboxToken)
    }

    fun fetchDirectRouteFromTo(fromGps: String, toGps: String) {
        viewModelScope.launch {
            isLoadingNavRoute = true
            navRouteError = null
            try {
                val (fromLat, fromLon) = fromGps.parseLatLon() ?: run {
                    navRouteError = "Invalid starting location"; isLoadingNavRoute = false; return@launch
                }
                val (toLat, toLon) = toGps.parseLatLon() ?: run {
                    navRouteError = "Invalid destination"; isLoadingNavRoute = false; return@launch
                }
                val coords = "$fromLon,$fromLat;$toLon,$toLat"
                val url = "https://api.mapbox.com/directions/v5/mapbox/walking/$coords" +
                    "?geometries=geojson&overview=full&access_token=$mapboxToken"
                val json = withContext(Dispatchers.IO) { java.net.URL(url).readText() }
                val routesArr = JSONObject(json).getJSONArray("routes")
                if (routesArr.length() == 0) {
                    navRouteError = "No walking route found between these locations"
                    isLoadingNavRoute = false; return@launch
                }
                val coordsArr = routesArr.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                navigationPoints = (0 until coordsArr.length()).map { i ->
                    val c = coordsArr.getJSONArray(i)
                    Point.fromLngLat(c.getDouble(0), c.getDouble(1))
                }
            } catch (e: Exception) {
                navRouteError = e.localizedMessage ?: "Failed to load route"
            }
            isLoadingNavRoute = false
        }
    }

    fun searchMapPlaces(q: String) {
        mapSearchQuery = q
        if (q.isBlank()) { mapSearchResults = emptyList(); isSearchingMap = false; mapSearchJob?.cancel(); return }
        isSearchingMap = true
        mapSearchJob?.cancel()
        mapSearchJob = viewModelScope.launch {
            delay(300)
            repo.getLieux(
                lat = currentLat.takeIf { it != 0.0 },
                lon = currentLon.takeIf { it != 0.0 },
                radiusKm = null,
                q = q,
                limit = 8
            ).fold(
                onSuccess = { mapSearchResults = it; isSearchingMap = false },
                onFailure = { mapSearchResults = emptyList(); isSearchingMap = false }
            )
        }
    }

    fun clearMapSearch() {
        mapSearchJob?.cancel()
        mapSearchQuery = ""
        mapSearchResults = emptyList()
        isSearchingMap = false
    }

    fun selectPlaceFromSearch(lieuId: Int) {
        selectedLieuId = lieuId
        backFromPlaceState = PlanningState.IDLE
        state = PlanningState.PLACE_DETAIL
    }

    fun selectPlaceFromList(lieuId: Int) {
        selectedLieuId = lieuId
        backFromPlaceState = PlanningState.PLACES
        state = PlanningState.PLACE_DETAIL
    }

    fun backFromPlace() { selectedLieuId = -1; state = backFromPlaceState }
    fun addToFavorites(placeName: String) {
        if (placeName.isNotBlank() && placeName !in preferences.favoritePlaces) {
            preferences = preferences.copy(favoritePlaces = preferences.favoritePlaces + placeName)
        }
    }

    fun backToOptions() { state = backFromDetailState }
    fun close() { selectedRoute = null; state = PlanningState.IDLE; error = null; navigationPoints = emptyList() }

    fun openSavedRoutes() {
        loadSavedItineraries()
        state = PlanningState.SAVED_ROUTES
    }

    fun openPlaces() {
        loadNearbyPlaces()
        state = PlanningState.PLACES
    }

    fun openExploreItins() {
        itinSearchResults = emptyList()
        state = PlanningState.EXPLORE_ITINS
    }

    fun loadNearbyPlaces() {
        isLoadingPlaces = true
        viewModelScope.launch {
            repo.getLieux(
                lat = currentLat.takeIf { it != 0.0 },
                lon = currentLon.takeIf { it != 0.0 },
                radiusKm = 5.0,
                limit = 30
            ).fold(
                onSuccess = { places = it; isLoadingPlaces = false },
                onFailure = { isLoadingPlaces = false }
            )
        }
    }

    fun searchItinsByQuery(q: String) {
        if (token.isBlank()) return
        isSearchingItins = true
        viewModelScope.launch {
            repo.searchItineraires(token, q = q.ifBlank { null }).fold(
                onSuccess = { itinSearchResults = it; isSearchingItins = false },
                onFailure = { isSearchingItins = false }
            )
        }
    }

    fun createPlace(request: CreateLieuRequest) {
        if (token.isBlank()) return
        viewModelScope.launch {
            repo.createLieu(token, request).fold(
                onSuccess = { loadNearbyPlaces() },
                onFailure = {}
            )
        }
    }

    fun loadNearbyPostMarkers() {
        if (gps.isBlank()) return
        viewModelScope.launch {
            repo.searchPosts(gps = gps, radiusKm = 5.0, limit = 50).fold(
                onSuccess = { nearbyPosts = it.results.filter { p -> !p.locGps.isNullOrBlank() } },
                onFailure = {}
            )
        }
    }
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
            name = etape.nomLieu.orEmpty(),
            address = etape.adresseLieu.orEmpty(),
            category = (etape.categorie ?: "autre").toActivityCategory(),
            timeSlot = (etape.creneau ?: "soir").toTimeSlot(),
            distanceFromPrev = dist,
            durationMinutes = etape.dureeMinutes,
            openingHours = etape.horaires?.ifEmpty { "Open" } ?: "Open",
            imageUrl = etape.urlImage.orEmpty(),
            price = etape.prix,
            rating = etape.note,
            gps = etape.gpsLieu.orEmpty()
        )
    },
    totalBudget = budgetTotal,
    totalDurationMinutes = dureeMinutes,
    effortScore = effortScore,
    weatherSensitive = meteoSensible,
    resume = resume
)

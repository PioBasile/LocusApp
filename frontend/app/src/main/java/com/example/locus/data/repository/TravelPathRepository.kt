package com.example.locus.data.repository

import com.example.locus.data.remote.*

class TravelPathRepository(private val api: ApiService) {

    // -- Places ----------------------------------------------------------------

    suspend fun getLieux(
        lat: Double? = null, lon: Double? = null,
        radiusKm: Double? = null, categorie: String? = null, q: String? = null,
        limit: Int = 50
    ): Result<List<LieuResponse>> = runCatching {
        api.getLieux(lat, lon, radiusKm, categorie, q, limit)
    }

    suspend fun getLieu(id: Int): Result<LieuResponse> = runCatching { api.getLieu(id) }

    suspend fun getLieuPosts(id: Int): Result<List<PostResponse>> = runCatching { api.getLieuPosts(id) }

    suspend fun createLieu(token: String, request: CreateLieuRequest): Result<CreateLieuResponse> =
        runCatching { api.createLieu(token, request) }

    // -- Itineraries -----------------------------------------------------------

    suspend fun generateItineraires(request: ItineraireRequest): Result<GenerateItinerairesResponse> =
        runCatching { api.generateItineraires(request) }

    suspend fun saveItineraire(token: String, itineraire: ItineraireResponse): Result<SaveItineraireResponse> =
        runCatching { api.saveItineraire(token, itineraire) }

    suspend fun getSavedItineraires(token: String): Result<List<SavedItinResponse>> =
        runCatching { api.getSavedItineraires(token) }

    suspend fun likeItineraire(token: String, id: Int): Result<Unit> =
        runCatching { api.likeItineraire(token, id) }

    suspend fun unlikeItineraire(token: String, id: Int): Result<Unit> =
        runCatching { api.unlikeItineraire(token, id) }

    suspend fun searchItineraires(
        token: String, categories: String? = null, q: String? = null
    ): Result<List<SavedItinResponse>> =
        runCatching { api.searchItineraires(token, categories, q) }

    // -- Weather ---------------------------------------------------------------

    suspend fun getWeather(lat: Double, lon: Double): Result<WeatherResponse> =
        runCatching { api.getWeather(lat, lon) }

    // -- Reviews ---------------------------------------------------------------

    suspend fun getLieuAvis(lieuId: Int): Result<List<LieuAvisResponse>> =
        runCatching { api.getLieuAvis(lieuId) }

    suspend fun submitLieuAvis(token: String, lieuId: Int, request: LieuAvisCreateRequest): Result<SubmitAvisResponse> =
        runCatching { api.submitLieuAvis(token, lieuId, request) }

    // -- Itinerary share -------------------------------------------------------

    suspend fun getShareItineraire(id: Int): Result<ShareItineraireResponse> =
        runCatching { api.getShareItineraire(id) }

    // -- Search posts ----------------------------------------------------------

    suspend fun searchPosts(
        q: String? = null, gps: String? = null, radiusKm: Double? = null,
        tags: String? = null, limit: Int = 20, offset: Int = 0
    ): Result<SearchPostsResponse> =
        runCatching { api.searchPosts(q, gps, radiusKm, tags, limit, offset) }
}

package com.example.locus.data.remote

import com.google.gson.annotations.SerializedName


// -- Auth ----------------------------------------------------------------------
data class LoginResponse(val token: String)
data class SignupResponse(val message: String)

// -- Posts ---------------------------------------------------------------------
data class PostResponse(
    val id: Int,
    val user_id: Int,
    val groupe: List<Int>,
    val description: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("audio_url") val audioUrl: String? = null,
    val tags: List<String>? = null,
    val date: String,
    val id_loc: Int? = null
)

// -- Profile -------------------------------------------------------------------
data class PublicProfileResponse(
    val id: Int,
    val username: String,
    val ppurl: String?,
    val posts: List<PostResponse>? = null
)

data class ProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val ppurl: String
)

// -- Locations -----------------------------------------------------------------
data class LocationResponse(
    val id: Int,
    val name: String,
    val gps: String
)

// -- Groups --------------------------------------------------------------------
data class MakeGroupResponse(
    val message: String,
    val group_id: Int
)

data class JoinGroupResponse(
    val message: String
)


data class GroupResponse(
    val id: Int,
    val name: String,
    val is_private: Boolean,
    val description: String,
    @SerializedName("image_url") val imageUrl: String?
)

data class GroupDetailResponse(
    val name: String,
    @SerializedName("image_url") val imageUrl: String?,
    val members: List<GroupMemberResponse>
)

data class GroupMemberResponse(
    val id: Int,
    val username: String,
    val ppurl: String? = null
)

data class MyGroupResponse(
    @SerializedName("id_grp") val id: Int,
    @SerializedName("nom") val name: String,
    @SerializedName("is_private") val isPrivate: Boolean?,
    @SerializedName("description") val description: String?,
    @SerializedName("image_url") val imageUrl: String?
)

// -- Followers ------------------------------------------------------------------
data class FollowerResponse(
    val id: Int,
    val username: String,
    val ppurl: String? = null
)

// -- Profile pic ----------------------------------------------------------------
data class ChangePPResponse(
    val message: String,
)

// -- Comments ...................................................................
data class CommentResponse(
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("post_id")
    val postId: Int,

    val commentaire: String,

    @SerializedName("audio_url")
    val audioUrl: String? = null
)

// -- Likes ......................................................................
data class LikesCountResponse(val likes_count: Int)

// -- TravelPath / Places -------------------------------------------------------
data class LieuPhotoResponse(
    val id: Int,
    @SerializedName("id_lieu") val idLieu: Int,
    val url: String,
    val legende: String,
    val ordre: Int
)

data class LieuResponse(
    val id: Int,
    val nom: String,
    val description: String,
    val adresse: String,
    val categorie: String,
    val gps: String,
    @SerializedName("url_image") val urlImage: String?,
    val note: Float,
    @SerializedName("nb_avis") val nbAvis: Int,
    val horaires: String?,
    @SerializedName("prix_moyen") val prixMoyen: Int,
    @SerializedName("site_web") val siteWeb: String?,
    val telephone: String?,
    @SerializedName("id_loc") val idLoc: Int?,
    @SerializedName("distance_km") val distanceKm: Double = 0.0,
    val photos: List<LieuPhotoResponse> = emptyList()
)

data class CreateLieuResponse(
    val message: String,
    @SerializedName("id_lieu") val idLieu: Int,
    @SerializedName("id_loc") val idLoc: Int
)

// -- TravelPath / Itineraries --------------------------------------------------
data class ItineraireEtapeResponse(
    val ordre: Int,
    @SerializedName("id_lieu") val idLieu: Int,
    @SerializedName("nom_lieu") val nomLieu: String,
    @SerializedName("adresse_lieu") val adresseLieu: String,
    @SerializedName("gps_lieu") val gpsLieu: String,
    val categorie: String,
    val creneau: String,
    @SerializedName("duree_minutes") val dureeMinutes: Int,
    @SerializedName("distance_prev_km") val distancePrevKm: Double,
    @SerializedName("temps_trajet_min") val tempsTrajMin: Int,
    val prix: Int,
    val horaires: String,
    @SerializedName("url_image") val urlImage: String,
    val note: Float
)

data class ItineraireResponse(
    val id: Int = 0,
    val type: String,
    val nom: String,
    val etapes: List<ItineraireEtapeResponse>,
    @SerializedName("budget_total") val budgetTotal: Int,
    @SerializedName("duree_minutes") val dureeMinutes: Int,
    @SerializedName("effort_score") val effortScore: Int,
    @SerializedName("meteo_sensible") val meteoSensible: Boolean,
    val resume: String = ""
)

data class GenerateItinerairesResponse(
    val itineraires: List<ItineraireResponse>,
    @SerializedName("gps_depart") val gpsDepart: String,
    @SerializedName("duree_heures") val dureeHeures: Int
)

data class SaveItineraireResponse(
    val message: String,
    val id: Int
)

data class SavedItinResponse(
    val id: Int,
    val nom: String,
    val type: String,
    val budget: Int,
    @SerializedName("duree_minutes") val dureeMinutes: Int,
    @SerializedName("effort_score") val effortScore: Int,
    val itineraire: ItineraireResponse,
    @SerializedName("created_at") val createdAt: String
)

// -- Search posts --------------------------------------------------------------
data class SearchPostResult(
    val id: Int,
    val user_id: Int,
    val description: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("audio_url") val audioUrl: String? = null,
    val tags: List<String>? = null,
    val date: String,
    val id_loc: Int? = null,
    @SerializedName("loc_gps") val locGps: String? = null,
    @SerializedName("loc_nom") val locNom: String? = null,
    @SerializedName("distance_km") val distanceKm: Double? = null
)

data class SearchPostsResponse(
    val total: Int,
    val results: List<SearchPostResult>
)

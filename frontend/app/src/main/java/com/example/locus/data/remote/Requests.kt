package com.example.locus.data.remote

import android.net.Uri

data class CreateGroupRequest(
    val nom: String,
    val description: String
)


data class LoginRequest(
    val username: String = "",
    val email: String,
    val password: String,
    val avatarUri: Uri? = null
)

data class FCMTokenRequest(val fcm_token: String)

data class ItineraireRequest(
    val gps: String = "",
    val categories: List<String>,
    val budget_max: Int,
    val duree_heures: Int,
    val effort: String,
    val tout_temps: Boolean,
    val lieux_favoris: List<String>
)

data class CreateLieuRequest(
    val nom: String,
    val description: String = "",
    val adresse: String = "",
    val categorie: String,
    val lat: Double,
    val lon: Double,
    val horaires: String = "",
    val prix_moyen: Int = 0,
    val site_web: String = "",
    val telephone: String = ""
)


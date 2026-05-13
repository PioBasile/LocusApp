package com.example.locus.data.model

import com.google.gson.annotations.SerializedName
data class Post(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("username")
    val username: String,

    @SerializedName("groupe")
    val groupe: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("id_loc")
    val idLoc: Int?,

    @SerializedName("location_name")
    val locationName: String?,

    @SerializedName("audio_url")
    val audioUrl: String? = null,

    @SerializedName("tags")
    val tags: List<String>? = null,

    @SerializedName("loc_gps")
    val locGps: String? = null
)

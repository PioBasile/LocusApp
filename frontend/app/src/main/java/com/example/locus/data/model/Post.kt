package com.example.locus.data.model

import com.google.gson.annotations.SerializedName
data class Post(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("groupe")
    val groupe: Int,

    @SerializedName("description")
    val description: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("id_loc")
    val idLoc: Int?
)

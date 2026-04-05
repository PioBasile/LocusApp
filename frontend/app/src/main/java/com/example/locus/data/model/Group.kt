package com.example.locus.data.model

data class Group (
    val id: Int,
    val name: String,
    val description: String,
    val isPrivate: Boolean,
    val password: String,
    //val member_count : Int,
    //val profile_picture : String
)
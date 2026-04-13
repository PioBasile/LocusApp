package com.example.locus.data.remote


// -- Auth ----------------------------------------------------------------------
data class LoginResponse(val token: String)
data class SignupResponse(val message: String)

// -- Posts ---------------------------------------------------------------------
data class PostResponse(
    val id: Int,
    val user_id: Int,
    val groupe: Int,
    val description: String,
    val image_url: String,
    val date: String,
    val id_loc: Int?
)

// -- Profile -------------------------------------------------------------------
data class PublicProfileResponse(
    val id: Int,
    val username: String,
    val ppurl: String
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
    val description: String
)

// -- Followers ------------------------------------------------------------------
data class FollowerResponse(
    val id: Int,
    val username: String,
)

// -- Profile pic ----------------------------------------------------------------
data class ChangePPResponse(
    val message: String,
)
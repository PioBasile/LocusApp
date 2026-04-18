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
    val date: String,
    val id_loc: Int?
)

// -- Profile -------------------------------------------------------------------
data class PublicProfileResponse(
    val id: Int,
    val username: String,
    val ppurl: String?
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

    val commentaire: String
)
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


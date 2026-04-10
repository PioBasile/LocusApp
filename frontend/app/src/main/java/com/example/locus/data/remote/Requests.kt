package com.example.locus.data.remote

data class CreateGroupRequest(
    val nom: String,
    val description: String
)


data class LoginRequest(
    val username: String = "",
    val email: String,
    val password: String
)


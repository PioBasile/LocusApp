package com.example.locus.data.repository

import com.example.locus.data.remote.LoginRequest
import com.example.locus.data.remote.LoginResponse
import com.example.locus.data.remote.ProfileResponse
import com.example.locus.data.remote.RetrofitClient
import com.example.locus.data.remote.SignupResponse

class UserRepository {
    private val api = RetrofitClient.api

    suspend fun login(email: String, password: String): LoginResponse {
        return api.login(LoginRequest(email = email, password = password))
    }

    suspend fun signup(username: String, email: String, password: String): SignupResponse {
        return api.signup(LoginRequest(username = username, email = email, password = password))
    }

    suspend fun getProfile(token: String): ProfileResponse {
        return api.getProfile(token)
    }
}
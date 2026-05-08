package com.example.locus.data.repository

import android.net.Uri
import com.example.locus.data.remote.FCMTokenRequest
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.remote.LoginRequest
import com.example.locus.data.remote.LoginResponse
import com.example.locus.data.remote.ProfileResponse
import com.example.locus.data.remote.RetrofitClient
import com.example.locus.data.remote.SignupResponse
import com.example.locus.data.remote.ChangePPResponse

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserRepository {
    private val api = RetrofitClient.api

    // -- Authentification ------------------------------------------
    suspend fun login(email: String, password: String): LoginResponse {
        return api.login(LoginRequest(email = email, password = password))
    }

    suspend fun signup(username: String, email: String, password: String): SignupResponse {
        return api.signup(LoginRequest(username = username, email = email, password = password))
    }

    // -- Profil ----------------------------------------------------
    suspend fun getProfile(token: String): ProfileResponse {
        return api.getProfile(token)
    }

    suspend fun uploadProfilePicture(token: String, imageFile: File) {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("profile_picture", imageFile.name, requestFile)
        api.changeProfilePicture(token, body)
    }

    suspend fun changeUsername(token: String, username: String): String {
        return api.changeUsername(token, username)
    }

    // -- Social (Followers) ----------------------------------------
    suspend fun followUser(token: String, targetUserId: Int): String {
        return api.followUser(token, targetUserId)
    }

    suspend fun unfollowUser(token: String, targetUserId: Int): String {
        return api.unfollowUser(token, targetUserId)
    }

    suspend fun getFollowers(token: String): List<FollowerResponse> {
        return try {
            api.getFollowers(token)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMyFollowing(token: String): List<FollowerResponse> {
        return try {
            api.getMyFollowing(token)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTopUsers(limit: Int = 5): List<FollowerResponse> {
        return try {
            api.getMostFollowedUsers(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateFCMToken(token: String, fcmToken: String) {
        api.updateFCMToken(token, FCMTokenRequest(fcmToken))
    }
}

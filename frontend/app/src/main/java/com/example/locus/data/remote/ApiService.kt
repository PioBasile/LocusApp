package com.example.locus.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String)
data class SignupResponse(val message: String)

data class PostResponse(
    val id: Int,
    val user_id: Int,
    val groupe: Int,
    val description: String,
    val image_url: String,
    val date: String,
    val id_loc: Int?
)

interface ApiService {

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/signup")
    suspend fun signup(@Body request: LoginRequest): SignupResponse

    @Multipart
    @POST("/makepost")
    suspend fun makePost(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("description") description: RequestBody,
        @Part("groupe") groupe: RequestBody,
        @Part("id_loc") idLoc: RequestBody
    ): String

    @GET("/getpost")
    suspend fun getPost(@Query("id") postId: Int): PostResponse
}
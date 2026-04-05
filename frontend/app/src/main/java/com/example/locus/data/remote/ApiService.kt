package com.example.locus.data.remote

import com.example.locus.data.repository.JoinGroupResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    // -- Auth (public) ---------------------------------------------
    @POST("/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("/signup")
    suspend fun signup(@Body request: LoginRequest): SignupResponse

    // -- Posts (public) --------------------------------------------
    @GET("/getpost")
    suspend fun getPost(@Query("id") postId: Int): PostResponse

    // -- Posts (protected) -----------------------------------------
    @Multipart
    @POST("/makepost")
    suspend fun makePost(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part("description") description: RequestBody,
        @Part("groupe") groupe: RequestBody,
        @Part("id_loc") idLoc: RequestBody
    ): String

    @GET("/getPostsByGroup")
    suspend fun getPostsByGroup(
        @Header("Authorization") token: String,
        @Query("groupe") groupId: Int
    ): List<PostResponse>

    // -- Profile (public) ------------------------------------------
    @GET("/getPublicProfile")
    suspend fun getPublicProfile(@Query("id") userId: Int): PublicProfileResponse

    // -- Profile (protected) ---------------------------------------
    @GET("/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ProfileResponse

    // -- Locations (public) ----------------------------------------
    @GET("/getLocations")
    suspend fun getLocations(): List<LocationResponse>

    // -- Groups (public) -------------------------------------------
    @GET("/getGroups")
    suspend fun getGroups(): List<GroupResponse>


    // -- Groups (protected) ----------------------------------------
    @POST("/makeGroup")
    suspend fun makeGroup(
        @Header("Authorization") token: String,
        @Body group: CreateGroupRequest
    ): String

    @FormUrlEncoded
    @POST("/makeGroup")
    suspend fun makeGroup(
        @Header("Authorization") token: String,
        @Field("name") name: String,
        @Field("description") description: String,
        @Field("is_private") isPrivate: Boolean,
        @Field("password") password: String = ""
    ): MakeGroupResponse

    @FormUrlEncoded
    @POST("/joinGroup")
    suspend fun joinGroup(
        @Header("Authorization") token: String,
        @Field("group_id") groupId: Int,
        @Field("password") password: String = ""
    ): JoinGroupResponse
}
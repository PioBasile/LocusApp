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
        @Part("groupe") groupes: @JvmSuppressWildcards List<RequestBody>,
        @Part("id_loc") idLoc: RequestBody
    ): String

    @GET("/getPostsByGroup")
    suspend fun getPostsByGroup(
        @Header("Authorization") token: String,
        @Query("groupe") groupId: Int
    ): List<PostResponse>

    @GET("/deletePost")
    suspend fun deletePost(
        @Header("Authorization") token: String,
        @Query("id") postId: Int
    ): String

    @POST("/comment")
    suspend fun postComment(
        @Header("Authorization") token: String,
        @Query("id") postId: Int,
        @Query("comment") comment: String
    )

    @GET("/getComments")
    suspend fun getComments(
        @Header("Authorization") token: String,
        @Query("id") postId: Int
    ): List<CommentResponse>

    @POST("/like")
    suspend fun likePost(
        @Header("Authorization") token: String,
        @Query("id") postId: Int
    )

    @GET("/getLikes")
    suspend fun getLikes(
        @Header("Authorization") token: String)
    : Int

    @POST("/unlike")
    suspend fun unlikePost(
        @Header("Authorization") token: String,
        @Query("id") postId: Int
    )

    @POST("/report")
    suspend fun reportPost(
        @Header("Authorization") token: String,
        @Query("id") postId: Int,
        @Query("comment") comment: String,
        @Query("reason") reason: String
    )



    // -- Profile (public) ------------------------------------------
    @GET("/getPublicProfile")
    suspend fun getPublicProfile(@Query("id") userId: Int): PublicProfileResponse

    // -- Profile (protected) ---------------------------------------
    @GET("/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ProfileResponse

    // -- Locations (public) ----------------------------------------
    @GET("/getLocations")
    suspend fun getLocation(@Query("id") locId: Int): LocationResponse

    // -- Groups (public) -------------------------------------------
    @GET("/getGroups")
    suspend fun getGroups(): List<GroupResponse>

    // -- Groups (protected) ----------------------------------------

    @Multipart
    @POST("/makeGroup")
    suspend fun makeGroup(
        @Header("Authorization") token: String,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("is_private") isPrivate: RequestBody,
        @Part("password") password: RequestBody,
        @Part image: MultipartBody.Part
    ): MakeGroupResponse

    @FormUrlEncoded
    @POST("/joinGroup")
    suspend fun joinGroup(
        @Header("Authorization") token: String,
        @Field("group_id") groupId: Int,
        @Field("password") password: String = ""
    ): JoinGroupResponse

    @GET("/getGroupInfo")
    suspend fun getGroupById(
        @Query("id") groupId: Int
    ): GroupDetailResponse

    @GET("/getUserGroups")
    suspend fun getMyGroupIds(
        @Header("Authorization") token: String
    ): List<Int>

    // -- Social / Follows (protected) ------------------------------
    @FormUrlEncoded
    @POST("/follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Field("user_id") targetUserId: Int
    ): String

    @FormUrlEncoded
    @POST("/unfollow")
    suspend fun unfollowUser(
        @Header("Authorization") token: String,
        @Field("user_id") targetUserId: Int
    ): String

    @GET("/getFollowers")
    suspend fun getFollowers(
        @Header("Authorization") token: String
    ): List<FollowerResponse>

    // -- Paramètres Profil (protected) -----------------------------
    @Multipart
    @POST("/changePP")
    suspend fun changeProfilePicture(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    )
}
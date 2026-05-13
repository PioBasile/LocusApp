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

    @GET("/getAllUserPosts")
    suspend fun getAllUserPosts(@Query("user_id") userId: Int): List<PostResponse>

    // -- Posts (protected) -----------------------------------------
    @Multipart
    @POST("/makepost")
    suspend fun makePost(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
        @Part audio: MultipartBody.Part?,
        @Part("description") description: RequestBody,
        @Part("groupe") groupes: @JvmSuppressWildcards List<RequestBody>,
        @Part("id_loc") idLoc: RequestBody,
        @Part("ai_tags") aiTags: RequestBody,
        @Part("tags") tags: @JvmSuppressWildcards List<RequestBody> = emptyList()
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

    @Multipart
    @POST("/comment")
    suspend fun postComment(
        @Header("Authorization") token: String,
        @Query("id") postId: Int,
        @Part("comment") comment: RequestBody,
        @Part audio: MultipartBody.Part?
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
        @Header("Authorization") token: String,
        @Query("id") postId: Int
    ): LikesCountResponse

    @GET("/getAllUserLikes")
    suspend fun getAllUserLikes(
        @Header("Authorization") token: String
    ): List<Int>

    @POST("/unlike")
    suspend fun unlikePost(
        @Header("Authorization") token: String,
        @Query("id") postId: Int
    )

    @FormUrlEncoded
    @POST("/reportPost")
    suspend fun reportPost(
        @Header("Authorization") token: String,
        @Query("id") postId: Int,
        @Field("comment") comment: String,
        @Field("reason") reason: String
    ): String

    // -- Nearby posts (public) -------------------------------------
    @GET("/getNearbyPosts")
    suspend fun getNearbyPosts(@Query("gps") gps: String): List<Int>



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

    @GET("/getMyFollowers")
    suspend fun getMyFollowing(
        @Header("Authorization") token: String
    ): List<FollowerResponse>

    @GET("/getMostFollowedUsers")
    suspend fun getMostFollowedUsers(
        @Query("limit") limit: Int = 5
    ): List<FollowerResponse>

    // -- Paramètres Profil (protected) -----------------------------
    @Multipart
    @POST("/changePP")
    suspend fun changeProfilePicture(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    )

    @FormUrlEncoded
    @POST("/ChangeUsername")
    suspend fun changeUsername(
        @Header("Authorization") token: String,
        @Field("username") username: String
    ): String

    // -- Push notifications (protected) ----------------------------
    @POST("/updateFCMToken")
    suspend fun updateFCMToken(
        @Header("Authorization") token: String,
        @Body body: FCMTokenRequest
    )

    // -- TravelPath / Places (public) ------------------------------
    @GET("/travelPath/lieux")
    suspend fun getLieux(
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radius_km") radiusKm: Double? = null,
        @Query("categorie") categorie: String? = null,
        @Query("q") q: String? = null,
        @Query("limit") limit: Int = 50
    ): List<LieuResponse>

    @GET("/travelPath/lieu")
    suspend fun getLieu(@Query("id") id: Int): LieuResponse

    @GET("/travelPath/lieux/posts")
    suspend fun getLieuPosts(@Query("id") lieuId: Int): List<PostResponse>

    // -- TravelPath / Places (protected) ---------------------------
    @POST("/travelPath/lieux/create")
    suspend fun createLieu(
        @Header("Authorization") token: String,
        @Body request: CreateLieuRequest
    ): CreateLieuResponse

    // -- TravelPath / Itineraries (public) -------------------------
    @POST("/travelPath/itineraires/generate")
    suspend fun generateItineraires(@Body request: ItineraireRequest): GenerateItinerairesResponse

    // -- TravelPath / Itineraries (protected) ----------------------
    @POST("/travelPath/itineraires/save")
    suspend fun saveItineraire(
        @Header("Authorization") token: String,
        @Body itineraire: ItineraireResponse
    ): SaveItineraireResponse

    @GET("/travelPath/itineraires")
    suspend fun getSavedItineraires(
        @Header("Authorization") token: String
    ): List<SavedItinResponse>

    @POST("/travelPath/itineraires/like")
    suspend fun likeItineraire(
        @Header("Authorization") token: String,
        @Query("id") id: Int
    )

    @DELETE("/travelPath/itineraires/unlike")
    suspend fun unlikeItineraire(
        @Header("Authorization") token: String,
        @Query("id") id: Int
    )

    @GET("/travelPath/itineraires/search")
    suspend fun searchItineraires(
        @Header("Authorization") token: String,
        @Query("categories") categories: String? = null,
        @Query("q") q: String? = null
    ): List<SavedItinResponse>

    // -- Weather (public) ------------------------------------------
    @GET("/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): WeatherResponse

    // -- TravelPath / Reviews (GET public, POST auth) --------------
    @GET("/travelPath/lieux/avis")
    suspend fun getLieuAvis(@Query("id") lieuId: Int): List<LieuAvisResponse>

    @POST("/travelPath/lieux/avis")
    suspend fun submitLieuAvis(
        @Header("Authorization") token: String,
        @Query("id") lieuId: Int,
        @Body request: LieuAvisCreateRequest
    ): SubmitAvisResponse

    @GET("/getUserAvis")
    suspend fun getUserAvis(@Query("user_id") userId: Int): List<UserAvisResponse>

    // -- TravelPath / Share (public) --------------------------------
    @GET("/travelPath/itineraires/share")
    suspend fun getShareItineraire(@Query("id") id: Int): ShareItineraireResponse

    // -- Search posts (public) -------------------------------------
    @GET("/searchPosts")
    suspend fun searchPosts(
        @Query("q") q: String? = null,
        @Query("gps") gps: String? = null,
        @Query("radius_km") radiusKm: Double? = null,
        @Query("tags") tags: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SearchPostsResponse
}

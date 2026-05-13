package com.example.locus.data.repository

import com.example.locus.data.model.Post
import com.example.locus.data.remote.CommentResponse
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.PublicProfileResponse
import com.example.locus.data.remote.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PostRepository {
    private val api = RetrofitClient.api

    // -- Fetch -----------------------------------------------------
    suspend fun getPost(postId: Int): PostResponse {
        return api.getPost(postId)
    }

    suspend fun getPostsByGroup(token: String, groupId: Int): List<PostResponse> {
        return try {
            api.getPostsByGroup(token, groupId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPublicProfile(userId: Int): PublicProfileResponse {
        return api.getPublicProfile(userId)
    }

    suspend fun getAllUserPosts(userId: Int): List<PostResponse> {
        return try {
            api.getAllUserPosts(userId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // -- Create ----------------------------------------------------
    suspend fun uploadPost(
        token: String,
        imageFile: File,
        description: String,
        groupIds: List<Int>,
        locationId: Int,
        audioFile: File? = null,
        aiTags: Boolean = true,
        tags: List<String> = emptyList()
    ): String {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

        val audioPart = audioFile?.let { file ->
            val audioRequestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("audio", file.name, audioRequestFile)
        }

        val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val groupParts = groupIds.map { it.toString().toRequestBody("text/plain".toMediaTypeOrNull()) }
        val locationPart = locationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val aiTagsPart = aiTags.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val tagParts = tags.map { it.toRequestBody("text/plain".toMediaTypeOrNull()) }

        return api.makePost(
            token = token,
            image = imagePart,
            audio = audioPart,
            description = descriptionPart,
            groupes = groupParts,
            idLoc = locationPart,
            aiTags = aiTagsPart,
            tags = tagParts
        )
    }

    // -- Comments -------------------------------------------------------
    suspend fun getComments(token: String, postId: Int): List<CommentResponse> {
        return try {
            api.getComments(token, postId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addComment(token: String, postId: Int, comment: String, audioFile: File? = null) {
        val commentPart = comment.toRequestBody("text/plain".toMediaTypeOrNull())
        val audioPart = audioFile?.let { file ->
            val audioRequestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("audio", file.name, audioRequestFile)
        }
        api.postComment(token, postId, commentPart, audioPart)
    }

    // -- Like ------------------------------------------------------------
    suspend fun likePost(token: String, postId: Int) {
        api.likePost(token, postId)
    }

    suspend fun unlikePost(token: String, postId: Int) {
        api.unlikePost(token, postId)
    }

    suspend fun getLikesForPost(token: String, postId: Int): Int {
        return try {
            api.getLikes(token, postId).likes_count
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getAllUserLikes(token: String): List<Int> {
        return try {
            api.getAllUserLikes(token)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // -- Follow / unfollow -----------------------------------------
    suspend fun followUser(token: String, targetUserId: Int): String {
        return api.followUser(token, targetUserId)
    }

    suspend fun unfollowUser(token: String, targetUserId: Int): String {
        return api.unfollowUser(token, targetUserId)
    }

    suspend fun getMyFollowing(token: String): List<FollowerResponse> {
        return try { api.getMyFollowing(token) } catch (e: Exception) { emptyList() }
    }

    // -- Report a post ---------------------------------------------
    suspend fun reportPost(token: String, postId: Int, reason: String, comment: String): String {
        return api.reportPost(token, postId, reason, comment)
    }

    // -- Delete a post ---------------------------------------------
    suspend fun deletePost(token: String, postId: Int): String {
        return api.deletePost(token, postId)
    }

    // -- Location GPS lookup ---------------------------------------
    suspend fun getLocationGps(locId: Int): String? {
        return try { api.getLocation(locId).gps } catch (e: Exception) { null }
    }

    // -- Nearby posts (public) -------------------------------------
    suspend fun getNearbyPosts(gps: String): List<Int> {
        return try {
            api.getNearbyPosts(gps)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

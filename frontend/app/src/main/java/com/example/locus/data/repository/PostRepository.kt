package com.example.locus.data.repository

import com.example.locus.data.model.Post
import com.example.locus.data.remote.CommentResponse
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
            println("Erreur chargement des posts : ${e.message}")
            emptyList()
        }
    }

    suspend fun getPublicProfile(userId: Int): PublicProfileResponse {
        return api.getPublicProfile(userId)
    }


    // -- Create ----------------------------------------------------
    suspend fun uploadPost(
        token: String,
        imageFile: File,
        description: String,
        groupIds: List<Int>,
        locationId: Int
    ): String {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

        val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val groupParts = groupIds.map { groupId -> groupId.toString().toRequestBody("text/plain".toMediaTypeOrNull()) }
        val locationPart = locationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return api.makePost(
            token = token,
            image = imagePart,
            description = descriptionPart,
            groupes = groupParts,
            idLoc = locationPart
        )
    }

    // -- Comments -------------------------------------------------------

    suspend fun getComments(token: String, postId: Int): List<CommentResponse> {
        return try {
            api.getComments(token, postId)
        } catch (e: Exception) {
            println("Erreur chargement des commentaires : ${e.message}")
            emptyList()
        }
    }

    suspend fun addComment(token: String, postId: Int, comment: String) {
        api.postComment(token, postId, comment)
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

    // -- Follow a user ---------------------------------------------
    suspend fun followUser(token: String, targetUserId: Int): String {
        return api.followUser(token, targetUserId)
    }

    // -- Report a post ---------------------------------------------
    suspend fun reportPost(token: String, postId: Int, reason: String, comment: String): String {
        return api.reportPost(token, postId, reason, comment)
    }

    // -- Delete a post ---------------------------------------------
    suspend fun deletePost(token: String, postId: Int): String {
        return api.deletePost(token, postId)
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
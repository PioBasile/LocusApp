package com.example.locus.data.repository

import com.example.locus.data.model.Post
import com.example.locus.data.remote.PostResponse
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
}
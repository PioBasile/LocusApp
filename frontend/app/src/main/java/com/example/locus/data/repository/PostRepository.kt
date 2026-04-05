package com.example.locus.data.repository

import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

data class FeedPost(
    val id: Int,
    val userId: Int,
    val groupe: Int,
    val description: String,
    val imageUrl: String,
    val date: String,
    val idLoc: Int?
)

class PostRepository {
    private val api = RetrofitClient.api

    // -- Fetch -----------------------------------------------------
    suspend fun getPost(postId: Int): PostResponse {
        return api.getPost(postId)
    }

    suspend fun getPostsByGroup(token: String, groupId: Int): List<FeedPost> {
        return api.getPostsByGroup(token, groupId).map { response ->
            FeedPost(
                id = response.id,
                userId = response.user_id,
                groupe = response.groupe,
                description = response.description,
                imageUrl = response.image_url,
                date = response.date,
                idLoc = response.id_loc
            )
        }
    }

    // -- Create ----------------------------------------------------
    suspend fun uploadPost(
        token: String,
        imageFile: File,
        description: String,
        groupId: Int,
        locationId: Int
    ): String {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

        val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val groupPart = groupId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val locationPart = locationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return api.makePost(
            token = token,
            image = imagePart,
            description = descriptionPart,
            groupe = groupPart,
            idLoc = locationPart
        )
    }
}
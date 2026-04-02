package com.example.locus.data.repository

import com.example.locus.data.model.Post
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

class AddPostRepository {
    private val api = RetrofitClient.api

    suspend fun getPost(postId: Int): PostResponse {
        return api.getPost(postId)
    }

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
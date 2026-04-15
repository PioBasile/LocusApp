package com.example.locus.data.repository


import com.example.locus.data.model.Group
import com.example.locus.data.remote.GroupDetailResponse
import com.example.locus.data.remote.GroupResponse
import com.example.locus.data.remote.MakeGroupResponse
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.data.remote.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File



data class JoinGroupResponse(
    val message: String
)

// -- Repository ----------------------------------------------------------------
class GroupRepository {
    private val api = RetrofitClient.api

    // Get all groups - public, no token needed
    suspend fun getGroups(): List<Group> {
        return api.getGroups().map { response ->
            Group(
                id = response.id,
                name = response.name,
                description = response.description,
                isPrivate = response.is_private,
                password = "",
                imageUrl = response.imageUrl
            )

        }
    }

    // Create a group - protected
    suspend fun makeGroup(
        token: String,
        name: String,
        description: String,
        isPrivate: Boolean,
        password: String = "",
        imageFile: File
    ): MakeGroupResponse {
        val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val isPrivatePart = isPrivate.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val passwordPart = password.toRequestBody("text/plain".toMediaTypeOrNull())

        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)

        return api.makeGroup(
            token = token,
            name = namePart,
            description = descPart,
            isPrivate = isPrivatePart,
            password = passwordPart,
            image = imagePart
        )
    }

    // Join a group - protected
    suspend fun joinGroup(
        token: String,
        groupId: Int,
        password: String = ""
    ): JoinGroupResponse {
        return api.joinGroup(
            token = token,
            groupId = groupId,
            password = password
        )
    }

    suspend fun getGroupDetails(groupId: Int): GroupDetailResponse? {
        return try {
            api.getGroupById(groupId)
        } catch (e: Exception) {
            println("Erreur chargement des détails du groupe $groupId : ${e.message}")
            null
        }
    }

    suspend fun getMyGroups(token: String): List<Int> {
        return try {
            api.getMyGroupIds(token)
        } catch (e: Exception) {
            println("Erreur chargement de mes groupes : ${e.message}")
            emptyList()
        }
    }
}
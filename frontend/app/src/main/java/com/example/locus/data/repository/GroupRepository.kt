package com.example.locus.data.repository


import com.example.locus.data.model.Group
import com.example.locus.data.remote.GroupResponse
import com.example.locus.data.remote.MakeGroupResponse
import com.example.locus.data.remote.RetrofitClient
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request


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
                password = ""
            )

        }
    }

    // Create a group - protected
    suspend fun makeGroup(
        token: String,
        name: String,
        description: String,
        isPrivate: Boolean,
        password: String = ""
    ): MakeGroupResponse {
        return api.makeGroup(
            token = token,
            name = name,
            description = description,
            isPrivate = isPrivate,
            password = password
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
}
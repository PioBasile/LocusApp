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

    suspend fun getPostsByGroup(token: String, groupId: Int): List<Post> {
        return coroutineScope {
            // 1. Récupérer les posts
            val rawPosts = try {
                api.getPostsByGroup(token, groupId)
            } catch (e: Exception) {
                println("Erreur API getPostsByGroup : ${e.message}")
                return@coroutineScope emptyList()
            }

            // 2. Extraire les IDs UNIQUES (Utilisateurs ET Localisations)
            val uniqueUserIds = rawPosts.map { it.user_id }.distinct()
            val uniqueLocIds = rawPosts.mapNotNull { it.id_loc }.distinct()

            // 3. Récupérer les profils en parallèle
            val usersMap = uniqueUserIds.map { userId ->
                async {
                    try {
                        val profile = api.getPublicProfile(userId)
                        userId to profile.username
                    } catch (e: Exception) {
                        println("Erreur Profil ID $userId : ${e.message}")
                        userId to "Inconnu"
                    }
                }
            }.awaitAll().toMap()

            // 4. Récupérer les localisations en parallèle (NOUVEAU)
            val locationsMap = uniqueLocIds.map { locId ->
                async {
                    try {
                        val location = api.getLocation(locId)
                        locId to location.name
                    } catch (e: Exception) {
                        println("Erreur Location ID $locId : ${e.message}")
                        locId to "Lieu inconnu"
                    }
                }
            }.awaitAll().toMap()

            // 5. Assembler le tout
            rawPosts.map { response ->
                Post(
                    id = response.id,
                    userId = response.user_id,
                    username = usersMap[response.user_id] ?: "Inconnu",
                    groupe = response.groupe,
                    description = response.description,
                    imageUrl = response.image_url,
                    date = response.date,
                    idLoc = response.id_loc,
                    locationName = locationsMap[response.id_loc] ?: "Lieu inconnu"
                )
            }
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
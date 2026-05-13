package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.remote.LieuAvisCreateRequest
import com.example.locus.data.remote.LieuAvisResponse
import com.example.locus.data.remote.LieuResponse
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.RetrofitClient
import com.example.locus.data.repository.TravelPathRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class PlaceDetailViewModel : ViewModel() {

    private val repo = TravelPathRepository(RetrofitClient.api)

    var isLoading by mutableStateOf(false)
        private set
    var lieu by mutableStateOf<LieuResponse?>(null)
        private set
    var posts by mutableStateOf<List<PostResponse>>(emptyList())
        private set
    var avis by mutableStateOf<List<LieuAvisResponse>>(emptyList())
        private set

    var token: String = ""
    private var loadedLieuId: Int = -1

    fun load(lieuId: Int) {
        loadedLieuId = lieuId
        viewModelScope.launch {
            isLoading = true
            try {
                supervisorScope {
                    val lieuDeferred = async { repo.getLieu(lieuId) }
                    val postsDeferred = async { repo.getLieuPosts(lieuId) }
                    val avisDeferred = async { repo.getLieuAvis(lieuId) }
                    lieu = lieuDeferred.await().getOrNull()
                    posts = postsDeferred.await().getOrElse { emptyList() }
                    avis = avisDeferred.await().getOrElse { emptyList() }
                }
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    fun submitAvis(note: Int, commentaire: String) {
        if (token.isBlank() || loadedLieuId < 0) return
        viewModelScope.launch {
            repo.submitLieuAvis(token, loadedLieuId, LieuAvisCreateRequest(note, commentaire)).fold(
                onSuccess = {
                    repo.getLieuAvis(loadedLieuId).fold(
                        onSuccess = { avis = it },
                        onFailure = {}
                    )
                },
                onFailure = {}
            )
        }
    }
}

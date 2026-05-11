package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun load(lieuId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                supervisorScope {
                    val lieuDeferred = async { repo.getLieu(lieuId) }
                    val postsDeferred = async { repo.getLieuPosts(lieuId) }
                    lieu = lieuDeferred.await().getOrNull()
                    posts = postsDeferred.await().getOrElse { emptyList() }
                }
            } catch (_: Exception) { }
            isLoading = false
        }
    }
}

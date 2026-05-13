package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.RetrofitClient
import com.example.locus.data.remote.UserAvisResponse
import com.example.locus.data.repository.PostRepository
import com.example.locus.data.repository.TravelPathRepository
import com.example.locus.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class PublicProfileUiState(
    val userId: Int = 0,
    val username: String = "",
    val ppurl: String? = null,
    val posts: List<PostResponse> = emptyList(),
    val likeCounts: Map<Int, Int> = emptyMap(),
    val userAvis: List<UserAvisResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isFollowing: Boolean = false
)

class PublicProfileViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val travelPathRepository: TravelPathRepository = TravelPathRepository(RetrofitClient.api)
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int, token: String = "") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userId = userId) }
            try {
                supervisorScope {
                    val profileDeferred = async { postRepository.getPublicProfile(userId) }
                    val isFollowingDeferred = async {
                        if (token.isNotEmpty()) {
                            try { userRepository.getMyFollowing(token).any { it.id == userId } }
                            catch (_: Exception) { false }
                        } else false
                    }
                    val avisDeferred = async {
                        travelPathRepository.getUserAvis(userId).getOrElse { emptyList() }
                    }
                    val profile = profileDeferred.await()
                    val posts = profile.posts ?: emptyList()
                    val likeCountsMap = if (token.isNotEmpty()) {
                        posts.map { post -> async { post.id to postRepository.getLikesForPost(token, post.id) } }
                            .awaitAll().toMap()
                    } else emptyMap()
                    _uiState.update {
                        it.copy(
                            username = profile.username,
                            ppurl = profile.ppurl,
                            posts = posts,
                            likeCounts = likeCountsMap,
                            userAvis = avisDeferred.await(),
                            isLoading = false,
                            isFollowing = isFollowingDeferred.await()
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFollow(token: String) {
        val userId = _uiState.value.userId
        val wasFollowing = _uiState.value.isFollowing
        _uiState.update { it.copy(isFollowing = !wasFollowing) }
        viewModelScope.launch {
            try {
                if (wasFollowing) userRepository.unfollowUser(token, userId)
                else userRepository.followUser(token, userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isFollowing = wasFollowing) }
            }
        }
    }
}

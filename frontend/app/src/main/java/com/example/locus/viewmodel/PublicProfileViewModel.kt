package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.repository.PostRepository
import com.example.locus.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicProfileUiState(
    val userId: Int = 0,
    val username: String = "",
    val ppurl: String? = null,
    val posts: List<PostResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isFollowing: Boolean = false
)

class PublicProfileViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userId = userId) }
            try {
                val profile = postRepository.getPublicProfile(userId)
                _uiState.update {
                    it.copy(
                        username = profile.username,
                        ppurl = profile.ppurl,
                        posts = profile.posts ?: emptyList(),
                        isLoading = false
                    )
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

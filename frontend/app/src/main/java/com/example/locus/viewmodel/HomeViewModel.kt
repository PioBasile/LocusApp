package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.Post
import com.example.locus.data.repository.PostRepository
import kotlinx.coroutines.launch

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedGroupId: Int = 0  // default group
)

class HomeViewModel(
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    fun loadPosts(token: String, groupId: Int = uiState.selectedGroupId) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            try {
                val posts = postRepository.getPostsByGroup(token, groupId)
                uiState = uiState.copy(posts = posts, isLoading = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    error = "Failed to load posts: ${e.localizedMessage}"
                )
            }
        }
    }

    fun selectGroup(token: String, groupId: Int) {
        uiState = uiState.copy(selectedGroupId = groupId)
        loadPosts(token, groupId)
    }
}
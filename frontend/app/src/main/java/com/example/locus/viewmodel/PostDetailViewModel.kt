package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.remote.CommentResponse
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.repository.PostRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class PostDetailViewModel(
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    var post by mutableStateOf<PostResponse?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var authorName by mutableStateOf("")
        private set
    var authorPpUrl by mutableStateOf<String?>(null)
        private set
    var likeCount by mutableStateOf(0)
        private set
    var isLiked by mutableStateOf(false)
        private set
    var isLikeInFlight by mutableStateOf(false)
        private set
    var comments by mutableStateOf<List<CommentResponse>>(emptyList())
        private set

    var locationGps by mutableStateOf<String?>(null)
        private set

    fun load(postId: Int, token: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val fetchedPost = postRepository.getPost(postId)
                post = fetchedPost
                supervisorScope {
                    val profileDeferred = async { postRepository.getPublicProfile(fetchedPost.user_id) }
                    val likeCountDeferred = async { if (token.isNotEmpty()) postRepository.getLikesForPost(token, postId) else 0 }
                    val likedIdsDeferred = async { if (token.isNotEmpty()) postRepository.getAllUserLikes(token) else emptyList() }
                    val commentsDeferred = async { if (token.isNotEmpty()) postRepository.getComments(token, postId) else emptyList() }
                    val gpsDeferred = async { fetchedPost.locGps ?: fetchedPost.id_loc?.let { postRepository.getLocationGps(it) } }

                    val profile = profileDeferred.await()
                    authorName = profile.username
                    authorPpUrl = profile.ppurl
                    likeCount = likeCountDeferred.await()
                    isLiked = postId in likedIdsDeferred.await()
                    comments = commentsDeferred.await()
                    locationGps = gpsDeferred.await()
                }
            } catch (e: Exception) { }
            isLoading = false
        }
    }

    fun toggleLike(token: String, postId: Int) {
        if (isLikeInFlight) return
        val wasLiked = isLiked
        isLiked = !wasLiked
        likeCount += if (isLiked) 1 else -1
        isLikeInFlight = true
        viewModelScope.launch {
            try {
                if (wasLiked) postRepository.unlikePost(token, postId)
                else postRepository.likePost(token, postId)
                likeCount = postRepository.getLikesForPost(token, postId)
            } catch (e: Exception) {
                isLiked = wasLiked
                likeCount += if (wasLiked) 1 else -1
            } finally {
                isLikeInFlight = false
            }
        }
    }

    fun addComment(token: String, postId: Int, text: String) {
        viewModelScope.launch {
            try {
                postRepository.addComment(token, postId, text)
                comments = postRepository.getComments(token, postId)
            } catch (e: Exception) { }
        }
    }
}

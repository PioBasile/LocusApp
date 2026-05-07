package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.Post
import com.example.locus.data.remote.CommentResponse
import com.example.locus.data.remote.GroupDetailResponse
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.PublicProfileResponse
import com.example.locus.data.repository.GroupRepository
import com.example.locus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedGroupId: Int = 0  // default group
)

class HomeViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val grprepository: GroupRepository = GroupRepository()
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var groupDetail by mutableStateOf<GroupDetailResponse?>(null)
        private set

    private val _posts = MutableStateFlow<List<PostResponse>>(emptyList())
    val posts: StateFlow<List<PostResponse>> = _posts.asStateFlow()


    private val _userGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val userGroups: StateFlow<List<MyGroupResponse>> = _userGroups.asStateFlow()

    fun loadPostsForGroup(token: String, groupId: Int) {
        viewModelScope.launch {
            val fetchedPosts = postRepository.getPostsByGroup(token, groupId)
            _posts.value = fetchedPosts
        }
    }


    fun loadUserGroups(token: String) {
        viewModelScope.launch {
            try {
                // 1. Get the list of IDs: [2, 3]
                val groupIds = grprepository.getMyGroups(token)

                val loadedGroups = mutableListOf<MyGroupResponse>()

                // 2. Loop through each ID and fetch details
                for (id in groupIds) {
                    val groupDetail = grprepository.getGroupDetails(id)

                    if (groupDetail != null) {
                        loadedGroups.add(
                            MyGroupResponse(
                                id = id,
                                name = groupDetail.name,
                                isPrivate = false,
                                description = "",
                                imageUrl = groupDetail.imageUrl
                            )
                        )
                    }
                }

                // 3. Update the state with the full list
                _userGroups.value = loadedGroups

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Fonction pour charger les détails d'un groupe spécifique
    fun loadGroup(groupId: Int) {
        viewModelScope.launch {
            isLoading = true
            groupDetail = grprepository.getGroupDetails(groupId)
            isLoading = false
        }
    }

    // -- Likes state ---------------------------------------------------
    private val _likedPostIds = MutableStateFlow<Set<Int>>(emptySet())
    val likedPostIds: StateFlow<Set<Int>> = _likedPostIds.asStateFlow()

    private val _likeCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val likeCounts: StateFlow<Map<Int, Int>> = _likeCounts.asStateFlow()

    // -- Comment counts state ------------------------------------------
    private val _commentCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val commentCounts: StateFlow<Map<Int, Int>> = _commentCounts.asStateFlow()

    // -- Comment sheet state -------------------------------------------
    private val _currentComments = MutableStateFlow<List<CommentResponse>>(emptyList())
    val currentComments: StateFlow<List<CommentResponse>> = _currentComments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    // -- Fonctions -----------------------------------------------------

    fun loadUserLikes(token: String) {
        viewModelScope.launch {
            try {
                val liked = postRepository.getAllUserLikes(token)
                _likedPostIds.value = liked.toSet()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load user likes: ${e.message}")
            }
        }
    }

    suspend fun loadLikesForPost(token: String, postId: Int) {
        val count = postRepository.getLikesForPost(token, postId)
        _likeCounts.value = _likeCounts.value + (postId to count)
    }

    fun loadCommentsForPost(token: String, postId: Int) {
        viewModelScope.launch {
            _isLoadingComments.value = true
            _currentComments.value = emptyList()
            val fetchedComments = postRepository.getComments(token, postId)
            _currentComments.value = fetchedComments
            _commentCounts.value = _commentCounts.value + (postId to fetchedComments.size)
            _isLoadingComments.value = false
        }
    }

    fun addComment(token: String, postId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                postRepository.addComment(token, postId, text)
                loadCommentsForPost(token, postId)
            } catch (e: Exception) {
                println("Erreur lors de l'ajout du commentaire : ${e.message}")
            }
        }
    }

    suspend fun getCommentCountForPost(token: String, postId: Int) {
        try {
            val comments = postRepository.getComments(token, postId)
            _commentCounts.value = _commentCounts.value + (postId to comments.size)
        } catch (e: Exception) {
            // leave existing count unchanged
        }
    }

    fun toggleLike(token: String, postId: Int, isNowLiked: Boolean) {
        // Optimistic update
        if (isNowLiked) {
            _likedPostIds.value = _likedPostIds.value + postId
            _likeCounts.value = _likeCounts.value + (postId to ((_likeCounts.value[postId] ?: 0) + 1))
        } else {
            _likedPostIds.value = _likedPostIds.value - postId
            _likeCounts.value = _likeCounts.value + (postId to maxOf(0, (_likeCounts.value[postId] ?: 0) - 1))
        }
        viewModelScope.launch {
            try {
                if (isNowLiked) postRepository.likePost(token, postId)
                else postRepository.unlikePost(token, postId)
            } catch (e: Exception) {
                // Revert optimistic update on failure
                if (isNowLiked) {
                    _likedPostIds.value = _likedPostIds.value - postId
                    _likeCounts.value = _likeCounts.value + (postId to maxOf(0, (_likeCounts.value[postId] ?: 1) - 1))
                } else {
                    _likedPostIds.value = _likedPostIds.value + postId
                    _likeCounts.value = _likeCounts.value + (postId to ((_likeCounts.value[postId] ?: 0) + 1))
                }
                android.util.Log.e("HomeViewModel", "Like failed: ${e.message}")
            }
        }
    }

    suspend fun getPublicProfile(userId: Int): PublicProfileResponse? {
        return try {
            postRepository.getPublicProfile(userId)
        } catch (e: Exception) {
            null
        }
    }

    // -- Follow a user ---------------------------------------------
    fun followUser(token: String, targetUserId: Int) {
        viewModelScope.launch {
            try {
                postRepository.followUser(token, targetUserId)
            } catch (e: Exception) {
                // silent fail — UI already toggled optimistically
                android.util.Log.e("HomeViewModel", "Follow failed: ${e.message}")
            }
        }
    }

    // -- Report a post ---------------------------------------------
    fun reportPost(token: String, postId: Int, reason: String, comment: String) {
        viewModelScope.launch {
            try {
                postRepository.reportPost(token, postId, reason, comment)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Report failed: ${e.message}")
            }
        }
    }

    // -- Delete a post ---------------------------------------------
    fun deletePost(token: String, postId: Int) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(token, postId)
                // Remove from local list immediately
                uiState = uiState.copy(
                    posts = uiState.posts.filter { it.id != postId }
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Delete failed: ${e.message}")
            }
        }
    }
}
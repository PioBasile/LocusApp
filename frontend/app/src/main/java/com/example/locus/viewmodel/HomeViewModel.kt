package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.Post
import com.example.locus.data.remote.CommentResponse
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.remote.GroupDetailResponse
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.PublicProfileResponse
import com.example.locus.data.repository.GroupRepository
import com.example.locus.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedGroupId: Int = 0
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
    var selectedGroup by mutableStateOf<MyGroupResponse?>(null)
        private set

    fun selectGroup(group: MyGroupResponse?) {
        selectedGroup = group
    }

    private val _posts = MutableStateFlow<List<PostResponse>>(emptyList())
    val posts: StateFlow<List<PostResponse>> = _posts.asStateFlow()

    private val _userGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val userGroups: StateFlow<List<MyGroupResponse>> = _userGroups.asStateFlow()

    private val _followingUserIds = MutableStateFlow<Set<Int>>(emptySet())
    val followingUserIds: StateFlow<Set<Int>> = _followingUserIds.asStateFlow()

    fun loadFollowing(token: String) {
        viewModelScope.launch {
            try {
                _followingUserIds.value = postRepository.getMyFollowing(token).map { it.id }.toSet()
            } catch (e: Exception) { }
        }
    }

    fun loadPostsForGroup(token: String, groupId: Int) {
        viewModelScope.launch {
            val fetchedPosts = postRepository.getPostsByGroup(token, groupId)
            _posts.value = fetchedPosts
        }
    }

    fun loadUserGroups(token: String) {
        viewModelScope.launch {
            try {
                val groupIds = grprepository.getMyGroups(token)
                val loadedGroups = mutableListOf<MyGroupResponse>()
                for (id in groupIds) {
                    val detail = grprepository.getGroupDetails(id)
                    if (detail != null) {
                        loadedGroups.add(
                            MyGroupResponse(id = id, name = detail.name, isPrivate = false, description = "", imageUrl = detail.imageUrl)
                        )
                    }
                }
                _userGroups.value = loadedGroups
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    private val _likeJobs = mutableMapOf<Int, Job>()
    private var likesInitialized = false

    // -- Comment counts state ------------------------------------------
    private val _commentCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val commentCounts: StateFlow<Map<Int, Int>> = _commentCounts.asStateFlow()

    // -- Comment sheet state -------------------------------------------
    private val _currentComments = MutableStateFlow<List<CommentResponse>>(emptyList())
    val currentComments: StateFlow<List<CommentResponse>> = _currentComments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    fun loadUserLikes(token: String) {
        if (likesInitialized) return
        viewModelScope.launch {
            try {
                val liked = postRepository.getAllUserLikes(token)
                _likedPostIds.value = liked.toSet()
                likesInitialized = true
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
            val fetched = postRepository.getComments(token, postId)
            _currentComments.value = fetched
            _commentCounts.value = _commentCounts.value + (postId to fetched.size)
            _isLoadingComments.value = false
        }
    }

    fun addComment(token: String, postId: Int, text: String, audioFile: File? = null) {
        if (text.isBlank() && audioFile == null) return
        viewModelScope.launch {
            try {
                postRepository.addComment(token, postId, text, audioFile)
                loadCommentsForPost(token, postId)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Comment failed: ${e.message}")
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
        // Cancel any pending API call for this post — rapid clicks only fire the last state
        _likeJobs[postId]?.cancel()

        // Optimistic update immediately
        if (isNowLiked) {
            _likedPostIds.value = _likedPostIds.value + postId
            _likeCounts.value = _likeCounts.value + (postId to ((_likeCounts.value[postId] ?: 0) + 1))
        } else {
            _likedPostIds.value = _likedPostIds.value - postId
            _likeCounts.value = _likeCounts.value + (postId to maxOf(0, (_likeCounts.value[postId] ?: 0) - 1))
        }

        _likeJobs[postId] = viewModelScope.launch {
            delay(400) // debounce: only fire API if no further tap within 400ms
            try {
                if (isNowLiked) postRepository.likePost(token, postId)
                else postRepository.unlikePost(token, postId)
                val serverCount = postRepository.getLikesForPost(token, postId)
                _likeCounts.value = _likeCounts.value + (postId to serverCount)
            } catch (e: Exception) {
                if (isNowLiked) {
                    _likedPostIds.value = _likedPostIds.value - postId
                    _likeCounts.value = _likeCounts.value + (postId to maxOf(0, (_likeCounts.value[postId] ?: 1) - 1))
                } else {
                    _likedPostIds.value = _likedPostIds.value + postId
                    _likeCounts.value = _likeCounts.value + (postId to ((_likeCounts.value[postId] ?: 0) + 1))
                }
                android.util.Log.e("HomeViewModel", "Like failed: ${e.message}")
            } finally {
                _likeJobs.remove(postId)
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

    fun followUser(token: String, targetUserId: Int) {
        _followingUserIds.value = _followingUserIds.value + targetUserId
        viewModelScope.launch {
            try {
                postRepository.followUser(token, targetUserId)
            } catch (e: Exception) {
                _followingUserIds.value = _followingUserIds.value - targetUserId
                android.util.Log.e("HomeViewModel", "Follow failed: ${e.message}")
            }
        }
    }

    fun unfollowUser(token: String, targetUserId: Int) {
        _followingUserIds.value = _followingUserIds.value - targetUserId
        viewModelScope.launch {
            try {
                postRepository.unfollowUser(token, targetUserId)
            } catch (e: Exception) {
                _followingUserIds.value = _followingUserIds.value + targetUserId
                android.util.Log.e("HomeViewModel", "Unfollow failed: ${e.message}")
            }
        }
    }

    fun reportPost(token: String, postId: Int, reason: String, comment: String) {
        viewModelScope.launch {
            try {
                postRepository.reportPost(token, postId, reason, comment)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Report failed: ${e.message}")
            }
        }
    }

    fun deletePost(token: String, postId: Int) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(token, postId)
                _posts.value = _posts.value.filter { it.id != postId }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Delete failed: ${e.message}")
            }
        }
    }
}

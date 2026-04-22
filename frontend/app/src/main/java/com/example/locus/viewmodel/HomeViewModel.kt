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

    // -- État des commentaires -----------------------------------------
    private val _currentComments = MutableStateFlow<List<CommentResponse>>(emptyList())
    val currentComments: StateFlow<List<CommentResponse>> = _currentComments.asStateFlow()

    private val _isLoadingComments = MutableStateFlow(false)
    val isLoadingComments: StateFlow<Boolean> = _isLoadingComments.asStateFlow()

    // -- Fonctions -----------------------------------------------------

    // Appelé quand on clique sur le bouton commentaire d'un post
    fun loadCommentsForPost(token: String, postId: Int) {
        viewModelScope.launch {
            _isLoadingComments.value = true
            // On vide la liste temporairement pour éviter de voir les commentaires du post précédent
            _currentComments.value = emptyList()

            // Appel à repository
            val fetchedComments = postRepository.getComments(token, postId)
            _currentComments.value = fetchedComments

            _isLoadingComments.value = false
        }
    }

    // Appelé quand on clique sur "Envoyer" dans le Bottom Sheet
    fun addComment(token: String, postId: Int, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                postRepository.addComment(token, postId, text)
                // On recharge directement les commentaires pour voir le nouveau apparaître !
                loadCommentsForPost(token, postId)
            } catch (e: Exception) {
                println("Erreur lors de l'ajout du commentaire : ${e.message}")
            }
        }
    }

    suspend fun getCommentCountForPost(token: String, postId: Int): Int {
        return try {
            val comments = postRepository.getComments(token, postId)
            comments.size
        } catch (e: Exception) {
            0
        }
    }

    fun toggleLike(token: String, postId: Int, isNowLiked: Boolean) {
        viewModelScope.launch {
            try {
                if (isNowLiked) {
                    postRepository.likePost(token, postId)
                } else {
                    postRepository.unlikePost(token, postId)
                }
            } catch (e: Exception) {
                println("Erreur lors du like : ${e.message}")
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
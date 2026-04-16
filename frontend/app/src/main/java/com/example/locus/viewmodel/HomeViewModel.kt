package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.Post
import com.example.locus.data.remote.GroupDetailResponse
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.data.remote.PostResponse
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
}
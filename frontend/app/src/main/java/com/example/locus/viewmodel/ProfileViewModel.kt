package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.data.remote.PostResponse
import com.example.locus.data.remote.ProfileResponse
import com.example.locus.data.repository.GroupRepository
import com.example.locus.data.repository.PostRepository
import com.example.locus.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ProfileUiState(
    val profile: ProfileResponse? = null,
    val followers: List<FollowerResponse> = emptyList(),
    val userPosts: List<PostResponse> = emptyList(),
    val userGroupDetails: List<MyGroupResponse> = emptyList(),
    val groupCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repository: UserRepository,
    private val groupRepository: GroupRepository = GroupRepository(),
    private val postRepository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadFullProfile(token: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    profile = null,
                    followers = emptyList(),
                    userPosts = emptyList(),
                    userGroupDetails = emptyList(),
                    groupCount = 0
                )
            }
            try {
                val profileDeferred = async { repository.getProfile(token) }
                val followersDeferred = async { repository.getFollowers(token) }
                val groupIdsDeferred = async { groupRepository.getMyGroups(token) }

                val profile = profileDeferred.await()
                val followers = followersDeferred.await()
                val groupIds = groupIdsDeferred.await()

                // Fetch posts from every group the user belongs to + public (group 0), in parallel
                val allGroupIds = (groupIds + 0).distinct()
                val allPosts = allGroupIds
                    .map { groupId -> async { postRepository.getPostsByGroup(token, groupId) } }
                    .awaitAll()
                    .flatten()

                // Deduplicate (a post can belong to multiple groups) then keep only this user's
                val userPosts = allPosts
                    .distinctBy { it.id }
                    .filter { it.user_id == profile.id }

                // Load group names for filter chips — parallel, group 0 handled locally
                val namedGroups = mutableListOf(
                    MyGroupResponse(id = 0, name = "Public", isPrivate = false, description = null, imageUrl = null)
                )
                groupIds
                    .map { id -> async { groupRepository.getGroupDetails(id)?.let { MyGroupResponse(id = id, name = it.name, isPrivate = false, description = null, imageUrl = it.imageUrl) } } }
                    .awaitAll()
                    .filterNotNull()
                    .let { namedGroups.addAll(it) }

                _uiState.update {
                    it.copy(
                        profile = profile,
                        followers = followers,
                        groupCount = groupIds.size,
                        userPosts = userPosts,
                        userGroupDetails = namedGroups,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load profile: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateProfilePicture(token: String, imageFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                repository.uploadProfilePicture(token, imageFile)
                val updatedProfile = repository.getProfile(token)
                _uiState.update {
                    it.copy(
                        profile = updatedProfile,
                        isLoading = false,
                        successMessage = "Profile picture updated!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Upload failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun followUser(token: String, targetUserId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.followUser(token, targetUserId)
                _uiState.update { it.copy(successMessage = response) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Follow failed: ${e.message}") }
            }
        }
    }

    fun unfollowUser(token: String, targetUserId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.unfollowUser(token, targetUserId)
                _uiState.update { it.copy(successMessage = response) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Unfollow failed: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    object ProfileViewModelFactory {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(
                    repository = UserRepository(),
                    groupRepository = GroupRepository(),
                    postRepository = PostRepository()
                )
            }
        }
    }
}

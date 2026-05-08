package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.Group
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.repository.GroupRepository
import com.example.locus.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ExploreUiState(
    val groups: List<Group> = emptyList(),
    val isLoadingGroups: Boolean = false,
    val groupsError: String? = null,
    val joinSuccess: String? = null,
    val joinError: String? = null,
    val createSuccess: String? = null,
    val createError: String? = null
)

class ExploreViewModel(
    private val groupRepository: GroupRepository = GroupRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ExploreUiState())
        private set

    private val _topUsers = MutableStateFlow<List<FollowerResponse>>(emptyList())
    val topUsers: StateFlow<List<FollowerResponse>> = _topUsers.asStateFlow()

    private val _followedUserIds = MutableStateFlow<Set<Int>>(emptySet())
    val followedUserIds: StateFlow<Set<Int>> = _followedUserIds.asStateFlow()

    private val _myGroupIds = MutableStateFlow<Set<Int>>(emptySet())
    val myGroupIds: StateFlow<Set<Int>> = _myGroupIds.asStateFlow()

    init {
        loadGroups()
        loadTopUsers()
    }

    fun loadMyGroups(token: String) {
        viewModelScope.launch {
            _myGroupIds.value = groupRepository.getMyGroups(token).toSet()
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingGroups = true, groupsError = null)
            try {
                val groups: List<Group> = groupRepository.getGroups()
                uiState = uiState.copy(groups = groups, isLoadingGroups = false)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoadingGroups = false,
                    groupsError = "Failed to load groups: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadFollowingIds(token: String) {
        viewModelScope.launch {
            try {
                _followedUserIds.value = userRepository.getMyFollowing(token).map { it.id }.toSet()
            } catch (e: Exception) { }
        }
    }

    fun loadTopUsers() {
        viewModelScope.launch {
            _topUsers.value = userRepository.getTopUsers(50)
        }
    }

    fun joinGroup(token: String, groupId: Int, password: String = "") {
        viewModelScope.launch {
            try {
                groupRepository.joinGroup(token, groupId, password)
                _myGroupIds.value = _myGroupIds.value + groupId
                uiState = uiState.copy(joinSuccess = "You've joined the group!", joinError = null)
            } catch (e: Exception) {
                uiState = uiState.copy(
                    joinError = e.localizedMessage ?: "Failed to join group",
                    joinSuccess = null
                )
            }
        }
    }

    fun createGroup(
        token: String,
        name: String,
        description: String,
        isPrivate: Boolean,
        password: String = "",
        imageFile: File
    ) {
        viewModelScope.launch {
            try {
                groupRepository.makeGroup(token, name, description, isPrivate, password, imageFile)
                uiState = uiState.copy(createSuccess = "Group created successfully!", createError = null)
                loadGroups()
            } catch (e: Exception) {
                uiState = uiState.copy(
                    createError = e.localizedMessage ?: "Failed to create group",
                    createSuccess = null
                )
            }
        }
    }

    fun followUser(token: String, targetUserId: Int) {
        _followedUserIds.value = _followedUserIds.value + targetUserId
        viewModelScope.launch {
            try {
                userRepository.followUser(token, targetUserId)
            } catch (e: Exception) {
                _followedUserIds.value = _followedUserIds.value - targetUserId
                android.util.Log.e("ExploreViewModel", "Follow failed: ${e.message}")
            }
        }
    }

    fun unfollowUser(token: String, targetUserId: Int) {
        _followedUserIds.value = _followedUserIds.value - targetUserId
        viewModelScope.launch {
            try {
                userRepository.unfollowUser(token, targetUserId)
            } catch (e: Exception) {
                _followedUserIds.value = _followedUserIds.value + targetUserId
                android.util.Log.e("ExploreViewModel", "Unfollow failed: ${e.message}")
            }
        }
    }

    suspend fun getGroupMemberCount(groupId: Int): Int =
        try { groupRepository.getGroupDetails(groupId)?.members?.size ?: 0 } catch (e: Exception) { 0 }

    fun clearMessages() {
        uiState = uiState.copy(
            joinSuccess = null,
            joinError = null,
            createSuccess = null,
            createError = null
        )
    }
}

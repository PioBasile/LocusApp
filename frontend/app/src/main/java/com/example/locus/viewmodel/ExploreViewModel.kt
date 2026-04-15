package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.model.Group
import com.example.locus.data.repository.GroupRepository
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
    private val groupRepository: GroupRepository = GroupRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ExploreUiState())
        private set

    init {
        loadGroups()
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

    fun joinGroup(token: String, groupId: Int, password: String = "") {
        viewModelScope.launch {
            try {
                val result = groupRepository.joinGroup(token, groupId, password)
                uiState = uiState.copy(joinSuccess = result.message, joinError = null)
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
                val result = groupRepository.makeGroup(token, name, description, isPrivate, password, imageFile)
                uiState = uiState.copy(createSuccess = result.message, createError = null)
                loadGroups() // refresh list after creating
            } catch (e: Exception) {
                uiState = uiState.copy(
                    createError = e.localizedMessage ?: "Failed to create group",
                    createSuccess = null
                )
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(
            joinSuccess = null,
            joinError = null,
            createSuccess = null,
            createError = null
        )
    }
}


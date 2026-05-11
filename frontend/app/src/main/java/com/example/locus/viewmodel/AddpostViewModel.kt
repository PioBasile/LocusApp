package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.remote.GroupDetailResponse
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.data.repository.GroupRepository
import com.example.locus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AddPostViewModel(
    private val repository: PostRepository = PostRepository(),
    private val grprepository: GroupRepository = GroupRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _userGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val userGroups: StateFlow<List<MyGroupResponse>> = _userGroups.asStateFlow()

    var groupDetail by mutableStateOf<GroupDetailResponse?>(null)
        private set

    fun uploadPost(
        token: String,
        imageFile: File,
        description: String,
        groupIds: List<Int>,
        locationId: Int,
        audioFile: File? = null,
        aiTags: Boolean = true,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            isLoading = true
            successMessage = null
            errorMessage = null
            try {
                val result = repository.uploadPost(token, imageFile, description, groupIds, locationId, audioFile, aiTags, tags)
                successMessage = result
            } catch (e: Exception) {
                errorMessage = "Erreur de connexion : ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun clearMessages() {
        successMessage = null
        errorMessage = null
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
}

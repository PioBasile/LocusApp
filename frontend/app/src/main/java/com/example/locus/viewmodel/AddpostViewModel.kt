package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// N'oublie pas d'importer GroupDetailResponse !
import com.example.locus.data.remote.GroupDetailResponse
import com.example.locus.data.remote.GroupResponse
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

    // On garde un seul isLoading pour tout le ViewModel !
    var isLoading by mutableStateOf(false)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val _userGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val userGroups: StateFlow<List<MyGroupResponse>> = _userGroups.asStateFlow()

    // Variable pour stocker les détails d'un groupe
    var groupDetail by mutableStateOf<GroupDetailResponse?>(null)
        private set

    fun uploadPost(token: String, imageFile: File, description: String, groupIds: List<Int>, locationId: Int) {
        viewModelScope.launch {
            isLoading = true
            successMessage = null
            errorMessage = null

            try {
                // Appel au repository
                val result = repository.uploadPost(token, imageFile, description, groupIds, locationId)
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

    // Fonction pour récupérer la liste des groupes
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
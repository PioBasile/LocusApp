package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.remote.ProfileResponse
import com.example.locus.data.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ProfileUiState(
    val profile: ProfileResponse? = null,
    val followers: List<FollowerResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // -- Load profile + followers in parallel ----------------------
    fun loadFullProfile(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profileDeferred = async { repository.getProfile(token) }
                val followersDeferred = async { repository.getFollowers(token) }

                val profile = profileDeferred.await()
                val followers = followersDeferred.await()

                _uiState.update {
                    it.copy(
                        profile = profile,
                        followers = followers,
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

    // -- Update profile picture ------------------------------------
    fun updateProfilePicture(token: String, imageFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                repository.uploadProfilePicture(token, imageFile)
                // Reload profile to show new picture
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

    // -- Follow ----------------------------------------------------
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

    // -- Unfollow --------------------------------------------------
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

    // -- Clear messages --------------------------------------------
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // -- Factory ---------------------------------------------------
    object ProfileViewModelFactory {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProfileViewModel(repository = UserRepository())
            }
        }
    }
}
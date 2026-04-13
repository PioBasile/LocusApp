package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.data.remote.ProfileResponse
import com.example.locus.data.repository.UserRepository
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

    // -- Chargement des données initiales --------------------------

    fun loadFullProfile(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // On peut lancer les deux requêtes en parallèle si on veut,
                // mais les faire à la suite est plus simple pour gérer les erreurs
                val profileData = repository.getProfile(token)
                val followersData = repository.getFollowers(token)

                _uiState.update {
                    it.copy(
                        profile = profileData,
                        followers = followersData,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Erreur de chargement : ${e.message}"
                    )
                }
            }
        }
    }

    // -- Modification de la photo de profil ------------------------

    fun updateProfilePicture(token: String, imageFile: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val response = repository.changeProfilePicture(token, imageFile)

                // Si ça réussit, on recharge le profil pour afficher la nouvelle image
                val updatedProfile = repository.getProfile(token)

                _uiState.update {
                    it.copy(
                        profile = updatedProfile,
                        isLoading = false,
                        successMessage = response.message ?: "Photo mise à jour avec succès !"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Échec de l'upload : ${e.message}"
                    )
                }
            }
        }
    }

    // -- Actions Sociales (Follow / Unfollow) ----------------------

    fun followUser(token: String, targetUserId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.followUser(token, targetUserId)
                _uiState.update { it.copy(successMessage = response) }

                // Optionnel : recharger les followers si c'est nécessaire pour l'UI
                // loadFullProfile(token)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur lors du follow : ${e.message}") }
            }
        }
    }

    fun unfollowUser(token: String, targetUserId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.unfollowUser(token, targetUserId)
                _uiState.update { it.copy(successMessage = response) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erreur lors du unfollow : ${e.message}") }
            }
        }
    }

    // -- Nettoyage des messages ------------------------------------

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    object ProfileViewModelFactory {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Ici, tu indiques comment créer le ViewModel
                // Tu instancies le UserRepository dont il a besoin
                ProfileViewModel(
                    repository = UserRepository()
                )
            }
        }
    }
}
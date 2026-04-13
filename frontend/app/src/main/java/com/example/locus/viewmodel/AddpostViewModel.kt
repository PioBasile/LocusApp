package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.repository.PostRepository
import kotlinx.coroutines.launch
import java.io.File

class AddPostViewModel(
    private val repository: PostRepository = PostRepository() // ← was AddPostRepository
) : ViewModel() {

    // États de l'UI
    var isLoading by mutableStateOf(false)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
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
}
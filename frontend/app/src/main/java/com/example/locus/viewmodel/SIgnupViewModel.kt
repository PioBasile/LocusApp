package com.example.locus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SignupUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val token: String? = null
)

class SignupViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    var uiState by mutableStateOf(SignupUiState())
        private set

    fun sendFCMToken(token: String) {
        viewModelScope.launch {
            try {
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                userRepository.updateFCMToken(token, fcmToken)
            } catch (e: Exception) {
                // non-critical — silently ignored
            }
        }
    }

    fun signup(username: String, email: String, password: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            try {
                userRepository.signup(username, email, password)

                val loginResponse = userRepository.login(email, password)

                uiState = uiState.copy(
                    isLoading = false,
                    isSuccess = true,
                    token = loginResponse.token
                )

            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Erreur : ${e.message}"
                )
            }
        }
    }
}
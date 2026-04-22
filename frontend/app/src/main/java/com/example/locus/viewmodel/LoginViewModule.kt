package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.locus.data.repository.UserRepository
import com.example.locus.utils.decodeUserIdFromToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val token: String? = null,
    val userId: Int? = null,
    val errorMessage: String? = null
)

class UserViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please fill in all fields")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = userRepository.login(email, password)
                val userId = decodeUserIdFromToken(response.token)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    token = response.token,
                    userId = userId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Login failed: ${e.message}"
                )
            }
        }
    }

    fun continueAsGuest() {
        _uiState.value = _uiState.value.copy(
            isSuccess = true,
            token = null,
            userId = null
        )
    }

    fun saveTokenFromSignup(newToken: String) {
        val userId = decodeUserIdFromToken(newToken)
        _uiState.value = _uiState.value.copy(
            isSuccess = true,
            token = newToken,
            userId = userId,
            errorMessage = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
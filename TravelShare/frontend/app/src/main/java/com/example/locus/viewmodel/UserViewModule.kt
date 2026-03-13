package com.example.locus.viewmodel

import androidx.lifecycle.ViewModel
import com.example.locus.data.model.User
import com.example.locus.data.remote.ApiService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserViewModel(private val apiService: ApiService) : ViewModel() {
    var users by mutableStateOf<List<User>>(emptyList())
        private set

    fun fetchUsers() {
        viewModelScope.launch {
            try {
                users = apiService.getUsers()
            } catch (e: Exception) {
                // Gérer l'erreur ici (ex: log)
            }
        }
    }
}

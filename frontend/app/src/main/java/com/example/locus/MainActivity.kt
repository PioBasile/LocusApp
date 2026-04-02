package com.example.locus


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.screens.AddPostScreen
import com.example.locus.ui.screens.ExploreScreen
import com.example.locus.ui.screens.HomeScreen
import com.example.locus.ui.screens.LoginScreen
import com.example.locus.ui.screens.ProfileScreen
import com.example.locus.ui.theme.LocusTheme
import com.example.locus.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocusTheme {
                val uiState by viewModel.uiState.collectAsState()
                var currentNav by remember { mutableStateOf(NavDestination.HOME) }

                when {
                    !uiState.isSuccess -> {
                        LoginScreen(
                            onLoginClick = { email, password -> viewModel.login(email, password) },
                            onGuestClick = { viewModel.continueAsGuest() },
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage
                        )
                    }
                    else -> {
                        when (currentNav) {
                            NavDestination.HOME -> HomeScreen(
                                isGuest = uiState.token == null,
                                onNavigate = { currentNav = it }
                            )
                            NavDestination.ADD -> AddPostScreen(
                                onNavigate = { currentNav = it },
                                token = uiState.token ?: "",
                            )
                            NavDestination.COMPASS -> HomeScreen(
                                onNavigate = { currentNav = it }
                            )
                            NavDestination.EXPLORE -> ExploreScreen(
                                onNavigate = { currentNav = it }
                            )
                            NavDestination.PROFILE -> ProfileScreen(
                                onNavigate = { currentNav = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.example.locus


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.locus.ui.screens.PostDetailScreen
import com.example.locus.ui.screens.ProfileScreen
import com.example.locus.ui.screens.PublicProfileScreen
import com.example.locus.ui.screens.MapScreen
import com.example.locus.ui.screens.SignupScreen
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
                var showSignup by remember { mutableStateOf(false) }
                var viewingUserId by remember { mutableStateOf<Int?>(null) }
                var viewingPostId by remember { mutableStateOf<Int?>(null) }

                val onUserClick: (Int) -> Unit = { userId ->
                    if (userId != uiState.userId) viewingUserId = userId
                }
                val onPostClick: (Int) -> Unit = { postId -> viewingPostId = postId }

                when {
                    !uiState.isSuccess && !showSignup -> LoginScreen(
                        onLoginClick = { email, password -> viewModel.login(email, password) },
                        onGuestClick = { viewModel.continueAsGuest() },
                        onSignupClick = { showSignup = true },
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage
                    )
                    !uiState.isSuccess && showSignup -> SignupScreen(
                        onSignupSuccess = { token ->
                            showSignup = false
                            viewModel.saveTokenFromSignup(token)
                        },
                        onBackToLogin = { showSignup = false }
                    )
                    else -> {
                        when {
                            viewingPostId != null -> PostDetailScreen(
                                postId = viewingPostId!!,
                                token = uiState.token ?: "",
                                onBack = { viewingPostId = null }
                            )
                            viewingUserId != null -> PublicProfileScreen(
                                userId = viewingUserId!!,
                                token = uiState.token ?: "",
                                currentUserId = uiState.userId,
                                onBack = { viewingUserId = null },
                                onPostClick = onPostClick
                            )
                            else -> when (currentNav) {
                                NavDestination.HOME -> HomeScreen(
                                    token = uiState.token ?: "",
                                    currentUserId = uiState.userId,
                                    isGuest = uiState.token == null,
                                    onNavigate = { currentNav = it },
                                    onUserClick = onUserClick
                                )
                                NavDestination.ADD -> AddPostScreen(
                                    onNavigate = { currentNav = it },
                                    token = uiState.token ?: ""
                                )
                                NavDestination.COMPASS -> MapScreen(
                                    onNavigate = { currentNav = it },
                                    onPostClick = onPostClick
                                )
                                NavDestination.EXPLORE -> ExploreScreen(
                                    token = uiState.token ?: "",
                                    currentUserId = uiState.userId,
                                    onNavigate = { currentNav = it },
                                    onUserClick = onUserClick
                                )
                                NavDestination.PROFILE -> ProfileScreen(
                                    token = uiState.token ?: "",
                                    currentUserId = uiState.userId,
                                    onNavigate = { currentNav = it },
                                    onLogout = { viewModel.logout() },
                                    onPostClick = onPostClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

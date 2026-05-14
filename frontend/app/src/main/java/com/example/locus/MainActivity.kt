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
import com.example.locus.ui.components.GuestLoginPrompt
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
import com.example.locus.viewmodel.RoutePlanningViewModel
import com.example.locus.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: UserViewModel by viewModels()
    private val planningVm: RoutePlanningViewModel by viewModels()

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
                var mapFocusGps by remember { mutableStateOf<String?>(null) }
                val isGuest = uiState.token == null
                val onLoginRequest: () -> Unit = { viewModel.logout() }
                var guestNavPromptAction by remember { mutableStateOf<String?>(null) }

                val onUserClick: (Int) -> Unit = { userId ->
                    if (userId != uiState.userId) viewingUserId = userId
                }
                val onPostClick: (Int) -> Unit = { postId -> viewingPostId = postId }
                val onNavigate: (NavDestination) -> Unit = { dest ->
                    when {
                        isGuest && dest == NavDestination.ADD -> guestNavPromptAction = "share a post"
                        isGuest && dest == NavDestination.PROFILE -> guestNavPromptAction = "view your profile"
                        else -> currentNav = dest
                    }
                }

                guestNavPromptAction?.let { action ->
                    GuestLoginPrompt(
                        action = action,
                        onDismiss = { guestNavPromptAction = null },
                        onLogin = { guestNavPromptAction = null; onLoginRequest() }
                    )
                }

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
                                isGuest = isGuest,
                                onBack = { viewingPostId = null },
                                onLoginRequest = onLoginRequest,
                                onLocationClick = { gps ->
                                    viewingPostId = null
                                    mapFocusGps = gps
                                    planningVm.setFocusedGps(gps)
                                    currentNav = NavDestination.COMPASS
                                }
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
                                    isGuest = isGuest,
                                    onNavigate = onNavigate,
                                    onLoginRequest = onLoginRequest,
                                    onUserClick = onUserClick,
                                    onLocationClick = { gps ->
                                        mapFocusGps = gps
                                        planningVm.setFocusedGps(gps)
                                        currentNav = NavDestination.COMPASS
                                    }
                                )
                                NavDestination.ADD -> AddPostScreen(
                                    onNavigate = onNavigate,
                                    token = uiState.token ?: ""
                                )
                                NavDestination.COMPASS -> MapScreen(
                                    onNavigate = onNavigate,
                                    onPostClick = onPostClick,
                                    focusGps = mapFocusGps.also { mapFocusGps = null }
                                )
                                NavDestination.EXPLORE -> ExploreScreen(
                                    token = uiState.token ?: "",
                                    currentUserId = uiState.userId,
                                    isGuest = isGuest,
                                    onNavigate = onNavigate,
                                    onLoginRequest = onLoginRequest,
                                    onUserClick = onUserClick,
                                    onPostClick = onPostClick
                                )
                                NavDestination.PROFILE -> ProfileScreen(
                                    token = uiState.token ?: "",
                                    currentUserId = uiState.userId,
                                    onNavigate = onNavigate,
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

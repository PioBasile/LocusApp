package com.example.locus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.data.model.Post
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Postcard
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    token: String = "",
    isGuest: Boolean = false,
    onNavigate: (NavDestination) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()

    // Load posts on first launch
    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            viewModel.loadPosts(token, groupId = 0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // -- Top bar -----------------------------------------------
        Topbar()

        // -- Feed --------------------------------------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 8.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isGuest -> {
                    GuestBanner()
                }
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NavyDark)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadPosts(token) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text("Retry", color = White)
                            }
                        }
                    }
                }
                uiState.posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts yet - be the first to share!",
                            color = MediumGray,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                else -> {
                    uiState.posts.forEach { post ->
                        Postcard(post = post)
                    }
                }
            }
        }

        // -- Bottom nav --------------------------------------------
        BottomNav(
            selected = NavDestination.HOME,
            onSelect = onNavigate
        )
    }
}

// -- Guest banner --------------------------------------------------------------
@Composable
private fun GuestBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NavyDark)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "You're browsing as guest",
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Log in to see posts, join groups and share your moments.",
                color = White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}


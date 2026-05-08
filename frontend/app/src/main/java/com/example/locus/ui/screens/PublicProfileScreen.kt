package com.example.locus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.data.remote.PostResponse
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.PublicProfileViewModel

@Composable
fun PublicProfileScreen(
    userId: Int,
    token: String = "",
    currentUserId: Int? = null,
    onBack: () -> Unit = {},
    viewModel: PublicProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    val isOwnProfile = currentUserId != null && currentUserId == userId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // -- Navy header -------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(NavyDark, NavyMedium)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // Back button
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = White
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(3.dp, GoldPrimary, CircleShape)
                        .background(White),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    } else if (!uiState.ppurl.isNullOrBlank() && uiState.ppurl != "img.jpg") {
                        AsyncImage(
                            model = uiState.ppurl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Default avatar",
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        )
                    }
                }

                if (!uiState.isLoading) {
                    Text(text = uiState.username, color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PubStatItem(label = "Posts", value = uiState.posts.size.toString())
                }

                // Follow button (hidden for own profile)
                if (!isOwnProfile && !uiState.isLoading && token.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.toggleFollow(token) },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isFollowing) White.copy(alpha = 0.15f) else GoldPrimary,
                            contentColor = White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (uiState.isFollowing) "Following" else "Follow",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = LightGray, thickness = 0.5.dp)

        // -- Posts grid --------------------------------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyDark)
                }
            } else if (uiState.posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No posts yet", color = MediumGray, fontSize = 14.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.posts.chunked(3).forEach { rowPosts ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowPosts.forEach { post ->
                                PubPhotoGridItem(post = post, modifier = Modifier.weight(1f))
                            }
                            repeat(3 - rowPosts.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PubStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, color = White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
private fun PubPhotoGridItem(post: PostResponse, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .border(2.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .background(LightGray)
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

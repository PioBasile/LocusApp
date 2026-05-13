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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.data.remote.UserAvisResponse
import com.example.locus.ui.theme.*
import com.example.locus.utils.formatTimeAgo
import com.example.locus.viewmodel.PublicProfileViewModel

@Composable
fun PublicProfileScreen(
    userId: Int,
    token: String = "",
    currentUserId: Int? = null,
    onBack: () -> Unit = {},
    onPostClick: (Int) -> Unit = {},
    viewModel: PublicProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId, token)
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
                .padding(top = statusBarTopPadding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
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
                    PubStatItem(label = "Reviews", value = uiState.userAvis.size.toString())
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

        // -- Reviews list ------------------------------------------
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, bottom = 16.dp)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NavyDark)
                }
            } else if (uiState.userAvis.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No reviews yet",
                        color = MediumGray,
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))
                uiState.userAvis.forEach { avis ->
                    AvisCard(avis = avis)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun AvisCard(avis: UserAvisResponse) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = avis.nomLieu,
                    color = NavyDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatTimeAgo(avis.createdAt),
                    color = MediumGray,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "★".repeat(avis.note) + "☆".repeat(5 - avis.note),
                color = GoldPrimary,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
            if (avis.commentaire.isNotBlank()) {
                Text(
                    text = avis.commentaire,
                    color = NavyDark.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
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

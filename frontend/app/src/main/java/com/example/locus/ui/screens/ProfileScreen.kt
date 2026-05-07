package com.example.locus.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.ProfileViewModel

data class ProfilePost(
    val id: Int,
    val imageUrl: String,
    val likeCount: Int
)

enum class ProfileTab { PHOTOS, PINS }

@Composable
fun ProfileScreen(
    onNavigate: (NavDestination) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.ProfileViewModelFactory.Factory),
    token: String = "",
    currentUserId: Int? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ProfileTab.PHOTOS) }
    var settingsExpanded by remember { mutableStateOf(false) }
    var selectedGroupFilter by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    // Groups that actually appear in the user's posts (for filter chips)
    val postGroupIds = remember(uiState.userPosts) { uiState.userPosts.flatMap { it.groupe }.toSet() }
    val filterGroups = remember(uiState.userGroupDetails, postGroupIds) {
        uiState.userGroupDetails.filter { it.id == 0 || it.id in postGroupIds }
    }

    val posts = remember(uiState.userPosts, selectedGroupFilter) {
        uiState.userPosts
            .filter { selectedGroupFilter == null || it.groupe.contains(selectedGroupFilter) }
            .map { p -> ProfilePost(id = p.id, imageUrl = p.imageUrl, likeCount = 0) }
    }

    // Reload every time this screen enters the composition
    LaunchedEffect(Unit) {
        if (token.isNotBlank()) viewModel.loadFullProfile(token)
    }

    // Toast feedback
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val imageFile = getFileFromUri(context, uri)
            if (imageFile != null) {
                viewModel.updateProfilePicture(token, imageFile)
            } else {
                Toast.makeText(context, "Erreur avec l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp)
        ) {

            // -- Navy header ---------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(colors = listOf(NavyDark, NavyMedium))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 24.dp)
            ) {
                // Wordmark
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_name),
                    contentDescription = "Locus",
                    modifier = Modifier
                        .width(80.dp)
                        .height(30.dp)
                        .align(Alignment.TopStart)
                )

                // Settings
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { settingsExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = settingsExpanded,
                        onDismissRequest = { settingsExpanded = false },
                        modifier = Modifier.background(White)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Log out",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Logout,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                settingsExpanded = false
                                onLogout()
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // -- Avatar ------------------------------------
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .border(3.dp, GoldPrimary, CircleShape)
                                .background(White)
                        ) {
                            val avatarUrl = uiState.profile?.ppurl
                            if (!avatarUrl.isNullOrBlank() && avatarUrl != "img.jpg") {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Default: app logo
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(id = R.drawable.ic_logo),
                                        contentDescription = "Default avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    )
                                }
                            }

                            // Loading overlay
                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = GoldPrimary,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }

                        // Camera button
                        IconButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary)
                                .border(2.dp, NavyDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Change photo",
                                tint = NavyDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // -- Name --------------------------------------
                    Text(
                        text = uiState.profile?.username ?: "...",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    // -- Stats row ---------------------------------
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(label = "Posts", value = uiState.userPosts.size.toString())
                        StatDivider()
                        StatItem(label = "Groups", value = uiState.groupCount.toString())
                        StatDivider()
                        StatItem(label = "Followers", value = uiState.followers.size.toString())
                    }
                }
            }

            // -- Tab selector --------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileTabChip(
                    label = "Photos",
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    selected = selectedTab == ProfileTab.PHOTOS,
                    onClick = { selectedTab = ProfileTab.PHOTOS },
                    modifier = Modifier.weight(1f)
                )
                ProfileTabChip(
                    label = "Pins",
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    selected = selectedTab == ProfileTab.PINS,
                    onClick = { selectedTab = ProfileTab.PINS },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = LightGray, thickness = 0.5.dp)

            // -- Group filter chips --------------------------------
            if (filterGroups.size > 1) {
                val chipScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(chipScrollState)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GroupFilterChip(
                        label = "All",
                        selected = selectedGroupFilter == null,
                        onClick = { selectedGroupFilter = null }
                    )
                    filterGroups.forEach { group ->
                        GroupFilterChip(
                            label = group.name,
                            selected = selectedGroupFilter == group.id,
                            onClick = { selectedGroupFilter = if (selectedGroupFilter == group.id) null else group.id }
                        )
                    }
                }
                HorizontalDivider(color = LightGray, thickness = 0.5.dp)
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // -- Photo grid ----------------------------------------
            if (posts.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = if (selectedGroupFilter != null) "No posts in this group" else "No posts yet",
                        color = MediumGray,
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    posts.chunked(3).forEach { rowPosts ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowPosts.forEach { post ->
                                PhotoGridItem(
                                    post = post,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowPosts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // -- Floating nav ------------------------------------------
        BottomNav(
            selected = NavDestination.PROFILE,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// -- Stat item -----------------------------------------------------------------
@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(White.copy(alpha = 0.2f))
    )
}

// -- Tab chip ------------------------------------------------------------------
@Composable
private fun ProfileTabChip(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) NavyDark else OffWhite,
            contentColor = if (selected) White else NavyDark
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

// -- Group filter chip ---------------------------------------------------------
@Composable
private fun GroupFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) NavyDark else OffWhite
    val textColor = if (selected) White else NavyDark
    val borderColor = if (selected) NavyDark else LightGray

    Box(
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(50.dp))
            .background(bg, RoundedCornerShape(50.dp))
            .clip(RoundedCornerShape(50.dp))
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// -- Photo grid item -----------------------------------------------------------
@Composable
private fun PhotoGridItem(post: ProfilePost, modifier: Modifier = Modifier) {
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
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 5.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = post.likeCount.toString(),
                color = White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
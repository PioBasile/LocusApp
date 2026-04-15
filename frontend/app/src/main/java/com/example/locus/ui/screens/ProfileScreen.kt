package com.example.locus.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val dummyPhotos = List(12) { i ->
    ProfilePost(
        id = i,
        imageUrl = "https://picsum.photos/seed/post$i/300/300",
        likeCount = 210
    )
}

private val dummyPins = List(9) { i ->
    ProfilePost(
        id = i + 100,
        imageUrl = "https://picsum.photos/seed/pin$i/300/300",
        likeCount = (50..300).random()
    )
}

enum class ProfileTab { PHOTOS, PINS }

@Composable
fun ProfileScreen(
    onNavigate: (NavDestination) -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.ProfileViewModelFactory.Factory),
    token: String = ""
) {
    var selectedTab by remember { mutableStateOf(ProfileTab.PHOTOS) }
    val scrollState = rememberScrollState()
    val posts = if (selectedTab == ProfileTab.PHOTOS) dummyPhotos else dummyPins

    // États pour le Follow/Unfollow
    var isFollowing by remember { mutableStateOf(false) }

    // États pour la modification de photo
    var isEditMode by remember { mutableStateOf(false) }
    var showChangeDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    Box(modifier = Modifier.fillMaxSize().background(White)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp) // dégage la place pour la BottomNav
        ) {

            // -- Navy header ---------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDark) // Remplacé par NavyDark solide comme sur le design
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 20.dp)
            ) {
                // Locus wordmark top left
                Icon(
                    painter = painterResource(id = R.drawable.ic_name),
                    tint = White,
                    contentDescription = "Locus",
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                        .align(Alignment.TopStart)
                )

                // Settings top right
                IconButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Centered content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar avec interaction (Crayon & Pop-up)
                    Surface(
                        onClick = {
                            if (isEditMode) showChangeDialog = true else isEditMode = true
                        },
                        shape = CircleShape,
                        color = GoldPrimary,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            AsyncImage(
                                model = "https://picsum.photos/seed/avatar1/200/200",
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )

                            // Overlay sombre + Crayon si on est en mode édition
                            if (isEditMode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Profile Picture",
                                        tint = White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            if (uiState.isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = GoldPrimary,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }

                    // Ligne : Follow (si non suivi) - Nom - Unfollow (si suivi)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isFollowing) {
                            ActionPill(text = "Follow", onClick = { isFollowing = true })
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Text(
                            text = uiState.profile?.username ?: "Erreur no name",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )

                        if (isFollowing) {
                            Spacer(modifier = Modifier.width(16.dp))
                            ActionPill(text = "Unfollow", onClick = { isFollowing = false })
                        }
                    }

                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(label = "Posts:", value = "20")
                        StatItem(label = "Groups:", value = "20")
                        StatItem(label = "Followers:", value = uiState.followers.size.toString())
                    }
                }
            }

            // -- Tab selector ---------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(GoldPrimary, RoundedCornerShape(50.dp))
                        .padding(4.dp), // Inner padding de la pilule dorée
                    horizontalArrangement = Arrangement.Center
                ) {
                    TabChip(
                        label = "Photos",
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_grid),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        selected = selectedTab == ProfileTab.PHOTOS,
                        onClick = { selectedTab = ProfileTab.PHOTOS }
                    )
                    TabChip(
                        label = "Pins",
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_location2),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        selected = selectedTab == ProfileTab.PINS,
                        onClick = { selectedTab = ProfileTab.PINS }
                    )
                }
            }

            // -- Photo grid ----------------------------------------
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                posts.chunked(3).forEach { rowPosts ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
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

        // -- Floating nav ------------------------------------------
        BottomNav(
            selected = NavDestination.PROFILE,
            onSelect = onNavigate,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // -- Pop-up pour changer la photo ------------------------------
    if (showChangeDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangeDialog = false
                isEditMode = false
            },
            title = {
                Text(text = "Changer la photo", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Voulez-vous modifier votre photo de profil ?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showChangeDialog = false
                        isEditMode = false
                        imagePicker.launch("image/*")
                    }
                ) {
                    Text("Ouvrir la galerie", color = NavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangeDialog = false
                        isEditMode = false
                    }
                ) {
                    Text("Annuler", color = Color.Gray)
                }
            },
            containerColor = White
        )
    }
}

// Bouton Follow/Unfollow
@Composable
private fun ActionPill(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = GoldPrimary,
        modifier = Modifier.wrapContentSize()
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = NavyDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = GoldPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// Correction du crash clickable
@Composable
private fun TabChip(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50.dp),
        color = if (selected) NavyDark else Color.Transparent,
        modifier = Modifier
            .width(110.dp)
            .height(38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) White else NavyDark
            ) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PhotoGridItem(post: ProfilePost, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(3.dp, GoldPrimary, RoundedCornerShape(12.dp))
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
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = post.likeCount.toString(),
                color = White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
package com.example.locus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.theme.*

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
fun ProfileScreen(onNavigate: (NavDestination) -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(ProfileTab.PHOTOS) }
    val scrollState = rememberScrollState()
    val posts = if (selectedTab == ProfileTab.PHOTOS) dummyPhotos else dummyPins

    Box(modifier = Modifier.fillMaxSize().background(OffWhite)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp) // clears floating nav
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Centered content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Avatar with gold ring
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary)
                            .padding(3.dp)
                    ) {
                        AsyncImage(
                            model = "https://picsum.photos/seed/avatar1/200/200",
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    // Name
                    Text(
                        text = "Jean Dupont",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(label = "Posts:", value = "20")
                        StatItem(label = "Groups:", value = "20")
                        StatItem(label = "Followers:", value = "200")
                    }
                }
            }

            // -- Tab selector --------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 40.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TabChip(
                    label = "Photos",
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_grid),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    selected = selectedTab == ProfileTab.PHOTOS,
                    onClick = { selectedTab = ProfileTab.PHOTOS },
                    modifier = Modifier.weight(1f)
                )
                TabChip(
                    label = "Pins",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_location2),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    selected = selectedTab == ProfileTab.PINS,
                    onClick = { selectedTab = ProfileTab.PINS },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = LightGray, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // -- Photo grid ----------------------------------------
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                posts.chunked(3).forEach { rowPosts ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
}

@Composable
private fun StatItem(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TabChip(
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

@Composable
private fun PhotoGridItem(post: ProfilePost, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
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
                .align(Alignment.BottomStart)
                .padding(5.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(NavyDark.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
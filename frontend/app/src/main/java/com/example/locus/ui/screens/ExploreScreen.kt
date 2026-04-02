package com.example.locus.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*

// -- Dummy data ----------------------------------------------------------------

data class Group(
    val id: Int,
    val name: String,
    val memberCount: String,
    val imageUrl: String
)

data class TrendingUser(
    val id: Int,
    val displayName: String,
    val username: String,
    val avatarUrl: String,
    val isFollowing: Boolean = false
)

private val dummyGroups = listOf(
    Group(1, "Hiking", "12k Members", "https://picsum.photos/seed/hiking/300/200"),
    Group(2, "Sea Lovers", "8.4k Members", "https://picsum.photos/seed/sea/300/200"),
    Group(3, "City Cruisers", "15k Members", "https://picsum.photos/seed/city/300/200"),
    Group(4, "Road Trips", "6.2k Members", "https://picsum.photos/seed/road/300/200"),
    Group(5, "Food & Travel", "9.1k Members", "https://picsum.photos/seed/food/300/200"),
    Group(6, "Photography", "11k Members", "https://picsum.photos/seed/photo/300/200"),
)

private val dummyUsers = listOf(
    TrendingUser(1, "Jean Dupont", "@jeandupont", "https://picsum.photos/seed/user1/100/100"),
    TrendingUser(2, "Marie Curie", "@mariecurie", "https://picsum.photos/seed/user2/100/100"),
    TrendingUser(3, "Alex Martin", "@alexmartin", "https://picsum.photos/seed/user3/100/100"),
    TrendingUser(4, "Sofia Loren", "@sofialoren", "https://picsum.photos/seed/user4/100/100"),
)

// -- Screen --------------------------------------------------------------------

@Composable
fun ExploreScreen(onNavigate: (NavDestination) -> Unit = {}) {
    var searchQuery by remember { mutableStateOf("") }
    val followStates = remember { mutableStateMapOf<Int, Boolean>() }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        Topbar()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(bottom = 8.dp)
        ) {

            // -- Search bar ----------------------------------------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search users, groups, or locations",
                        color = InputHint,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = White,
                    focusedContainerColor = White,
                    unfocusedBorderColor = InputBorder,
                    focusedBorderColor = NavyDark,
                    cursorColor = NavyDark,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // -- Create group banner -------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyDark)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Create a New Group",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Start your own community around a location and curate the best local secrets",
                        color = White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text(
                            text = "Launch Community",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -- Popular groups ------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular Groups",
                    color = NavyDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {}) {
                    Text(
                        text = "VIEW ALL",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Groups grid — 3 per row
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                dummyGroups.chunked(3).forEach { rowGroups ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowGroups.forEach { group ->
                            GroupCard(
                                group = group,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill empty slots if last row has < 3
                        repeat(3 - rowGroups.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- Trending users ------------------------------------
            Text(
                text = "Trending Users",
                color = NavyDark,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dummyUsers.forEach { user -> // matches Go: r.FormValue("groupe")
                    val isFollowing = followStates[user.id] ?: user.isFollowing
                    UserCard(
                        user = user,
                        isFollowing = isFollowing,
                        onFollowClick = { followStates[user.id] = !isFollowing }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        BottomNav(
            selected = NavDestination.EXPLORE,
            onSelect = onNavigate
        )
    }
}

// -- Group card ----------------------------------------------------------------
@Composable
private fun GroupCard(group: Group, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = group.imageUrl,
                contentDescription = group.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                NavyDark.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = group.name,
            color = NavyDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = group.memberCount,
            color = MediumGray,
            fontSize = 11.sp
        )
    }
}

// -- User card -----------------------------------------------------------------
@Composable
private fun UserCard(
    user: TrendingUser,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    color = NavyDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = user.username,
                    color = MediumGray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onFollowClick,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) OffWhite else GoldPrimary,
                    contentColor = if (isFollowing) NavyDark else White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
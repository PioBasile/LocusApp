package com.example.locus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.locus.data.model.Post
import com.example.locus.ui.theme.*
import com.example.locus.utils.formatTimeAgo
import androidx.compose.material.icons.filled.Favorite
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locus.viewmodel.HomeViewModel

@Composable
fun Postcard(
    post: Post,
    viewModel: HomeViewModel,
    onCommentClick: () -> Unit = {},
    onLikeClick: (Boolean) -> Unit = {},
) {
    key(post.id) {
        PostCardContent(
            post = post,
            viewModel = viewModel,
            onCommentClick = onCommentClick,
            onLikeClick = onLikeClick,

            )
    }
}

@Composable
private fun PostCardContent(post: Post, viewModel: HomeViewModel,onCommentClick: () -> Unit = {},onLikeClick: (Boolean) -> Unit = {}) {

    var authorName by remember { mutableStateOf(post.username ?: "User ${post.userId}") }
    var authorAvatarUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    val locationName = post.locationName ?: "Unknown"
    val imageCount = 1

    var isLiked by remember { mutableStateOf(false) }
    var likesCount by remember { mutableStateOf(0) }

    LaunchedEffect(post.userId) {
        val profile = viewModel.getPublicProfile(post.userId)
        if (profile != null) {
            authorName = profile.username
            authorAvatarUrl = profile.ppurl
        }
        isLoadingProfile = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {

            // -- Header --------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isLoadingProfile) Color.LightGray else GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!authorAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = authorAvatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (isLoadingProfile) "" else authorName.take(1).uppercase(),
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorName,
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = formatTimeAgo(post.date),
                        color = White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = GoldPrimary
                    )
                }
            }

            // -- Image -------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(12.dp))
                )

                if (imageCount > 1) {
                    // Badge "1/X"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "1/$imageCount",
                            color = White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Points de pagination
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(imageCount) { index ->
                            val isSelected = index == 0
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) GoldPrimary else White.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }

            // -- Actions row ---------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Actions de gauche (Like, Comment, Save)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PostActionItem(
                        icon = {
                            IconButton(
                                onClick = {
                                    isLiked = !isLiked // Inverse l'état
                                    likesCount = if (isLiked) likesCount + 1 else likesCount - 1
                                    onLikeClick(isLiked) // Prévient le ViewModel
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    // Change l'icône si c'est liké
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    // Change la couleur (Rouge si liké, Or sinon)
                                    tint = if (isLiked) Color(0xFFE53935) else GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        count = likesCount // Affiche la vraie variable
                    )


                    PostActionItem(
                        icon = {
                            // Wrap the Icon in an IconButton to make it clickable
                            IconButton(
                                onClick = onCommentClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ChatBubbleOutline,
                                    contentDescription = "Comment",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        count = 676
                    )
//                    PostActionItem(
//                        icon = { Icon(Icons.Filled.BookmarkBorder, contentDescription = "Bookmark", tint = GoldPrimary, modifier = Modifier.size(22.dp)) },
//                        count = 676
//                    )
                }

                // Location chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationName,
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // -- Caption -------------------------------------------------------------------
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = White)) {
                        append("$authorName : ")
                    }
                    withStyle(style = SpanStyle(color = White.copy(alpha = 0.9f))) {
                        append(post.description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 16.dp),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PostActionItem(icon: @Composable () -> Unit, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = count.toString(),
            color = White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
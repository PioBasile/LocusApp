package com.example.locus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.data.model.Post
import com.example.locus.ui.theme.*
import com.example.locus.utils.formatTimeAgo
import com.example.locus.viewmodel.HomeViewModel

@Composable
fun Postcard(
    post: Post,
    viewModel: HomeViewModel,
    currentUserId: Int? = null,
    token: String = "",
    onCommentClick: () -> Unit = {},
    onLikeClick: (Boolean) -> Unit = {},
    onDeleted: () -> Unit = {},
    onUserClick: (Int) -> Unit = {}
) {
    key(post.id) {
        PostCardContent(
            post = post,
            viewModel = viewModel,
            currentUserId = currentUserId,
            token = token,
            onCommentClick = onCommentClick,
            onLikeClick = onLikeClick,
            onDeleted = onDeleted,
            onUserClick = onUserClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostCardContent(
    post: Post,
    viewModel: HomeViewModel,
    currentUserId: Int? = null,
    token: String = "",
    onCommentClick: () -> Unit = {},
    onLikeClick: (Boolean) -> Unit = {},
    onDeleted: () -> Unit = {},
    onUserClick: (Int) -> Unit = {}
) {
    var authorName by remember { mutableStateOf(post.username ?: "User ${post.userId}") }
    var authorAvatarUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    val locationName = post.locationName
    val tags = post.tags?.filter { it.isNotBlank() }

    val likedPostIds by viewModel.likedPostIds.collectAsState()
    val likeCounts by viewModel.likeCounts.collectAsState()
    val commentCounts by viewModel.commentCounts.collectAsState()

    val isLiked = post.id in likedPostIds
    val likesCount = likeCounts[post.id] ?: 0
    val commentsCount = commentCounts[post.id] ?: 0

    val followingUserIds by viewModel.followingUserIds.collectAsState()
    val isFollowing = post.userId in followingUserIds
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isMyPost = currentUserId != null && post.userId == currentUserId

    LaunchedEffect(post.id) {
        val profile = viewModel.getPublicProfile(post.userId)
        if (profile != null) {
            authorName = profile.username
            authorAvatarUrl = profile.ppurl
        }
        isLoadingProfile = false

        if (token.isNotEmpty()) {
            viewModel.getCommentCountForPost(token, post.id)
            viewModel.loadLikesForPost(token, post.id)
        }
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason, comment ->
                viewModel.reportPost(token, post.id, reason, comment)
                showReportDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = White,
            title = { Text("Delete post?", color = NavyDark, fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone.", color = MediumGray, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(token, post.id)
                        showDeleteDialog = false
                        onDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(50.dp)
                ) { Text("Delete", color = White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = MediumGray) }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
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
                // Tappable avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(White)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { if (!isMyPost) onUserClick(post.userId) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingProfile) {
                        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else if (!authorAvatarUrl.isNullOrBlank() && authorAvatarUrl != "img.jpg") {
                        AsyncImage(
                            model = authorAvatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Default avatar",
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Tappable username
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { if (!isMyPost) onUserClick(post.userId) }
                ) {
                    Text(text = authorName, color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = formatTimeAgo(post.date), color = White.copy(alpha = 0.6f), fontSize = 12.sp)
                }

                if (!isMyPost) {
                    Button(
                        onClick = {
                            if (isFollowing) viewModel.unfollowUser(token, post.userId)
                            else viewModel.followUser(token, post.userId)
                        },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowing) White.copy(alpha = 0.15f) else GoldPrimary,
                            contentColor = White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(White)) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.Flag, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                                    Text("Report", color = Color(0xFFE53935), fontSize = 14.sp)
                                }
                            },
                            onClick = { showMenu = false; showReportDialog = true }
                        )
                        if (isMyPost) {
                            HorizontalDivider(color = LightGray, thickness = 0.5.dp)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
                                        Text("Delete post", color = NavyDark, fontSize = 14.sp)
                                    }
                                },
                                onClick = { showMenu = false; showDeleteDialog = true }
                            )
                        }
                    }
                }
            }

            // -- Image ---------------------------------------------
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(12.dp))
                )
            }

            // -- Audio player (if post has voice note) -------------
            if (!post.audioUrl.isNullOrBlank()) {
                AudioPlayerBar(
                    audioUrl = post.audioUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 10.dp)
                )
            }

            // -- Actions row ---------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PostActionItem(
                        icon = {
                            IconButton(
                                onClick = { if (token.isNotEmpty()) viewModel.toggleLike(token, post.id, !isLiked) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color(0xFFE53935) else GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        count = likesCount
                    )
                    PostActionItem(
                        icon = {
                            IconButton(onClick = onCommentClick, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Comment", tint = GoldPrimary, modifier = Modifier.size(22.dp))
                            }
                        },
                        count = commentsCount
                    )
                }

                if (locationName != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(color = White.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = locationName, color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // -- Caption -------------------------------------------
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = White)) { append("$authorName : ") }
                    withStyle(style = SpanStyle(color = White.copy(alpha = 0.9f))) { append(post.description) }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // -- AI Tags -------------------------------------------
            if (!tags.isNullOrEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 8.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.take(5).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "#$tag", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

// -- Report dialog -------------------------------------------------------------
@Composable
private fun ReportDialog(onDismiss: () -> Unit, onReport: (reason: String, comment: String) -> Unit) {
    val reasons = listOf("Spam", "Inappropriate content", "Harassment", "Misinformation", "Other")
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var comment by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text("Report post", color = NavyDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Why are you reporting this post?", color = MediumGray, fontSize = 13.sp)
                Box {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = NavyDark)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = InputBorder,
                            focusedBorderColor = NavyDark,
                            unfocusedTextColor = NavyDark,
                            focusedTextColor = NavyDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.matchParentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Transparent),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {}
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(White)) {
                        reasons.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason, color = NavyDark, fontSize = 14.sp) },
                                onClick = { selectedReason = reason; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Additional details (optional)", color = InputHint, fontSize = 13.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = OffWhite,
                        focusedContainerColor = OffWhite,
                        unfocusedBorderColor = InputBorder,
                        focusedBorderColor = NavyDark,
                        cursorColor = NavyDark,
                        unfocusedTextColor = NavyDark,
                        focusedTextColor = NavyDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onReport(selectedReason, comment) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), shape = RoundedCornerShape(50.dp)) {
                Text("Report", color = White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MediumGray) } }
    )
}

@Composable
private fun PostActionItem(icon: @Composable () -> Unit, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = count.toString(), color = White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

package com.example.locus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.ui.components.AudioPlayerBar
import com.example.locus.ui.theme.*
import com.example.locus.utils.formatTimeAgo
import com.example.locus.viewmodel.PostDetailViewModel

private data class ParsedPost(val caption: String, val location: String?, val manualTags: List<String>)

private fun parseDescription(raw: String): ParsedPost {
    var rest = raw.trim()

    val tagsMarker = "\n---tags:"
    val tagsIdx = rest.indexOf(tagsMarker)
    val manualTags = if (tagsIdx >= 0) {
        val tagsStr = rest.substring(tagsIdx + tagsMarker.length).trim()
        rest = rest.substring(0, tagsIdx)
        tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
    } else emptyList()

    val locMarker = "\n---loc:"
    val locIdx = rest.indexOf(locMarker)
    val location = if (locIdx >= 0) {
        val loc = rest.substring(locIdx + locMarker.length).trim()
        rest = rest.substring(0, locIdx)
        loc
    } else null

    return ParsedPost(rest.trim(), location, manualTags)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    token: String = "",
    onBack: () -> Unit = {},
    viewModel: PostDetailViewModel = viewModel()
) {
    LaunchedEffect(postId) { viewModel.load(postId, token) }

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val post = viewModel.post
    var showComments by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }

    val parsed = remember(post?.description) { parseDescription(post?.description ?: "") }
    val caption = parsed.caption
    val location = parsed.location

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        // weight(1f) is always on this Box — never inside a conditional
        Box(modifier = Modifier.weight(1f)) {
            when {
                viewModel.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(40.dp))
                }
                post == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Post not found", color = White.copy(alpha = 0.5f), fontSize = 15.sp)
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

                    // ── Header ─────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = statusBarTopPadding)
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Back button
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!viewModel.authorPpUrl.isNullOrBlank() && viewModel.authorPpUrl != "img.jpg") {
                                AsyncImage(
                                    model = viewModel.authorPpUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.ic_logo),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(8.dp)
                                )
                            }
                        }

                        // Username + time
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.authorName.ifEmpty { "User ${post.user_id}" },
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatTimeAgo(post.date),
                                color = White.copy(alpha = 0.55f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // ── Image ──────────────────────────────────────────
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "Post image",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    // ── Audio player ───────────────────────────────────
                    if (!post.audioUrl.isNullOrBlank()) {
                        AudioPlayerBar(
                            audioUrl = post.audioUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(top = 10.dp)
                        )
                    }

                    // ── Actions + location ─────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            // Like
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleLike(token, post.id) },
                                    enabled = !viewModel.isLikeInFlight,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (viewModel.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (viewModel.isLiked) Color(0xFFE53935) else GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = viewModel.likeCount.toString(),
                                    color = White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            // Comment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { showComments = !showComments },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ChatBubbleOutline,
                                        contentDescription = "Comments",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = viewModel.comments.size.toString(),
                                    color = White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Location pill
                        if (!location.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .background(White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = location,
                                    color = White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // ── Caption ────────────────────────────────────────
                    if (caption.isNotBlank()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = White)) {
                                    append("${viewModel.authorName.ifEmpty { "User ${post.user_id}" }} : ")
                                }
                                withStyle(SpanStyle(color = White.copy(alpha = 0.9f))) {
                                    append(caption)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }

                    // ── Tags — prefer AI tags, fall back to manual tags embedded in description ──
                    val tags = post.tags?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }
                        ?: parsed.manualTags
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = GoldPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // ── Comments ───────────────────────────────────────
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        color = White.copy(alpha = 0.1f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Comments (${viewModel.comments.size})",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = { showComments = !showComments },
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            Text(
                                text = if (showComments) "Hide" else "Show",
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (showComments) {
                        if (viewModel.comments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No comments yet. Be the first!",
                                    color = White.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        } else {
                            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                viewModel.comments.forEach { comment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(White.copy(alpha = 0.1f))
                                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "U",
                                                color = GoldPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "User ${comment.userId}",
                                                color = GoldPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = comment.commentaire,
                                                color = White.copy(alpha = 0.88f),
                                                fontSize = 13.sp,
                                                lineHeight = 19.sp
                                            )
                                        }
                                    }
                                    if (comment != viewModel.comments.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            color = White.copy(alpha = 0.06f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (token.isNotEmpty()) 72.dp else 16.dp))
                }
            }
        }

        // ── Comment input ─────────────────────────────────────────────
        // Kept outside the weight(1f) child and uses fixed padding (no navigationBarsPadding)
        if (token.isNotEmpty() && post != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyDark)
                    .drawBehind {
                        drawRect(color = White.copy(alpha = 0.08f), size = Size(size.width, 1.dp.toPx()))
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment…", color = White.copy(alpha = 0.35f), fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = White.copy(alpha = 0.08f),
                        focusedContainerColor = White.copy(alpha = 0.12f),
                        unfocusedBorderColor = White.copy(alpha = 0.15f),
                        focusedBorderColor = GoldPrimary,
                        cursorColor = GoldPrimary,
                        unfocusedTextColor = White,
                        focusedTextColor = White
                    ),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (commentText.isNotBlank()) GoldPrimary else White.copy(alpha = 0.1f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(token, post.id, commentText)
                                commentText = ""
                                showComments = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (commentText.isNotBlank()) NavyDark else White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


package com.example.locus.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Brush
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.data.remote.CommentResponse
import com.example.locus.ui.components.AudioPlayerBar
import com.example.locus.ui.components.AudioRecorderButton
import com.example.locus.ui.theme.*
import com.example.locus.utils.formatTimeAgo
import com.example.locus.viewmodel.PostDetailViewModel
import java.io.File

private data class ParsedPost(val caption: String, val location: String?, val manualTags: List<String>)

private fun parseDescription(raw: String): ParsedPost {
    var rest = raw.trim()
    val tagsMarker = "\n---tags:"
    val tagsIdx = rest.indexOf(tagsMarker)
    val manualTags = if (tagsIdx >= 0) {
        val t = rest.substring(tagsIdx + tagsMarker.length).trim()
        rest = rest.substring(0, tagsIdx)
        t.split(",").map { it.trim() }.filter { it.isNotBlank() }
    } else emptyList()
    val locMarker = "\n---loc:"
    val locIdx = rest.indexOf(locMarker)
    val location = if (locIdx >= 0) {
        val l = rest.substring(locIdx + locMarker.length).trim()
        rest = rest.substring(0, locIdx)
        l
    } else null
    return ParsedPost(rest.trim(), location, manualTags)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    token: String = "",
    onBack: () -> Unit = {},
    onLocationClick: ((String) -> Unit)? = null,
    viewModel: PostDetailViewModel = viewModel()
) {
    LaunchedEffect(postId) { viewModel.load(postId, token) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val post = viewModel.post
    var commentText by remember { mutableStateOf("") }
    var recordedAudio by remember { mutableStateOf<File?>(null) }
    val parsed = remember(post?.description) { parseDescription(post?.description ?: "") }

    Box(modifier = Modifier.fillMaxSize().background(White)) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    color = GoldPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.align(Alignment.Center).size(44.dp)
                )
            }
            post == null -> {
                Text(
                    "Post not found",
                    color = MediumGray,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                val caption  = parsed.caption
                val location = parsed.location?.takeIf { it.isNotBlank() && !it.equals("debug", ignoreCase = true) }
                val tags     = post.tags?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: parsed.manualTags

                Column(modifier = Modifier.fillMaxSize()) {

                    // ─── Scrollable body ──────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {

                        // ── Hero image with status bar padding ────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = statusBarTop)
                        ) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            )
                            // Gradient for back-button legibility
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                                        )
                                    )
                            )
                            // Back button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(top = 10.dp, start = 14.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.28f))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { onBack() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // ── Author row ────────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .padding(top = 18.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(LightGray),
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
                                    Image(
                                        painter = painterResource(R.drawable.ic_logo),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().padding(8.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewModel.authorName.ifEmpty { "User ${post.user_id}" },
                                    color = NavyDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatTimeAgo(post.date),
                                    color = MediumGray,
                                    fontSize = 12.sp
                                )
                            }

                            // Like + comment actions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { viewModel.toggleLike(token, post.id) },
                                        enabled = !viewModel.isLikeInFlight,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (viewModel.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "Like",
                                            tint = if (viewModel.isLiked) Color(0xFFE05C5C) else NavyDark,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Text(
                                        text = viewModel.likeCount.toString(),
                                        color = NavyDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(30.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = NavyDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = viewModel.comments.size.toString(),
                                        color = NavyDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // ── Caption ───────────────────────────────────────────
                        if (caption.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = NavyDark)) {
                                        append("${viewModel.authorName.ifEmpty { "User ${post.user_id}" }} ")
                                    }
                                    withStyle(SpanStyle(color = NavyDark.copy(alpha = 0.72f))) {
                                        append(caption)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        }

                        // ── Location ──────────────────────────────────────────
                        if (!location.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            val locGps = viewModel.locationGps
                            val tappable = locGps != null && onLocationClick != null
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 18.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(LightGray)
                                    .then(
                                        if (tappable) Modifier.clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { onLocationClick!!(locGps!!) }
                                        else Modifier
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = location,
                                    color = NavyDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // ── Tags ──────────────────────────────────────────────
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                GoldPrimary.copy(alpha = 0.10f),
                                                RoundedCornerShape(50.dp)
                                            )
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

                        // ── Audio ─────────────────────────────────────────────
                        if (!post.audioUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            AudioPlayerBar(
                                audioUrl = post.audioUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                            )
                        }

                        // ── Divider + comments ────────────────────────────────
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            color = LightGray
                        )
                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Comments",
                                color = NavyDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (viewModel.comments.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(NavyDark.copy(alpha = 0.08f), RoundedCornerShape(50.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = viewModel.comments.size.toString(),
                                        color = NavyDark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (viewModel.comments.isEmpty()) {
                            Text(
                                text = "No comments yet. Be the first!",
                                color = MediumGray,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                viewModel.comments.forEachIndexed { index, comment ->
                                    PostDetailCommentRow(comment = comment, viewModel = viewModel)
                                    if (index < viewModel.comments.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 14.dp),
                                            color = LightGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(if (token.isNotEmpty()) 90.dp else 28.dp))
                    }

                    // ─── Sticky comment input bar ─────────────────────────────
                    if (token.isNotEmpty()) {
                        HorizontalDivider(color = LightGray)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(White)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .padding(bottom = navBarBottom.coerceAtLeast(0.dp))
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AudioRecorderButton(
                                recordedFile = recordedAudio,
                                onRecordingDone = { recordedAudio = it },
                                onCleared = { recordedAudio = null },
                                tintColor = NavyDark
                            )

                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = {
                                    Text(
                                        "Add a comment…",
                                        color = MediumGray,
                                        fontSize = 13.sp
                                    )
                                },
                                maxLines = 3,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NavyDark,
                                    unfocusedBorderColor = LightGray,
                                    cursorColor = NavyDark,
                                    focusedTextColor = NavyDark,
                                    unfocusedTextColor = NavyDark,
                                    focusedContainerColor = White,
                                    unfocusedContainerColor = OffWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            val canSend = commentText.isNotBlank() || recordedAudio != null
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (canSend) NavyDark else LightGray)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        if (canSend) {
                                            viewModel.addComment(token, post.id, commentText, recordedAudio)
                                            commentText = ""
                                            recordedAudio = null
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (canSend) White else MediumGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailCommentRow(
    comment: CommentResponse,
    viewModel: PostDetailViewModel
) {
    var username by remember { mutableStateOf("User ${comment.userId}") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    LaunchedEffect(comment.userId) {
        val profile = viewModel.getPublicProfile(comment.userId)
        if (profile != null) {
            username = profile.username
            avatarUrl = profile.ppurl
        }
        isLoadingProfile = false
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingProfile) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else if (!avatarUrl.isNullOrBlank() && avatarUrl != "img.jpg") {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                color = NavyDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(3.dp))
            if (comment.commentaire.isNotBlank()) {
                Text(
                    text = comment.commentaire,
                    color = NavyDark.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            if (!comment.audioUrl.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                AudioPlayerBar(
                    audioUrl = comment.audioUrl,
                    tintColor = NavyDark,
                    bgColor = OffWhite,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

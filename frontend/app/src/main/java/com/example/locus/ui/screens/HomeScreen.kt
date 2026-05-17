package com.example.locus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locus.data.model.Post
import com.example.locus.data.remote.MyGroupResponse
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.CommentBottomSheet
import com.example.locus.ui.components.GuestLoginPrompt
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Postcard
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    token: String = "",
    currentUserId: Int? = null,
    isGuest: Boolean = false,
    onNavigate: (NavDestination) -> Unit = {},
    onLoginRequest: () -> Unit = {},
    onUserClick: (Int) -> Unit = {},
    onLocationClick: ((String) -> Unit)? = null,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()
    val myGroups by viewModel.userGroups.collectAsState()

    val currentPosts by viewModel.posts.collectAsState()
    val currentGroup = viewModel.selectedGroup

    var selectedPostIdForComments by remember { mutableStateOf<Int?>(null) }
    val currentComments by viewModel.currentComments.collectAsState()
    val isLoadingComments by viewModel.isLoadingComments.collectAsState()

    var showGuestPrompt by remember { mutableStateOf(false) }
    var guestPromptAction by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (token.isNotEmpty()) {
            viewModel.loadUserGroups(token)
            viewModel.loadUserLikes(token)
            viewModel.loadFollowing(token)
        }
        viewModel.loadPostsForGroup(token, currentGroup?.id ?: 0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        Topbar(
            showGroupSelector = true,
            selectedGroup = currentGroup,
            groups = myGroups,
            onGroupChange = { clickedGroup ->
                viewModel.selectGroup(clickedGroup)
                viewModel.loadPostsForGroup(token, clickedGroup.id)
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NavyDark)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadPostsForGroup(token, currentGroup?.id ?: 0) },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text("Retry", color = White)
                            }
                        }
                    }
                }
                currentPosts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No posts yet - be the first to share!",
                            color = MediumGray,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                else -> {
                    currentPosts.forEach { postResponse ->
                        val rawDesc = postResponse.description
                        val tagsDelimiter = "\n---tags:"
                        val locDelimiter = "\n---loc:"
                        val tagsIdx = rawDesc.indexOf(tagsDelimiter)
                        val locIdx = rawDesc.indexOf(locDelimiter)

                        val caption: String
                        val inlineTags: List<String>
                        val locationName: String?

                        when {
                            tagsIdx != -1 && locIdx != -1 -> {
                                if (tagsIdx < locIdx) {
                                    // tags before location
                                    caption = rawDesc.substring(0, tagsIdx)
                                    inlineTags = rawDesc.substring(tagsIdx + tagsDelimiter.length, locIdx)
                                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    locationName = rawDesc.substring(locIdx + locDelimiter.length).trim()
                                } else {
                                    // location before tags (current AddpostScreen format)
                                    caption = rawDesc.substring(0, locIdx)
                                    val locRaw = rawDesc.substring(locIdx + locDelimiter.length, tagsIdx)
                                    locationName = locRaw.substringBefore("\n---gps:").trim()
                                    inlineTags = rawDesc.substring(tagsIdx + tagsDelimiter.length)
                                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                                }
                            }
                            tagsIdx != -1 -> {
                                caption = rawDesc.substring(0, tagsIdx)
                                inlineTags = rawDesc.substring(tagsIdx + tagsDelimiter.length)
                                    .split(",").map { it.trim() }.filter { it.isNotBlank() }
                                locationName = null
                            }
                            locIdx != -1 -> {
                                caption = rawDesc.substring(0, locIdx)
                                inlineTags = emptyList()
                                locationName = rawDesc.substring(locIdx + locDelimiter.length)
                                    .substringBefore("\n---gps:").trim()
                            }
                            else -> {
                                caption = rawDesc
                                inlineTags = emptyList()
                                locationName = null
                            }
                        }

                        val serverTags = postResponse.tags?.filter { it.isNotBlank() } ?: emptyList()
                        val allTags = (inlineTags + serverTags).distinct().takeIf { it.isNotEmpty() }

                        val resolvedGps = postResponse.id_loc?.let { viewModel.locationGpsCache[it] }
                        val postForUI = Post(
                            id = postResponse.id,
                            userId = postResponse.user_id,
                            username = "User ${postResponse.user_id}",
                            groupe = postResponse.groupe.firstOrNull() ?: 0,
                            description = caption,
                            imageUrl = postResponse.imageUrl,
                            date = postResponse.date,
                            idLoc = postResponse.id_loc,
                            locationName = "$locationName >",
                            audioUrl = postResponse.audioUrl,
                            tags = allTags,
                            locGps = resolvedGps
                        )

                        Postcard(
                            post = postForUI,
                            viewModel = viewModel,
                            currentUserId = currentUserId,
                            token = token,
                            isGuest = isGuest,
                            onGuestAction = { action ->
                                guestPromptAction = action
                                showGuestPrompt = true
                            },
                            onCommentClick = {
                                viewModel.loadCommentsForPost(token, postForUI.id)
                                selectedPostIdForComments = postForUI.id
                            },
                            onUserClick = onUserClick,
                            onLocationClick = onLocationClick
                        )
                    }
                }
            }
        }

    }

    BottomNav(
        selected = NavDestination.HOME,
        onSelect = onNavigate,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    } // end outer Box

    selectedPostIdForComments?.let { postId ->
        CommentBottomSheet(
            comments = currentComments,
            isLoading = isLoadingComments,
            viewModel = viewModel,
            onDismiss = { selectedPostIdForComments = null },
            onUserClick = onUserClick,
            onSendComment = { text, audioFile ->
                viewModel.addComment(token, postId, text, audioFile)
            }
        )
    }

    if (showGuestPrompt) {
        GuestLoginPrompt(
            action = guestPromptAction,
            onDismiss = { showGuestPrompt = false },
            onLogin = { showGuestPrompt = false; onLoginRequest() }
        )
    }
}

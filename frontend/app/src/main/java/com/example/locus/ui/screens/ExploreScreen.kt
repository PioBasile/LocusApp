package com.example.locus.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.locus.data.model.Group
import com.example.locus.data.remote.FollowerResponse
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.ExploreViewModel

@Composable
fun ExploreScreen(
    token: String = "",
    currentUserId: Int? = null,
    onNavigate: (NavDestination) -> Unit = {},
    onUserClick: (Int) -> Unit = {},
    viewModel: ExploreViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val topUsers by viewModel.topUsers.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()
    val myGroupIds by viewModel.myGroupIds.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var joinTargetGroup by remember { mutableStateOf<Group?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var notificationIsError by remember { mutableStateOf(false) }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            viewModel.loadMyGroups(token)
            viewModel.loadFollowingIds(token)
        }
    }

    LaunchedEffect(uiState.joinSuccess) {
        uiState.joinSuccess?.let { notificationMessage = it; notificationIsError = false; viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.joinError) {
        uiState.joinError?.let { notificationMessage = it; notificationIsError = true; viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.createSuccess) {
        uiState.createSuccess?.let { notificationMessage = it; notificationIsError = false; viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.createError) {
        uiState.createError?.let { notificationMessage = it; notificationIsError = true; viewModel.clearMessages() }
    }
    LaunchedEffect(notificationMessage) {
        if (notificationMessage != null) { delay(2500); notificationMessage = null }
    }

    if (showCreateDialog) {
        CreateGroupSheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, isPrivate, password, imageFile ->
                viewModel.createGroup(token, name, description, isPrivate, password, imageFile)
                showCreateDialog = false
            }
        )
    }

    joinTargetGroup?.let { group ->
        JoinPrivateGroupSheet(
            groupName = group.name,
            onDismiss = { joinTargetGroup = null },
            onJoin = { password -> viewModel.joinGroup(token, group.id, password); joinTargetGroup = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
            Topbar()

            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(bottom = 8.dp)) {

                // -- Search bar ----------------------------------------
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(text = "Search users, groups, or locations", color = InputHint, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = White,
                        focusedContainerColor = White,
                        unfocusedBorderColor = InputBorder,
                        focusedBorderColor = NavyDark,
                        cursorColor = NavyDark
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
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
                        Text(text = "Create a New Group", color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Start your own community around a location and curate the best local secrets", color = White.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = White),
                            elevation = ButtonDefaults.buttonElevation(0.dp)
                        ) { Text(text = "Launch Community", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -- Popular groups ------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Popular Groups", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.loadGroups() }) {
                        Text(text = "REFRESH", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    uiState.isLoadingGroups -> Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NavyDark) }
                    uiState.groupsError != null -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text(text = uiState.groupsError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    uiState.groups.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text(text = "No groups yet — create the first one!", color = MediumGray, fontSize = 13.sp, fontStyle = FontStyle.Italic) }
                    else -> {
                        val filtered = uiState.groups
                            .filter { it.id != 0 }
                            .let { groups -> if (searchQuery.isBlank()) groups else groups.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) } }

                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            filtered.forEach { group ->
                                GroupCard(
                                    group = group,
                                    viewModel = viewModel,
                                    isJoined = group.id in myGroupIds,
                                    onJoin = { if (group.isPrivate) joinTargetGroup = group else viewModel.joinGroup(token, group.id) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // -- Top Creators --------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Top Creators", color = NavyDark, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.loadTopUsers() }) {
                        Text(text = "REFRESH", color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (topUsers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No users yet", color = MediumGray, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    }
                } else {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        topUsers.forEachIndexed { index, user ->
                            TopUserCard(
                                rank = index + 1,
                                user = user,
                                isFollowing = user.id in followedUserIds,
                                showFollowButton = user.id != currentUserId,
                                onFollowClick = {
                                    if (user.id in followedUserIds) viewModel.unfollowUser(token, user.id)
                                    else viewModel.followUser(token, user.id)
                                },
                                onUserClick = { onUserClick(user.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            BottomNav(selected = NavDestination.EXPLORE, onSelect = onNavigate)
        }

        AnimatedVisibility(
            visible = notificationMessage != null,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut() + scaleOut(targetScale = 0.88f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(NavyDark.copy(alpha = 0.93f))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = notificationMessage ?: "",
                    color = White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -- Top user card -------------------------------------------------------------
@Composable
private fun TopUserCard(
    rank: Int,
    user: FollowerResponse,
    isFollowing: Boolean,
    showFollowButton: Boolean,
    onFollowClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onUserClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when (rank) {
                            1 -> GoldPrimary
                            2 -> Color(0xFFB0BEC5)
                            3 -> Color(0xFFCD7F32)
                            else -> OffWhite
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    color = if (rank <= 3) White else NavyDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (rank == 1) GoldPrimary else LightGray, CircleShape)
                    .background(NavyDark),
                contentAlignment = Alignment.Center
            ) {
                if (!user.ppurl.isNullOrBlank() && user.ppurl != "img.jpg") {
                    AsyncImage(model = user.ppurl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        text = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = user.username,
                color = NavyDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )

            if (showFollowButton) {
                Spacer(modifier = Modifier.width(8.dp))
                if (isFollowing) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, LightGray, RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Following", color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onFollowClick,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Follow", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -- Group card ----------------------------------------------------------------
@Composable
private fun GroupCard(group: Group, viewModel: ExploreViewModel, isJoined: Boolean, onJoin: () -> Unit) {
    var memberCount by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(group.id) {
        memberCount = viewModel.getGroupMemberCount(group.id)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // -- Image header ------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (!group.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = group.imageUrl,
                        contentDescription = group.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(NavyDark, NavyMedium))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            color = GoldPrimary.copy(alpha = 0.4f),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Gradient scrim so name is always readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, NavyDark.copy(alpha = 0.75f)),
                                startY = 30f
                            )
                        )
                )

                // Private badge — top right
                if (group.isPrivate) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(10.dp))
                        Text(text = "Private", color = White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Group name — bottom left
                Text(
                    text = group.name,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // -- Info row ----------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (group.description.isNotBlank()) {
                        Text(
                            text = group.description,
                            color = NavyDark.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    memberCount?.let { count ->
                        Text(
                            text = "$count ${if (count == 1) "member" else "members"}",
                            color = MediumGray,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (isJoined) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, LightGray, RoundedCornerShape(50.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(text = "Joined", color = MediumGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onJoin,
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "Join", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -- Create group sheet --------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPrivate: Boolean, password: String, imageFile: java.io.File) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> imageUri = uri }

    val canCreate = name.isNotBlank() && imageUri != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("New Group", color = NavyDark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Build a community around a place you love", color = MediumGray, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(4.dp))

            // Cover photo picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(OffWhite)
                    .border(1.dp, if (imageUri == null) LightGray else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tap to change", color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = MediumGray, modifier = Modifier.size(32.dp))
                        Text("Add cover photo", color = MediumGray, fontSize = 13.sp)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyDark,
                    unfocusedBorderColor = LightGray,
                    cursorColor = NavyDark,
                    unfocusedContainerColor = OffWhite,
                    focusedContainerColor = White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyDark,
                    unfocusedBorderColor = LightGray,
                    cursorColor = NavyDark,
                    unfocusedContainerColor = OffWhite,
                    focusedContainerColor = White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Private toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OffWhite)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isPrivate) NavyDark else MediumGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Private group", color = NavyDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Members join by password only", color = MediumGray, fontSize = 12.sp)
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = NavyDark)
                )
            }

            if (isPrivate) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Group password") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDark,
                        unfocusedBorderColor = LightGray,
                        cursorColor = NavyDark,
                        unfocusedContainerColor = OffWhite,
                        focusedContainerColor = White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val imageFile = imageUri?.let { uri -> getFileFromUri(context, uri) }
                    if (imageFile != null) onCreate(name, description, isPrivate, password, imageFile)
                },
                enabled = canCreate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Create Group", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MediumGray, fontSize = 14.sp)
            }
        }
    }
}

// -- Join private group sheet --------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinPrivateGroupSheet(groupName: String, onDismiss: () -> Unit, onJoin: (password: String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = White,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 32.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(NavyDark.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = NavyDark, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PRIVATE GROUP",
                color = MediumGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = groupName,
                color = NavyDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the password to join this community",
                color = MediumGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("Password", color = InputHint, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = MediumGray
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NavyDark,
                    unfocusedBorderColor = LightGray,
                    cursorColor = NavyDark,
                    unfocusedContainerColor = OffWhite,
                    focusedContainerColor = White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { if (password.isNotBlank()) onJoin(password) },
                enabled = password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Join Group", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MediumGray, fontSize = 14.sp)
            }
        }
    }
}

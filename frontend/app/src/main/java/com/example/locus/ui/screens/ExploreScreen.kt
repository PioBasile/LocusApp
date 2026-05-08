package com.example.locus.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val topUsers by viewModel.topUsers.collectAsState()
    val followedUserIds by viewModel.followedUserIds.collectAsState()
    val myGroupIds by viewModel.myGroupIds.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var joinTargetGroup by remember { mutableStateOf<Group?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(token) {
        if (token.isNotEmpty()) {
            viewModel.loadMyGroups(token)
            viewModel.loadFollowingIds(token)
        }
    }

    LaunchedEffect(uiState.joinSuccess) {
        uiState.joinSuccess?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.joinError) {
        uiState.joinError?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.createSuccess) {
        uiState.createSuccess?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.createError) {
        uiState.createError?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            token = token,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, isPrivate, password, imageFile ->
                if (imageFile != null) {
                    viewModel.createGroup(token, name, description, isPrivate, password, imageFile)
                    showCreateDialog = false
                } else {
                    Toast.makeText(context, "An image is required", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    joinTargetGroup?.let { group ->
        JoinPrivateGroupDialog(
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = NavyDark,
                contentColor = White,
                shape = RoundedCornerShape(12.dp)
            )
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

// -- Create group dialog -------------------------------------------------------
@Composable
private fun CreateGroupDialog(
    token: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPrivate: Boolean, password: String, imageFile: java.io.File?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri -> imageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text(text = "Create a Group", color = NavyDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)).background(OffWhite),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = "Group Image", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Button(onClick = { imagePicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = NavyDark.copy(alpha = 0.7f))) { Text("Change Image", color = White) }
                    } else {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }, shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = NavyDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Group Cover", color = NavyDark)
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group name") }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyDark, cursorColor = NavyDark), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 2, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyDark, cursorColor = NavyDark), modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it }, colors = CheckboxDefaults.colors(checkedColor = NavyDark))
                    Text(text = "Private group", color = NavyDark, fontSize = 14.sp)
                }
                if (isPrivate) {
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyDark, cursorColor = NavyDark), modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val imageFile = imageUri?.let { uri -> getFileFromUri(context, uri) }
                        onCreate(name, description, isPrivate, password, imageFile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(50.dp)
            ) { Text("Create", color = White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MediumGray) } }
    )
}

// -- Join private group dialog -------------------------------------------------
@Composable
private fun JoinPrivateGroupDialog(groupName: String, onDismiss: () -> Unit, onJoin: (password: String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = { Text(text = "Join \"$groupName\"", color = NavyDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "This is a private group. Enter the password to join.", color = MediumGray, fontSize = 13.sp)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null, tint = MediumGray)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyDark, cursorColor = NavyDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (password.isNotBlank()) onJoin(password) }, enabled = password.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = NavyDark), shape = RoundedCornerShape(50.dp)) { Text("Join", color = White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MediumGray) } }
    )
}

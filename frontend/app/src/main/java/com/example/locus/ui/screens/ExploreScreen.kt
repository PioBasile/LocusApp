package com.example.locus.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.locus.data.model.Group
import com.example.locus.data.remote.GroupResponse
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.ExploreViewModel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

// -- Dummy trending users (no backend endpoint yet) ----------------------------
private data class TrendingUser(
    val id: Int,
    val displayName: String,
    val username: String,
    val avatarUrl: String
)

private val dummyUsers = listOf(
    TrendingUser(1, "Jean Dupont", "@jeandupont", "https://picsum.photos/seed/user1/100/100"),
    TrendingUser(2, "Marie Curie", "@mariecurie", "https://picsum.photos/seed/user2/100/100"),
    TrendingUser(3, "Alex Martin", "@alexmartin", "https://picsum.photos/seed/user3/100/100"),
    TrendingUser(4, "Sofia Loren", "@sofialoren", "https://picsum.photos/seed/user4/100/100"),
)

// -- Screen --------------------------------------------------------------------

@Composable
fun ExploreScreen(
    token: String = "",
    onNavigate: (NavDestination) -> Unit = {},
    viewModel: ExploreViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var searchQuery by remember { mutableStateOf("") }
    val followStates = remember { mutableStateMapOf<Int, Boolean>() }
    val scrollState = rememberScrollState()
    var joinTargetGroup by remember { mutableStateOf<Group?>(null) }


    // Join/create feedback
    LaunchedEffect(uiState.joinSuccess) {
        uiState.joinSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.joinError) {
        uiState.joinError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.createSuccess) {
        uiState.createSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    // Create group dialog state
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateGroupDialog(
            token = token,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description, isPrivate, password ->
                viewModel.createGroup(token, name, description, isPrivate, password)
                showCreateDialog = false
            }
        )
    }

    joinTargetGroup?.let { group ->
        JoinPrivateGroupDialog(
            groupName = group.name,
            onDismiss = { joinTargetGroup = null },
            onJoin = { password ->
                viewModel.joinGroup(token, group.id, password)
                joinTargetGroup = null
            }
        )
    }

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
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
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
                TextButton(onClick = { viewModel.loadGroups() }) {
                    Text(
                        text = "REFRESH",
                        color = GoldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoadingGroups -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NavyDark)
                    }
                }
                uiState.groupsError != null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.groupsError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
                uiState.groups.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No groups yet — create the first one!",
                            color = MediumGray,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                else -> {
                    // Filter by search query
                    val filtered = if (searchQuery.isBlank()) uiState.groups
                    else uiState.groups.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                                it.description.contains(searchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filtered.chunked(3).forEach { rowGroups ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowGroups.forEach { group ->
                                    GroupCard(
                                        group = group,
                                        onJoin = {
                                            if (group.isPrivate) {
                                                joinTargetGroup = group  // ← triggers the dialog
                                            } else {
                                                viewModel.joinGroup(token, group.id)
                                            }

                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowGroups.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- Trending users (dummy for now) --------------------
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
                dummyUsers.forEach { user ->
                    val isFollowing = followStates[user.id] ?: false
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
private fun GroupCard(
    group: Group,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(NavyMedium)
        ) {
            // No image URL from backend yet — show navy placeholder with initial
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name.first().uppercaseChar().toString(),
                    color = GoldPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Lock icon for private groups
            if (group.isPrivate) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Private",
                    tint = White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(14.dp)
                )
            }

            // Join overlay button at bottom
            Button(
                onClick = onJoin,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp),
                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary.copy(alpha = 0.85f),
                    contentColor = White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "Join", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
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
            text = if (group.isPrivate) "Private" else "Public",
            color = if (group.isPrivate) GoldPrimary else MediumGray,
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

// -- Create group dialog -------------------------------------------------------
@Composable
private fun CreateGroupDialog(
    token: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, isPrivate: Boolean, password: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(text = "Create a Group", color = NavyDark, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDark,
                        cursorColor = NavyDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDark,
                        cursorColor = NavyDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it },
                        colors = CheckboxDefaults.colors(checkedColor = NavyDark)
                    )
                    Text(text = "Private group", color = NavyDark, fontSize = 14.sp)
                }
                if (isPrivate) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyDark,
                            cursorColor = NavyDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, isPrivate, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Create", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediumGray)
            }
        }
    )
}

// ── Join private group dialog ─────────────────────────────────────────────────
@Composable
private fun JoinPrivateGroupDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onJoin: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                text = "Join \"$groupName\"",
                color = NavyDark,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This is a private group. Enter the password to join.",
                    color = MediumGray,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MediumGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyDark,
                        cursorColor = NavyDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (password.isNotBlank()) onJoin(password) },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Join", color = White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediumGray)
            }
        }
    )
}
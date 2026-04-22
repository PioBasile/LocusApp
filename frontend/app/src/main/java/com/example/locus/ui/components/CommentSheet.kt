package com.example.locus.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.data.remote.CommentResponse
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.HomeViewModel

@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    comments: List<CommentResponse>,
    isLoading: Boolean,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var textValue by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = White,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.82f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // -- Custom drag handle + header -----------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(NavyDark, NavyMedium)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(White.copy(alpha = 0.3f))
                        .align(Alignment.TopCenter)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Comments",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (comments.isNotEmpty()) {
                        Text(
                            text = "${comments.size} comment${if (comments.size > 1) "s" else ""}",
                            color = White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // -- Comments list -------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(OffWhite)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = NavyDark,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    comments.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No comments yet",
                                color = NavyDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Start the conversation below",
                                color = MediumGray,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                    else -> {
                        LazyColumn(state = listState, contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 8.dp
                        ), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(comments) { comment ->
                                CommentRow(
                                    comment = comment,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            // -- Input bar — always visible at bottom --------------
            Surface(
                color = White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User avatar
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(NavyDark, NavyMedium)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Me",
                            color = White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        placeholder = {
                            Text(
                                text = "Write a comment…",
                                color = InputHint,
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = OffWhite,
                            focusedContainerColor = OffWhite,
                            unfocusedBorderColor = InputBorder,
                            focusedBorderColor = NavyDark,
                            cursorColor = NavyDark,
                            focusedTextColor = NavyDark,
                            unfocusedTextColor = NavyDark
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (textValue.isNotBlank()) NavyDark
                                else LightGray
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (textValue.isNotBlank()) {
                                    onSendComment(textValue)
                                    textValue = ""
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (textValue.isNotBlank()) GoldPrimary else MediumGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -- Single comment row --------------------------------------------------------
@Composable
fun CommentRow(comment: CommentResponse, viewModel: HomeViewModel) {

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
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(White), // Fond toujours blanc pour que le logo ressorte bien
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingProfile) {
                CircularProgressIndicator(
                    color = GoldPrimary,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else if (!avatarUrl.isNullOrBlank() && avatarUrl != "img.jpg") {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Default avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp) // Règle la taille du logo ici
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Comment bubble
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 16.dp
                    )
                )
                .background(White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = username,
                color = NavyDark,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = comment.commentaire,
                color = NavyDark.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
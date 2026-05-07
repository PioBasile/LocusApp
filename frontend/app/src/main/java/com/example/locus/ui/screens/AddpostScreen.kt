package com.example.locus.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.ui.components.BottomNav
import com.example.locus.ui.components.NavDestination
import com.example.locus.ui.components.Topbar
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.AddPostViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun AddPostScreen(
    onNavigate: (NavDestination) -> Unit = {},
    viewModel: AddPostViewModel = viewModel(),
    token: String = ""
) {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val scrollState = rememberScrollState()

    val myGroups by viewModel.userGroups.collectAsState()
    var selectedGroupIds by remember { mutableStateOf(setOf<Int>()) }
    var showGroupDialog by remember { mutableStateOf(false) }

    var isPublic by remember { mutableStateOf(true) } // Activé par défaut si tu veux

    val selectableGroups = myGroups

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    LaunchedEffect(Unit) {
        if (token.isNotEmpty() ) {
            viewModel.loadUserGroups(token)
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            Toast.makeText(context, "Post partagé avec succès !", Toast.LENGTH_SHORT).show()
            onNavigate(NavDestination.HOME)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, "Erreur : $it", Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        Topbar()

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {

            // -- Photo hero ----------
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDark,
                    contentColor = White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, NavyDark.copy(alpha = 0.6f)),
                                        startY = 150f
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(NavyDark.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Change",
                                color = White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = "Add photo",
                                tint = White.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Tap to select a photo",
                                color = White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- Form fields ---------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JournalField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = "Caption",
                    placeholder = "What's the story?",
                    maxLines = 3
                )

                JournalField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Location",
                    placeholder = "Where was this?",
                    leadingIcon = {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "VISIBILITY",
                        color = NavyDark.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Public Post", color = NavyDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Show in the global Public Posts feed", color = InputHint, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = GoldPrimary,
                                uncheckedThumbColor = InputHint,
                                uncheckedTrackColor = Color.LightGray
                            )
                        )
                    }
                }

                // -- Group dropdown --------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SPECIFIC GROUPS",
                        color = NavyDark.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    Box {
                        val groupDisplayText = if (selectedGroupIds.isEmpty()) {
                            "Select groups (Optional)"
                        } else {
                            selectableGroups.filter { it.id in selectedGroupIds }
                                .joinToString(", ") { it.name }
                        }

                        OutlinedTextField(
                            value = groupDisplayText,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { showGroupDialog = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Expand", tint = NavyDark)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = White,
                                focusedContainerColor = White,
                                unfocusedBorderColor = InputBorder,
                                focusedBorderColor = NavyDark,
                                unfocusedTextColor = if (selectedGroupIds.isEmpty()) InputHint else NavyDark,
                                focusedTextColor = NavyDark,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { showGroupDialog = true },
                            modifier = Modifier.matchParentSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Transparent),
                            elevation = ButtonDefaults.buttonElevation(0.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {}
                    }
                }

                // -- Dialog Multi-Sélection ------------------------
                if (showGroupDialog) {
                    AlertDialog(
                        onDismissRequest = { showGroupDialog = false },
                        title = { Text("Select Groups", color = NavyDark, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                selectableGroups.forEach { group ->
                                    val isChecked = selectedGroupIds.contains(group.id)
                                    Surface(
                                        onClick = {
                                            selectedGroupIds = if (isChecked) {
                                                selectedGroupIds - group.id
                                            } else {
                                                selectedGroupIds + group.id
                                            }
                                        },
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = GoldPrimary,
                                                    checkmarkColor = White,
                                                    uncheckedColor = InputBorder
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = group.name, color = NavyDark, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showGroupDialog = false }) {
                                Text("Done", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        },
                        containerColor = White,
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        imageUri?.let { uri ->
                            val imageFile = getFileFromUri(context, uri)

                            val groupsListToPost = selectedGroupIds.toMutableList()
                            if (isPublic && !groupsListToPost.contains(0)) {
                                groupsListToPost.add(0)
                            }

                            val fullDescription = if (location.isBlank()) caption
                                                  else "$caption\n---loc:$location"

                            if (imageFile != null) {
                                viewModel.uploadPost(token, imageFile, fullDescription, groupsListToPost, locationId = 1)
                            } else {
                                Toast.makeText(context, "Erreur avec l'image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    // Le bouton est cliquable même si pas de groupe sélectionné (si c'est public par exemple)
                    enabled = imageUri != null && caption.isNotBlank() && !viewModel.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDark,
                        contentColor = White,
                        disabledContainerColor = NavyDark.copy(alpha = 0.3f),
                        disabledContentColor = White.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Share Moment",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        BottomNav(
            selected = NavDestination.ADD,
            onSelect = onNavigate
        )
    }
}

@Composable
private fun JournalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            color = NavyDark.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = InputHint,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                )
            },
            maxLines = maxLines,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = leadingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = White,
                focusedContainerColor = White,
                unfocusedBorderColor = InputBorder,
                focusedBorderColor = NavyDark,
                cursorColor = NavyDark,
                unfocusedTextColor = NavyDark,
                focusedTextColor = NavyDark,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun getFileFromUri(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
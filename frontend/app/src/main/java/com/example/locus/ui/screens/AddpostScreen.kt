package com.example.locus.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
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
import com.example.locus.ui.components.AudioPlayerBar
import com.example.locus.ui.components.AudioRecorderButton
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
    var recordedAudio by remember { mutableStateOf<File?>(null) }
    val scrollState = rememberScrollState()

    val myGroups by viewModel.userGroups.collectAsState()
    var selectedGroupIds by remember { mutableStateOf(setOf<Int>()) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(true) }
    var aiTagsEnabled by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var manualTags by remember { mutableStateOf<List<String>>(emptyList()) }

    fun addTag() {
        val clean = tagInput.trim().lowercase().replace(" ", "_")
        if (clean.isNotEmpty() && clean !in manualTags) manualTags = manualTags + clean
        tagInput = ""
    }

    val selectableGroups = myGroups

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    LaunchedEffect(Unit) {
        if (token.isNotEmpty()) viewModel.loadUserGroups(token)
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            Toast.makeText(context, "Post shared!", Toast.LENGTH_SHORT).show()
            onNavigate(NavDestination.HOME)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(OffWhite)) {
        Topbar()

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).padding(bottom = 100.dp)) {

            // -- Photo hero ----------------------------------------
            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(300.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark, contentColor = White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (imageUri != null) {
                        AsyncImage(model = imageUri, contentDescription = "Selected photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, NavyDark.copy(alpha = 0.6f)), startY = 150f)))
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(NavyDark.copy(alpha = 0.7f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Change", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(imageVector = Icons.Filled.AddPhotoAlternate, contentDescription = "Add photo", tint = White.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                            Text(text = "Tap to select a photo", color = White.copy(alpha = 0.5f), fontSize = 14.sp, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // -- Caption + mic ----------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "CAPTION", color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            placeholder = { Text(text = "What's the story?", color = InputHint, fontSize = 14.sp, fontStyle = FontStyle.Italic) },
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = White,
                                focusedContainerColor = White,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                cursorColor = NavyDark,
                                unfocusedTextColor = NavyDark,
                                focusedTextColor = NavyDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        AudioRecorderButton(
                            recordedFile = recordedAudio,
                            onRecordingDone = { recordedAudio = it },
                            onCleared = { recordedAudio = null },
                            tintColor = NavyDark
                        )
                    }
                    if (recordedAudio != null) {
                        AudioPlayerBar(audioUrl = recordedAudio!!.absolutePath, tintColor = NavyDark, bgColor = NavyDark.copy(alpha = 0.08f))
                    }
                }

                // -- Location field ---------------------------------------
                JournalField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Location",
                    placeholder = "Where was this?",
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) }
                )



                // -- Manual tags ------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "TAGS", color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)

                    // -- AI Tags toggle ---------------------------------------
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
                            Text(text = "AI Tags", color = NavyDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(text = "Automatically tag this post with AI", color = InputHint, fontSize = 12.sp)
                        }
                        Switch(
                            checked = aiTagsEnabled,
                            onCheckedChange = { aiTagsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = White,
                                checkedTrackColor = GoldPrimary,
                                uncheckedThumbColor = InputHint,
                                uncheckedTrackColor = Color.LightGray
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .border(1.dp, InputBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = { Text("Add a tag…", color = InputHint, fontSize = 14.sp, fontStyle = FontStyle.Italic) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTag() }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = White,
                                focusedContainerColor = White,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                cursorColor = NavyDark,
                                unfocusedTextColor = NavyDark,
                                focusedTextColor = NavyDark
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (tagInput.isNotBlank()) NavyDark else LightGray)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { addTag() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add tag", tint = if (tagInput.isNotBlank()) White else MediumGray, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (manualTags.isNotEmpty()) {
                        val tagScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tagScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            manualTags.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(NavyDark.copy(alpha = 0.08f))
                                        .border(1.dp, NavyDark.copy(alpha = 0.18f), RoundedCornerShape(50.dp))
                                        .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = "#$tag", color = NavyDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(NavyDark.copy(alpha = 0.12f))
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { manualTags = manualTags - tag },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove", tint = NavyDark, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // -- Visibility toggle ------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Visibility", color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
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

                // -- Group picker -----------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "SPECIFIC GROUPS", color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Box {
                        val groupDisplayText = if (selectedGroupIds.isEmpty()) "Select groups (Optional)"
                        else selectableGroups.filter { it.id in selectedGroupIds }.joinToString(", ") { it.name }

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
                                focusedTextColor = NavyDark
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

                if (showGroupDialog) {
                    AlertDialog(
                        onDismissRequest = { showGroupDialog = false },
                        title = { Text("Select Groups", color = NavyDark, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                selectableGroups.forEach { group ->
                                    val isChecked = selectedGroupIds.contains(group.id)
                                    Surface(
                                        onClick = { selectedGroupIds = if (isChecked) selectedGroupIds - group.id else selectedGroupIds + group.id },
                                        color = Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = isChecked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = GoldPrimary, checkmarkColor = White, uncheckedColor = InputBorder))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(text = group.name, color = NavyDark, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showGroupDialog = false }) { Text("Done", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) } },
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
                            if (isPublic && !groupsListToPost.contains(0)) groupsListToPost.add(0)

                            val locPart = if (location.isNotBlank()) "\n---loc:$location" else ""
                            val tagPart = if (manualTags.isNotEmpty()) "\n---tags:${manualTags.joinToString(",")}" else ""
                            val fullDescription = "$caption$locPart$tagPart"

                            if (imageFile != null) {
                                viewModel.uploadPost(token, imageFile, fullDescription, groupsListToPost, locationId = 1, audioFile = recordedAudio, aiTags = aiTagsEnabled, tags = manualTags)
                            } else {
                                Toast.makeText(context, "Image error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = imageUri != null && !viewModel.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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
                        Text(text = "Share Moment", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

    }

    BottomNav(selected = NavDestination.ADD, onSelect = onNavigate, modifier = Modifier.align(Alignment.BottomCenter))
    } // end outer Box
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
        Text(text = label.uppercase(), color = NavyDark.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = InputHint, fontSize = 14.sp, fontStyle = FontStyle.Italic) },
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
                focusedTextColor = NavyDark
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

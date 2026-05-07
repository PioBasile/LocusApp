package com.example.locus.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.locus.R
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.SignupViewModel

// Predefined gradient backgrounds for default avatars
private val avatarGradients = listOf(
    listOf(NavyDark, NavyMedium),
    listOf(GoldPrimary, GoldLight),
    listOf(NavyMedium, GoldPrimary),
)

@Composable
fun SignupScreen(
    onSignupSuccess: (String) -> Unit = {},
    onBackToLogin: () -> Unit = {},
    viewModel: SignupViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var pendingToken by remember { mutableStateOf<String?>(null) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.sendFCMToken(pendingToken!!)
        onSignupSuccess(pendingToken!!)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.token != null) {
            pendingToken = uiState.token
            showWelcomeDialog = true
        }
    }

    if (showWelcomeDialog && pendingToken != null) {
        WelcomeDialog(
            onContinue = {
                showWelcomeDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val alreadyGranted = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (alreadyGranted) {
                        onSignupSuccess(pendingToken!!)
                    } else {
                        notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    onSignupSuccess(pendingToken!!)
                }
            }
        )
    }

    val localError = when {
        password.isNotBlank() && confirmPassword.isNotBlank()
                && password != confirmPassword -> "Passwords do not match"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.height(32.dp))

            // -- Logo ----------------------------------------------
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Locus Logo",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_name),
                contentDescription = "Locus",
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(36.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Create your account",
                color = NavyDark.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(28.dp))

            // -- Fields --------------------------------------------
            LocusInputField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(10.dp))

            LocusInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(10.dp))

            LocusInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LocusInputField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirm password",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = confirmVisible,
                onPasswordVisibilityToggle = { confirmVisible = !confirmVisible }
            )

            // -- Errors --------------------------------------------
            if (localError != null || uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = localError ?: uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -- Sign up button ------------------------------------
            Button(
                onClick = {
                    if (localError == null) {
                        viewModel.signup(username, email, password)
                    }
                },
                enabled = username.isNotBlank() && email.isNotBlank()
                        && password.isNotBlank() && confirmPassword.isNotBlank()
                        && localError == null && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDark,
                    contentColor = White,
                    disabledContainerColor = NavyDark.copy(alpha = 0.3f),
                    disabledContentColor = White.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "Create Account",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Already have an account? Log in",
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = NavyDark
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeDialog(onContinue: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = tween(durationMillis = 400),
        label = "iconScale"
    )

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .background(White)
                .padding(horizontal = 28.dp, vertical = 36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Gold ring + checkmark
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.10f))
                        .border(2.dp, GoldPrimary.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to Locus!",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = NavyDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your account is all set.\nReady to explore?",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = MediumGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Thin gold divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(GoldPrimary.copy(alpha = 0f), GoldPrimary, GoldPrimary.copy(alpha = 0f))
                            )
                        )
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDark,
                        contentColor = White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "Let's Explore",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
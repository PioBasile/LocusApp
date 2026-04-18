package com.example.locus.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.token != null) {
            onSignupSuccess(uiState.token)
        }
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
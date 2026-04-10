package com.example.locus.ui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locus.R
import com.example.locus.ui.theme.*
import com.example.locus.viewmodel.SignupViewModel

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit = {},
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

    // Navigate on success
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSignupSuccess()
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

            // -- Logo ----------------------------------------------
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Locus Logo",
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_name),
                contentDescription = "Locus",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your account",
                color = NavyDark.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(32.dp))

            // -- Username ------------------------------------------
            LocusInputField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                keyboardType = KeyboardType.Text
            )

            Spacer(modifier = Modifier.height(12.dp))

            // -- Email ---------------------------------------------
            LocusInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            // -- Password ------------------------------------------
            LocusInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // -- Confirm password ----------------------------------
            LocusInputField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirm password",
                keyboardType = KeyboardType.Password,
                isPassword = true,
                passwordVisible = confirmVisible,
                onPasswordVisibilityToggle = { confirmVisible = !confirmVisible }
            )

            // -- Validation errors ---------------------------------
            val localError = when {
                password.isNotBlank() && confirmPassword.isNotBlank()
                        && password != confirmPassword -> "Passwords do not match"
                else -> null
            }

            if (localError != null || uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = localError ?: uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // -- Back to login -------------------------------------
            TextButton(onClick = onBackToLogin) {
                Text(
                    text = "Already have an account? Log in",
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = NavyDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
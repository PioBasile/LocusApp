package com.example.locus.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.locus.ui.theme.*

@Composable
fun GuestLoginPrompt(
    action: String,
    onDismiss: () -> Unit,
    onLogin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                text = "Sign in to continue",
                color = NavyDark,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "Create an account or sign in to $action.",
                color = MediumGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onLogin,
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                shape = RoundedCornerShape(50.dp)
            ) {
                Text("Sign in", color = White, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe later", color = MediumGray)
            }
        }
    )
}

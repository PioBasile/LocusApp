package com.example.locus


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.locus.ui.screens.LoginScreen
import com.example.locus.ui.theme.LocusTheme
import com.example.locus.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocusTheme {
                LoginScreen(
                    onLoginClick = { email, password ->
                        viewModel.login(email, password)
                    },
                    onGuestClick = {
                        viewModel.continueAsGuest()
                    }
                )
            }
        }
    }
}
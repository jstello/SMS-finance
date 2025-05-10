package com.example.finanzaspersonales.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.compose.rememberNavController
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanzasPersonalesTheme {
                val navController = rememberNavController() // Though SettingsScreen itself might not navigate, providing it for consistency or future use.
                SettingsScreen(navController = navController)
            }
        }
    }
} 
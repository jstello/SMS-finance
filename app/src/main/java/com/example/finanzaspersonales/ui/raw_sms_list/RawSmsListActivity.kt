package com.example.finanzaspersonales.ui.raw_sms_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RawSmsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanzasPersonalesTheme {
                val navController = rememberNavController()
                RawSmsListScreen(navController = navController)
            }
        }
    }
} 
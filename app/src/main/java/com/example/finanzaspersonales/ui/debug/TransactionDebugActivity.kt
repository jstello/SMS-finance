package com.example.finanzaspersonales.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TransactionDebugScreen(
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}

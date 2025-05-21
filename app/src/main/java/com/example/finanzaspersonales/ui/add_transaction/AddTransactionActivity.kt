package com.example.finanzaspersonales.ui.add_transaction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import dagger.hilt.android.AndroidEntryPoint
// TODO: Import necessary dependencies for ViewModelFactory or use Hilt

@AndroidEntryPoint
class AddTransactionActivity : ComponentActivity() {

    // Inject ViewModel using Hilt
    private val viewModel: AddTransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove manual dependency and ViewModel instantiation
        // ... removed manual instantiation block ...

        setContent {
            FinanzasPersonalesTheme {
                Surface {
                    AddTransactionScreen(
                        viewModel = viewModel, // Pass Hilt injected ViewModel
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
} 
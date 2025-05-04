package com.example.finanzaspersonales.ui.transaction_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl // Import AuthRepositoryImpl
import com.example.finanzaspersonales.data.repository.ProviderStat // Import ProviderStat
import com.example.finanzaspersonales.ui.transaction_list.TransactionListScreen // <-- ADDED IMPORT
import com.example.finanzaspersonales.data.repository.CategoryRepository
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TransactionListActivity : ComponentActivity() {

    // Inject ViewModel using Hilt
    private val viewModel: TransactionListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove manual repository and ViewModel instantiation
        // ... removed manual instantiation block ...

        setContent {
            FinanzasPersonalesTheme {
                TransactionListScreen(viewModel = viewModel) // Pass Hilt injected ViewModel
            }
        }
    }
}

// Remove TransactionListViewModelFactory
// class TransactionListViewModelFactory( ... ) : ViewModelProvider.Factory { ... }

// Preview function remains the same if needed for TransactionListScreen itself
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FinanzasPersonalesTheme {
        // You might need a mock ViewModel for preview
        // TransactionListScreen(viewModel = MockViewModel())
    }
} 
package com.example.finanzaspersonales.ui.providers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
// import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl // Removed
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.ui.transaction_list.TransactionListActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProvidersActivity : ComponentActivity() {

    // Inject ViewModel using Hilt
    private val viewModel: ProvidersViewModel by viewModels()

    // Define a constant for the Intent extra key
    companion object {
        const val EXTRA_PROVIDER_FILTER = "com.example.finanzaspersonales.PROVIDER_FILTER"
        const val EXTRA_SELECTED_DATE = "selected_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Remove manual repository and ViewModel instantiation
        // ... removed manual instantiation block ...

        setContent {
            FinanzasPersonalesTheme {
                // Get the selected date from intent or use current month
                // This logic might move to the ViewModel initialization with SavedStateHandle
                val selectedDateMillis = intent.getLongExtra(EXTRA_SELECTED_DATE, System.currentTimeMillis())
                
                ProvidersScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onProviderClick = { providerName ->
                        val intent = Intent(this, TransactionListActivity::class.java)
                        intent.putExtra(EXTRA_PROVIDER_FILTER, providerName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
} 
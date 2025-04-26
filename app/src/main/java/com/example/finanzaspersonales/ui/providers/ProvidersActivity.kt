package com.example.finanzaspersonales.ui.providers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.ui.transaction_list.TransactionListActivity

class ProvidersActivity : ComponentActivity() {

    private lateinit var viewModel: ProvidersViewModel

    // Define a constant for the Intent extra key
    companion object {
        const val EXTRA_PROVIDER_FILTER = "com.example.finanzaspersonales.PROVIDER_FILTER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Instantiate Dependencies (Needs proper DI later) ---
        // This setup mirrors TransactionListActivity for now.
        // Consider a proper Dependency Injection solution (like Hilt) later.
        val authRepository = AuthRepositoryImpl()
        val sharedPrefsManager = SharedPrefsManager(this)
        val smsDataSource = SmsDataSource(this)
        val extractTransactionDataUseCase = ExtractTransactionDataUseCase(this)
        
        // Dummy Transaction Repo for Category Repo init (avoids cyclic dependency)
         val dummyTransactionRepository = object : com.example.finanzaspersonales.data.repository.TransactionRepository { /* ... stubs ... */ 
            override suspend fun getAllSmsMessages(): List<com.example.finanzaspersonales.data.model.SmsMessage> = emptyList()
            override suspend fun getTransactions(): List<com.example.finanzaspersonales.data.model.TransactionData> = emptyList()
            override suspend fun filterTransactions(transactions: List<com.example.finanzaspersonales.data.model.TransactionData>, year: Int?, month: Int?, isIncome: Boolean?): List<com.example.finanzaspersonales.data.model.TransactionData> = emptyList()
            override suspend fun getTransactionById(id: String): com.example.finanzaspersonales.data.model.TransactionData? = null
            override suspend fun getTransactionsByCategory(categoryId: String): List<com.example.finanzaspersonales.data.model.TransactionData> = emptyList()
            override suspend fun assignCategoryToTransaction(transactionId: String, categoryId: String): Boolean = false
            override suspend fun refreshSmsData(limitToRecentMonths: Int) { /* Dummy */ }
            override suspend fun initializeTransactions() { /* Dummy */ }
            override suspend fun saveTransactionToFirestore(transaction: com.example.finanzaspersonales.data.model.TransactionData): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun getTransactionsFromFirestore(userId: String): Result<List<com.example.finanzaspersonales.data.model.TransactionData>> = Result.failure(NotImplementedError())
            override suspend fun updateTransactionInFirestore(transaction: com.example.finanzaspersonales.data.model.TransactionData): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun deleteTransactionFromFirestore(transactionId: String, userId: String): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun performInitialTransactionSync(userId: String, syncStartDate: Long): Result<Unit> = Result.failure(NotImplementedError())
            override suspend fun getProviderStats(from: Long, to: Long): List<com.example.finanzaspersonales.data.repository.ProviderStat> = emptyList() // Dummy implementation
         }

        val categoryRepository = CategoryRepositoryImpl(
            context = this,
            sharedPrefsManager = sharedPrefsManager,
            transactionRepository = dummyTransactionRepository, 
            authRepository = authRepository
        )
        val categoryAssignmentUseCase = CategoryAssignmentUseCase(categoryRepository)

        // Real Transaction Repository
        val transactionRepository = TransactionRepositoryImpl(
            context = this,
            smsDataSource = smsDataSource,
            extractTransactionDataUseCase = extractTransactionDataUseCase,
            categoryAssignmentUseCase = categoryAssignmentUseCase,
            sharedPrefsManager = sharedPrefsManager,
            authRepository = authRepository
        )
        // -----------------------------------------------------------

        // Create ViewModel using the Factory
        val viewModelFactory = ProvidersViewModelFactory(transactionRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[ProvidersViewModel::class.java]

        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProvidersScreen(
                        viewModel = viewModel,
                        onBackClick = { finish() },
                        onProviderClick = { providerName ->
                            // Log the provider name being clicked
                            Log.d("PROVIDERS_ACTIVITY", "Provider clicked: '$providerName'")
                            val intent = Intent(this, TransactionListActivity::class.java).apply {
                                putExtra(EXTRA_PROVIDER_FILTER, providerName)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
} 
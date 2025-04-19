package com.example.finanzaspersonales.ui.transaction_list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl // Import AuthRepositoryImpl
import com.example.finanzaspersonales.ui.transaction_list.TransactionListScreen // <-- ADDED IMPORT

class TransactionListActivity : ComponentActivity() {

    private lateinit var viewModel: TransactionListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Instantiate Dependencies (Needs proper DI later) ---
        val authRepository = AuthRepositoryImpl() // Added instantiation
        val sharedPrefsManager = SharedPrefsManager(this)
        val smsDataSource = SmsDataSource(this)
        val extractTransactionDataUseCase = ExtractTransactionDataUseCase(this)
        
        // Dummy Transaction Repo needed for Category Repo init (cyclic dependency issue without DI)
         val dummyTransactionRepository = object : com.example.finanzaspersonales.data.repository.TransactionRepository {
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
        }

        val categoryRepository = CategoryRepositoryImpl(
            context = this,
            sharedPrefsManager = sharedPrefsManager,
            transactionRepository = dummyTransactionRepository, // Use dummy for now
            authRepository = authRepository // Provide the AuthRepository instance
        )
        val categoryAssignmentUseCase = CategoryAssignmentUseCase(categoryRepository)

        // Real Transaction Repository
        val transactionRepository = TransactionRepositoryImpl(
            context = this,
            smsDataSource = smsDataSource,
            extractTransactionDataUseCase = extractTransactionDataUseCase,
            categoryAssignmentUseCase = categoryAssignmentUseCase,
            sharedPrefsManager = sharedPrefsManager,
            authRepository = authRepository // Add authRepository parameter
        )
        // -----------------------------------------------------------

        // Create ViewModel using the Factory
        val viewModelFactory = TransactionListViewModelFactory(transactionRepository)
        viewModel = ViewModelProvider(this, viewModelFactory)[TransactionListViewModel::class.java]

        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TransactionListScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// Preview function remains the same if needed for TransactionListScreen itself
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FinanzasPersonalesTheme {
        // You might need a mock ViewModel for preview
        // TransactionListScreen(viewModel = MockViewModel())
    }
} 
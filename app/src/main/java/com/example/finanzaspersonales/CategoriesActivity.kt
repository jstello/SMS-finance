package com.example.finanzaspersonales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.ui.categories.CategoriesScreen
import com.example.finanzaspersonales.ui.categories.CategoriesViewModel
import com.example.finanzaspersonales.ui.categories.CategoryDetailScreen
import com.example.finanzaspersonales.ui.categories.CategoryEditScreen
import com.example.finanzaspersonales.ui.categories.TransactionDetailScreen
import com.example.finanzaspersonales.ui.theme.FinanzasPersonalesTheme

/**
 * Activity for the Categories feature
 */
class CategoriesActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanzasPersonalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CategoriesApp(onBack = { finish() })
                }
            }
        }
    }
}

/**
 * Navigation state for Categories feature
 */
sealed class CategoriesNavState {
    object Categories : CategoriesNavState()
    data class CategoryDetail(val category: Category) : CategoriesNavState()
    data class TransactionDetail(val transaction: TransactionData) : CategoriesNavState()
    data class CategoryEdit(val category: Category? = null) : CategoriesNavState()
}

/**
 * Main composable for the Categories feature
 */
@Composable
fun CategoriesApp(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // Navigation state
    var navState by remember { mutableStateOf<CategoriesNavState>(CategoriesNavState.Categories) }
    
    val categoriesViewModel: CategoriesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Create repositories and dependencies
                val sharedPrefsManager = SharedPrefsManager(context)
                val smsDataSource = SmsDataSource(context)
                val extractTransactionDataUseCase = ExtractTransactionDataUseCase(context)
                
                // Create the CategoryAssignmentUseCase with a temporary CategoryRepository
                // (We'll replace this with the actual repository later)
                val dummyTransactionRepository = object : TransactionRepository {
                    override suspend fun getAllSmsMessages(): List<SmsMessage> {
                        return emptyList()
                    }
                    
                    override suspend fun getTransactions(): List<TransactionData> {
                        return emptyList()
                    }
                    
                    override suspend fun filterTransactions(
                        transactions: List<TransactionData>,
                        year: Int?,
                        month: Int?,
                        isIncome: Boolean?
                    ): List<TransactionData> {
                        return emptyList()
                    }
                    
                    override suspend fun getTransactionById(id: String): TransactionData? {
                        return null
                    }
                    
                    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> {
                        return emptyList()
                    }
                    
                    override suspend fun assignCategoryToTransaction(transactionId: String, categoryId: String): Boolean {
                        return false
                    }
                    
                    override suspend fun refreshSmsData() {
                        // Do nothing
                    }
                }
                
                val tempCategoryRepository = CategoryRepositoryImpl(
                    context = context,
                    sharedPrefsManager = sharedPrefsManager,
                    transactionRepository = dummyTransactionRepository
                )
                
                val categoryAssignmentUseCase = CategoryAssignmentUseCase(tempCategoryRepository)
                
                // Now we can create the actual repositories with the proper dependencies
                val transactionRepository: TransactionRepository = TransactionRepositoryImpl(
                    context = context,
                    smsDataSource = smsDataSource,
                    extractTransactionDataUseCase = extractTransactionDataUseCase,
                    categoryAssignmentUseCase = categoryAssignmentUseCase
                )
                
                // Replace the temporary repository with the real one
                val categoryRepository: CategoryRepository = CategoryRepositoryImpl(
                    context = context,
                    sharedPrefsManager = sharedPrefsManager,
                    transactionRepository = transactionRepository
                )
                
                return CategoriesViewModel(
                    categoryRepository = categoryRepository,
                    transactionRepository = transactionRepository
                ) as T
            }
        }
    )
    
    // Handle navigation state
    when (val currentState = navState) {
        is CategoriesNavState.Categories -> {
            CategoriesScreen(
                viewModel = categoriesViewModel,
                onBack = onBack,
                onCategoryClick = { category ->
                    navState = CategoriesNavState.CategoryDetail(category)
                },
                onAddCategory = {
                    navState = CategoriesNavState.CategoryEdit()
                }
            )
        }
        
        is CategoriesNavState.CategoryDetail -> {
            CategoryDetailScreen(
                viewModel = categoriesViewModel,
                category = currentState.category,
                onBack = { navState = CategoriesNavState.Categories },
                onTransactionClick = { transaction ->
                    navState = CategoriesNavState.TransactionDetail(transaction)
                }
            )
        }
        
        is CategoriesNavState.TransactionDetail -> {
            TransactionDetailScreen(
                viewModel = categoriesViewModel,
                transaction = currentState.transaction,
                onBack = { 
                    // Go back to the appropriate screen based on where we came from
                    val previousState = navState
                    if (previousState is CategoriesNavState.CategoryDetail) {
                        navState = previousState
                    } else {
                        navState = CategoriesNavState.Categories
                    }
                },
                onCategorySelected = { transaction, category ->
                    categoriesViewModel.assignCategoryToTransaction(transaction, category)
                    // Go back after assigning
                    val previousState = navState
                    if (previousState is CategoriesNavState.CategoryDetail) {
                        navState = previousState
                    } else {
                        navState = CategoriesNavState.Categories
                    }
                }
            )
        }
        
        is CategoriesNavState.CategoryEdit -> {
            CategoryEditScreen(
                viewModel = categoriesViewModel,
                category = currentState.category,
                onBack = { navState = CategoriesNavState.Categories },
                onSave = { category ->
                    categoriesViewModel.saveCategory(category)
                    navState = CategoriesNavState.Categories
                }
            )
        }
    }
} 
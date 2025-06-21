package com.example.finanzaspersonales.ui.transaction_list

import androidx.lifecycle.SavedStateHandle
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@FlowPreview
class TransactionListViewModelTest {

    private lateinit var viewModel: TransactionListViewModel
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        transactionRepository = mock()
        categoryRepository = mock()
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateSortOrder updates sort order correctly`() = runTest {
        // Given
        whenever(transactionRepository.getTransactions(any(), any(), any(), any()))
            .thenReturn(createMockTransactions())
        whenever(categoryRepository.getCategories()).thenReturn(emptyList())
        
        viewModel = TransactionListViewModel(
            transactionRepository,
            categoryRepository,
            savedStateHandle
        )
        
        // When
        viewModel.updateSortOrder(SortOrder.AMOUNT_DESC)
        
        // Then
        assertEquals(SortOrder.AMOUNT_DESC, viewModel.sortOrder.value)
    }

    @Test
    fun `assignCategoryToTransaction success updates state correctly`() = runTest {
        // Given
        whenever(transactionRepository.getTransactions(any(), any(), any(), any()))
            .thenReturn(createMockTransactions())
        whenever(categoryRepository.getCategories()).thenReturn(createMockCategories())
        whenever(transactionRepository.assignCategoryToTransaction(eq("1"), eq("cat1")))
            .thenReturn(true)
        
        viewModel = TransactionListViewModel(
            transactionRepository,
            categoryRepository,
            savedStateHandle
        )
        
        // When
        viewModel.assignCategoryToTransaction("1", "cat1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(transactionRepository).assignCategoryToTransaction("1", "cat1")
        assertFalse(viewModel.isAssigningCategory.value)
        assertTrue(viewModel.assignmentResult.value?.isSuccess == true)
    }

    @Test
    fun `assignCategoryToTransaction failure updates state correctly`() = runTest {
        // Given
        whenever(transactionRepository.getTransactions(any(), any(), any(), any()))
            .thenReturn(createMockTransactions())
        whenever(categoryRepository.getCategories()).thenReturn(createMockCategories())
        whenever(transactionRepository.assignCategoryToTransaction(eq("1"), eq("cat1")))
            .thenReturn(false)
        
        viewModel = TransactionListViewModel(
            transactionRepository,
            categoryRepository,
            savedStateHandle
        )
        
        // When
        viewModel.assignCategoryToTransaction("1", "cat1")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(transactionRepository).assignCategoryToTransaction("1", "cat1")
        assertFalse(viewModel.isAssigningCategory.value)
        assertTrue(viewModel.assignmentResult.value?.isFailure == true)
    }

    @Test
    fun `clearAssignmentResult clears the result state`() = runTest {
        // Given
        whenever(transactionRepository.getTransactions(any(), any(), any(), any()))
            .thenReturn(createMockTransactions())
        whenever(categoryRepository.getCategories()).thenReturn(emptyList())
        
        viewModel = TransactionListViewModel(
            transactionRepository,
            categoryRepository,
            savedStateHandle
        )
        
        // When
        viewModel.clearAssignmentResult()
        
        // Then
        assertNull(viewModel.assignmentResult.value)
    }

    @Test
    fun `categories are loaded on initialization`() = runTest {
        // Given
        val mockCategories = createMockCategories()
        whenever(transactionRepository.getTransactions(any(), any(), any(), any()))
            .thenReturn(createMockTransactions())
        whenever(categoryRepository.getCategories()).thenReturn(mockCategories)
        
        // When
        viewModel = TransactionListViewModel(
            transactionRepository,
            categoryRepository,
            savedStateHandle
        )
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        verify(categoryRepository).getCategories()
        assertEquals(mockCategories, viewModel.categories.value)
    }

    private fun createMockTransactions(): List<TransactionData> {
        return listOf(
            TransactionData(id = "1", date = Date(1000), amount = 100.0f, isIncome = false, provider = "A", categoryId = null),
            TransactionData(id = "2", date = Date(2000), amount = 200.0f, isIncome = false, provider = "B", categoryId = null),
            TransactionData(id = "3", date = Date(3000), amount = 50.0f, isIncome = false, provider = "C", categoryId = null)
        )
    }

    private fun createMockCategories(): List<Category> {
        return listOf(
            Category(id = "cat1", name = "Food", color = 0xFF0000),
            Category(id = "cat2", name = "Transport", color = 0x00FF00)
        )
    }
}

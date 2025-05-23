package com.example.finanzaspersonales.data.repository

import com.example.finanzaspersonales.data.db.mapper.toDomain
import com.example.finanzaspersonales.data.db.mapper.toEntity
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.local.room.dao.TransactionDao
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.ProviderStat
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toMonth
import com.example.finanzaspersonales.domain.util.DateTimeUtils.toYear
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val smsDataSource: SmsDataSource,
    private val extractTransactionDataUseCase: ExtractTransactionDataUseCase,
    private val categoryAssignmentUseCase: CategoryAssignmentUseCase
) : TransactionRepository {

    override suspend fun getAllSmsMessages(): List<SmsMessage> =
        smsDataSource.readSmsMessages()

    override suspend fun refreshSmsData(limitToRecentMonths: Int) {
        // Extract and insert unique transactions from SMS messages
        val smsList = smsDataSource.readSmsMessages(limitToRecentMonths)
        val transactions = extractTransactionDataUseCase.execute(smsList)
            .distinctBy { it.id }
        
        transactions.forEach { tx ->
            val assignedCategory = categoryAssignmentUseCase.assignCategoryToTransaction(tx)
            val entity = tx.copy(categoryId = assignedCategory?.id).toEntity()
            transactionDao.insertTransaction(entity)
        }
    }

    override suspend fun getTransactions(forceRefresh: Boolean): List<TransactionData> {
        if (forceRefresh) {
            refreshSmsData(0)
        }
        return transactionDao.getAllTransactions().first().map { it.toDomain() }
    }

    override suspend fun filterTransactions(
        transactions: List<TransactionData>,
        year: Int?,
        month: Int?,
        isIncome: Boolean?
    ): List<TransactionData> = transactions.filter { tx ->
        (year == null || tx.date.toYear() == year) &&
        (month == null || tx.date.toMonth() == month) &&
        (isIncome == null || tx.isIncome == isIncome)
    }

    override suspend fun getTransactionById(id: String): TransactionData? =
        transactionDao.getTransactionById(id)?.toDomain()

    override suspend fun getTransactionsByCategory(categoryId: String): List<TransactionData> =
        transactionDao.getTransactionsByCategory(categoryId).first().map { it.toDomain() }

    override suspend fun assignCategoryToTransaction(transactionId: String, categoryId: String): Boolean {
        val entity = transactionDao.getTransactionById(transactionId) ?: return false
        transactionDao.updateTransaction(entity.copy(categoryId = categoryId))
        return true
    }

    override suspend fun initializeTransactions() {
        // No-op for now
    }

    override suspend fun saveTransactionToFirestore(transaction: TransactionData): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun getTransactionsFromFirestore(userId: String): Result<List<TransactionData>> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun updateTransactionInFirestore(transaction: TransactionData): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun deleteTransactionFromFirestore(transactionId: String, userId: String): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun performInitialTransactionSync(userId: String, syncStartDate: Long): Result<Unit> =
        throw UnsupportedOperationException("Firestore is no longer supported")

    override suspend fun getProviderStats(from: Long, to: Long): List<ProviderStat> {
        val all = transactionDao.getAllTransactions().first().map { it.toDomain() }
        return all.filter { (it.date?.time ?: 0L) in from..to }
            .groupBy { it.provider ?: "Unknown" }
            .map { (provider, txs) -> ProviderStat(provider, txs.sumOf { it.amount.toDouble() }.toFloat()) }
            .sortedByDescending { it.total }
    }

    override suspend fun getSmsMessages(startTimeMillis: Long, endTimeMillis: Long): List<SmsMessage> =
        smsDataSource.readSmsMessages().filter { it.dateTime?.time?.let { timestamp -> timestamp in startTimeMillis..endTimeMillis } ?: false }

    override suspend fun refreshSmsData(lastSyncTimestamp: Long): Result<Unit> =
        runCatching { refreshSmsData(0) }

    override suspend fun developerClearUserTransactions(userId: String): Result<Unit> =
        runCatching {
            transactionDao.getAllTransactions().first().forEach { entity ->
                transactionDao.deleteTransaction(entity)
            }
        }

    override suspend fun updateTransaction(transaction: TransactionData): Result<Unit> =
        runCatching {
            val entity = transactionDao.getTransactionById(transaction.id!!)
                ?: throw IllegalArgumentException("Transaction not found")
            transactionDao.updateTransaction(transaction.copy(id = entity.id).toEntity())
        }

    override suspend fun getTransactionCount(): Int {
        return transactionDao.getTransactionCount()
    }

    override suspend fun addTransaction(transaction: TransactionData): Result<Unit> = runCatching {
        val entity = transaction.let {
            if (it.id == null) {
                it.copy(id = UUID.randomUUID().toString())
            } else {
                it
            }
        }.toEntity()
        transactionDao.insertTransaction(entity)
    }
} 
package com.example.finanzaspersonales.data.local.room.dao

import androidx.room.*
import com.example.finanzaspersonales.data.local.room.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionsBetweenDates(startDate: Date, endDate: Date): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId")
    fun getTransactionsByCategory(categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    /**
     * Returns the earliest transaction date as a timestamp (ms) or null if empty
     */
    @Query("SELECT MIN(date) FROM transactions")
    suspend fun getMinTransactionDateMillis(): Long?

    /**
     * Returns the latest transaction date as a timestamp (ms) or null if empty
     */
    @Query("SELECT MAX(date) FROM transactions")
    suspend fun getMaxTransactionDateMillis(): Long?
} 
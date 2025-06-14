package com.example.finanzaspersonales.domain.usecase

import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class GetSpendingHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(startDate: Date, endDate: Date): Flow<List<TransactionData>> {
        // TODO: Implement the logic to get spending history from the repository
        return transactionRepository.getTransactionsBetweenDates(startDate, endDate)
    }
} 
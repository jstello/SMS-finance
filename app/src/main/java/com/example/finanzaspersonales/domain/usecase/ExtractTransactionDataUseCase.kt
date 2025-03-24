package com.example.finanzaspersonales.domain.usecase

import android.content.Context
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.util.TextExtractors

/**
 * Use case for extracting transaction data from SMS messages
 */
class ExtractTransactionDataUseCase(private val context: Context) {
    
    /**
     * Convert a list of SmsMessage objects to a list of TransactionData objects
     */
    fun execute(messages: List<SmsMessage>): List<TransactionData> {
        return messages.mapNotNull { message ->
            if (message.dateTime != null && message.numericAmount != null) {
                // Extract provider name
                val provider = TextExtractors.extractProviderFromBody(message.body)
                
                // Extract account information
                val (accountInfo, sourceAccount) = TextExtractors.detectAccountInfo(message.body)
                
                // Try to get contact name if account contains a phone number
                val phoneNumber = sourceAccount?.let { TextExtractors.extractPhoneNumberFromAccount(it) }
                val contactName = phoneNumber?.let { TextExtractors.lookupContactName(context, it) }
                
                TransactionData(
                    date = message.dateTime,
                    amount = message.numericAmount,
                    isIncome = TextExtractors.isIncome(message.body),
                    originalMessage = message,
                    provider = provider,
                    contactName = contactName,
                    accountInfo = accountInfo
                )
            } else null
        }
    }
} 
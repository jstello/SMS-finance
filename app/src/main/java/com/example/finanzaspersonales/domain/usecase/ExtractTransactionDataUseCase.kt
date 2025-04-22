package com.example.finanzaspersonales.domain.usecase

import android.content.Context
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.domain.util.TextExtractors
import java.util.UUID

/**
 * Use case for extracting transaction data from SMS messages
 */
class ExtractTransactionDataUseCase(private val context: Context) {
    
    // Regex to find potential phone numbers starting with many zeros
    private val directPhonePattern = Regex("0{4,}(3\\d{9})")

    /**
     * Convert a list of SmsMessage objects to a list of TransactionData objects
     */
    fun execute(messages: List<SmsMessage>): List<TransactionData> {
        return messages.mapNotNull { message ->
            // Basic validation: ensure date and amount are present
            if (message.dateTime != null && message.numericAmount != null) {
                // Extract provider name
                var provider = TextExtractors.extractProviderFromBody(message.body)
                
                // Extract account information
                val (accountInfo, sourceAccount) = TextExtractors.detectAccountInfo(message.body)
                
                // Try to get contact name if account contains a phone number
                val phoneNumberFromAccount = sourceAccount?.let { TextExtractors.extractPhoneNumberFromAccount(it) }
                var contactName = phoneNumberFromAccount?.let { TextExtractors.lookupContactName(context, it) }
                
                // --- New Direct Phone Number Check ---
                val directPhoneMatch = directPhonePattern.find(message.body)
                if (directPhoneMatch != null) {
                    val directPhoneNumber = directPhoneMatch.groupValues[1]
                    val directContact = TextExtractors.lookupContactName(context, directPhoneNumber)
                    if (directContact != null) {
                        // Prioritize contact found via direct pattern match
                        contactName = directContact
                        // If no provider was found via original logic, use contact name as provider
                        if (provider == null) {
                            provider = directContact
                        }
                    }
                }
                // --- End New Check ---

                TransactionData(
                    id = null, // ID should be generated later by the repository
                    date = message.dateTime,
                    amount = message.numericAmount,
                    isIncome = TextExtractors.isIncome(message.body),
                    description = message.body, // Populate description field with SMS body
                    provider = provider,
                    contactName = contactName,
                    accountInfo = accountInfo
                )
            } else {
                 null // Skip messages without date or amount
            }
        }
    }
} 
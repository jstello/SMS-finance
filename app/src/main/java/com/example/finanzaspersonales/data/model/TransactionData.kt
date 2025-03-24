package com.example.finanzaspersonales.data.model

import java.util.Date

/**
 * Data class representing a financial transaction extracted from an SMS
 */
data class TransactionData(
    val date: Date,
    val amount: Float,
    val isIncome: Boolean,
    val originalMessage: SmsMessage,
    val provider: String? = null,
    val contactName: String? = null,
    val accountInfo: String? = null,
    var categoryId: String? = null
) 
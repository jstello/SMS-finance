package com.example.finanzaspersonales.data.model

import java.util.Date

/**
 * Data class representing an SMS message with financial information
 */
data class SmsMessage(
    val address: String,
    val body: String,
    val amount: String? = null,
    val numericAmount: Float? = null,
    val dateTime: Date? = null,
    val detectedAccount: String? = null,
    val sourceAccount: String? = null,
    val recipientContact: String? = null,
    val recipientPhoneNumber: String? = null,
    val provider: String? = null
) 
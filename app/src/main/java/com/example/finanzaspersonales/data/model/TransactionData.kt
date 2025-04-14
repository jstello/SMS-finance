package com.example.finanzaspersonales.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Data class representing a financial transaction.
 * Updated for Firestore compatibility.
 */
data class TransactionData(
    @DocumentId
    val id: String? = null,
    val userId: String? = null,
    val date: Date,
    val amount: Float,
    val isIncome: Boolean,
    val description: String? = null,
    val provider: String? = null,
    val contactName: String? = null,
    val accountInfo: String? = null,
    var categoryId: String? = null
) {
    constructor() : this(
        id = null,
        userId = null,
        date = Date(0),
        amount = 0f,
        isIncome = false,
        description = null,
        provider = null,
        contactName = null,
        accountInfo = null,
        categoryId = null
    )
} 
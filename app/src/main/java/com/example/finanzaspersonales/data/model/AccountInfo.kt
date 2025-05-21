package com.example.finanzaspersonales.data.model

/**
 * Data class representing account information.
 * Updated for Firestore compatibility.
 */
data class AccountInfo(
    val id: String? = null,
    val userId: String? = null,
    val contactName: String = "",
    val phoneNumber: String = "",
    val accountNumber: String = "",
    val bankName: String = ""
) {
    // Explicit no-argument constructor for Firestore
    constructor() : this(id = null, userId = null, contactName = "", phoneNumber = "", accountNumber = "", bankName = "")
} 
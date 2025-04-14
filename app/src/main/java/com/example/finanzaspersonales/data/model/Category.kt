package com.example.finanzaspersonales.data.model

import com.google.firebase.firestore.DocumentId
// Remove UUID import if no longer needed elsewhere
// import java.util.UUID 

/**
 * Data class representing a spending category.
 * Updated for Firestore compatibility.
 */
data class Category(
    @DocumentId
    val id: String? = null, // Make nullable, add annotation
    val userId: String? = null, // Add userId
    val name: String = "", // Provide default for no-arg constructor
    val color: Int = 0   // Provide default for no-arg constructor
) {
    // Explicit no-argument constructor for Firestore (optional but safer)
    constructor() : this(id = null, userId = null, name = "", color = 0)
} 
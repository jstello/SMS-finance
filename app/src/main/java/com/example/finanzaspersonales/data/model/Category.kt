package com.example.finanzaspersonales.data.model

import java.util.UUID

/**
 * Data class representing a spending category
 */
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Int // Color resource or ARGB value
) 
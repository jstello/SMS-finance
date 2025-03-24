package com.example.finanzaspersonales.domain.util

import java.util.Locale

/**
 * Utility functions for string operations
 */
object StringUtils {
    
    /**
     * Capitalizes the first letter of a string
     */
    fun capitalize(str: String): String {
        return str.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
    }
    
    /**
     * Formats a number as a currency string
     */
    fun formatAmount(amount: Float): String {
        return String.format("%.2f", amount)
    }
} 
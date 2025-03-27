package com.example.finanzaspersonales.domain.util

import java.util.Locale
import java.text.DecimalFormat

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
     * Formats a number as a currency string with thousands separators
     */
    fun formatAmount(amount: Float): String {
        val formatter = DecimalFormat("#,##0.00")
        return formatter.format(amount)
    }
} 
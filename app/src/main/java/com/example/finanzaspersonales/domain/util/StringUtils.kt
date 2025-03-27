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

    /**
     * Extracts a Colombian phone number from a provider string that follows the pattern
     * of leading zeros followed by a 10-digit number starting with 3
     *
     * @param providerString The provider string that may contain a phone number
     * @return The extracted phone number or null if no pattern match found
     */
    fun extractPhoneNumber(providerString: String?): String? {
        if (providerString == null) return null
        
        // Pattern: sequence of zeros followed by a 10-digit number starting with 3
        val regex = Regex("^0*(3\\d{9})$")
        val matchResult = regex.find(providerString)
        
        return matchResult?.groupValues?.get(1)
    }
} 
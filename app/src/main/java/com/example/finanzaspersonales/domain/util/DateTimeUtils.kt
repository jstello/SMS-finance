package com.example.finanzaspersonales.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

/**
 * Utility functions for date and time operations
 */
object DateTimeUtils {
    
    /**
     * Extracts date and time from SMS body text
     */
    fun extractDateTimeFromBody(body: String): Date? {
        val pattern = Pattern.compile("""(\d{2}/\d{2}/\d{4}).*?(\d{2}:\d{2}(?::\d{2})?)|(\d{2}:\d{2}(?::\d{2})?).*?(\d{2}/\d{2}/\d{4})""")
        val matcher = pattern.matcher(body)
        
        return if (matcher.find()) {
            when {
                matcher.group(1) != null && matcher.group(2) != null -> 
                    parseDateTimeString("${matcher.group(1)} ${matcher.group(2)}")
                matcher.group(3) != null && matcher.group(4) != null -> 
                    parseDateTimeString("${matcher.group(4)} ${matcher.group(3)}")
                else -> null
            }
        } else null
    }
    
    /**
     * Parses a date time string in the format "dd/MM/yyyy HH:mm:ss" or "dd/MM/yyyy HH:mm"
     */
    fun parseDateTimeString(dateTimeStr: String): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(dateTimeStr)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(dateTimeStr)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Formats a date as a string in the format "dd/MM/yyyy"
     */
    fun formatDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }
    
    /**
     * Gets the year from a date
     */
    fun Date.toYear(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = this
        return calendar.get(Calendar.YEAR)
    }
    
    /**
     * Gets the month (1-12) from a date
     */
    fun Date.toMonth(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = this
        return calendar.get(Calendar.MONTH) + 1
    }
    
    /**
     * Gets the day of month from a date
     */
    fun Date.toDay(): Int {
        val calendar = Calendar.getInstance()
        calendar.time = this
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Formats a LocalDateTime object to a string with a specific pattern.
     */
    fun formatLocalDateTime(dateTime: LocalDateTime, pattern: String = "dd MMM yyyy HH:mm"): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            dateTime.format(formatter)
        } catch (e: Exception) {
            // Fallback or error representation
            dateTime.toString() // Or some other default like "Error formatting date"
        }
    }

    /**
     * Formats a java.util.Date to a string with a specific pattern by converting to LocalDateTime.
     */
    fun formatDateTime(date: Date?, pattern: String = "dd MMM yyyy HH:mm"): String = date?.let {
        try {
            val ldt = LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            ldt.format(formatter)
        } catch (e: Exception) {
            it.toString()
        }
    } ?: ""
} 
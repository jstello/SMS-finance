package com.example.finanzaspersonales.domain.util

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import java.util.regex.Pattern
import kotlin.text.RegexOption

/**
 * Utility functions for extracting information from SMS text
 */
object TextExtractors {
    
    // Compile URL pattern once - combine all patterns into a single regex
    private val URL_PATTERN = Pattern.compile(
        """(https?://\S+)|(bit\.ly/\S+)|(tinyurl\.com/\S+)|(goo\.gl/\S+)|(www\.\S+\.\S+)|(\S+\.(com|net|org|co|io)/\S+)""",
        Pattern.CASE_INSENSITIVE
    )
    
    // Compile promotional keywords pattern once
    private val PROMO_KEYWORDS_PATTERN = Pattern.compile(
        """(promocion|promoción|descuento|oferta|hotsale|sale|ahorra|gana|sorteo|codigo|código|promo|cupon|cupón)""",
        Pattern.CASE_INSENSITIVE
    )
    
    // Cache for promotional message detection results
    private val promoMessageCache = mutableMapOf<String, Boolean>()
    
    /**
     * Checks if a message contains URLs, which likely indicates
     * it's a promotional message rather than a transaction
     */
    fun containsUrl(body: String): Boolean {
        return URL_PATTERN.matcher(body).find()
    }
    
    /**
     * Determines if a message is likely a promotional message
     * and not a genuine transaction
     */
    fun isPromotionalMessage(body: String): Boolean {
        // Check cache first
        promoMessageCache[body]?.let { return it }
        
        val result = containsUrl(body) || PROMO_KEYWORDS_PATTERN.matcher(body).find()
        
        // Cache the result
        promoMessageCache[body] = result
        return result
    }
    
    /**
     * Clear promotional message cache when no longer needed
     * (call this when transaction processing is complete)
     */
    fun clearPromoCache() {
        promoMessageCache.clear()
    }
    
    /**
     * Process message for transaction data
     * Returns null if promotional, otherwise processes the message
     */
    fun processMessage(body: String): Boolean {
        // Check once if it's a promotional message
        if (isPromotionalMessage(body)) {
            return false
        }
        return true
    }
    
    /**
     * Extracts provider name from SMS body text
     */
    fun extractProviderFromBody(body: String): String? {
        // Skip promotional messages - use cached result when possible
        if (promoMessageCache[body] == true || (promoMessageCache[body] == null && isPromotionalMessage(body))) {
            return null
        }
        
        // First, try to extract the provider using the common Bancolombia pattern
        val bancolombiaPattern = Pattern.compile("""(?:Compraste|pagaste)(?:\s[\$\w,.]+\s|\s)en\s((?:[A-Z0-9]|[*])+(?:\s[A-Z0-9]+)*)""")
        val matcher = bancolombiaPattern.matcher(body)
        
        if (matcher.find()) {
            return matcher.group(1)
        }
        
        // Try to match common banking terms
        val bankingPattern = Pattern.compile(
            """(?:confirmaci[óo]n|transferencia|pago|compra)\s(?:(?:a|en|de)\s)?(.+?)(?:\s(?:por|con|desde|de))""",
            Pattern.CASE_INSENSITIVE
        )
        val bankingMatcher = bankingPattern.matcher(body)
        if (bankingMatcher.find()) {
            return bankingMatcher.group(1)
        }
        
        // Fallback to finding any words in ALL CAPS with at least 3 characters
        val allCapsPattern = Pattern.compile("""(?<!\w)([A-Z0-9][A-Z0-9*]+(?:\s[A-Z0-9]+)*)(?!\w)""")
        val allCapsMatcher = allCapsPattern.matcher(body)
        
        // Find the longest all-caps match that's likely to be a provider
        var bestMatch: String? = null
        var maxLength = 0
        
        while (allCapsMatcher.find()) {
            val match = allCapsMatcher.group(1)
            // Skip known non-provider words often in ALL CAPS (add more as needed)
            val matchLength = match?.length ?: 0
            if (match != "COP" && match != "USD" && matchLength > 3 && matchLength > maxLength) {
                maxLength = matchLength
                bestMatch = match
            }
        }
        
        return bestMatch
    }
    
    /**
     * Extracts amount from SMS body text
     */
    fun extractAmountFromBody(body: String): String? {
        // Skip promotional messages - use cached result
        if (promoMessageCache[body] == true) {
            return null
        }
        
        val pattern = Pattern.compile("""(\$|COP)\s*((\d{1,3}(?:[.,]\d{3})*|\d+))(?:([.,])(\d{2}))?""")
        val matcher = pattern.matcher(body)
        return if (matcher.find()) {
            val currency = matcher.group(1)
            val mainNumber = matcher.group(2)?.replace("[.,]".toRegex(), "")
            val decimal = matcher.group(5)
            
            when {
                decimal == null -> "$currency$mainNumber"
                decimal == "00" -> "$currency$mainNumber"
                else -> "$currency$mainNumber.$decimal"
            }
        } else null
    }
    
    /**
     * Parse amount string to float
     */
    fun parseToFloat(amount: String?): Float? {
        return amount?.replace("^(\\\$|COP)".toRegex(), "")
            ?.replace(",", ".")
            ?.toFloatOrNull()
    }
    
    /**
     * Determines if a transaction is an income based on SMS body text
     */
    fun isIncome(body: String): Boolean {
        // Skip promotional messages - use cached result
        if (promoMessageCache[body] == true) {
            return false
        }
        
        return body.contains(
            Regex("(recepci[óo]n|recibiste|n[óo]mina|abono|consignaci[óo]n|dep[óo]sito|ingreso)", RegexOption.IGNORE_CASE)
        )
    }
    
    /**
     * Extract account information from SMS body
     * Returns a pair of (detectedAccount, sourceAccount)
     */
    fun detectAccountInfo(body: String): Pair<String?, String?> {
        // Skip promotional messages - use cached result
        if (promoMessageCache[body] == true) {
            return Pair(null, null)
        }
        
        // Bancolombia pattern - highest priority
        val bancolombiaPattern = Pattern.compile(
            "a\\s(.+?)\\sdesde\\sproducto\\s([*]\\d+)"
        )
        val bcMatcher = bancolombiaPattern.matcher(body)
        if (bcMatcher.find()) {
            return Pair(bcMatcher.group(1), bcMatcher.group(2))
        }
        
        // General account patterns
        val accountPatterns = listOf(
            // Account patterns with variations
            Pattern.compile("(cuenta|producto|desde)\\s*([*]\\d+)"),
            Pattern.compile("a\\s(.+?)\\sdesde"),
            Pattern.compile("(?:destino|destinatario)\\s+([\\w\\s*]+)"),
            Pattern.compile("(?:enviado|envio|transferido)\\s+a\\s+([\\w\\s*]+)"),
            Pattern.compile("(?:n[úu]mero|cuenta)\\s+([*\\d]+)")
        )
        
        for (pattern in accountPatterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                when {
                    matcher.groupCount() >= 2 && matcher.group(2) != null -> {
                        return Pair(matcher.group(2), matcher.group(2))
                    }
                    matcher.group(1) != null -> {
                        return Pair(matcher.group(1), null)
                    }
                }
            }
        }
        
        return Pair(null, null)
    }
    
    /**
     * Extract phone number from account information
     * Often phone numbers are masked within account numbers like *0000+3XXXXXXXXX or 00000032228962828
     */
    fun extractPhoneNumberFromAccount(account: String): String? {
        // Pattern for Colombia mobile numbers starting with 3
        // Matches:
        // - Optional * at start
        // - 3 or more zeros
        // - Optional + or other non-digit character
        // - 10 digit number starting with 3
        val mobilePattern = Regex("""[*]?0{3,}[^0-9]?(3\d{9})""")
        val mobileMatch = mobilePattern.find(account)
        if (mobileMatch != null) {
            return mobileMatch.groupValues[1]
        }
        
        // General phone number pattern - look for sequences of 10+ digits
        val generalPattern = Regex("""(\d{10,})""")
        val generalMatch = generalPattern.find(account)
        if (generalMatch != null) {
            val number = generalMatch.groupValues[1]
            // Only return if it starts with 3 and is 10 digits
            if (number.startsWith("3") && number.length == 10) {
                return number
            }
        }
        
        return null
    }
    
    /**
     * Lookup contact name from phone number
     */
    fun lookupContactName(context: Context, phoneNumber: String): String? {
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(phoneNumber).build()
            
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }
        
        return null
    }
} 
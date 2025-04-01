package com.example.finanzaspersonales.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.data.model.TransactionData
import java.util.Date

/**
 * BroadcastReceiver for handling incoming SMS messages
 * Detects financial transaction SMS and logs them
 */
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
        
        // Common bank sender IDs
        private val BANK_SENDERS = listOf(
            "87400", // Bancolombia
            "85432", // Davivienda
            "87746", // Banco de Bogotá
            "87267", // BBVA
            "85764", // Banco Popular
            "3203447373", // Example Bank WhatsApp
            "899273" // Any other bank code
        )
        
        // Transaction keywords to identify financial messages
        private val TRANSACTION_KEYWORDS = listOf(
            "compra", "pago", "transaccion", "transferencia", "retiro", "deposito", 
            "payout", "payment", "transfer", "transaction", "purchase", "pagaste",
            "recibiste", "withdraw", "deposit"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val sender = message.displayOriginatingAddress
                val body = message.messageBody
                
                // Log all SMS for debugging
                Log.d(TAG, "SMS from: $sender")
                Log.d(TAG, "Body: $body")
                
                // Check if this is a financial transaction SMS
                if (isFinancialSms(sender, body)) {
                    Log.i(TAG, "Financial transaction detected from $sender")
                    
                    // Extract transaction details (basic implementation)
                    val smsMessage = createSmsMessage(sender, body)
                    val transaction = parseTransaction(smsMessage)
                    if (transaction != null) {
                        Log.i(TAG, "Parsed transaction: $transaction")
                        // In Phase 2, we'll trigger a notification here
                    }
                }
            }
        }
    }
    
    /**
     * Determine if an SMS is likely a financial transaction
     */
    private fun isFinancialSms(sender: String, body: String): Boolean {
        // Check if sender is from a known bank
        val isFromBank = BANK_SENDERS.any { sender.contains(it) }
        
        // Check if body contains transaction keywords
        val hasTransactionKeywords = TRANSACTION_KEYWORDS.any { 
            body.lowercase().contains(it.lowercase()) 
        }
        
        // In the future, we can add more sophisticated detection
        return isFromBank || hasTransactionKeywords
    }
    
    /**
     * Create an SmsMessage from raw SMS data
     */
    private fun createSmsMessage(sender: String, body: String): SmsMessage {
        val amount = extractAmountString(body)
        val numericAmount = extractNumericAmount(amount)
        
        return SmsMessage(
            address = sender,
            body = body,
            amount = amount,
            numericAmount = numericAmount,
            dateTime = Date(),
            detectedAccount = extractAccount(body),
            provider = sender,
            sourceAccount = null,
            recipientContact = null,
            recipientPhoneNumber = null
        )
    }
    
    /**
     * Parse transaction details from SMS
     */
    private fun parseTransaction(smsMessage: SmsMessage): TransactionData? {
        // Basic implementation - just create a transaction with minimal info
        return try {
            val isIncome = isIncomingTransaction(smsMessage.body)
            val amount = smsMessage.numericAmount ?: 0.0f
            
            TransactionData(
                date = smsMessage.dateTime ?: Date(),
                amount = amount,
                isIncome = isIncome,
                originalMessage = smsMessage,
                provider = smsMessage.provider,
                contactName = smsMessage.recipientContact,
                accountInfo = smsMessage.detectedAccount,
                categoryId = null // No category assigned yet
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transaction", e)
            null
        }
    }
    
    /**
     * Determine if a transaction is incoming (income) based on message text
     */
    private fun isIncomingTransaction(body: String): Boolean {
        val incomeKeywords = listOf(
            "recibiste", "deposito", "abono", "transferencia recibida",
            "received", "deposit", "credit", "incoming transfer"
        )
        
        return incomeKeywords.any { body.lowercase().contains(it.lowercase()) }
    }
    
    /**
     * Extract the transaction amount string from message body
     */
    private fun extractAmountString(body: String): String? {
        // Look for currency patterns like $1,234.56 or 1.234,56 
        val regex = """\$?\s?(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{2})?)""".toRegex()
        val match = regex.find(body)
        
        return match?.groupValues?.get(1)
    }
    
    /**
     * Convert amount string to numeric value
     */
    private fun extractNumericAmount(amountString: String?): Float? {
        if (amountString == null) return null
        
        return try {
            // Remove thousands separators and convert to double
            val normalized = amountString.replace(",", "").replace(".", "")
            normalized.toFloat() / 100 // Assuming last two digits are cents
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse amount: $amountString", e)
            null
        }
    }
    
    /**
     * Extract the account information from message body
     */
    private fun extractAccount(body: String): String? {
        // Look for account patterns like "cuenta *1234" or "tarjeta *4321"
        val regex = """(?:cuenta|tarjeta|card|account)\s+[*x](\d{4})""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(body)
        
        return match?.groupValues?.get(1)?.let { "*$it" }
    }
} 
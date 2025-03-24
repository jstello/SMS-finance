package com.example.finanzaspersonales.data.local

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.domain.util.DateTimeUtils
import com.example.finanzaspersonales.domain.util.TextExtractors

/**
 * Data source for SMS messages
 */
class SmsDataSource(private val context: Context) {
    
    /**
     * Read SMS messages from the device with enhanced filtering for financial institutions
     */
    fun readSmsMessages(): List<SmsMessage> {
        val smsMessages = mutableListOf<SmsMessage>()
        
        // Create a filter for known financial institutions
        val bankFilters = listOf(
            "%Bancolombia%", 
            "%Nequi%", 
            "%Daviplata%", 
            "%BBVA%", 
            "%Davivienda%", 
            "%Banco%", 
            "%transfer%", 
            "%cuenta%", 
            "%pago%", 
            "%compra%",
            "%tarjeta%",
            "%credito%"
        )
        
        // Build SQL WHERE clause dynamically
        val whereClause = StringBuilder("(")
        val whereArgs = bankFilters.mapIndexed { index, filter ->
            if (index > 0) whereClause.append(" OR ")
            whereClause.append("${Telephony.Sms.BODY} LIKE ?")
            filter
        }.toTypedArray()
        whereClause.append(")")
        
        val cursor: Cursor? = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            whereClause.toString(),
            whereArgs,
            "${Telephony.Sms.DATE} DESC LIMIT 500" // Limit to 500 most recent messages for performance
        )
        
        cursor?.use { c ->
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
            
            while (c.moveToNext()) {
                val address = c.getString(addressIndex)
                val body = c.getString(bodyIndex)
                
                // Extract amount with improved logic
                val amount = TextExtractors.extractAmountFromBody(body)
                val numericAmount = TextExtractors.parseToFloat(amount)
                
                // Only process if we found an amount or there are financial keywords
                if (numericAmount != null || containsFinancialKeywords(body)) {
                    // Extract date either from SMS metadata or from body
                    val dateFromBody = DateTimeUtils.extractDateTimeFromBody(body)
                    val dateTime = dateFromBody ?: java.util.Date(c.getLong(dateIndex))
                    
                    // Extract account information
                    val (detectedAccount, sourceAccount) = TextExtractors.detectAccountInfo(body)
                    
                    // Extract phone number from account if available
                    val phoneNumber = detectedAccount?.let { account -> 
                        TextExtractors.extractPhoneNumberFromAccount(account) 
                    }
                    
                    // Look up contact name if phone number is available
                    val contactName = phoneNumber?.let { number ->
                        TextExtractors.lookupContactName(context, number)
                    }
                    
                    val message = SmsMessage(
                        address = address,
                        body = body,
                        amount = amount,
                        numericAmount = numericAmount,
                        dateTime = dateTime,
                        detectedAccount = detectedAccount,
                        sourceAccount = sourceAccount,
                        recipientContact = contactName,
                        recipientPhoneNumber = phoneNumber,
                        provider = TextExtractors.extractProviderFromBody(body)
                    )
                    
                    smsMessages.add(message)
                }
            }
        }
        
        return smsMessages
    }
    
    /**
     * Check if the message contains financial keywords
     */
    private fun containsFinancialKeywords(body: String): Boolean {
        val keywords = listOf(
            "transferencia", "transfer", "pago", "payment", "compra", "purchase",
            "cuenta", "account", "tarjeta", "card", "débito", "debit", "crédito", "credit",
            "recibo", "factura", "bill", "receipt", "efectivo", "cash",
            "transacción", "transaction", "saldo", "balance", "dinero", "money",
            "banco", "bank", "cajero", "atm", "depósito", "deposit", "retiro", "withdraw"
        )
        
        val lowerBody = body.lowercase()
        return keywords.any { lowerBody.contains(it.lowercase()) }
    }
} 
package com.example.finanzaspersonales.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.finanzaspersonales.data.model.SmsMessage
import com.example.finanzaspersonales.domain.util.DateTimeUtils
import com.example.finanzaspersonales.domain.util.TextExtractors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for SMS messages
 */
@Singleton // Added Singleton scope
class SmsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "SmsDataSource"
    }
    
    /**
     * Check if SMS read permission is granted
     */
    fun hasReadSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Read SMS messages from the device with enhanced filtering for financial institutions
     * Optionally limit by date range
     */
    fun readSmsMessages(
        limitToRecentMonths: Int = 12,
        maxResults: Int = 500
    ): List<SmsMessage> {
        // Check for permission first
        if (!hasReadSmsPermission()) {
            Log.e(TAG, "SMS_PERMISSION_DENIED: Cannot read SMS messages without READ_SMS permission")
            return emptyList()
        }
        
        val smsMessages = mutableListOf<SmsMessage>()
        
        try {
            // Create a filter for known financial institutions and keywords
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
                "%credito%",
                "%debito%",
                "%recepcion%",
                "%recibiste%",
                "%nomina%",
                "%abono%",
                "%consignacion%",
                "%deposito%",
                "%ingreso%",
                "%retiro%",
                "%cajero%",
                "%efectivo%",
                "%transaccion%"
            )
            
            // Calculate date filter if limiting by recent months
            var dateConstraint = ""
            var dateArg: String? = null
            
            if (limitToRecentMonths > 0) {
                val calendar = java.util.Calendar.getInstance()
                calendar.add(java.util.Calendar.MONTH, -limitToRecentMonths)
                val cutoffTimestamp = calendar.timeInMillis
                
                dateConstraint = " AND ${Telephony.Sms.DATE} >= ?"
                dateArg = cutoffTimestamp.toString()
                
                Log.d(TAG, "Limiting SMS to last $limitToRecentMonths months (since ${java.util.Date(cutoffTimestamp)})")
            }
            
            // Build SQL WHERE clause dynamically
            val whereClause = StringBuilder("(")
            val whereArgs = mutableListOf<String>()
            
            bankFilters.forEachIndexed { index, filter ->
                if (index > 0) whereClause.append(" OR ")
                whereClause.append("${Telephony.Sms.BODY} LIKE ?")
                whereArgs.add(filter)
            }
            
            whereClause.append(")")
            whereClause.append(dateConstraint) // Add date constraint if needed
            
            // Add date arg if we have one
            if (dateArg != null) {
                whereArgs.add(dateArg)
            }
            
            Log.d(TAG, "Querying SMS content provider with filter and limit to $limitToRecentMonths months")
            
            val cursor: Cursor? = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                whereClause.toString(),
                whereArgs.toTypedArray(),
                "${Telephony.Sms.DATE} DESC LIMIT $maxResults" // Limit results for performance
            )
            
            cursor?.use { c ->
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                
                Log.d(TAG, "Found ${c.count} SMS messages matching filter")
                
                while (c.moveToNext()) {
                    val address = c.getString(addressIndex)
                    val body = c.getString(bodyIndex)
                    
                    // ENFORCE: Only process messages starting with 'Bancolombia:' Do not remove this line
                    if (!body.startsWith("Bancolombia:", ignoreCase = true)) continue
                    
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
            
            Log.d(TAG, "Processed and returning ${smsMessages.size} relevant financial messages")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS messages", e)
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
            "banco", "bank", "cajero", "atm", "depósito", "deposit", "retiro", "withdraw",
            "nómina", "payroll", "abono", "consignación", "ingreso", "income"
        )
        
        val lowerBody = body.lowercase()
        return keywords.any { lowerBody.contains(it.lowercase()) }
    }
} 
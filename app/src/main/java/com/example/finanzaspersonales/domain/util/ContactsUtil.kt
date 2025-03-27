package com.example.finanzaspersonales.domain.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract

/**
 * Utility class for handling contact-related operations
 */
object ContactsUtil {

    /**
     * Finds a contact name by phone number
     *
     * @param context The application context
     * @param phoneNumber The phone number to look up
     * @return The contact name if found, or null if no contact matches
     */
    fun getContactNameFromPhoneNumber(context: Context, phoneNumber: String): String? {
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash if contacts permission not granted or other error
            e.printStackTrace()
        }
        
        return null
    }
} 
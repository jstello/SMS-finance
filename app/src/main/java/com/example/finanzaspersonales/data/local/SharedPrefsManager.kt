package com.example.finanzaspersonales.data.local

import android.content.Context
import com.example.finanzaspersonales.data.model.AccountInfo
import com.example.finanzaspersonales.data.model.Category
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import android.util.Log

/**
 * Manager for SharedPreferences
 */
class SharedPrefsManager(private val context: Context) {
    
    private val gson = Gson()
    
    // Account preferences
    private val accountPrefs = context.getSharedPreferences(ACCOUNT_PREFS, Context.MODE_PRIVATE)
    
    // Category preferences
    private val categoryPrefs = context.getSharedPreferences(CATEGORY_PREFS, Context.MODE_PRIVATE)
    
    // Transaction preferences
    private val transactionPrefs = context.getSharedPreferences(TRANSACTION_PREFS, Context.MODE_PRIVATE)
    
    // Sync preferences
    private val syncPrefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
    
    /**
     * Save accounts
     */
    fun saveAccounts(accounts: List<AccountInfo>) {
        val json = gson.toJson(accounts)
        accountPrefs.edit().putString(KEY_ACCOUNTS, json).apply()
    }
    
    /**
     * Load accounts
     */
    fun loadAccounts(): List<AccountInfo> {
        val json = accountPrefs.getString(KEY_ACCOUNTS, null)
        return if (json != null) {
            val type = object : TypeToken<List<AccountInfo>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }
    
    /**
     * Save categories
     */
    fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        categoryPrefs.edit().putString(KEY_CATEGORIES, json).apply()
    }
    
    /**
     * Load categories
     */
    fun loadCategories(): List<Category> {
        val json = categoryPrefs.getString(KEY_CATEGORIES, null)
        return if (json != null) {
            val type = object : TypeToken<List<Category>>() {}.type
            gson.fromJson(json, type)
        } else {
            DEFAULT_CATEGORIES
        }
    }
    
    /**
     * Save transaction categories - maps transaction IDs to category IDs
     */
    fun saveTransactionCategories(transactionCategoryMap: Map<String, String>) {
        val json = gson.toJson(transactionCategoryMap)
        Log.d("SharedPrefsManager", "Saving transaction categories JSON: $json (Size: ${transactionCategoryMap.size})")
        try {
            transactionPrefs.edit().putString(KEY_TRANSACTION_CATEGORIES, json).apply()
            Log.d("SharedPrefsManager", "Successfully applied transaction categories save.")
        } catch (e: Exception) {
            Log.e("SharedPrefsManager", "Error saving transaction categories to SharedPreferences", e)
        }
    }
    
    /**
     * Load transaction categories
     */
    fun loadTransactionCategories(): Map<String, String> {
        val json = transactionPrefs.getString(KEY_TRANSACTION_CATEGORIES, null)
        return if (json != null) {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyMap()
        }
    }
    
    // Sync status functions
    /**
     * Check if initial sync has been completed for a user
     */
    fun hasCompletedInitialSync(userId: String): Boolean {
        return syncPrefs.getBoolean(getSyncStatusKey(userId), false)
    }

    /**
     * Mark initial sync as completed for a user
     */
    fun markInitialSyncComplete(userId: String) {
        syncPrefs.edit().putBoolean(getSyncStatusKey(userId), true).apply()
    }

    private fun getSyncStatusKey(userId: String): String {
        return "${KEY_INITIAL_SYNC_STATUS_PREFIX}_$userId"
    }
    
    companion object {
        private const val ACCOUNT_PREFS = "account_prefs"
        private const val CATEGORY_PREFS = "category_prefs"
        private const val TRANSACTION_PREFS = "transaction_prefs"
        private const val SYNC_PREFS = "sync_prefs"
        
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_TRANSACTION_CATEGORIES = "transaction_categories"
        private const val KEY_INITIAL_SYNC_STATUS_PREFIX = "initial_sync_status"
        
        // Default categories with stable UUIDs
        val DEFAULT_CATEGORIES = listOf(
            Category(id = "d5f1e4a3-9a86-4f9c-9a7b-1c9e3d5a7b1f", name = "Food & Dining", color = 0xFF4CAF50.toInt()),
            Category(id = "b8e2d7c6-5b9a-4e8f-8c3d-2b7e6a5d9c4e", name = "Transportation", color = 0xFF2196F3.toInt()),
            Category(id = "a1c3b5d7-8e2f-4a9c-9b8e-3d7c1b5a9e6d", name = "Shopping", color = 0xFFF44336.toInt()),
            Category(id = "f4e8d2a6-7b1c-4f9a-8d5c-4e9b1a8d3c7b", name = "Entertainment", color = 0xFF9C27B0.toInt()),
            Category(id = "c9b7e3d1-5a6f-4c8e-9a2b-5d1c7e9a3b8d", name = "Housing", color = 0xFF795548.toInt()),
            Category(id = "e2d6c8b4-9f3a-4a7d-8b1e-6c9a5b3d1e7f", name = "Utilities", color = 0xFF607D8B.toInt()),
            Category(id = "a7b3c9d5-8e1f-4b6a-9c4d-7e3b1a9d5c2e", name = "Health", color = 0xFFE91E63.toInt()),
            Category(id = "d1e7f3b9-6a4c-4d8e-a1b9-8c5d7e3a1b9d", name = "Personal", color = 0xFFFF9800.toInt()),
            Category(id = "b3c7a1d9-5e8f-4a2c-8b6d-9e5c3a1b7d4e", name = "Education", color = 0xFF3F51B5.toInt()),
            Category(id = "e9f1d7b3-a4c8-4e6f-9a1d-b5c8e3a7d1b9", name = "Investments", color = 0xFF009688.toInt()),
            Category(id = "c5d9e1a7-8b3f-4c5e-a9d1-f3b7c5e1a9d3", name = "Payroll", color = 0xFF4CAF50.toInt()),
            Category(id = "f8a3d9c1-7e5b-4f1a-8c9d-e1b7a5d9c3e7", name = "Pets", color = 0xFFFF9800.toInt()),
            Category(id = "a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0", name = "Other", color = 0xFF9E9E9E.toInt()) // Special ID for Other
        )
    }
} 
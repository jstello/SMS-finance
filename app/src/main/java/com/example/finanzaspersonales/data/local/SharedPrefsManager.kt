package com.example.finanzaspersonales.data.local

import android.content.Context
import com.example.finanzaspersonales.data.model.AccountInfo
import com.example.finanzaspersonales.data.model.Category
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        transactionPrefs.edit().putString(KEY_TRANSACTION_CATEGORIES, json).apply()
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
        
        // Default categories
        val DEFAULT_CATEGORIES = listOf(
            Category(name = "Food & Dining", color = 0xFF4CAF50.toInt()),
            Category(name = "Transportation", color = 0xFF2196F3.toInt()),
            Category(name = "Shopping", color = 0xFFF44336.toInt()),
            Category(name = "Entertainment", color = 0xFF9C27B0.toInt()),
            Category(name = "Housing", color = 0xFF795548.toInt()),
            Category(name = "Utilities", color = 0xFF607D8B.toInt()),
            Category(name = "Health", color = 0xFFE91E63.toInt()),
            Category(name = "Personal", color = 0xFFFF9800.toInt()),
            Category(name = "Education", color = 0xFF3F51B5.toInt()),
            Category(name = "Investments", color = 0xFF009688.toInt()),
            Category(name = "Payroll", color = 0xFF4CAF50.toInt()),  // Green for income
            Category(name = "Pets", color = 0xFFFF9800.toInt()),     // Orange for pets
            Category(name = "Other", color = 0xFF9E9E9E.toInt())
        )
    }
} 
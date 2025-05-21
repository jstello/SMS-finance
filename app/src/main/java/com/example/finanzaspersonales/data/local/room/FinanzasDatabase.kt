package com.example.finanzaspersonales.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.finanzaspersonales.data.local.room.dao.TransactionDao
import com.example.finanzaspersonales.data.local.room.dao.CategoryDao
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.finanzaspersonales.data.local.SharedPrefsManager // For DEFAULT_CATEGORIES
import com.example.finanzaspersonales.data.db.mapper.toEntity // For Category.toEntity()
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context // Required for SharedPrefsManager context

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class],
    version = 1, // Increment version if schema changes
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinanzasDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: FinanzasDatabase? = null

        fun getDatabase(context: Context): FinanzasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    FinanzasDatabase::class.java,
                    "finanzas_personales_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate categories on first creation
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val categoryDao = database.categoryDao()
                                // Use SharedPrefsManager to get default categories
                                // Note: SharedPrefsManager itself might need context,
                                // but DEFAULT_CATEGORIES is a static list.
                                SharedPrefsManager.DEFAULT_CATEGORIES.forEach { categoryModel ->
                                    categoryDao.insertCategory(categoryModel.toEntity())
                                }
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 
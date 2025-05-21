package com.example.finanzaspersonales.di

import android.content.Context
import androidx.room.Room
import com.example.finanzaspersonales.data.local.room.FinanzasDatabase
import com.example.finanzaspersonales.data.local.room.dao.TransactionDao
import com.example.finanzaspersonales.data.local.room.dao.CategoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFinanzasDatabase(@ApplicationContext context: Context): FinanzasDatabase {
        return Room.databaseBuilder(
            context,
            FinanzasDatabase::class.java,
            "finanzas_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTransactionDao(database: FinanzasDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: FinanzasDatabase): CategoryDao {
        return database.categoryDao()
    }
} 
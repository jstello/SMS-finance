package com.example.finanzaspersonales.di

import android.content.Context
import android.content.SharedPreferences
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
import com.example.finanzaspersonales.domain.usecase.GetSpendingByCategoryUseCase
import com.example.finanzaspersonales.domain.tools.SpendingInsightsTool
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Provides SharedPrefsManager
    @Provides
    @Singleton
    fun provideSharedPrefsManager(@ApplicationContext context: Context): SharedPrefsManager {
        return SharedPrefsManager(context)
    }

    // Provides SmsDataSource
    @Provides
    @Singleton
    fun provideSmsDataSource(@ApplicationContext context: Context): SmsDataSource {
        return SmsDataSource(context)
    }

    // Provides ExtractTransactionDataUseCase
    @Provides
    @Singleton
    fun provideExtractTransactionDataUseCase(@ApplicationContext context: Context): ExtractTransactionDataUseCase {
        return ExtractTransactionDataUseCase(context)
    }

    // Provides CategoryAssignmentUseCase (Requires CategoryRepository, which Hilt gets from RepositoryModule)
    @Provides
    @Singleton
    fun provideCategoryAssignmentUseCase(
        categoryRepository: CategoryRepository
    ): CategoryAssignmentUseCase {
        return CategoryAssignmentUseCase(categoryRepository)
    }

    // Provides Gson for JSON serialization/deserialization
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setPrettyPrinting()
            .create()
    }

    // Provides SpendingInsightsTool for user-focused financial insights
    @Provides
    @Singleton
    fun provideSpendingInsightsTool(
        getSpendingByCategoryUseCase: GetSpendingByCategoryUseCase,
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        gson: Gson
    ): SpendingInsightsTool {
        return SpendingInsightsTool(
            getSpendingByCategoryUseCase,
            transactionRepository,
            categoryRepository,
            gson
        )
    }

    // Add provides for other dependencies if needed (e.g., Retrofit, Room database)
} 
package com.example.finanzaspersonales.di

import android.content.Context
import android.content.SharedPreferences
import com.example.finanzaspersonales.data.local.SharedPrefsManager
import com.example.finanzaspersonales.data.local.SmsDataSource
import com.example.finanzaspersonales.domain.usecase.CategoryAssignmentUseCase
import com.example.finanzaspersonales.domain.usecase.ExtractTransactionDataUseCase
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
    fun provideCategoryAssignmentUseCase(categoryRepository: com.example.finanzaspersonales.data.repository.CategoryRepository): CategoryAssignmentUseCase {
        return CategoryAssignmentUseCase(categoryRepository)
    }

    // Add provides for other dependencies if needed (e.g., Retrofit, Room database)
} 
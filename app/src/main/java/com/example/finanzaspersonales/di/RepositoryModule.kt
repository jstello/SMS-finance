package com.example.finanzaspersonales.di

import com.example.finanzaspersonales.data.auth.AuthRepository
import com.example.finanzaspersonales.data.auth.AuthRepositoryImpl
import com.example.finanzaspersonales.data.repository.CategoryRepository
import com.example.finanzaspersonales.data.repository.CategoryRepositoryImpl
import com.example.finanzaspersonales.data.repository.TransactionRepository
import com.example.finanzaspersonales.data.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Define scope for bindings (Application scope)
abstract class RepositoryModule {

    @Binds
    @Singleton // Ensure single instance throughout the app
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository
} 
package com.example.finanzaspersonales

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FinanzasApp : Application() {
    companion object {
        private const val TAG = "FinanzasApp"
        // Create a singleton instance to access from anywhere if needed
        private lateinit var instance: FinanzasApp
        
        fun getInstance(): FinanzasApp = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        try {
            // Initialize crash reporting
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
                throwable.printStackTrace()
            }
            
            // Log successful initialization
            Log.d(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing application", e)
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Memory trimmed to level: $level")
    }
} 
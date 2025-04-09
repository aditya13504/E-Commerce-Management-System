package com.yourdomain.ecommerce

import android.app.Application
import com.yourdomain.ecommerce.di.ServiceLocator
import timber.log.Timber

/**
 * Main Application class for the E-commerce app
 */
class ECommerceApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize ServiceLocator instead of Hilt
        ServiceLocator.initialize(this)
    }
} 
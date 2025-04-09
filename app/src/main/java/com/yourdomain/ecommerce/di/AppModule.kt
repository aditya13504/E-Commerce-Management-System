package com.yourdomain.ecommerce.di

import com.yourdomain.ecommerce.core.data.repository.CartRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.OrderRepositoryImpl
import com.yourdomain.ecommerce.core.domain.repository.CartRepository
import com.yourdomain.ecommerce.core.domain.repository.OrderRepository
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Module that provides dependencies through ServiceLocator
 */
object AppModule {
    // Singleton Postgrest instance
    private val postgrest: Postgrest by lazy {
        // In a real implementation, this would come from a SupabaseClient
        // For demo purposes, we'll provide a mock implementation
        Postgrest {}
    }
    
    // Singleton OrderRepository instance
    val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(postgrest)
    }
    
    // Singleton ProductRepository instance
    val productRepository: ProductRepository by lazy {
        // In a real implementation, this would be ProductRepositoryImpl
        // For now, just to make compilation work, we'll use a mock object
        object : ProductRepository {
            // Minimal implementation to satisfy the interface
            // This would be replaced with ProductRepositoryImpl in a real app
        }
    }
    
    // Singleton CartRepository instance
    val cartRepository: CartRepository by lazy {
        CartRepositoryImpl(productRepository)
    }
} 
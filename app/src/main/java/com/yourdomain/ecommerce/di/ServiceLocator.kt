package com.yourdomain.ecommerce.di

import android.content.Context
import com.yourdomain.ecommerce.BuildConfig
import com.yourdomain.ecommerce.core.data.repository.AuthRepository
import com.yourdomain.ecommerce.core.data.repository.CartRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.CustomerRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.OrderRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.PaymentRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.ProductRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.ReturnOrderRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.SellerRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.ShipmentRepositoryImpl
import com.yourdomain.ecommerce.core.data.repository.StorageRepository
import com.yourdomain.ecommerce.core.domain.repository.CartRepository
import com.yourdomain.ecommerce.core.domain.repository.CustomerRepository
import com.yourdomain.ecommerce.core.domain.repository.OrderRepository
import com.yourdomain.ecommerce.core.domain.repository.PaymentRepository
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import com.yourdomain.ecommerce.core.domain.repository.ReturnOrderRepository
import com.yourdomain.ecommerce.core.domain.repository.SellerRepository
import com.yourdomain.ecommerce.core.domain.repository.ShipmentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

/**
 * Service locator pattern implementation to replace Dagger Hilt
 * Provides access to all repositories and services
 */
object ServiceLocator {
    private var appContext: Context? = null
    
    // Fix for emulator connections - replace localhost with 10.0.2.2 for emulator
    private fun getAdjustedSupabaseUrl(url: String): String {
        return if (url.contains("localhost") || url.contains("127.0.0.1")) {
            // Replace localhost with 10.0.2.2 when running in emulator
            url.replace("localhost", "10.0.2.2").replace("127.0.0.1", "10.0.2.2")
        } else {
            url
        }
    }
    
    // Supabase client
    private val supabaseClient: SupabaseClient by lazy {
        val adjustedUrl = getAdjustedSupabaseUrl(BuildConfig.SUPABASE_URL)
        createSupabaseClient(
            supabaseUrl = adjustedUrl,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(GoTrue)
            install(Storage)
            install(Realtime) // Add Realtime support for live updates
        }
    }
    
    // Repositories
    private val authRepository by lazy {
        AuthRepository.getInstance(supabaseClient)
    }
    
    private val productRepository: ProductRepository by lazy {
        ProductRepositoryImpl(supabaseClient)
    }
    
    private val orderRepository: OrderRepository by lazy {
        OrderRepositoryImpl(supabaseClient)
    }
    
    private val cartRepository: CartRepository by lazy {
        CartRepositoryImpl(productRepository)
    }
    
    private val customerRepository: CustomerRepository by lazy {
        CustomerRepositoryImpl(supabaseClient)
    }
    
    private val paymentRepository: PaymentRepository by lazy {
        PaymentRepositoryImpl(supabaseClient)
    }
    
    private val shipmentRepository: ShipmentRepository by lazy {
        ShipmentRepositoryImpl(supabaseClient)
    }
    
    private val returnOrderRepository: ReturnOrderRepository by lazy {
        ReturnOrderRepositoryImpl(supabaseClient)
    }
    
    private val sellerRepository: SellerRepository by lazy {
        SellerRepositoryImpl(supabaseClient)
    }
    
    private val storageRepository by lazy {
        StorageRepository.getInstance(supabaseClient)
    }
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    // Getters for repositories
    fun getAuthRepository() = authRepository
    fun getProductRepository() = productRepository
    fun getOrderRepository() = orderRepository
    fun getCartRepository() = cartRepository
    fun getCustomerRepository() = customerRepository
    fun getPaymentRepository() = paymentRepository
    fun getShipmentRepository() = shipmentRepository
    fun getReturnOrderRepository() = returnOrderRepository
    fun getSellerRepository() = sellerRepository
    fun getStorageRepository() = storageRepository
} 
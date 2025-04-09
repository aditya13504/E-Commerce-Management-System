package com.yourdomain.ecommerce.core.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * App navigation destinations
 */
sealed class AppDestination(
    val route: String,
    val navArguments: List<NamedNavArgument> = emptyList()
) {
    // Auth graph
    object Auth : AppDestination("auth_graph")
    object Login : AppDestination("login")
    object Register : AppDestination("register")
    
    // Main graph
    object Main : AppDestination("main_graph")
    
    // Home tab
    object Home : AppDestination("home")
    
    // Product tab
    object Products : AppDestination("products")
    object ProductDetail : AppDestination(
        route = "product/{productId}",
        navArguments = listOf(
            navArgument("productId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(productId: String) = "product/$productId"
        
        fun getProductId(savedStateHandle: SavedStateHandle): String {
            return checkNotNull(savedStateHandle["productId"])
        }
    }
    
    // Cart & Checkout
    object Cart : AppDestination("cart")
    object Checkout : AppDestination("checkout")
    object OrderConfirmation : AppDestination(
        route = "order_confirmation/{orderId}",
        navArguments = listOf(
            navArgument("orderId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(orderId: String) = "order_confirmation/$orderId"
        
        fun getOrderId(savedStateHandle: SavedStateHandle): String {
            return checkNotNull(savedStateHandle["orderId"])
        }
    }
    
    // Orders tab
    object Orders : AppDestination("orders")
    object OrderDetail : AppDestination(
        route = "order/{orderId}",
        navArguments = listOf(
            navArgument("orderId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(orderId: String) = "order/$orderId"
        
        fun getOrderId(savedStateHandle: SavedStateHandle): String {
            return checkNotNull(savedStateHandle["orderId"])
        }
    }
    
    // Profile tab
    object Profile : AppDestination("profile")
    object EditProfile : AppDestination("edit_profile")
    object AddressList : AppDestination("addresses")
    object EditAddress : AppDestination(
        route = "edit_address/{addressId}",
        navArguments = listOf(
            navArgument("addressId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) {
        fun createRoute(addressId: String? = null) = 
            if (addressId != null) "edit_address/$addressId" else "edit_address/null"
        
        fun getAddressId(savedStateHandle: SavedStateHandle): String? {
            return savedStateHandle["addressId"]
        }
    }
    
    // Bottom Navigation Items
    val bottomNavItems = listOf(Home, Products, Cart, Orders, Profile)
} 
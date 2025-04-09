package com.yourdomain.ecommerce.core.data.model

import java.math.BigDecimal

/**
 * Represents an item in the shopping cart
 */
data class CartItem(
    val id: String,                  // Unique identifier for the cart item
    val productId: String,           // Reference to the product
    val name: String,                // Product name
    val imageUrl: String?,           // Product image URL
    val price: String,               // Product unit price as string for BigDecimal conversion
    val quantity: Int,               // Quantity in cart
    val maxQuantity: Int = 99,       // Maximum allowed quantity (default to 99 or use stock)
    val sellerName: String? = null   // Optional seller information
) 
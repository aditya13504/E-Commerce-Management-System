package com.yourdomain.ecommerce.core.domain.repository

import com.yourdomain.ecommerce.core.data.model.CartItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for shopping cart operations
 */
interface CartRepository {
    
    /**
     * Get all items in the cart
     */
    suspend fun getCartItems(): List<CartItem>
    
    /**
     * Observe cart items for real-time updates
     */
    fun observeCartItems(): Flow<List<CartItem>>
    
    /**
     * Add a product to the cart
     */
    suspend fun addToCart(productId: String, quantity: Int = 1)
    
    /**
     * Update the quantity of an item in the cart
     * @param cartItemId The unique ID of the cart item
     * @param quantityChange The amount to change the quantity by (positive or negative)
     */
    suspend fun updateItemQuantity(cartItemId: String, quantityChange: Int)
    
    /**
     * Remove an item from the cart
     */
    suspend fun removeFromCart(cartItemId: String)
    
    /**
     * Clear all items from the cart
     */
    suspend fun clearCart()
    
    /**
     * Get the total number of items in the cart
     */
    suspend fun getCartItemCount(): Flow<Int>
} 
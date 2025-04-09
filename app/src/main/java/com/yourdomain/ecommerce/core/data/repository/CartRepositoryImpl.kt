package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.CartItem
import com.yourdomain.ecommerce.core.domain.repository.CartRepository
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

/**
 * Implementation of CartRepository that manages shopping cart operations
 * 
 * Note: This is a simplified in-memory implementation for demo purposes.
 * In a real app, this would likely use Room or a remote data source.
 */
class CartRepositoryImpl constructor(
    private val productRepository: ProductRepository
) : CartRepository {

    // In-memory storage of cart items
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    override suspend fun getCartItems(): List<CartItem> {
        return cartItems.value
    }

    override fun observeCartItems(): Flow<List<CartItem>> {
        return cartItems
    }

    override suspend fun addToCart(productId: String, quantity: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Get product details
                val product = productRepository.getProductById(productId)
                
                // Check if product is in stock
                if (product.quantityInStock <= 0) {
                    throw IllegalStateException("Product is out of stock")
                }
                
                // Check if requested quantity is available
                if (quantity > product.quantityInStock) {
                    throw IllegalStateException("Not enough stock available")
                }
                
                // Check if product is already in cart
                val existingItemIndex = _cartItems.value.indexOfFirst { it.productId == productId }
                
                if (existingItemIndex >= 0) {
                    // Update existing item
                    val existingItem = _cartItems.value[existingItemIndex]
                    val newQuantity = existingItem.quantity + quantity
                    
                    // Validate against stock
                    if (newQuantity > product.quantityInStock) {
                        throw IllegalStateException("Cannot add more items than available in stock")
                    }
                    
                    // Create updated list
                    val updatedItems = _cartItems.value.toMutableList()
                    updatedItems[existingItemIndex] = existingItem.copy(quantity = newQuantity)
                    _cartItems.value = updatedItems
                } else {
                    // Add new item
                    val cartItem = CartItem(
                        id = UUID.randomUUID().toString(),
                        productId = product.productId,
                        name = product.name,
                        imageUrl = product.imageUrl,
                        price = product.price.toString(),
                        quantity = quantity,
                        maxQuantity = product.quantityInStock,
                        sellerName = product.sellerName
                    )
                    
                    _cartItems.value = _cartItems.value + cartItem
                }
                
                Timber.d("Added product $productId to cart, quantity: $quantity")
            } catch (e: Exception) {
                Timber.e(e, "Error adding product $productId to cart")
                throw e
            }
        }
    }

    override suspend fun updateItemQuantity(cartItemId: String, quantityChange: Int) {
        withContext(Dispatchers.IO) {
            try {
                val item = _cartItems.value.find { it.id == cartItemId }
                    ?: throw IllegalArgumentException("Cart item not found")
                
                val newQuantity = item.quantity + quantityChange
                
                // Validate quantity
                if (newQuantity <= 0) {
                    throw IllegalArgumentException("Quantity must be positive")
                }
                
                // Check against max quantity (stock)
                if (newQuantity > item.maxQuantity) {
                    throw IllegalStateException("Cannot exceed available stock quantity")
                }
                
                // Update the item
                val updatedItems = _cartItems.value.map { 
                    if (it.id == cartItemId) it.copy(quantity = newQuantity) else it 
                }
                
                _cartItems.value = updatedItems
                
                Timber.d("Updated cart item $cartItemId quantity to $newQuantity")
            } catch (e: Exception) {
                Timber.e(e, "Error updating cart item $cartItemId quantity")
                throw e
            }
        }
    }

    override suspend fun removeFromCart(cartItemId: String) {
        withContext(Dispatchers.IO) {
            try {
                _cartItems.value = _cartItems.value.filter { it.id != cartItemId }
                Timber.d("Removed item $cartItemId from cart")
            } catch (e: Exception) {
                Timber.e(e, "Error removing item $cartItemId from cart")
                throw e
            }
        }
    }

    override suspend fun clearCart() {
        withContext(Dispatchers.IO) {
            try {
                _cartItems.value = emptyList()
                Timber.d("Cart cleared")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing cart")
                throw e
            }
        }
    }

    override suspend fun getCartItemCount(): Flow<Int> {
        return cartItems.map { items -> items.sumOf { it.quantity } }
    }
} 
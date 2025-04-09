package com.yourdomain.ecommerce.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yourdomain.ecommerce.core.data.model.Product // Needed if we store full product later
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Represents an item in the cart
data class CartItem( 
    val productId: Int,
    var quantity: Int,
    val price: Double, // Price per item
    val name: String? = null // Add product name
    // Optional: Add product details like name, price if needed directly in cart state
    // val name: String? = null, 
    // val price: Double? = null
)

// Represents the state of the shopping cart screen
data class CartUiState(
    val items: Map<Int, CartItem> = emptyMap(), // Map<ProductId, CartItem>
    val totalAmount: Double = 0.0
)

class CartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun addToCart(product: Product) {
        val productId = product.productid ?: return
        val price = product.price ?: 0.0
        val name = product.name // Get name

        _uiState.update { currentState ->
            val currentItems = currentState.items.toMutableMap()
            val existingItem = currentItems[productId]

            if (existingItem != null) {
                currentItems[productId] = existingItem.copy(quantity = existingItem.quantity + 1)
                // Name & Price remain the same as when first added
            } else {
                // Add new item with its price and name
                currentItems[productId] = CartItem(productId = productId, quantity = 1, price = price, name = name)
            }

            val newTotal = calculateTotal(currentItems)
            currentState.copy(items = currentItems, totalAmount = newTotal)
        }
    }

    // Recalculate total based on prices stored in CartItems
    private fun calculateTotal(items: Map<Int, CartItem>): Double {
        var total = 0.0
        items.values.forEach { item ->
            total += item.quantity * item.price
        }
        return total
    }

    fun removeFromCart(productId: Int) {
        updateQuantity(productId, 0) // Remove by setting quantity to 0
    }

    fun increaseQuantity(productId: Int) {
        _uiState.value.items[productId]?.let { currentItem ->
            updateQuantity(productId, currentItem.quantity + 1)
        }
    }

    fun decreaseQuantity(productId: Int) {
        _uiState.value.items[productId]?.let { currentItem ->
            if (currentItem.quantity > 0) { // Prevent going below 0, though updateQuantity handles removal
                 updateQuantity(productId, currentItem.quantity - 1)
            }
        }
    }

    // Core function to handle quantity changes and removal
    private fun updateQuantity(productId: Int, newQuantity: Int) {
         _uiState.update { currentState ->
            val currentItems = currentState.items.toMutableMap()
            val item = currentItems[productId]

            if (item != null) {
                if (newQuantity > 0) {
                    currentItems[productId] = item.copy(quantity = newQuantity)
                } else {
                    // Remove item if quantity is 0 or less
                    currentItems.remove(productId)
                }
            } // If item is null, do nothing (shouldn't happen with increase/decrease calls)
            
            val newTotal = calculateTotal(currentItems)
            currentState.copy(items = currentItems, totalAmount = newTotal)
        }
    }

    fun clearCart() {
        _uiState.value = CartUiState()
    }
}

class Factory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartViewModel::class.java)) {
            return CartViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
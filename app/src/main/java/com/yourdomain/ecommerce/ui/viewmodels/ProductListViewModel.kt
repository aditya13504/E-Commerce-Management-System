package com.yourdomain.ecommerce.ui.viewmodels // Or a suitable package like presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

// Represents the state of the product list screen
data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState(isLoading = true))
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val productRepository = ProductRepository // Accessing the singleton object

    init {
        fetchProducts()
    }

    fun fetchProducts() {
        _uiState.update { it.copy(isLoading = true, error = null) } // Clear error on reload
        viewModelScope.launch {
            val result = productRepository.getAllProducts()
            result.onSuccess { productsList ->
                _uiState.update { currentState -> currentState.copy(isLoading = false, products = productsList) }
            }.onFailure {
                _uiState.update { currentState -> currentState.copy(isLoading = false, error = it.message ?: "Failed to load products") }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

class Factory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductListViewModel::class.java)) {
            return ProductListViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 
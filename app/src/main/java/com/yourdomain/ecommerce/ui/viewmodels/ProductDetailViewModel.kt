package com.yourdomain.ecommerce.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProductDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState(isLoading = true))
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val productRepository = ProductRepository

    fun loadProductDetails(productId: Int) {
        _uiState.update { it.copy(isLoading = true, error = null) } // Clear error
        viewModelScope.launch {
            val result = productRepository.getProductById(productId)
            result.onSuccess { product ->
                if (product != null) {
                    _uiState.update { it.copy(isLoading = false, product = product) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Product not found") }
                }
            }.onFailure {
                 _uiState.update { currentState -> currentState.copy(isLoading = false, error = it.message ?: "Failed to load product details") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductDetailViewModel::class.java)) {
                // Assuming ProductDetailViewModel needs ProductRepository
                // You might need to adjust this based on actual dependencies
                return ProductDetailViewModel(ServiceLocator.getProductRepository()) as T 
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
package com.yourdomain.ecommerce.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import com.yourdomain.ecommerce.di.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Products screen
 */
class ProductsViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    // Factory for creating ProductsViewModel
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductsViewModel::class.java)) {
                return ProductsViewModel(ServiceLocator.getProductRepository()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadProducts()
    }

    /**
     * Load products from repository
     */
    fun loadProducts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val products = productRepository.getAllProducts(
                    page = 1,
                    pageSize = 20,
                    sortField = "name",
                    sortDirection = "asc"
                )
                
                _uiState.update { 
                    it.copy(
                        allProducts = products,
                        filteredProducts = products,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                
                Timber.e(e, "Error loading products")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load products"
                    )
                }
            }
        }
    }

    /**
     * Refresh products (for pull-to-refresh)
     */
    fun refreshProducts() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                
                val products = productRepository.getAllProducts(
                    page = 1,
                    pageSize = 20,
                    sortField = "name",
                    sortDirection = "asc"
                )
                
                _uiState.update {
                    it.copy(
                        allProducts = products,
                        filteredProducts = if (it.searchActive) filterProducts(products, _searchQuery.value) else products,
                        error = null
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                
                Timber.e(e, "Error refreshing products")
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to refresh products") 
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Update search query and trigger search
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        // Update search active state
        _uiState.update { it.copy(searchActive = query.isNotEmpty()) }
        
        // Cancel any ongoing search job
        searchJob?.cancel()
        
        // If query is empty, reset to show all products
        if (query.isEmpty()) {
            _uiState.update { it.copy(filteredProducts = it.allProducts) }
            return
        }
        
        // Debounce search to avoid excessive repository calls
        searchJob = viewModelScope.launch {
            delay(300) // Debounce timeout
            performSearch(query)
        }
    }

    /**
     * Perform search operation
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSearching = true) }
                
                // Option 1: Use repository search if available
                val searchResults = productRepository.searchProducts(query)
                
                _uiState.update { 
                    it.copy(
                        filteredProducts = searchResults,
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                
                Timber.e(e, "Error searching products")
                _uiState.update { 
                    it.copy(
                        isSearching = false,
                        error = e.message ?: "Failed to search products"
                    )
                }
            }
        }
    }

    /**
     * Filter products locally based on search query
     */
    private fun filterProducts(products: List<Product>, query: String): List<Product> {
        if (query.isEmpty()) return products
        
        val lowerCaseQuery = query.lowercase()
        return products.filter {
            it.name.lowercase().contains(lowerCaseQuery) ||
            it.description?.lowercase()?.contains(lowerCaseQuery) == true ||
            it.category?.lowercase()?.contains(lowerCaseQuery) == true
        }
    }

    /**
     * Clear search and reset to show all products
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.update { 
            it.copy(
                filteredProducts = it.allProducts,
                searchActive = false
            )
        }
    }

    /**
     * Get products with low stock for highlighting
     */
    fun getProductsWithLowStock(threshold: Int = 5) {
        viewModelScope.launch {
            try {
                val lowStockProducts = productRepository.getProductsWithLowStock(threshold)
                _uiState.update { it.copy(lowStockProducts = lowStockProducts.map { product -> product.productId }) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error getting low stock products")
            }
        }
    }

    /**
     * Handle error shown to user
     */
    fun errorShown() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Products screen
 */
data class ProductsUiState(
    val allProducts: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val lowStockProducts: List<String> = emptyList(), // List of product IDs with low stock
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchActive: Boolean = false,
    val error: String? = null
) 
package com.yourdomain.ecommerce.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.model.OrderTable
import com.yourdomain.ecommerce.core.data.repository.CustomerRepository
import com.yourdomain.ecommerce.core.data.repository.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

data class OrderHistoryUiState(
    val orders: List<OrderTable> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class OrderHistoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState(isLoading = true))
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    private val orderRepository = OrderRepository
    private val customerRepository = CustomerRepository
    // We need AuthViewModel or similar to get the current authUserId
    // For simplicity, we might pass authUserId directly, but ideally observe it

    fun loadOrderHistory(authUserId: String?) {
        if (authUserId == null) {
            _uiState.value = OrderHistoryUiState(error = "User not logged in")
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // 1. Get internal customer ID
            val customerIdResult = customerRepository.getCustomerIdForAuthUser(authUserId)
            val customerId = customerIdResult.getOrNull()

            if (customerId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Could not find customer profile.") }
                return@launch
            }

            // 2. Fetch orders using internal customer ID
            val ordersResult = orderRepository.getOrdersByCustomerId(customerId)
            ordersResult.onSuccess {
                _uiState.update { it.copy(isLoading = false, orders = it) }
            }.onFailure { orderError ->
                 _uiState.update { it.copy(isLoading = false, error = orderError.message ?: "Failed to load order history") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderHistoryViewModel::class.java)) {
                // Assuming OrderHistoryViewModel needs OrderRepository and AuthRepository
                return OrderHistoryViewModel(
                    ServiceLocator.getOrderRepository(),
                    ServiceLocator.getAuthRepository()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
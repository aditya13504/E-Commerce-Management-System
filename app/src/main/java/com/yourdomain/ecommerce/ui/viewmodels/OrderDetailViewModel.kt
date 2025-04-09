package com.yourdomain.ecommerce.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.model.OrderTable
import com.yourdomain.ecommerce.core.data.model.OrderItem
import com.yourdomain.ecommerce.core.data.repository.OrderRepository
import com.yourdomain.ecommerce.core.data.repository.ProductRepository // Needed to get product names
import com.yourdomain.ecommerce.core.data.model.ReturnOrder
import com.yourdomain.ecommerce.core.data.repository.ReturnRepository // Import ReturnRepository
import com.yourdomain.ecommerce.core.data.model.Shipment // Import Shipment
import com.yourdomain.ecommerce.core.data.repository.ShipmentRepository // Import Shipment repo
import com.yourdomain.ecommerce.core.util.DateTimeUtil // Add DateTimeUtil import
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// import java.time.Instant // Remove this import
import kotlinx.coroutines.flow.update // Import update
import kotlinx.coroutines.delay // Import delay
import androidx.lifecycle.ViewModelProvider

// Enhanced OrderItem with product name for display
data class OrderDetailItem( 
    val item: OrderItem,
    val productName: String?,
    var returnStatus: String? = null // Add return status
)

data class OrderDetailUiState(
    val order: OrderTable? = null,
    val items: List<OrderDetailItem> = emptyList(),
    val shipment: Shipment? = null, // Add shipment state
    val isLoading: Boolean = false,
    val error: String? = null,
    val returnRequestInProgress: Set<Int> = emptySet(), // Track productIds being returned
    val returnRequestError: String? = null // Separate error for return request
)

class OrderDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState(isLoading = true))
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val orderRepository = OrderRepository
    private val productRepository = ProductRepository // Inject or get instance
    private val returnRepository = ReturnRepository // Get instance
    private val shipmentRepository = ShipmentRepository // Get instance

    // Note: Ideally, fetch the specific OrderTable as well, not just items
    // This assumes the OrderTable object might be passed via navigation or fetched separately
    fun loadOrderDetails(orderId: Int) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // Fetch items and shipment concurrently
            val itemsDeferred = async { orderRepository.getOrderItemsByOrderId(orderId) }
            val shipmentDeferred = async { shipmentRepository.getShipmentByOrderId(orderId) }
            val orderDeferred = async { orderRepository.getOrderById(orderId) } // Fetch order details

            val itemsResult = itemsDeferred.await()
            val shipmentResult = shipmentDeferred.await()
            val orderResult = orderDeferred.await() // Await order details

            val shipment = shipmentResult.getOrNull() // Get shipment or null
            val order = orderResult.getOrNull() // Get order or null

            if (itemsResult.isFailure || orderResult.isFailure) { // Check both results
                 _uiState.update { it.copy(isLoading = false, error = itemsResult.exceptionOrNull()?.message ?: orderResult.exceptionOrNull()?.message ?: "Failed to load order details") }
                 return@launch
            }

            val orderItems = itemsResult.getOrNull() ?: emptyList()
            
            // Fetch product names and return status concurrently
            val detailedItemsDeferred = orderItems.map { item ->
                async { 
                    val productNameDeferred = async { 
                        item.productid?.let { productRepository.getProductById(it).getOrNull()?.name } ?: "Unknown"
                    }
                    val returnStatusDeferred = async { 
                        if(item.orderid != null && item.productid != null) {
                             returnRepository.getReturnStatusForItem(item.orderid, item.productid).getOrNull()?.returnstatus
                        } else null
                    }
                    OrderDetailItem(
                        item = item, 
                        productName = productNameDeferred.await(),
                        returnStatus = returnStatusDeferred.await()
                    )
                }
            }
            val detailedItems = detailedItemsDeferred.awaitAll()
            
            // TODO: Fetch the actual OrderTable object for this orderId to display date/total
            // For now, creating a placeholder OrderTable
             // val placeholderOrder = OrderTable(orderid = orderId)
            
            _uiState.update { it.copy(
                isLoading = false,
                order = order, // Use fetched order
                items = detailedItems,
                shipment = shipment // Set shipment state
            ) }
        }
    }

    fun requestReturnForItem(orderId: Int?, productId: Int?) {
        if (orderId == null || productId == null) {
             println("Cannot request return without orderId and productId")
            // TODO: Update UI state with error
            _uiState.update { it.copy(returnRequestError = "Missing Order ID or Product ID for return.") }
            return
        }
        
        // Indicate processing for this specific item
         _uiState.update { it.copy(returnRequestInProgress = it.returnRequestInProgress + productId, returnRequestError = null) }
        
        viewModelScope.launch {
            val returnRequest = ReturnOrder(
                orderid = orderId,
                productid = productId,
                returndate = DateTimeUtil.getCurrentTimestampString(),
                returnreason = "Requested via app", // Default reason
                returnstatus = "PENDING" // Default status
            )
            val result = returnRepository.requestReturn(returnRequest)
            
            result.onSuccess { createdReturn ->
                val returnId = createdReturn.returnid
                println("Return requested successfully for product $productId in order $orderId with return ID $returnId")
                
                // Update UI immediately to PENDING
                 _uiState.update { currentState ->
                    val updatedItems = currentState.items.map {
                        if (it.item.productid == productId) it.copy(returnStatus = "PENDING") else it
                    }
                    currentState.copy(
                        items = updatedItems, 
                        returnRequestInProgress = currentState.returnRequestInProgress - productId
                    )
                }
                
                // Schedule the status update if we got a return ID
                if (returnId != null) {
                    viewModelScope.launch {
                        try {
                            delay(3 * 60 * 1000) // 3 minutes delay
                             // Simulate approval and refund status update
                            val updateResult = returnRepository.updateReturnStatus(returnId, "APPROVED") // Or REFUNDED directly if needed
                            if (updateResult.isSuccess) {
                                 println("Return $returnId auto-updated to APPROVED")
                                 // Update local state to APPROVED first
                                 _uiState.update { currentState ->
                                    val updatedItems = currentState.items.map {
                                        if (it.item.productid == productId) it.copy(returnStatus = "APPROVED") else it 
                                    }
                                    currentState.copy(items = updatedItems)
                                 }
                                 
                                 // Simulate refund status update after another short delay
                                 delay(1000) // Short delay for refund simulation
                                 val refundResult = returnRepository.updateReturnStatus(returnId, "REFUNDED")
                                 if (refundResult.isSuccess) {
                                    println("Return $returnId simulated refund update to REFUNDED")
                                    // Update UI to REFUNDED
                                    _uiState.update { currentState ->
                                        val updatedItems = currentState.items.map {
                                            if (it.item.productid == productId) it.copy(returnStatus = "REFUNDED") else it
                                        }
                                        currentState.copy(items = updatedItems)
                                    }
                                 } else {
                                    println("Failed to simulate refund update for $returnId")
                                 }
                                 
                            } else {
                                 println("Failed to auto-update return $returnId status to APPROVED")
                            }
                            // TODO: Add separate logic/call to simulate marking payment as refunded if necessary
                        } catch (e: Exception) {
                             println("Error during delayed return update: ${e.message}")
                        }
                    }
                }
            }.onFailure { returnError ->
                println("Failed to request return: ${returnError.message}")
                 _uiState.update { 
                     it.copy(
                         returnRequestError = returnError.message ?: "Failed to request return",
                         returnRequestInProgress = it.returnRequestInProgress - productId
                    )
                 }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearReturnError() {
         _uiState.update { it.copy(returnRequestError = null) }
    }

    // Add this factory class inside OrderDetailViewModel
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderDetailViewModel::class.java)) {
                // Assuming OrderDetailViewModel needs multiple repositories
                return OrderDetailViewModel(
                    ServiceLocator.getOrderRepository(),
                    ServiceLocator.getProductRepository(),
                    ServiceLocator.getReturnOrderRepository(),
                    ServiceLocator.getShipmentRepository()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
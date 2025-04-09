package com.yourdomain.ecommerce.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.model.OrderTable
import com.yourdomain.ecommerce.core.data.model.OrderItem
import com.yourdomain.ecommerce.core.data.model.Payment
import com.yourdomain.ecommerce.core.data.repository.OrderRepository
import com.yourdomain.ecommerce.core.data.repository.CustomerRepository
import com.yourdomain.ecommerce.core.data.repository.ProductRepository
import com.yourdomain.ecommerce.core.data.repository.ShipmentRepository
import com.yourdomain.ecommerce.core.data.model.Shipment
import com.yourdomain.ecommerce.core.data.repository.CustomerRepositoryImpl
import com.yourdomain.ecommerce.core.util.DateTimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant // For setting dates
import kotlinx.coroutines.async // For potential parallel stock updates
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay // Import delay
import java.util.UUID // For unique tracking numbers
import androidx.lifecycle.ViewModelProvider

// Represents the state of the checkout/order process
enum class OrderStatus {
    IDLE,       // Initial state
    PROCESSING, // Order creation/payment in progress
    SUCCESS,    // Order successfully placed and paid
    FAILURE     // Error occurred
}

data class OrderUiState(
    val status: OrderStatus = OrderStatus.IDLE,
    val error: String? = null,
    val createdOrderId: Int? = null // ID of the created order on success
)

class OrderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    private val orderRepository = OrderRepository
    private val customerRepository = CustomerRepositoryImpl
    private val productRepository = ProductRepository
    private val shipmentRepository = ShipmentRepository

    fun placeOrder(cartState: CartUiState, authUserId: String?) {
        if (authUserId == null) {
            _uiState.value = OrderUiState(status = OrderStatus.FAILURE, error = "User not logged in")
            return
        }
         if (cartState.items.isEmpty()) {
            _uiState.value = OrderUiState(status = OrderStatus.FAILURE, error = "Cart is empty")
            return
        }
        
        _uiState.update { it.copy(status = OrderStatus.PROCESSING, error = null) }

        viewModelScope.launch {
            // Store fetched products to get stock later
            val productDetailsMap = mutableMapOf<Int, Product>() 
            
            // 0. Check Stock Levels First (and store product details)
            val stockCheckFailedItems = mutableListOf<String>()
            for (item in cartState.items.values) {
                val productResult = productRepository.getProductById(item.productId)
                val product = productResult.getOrNull()
                if (product != null) {
                    productDetailsMap[item.productId] = product // Store product details
                    if ((product.stock ?: 0) < item.quantity) {
                        stockCheckFailedItems.add("${item.name ?: "Product ID ${item.productId}"}: Only ${product.stock ?: 0} available, requested ${item.quantity}.")
                    }
                } else {
                    stockCheckFailedItems.add("Product ID ${item.productId} not found.")
                }
                // Optional: Add delay here if making many requests to avoid rate limiting
            }

            if (stockCheckFailedItems.isNotEmpty()) {
                _uiState.update { it.copy(status = OrderStatus.FAILURE, error = "Stock check failed: ${stockCheckFailedItems.joinToString()}") }
                return@launch
            }

            // 1. Get internal customer ID from Auth ID
            val customerIdResult = customerRepository.getCustomerIdForAuthUser(authUserId)
            val customerIdInt = customerIdResult.getOrNull()

            if (customerIdInt == null) {
                _uiState.update { it.copy(status = OrderStatus.FAILURE, error = "Could not find customer profile. (${customerIdResult.exceptionOrNull()?.message})" ) }
                return@launch
            }

            // 2. Prepare OrderTable object
            val order = OrderTable(
                customerid = customerIdInt, // Use fetched internal ID
                orderdate = DateTimeUtil.getCurrentTimestampString(),
                totalamount = cartState.totalAmount
            )

            // 3. Prepare List<OrderItem>
            val orderItems = cartState.items.values.map {
                OrderItem(
                    // orderitemid is auto-generated
                    orderid = null, // Will be set by repository/DB relationship
                    productid = it.productId,
                    quantity = it.quantity,
                    itemprice = it.price // Use the price stored in CartItem
                )
            }
            
            // 4. Create Order and Items in DB
            val orderResult = orderRepository.createOrder(order, orderItems)
            
            orderResult.onSuccess { createdOrder ->
                val orderId = createdOrder.orderid ?: return@onSuccess
                if (orderId == null) {
                     _uiState.update { it.copy(status = OrderStatus.FAILURE, error = "Failed to get created order ID") }
                    return@onSuccess
                }
                
                // 4. Simulate Payment (create payment record)
                val payment = Payment(
                    orderid = orderId,
                    amountpaid = createdOrder.totalamount, // Use total from created order
                    paymentdate = DateTimeUtil.getCurrentTimestampString()
                    // paymentmethod defaults to "cash" in data class
                )
                val paymentResult = orderRepository.recordPayment(payment)
                
                paymentResult.onSuccess {
                    // 6. ** Attempt to Decrement Stock **
                    val stockUpdateJobs = orderItems.map { orderItem ->
                        async { // Launch updates potentially in parallel
                            val product = productDetailsMap[orderItem.productid]
                            val productId = orderItem.productid // Get productId safely
                            if (product != null && productId != null && orderItem.quantity != null) { // Check productId != null
                                val currentStock = product.stock ?: 0
                                val newStock = currentStock - orderItem.quantity
                                productRepository.updateStock(productId, maxOf(0, newStock)) // Pass unwrapped productId
                            } else {
                                Result.failure<Unit>(Exception("Product details or ID not found for stock update: ID ${orderItem.productid}"))
                            }
                        }
                    }
                    val stockUpdateResults = stockUpdateJobs.awaitAll()
                    val failedStockUpdates = stockUpdateResults.filter { it.isFailure }
                    
                    if (failedStockUpdates.isNotEmpty()) {
                        // Log error, but proceed with success state for the order/payment itself
                         println("WARNING: Order placed and paid, but failed to update stock for some items: ${failedStockUpdates.map { it.exceptionOrNull()?.message }}")
                         _uiState.update { it.copy(status = OrderStatus.SUCCESS, createdOrderId = orderId, error = "Stock update failed for some items.") }
                    } else {
                        // ** SUCCESS **
                        _uiState.update { it.copy(status = OrderStatus.SUCCESS, createdOrderId = orderId, error = null) }
                    }

                    // 7. Create Initial Shipment Record with Unique Tracking & Schedule Update
                    val uniqueTrackingNumber = "SIM-${UUID.randomUUID().toString().take(10).uppercase()}"
                    val initialShipment = Shipment(
                        orderid = orderId,
                        shipmentdate = DateTimeUtil.getCurrentTimestampString(),
                        deliverystatus = "PENDING",
                        trackingnumber = uniqueTrackingNumber // Assign unique number
                    )
                    val shipmentResult = shipmentRepository.createShipment(initialShipment)
                    
                    if (shipmentResult.isFailure) {
                         println("WARNING: Order placed/paid, but failed to create shipment record: ${shipmentResult.exceptionOrNull()?.message}")
                         // Update state to reflect partial success with shipment error
                         val finalError = (if(failedStockUpdates.isNotEmpty()) "Stock update failed. " else "") + "Shipment creation failed."
                         _uiState.update { it.copy(status = OrderStatus.SUCCESS, createdOrderId = orderId, error = finalError) }
                    } else {
                        // Shipment created, now schedule the status update
                        val createdShipment = shipmentResult.getOrNull()
                        if (createdShipment?.shipmentid != null) {
                            viewModelScope.launch { // Launch separate job for delayed update
                                try {
                                    delay(3 * 60 * 1000) // 3 minutes delay
                                    val updateResult = shipmentRepository.updateShipmentStatus(createdShipment.shipmentid, "DELIVERED")
                                    if (updateResult.isFailure) {
                                        println("Failed to auto-update shipment ${createdShipment.shipmentid} to DELIVERED")
                                    } else {
                                        println("Shipment ${createdShipment.shipmentid} auto-updated to DELIVERED")
                                    }
                                } catch (e: Exception) {
                                    // Handle potential exceptions during delay/update
                                     println("Error during delayed shipment update: ${e.message}")
                                }
                            }
                        }
                        // Update UI state (success/partial success)
                        if (failedStockUpdates.isNotEmpty()) {
                            _uiState.update { it.copy(status = OrderStatus.SUCCESS, createdOrderId = orderId, error = "Stock update failed for some items.") }
                        } else {
                             _uiState.update { it.copy(status = OrderStatus.SUCCESS, createdOrderId = orderId, error = null) } // Full success
                        }
                    }

                }.onFailure { paymentError ->
                     _uiState.update { it.copy(status = OrderStatus.FAILURE, error = "Order placed, but payment failed: ${paymentError.message}") }
                }
                
            }.onFailure { orderError ->
                _uiState.update { it.copy(status = OrderStatus.FAILURE, error = "Failed to place order: ${orderError.message}") }
            }
        }
    }
    
    fun resetOrderState() {
        _uiState.value = OrderUiState()
    }

    fun clearError() {
         _uiState.update { it.copy(error = null) }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OrderViewModel::class.java)) {
                // Assuming OrderViewModel needs multiple repositories
                return OrderViewModel(
                    ServiceLocator.getOrderRepository(),
                    ServiceLocator.getCustomerRepository(),
                    ServiceLocator.getProductRepository(),
                    ServiceLocator.getShipmentRepository()
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
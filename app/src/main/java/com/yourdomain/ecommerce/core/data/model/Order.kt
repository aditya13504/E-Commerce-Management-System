package com.yourdomain.ecommerce.core.data.model

import com.yourdomain.ecommerce.core.data.model.OrderItem
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Represents an order in the system
 */
data class Order(
    val orderId: String,
    val customerId: String,
    val orderDate: LocalDateTime,
    val status: OrderStatus,
    val total: BigDecimal,
    val items: List<OrderItem> = emptyList(),
    val shippingAddress: String? = null,
    val trackingNumber: String? = null,
    val paymentMethod: String? = null
)

/**
 * Possible order statuses
 */
enum class OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURNED;
    
    fun getDisplayName(): String {
        return when (this) {
            PENDING -> "Pending"
            PROCESSING -> "Processing"
            SHIPPED -> "Shipped"
            DELIVERED -> "Delivered"
            CANCELLED -> "Cancelled"
            RETURNED -> "Returned"
        }
    }
    
    fun getColor(): Long {
        return when (this) {
            PENDING -> 0xFFFFA000     // Amber
            PROCESSING -> 0xFF2196F3  // Blue
            SHIPPED -> 0xFF9C27B0     // Purple
            DELIVERED -> 0xFF4CAF50   // Green
            CANCELLED -> 0xFFF44336   // Red
            RETURNED -> 0xFF607D8B    // Blue Grey
        }
    }
} 
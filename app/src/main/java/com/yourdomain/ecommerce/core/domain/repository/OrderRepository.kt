package com.yourdomain.ecommerce.core.domain.repository

import com.yourdomain.ecommerce.core.data.model.Order
import com.yourdomain.ecommerce.core.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for order operations
 */
interface OrderRepository {
    
    /**
     * Get all orders for a customer with pagination
     */
    suspend fun getOrdersByCustomer(
        customerId: String,
        page: Int,
        pageSize: Int
    ): List<Order>
    
    /**
     * Get order by ID
     */
    suspend fun getOrderById(orderId: String): Order
    
    /**
     * Get orders by status
     */
    suspend fun getOrdersByStatus(status: OrderStatus): List<Order>
    
    /**
     * Get orders within a date range
     */
    suspend fun getOrdersByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Order>
    
    /**
     * Create a new order
     */
    suspend fun createOrder(order: Order): Order
    
    /**
     * Update order status
     */
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus)
    
    /**
     * Cancel an order
     */
    suspend fun cancelOrder(orderId: String)
    
    /**
     * Get order count by customer
     */
    suspend fun getOrderCountByCustomer(customerId: String): Int
    
    /**
     * Observe recent orders for a customer
     */
    fun observeRecentOrders(customerId: String, limit: Int): Flow<List<Order>>
} 
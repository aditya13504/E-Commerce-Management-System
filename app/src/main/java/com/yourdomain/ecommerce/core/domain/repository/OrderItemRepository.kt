package com.yourdomain.ecommerce.core.domain.repository

import com.yourdomain.ecommerce.core.data.model.OrderItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for OrderItem entity operations
 * Following clean architecture principles
 */
interface OrderItemRepository {
    /**
     * Get all order items
     * @return Flow of list of all order items
     */
    fun getAllOrderItems(): Flow<List<OrderItem>>
    
    /**
     * Get an order item by ID
     * @param orderItemId The ID of the order item to retrieve
     * @return The order item if found, null otherwise
     */
    suspend fun getOrderItemById(orderItemId: String): OrderItem?
    
    /**
     * Get order items by order ID
     * @param orderId The ID of the order
     * @return List of order items for the order
     */
    suspend fun getOrderItemsByOrderId(orderId: String): List<OrderItem>
    
    /**
     * Get order items by product ID
     * @param productId The ID of the product
     * @return List of order items containing the product
     */
    suspend fun getOrderItemsByProductId(productId: String): List<OrderItem>
    
    /**
     * Create a new order item
     * @param orderItem The order item to create
     * @return Result containing the created order item on success, or exception on failure
     */
    suspend fun createOrderItem(orderItem: OrderItem): Result<OrderItem>
    
    /**
     * Create multiple order items in a batch
     * @param orderItems The list of order items to create
     * @return Result containing the list of created order items on success, or exception on failure
     */
    suspend fun createOrderItems(orderItems: List<OrderItem>): Result<List<OrderItem>>
    
    /**
     * Update an existing order item
     * @param orderItemId The ID of the order item to update
     * @param updates Map of fields to update with their new values
     * @return Result containing the updated order item on success, or exception on failure
     */
    suspend fun updateOrderItem(orderItemId: String, updates: Map<String, Any>): Result<OrderItem>
    
    /**
     * Delete an order item
     * @param orderItemId The ID of the order item to delete
     * @return Result containing true on success, or exception on failure
     */
    suspend fun deleteOrderItem(orderItemId: String): Result<Boolean>
    
    /**
     * Delete all order items for an order
     * @param orderId The ID of the order
     * @return Result containing true on success, or exception on failure
     */
    suspend fun deleteOrderItemsByOrderId(orderId: String): Result<Boolean>
    
    /**
     * Calculate total amount for an order
     * @param orderId The ID of the order
     * @return The total amount for the order's items
     */
    suspend fun calculateOrderTotal(orderId: String): Double
} 
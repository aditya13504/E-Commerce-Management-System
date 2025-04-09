package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.OrderItem
import com.yourdomain.ecommerce.core.domain.repository.OrderItemRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.ReturnFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Implementation of OrderItemRepository that uses Supabase as data source
 */
class OrderItemRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : OrderItemRepository {

    private val tableName = "order_items"
    private val tag = "OrderItemRepository"

    override fun getAllOrderItems(): Flow<List<OrderItem>> = flow {
        try {
            val orderItems = supabaseClient
                .from(tableName)
                .select {
                    Order.asc("order_id")
                }
                .decodeList<OrderItem>()
            emit(orderItems)
        } catch (e: Exception) {
            Log.e(tag, "Error getting all order items: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getOrderItemById(orderItemId: String): OrderItem? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("order_item_id", orderItemId)
                    }
                }
                .decodeSingleOrNull<OrderItem>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting order item with ID $orderItemId: ${e.message}", e)
            null
        }
    }

    override suspend fun getOrderItemsByOrderId(orderId: String): List<OrderItem> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("order_id", orderId)
                    }
                }
                .decodeList<OrderItem>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting order items for order $orderId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getOrderItemsByProductId(productId: String): List<OrderItem> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("product_id", productId)
                    }
                }
                .decodeList<OrderItem>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting order items for product $productId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun createOrderItem(orderItem: OrderItem): Result<OrderItem> {
        return try {
            val createdOrderItem = supabaseClient
                .from(tableName)
                .insert(orderItem, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<OrderItem>()
                
            Result.success(createdOrderItem)
        } catch (e: Exception) {
            Log.e(tag, "Error creating order item: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun createOrderItems(orderItems: List<OrderItem>): Result<List<OrderItem>> {
        return try {
            val createdOrderItems = supabaseClient
                .from(tableName)
                .insert(orderItems, returning = ReturnFormat.REPRESENTATION)
                .decodeList<OrderItem>()
                
            Result.success(createdOrderItems)
        } catch (e: Exception) {
            Log.e(tag, "Error creating multiple order items: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateOrderItem(orderItemId: String, updates: Map<String, Any>): Result<OrderItem> {
        return try {
            // Build the JSON object for the update
            val updateData = buildJsonObject {
                updates.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> {
                            // Convert other types to string if necessary
                            put(key, value.toString())
                        }
                    }
                }
                
                // Add updated_at timestamp
                put("updated_at", Instant.now().toString())
            }
            
            val updatedOrderItem = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("order_item_id", orderItemId)
                    }
                }
                .decodeSingle<OrderItem>()
                
            Result.success(updatedOrderItem)
        } catch (e: Exception) {
            Log.e(tag, "Error updating order item with ID $orderItemId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteOrderItem(orderItemId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("order_item_id", orderItemId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting order item with ID $orderItemId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteOrderItemsByOrderId(orderId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("order_id", orderId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting order items for order $orderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun calculateOrderTotal(orderId: String): Double {
        return try {
            val orderItems = getOrderItemsByOrderId(orderId)
            if (orderItems.isEmpty()) {
                return 0.0
            }
            
            // Calculate the total from item prices * quantities
            orderItems.sumOf { it.itemPrice * it.quantity }
        } catch (e: Exception) {
            Log.e(tag, "Error calculating total for order $orderId: ${e.message}", e)
            0.0
        }
    }
} 
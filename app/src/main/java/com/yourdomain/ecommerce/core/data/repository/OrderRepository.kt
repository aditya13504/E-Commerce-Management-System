package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.OrderTable
import com.yourdomain.ecommerce.core.data.model.OrderItem
import com.yourdomain.ecommerce.core.data.model.Payment
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OrderRepository {

    private val client = SupabaseClientManager.client
    private const val ORDER_TABLE = "order_table" // Match Supabase table name
    private const val ORDER_ITEM_TABLE = "orderitem"
    private const val PAYMENT_TABLE = "payment"

    suspend fun createOrder(order: OrderTable, items: List<OrderItem>): Result<OrderTable> = withContext(Dispatchers.IO) {
        try {
            // 1. Insert the Order
            val createdOrder = client.postgrest[ORDER_TABLE]
                .insert(order, returning = Columns.ALL)
                .decodeSingle<OrderTable>()

            // 2. Insert Order Items with the new orderId
            val itemsWithOrderId = items.map { it.copy(orderid = createdOrder.orderid) }
            client.postgrest[ORDER_ITEM_TABLE]
                .insert(itemsWithOrderId)
            
            Result.success(createdOrder)
        } catch (e: Exception) {
            println("Error creating order: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun recordPayment(payment: Payment): Result<Payment> = withContext(Dispatchers.IO) {
        try {
            val recordedPayment = client.postgrest[PAYMENT_TABLE]
                .insert(payment, returning = Columns.ALL)
                .decodeSingle<Payment>()
            Result.success(recordedPayment)
        } catch (e: Exception) {
            println("Error recording payment: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches all orders for a given customer ID, ordered by date descending.
     */
    suspend fun getOrdersByCustomerId(customerId: Int): Result<List<OrderTable>> = withContext(Dispatchers.IO) {
        try {
            val orders = client.postgrest[ORDER_TABLE]
                .select {
                    filter {
                        eq("customerid", customerId)
                    }
                    order("orderdate", io.github.jan.supabase.postgrest.query.Order.Direction.DESCENDING)
                }
                .decodeList<OrderTable>()
            Result.success(orders)
        } catch (e: Exception) {
            println("Error fetching orders for customer $customerId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches all order items for a specific order ID.
     */
    suspend fun getOrderItemsByOrderId(orderId: Int): Result<List<OrderItem>> = withContext(Dispatchers.IO) {
        try {
            val items = client.postgrest[ORDER_ITEM_TABLE]
                .select {
                    filter {
                        eq("orderid", orderId)
                    }
                }
                .decodeList<OrderItem>()
            Result.success(items)
        } catch (e: Exception) {
            println("Error fetching items for order $orderId: ${e.message}")
            Result.failure(e)
        }
    }

    // Add functions to fetch orders, order details etc. if needed
} 
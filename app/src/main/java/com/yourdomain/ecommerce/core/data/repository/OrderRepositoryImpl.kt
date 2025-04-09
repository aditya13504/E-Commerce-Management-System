package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.OrderTable
import com.yourdomain.ecommerce.core.domain.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.ReturnFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Implementation of OrderRepository that uses Supabase as data source
 */
class OrderRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : OrderRepository {

    private val tableName = "orders"
    private val tag = "OrderRepository"
    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    override fun getAllOrders(page: Int, pageSize: Int): Flow<List<OrderTable>> = flow {
        try {
            val offset = (page - 1) * pageSize
            val orders = supabaseClient
                .from(tableName)
                .select(columns = Columns.raw("*")) {
                    Order.desc("order_date") // Most recent orders first
                    limit(pageSize)
                    offset(offset)
                }
                .decodeList<OrderTable>()
            emit(orders)
        } catch (e: Exception) {
            Log.e(tag, "Error getting orders: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getOrderById(orderId: String): OrderTable? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("order_id", orderId)
                    }
                }
                .decodeSingleOrNull<OrderTable>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting order with ID $orderId: ${e.message}", e)
            null
        }
    }

    override suspend fun getOrdersByCustomer(customerId: String): List<OrderTable> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("customer_id", customerId)
                    }
                    Order.desc("order_date")
                }
                .decodeList<OrderTable>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting orders for customer $customerId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun createOrder(order: OrderTable): Result<OrderTable> {
        return try {
            // Ensure order_date has a value if not provided
            val orderToCreate = if (order.orderDate.isBlank()) {
                order.copy(orderDate = Instant.now().toString())
            } else {
                order
            }
            
            val createdOrder = supabaseClient
                .from(tableName)
                .insert(orderToCreate, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<OrderTable>()
                
            Result.success(createdOrder)
        } catch (e: Exception) {
            Log.e(tag, "Error creating order: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateOrder(orderId: String, updates: Map<String, Any>): Result<OrderTable> {
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
            
            val updatedOrder = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("order_id", orderId)
                    }
                }
                .decodeSingle<OrderTable>()
                
            Result.success(updatedOrder)
        } catch (e: Exception) {
            Log.e(tag, "Error updating order with ID $orderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteOrder(orderId: String): Result<Boolean> {
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
            Log.e(tag, "Error deleting order with ID $orderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getOrdersByStatus(status: String): List<OrderTable> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("status", status)
                    }
                    Order.desc("order_date")
                }
                .decodeList<OrderTable>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting orders with status $status: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getOrdersByDateRange(startDate: LocalDate, endDate: LocalDate): List<OrderTable> {
        return try {
            // Convert LocalDate to ISO string for database comparison
            val startDateStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            val endDateStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant().toString()
            
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        gte("order_date", startDateStr)
                        lte("order_date", endDateStr)
                    }
                    Order.desc("order_date")
                }
                .decodeList<OrderTable>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting orders between $startDate and $endDate: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getTotalRevenue(startDate: LocalDate?, endDate: LocalDate?): Double {
        return try {
            val query = supabaseClient.from(tableName).select()
            
            // Apply date filtering if provided
            if (startDate != null && endDate != null) {
                val startDateStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                val endDateStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant().toString()
                
                query.filter {
                    gte("order_date", startDateStr)
                    lte("order_date", endDateStr)
                }
            }
            
            val orders = query.decodeList<OrderTable>()
            orders.sumOf { it.totalAmount }
        } catch (e: Exception) {
            Log.e(tag, "Error calculating total revenue: ${e.message}", e)
            0.0
        }
    }

    override suspend fun countOrdersByStatus(): Map<String, Int> {
        return try {
            // Get all orders and group them by status
            val orders = supabaseClient
                .from(tableName)
                .select()
                .decodeList<OrderTable>()
            
            // Count orders by status
            orders.groupingBy { it.status }.eachCount()
        } catch (e: Exception) {
            Log.e(tag, "Error counting orders by status: ${e.message}", e)
            emptyMap()
        }
    }

    override suspend fun countOrders(): Int {
        return try {
            val result = supabaseClient
                .from(tableName)
                .select(count = Count.exact, head = true)
                
            result.count?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(tag, "Error counting orders: ${e.message}", e)
            0
        }
    }
} 
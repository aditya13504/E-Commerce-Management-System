package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.ReturnOrder
import com.yourdomain.ecommerce.core.domain.repository.ReturnOrderRepository
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

/**
 * Implementation of ReturnOrderRepository that uses Supabase as data source
 */
class ReturnOrderRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : ReturnOrderRepository {

    private val tableName = "return_orders"
    private val tag = "ReturnOrderRepository"

    override fun getAllReturnOrders(page: Int, pageSize: Int): Flow<List<ReturnOrder>> = flow {
        try {
            val offset = (page - 1) * pageSize
            val returnOrders = supabaseClient
                .from(tableName)
                .select(columns = Columns.raw("*")) {
                    Order.desc("created_at") // Most recent return orders first
                    limit(pageSize)
                    offset(offset)
                }
                .decodeList<ReturnOrder>()
            emit(returnOrders)
        } catch (e: Exception) {
            Log.e(tag, "Error getting return orders: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getReturnOrderById(returnOrderId: String): ReturnOrder? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("return_order_id", returnOrderId)
                    }
                }
                .decodeSingleOrNull<ReturnOrder>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting return order with ID $returnOrderId: ${e.message}", e)
            null
        }
    }

    override suspend fun getReturnOrdersByOrderId(orderId: String): List<ReturnOrder> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("order_id", orderId)
                    }
                    Order.desc("created_at")
                }
                .decodeList<ReturnOrder>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting return orders for order $orderId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getReturnOrdersByCustomerId(customerId: String): List<ReturnOrder> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("customer_id", customerId)
                    }
                    Order.desc("created_at")
                }
                .decodeList<ReturnOrder>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting return orders for customer $customerId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun createReturnOrder(returnOrder: ReturnOrder): Result<ReturnOrder> {
        return try {
            // Ensure created_at has a value if not provided
            val returnOrderToCreate = if (returnOrder.createdAt.isBlank()) {
                returnOrder.copy(createdAt = Instant.now().toString())
            } else {
                returnOrder
            }
            
            val createdReturnOrder = supabaseClient
                .from(tableName)
                .insert(returnOrderToCreate, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<ReturnOrder>()
                
            Result.success(createdReturnOrder)
        } catch (e: Exception) {
            Log.e(tag, "Error creating return order: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateReturnOrder(returnOrderId: String, updates: Map<String, Any>): Result<ReturnOrder> {
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
            
            val updatedReturnOrder = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("return_order_id", returnOrderId)
                    }
                }
                .decodeSingle<ReturnOrder>()
                
            Result.success(updatedReturnOrder)
        } catch (e: Exception) {
            Log.e(tag, "Error updating return order with ID $returnOrderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteReturnOrder(returnOrderId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("return_order_id", returnOrderId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting return order with ID $returnOrderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getReturnOrdersByStatus(status: String): List<ReturnOrder> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("status", status)
                    }
                    Order.desc("created_at")
                }
                .decodeList<ReturnOrder>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting return orders with status $status: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getReturnOrdersByDateRange(startDate: LocalDate, endDate: LocalDate): List<ReturnOrder> {
        return try {
            // Convert LocalDate to ISO string for database comparison
            val startDateStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            val endDateStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant().toString()
            
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        gte("created_at", startDateStr)
                        lte("created_at", endDateStr)
                    }
                    Order.desc("created_at")
                }
                .decodeList<ReturnOrder>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting return orders between $startDate and $endDate: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun updateReturnOrderStatus(returnOrderId: String, status: String): Result<ReturnOrder> {
        return try {
            val updates = mapOf(
                "status" to status,
                "updated_at" to Instant.now().toString()
            )
            
            // If status is COMPLETED, update completed_at date
            val updatesWithCompletion = if (status == "COMPLETED") {
                updates + ("completed_at" to Instant.now().toString())
            } else {
                updates
            }
            
            updateReturnOrder(returnOrderId, updatesWithCompletion)
        } catch (e: Exception) {
            Log.e(tag, "Error updating status for return order $returnOrderId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getReturnOrderItemsByReturnOrderId(returnOrderId: String): List<Map<String, Any>> {
        return try {
            // This would typically join with a return_order_items table
            // For simplicity, we're assuming return items are stored in a separate table
            supabaseClient
                .from("return_order_items")
                .select {
                    filter {
                        eq("return_order_id", returnOrderId)
                    }
                }
                .decodeList<Map<String, Any>>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting items for return order $returnOrderId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun countReturnOrdersByStatus(): Map<String, Int> {
        return try {
            val returnOrders = supabaseClient
                .from(tableName)
                .select()
                .decodeList<ReturnOrder>()
            
            // Count return orders by status
            returnOrders.groupBy { it.status }
                .mapValues { (_, returnOrders) -> returnOrders.size }
        } catch (e: Exception) {
            Log.e(tag, "Error counting return orders by status: ${e.message}", e)
            emptyMap()
        }
    }

    override suspend fun calculateRefundAmount(returnOrderId: String): Double {
        return try {
            // This is simplified - in a real app this would involve processing return items
            // and their eligible refund amounts based on business rules
            val returnOrder = getReturnOrderById(returnOrderId) ?: return 0.0
            
            // For demo purposes, assume the refund amount is stored directly
            returnOrder.refundAmount ?: 0.0
        } catch (e: Exception) {
            Log.e(tag, "Error calculating refund amount for return order $returnOrderId: ${e.message}", e)
            0.0
        }
    }

    override suspend fun countReturnOrders(): Int {
        return try {
            val result = supabaseClient
                .from(tableName)
                .select(count = Count.exact, head = true)
                
            result.count?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(tag, "Error counting return orders: ${e.message}", e)
            0
        }
    }
} 
package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.ReturnOrder
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ReturnRepository {

    private val client = SupabaseClientManager.client
    private const val TABLE_NAME = "returnorder" // Match your Supabase table name

    /**
     * Creates a new return request entry.
     */
    suspend fun requestReturn(returnRequest: ReturnOrder): Result<ReturnOrder> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest[TABLE_NAME]
                .insert(returnRequest, returning = io.github.jan.supabase.postgrest.query.Columns.ALL)
                .decodeSingle<ReturnOrder>()
            Result.success(result)
        } catch (e: Exception) {
            println("Error creating return request: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches the latest return request for a specific item within an order.
     * Returns null if no return request exists.
     */
    suspend fun getReturnStatusForItem(orderId: Int, productId: Int): Result<ReturnOrder?> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("orderid", orderId)
                        eq("productid", productId)
                    }
                    order("returndate", io.github.jan.supabase.postgrest.query.Order.Direction.DESCENDING) // Get latest request
                    limit(1) // Only need one
                    single() // Expect 0 or 1
                }
                .decodeSingleOrNull<ReturnOrder>()
            Result.success(result)
        } catch (e: Exception) {
             // It's okay if not found, return null via success path
             if (e is io.github.jan.supabase.exceptions.RestException && e.message?.contains("PGRST116") == true) {
                 Result.success(null)
             } else {
                 println("Error fetching return status for order $orderId, product $productId: ${e.message}")
                 Result.failure(e)
             }
        }
    }

    /**
     * Updates the status for a specific return request.
     */
    suspend fun updateReturnStatus(returnId: Int, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest[TABLE_NAME]
                .update( buildJsonObject { put("returnstatus", newStatus) } ) {
                    filter {
                        eq("returnid", returnId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error updating status for return $returnId: ${e.message}")
            Result.failure(e)
        }
    }
} 
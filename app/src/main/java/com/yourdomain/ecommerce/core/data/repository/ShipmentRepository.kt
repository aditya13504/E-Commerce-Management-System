package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.Shipment
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object ShipmentRepository {

    private val client = SupabaseClientManager.client
    private const val TABLE_NAME = "shipment" // Match your Supabase table name

    /**
     * Creates a new shipment entry, linked to an order.
     */
    suspend fun createShipment(shipment: Shipment): Result<Shipment> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest[TABLE_NAME]
                .insert(shipment, returning = io.github.jan.supabase.postgrest.query.Columns.ALL)
                .decodeSingle<Shipment>()
            Result.success(result)
        } catch (e: Exception) {
            println("Error creating shipment entry: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches shipment details for a specific order ID.
     */
    suspend fun getShipmentByOrderId(orderId: Int): Result<Shipment?> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest[TABLE_NAME]
                .select {
                    filter {
                        eq("orderid", orderId)
                    }
                    single() // Expect 0 or 1 shipment per order
                }
                .decodeSingleOrNull<Shipment>()
            Result.success(result)
        } catch (e: Exception) {
            println("Error fetching shipment for order $orderId: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Updates the status for a specific shipment (identified by shipmentId or orderId).
     */
    suspend fun updateShipmentStatus(shipmentId: Int, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest[TABLE_NAME]
                .update( buildJsonObject { put("deliverystatus", newStatus) } ) {
                    filter {
                        eq("shipmentid", shipmentId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error updating status for shipment $shipmentId: ${e.message}")
            Result.failure(e)
        }
    }
} 
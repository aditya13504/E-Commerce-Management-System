package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.Shipment
import com.yourdomain.ecommerce.core.domain.repository.ShipmentRepository
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
 * Implementation of ShipmentRepository that uses Supabase as data source
 */
class ShipmentRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : ShipmentRepository {

    private val tableName = "shipments"
    private val tag = "ShipmentRepository"

    override fun getAllShipments(page: Int, pageSize: Int): Flow<List<Shipment>> = flow {
        try {
            val offset = (page - 1) * pageSize
            val shipments = supabaseClient
                .from(tableName)
                .select(columns = Columns.raw("*")) {
                    Order.desc("created_at") // Most recent shipments first
                    limit(pageSize)
                    offset(offset)
                }
                .decodeList<Shipment>()
            emit(shipments)
        } catch (e: Exception) {
            Log.e(tag, "Error getting shipments: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getShipmentById(shipmentId: String): Shipment? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("shipment_id", shipmentId)
                    }
                }
                .decodeSingleOrNull<Shipment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting shipment with ID $shipmentId: ${e.message}", e)
            null
        }
    }

    override suspend fun getShipmentsByOrderId(orderId: String): List<Shipment> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("order_id", orderId)
                    }
                    Order.desc("created_at")
                }
                .decodeList<Shipment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting shipments for order $orderId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun createShipment(shipment: Shipment): Result<Shipment> {
        return try {
            // Ensure created_at has a value if not provided
            val shipmentToCreate = if (shipment.createdAt.isBlank()) {
                shipment.copy(createdAt = Instant.now().toString())
            } else {
                shipment
            }
            
            val createdShipment = supabaseClient
                .from(tableName)
                .insert(shipmentToCreate, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<Shipment>()
                
            Result.success(createdShipment)
        } catch (e: Exception) {
            Log.e(tag, "Error creating shipment: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateShipment(shipmentId: String, updates: Map<String, Any>): Result<Shipment> {
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
            
            val updatedShipment = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("shipment_id", shipmentId)
                    }
                }
                .decodeSingle<Shipment>()
                
            Result.success(updatedShipment)
        } catch (e: Exception) {
            Log.e(tag, "Error updating shipment with ID $shipmentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteShipment(shipmentId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("shipment_id", shipmentId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting shipment with ID $shipmentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getShipmentsByStatus(status: String): List<Shipment> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("status", status)
                    }
                    Order.desc("created_at")
                }
                .decodeList<Shipment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting shipments with status $status: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getShipmentsByCarrier(carrier: String): List<Shipment> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("carrier", carrier)
                    }
                    Order.desc("created_at")
                }
                .decodeList<Shipment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting shipments for carrier $carrier: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getShipmentsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Shipment> {
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
                .decodeList<Shipment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting shipments between $startDate and $endDate: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun updateTrackingInfo(shipmentId: String, trackingNumber: String, carrier: String?): Result<Shipment> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "tracking_number" to trackingNumber
            )
            
            // Add carrier if provided
            if (carrier != null) {
                updates["carrier"] = carrier
            }
            
            updateShipment(shipmentId, updates)
        } catch (e: Exception) {
            Log.e(tag, "Error updating tracking info for shipment $shipmentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateShipmentStatus(shipmentId: String, status: String): Result<Shipment> {
        return try {
            val updates = mapOf(
                "status" to status,
                "updated_at" to Instant.now().toString()
            )
            
            // If status is DELIVERED, update delivered_at date
            val updatesWithDelivery = if (status == "DELIVERED") {
                updates + ("delivered_at" to Instant.now().toString())
            } else {
                updates
            }
            
            updateShipment(shipmentId, updatesWithDelivery)
        } catch (e: Exception) {
            Log.e(tag, "Error updating status for shipment $shipmentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun countShipmentsByStatus(): Map<String, Int> {
        return try {
            val shipments = supabaseClient
                .from(tableName)
                .select()
                .decodeList<Shipment>()
            
            // Count shipments by status
            shipments.groupBy { it.status }
                .mapValues { (_, shipments) -> shipments.size }
        } catch (e: Exception) {
            Log.e(tag, "Error counting shipments by status: ${e.message}", e)
            emptyMap()
        }
    }

    override suspend fun countShipments(): Int {
        return try {
            val result = supabaseClient
                .from(tableName)
                .select(count = Count.exact, head = true)
                
            result.count?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(tag, "Error counting shipments: ${e.message}", e)
            0
        }
    }
} 
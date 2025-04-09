package com.yourdomain.ecommerce.core.domain.repository

import com.yourdomain.ecommerce.core.data.model.Shipment
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for Shipment entity operations
 * Following clean architecture principles
 */
interface ShipmentRepository {
    /**
     * Get all shipments with pagination
     * @param page The page number to retrieve (1-based)
     * @param pageSize The number of items per page
     * @return Flow of list of shipments for the requested page
     */
    fun getAllShipments(page: Int = 1, pageSize: Int = 20): Flow<List<Shipment>>
    
    /**
     * Get a shipment by ID
     * @param shipmentId The ID of the shipment to retrieve
     * @return The shipment if found, null otherwise
     */
    suspend fun getShipmentById(shipmentId: String): Shipment?
    
    /**
     * Get shipments by order ID
     * @param orderId The ID of the order
     * @return List of shipments for the order
     */
    suspend fun getShipmentsByOrderId(orderId: String): List<Shipment>
    
    /**
     * Create a new shipment
     * @param shipment The shipment to create
     * @return Result containing the created shipment on success, or exception on failure
     */
    suspend fun createShipment(shipment: Shipment): Result<Shipment>
    
    /**
     * Update an existing shipment
     * @param shipmentId The ID of the shipment to update
     * @param updates Map of fields to update with their new values
     * @return Result containing the updated shipment on success, or exception on failure
     */
    suspend fun updateShipment(shipmentId: String, updates: Map<String, Any>): Result<Shipment>
    
    /**
     * Delete a shipment
     * @param shipmentId The ID of the shipment to delete
     * @return Result containing true on success, or exception on failure
     */
    suspend fun deleteShipment(shipmentId: String): Result<Boolean>
    
    /**
     * Get shipments by delivery status
     * @param status The delivery status
     * @return List of shipments with the specified status
     */
    suspend fun getShipmentsByStatus(status: String): List<Shipment>
    
    /**
     * Update shipment tracking information
     * @param shipmentId The ID of the shipment
     * @param trackingNumber The tracking number
     * @param trackingUrl Optional tracking URL
     * @return Result containing the updated shipment on success, or exception on failure
     */
    suspend fun updateTrackingInfo(
        shipmentId: String, 
        trackingNumber: String, 
        trackingUrl: String? = null
    ): Result<Shipment>
    
    /**
     * Update delivery status
     * @param shipmentId The ID of the shipment
     * @param status The new delivery status
     * @return Result containing the updated shipment on success, or exception on failure
     */
    suspend fun updateDeliveryStatus(shipmentId: String, status: String): Result<Shipment>
    
    /**
     * Get shipments by date range
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @return List of shipments between the dates
     */
    suspend fun getShipmentsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Shipment>
    
    /**
     * Get shipments that are in transit
     * @return List of shipments that are in transit
     */
    suspend fun getInTransitShipments(): List<Shipment>
    
    /**
     * Get shipments that are delivered
     * @return List of shipments that are delivered
     */
    suspend fun getDeliveredShipments(): List<Shipment>
    
    /**
     * Count shipments by status
     * @return Map of status to count
     */
    suspend fun countShipmentsByStatus(): Map<String, Int>
    
    /**
     * Get total count of shipments
     * @return The total number of shipments
     */
    suspend fun countShipments(): Int
} 
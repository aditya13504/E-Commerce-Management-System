package com.yourdomain.ecommerce.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Add a helper function to get ISO 8601 formatted timestamp compatible with lower API levels
private fun getCurrentTimestampString(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.format(Date())
}

/**
 * Data model for Shipment in the e-commerce system
 */
@Serializable
data class Shipment(
    @SerialName("shipment_id")
    val shipmentId: String = "",
    
    @SerialName("order_id")
    val orderId: String,
    
    @SerialName("shipment_date")
    val shipmentDate: String? = null,
    
    @SerialName("delivery_status")
    val deliveryStatus: String = "PENDING",
    
    @SerialName("tracking_number")
    val trackingNumber: String? = null,
    
    @SerialName("tracking_url")
    val trackingUrl: String? = null,
    
    @SerialName("shipping_carrier")
    val shippingCarrier: String? = null,
    
    @SerialName("shipping_method")
    val shippingMethod: String? = null,
    
    @SerialName("shipping_cost")
    val shippingCost: Double = 0.0,
    
    @SerialName("estimated_delivery_date")
    val estimatedDeliveryDate: String? = null,
    
    @SerialName("actual_delivery_date")
    val actualDeliveryDate: String? = null,
    
    @SerialName("shipping_address_id")
    val shippingAddressId: String? = null,
    
    @SerialName("shipping_label_url")
    val shippingLabelUrl: String? = null,
    
    @SerialName("package_weight")
    val packageWeight: Double? = null,
    
    @SerialName("package_dimensions")
    val packageDimensions: String? = null,
    
    @SerialName("signature_required")
    val signatureRequired: Boolean = false,
    
    @SerialName("proof_of_delivery_url")
    val proofOfDeliveryUrl: String? = null,
    
    @SerialName("notes")
    val notes: String? = null,
    
    @SerialName("shipment_items")
    val shipmentItems: List<ShipmentItem> = emptyList(),
    
    @SerialName("created_at")
    val createdAt: String = getCurrentTimestampString(),
    
    @SerialName("updated_at")
    val updatedAt: String? = null,
    
    @SerialName("last_update_timestamp")
    val lastUpdateTimestamp: String? = null,
    
    @SerialName("is_return")
    val isReturn: Boolean = false,
    
    @SerialName("metadata")
    val metadata: Map<String, String>? = null
) {
    fun isDelivered(): Boolean = deliveryStatus == "DELIVERED"
    
    fun isShipped(): Boolean = shipmentDate != null
    
    companion object {
        fun createEmpty() = Shipment(
            orderId = ""
        )
    }
}

/**
 * Item included in a shipment
 */
@Serializable
data class ShipmentItem(
    @SerialName("product_id")
    val productId: String,
    
    @SerialName("order_item_id")
    val orderItemId: String,
    
    val quantity: Int,
    
    @SerialName("product_name")
    val productName: String? = null,
    
    @SerialName("product_sku")
    val productSku: String? = null
)

/**
 * Delivery status for a shipment
 */
@Serializable
enum class DeliveryStatus {
    @SerialName("pending")
    PENDING,
    
    @SerialName("processing")
    PROCESSING,
    
    @SerialName("ready_for_pickup")
    READY_FOR_PICKUP,
    
    @SerialName("picked_up")
    PICKED_UP,
    
    @SerialName("in_transit")
    IN_TRANSIT,
    
    @SerialName("out_for_delivery")
    OUT_FOR_DELIVERY,
    
    @SerialName("attempted_delivery")
    ATTEMPTED_DELIVERY,
    
    @SerialName("delivered")
    DELIVERED,
    
    @SerialName("delayed")
    DELAYED,
    
    @SerialName("exception")
    EXCEPTION,
    
    @SerialName("returned")
    RETURNED,
    
    @SerialName("cancelled")
    CANCELLED
} 
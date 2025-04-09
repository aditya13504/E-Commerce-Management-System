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
 * Data model for ReturnOrder in the e-commerce system
 */
@Serializable
data class ReturnOrder(
    @SerialName("return_id")
    val returnId: Int? = null,
    
    @SerialName("order_id")
    val orderId: Int? = null,
    
    @SerialName("product_id")
    val productId: Int? = null,
    
    @SerialName("return_date")
    val returnDate: String? = null,
    
    @SerialName("return_reason")
    val returnReason: String? = null,
    
    @SerialName("return_status")
    val returnStatus: String? = null,
    
    @SerialName("created_at")
    val createdAt: String = getCurrentTimestampString(),
    
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun isCompleted(): Boolean = returnStatus == "COMPLETED"
    
    companion object {
        fun createEmpty() = ReturnOrder(
            orderId = null,
            productId = null,
            returnReason = null
        )
    }
}

/**
 * Status of a return order
 */
@Serializable
enum class ReturnStatus {
    @SerialName("requested")
    REQUESTED,
    
    @SerialName("approved")
    APPROVED,
    
    @SerialName("declined")
    DECLINED,
    
    @SerialName("return_in_transit")
    RETURN_IN_TRANSIT,
    
    @SerialName("received")
    RECEIVED,
    
    @SerialName("inspected")
    INSPECTED,
    
    @SerialName("refunded")
    REFUNDED,
    
    @SerialName("partially_refunded")
    PARTIALLY_REFUNDED,
    
    @SerialName("completed")
    COMPLETED,
    
    @SerialName("cancelled")
    CANCELLED
}

/**
 * Methods for refunding a return
 */
@Serializable
enum class RefundMethod {
    @SerialName("original_payment")
    ORIGINAL_PAYMENT,
    
    @SerialName("store_credit")
    STORE_CREDIT,
    
    @SerialName("gift_card")
    GIFT_CARD,
    
    @SerialName("bank_transfer")
    BANK_TRANSFER
}

/**
 * Condition of returned items
 */
@Serializable
enum class ItemCondition {
    @SerialName("unknown")
    UNKNOWN,
    
    @SerialName("new")
    NEW,
    
    @SerialName("like_new")
    LIKE_NEW,
    
    @SerialName("used")
    USED,
    
    @SerialName("damaged")
    DAMAGED,
    
    @SerialName("not_as_described")
    NOT_AS_DESCRIBED
} 
package com.yourdomain.ecommerce.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Helper function to get ISO 8601 formatted timestamp compatible with lower API levels
private fun getCurrentTimestampString(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.format(Date())
}

/**
 * Data model for Payment in the e-commerce system
 */
@Serializable
data class Payment(
    @SerialName("payment_id")
    val paymentId: String = "",
    
    @SerialName("order_id")
    val orderId: String,
    
    @SerialName("payment_method")
    val paymentMethod: String = "cash",
    
    @SerialName("amount_paid")
    val amountPaid: Double,
    
    @SerialName("payment_date")
    val paymentDate: String = getCurrentTimestampString(),
    
    @SerialName("payment_status")
    val paymentStatus: String = "COMPLETED",
    
    @SerialName("created_at")
    val createdAt: String = getCurrentTimestampString(),
    
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    companion object {
        fun createEmpty() = Payment(
            orderId = "",
            amountPaid = 0.0,
            paymentDate = getCurrentTimestampString()
        )
    }
}

// Removed the PaymentMethod enum as it's no longer needed
// Removed the PaymentProcessStatus enum as it's likely too complex for now 
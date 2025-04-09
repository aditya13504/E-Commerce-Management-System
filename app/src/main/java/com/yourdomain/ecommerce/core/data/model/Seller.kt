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

@Serializable
data class Seller(
    @SerialName("sellerid")
    val sellerId: Int? = null,

    val name: String? = null,

    // Add other seller details like contact info, address, etc.

    @SerialName("created_at")
    val createdAt: String = getCurrentTimestampString(),

    @SerialName("updated_at")
    val updatedAt: String? = null
)

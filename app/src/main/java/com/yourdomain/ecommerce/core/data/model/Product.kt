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
 * Data model for Product in the e-commerce system
 */
@Serializable
data class Product(
    @SerialName("productid")
    val productId: Int? = null,
    
    val name: String? = null,
    
    val description: String? = null,
    
    val price: Double? = null,
    
    @SerialName("stock")
    val stock: Int? = null,
    
    @SerialName("sellerid")
    val sellerId: Int? = null,
    
    val seller: Seller? = null,
    
    @SerialName("seller_rating")
    val sellerRating: Double? = null,
    
    @SerialName("image_url")
    val imageUrl: String? = null,
    
    @SerialName("category_id")
    val categoryId: String? = null,
    
    @SerialName("created_at")
    val createdAt: String = getCurrentTimestampString(),
    
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    companion object {
        fun createEmpty() = Product(
            productId = null,
            sellerId = null
        )
    }
}

/**
 * Product types in the e-commerce system
 */
@Serializable
enum class ProductType {
    @SerialName("physical")
    PHYSICAL,
    
    @SerialName("digital")
    DIGITAL,
    
    @SerialName("service")
    SERVICE,
    
    @SerialName("subscription")
    SUBSCRIPTION
} 
package com.yourdomain.ecommerce.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.yourdomain.ecommerce.core.util.DateTimeUtil

/**
 * Data model for OrderItem in the e-commerce system
 */
@Serializable
data class OrderItem(
    @SerialName("order_item_id")
    val orderItemId: String = "",
    
    @SerialName("order_id")
    val orderId: String,
    
    @SerialName("product_id")
    val productId: String,
    
    val quantity: Int,
    
    @SerialName("item_price")
    val itemPrice: Double,
    
    @SerialName("total_price")
    val totalPrice: Double,
    
    @SerialName("discount_amount")
    val discountAmount: Double = 0.0,
    
    @SerialName("tax_amount")
    val taxAmount: Double = 0.0,
    
    @SerialName("product_name")
    val productName: String,
    
    @SerialName("product_image_url")
    val productImageUrl: String? = null,
    
    @SerialName("product_sku")
    val productSku: String? = null,
    
    @SerialName("product_options")
    val productOptions: Map<String, String>? = null,
    
    @SerialName("is_gift")
    val isGift: Boolean = false,
    
    @SerialName("gift_message")
    val giftMessage: String? = null,
    
    @SerialName("status")
    val status: OrderItemStatus = OrderItemStatus.PROCESSING,
    
    @SerialName("created_at")
    val createdAt: String = DateTimeUtil.getCurrentTimestampString(),
    
    @SerialName("updated_at")
    val updatedAt: String? = null,
    
    @SerialName("returned_quantity")
    val returnedQuantity: Int = 0,
    
    @SerialName("canceled_quantity")
    val canceledQuantity: Int = 0,
    
    @SerialName("refunded_amount")
    val refundedAmount: Double = 0.0,
    
    @SerialName("metadata")
    val metadata: Map<String, String>? = null
) {
    fun getNetQuantity(): Int = quantity - returnedQuantity - canceledQuantity
    
    fun getSubtotal(): Double = itemPrice * quantity
    
    fun getNetTotal(): Double = totalPrice - refundedAmount
    
    companion object {
        fun createEmpty() = OrderItem(
            orderId = "",
            productId = "",
            quantity = 0,
            itemPrice = 0.0,
            totalPrice = 0.0,
            productName = ""
        )
    }
}

/**
 * Status of an order item
 */
@Serializable
enum class OrderItemStatus {
    @SerialName("processing")
    PROCESSING,
    
    @SerialName("shipped")
    SHIPPED,
    
    @SerialName("delivered")
    DELIVERED,
    
    @SerialName("canceled")
    CANCELED,
    
    @SerialName("returned")
    RETURNED,
    
    @SerialName("refunded")
    REFUNDED,
    
    @SerialName("back_ordered")
    BACK_ORDERED
} 
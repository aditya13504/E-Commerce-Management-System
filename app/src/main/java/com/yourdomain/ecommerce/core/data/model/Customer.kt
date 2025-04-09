package com.yourdomain.ecommerce.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.yourdomain.ecommerce.core.util.DateTimeUtil

@Serializable
data class Customer(
    @SerialName("customerid") // Assuming 'customerid' is the primary key in your DB table
    val customerId: Int? = null,

    @SerialName("auth_user_id") // Foreign key linking to Supabase auth user ID
    val authUserId: String,

    val email: String? = null,

    val name: String? = null,

    // Add other fields like address, phone number as needed

    @SerialName("created_at")
    val createdAt: String = DateTimeUtil.getCurrentTimestampString(),

    @SerialName("updated_at")
    val updatedAt: String? = null
)

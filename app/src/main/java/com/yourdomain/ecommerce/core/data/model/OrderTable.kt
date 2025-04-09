package com.yourdomain.ecommerce.core.data.model

import kotlinx.serialization.Serializable

/**
 * Data model for Order table in the e-commerce system
 * Named OrderTable to avoid conflicts with Kotlin's Order class
 */
@Serializable
data class OrderTable(
    val orderid: Int? = null,
    val customerid: Int? = null, // Foreign key to Customer
    val orderdate: String? = null, // Using String for simplicity
    val totalamount: Double? = null
) 
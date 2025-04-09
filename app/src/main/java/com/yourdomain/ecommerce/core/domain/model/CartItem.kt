package com.yourdomain.ecommerce.core.domain.model

import com.yourdomain.ecommerce.core.data.model.Product

/**
 * Model representing an item in the shopping cart
 * 
 * @property product The product in the cart
 * @property quantity The quantity of the product
 */
data class CartItem(
    val product: Product,
    val quantity: Int
) 
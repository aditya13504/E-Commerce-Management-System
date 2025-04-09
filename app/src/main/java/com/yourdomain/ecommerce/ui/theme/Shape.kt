package com.yourdomain.ecommerce.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape definitions for the E-commerce app.
 * 
 * Defines various corner shapes and sizes for different UI components
 * with a cohesive design language focused on rounded corners for a
 * modern, friendly, and accessible interface.
 */

// Main shape system for the app
val ECommerceShapes = Shapes(
    // Small components like buttons, chips, and inputs
    small = RoundedCornerShape(8.dp),
    
    // Medium components like cards, dialogs, and sheets
    medium = RoundedCornerShape(12.dp),
    
    // Large components like bottom sheets, navigation drawers
    large = RoundedCornerShape(16.dp),
    
    // Extra large components
    extraLarge = RoundedCornerShape(24.dp)
)

// Additional custom shapes for special UI elements
object ECommerceCustomShapes {
    // Product card shape with uneven corners for visual interest
    val productCard = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )
    
    // Cart item shape
    val cartItem = RoundedCornerShape(8.dp)
    
    // Custom shape for promo banners with one cut corner
    val promoBanner = CutCornerShape(
        topStart = 0.dp,
        topEnd = 16.dp,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )
    
    // Search bar shape
    val searchBar = RoundedCornerShape(28.dp)
    
    // Button shapes
    val primaryButton = RoundedCornerShape(12.dp)
    val secondaryButton = RoundedCornerShape(8.dp)
    val pillButton = RoundedCornerShape(24.dp)
    
    // Category chip shape
    val categoryChip = RoundedCornerShape(16.dp)
    
    // Circular shape for avatars, featured indicators
    val circle = CircleShape
} 
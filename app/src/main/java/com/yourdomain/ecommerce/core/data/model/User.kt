package com.yourdomain.ecommerce.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the user profile data, often derived from auth info.
 */
@Serializable
data class User(
    val id: String, // Matches Supabase auth user ID (UUID)
    val email: String,
    @SerialName("user_name") // Example if metadata uses snake_case
    val userName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null, // Supabase provides these as ISO strings
    @SerialName("last_sign_in")
    val lastSignIn: String? = null
)

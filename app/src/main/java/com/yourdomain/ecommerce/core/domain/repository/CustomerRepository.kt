package com.yourdomain.ecommerce.core.domain.repository

import com.yourdomain.ecommerce.core.data.model.Customer
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing customer data.
 */
interface CustomerRepository {
    /**
     * Creates a customer record, typically linked to an authenticated user.
     */
    suspend fun createCustomerForAuthUser(authUserId: String, email: String, name: String? = null): Result<Customer>

    /**
     * Gets a customer profile by their authentication user ID.
     */
    suspend fun getCustomerByAuthId(authUserId: String): Result<Customer?>

    /**
     * Updates a customer's profile.
     */
    suspend fun updateCustomer(customer: Customer): Result<Unit>

    // Add other methods as needed, e.g., getCustomerById(customerId: Int)
}

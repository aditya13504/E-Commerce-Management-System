package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.Customer
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.core.domain.repository.CustomerRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Using object for simplicity as we don't have DI for this specific repository yet.
// In a larger app with DI, this would be a class.
object CustomerRepositoryImpl : CustomerRepository {

    private val client = SupabaseClientManager.client
    private val table = client.postgrest["customers"] // Ensure your table name is correct

    override suspend fun createCustomerForAuthUser(authUserId: String, email: String, name: String?): Result<Customer> = withContext(Dispatchers.IO) {
        try {
            val customer = Customer(
                authUserId = authUserId,
                email = email,
                name = name
            )
            val result = table.insert(customer, upsert = false) { // Don't upsert by default
                 select(Columns.ALL)
            }.decodeSingle<Customer>()
            Result.success(result)
        } catch (e: Exception) {
            println("Error creating customer: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCustomerByAuthId(authUserId: String): Result<Customer?> = withContext(Dispatchers.IO) {
        try {
            val result = table.select {
                filter {
                    eq("auth_user_id", authUserId)
                }
                limit(1)
            }.decodeSingleOrNull<Customer>()
            Result.success(result)
        } catch (e: Exception) {
            println("Error fetching customer by auth ID: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateCustomer(customer: Customer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Ensure customerId is present for update
            val id = customer.customerId ?: return@withContext Result.failure(IllegalArgumentException("Customer ID required for update"))
            table.update({
                // Add fields to update here, e.g.:
                set("name", customer.name)
                set("email", customer.email)
                // Add other updatable fields
                set("updated_at", "now()") // Update timestamp using DB function
            }) {
                filter {
                    eq("customerid", id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
             println("Error updating customer: ${e.message}")
            Result.failure(e)
        }
    }
}

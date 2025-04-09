package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.Customer
import com.yourdomain.ecommerce.core.domain.repository.CustomerRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.ReturnFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of CustomerRepository that uses Supabase as data source
 */
class CustomerRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : CustomerRepository {

    private val tableName = "customers"
    private val tag = "CustomerRepository"

    override fun getAllCustomers(page: Int, pageSize: Int): Flow<List<Customer>> = flow {
        try {
            val offset = (page - 1) * pageSize
            val customers = supabaseClient.from(tableName)
                .select(columns = Columns.raw("*")) {
                    Order.asc("name")
                    limit(pageSize)
                    offset(offset)
                }
                .decodeList<Customer>()
            emit(customers)
        } catch (e: Exception) {
            Log.e(tag, "Error getting customers: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getCustomerById(customerId: String): Customer? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("customer_id", customerId)
                    }
                }
                .decodeSingleOrNull<Customer>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting customer with ID $customerId: ${e.message}", e)
            null
        }
    }

    override suspend fun getCustomerByEmail(email: String): Customer? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeSingleOrNull<Customer>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting customer with email $email: ${e.message}", e)
            null
        }
    }

    override suspend fun createCustomer(customer: Customer): Result<Customer> {
        return try {
            // Check if a customer with the same email already exists
            val existingCustomer = getCustomerByEmail(customer.email)
            if (existingCustomer != null) {
                return Result.failure(Exception("A customer with this email already exists"))
            }
            
            val createdCustomer = supabaseClient
                .from(tableName)
                .insert(customer, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<Customer>()
                
            Result.success(createdCustomer)
        } catch (e: Exception) {
            Log.e(tag, "Error creating customer: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Result<Customer> {
        return try {
            // If email is being updated, check if it would conflict with an existing customer
            if (updates.containsKey("email")) {
                val newEmail = updates["email"] as String
                val existingCustomer = getCustomerByEmail(newEmail)
                if (existingCustomer != null && existingCustomer.customerId != customerId) {
                    return Result.failure(Exception("A customer with this email already exists"))
                }
            }
            
            // Build the JSON object for the update
            val updateData = buildJsonObject {
                updates.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> {
                            // Convert other types to string if necessary
                            put(key, value.toString())
                        }
                    }
                }
                
                // Add updated_at timestamp
                put("updated_at", Instant.now().toString())
            }
            
            val updatedCustomer = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("customer_id", customerId)
                    }
                }
                .decodeSingle<Customer>()
                
            Result.success(updatedCustomer)
        } catch (e: Exception) {
            Log.e(tag, "Error updating customer with ID $customerId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomer(customerId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("customer_id", customerId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting customer with ID $customerId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun searchCustomers(query: String): List<Customer> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        or {
                            ilike("name", "%$query%")
                            ilike("email", "%$query%")
                        }
                    }
                    Order.asc("name")
                }
                .decodeList<Customer>()
        } catch (e: Exception) {
            Log.e(tag, "Error searching customers with query '$query': ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun countCustomers(): Int {
        return try {
            val result = supabaseClient
                .from(tableName)
                .select(count = Count.exact, head = true)
                
            result.count?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(tag, "Error counting customers: ${e.message}", e)
            0
        }
    }

    // New function to get customer ID from auth ID
    suspend fun getCustomerIdForAuthUser(authUserId: String): Result<Int?> = withContext(Dispatchers.IO) {
        try {
            val customer = getCustomerByAuthId(authUserId).getOrNull() // Reuse existing function
            Result.success(customer?.customerId) // Extract the Int? ID
        } catch (e: Exception) {
            // This catch might not be strictly necessary if getCustomerByAuthId handles its errors
            // but keeping it for safety.
            println("Error retrieving customer ID for auth ID $authUserId: ${e.message}")
            Result.failure(e)
        }
    }
} 
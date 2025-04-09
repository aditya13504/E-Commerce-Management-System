package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.Seller
import com.yourdomain.ecommerce.core.domain.repository.SellerRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.ReturnFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Implementation of SellerRepository that uses Supabase as data source
 */
class SellerRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : SellerRepository {

    private val tableName = "sellers"
    private val tag = "SellerRepository"

    override fun getAllSellers(): Flow<List<Seller>> = flow {
        try {
            val sellers = supabaseClient
                .from(tableName)
                .select(columns = Columns.raw("*")) {
                    Order.asc("name")
                }
                .decodeList<Seller>()
            emit(sellers)
        } catch (e: Exception) {
            Log.e(tag, "Error getting all sellers: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getSellerById(sellerId: String): Seller? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("seller_id", sellerId)
                    }
                }
                .decodeSingleOrNull<Seller>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting seller with ID $sellerId: ${e.message}", e)
            null
        }
    }

    override suspend fun createSeller(seller: Seller): Result<Seller> {
        return try {
            val createdSeller = supabaseClient
                .from(tableName)
                .insert(seller, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<Seller>()
                
            Result.success(createdSeller)
        } catch (e: Exception) {
            Log.e(tag, "Error creating seller: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSeller(sellerId: String, updates: Map<String, Any>): Result<Seller> {
        return try {
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
            
            val updatedSeller = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("seller_id", sellerId)
                    }
                }
                .decodeSingle<Seller>()
                
            Result.success(updatedSeller)
        } catch (e: Exception) {
            Log.e(tag, "Error updating seller with ID $sellerId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteSeller(sellerId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("seller_id", sellerId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting seller with ID $sellerId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun searchSellersByName(query: String): List<Seller> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        ilike("name", "%$query%")
                    }
                    Order.asc("name")
                }
                .decodeList<Seller>()
        } catch (e: Exception) {
            Log.e(tag, "Error searching sellers by name '$query': ${e.message}", e)
            emptyList()
        }
    }
} 
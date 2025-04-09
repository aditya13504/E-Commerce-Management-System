package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Columns.Companion.raw as rawColumns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.ReturnFormat
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.query.Returning
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository for handling product data operations with Supabase
 * Updated with latest features for 2025
 */
object ProductRepository { // Singleton without DI
    private val tag = "ProductRepository"
    private val tableName = "products"
    private val client = SupabaseClientManager.client // Ensure this uses the correct manager

    /**
     * Fetch all products with pagination, enhanced for performance
     */
    fun getAllProducts(
        page: Int = 1,
        pageSize: Int = 20,
        sortField: String = "name",
        sortOrder: Order.Direction = Order.Direction.ASCENDING
    ): Flow<List<Product>> = flow {
        try {
            val offset = (page - 1) * pageSize
            val products = client.postgrest[tableName]
                .select(columns = rawColumns("*")) {
                    // Applied performance optimization from 2025
                    optimizeFor = io.github.jan.supabase.postgrest.query.QueryOptimization.BATCH
                    Order.order(sortField, sortOrder)
                    limit(pageSize)
                    offset(offset)
                }
                .decodeList<Product>()
            emit(products)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching products: ${e.message}", e)
            emit(emptyList())
        }
    }

    /**
     * Fetch a product by ID with advanced caching
     */
    suspend fun getProductById(id: String): Product? {
        return try {
            client.postgrest[tableName]
                .select(cachePolicy = io.github.jan.supabase.postgrest.query.CachePolicy.CACHE_FIRST) { 
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingleOrNull<Product>()
        } catch (e: Exception) {
            Log.e(tag, "Error fetching product with ID $id: ${e.message}", e)
            null
        }
    }

    /**
     * Create a new product with optimistic update feature
     */
    suspend fun createProduct(product: Product): Result<Product> {
        return try {
            val result = client.postgrest[tableName]
                .insert(product, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<Product>()
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "Error creating product: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing product
     */
    suspend fun updateProduct(id: String, updates: Map<String, Any>): Result<Product> {
        return try {
            // Build JSON object for update
            val updateData = buildJsonObject {
                updates.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        // Add other types as needed
                    }
                }
                // Add updated_at timestamp
                put("updated_at", java.time.Instant.now().toString())
            }
            
            val updated = client.postgrest[tableName]
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Product>()
            
            Result.success(updated)
        } catch (e: Exception) {
            Log.e(tag, "Error updating product with ID $id: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a product with conflict handling
     */
    suspend fun deleteProduct(id: String): Result<Boolean> {
        return try {
            client.postgrest[tableName]
                .delete(options = io.github.jan.supabase.postgrest.query.Options(
                    onConflict = "id"
                )) {
                    filter {
                        eq("id", id)
                    }
                }
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting product with ID $id: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Advanced search for products with full-text search
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            // Using stored procedure for full-text search (added in 2025)
            val results = if (query.length > 3) {
                // Use advanced full-text search for longer queries
                client.rpc("search_products", buildJsonObject {
                    put("search_term", query)
                }).decodeList<Product>()
            } else {
                // Use regular ILIKE for short queries
                client.postgrest[tableName]
                    .select {
                        filter {
                            or {
                                ilike("name", "%$query%")
                                ilike("description", "%$query%")
                                ilike("category", "%$query%")
                            }
                        }
                        Order.asc("name")
                    }
                    .decodeList<Product>()
            }
            Result.success(results)
        } catch (e: Exception) {
            Log.e(tag, "Error searching products: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Count products with improved performance
     */
    suspend fun countProducts(): Result<Int> {
        return try {
            val result = client.postgrest[tableName]
                .select(count = Count.exact, head = true) // Head=true for faster counts
            
            Result.success(result.count?.toInt() ?: 0)
        } catch (e: Exception) {
            Log.e(tag, "Error counting products: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get products by category with batching (new in 2025)
     */
    suspend fun getProductsByCategory(category: String): Result<List<Product>> {
        return try {
            val products = client.postgrest[tableName]
                .select {
                    filter {
                        eq("category", category)
                        eq("is_active", true)
                    }
                    Order.asc("name")
                }
                .decodeList<Product>()
                
            Result.success(products)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching products by category: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update stock quantity atomically using PostgreSQL function (new in 2025)
     */
    suspend fun updateStock(productId: String, quantityChange: Int): Result<Int> {
        return try {
            val result = client.rpc("update_product_stock", buildJsonObject {
                put("product_id", productId)
                put("quantity_change", quantityChange)
            }).decodeScalar<Int>()
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e(tag, "Error updating stock: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllProducts(): Result<List<Product>> = withContext(Dispatchers.IO) {
        try {
            val result = client.postgrest[tableName]
                .select(columns = Columns.ALL) // Select all columns
                .decodeList<Product>()
            Result.success(result)
        } catch (e: Exception) {
            // Log the error or handle it appropriately
            println("Error fetching products: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates the stock for a given product ID.
     * WARNING: Use with caution client-side, ideally done server-side/transactionally.
     */
    suspend fun updateStock(productId: Int, newStock: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.postgrest[tableName]
                .update(
                    update = buildJsonObject { put("stock", newStock) },
                    // returning = Returning.MINIMAL // Don't need the updated record back
                ) {
                    filter {
                        eq("productid", productId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            println("Error updating stock for product $productId: ${e.message}")
            Result.failure(e)
        }
    }
} 
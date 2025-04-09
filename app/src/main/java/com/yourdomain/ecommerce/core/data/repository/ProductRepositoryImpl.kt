package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.Instant

/**
 * Implementation of the ProductRepository using Supabase
 */
class ProductRepositoryImpl constructor(
    private val supabaseClient: SupabaseClientManager
) : ProductRepository {

    companion object {
        private const val PRODUCTS_TABLE = "products"
    }

    private val tableName = PRODUCTS_TABLE
    private val tag = "ProductRepository"

    override suspend fun getAllProducts(
        page: Int,
        pageSize: Int,
        sortField: String,
        sortDirection: String
    ): List<Product> {
        return try {
            val rangeFrom = (page - 1) * pageSize
            val rangeTo = rangeFrom + pageSize - 1
            
            val order = if (sortDirection.equals("asc", ignoreCase = true)) Order.ASCENDING else Order.DESCENDING
            
            // Query the Supabase products table with pagination and sorting
            supabaseClient.client.postgrest[tableName]
                .select {
                    range(rangeFrom, rangeTo)
                    order(sortField, order)
                }
                .decodeList<Product>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error fetching products: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getProductById(productId: String): Product? {
        return try {
            // Query the Supabase products table by ID
            supabaseClient.client.postgrest[tableName]
                .select {
                    eq("product_id", productId)
                }
                .decodeSingleOrNull<Product>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error fetching product by ID: ${e.message}")
            null
        }
    }

    override suspend fun getProductsBySeller(sellerId: String): List<Product> {
        return try {
            // Query the Supabase products table by seller ID
            supabaseClient.client.postgrest[tableName]
                .select {
                    eq("seller_id", sellerId)
                }
                .decodeList<Product>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error fetching products by seller: ${e.message}")
            emptyList()
        }
    }

    override suspend fun createProduct(product: Product): Product {
        try {
            // Insert new product into Supabase
            supabaseClient.client.postgrest[tableName]
                .insert(product)
                
            return product
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error creating product: ${e.message}")
            throw e
        }
    }

    override suspend fun updateProduct(productId: String, updates: Map<String, Any>): Boolean {
        return try {
            // Update product in Supabase
            supabaseClient.client.postgrest[tableName]
                .update(updates) {
                    eq("product_id", productId)
                }
            
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error updating product: ${e.message}")
            false
        }
    }

    override suspend fun deleteProduct(productId: String): Boolean {
        return try {
            // Delete product from Supabase
            supabaseClient.client.postgrest[tableName]
                .delete {
                    eq("product_id", productId)
                }
            
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error deleting product: ${e.message}")
            false
        }
    }

    override suspend fun searchProducts(query: String): List<Product> {
        return try {
            // Using ilike for case-insensitive search in Supabase
            supabaseClient.client.postgrest[tableName]
                .select {
                    or {
                        ilike("name", "%$query%")
                        ilike("description", "%$query%")
                    }
                }
                .decodeList<Product>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error searching products: ${e.message}")
            emptyList()
        }
    }

    override suspend fun updateStock(productId: String, quantityChange: Int): Boolean {
        return try {
            // First, get the current product to check the stock
            val product = getProductById(productId) ?: return false
            
            // Calculate new stock value
            val newStock = product.stockQuantity + quantityChange
            
            // Ensure stock doesn't go below zero
            if (newStock < 0) {
                Timber.w("Cannot reduce stock below zero for product $productId")
                return false
            }
            
            // Update the stock
            return updateProduct(productId, mapOf("stock_quantity" to newStock))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error updating product stock: ${e.message}")
            false
        }
    }

    override suspend fun getProductsWithLowStock(threshold: Int): List<Product> {
        return try {
            // Query products with stock below threshold
            supabaseClient.client.postgrest[tableName]
                .select {
                    lt("stock_quantity", threshold)
                }
                .decodeList<Product>()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error fetching low stock products: ${e.message}")
            emptyList()
        }
    }

    override suspend fun countProducts(): Int {
        return try {
            // Count total products using Supabase
            supabaseClient.client.postgrest[tableName]
                .count()
                .count
                .toInt()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Error counting products: ${e.message}")
            0
        }
    }
} 
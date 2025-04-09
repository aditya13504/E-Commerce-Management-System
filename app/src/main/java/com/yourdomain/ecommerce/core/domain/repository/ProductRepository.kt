package com.yourdomain.ecommerce.core.domain.repository

import com.yourdomain.ecommerce.core.data.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Product entity operations
 * Following clean architecture principles
 */
interface ProductRepository {
    /**
     * Get all products with pagination, sorting, and filtering
     * @param page The page number to retrieve (1-based)
     * @param pageSize The number of items per page
     * @param sortField The field to sort by
     * @param sortDirection The sort direction ("asc" or "desc")
     * @return List of products for the requested page
     */
    suspend fun getAllProducts(
        page: Int, 
        pageSize: Int,
        sortField: String = "created_at",
        sortDirection: String = "desc"
    ): List<Product>
    
    /**
     * Get a specific product by ID
     * @param productId The ID of the product to retrieve
     * @return The product if found, null otherwise
     */
    suspend fun getProductById(productId: String): Product?
    
    /**
     * Get products by seller ID
     * @param sellerId The ID of the seller
     * @return List of products for the seller
     */
    suspend fun getProductsBySeller(sellerId: String): List<Product>
    
    /**
     * Create a new product
     * @param product The product to create
     * @return The created product
     */
    suspend fun createProduct(product: Product): Product
    
    /**
     * Update product fields
     * @param productId The ID of the product to update
     * @param updates Map of fields to update with their new values
     * @return true on success, false on failure
     */
    suspend fun updateProduct(productId: String, updates: Map<String, Any>): Boolean
    
    /**
     * Delete a product
     * @param productId The ID of the product to delete
     * @return true on success, false on failure
     */
    suspend fun deleteProduct(productId: String): Boolean
    
    /**
     * Search products by query string
     * @param query The search query
     * @return List of products matching the query
     */
    suspend fun searchProducts(query: String): List<Product>
    
    /**
     * Update product stock quantity
     * @param productId The ID of the product
     * @param quantityChange The change in quantity (positive or negative)
     * @return true on success, false on failure
     */
    suspend fun updateStock(productId: String, quantityChange: Int): Boolean
    
    /**
     * Get products with stock below threshold
     * @param threshold The threshold below which stock is considered low
     * @return List of products with stock below the threshold
     */
    suspend fun getProductsWithLowStock(threshold: Int): List<Product>
    
    /**
     * Count total number of products
     * @return The total number of products
     */
    suspend fun countProducts(): Int
} 
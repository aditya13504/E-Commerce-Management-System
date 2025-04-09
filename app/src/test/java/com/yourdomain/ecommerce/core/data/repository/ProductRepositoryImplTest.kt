package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.supabase.gotrue.postgres.postgrest
import io.supabase.postgrest.PostgrestBuilder
import io.supabase.supabase.SupabaseClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var supabaseClientManager: SupabaseClientManager
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var postgrestBuilder: PostgrestBuilder<Product>
    
    // Test data
    private val testProducts = listOf(
        Product(
            id = "1",
            name = "Test Product 1",
            description = "Description 1",
            price = 10.99,
            imageUrl = "https://example.com/image1.jpg",
            stock = 5,
            sellerId = "seller1",
            categoryId = "category1",
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        ),
        Product(
            id = "2",
            name = "Test Product 2",
            description = "Description 2",
            price = 20.49,
            imageUrl = "https://example.com/image2.jpg",
            stock = 10,
            sellerId = "seller1",
            categoryId = "category2",
            createdAt = "2023-01-02T00:00:00Z",
            updatedAt = "2023-01-02T00:00:00Z"
        ),
        Product(
            id = "3",
            name = "Budget Product",
            description = "Low-cost option",
            price = 5.99,
            imageUrl = "https://example.com/image3.jpg",
            stock = 15,
            sellerId = "seller2",
            categoryId = "category1",
            createdAt = "2023-01-03T00:00:00Z",
            updatedAt = "2023-01-03T00:00:00Z"
        )
    )
    
    @Before
    fun setUp() {
        // Set up mocks
        supabaseClient = mockk(relaxed = true)
        supabaseClientManager = mockk(relaxed = true)
        postgrestBuilder = mockk(relaxed = true)
        
        // Set up Supabase client manager behavior
        coEvery { supabaseClientManager.getClient() } returns supabaseClient
        
        // Set up PostgrestBuilder behavior
        coEvery { supabaseClient.postgrest.from<Product>(any()) } returns postgrestBuilder
        
        // Initialize repository
        productRepository = ProductRepositoryImpl(supabaseClientManager)
    }
    
    @Test
    fun `getAllProducts should return list of products`() = runTest {
        // Given
        val page = 1
        val pageSize = 10
        coEvery { 
            postgrestBuilder.select()
                .range(any(), any())
                .order(any(), any())
                .executeAndGetList<Product>()
        } returns testProducts
        
        // When
        val result = productRepository.getAllProducts(page, pageSize)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(testProducts, (result as Result.Success).data)
        
        // Verify range calculation
        coVerify { 
            postgrestBuilder.select()
                .range((page - 1) * pageSize, page * pageSize - 1)
                .order("createdAt", ascending = false)
                .executeAndGetList<Product>() 
        }
    }
    
    @Test
    fun `getAllProducts should return error on exception`() = runTest {
        // Given
        val exception = IOException("Network error")
        coEvery { 
            postgrestBuilder.select()
                .range(any(), any())
                .order(any(), any())
                .executeAndGetList<Product>()
        } throws exception
        
        // When
        val result = productRepository.getAllProducts(1, 10)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `getProductById should return product when found`() = runTest {
        // Given
        val productId = "1"
        val product = testProducts[0]
        
        coEvery { 
            postgrestBuilder.select()
                .eq("id", productId)
                .limit(1)
                .executeSingle<Product>()
        } returns product
        
        // When
        val result = productRepository.getProductById(productId)
        
        // Then
        assertEquals(product, result)
        
        // Verify correct query
        coVerify { 
            postgrestBuilder.select()
                .eq("id", productId)
                .limit(1)
                .executeSingle<Product>() 
        }
    }
    
    @Test
    fun `getProductById should return null when not found`() = runTest {
        // Given
        val productId = "999"
        
        coEvery { 
            postgrestBuilder.select()
                .eq("id", productId)
                .limit(1)
                .executeSingle<Product>()
        } returns null
        
        // When
        val result = productRepository.getProductById(productId)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getProductsByCategory should return products in category`() = runTest {
        // Given
        val categoryId = "category1"
        val expectedProducts = testProducts.filter { it.categoryId == categoryId }
        
        coEvery { 
            postgrestBuilder.select()
                .eq("categoryId", categoryId)
                .range(any(), any())
                .executeAndGetList<Product>()
        } returns expectedProducts
        
        // When
        val result = productRepository.getProductsByCategory(categoryId, 1, 10)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedProducts, (result as Result.Success).data)
    }
    
    @Test
    fun `searchProducts should filter by query`() = runTest {
        // Given
        val query = "budget"
        val expectedProducts = testProducts.filter { 
            it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) 
        }
        
        coEvery { 
            postgrestBuilder.select()
                .or("name.ilike.%$query%,description.ilike.%$query%")
                .executeAndGetList<Product>()
        } returns expectedProducts
        
        // When
        val result = productRepository.searchProducts(query)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedProducts, (result as Result.Success).data)
    }
    
    @Test
    fun `createProduct should return product with id`() = runTest {
        // Given
        val newProduct = testProducts[0].copy(id = "")
        val createdProduct = newProduct.copy(id = "new-id-123")
        
        val capturedProduct = slot<Product>()
        
        coEvery { 
            postgrestBuilder.insert(capture(capturedProduct))
                .executeAndGetSingle<Product>()
        } returns createdProduct
        
        // When
        val result = productRepository.createProduct(newProduct)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(createdProduct, (result as Result.Success).data)
        assertEquals(newProduct, capturedProduct.captured)
    }
    
    @Test
    fun `updateProduct should return updated product`() = runTest {
        // Given
        val productId = "1"
        val updates = mapOf(
            "name" to "Updated Name",
            "price" to 15.99
        )
        val updatedProduct = testProducts[0].copy(name = "Updated Name", price = 15.99)
        
        coEvery { 
            postgrestBuilder.update(updates)
                .eq("id", productId)
                .executeAndGetSingle<Product>()
        } returns updatedProduct
        
        // When
        val result = productRepository.updateProduct(productId, updates)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(updatedProduct, (result as Result.Success).data)
    }
    
    @Test
    fun `deleteProduct should return success when deleted`() = runTest {
        // Given
        val productId = "1"
        
        coEvery { 
            postgrestBuilder.delete()
                .eq("id", productId)
                .execute()
        } returns Unit
        
        // When
        val result = productRepository.deleteProduct(productId)
        
        // Then
        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data)
    }
    
    @Test
    fun `getProductsByPriceRange should filter by price`() = runTest {
        // Given
        val minPrice = 10.0
        val maxPrice = 15.0
        val expectedProducts = testProducts.filter { it.price in minPrice..maxPrice }
        
        coEvery { 
            postgrestBuilder.select()
                .gte("price", minPrice)
                .lte("price", maxPrice)
                .executeAndGetList<Product>()
        } returns expectedProducts
        
        // When
        val result = productRepository.getProductsByPriceRange(minPrice, maxPrice)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedProducts, (result as Result.Success).data)
    }
    
    @Test
    fun `getFeaturedProducts should return featured products`() = runTest {
        // Given
        val featuredProducts = testProducts.take(2)
        
        coEvery { 
            postgrestBuilder.select()
                .eq("featured", true)
                .limit(any())
                .executeAndGetList<Product>()
        } returns featuredProducts
        
        // When
        val result = productRepository.getFeaturedProducts(2)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(featuredProducts, (result as Result.Success).data)
    }
} 
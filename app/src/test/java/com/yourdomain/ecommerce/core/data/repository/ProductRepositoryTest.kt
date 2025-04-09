package com.yourdomain.ecommerce.core.data.repository

import app.cash.turbine.test
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.core.data.model.Product
import io.github.jan.supabase.postgrest.PostgrestResult
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryTest {

    private lateinit var repository: ProductRepositoryImpl
    private val supabaseClientManager: SupabaseClientManager = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    
    // Test data
    private val testProducts = listOf(
        Product(
            id = "1",
            name = "Test Product 1",
            description = "Test description 1",
            price = 99.99,
            imageUrl = "https://example.com/image1.jpg",
            stock = 10,
            sellerId = "seller1",
            categoryId = "category1",
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        ),
        Product(
            id = "2",
            name = "Test Product 2",
            description = "Test description 2",
            price = 149.99,
            imageUrl = "https://example.com/image2.jpg",
            stock = 5,
            sellerId = "seller2",
            categoryId = "category2",
            createdAt = "2023-01-02T00:00:00Z",
            updatedAt = "2023-01-02T00:00:00Z"
        ),
        Product(
            id = "3",
            name = "Low Stock Item",
            description = "Almost out of stock",
            price = 199.99,
            imageUrl = "https://example.com/image3.jpg",
            stock = 2,
            sellerId = "seller1",
            categoryId = "category3",
            createdAt = "2023-01-03T00:00:00Z",
            updatedAt = "2023-01-03T00:00:00Z"
        )
    )

    @Before
    fun setUp() {
        repository = ProductRepositoryImpl(supabaseClientManager, testDispatcher)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getAllProducts should return products with correct pagination and sorting`() = runTest {
        // Arrange
        val page = 1
        val pageSize = 10
        val sortField = "price"
        val sortDirection = "desc"
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .select() 
                .order(sortField, sortDirection == "desc")
                .range(0, 9)
                .decodeList<Product>()
        } returns testProducts
        
        // Act
        val result = repository.getAllProducts(page, pageSize, sortField, sortDirection)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { products ->
            assertEquals(3, products.size)
            assertEquals("3", products[0].id) // Highest price should be first with "desc" sorting
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .select()
                .order(sortField, true)
                .range(0, 9)
                .decodeList<Product>()
        }
    }
    
    @Test
    fun `getProductById should return a specific product`() = runTest {
        // Arrange
        val productId = "1"
        val expectedProduct = testProducts[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .select()
                .eq("id", productId)
                .limit(1)
                .decodeSingle<Product>()
        } returns expectedProduct
        
        // Act
        val result = repository.getProductById(productId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { product ->
            assertEquals(expectedProduct, product)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .select()
                .eq("id", productId)
                .limit(1)
                .decodeSingle<Product>()
        }
    }
    
    @Test
    fun `getProductsBySeller should return all products for a seller`() = runTest {
        // Arrange
        val sellerId = "seller1"
        val expectedProducts = testProducts.filter { it.sellerId == sellerId }
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .select()
                .eq("sellerId", sellerId)
                .decodeList<Product>()
        } returns expectedProducts
        
        // Act
        val result = repository.getProductsBySeller(sellerId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { products ->
            assertEquals(2, products.size)
            assertTrue(products.all { it.sellerId == sellerId })
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .select()
                .eq("sellerId", sellerId)
                .decodeList<Product>()
        }
    }
    
    @Test
    fun `createProduct should add a new product and return it`() = runTest {
        // Arrange
        val newProduct = testProducts[0].copy(id = "")
        val createdProduct = testProducts[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .insert(newProduct, returning = PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Product>()
        } returns createdProduct
        
        // Act
        val result = repository.createProduct(newProduct)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { product ->
            assertEquals(createdProduct, product)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .insert(newProduct, returning = PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Product>()
        }
    }
    
    @Test
    fun `updateProduct should modify an existing product`() = runTest {
        // Arrange
        val productId = "1"
        val updates = mapOf(
            "price" to 129.99,
            "stock" to 8
        )
        val updatedProduct = testProducts[0].copy(price = 129.99, stock = 8)
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .update(updates)
                .eq("id", productId)
                .returning(PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Product>()
        } returns updatedProduct
        
        // Act
        val result = repository.updateProduct(productId, updates)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { product ->
            assertEquals(updatedProduct, product)
            assertEquals(129.99, product.price, 0.001)
            assertEquals(8, product.stock)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .update(updates)
                .eq("id", productId)
                .returning(PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Product>()
        }
    }
    
    @Test
    fun `deleteProduct should remove a product`() = runTest {
        // Arrange
        val productId = "1"
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .delete()
                .eq("id", productId)
                .execute()
        } returns mockk(relaxed = true)
        
        // Act
        val result = repository.deleteProduct(productId)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .delete()
                .eq("id", productId)
                .execute()
        }
    }
    
    @Test
    fun `searchProducts should return products matching the query`() = runTest {
        // Arrange
        val query = "test"
        val expectedProducts = testProducts.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.description.contains(query, ignoreCase = true)
        }
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .select()
                .or("name.ilike.%$query%,description.ilike.%$query%")
                .decodeList<Product>()
        } returns expectedProducts
        
        // Act
        val result = repository.searchProducts(query)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { products ->
            assertEquals(2, products.size)
            assertTrue(products.all { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true)
            })
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .select()
                .or("name.ilike.%$query%,description.ilike.%$query%")
                .decodeList<Product>()
        }
    }
    
    @Test
    fun `updateStock should modify product stock level`() = runTest {
        // Arrange
        val productId = "1"
        val quantityChange = -2 // Decrease by 2
        val updatedProduct = testProducts[0].copy(stock = 8) // Original was 10
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .select()
                .eq("id", productId)
                .limit(1)
                .decodeSingle<Product>()
        } returns testProducts[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .update(mapOf("stock" to 8))
                .eq("id", productId)
                .returning(PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Product>()
        } returns updatedProduct
        
        // Act
        val result = repository.updateStock(productId, quantityChange)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { product ->
            assertEquals(8, product.stock)
        }
        
        // Verify sequence
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .select()
                .eq("id", productId)
                .limit(1)
                .decodeSingle<Product>()
                
            supabaseClientManager.getClient().from("products")
                .update(mapOf("stock" to 8))
                .eq("id", productId)
                .returning(PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Product>()
        }
    }
    
    @Test
    fun `getProductsWithLowStock should return products below threshold`() = runTest {
        // Arrange
        val threshold = 5
        val expectedProducts = testProducts.filter { it.stock <= threshold }
        
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .select()
                .lte("stock", threshold)
                .decodeList<Product>()
        } returns expectedProducts
        
        // Act
        val result = repository.getProductsWithLowStock(threshold)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { products ->
            assertEquals(2, products.size)
            assertTrue(products.all { it.stock <= threshold })
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .select()
                .lte("stock", threshold)
                .decodeList<Product>()
        }
    }
    
    @Test
    fun `countProducts should return total product count`() = runTest {
        // Arrange
        coEvery { 
            supabaseClientManager.getClient().from("products")
                .count()
                .executeSingle<HashMap<String, Int>>()
        } returns hashMapOf("count" to 3)
        
        // Act
        val result = repository.countProducts()
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { count ->
            assertEquals(3, count)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("products")
                .count()
                .executeSingle<HashMap<String, Int>>()
        }
    }
} 
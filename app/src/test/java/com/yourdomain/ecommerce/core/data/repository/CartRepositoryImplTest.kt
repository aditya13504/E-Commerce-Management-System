package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.local.dao.CartDao
import com.yourdomain.ecommerce.core.data.model.CartItem
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import com.yourdomain.ecommerce.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartRepositoryImplTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var cartRepository: CartRepositoryImpl
    private lateinit var cartDao: CartDao
    private lateinit var productRepository: ProductRepository
    
    // Test data
    private val testProduct1 = Product(
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
    )
    
    private val testProduct2 = Product(
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
    )
    
    private val testCartItem1 = CartItem(
        productId = "1",
        quantity = 2,
        product = testProduct1
    )
    
    private val testCartItem2 = CartItem(
        productId = "2",
        quantity = 1,
        product = testProduct2
    )
    
    private val cartItemsFlow = MutableStateFlow(listOf(testCartItem1, testCartItem2))
    
    @Before
    fun setUp() {
        cartDao = mockk(relaxed = true)
        productRepository = mockk(relaxed = true)
        
        // Mock DAO responses
        coEvery { cartDao.getCartItems() } returns listOf(testCartItem1, testCartItem2)
        coEvery { cartDao.getCartItemsFlow() } returns cartItemsFlow
        coEvery { cartDao.getCartItemCount() } returns 3 // 2 + 1 quantity
        coEvery { cartDao.insertCartItem(any()) } returns Unit
        coEvery { cartDao.updateQuantity(any(), any()) } returns Unit
        coEvery { cartDao.deleteCartItem(any()) } returns Unit
        coEvery { cartDao.clearCart() } returns Unit
        
        // Mock product repository
        coEvery { productRepository.getProductById("1") } returns testProduct1
        coEvery { productRepository.getProductById("2") } returns testProduct2
        
        // Initialize repository
        cartRepository = CartRepositoryImpl(cartDao, productRepository)
    }
    
    @Test
    fun `getCartItems should return items with product details`() = runTest {
        // When getting cart items
        val result = cartRepository.getCartItems()
        
        // Then the result should contain the expected items with product details
        assertEquals(2, result.size)
        assertEquals(testCartItem1.productId, result[0].productId)
        assertEquals(testCartItem1.quantity, result[0].quantity)
        assertEquals(testCartItem1.product, result[0].product)
        assertEquals(testCartItem2.productId, result[1].productId)
    }
    
    @Test
    fun `getCartItemsStream should emit cart items with product details`() = runTest {
        // When collecting from the cart items stream
        val result = cartRepository.getCartItemsStream().first()
        
        // Then the emitted items should contain the expected data
        assertEquals(2, result.size)
        assertEquals(testCartItem1.productId, result[0].productId)
        assertEquals(testCartItem1.product, result[0].product)
    }
    
    @Test
    fun `getCartItemCount should return total items quantity`() = runTest {
        // When getting cart item count
        val result = cartRepository.getCartItemCount()
        
        // Then the result should be the total quantity of items
        assertEquals(3, result) // 2 + 1 = 3
    }
    
    @Test
    fun `addToCart should add new item`() = runTest {
        // Given the item is not in the cart
        coEvery { cartDao.getCartItem("3") } returns null
        
        // When adding to cart
        val result = cartRepository.addToCart("3", 1)
        
        // Then the item should be inserted
        assertTrue(result)
        coVerify { cartDao.insertCartItem(any()) }
    }
    
    @Test
    fun `addToCart should update quantity for existing item`() = runTest {
        // Given the item is already in the cart
        val existingItem = testCartItem1.copy(quantity = 1)
        coEvery { cartDao.getCartItem("1") } returns existingItem
        
        // When adding to cart
        val result = cartRepository.addToCart("1", 2)
        
        // Then the quantity should be updated
        assertTrue(result)
        coVerify { cartDao.updateQuantity("1", 3) } // 1 + 2 = 3
    }
    
    @Test
    fun `updateQuantity should update item quantity`() = runTest {
        // Given an item exists in the cart
        coEvery { cartDao.getCartItem("1") } returns testCartItem1
        
        // When updating quantity
        val result = cartRepository.updateQuantity("1", 5)
        
        // Then the quantity should be updated
        assertTrue(result)
        coVerify { cartDao.updateQuantity("1", 5) }
    }
    
    @Test
    fun `updateQuantity should fail for non-existent item`() = runTest {
        // Given item doesn't exist in cart
        coEvery { cartDao.getCartItem("99") } returns null
        
        // When updating quantity
        val result = cartRepository.updateQuantity("99", 5)
        
        // Then the operation should fail
        assertFalse(result)
        coVerify(exactly = 0) { cartDao.updateQuantity(any(), any()) }
    }
    
    @Test
    fun `removeFromCart should remove item`() = runTest {
        // Given an item exists in the cart
        coEvery { cartDao.getCartItem("1") } returns testCartItem1
        
        // When removing the item
        val result = cartRepository.removeFromCart("1")
        
        // Then the item should be deleted
        assertTrue(result)
        coVerify { cartDao.deleteCartItem("1") }
    }
    
    @Test
    fun `removeFromCart should fail for non-existent item`() = runTest {
        // Given item doesn't exist in cart
        coEvery { cartDao.getCartItem("99") } returns null
        
        // When removing the item
        val result = cartRepository.removeFromCart("99")
        
        // Then the operation should fail
        assertFalse(result)
        coVerify(exactly = 0) { cartDao.deleteCartItem(any()) }
    }
    
    @Test
    fun `clearCart should empty the cart`() = runTest {
        // When clearing the cart
        cartRepository.clearCart()
        
        // Then the cart should be cleared
        coVerify { cartDao.clearCart() }
        
        // And the flow should emit an empty list
        cartItemsFlow.value = emptyList()
        val result = cartRepository.getCartItemsStream().first()
        assertEquals(0, result.size)
    }
} 
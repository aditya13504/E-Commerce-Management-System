package com.yourdomain.ecommerce.presentation.cart

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.yourdomain.ecommerce.core.data.model.CartItem
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.core.domain.repository.CartRepository
import com.yourdomain.ecommerce.core.domain.repository.ProductRepository
import com.yourdomain.ecommerce.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: CartViewModel
    private lateinit var cartRepository: CartRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var savedStateHandle: SavedStateHandle

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
    
    private val cartItemsFlow = MutableStateFlow<List<CartItem>>(emptyList())

    @Before
    fun setUp() {
        cartRepository = mockk(relaxed = true)
        productRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        
        // Mock repository responses
        every { cartRepository.getCartItemsStream() } returns cartItemsFlow
        coEvery { cartRepository.getCartItems() } returns emptyList()
        coEvery { cartRepository.getCartItemCount() } returns 0
        coEvery { cartRepository.addToCart(any(), any()) } returns true
        coEvery { cartRepository.updateQuantity(any(), any()) } returns true
        coEvery { cartRepository.removeFromCart(any()) } returns true
        coEvery { productRepository.getProductById(any()) } answers { 
            val id = firstArg<String>()
            when (id) {
                "1" -> testProduct1
                "2" -> testProduct2
                else -> null
            }
        }
        
        viewModel = CartViewModel(cartRepository, productRepository, savedStateHandle)
    }

    @Test
    fun `initial state should have empty cart and zero total`() = runTest {
        // Check initial state
        assertEquals(CartUiState(), viewModel.state.value)
        assertEquals(0.0, viewModel.state.value.cartTotal, 0.001)
        assertEquals(0, viewModel.state.value.cartItems.size)
    }

    @Test
    fun `adding item to cart should update state correctly`() = runTest {
        // Setup
        coEvery { cartRepository.addToCart("1", 1) } returns true
        
        // Add to cart
        viewModel.addToCart(testProduct1, 1)
        
        // Verify repository call
        coVerify { cartRepository.addToCart("1", 1) }
        
        // Update flow to simulate repository response
        val updatedItems = listOf(testCartItem1)
        cartItemsFlow.value = updatedItems
        
        // Check that state was updated
        viewModel.state.test {
            val emission = awaitItem()
            assertEquals(updatedItems, emission.cartItems)
            assertEquals(21.98, emission.cartTotal, 0.001) // 10.99 * 2
            assertEquals(false, emission.isLoading)
        }
    }
    
    @Test
    fun `removing item from cart should update state correctly`() = runTest {
        // Setup initial cart with items
        cartItemsFlow.value = listOf(testCartItem1, testCartItem2)
        
        // Remove item
        viewModel.removeFromCart(testCartItem1.productId)
        
        // Verify repository call
        coVerify { cartRepository.removeFromCart(testCartItem1.productId) }
        
        // Update flow to simulate repository response
        cartItemsFlow.value = listOf(testCartItem2)
        
        // Check state
        viewModel.state.test {
            val emission = awaitItem()
            assertEquals(1, emission.cartItems.size)
            assertEquals(testCartItem2.productId, emission.cartItems[0].productId)
            assertEquals(20.49, emission.cartTotal, 0.001) // Just the price of testProduct2
        }
    }
    
    @Test
    fun `updating quantity should call repository and update state`() = runTest {
        // Setup initial cart with item
        cartItemsFlow.value = listOf(testCartItem1)
        
        // Update quantity
        val newQuantity = 3
        viewModel.updateQuantity(testCartItem1.productId, newQuantity)
        
        // Verify repository call
        coVerify { cartRepository.updateQuantity(testCartItem1.productId, newQuantity) }
        
        // Update flow to simulate repository response
        val updatedCartItem = testCartItem1.copy(quantity = newQuantity)
        cartItemsFlow.value = listOf(updatedCartItem)
        
        // Check state
        viewModel.state.test {
            val emission = awaitItem()
            assertEquals(1, emission.cartItems.size)
            assertEquals(newQuantity, emission.cartItems[0].quantity)
            assertEquals(32.97, emission.cartTotal, 0.001) // 10.99 * 3
        }
    }
    
    @Test
    fun `clearCart should call repository and update state`() = runTest {
        // Setup initial cart with items
        cartItemsFlow.value = listOf(testCartItem1, testCartItem2)
        
        // Clear cart
        viewModel.clearCart()
        
        // Verify repository call
        coVerify { cartRepository.clearCart() }
        
        // Update flow to simulate repository response
        cartItemsFlow.value = emptyList()
        
        // Check state
        viewModel.state.test {
            val emission = awaitItem()
            assertEquals(0, emission.cartItems.size)
            assertEquals(0.0, emission.cartTotal, 0.001)
        }
    }
    
    @Test
    fun `incrementQuantity should increase item quantity by 1`() = runTest {
        // Setup initial cart with item
        val initialQuantity = testCartItem1.quantity
        cartItemsFlow.value = listOf(testCartItem1)
        
        // Increment quantity
        viewModel.incrementQuantity(testCartItem1.productId)
        
        // Verify repository call
        coVerify { cartRepository.updateQuantity(testCartItem1.productId, initialQuantity + 1) }
    }
    
    @Test
    fun `decrementQuantity should decrease item quantity by 1`() = runTest {
        // Setup initial cart with item
        val initialQuantity = testCartItem1.quantity
        cartItemsFlow.value = listOf(testCartItem1)
        
        // Decrement quantity
        viewModel.decrementQuantity(testCartItem1.productId)
        
        // Verify repository call
        coVerify { cartRepository.updateQuantity(testCartItem1.productId, initialQuantity - 1) }
    }
    
    @Test
    fun `decrementQuantity should remove item when quantity would become 0`() = runTest {
        // Setup cart with item that has quantity 1
        val itemWithQuantityOne = testCartItem1.copy(quantity = 1)
        cartItemsFlow.value = listOf(itemWithQuantityOne)
        
        // Decrement quantity
        viewModel.decrementQuantity(itemWithQuantityOne.productId)
        
        // Should call remove instead of update
        coVerify { cartRepository.removeFromCart(itemWithQuantityOne.productId) }
        coVerify(exactly = 0) { cartRepository.updateQuantity(any(), any()) }
    }
    
    @Test
    fun `total calculation should handle empty cart`() = runTest {
        // Setup empty cart
        cartItemsFlow.value = emptyList()
        
        // Check total
        viewModel.state.test {
            assertEquals(0.0, awaitItem().cartTotal, 0.001)
        }
    }
    
    @Test
    fun `total calculation should handle multiple items`() = runTest {
        // Setup cart with multiple items
        cartItemsFlow.value = listOf(testCartItem1, testCartItem2)
        
        // Expected total: (10.99 * 2) + (20.49 * 1) = 42.47
        val expectedTotal = (testProduct1.price * testCartItem1.quantity) + 
                           (testProduct2.price * testCartItem2.quantity)
        
        // Check total calculation
        viewModel.state.test {
            assertEquals(expectedTotal, awaitItem().cartTotal, 0.001)
        }
    }
} 
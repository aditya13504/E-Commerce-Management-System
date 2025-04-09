package com.yourdomain.ecommerce.presentation.cart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.yourdomain.ecommerce.core.domain.model.CartItem
import com.yourdomain.ecommerce.ui.theme.ECommerceTheme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class CartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockViewModel = mockk<CartViewModel>(relaxed = true)

    // Test data
    private val testCartItems = listOf(
        CartItem(
            id = "item1",
            productId = "product1",
            productName = "Smartphone X",
            quantity = 1,
            price = BigDecimal("499.99"),
            imageUrl = "https://example.com/smartphone.jpg"
        ),
        CartItem(
            id = "item2",
            productId = "product2",
            productName = "Wireless Headphones",
            quantity = 2,
            price = BigDecimal("89.99"),
            imageUrl = "https://example.com/headphones.jpg"
        )
    )

    @Test
    fun cartScreen_displaysItems_whenCartHasItems() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            totalPrice = BigDecimal("679.97") // 499.99 + (89.99 * 2)
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Smartphone X").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wireless Headphones").assertIsDisplayed()
        composeTestRule.onNodeWithText("$499.99").assertIsDisplayed()
        composeTestRule.onNodeWithText("$89.99").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total: $679.97").assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_button").assertIsEnabled()
    }

    @Test
    fun cartScreen_displaysEmptyState_whenCartIsEmpty() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = emptyList(),
            isLoading = false,
            totalPrice = BigDecimal.ZERO
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Your cart is empty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start shopping to add items to your cart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_button").assertIsNotEnabled()
    }

    @Test
    fun cartScreen_incrementsQuantity_whenPlusButtonClicked() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            totalPrice = BigDecimal("679.97")
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Find the increment button for the first item and click it
        composeTestRule.onNodeWithContentDescription("Increase quantity for Smartphone X").performClick()

        // Verify the ViewModel was called to update the quantity
        coVerify { mockViewModel.updateCartItemQuantity("item1", 2) }
    }

    @Test
    fun cartScreen_decrementsQuantity_whenMinusButtonClicked() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            totalPrice = BigDecimal("679.97")
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Find the decrement button for the second item (which has quantity=2) and click it
        composeTestRule.onNodeWithContentDescription("Decrease quantity for Wireless Headphones").performClick()

        // Verify the ViewModel was called to update the quantity
        coVerify { mockViewModel.updateCartItemQuantity("item2", 1) }
    }

    @Test
    fun cartScreen_removesItem_whenDeleteButtonClicked() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            totalPrice = BigDecimal("679.97")
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Find the remove button for the first item and click it
        composeTestRule.onNodeWithContentDescription("Remove Smartphone X from cart").performClick()

        // Verify the ViewModel was called to remove the item
        coVerify { mockViewModel.removeFromCart("item1") }
    }

    @Test
    fun cartScreen_navigatesToCheckout_whenCheckoutButtonClicked() {
        // Arrange
        var navigateToCheckoutCalled = false
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            totalPrice = BigDecimal("679.97")
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = { navigateToCheckoutCalled = true },
                    viewModel = mockViewModel
                )
            }
        }

        // Find checkout button and click it
        composeTestRule.onNodeWithTag("checkout_button").performClick()

        // Verify navigation was triggered
        assert(navigateToCheckoutCalled)
    }

    @Test
    fun cartScreen_showsLoadingIndicator_whenLoading() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = emptyList(),
            isLoading = true,
            totalPrice = BigDecimal.ZERO
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun cartScreen_displaysErrorMessage_whenErrorOccurs() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = emptyList(),
            isLoading = false,
            error = "Failed to load cart",
            totalPrice = BigDecimal.ZERO
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Failed to load cart").assertIsDisplayed()
    }

    @Test
    fun cartScreen_refreshesCart_onPullToRefresh() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            isRefreshing = false,
            totalPrice = BigDecimal("679.97")
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Simulate pull-to-refresh action
        composeTestRule.onNodeWithTag("cart_refresh").performTouchInput {
            swipeDown()
        }

        // Verify refresh was triggered
        coVerify { mockViewModel.refreshCart() }
    }

    @Test
    fun quantityLabel_showsCorrectValue_forEachItem() {
        // Arrange
        val uiState = MutableStateFlow(CartUiState(
            cartItems = testCartItems,
            isLoading = false,
            totalPrice = BigDecimal("679.97")
        ))
        coEvery { mockViewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                CartScreen(
                    onNavigateUp = {},
                    onNavigateToCheckout = {},
                    viewModel = mockViewModel
                )
            }
        }

        // Assert - Check quantity displays
        composeTestRule.onNodeWithTag("quantity_item1").assertTextEquals("1")
        composeTestRule.onNodeWithTag("quantity_item2").assertTextEquals("2")
    }
} 
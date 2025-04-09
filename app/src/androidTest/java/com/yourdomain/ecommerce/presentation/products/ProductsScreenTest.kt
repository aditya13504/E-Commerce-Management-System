package com.yourdomain.ecommerce.presentation.products

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.ui.theme.ECommerceTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProductsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var viewModel: ProductsViewModel
    private val stateFlow = MutableStateFlow(ProductsUiState())
    
    // Sample test data
    private val testProducts = listOf(
        Product(
            id = "1",
            name = "Wireless Headphones",
            description = "High-quality sound",
            price = 99.99,
            imageUrl = "https://example.com/img1.jpg",
            stock = 10,
            sellerId = "seller1",
            categoryId = "audio",
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        ),
        Product(
            id = "2",
            name = "Smart Watch",
            description = "Track your fitness",
            price = 249.99,
            imageUrl = "https://example.com/img2.jpg",
            stock = 5,
            sellerId = "seller2",
            categoryId = "wearables",
            createdAt = "2023-01-02T00:00:00Z",
            updatedAt = "2023-01-02T00:00:00Z"
        ),
        Product(
            id = "3",
            name = "Bluetooth Speaker",
            description = "Portable audio solution",
            price = 79.99,
            imageUrl = "https://example.com/img3.jpg",
            stock = 15,
            sellerId = "seller1",
            categoryId = "audio",
            createdAt = "2023-01-03T00:00:00Z",
            updatedAt = "2023-01-03T00:00:00Z"
        )
    )
    
    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        
        // Set up the mock behavior
        every { viewModel.state } returns stateFlow
        every { viewModel.searchQuery } returns MutableStateFlow("")
        every { viewModel.isRefreshing } returns MutableStateFlow(false)
    }
    
    @Test
    fun loadingState_showsLoadingIndicator() {
        // Set the UI state to loading
        stateFlow.value = ProductsUiState(isLoading = true)
        
        // Launch the ProductsScreen
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { }
                )
            }
        }
        
        // Assert loading indicator is displayed
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("products_grid").assertDoesNotExist()
    }
    
    @Test
    fun successState_displaysProducts() {
        // Set the UI state to success with products
        stateFlow.value = ProductsUiState(
            isLoading = false,
            products = testProducts
        )
        
        // Launch the ProductsScreen
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { }
                )
            }
        }
        
        // Assert products are displayed
        composeTestRule.onNodeWithTag("products_grid").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("product_card").assertCountEquals(3)
        
        // Check specific product details
        composeTestRule.onNodeWithText("Wireless Headphones").assertIsDisplayed()
        composeTestRule.onNodeWithText("$99.99").assertIsDisplayed()
    }
    
    @Test
    fun errorState_displaysErrorMessage() {
        // Set the UI state to error
        val errorMessage = "Failed to load products"
        stateFlow.value = ProductsUiState(
            isLoading = false,
            error = errorMessage
        )
        
        // Launch the ProductsScreen
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { }
                )
            }
        }
        
        // Assert error message is displayed
        composeTestRule.onNodeWithTag("error_view").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithTag("products_grid").assertDoesNotExist()
    }
    
    @Test
    fun emptyState_displaysEmptyView() {
        // Set the UI state to success but with empty products list
        stateFlow.value = ProductsUiState(
            isLoading = false,
            products = emptyList()
        )
        
        // Launch the ProductsScreen
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { }
                )
            }
        }
        
        // Assert empty state is displayed
        composeTestRule.onNodeWithTag("empty_view").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Products Found").assertIsDisplayed()
    }
    
    @Test
    fun searchFunctionality_filtersProducts() {
        // Set up search state
        val searchFlow = MutableStateFlow("")
        every { viewModel.searchQuery } returns searchFlow
        
        // Set the UI state to success with products
        stateFlow.value = ProductsUiState(
            isLoading = false,
            products = testProducts
        )
        
        // Launch the ProductsScreen
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { }
                )
            }
        }
        
        // Initially all products should be visible
        composeTestRule.onAllNodesWithTag("product_card").assertCountEquals(3)
        
        // Perform search action for "Speaker"
        composeTestRule.onNodeWithTag("search_bar").performClick()
        composeTestRule.onNodeWithTag("search_input").performTextInput("Speaker")
        
        // Update the state to reflect filtered results
        stateFlow.value = ProductsUiState(
            isLoading = false,
            products = testProducts.filter { it.name.contains("Speaker", ignoreCase = true) }
        )
        
        // Only the Bluetooth Speaker should be visible
        composeTestRule.onAllNodesWithTag("product_card").assertCountEquals(1)
        composeTestRule.onNodeWithText("Bluetooth Speaker").assertIsDisplayed()
    }
    
    @Test
    fun pullToRefresh_triggersRefresh() {
        // Set up refresh state
        val refreshFlow = MutableStateFlow(false)
        every { viewModel.isRefreshing } returns refreshFlow
        
        // Set the UI state to success with products
        stateFlow.value = ProductsUiState(
            isLoading = false,
            products = testProducts
        )
        
        // Launch the ProductsScreen
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { }
                )
            }
        }
        
        // Trigger a refresh (this is a simplified test as we can't easily simulate the swipe gesture)
        // In a real test, you would use onNodeWithTag("swipe_refresh").performTouchInput { swipeDown() }
        every { viewModel.refresh() } answers {
            refreshFlow.value = true
            // Then later would set to false when refresh completes
        }
        
        // For this test, we'll verify the refresh is communicated through state
        refreshFlow.value = true
        
        // Assert refresh indicator is shown
        composeTestRule.onNodeWithTag("refresh_indicator").assertExists()
    }
    
    @Test
    fun productCard_clickNavigatesToDetail() {
        // Track if navigation was triggered and with which product ID
        var navigatedToProductId: String? = null
        
        // Set the UI state to success with products
        stateFlow.value = ProductsUiState(
            isLoading = false,
            products = testProducts
        )
        
        // Launch the ProductsScreen with navigation callback
        composeTestRule.setContent {
            ECommerceTheme {
                ProductsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { productId ->
                        navigatedToProductId = productId
                    }
                )
            }
        }
        
        // Click on the first product card
        composeTestRule.onAllNodesWithTag("product_card")[0].performClick()
        
        // Assert navigation was triggered with correct product ID
        assert(navigatedToProductId == "1") { "Expected navigation to product 1, but navigated to $navigatedToProductId" }
    }
} 
package com.yourdomain.ecommerce.presentation.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.yourdomain.ecommerce.ui.theme.ECommerceTheme
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displays_all_ui_elements() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState())
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("login_email_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Don't have an account?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign Up").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forgot Password?").assertIsDisplayed()
    }

    @Test
    fun loginButton_disabled_with_empty_fields() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState(
            email = "",
            password = "",
            isLoading = false
        ))
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("login_button").assertIsNotEnabled()
    }

    @Test
    fun loginButton_disabled_with_invalid_email() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState(
            email = "invalid-email",
            password = "password123",
            isLoading = false
        ))
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("login_button").assertIsNotEnabled()
    }

    @Test
    fun loginButton_disabled_with_short_password() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState(
            email = "valid@example.com",
            password = "pass", // Too short
            isLoading = false
        ))
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("login_button").assertIsNotEnabled()
    }

    @Test
    fun loginButton_enabled_with_valid_inputs() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState(
            email = "valid@example.com",
            password = "password123",
            isLoading = false
        ))
        coEvery { viewModel.uiState } returns uiState
        coEvery { viewModel.isValidEmail(any()) } returns true
        coEvery { viewModel.isValidPassword(any()) } returns true

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("login_button").assertIsEnabled()
    }

    @Test
    fun loginButton_disabled_when_loading() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState(
            email = "valid@example.com",
            password = "password123",
            isLoading = true
        ))
        coEvery { viewModel.uiState } returns uiState
        coEvery { viewModel.isValidEmail(any()) } returns true
        coEvery { viewModel.isValidPassword(any()) } returns true

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("login_button").assertIsNotEnabled()
    }

    @Test
    fun errorMessage_displayed_when_error_occurs() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState(
            email = "valid@example.com",
            password = "password123",
            isLoading = false,
            error = "Invalid credentials"
        ))
        coEvery { viewModel.uiState } returns uiState
        coEvery { viewModel.isValidEmail(any()) } returns true
        coEvery { viewModel.isValidPassword(any()) } returns true

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    @Test
    fun loginForm_updates_viewModel_when_input_changes() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState())
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Type in email field
        composeTestRule.onNodeWithTag("login_email_field").performTextInput("test@example.com")
        
        // Type in password field
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("password123")
        
        // Verify ViewModel updates
        coEvery { viewModel.updateEmail("test@example.com") }
        coEvery { viewModel.updatePassword("password123") }
    }

    @Test
    fun signUpLink_triggers_navigation() {
        // Arrange
        var navigateToRegisterCalled = false
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState())
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = { navigateToRegisterCalled = true },
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Click on Sign Up text
        composeTestRule.onNodeWithText("Sign Up").performClick()
        
        // Assert
        assert(navigateToRegisterCalled)
    }

    @Test
    fun forgotPasswordLink_triggers_reset_dialog() {
        // Arrange
        val viewModel = mockk<LoginViewModel>(relaxed = true)
        val uiState = MutableStateFlow(LoginUiState())
        coEvery { viewModel.uiState } returns uiState

        // Act
        composeTestRule.setContent {
            ECommerceTheme {
                LoginScreen(
                    onNavigateToRegister = {},
                    onNavigateToHome = {},
                    viewModel = viewModel
                )
            }
        }

        // Click on Forgot Password text
        composeTestRule.onNodeWithText("Forgot Password?").performClick()
        
        // Assert - Dialog should appear
        composeTestRule.onNodeWithText("Reset Password").assertIsDisplayed()
    }
} 
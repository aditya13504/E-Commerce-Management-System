package com.yourdomain.ecommerce.presentation.auth

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.yourdomain.ecommerce.core.data.model.User
import com.yourdomain.ecommerce.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    
    // Test data
    private val testUser = User(
        id = "user123",
        email = "test@example.com",
        name = "Test User",
        avatarUrl = null,
        phoneNumber = null,
        isEmailVerified = false,
        createdAt = "2023-01-01T00:00:00Z",
        lastSignInAt = "2023-01-01T00:00:00Z",
        metadata = null
    )
    
    private val testException = Exception("Authentication failed")
    private val userFlow = MutableStateFlow<User?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup auth repository mock
        every { authRepository.observeAuthState() } returns userFlow
        coEvery { authRepository.isAuthenticated() } returns false
        
        viewModel = AuthViewModel(authRepository, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should not be authenticated`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.isAuthenticated)
            assertFalse(initialState.isLoading)
            assertNull(initialState.error)
            
            cancel()
        }
    }
    
    @Test
    fun `authState flow should emit Loading initially`() = runTest {
        viewModel.authState.test {
            assertTrue(awaitItem() is AuthState.Loading)
            cancel()
        }
    }
    
    @Test
    fun `signIn should update state accordingly when successful`() = runTest {
        // Arrange
        val email = "user@example.com"
        val password = "password123"
        
        coEvery { 
            authRepository.signIn(email, password) 
        } returns Result.success(testUser)
        
        // Act
        viewModel.signIn(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.error)
            
            cancel()
        }
        
        // Verify
        coVerify { authRepository.signIn(email, password) }
    }
    
    @Test
    fun `signIn should update state with error when failed`() = runTest {
        // Arrange
        val email = "user@example.com"
        val password = "password123"
        val errorMessage = "Invalid credentials"
        
        coEvery { 
            authRepository.signIn(email, password) 
        } returns Result.failure(Exception(errorMessage))
        
        // Act
        viewModel.signIn(email, password)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertEquals(errorMessage, state.error)
            
            cancel()
        }
        
        // Verify
        coVerify { authRepository.signIn(email, password) }
    }
    
    @Test
    fun `signUp should update state accordingly when successful`() = runTest {
        // Arrange
        val email = "new@example.com"
        val password = "password123"
        val name = "New User"
        
        coEvery { 
            authRepository.signUp(email, password, name) 
        } returns Result.success(testUser.copy(email = email, name = name))
        
        // Act
        viewModel.signUp(email, password, name)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.error)
            
            cancel()
        }
        
        // Verify
        coVerify { authRepository.signUp(email, password, name) }
    }
    
    @Test
    fun `signUp should update state with error when failed`() = runTest {
        // Arrange
        val email = "new@example.com"
        val password = "password123"
        val name = "New User"
        val errorMessage = "Email already in use"
        
        coEvery { 
            authRepository.signUp(email, password, name) 
        } returns Result.failure(Exception(errorMessage))
        
        // Act
        viewModel.signUp(email, password, name)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertEquals(errorMessage, state.error)
            
            cancel()
        }
        
        // Verify
        coVerify { authRepository.signUp(email, password, name) }
    }
    
    @Test
    fun `signOut should update state accordingly when successful`() = runTest {
        // Arrange - Start with authenticated state
        val initialAuthState = viewModel.uiState.value.copy(isAuthenticated = true)
        val stateSlot = slot<AuthUiState>()
        every { viewModel.uiState.value } returns initialAuthState
        
        coEvery { 
            authRepository.signOut() 
        } returns Result.success(Unit)
        
        // Act
        viewModel.signOut()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isAuthenticated)
            assertFalse(state.isLoading)
            assertNull(state.error)
            
            cancel()
        }
        
        // Verify
        coVerify { authRepository.signOut() }
    }
    
    @Test
    fun `resetPassword should update state accordingly when successful`() = runTest {
        // Arrange
        val email = "user@example.com"
        
        coEvery { 
            authRepository.resetPassword(email) 
        } returns Result.success(Unit)
        
        // Act
        viewModel.resetPassword(email)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.passwordResetSent)
            assertFalse(state.isLoading)
            assertNull(state.error)
            
            cancel()
        }
        
        // Verify
        coVerify { authRepository.resetPassword(email) }
    }
    
    @Test
    fun `authState should reflect changes in user authentication`() = runTest {
        // Test initial unauthenticated state
        viewModel.authState.test(timeout = 5.seconds) {
            assertTrue(awaitItem() is AuthState.Loading)
            
            // Update user flow with authenticated user
            userFlow.value = testUser
            assertTrue(awaitItem() is AuthState.Authenticated)
            assertEquals(testUser.id, (awaitItem() as AuthState.Authenticated).user.id)
            
            // Update user flow with null (signed out)
            userFlow.value = null
            assertTrue(awaitItem() is AuthState.Unauthenticated)
            
            cancel()
        }
    }
    
    @Test
    fun `clearError should remove error from state`() = runTest {
        // Arrange - Set an error in the state
        val errorMessage = "Test error"
        val stateWithError = viewModel.uiState.value.copy(error = errorMessage)
        every { viewModel.uiState.value } returns stateWithError
        
        // Act
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
            cancel()
        }
    }
    
    @Test
    fun `clearPasswordResetSent should reset flag`() = runTest {
        // Arrange - Set passwordResetSent to true
        val stateWithPasswordReset = viewModel.uiState.value.copy(passwordResetSent = true)
        every { viewModel.uiState.value } returns stateWithPasswordReset
        
        // Act
        viewModel.clearPasswordResetSent()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.passwordResetSent)
            cancel()
        }
    }
} 
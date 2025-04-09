package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.core.data.model.User
import com.yourdomain.ecommerce.util.MainDispatcherRule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.AuthResponse
import io.github.jan.supabase.gotrue.AuthStateChangeEvent
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.SignOutScope
import io.github.jan.supabase.gotrue.SignUpResponse
import io.github.jan.supabase.gotrue.UserInfo
import io.github.jan.supabase.gotrue.UserSession
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AuthRepositoryImpl
    private val supabaseClientManager: SupabaseClientManager = mockk()
    private val auth: GoTrue = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    // Test data
    private val testUserInfo = UserInfo(
        id = "user123",
        email = "test@example.com",
        userMetadata = mapOf("name" to "Test User"),
        appMetadata = emptyMap(),
        aud = "authenticated",
        createdAt = Instant.parse("2023-01-01T00:00:00Z").toString(),
        confirmedAt = Instant.parse("2023-01-01T00:00:00Z").toString(),
        lastSignInAt = Instant.parse("2023-01-01T00:00:00Z").toString(),
        role = "user",
        updatedAt = Instant.parse("2023-01-01T00:00:00Z").toString()
    )

    private val testUserSession = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        tokenType = "bearer", 
        expiresIn = 3600,
        expiresAt = Instant.now().plusSeconds(3600).epochSecond,
        user = testUserInfo
    )

    private val testUser = User(
        id = "user123",
        email = "test@example.com",
        name = "Test User",
        avatarUrl = null,
        phoneNumber = null,
        isEmailVerified = true,
        createdAt = "2023-01-01T00:00:00Z",
        lastSignInAt = "2023-01-01T00:00:00Z",
        metadata = null
    )

    private val sessionStatusFlow = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated())

    @Before
    fun setUp() {
        coEvery { supabaseClientManager.getClient().auth } returns auth
        every { auth.sessionStatus } returns sessionStatusFlow
        
        repository = AuthRepositoryImpl(supabaseClientManager, testDispatcher)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `signIn should authenticate user with email and password`() = runTest {
        // Arrange
        val email = "test@example.com"
        val password = "password123"
        
        val authResponse = mockk<AuthResponse>()
        every { authResponse.user } returns testUserInfo
        every { authResponse.session } returns testUserSession
        
        coEvery { 
            auth.signInWith {
                email(email, password)
            }
        } returns authResponse
        
        // Act
        val result = repository.signIn(email, password)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { user ->
            assertEquals(testUser.id, user.id)
            assertEquals(testUser.email, user.email)
            assertEquals(testUser.name, user.name)
        }
        
        // Verify
        coVerify { 
            auth.signInWith {
                email(email, password)
            }
        }
    }
    
    @Test
    fun `signUp should register user with email, password and name`() = runTest {
        // Arrange
        val email = "new@example.com"
        val password = "password123"
        val name = "New User"
        
        val signUpResponse = mockk<SignUpResponse.EmailConfirmation>()
        val authResponse = mockk<AuthResponse>()
        
        // Updated UserInfo with new email and name
        val newUserInfo = testUserInfo.copy(
            email = email,
            userMetadata = mapOf("name" to name)
        )
        
        every { authResponse.user } returns newUserInfo
        every { authResponse.session } returns testUserSession.copy(user = newUserInfo)
        
        coEvery { 
            auth.signUpWith {
                email(email, password, data = mapOf("name" to name))
            }
        } returns signUpResponse
        
        coEvery {
            auth.signInWith {
                email(email, password)
            }
        } returns authResponse
        
        // Act
        val result = repository.signUp(email, password, name)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { user ->
            assertEquals(testUser.id, user.id)
            assertEquals(email, user.email)
            assertEquals(name, user.name)
        }
        
        // Verify
        coVerify { 
            auth.signUpWith {
                email(email, password, data = mapOf("name" to name))
            }
            auth.signInWith {
                email(email, password)
            }
        }
    }
    
    @Test
    fun `signOut should log out the current user`() = runTest {
        // Arrange
        val scope = slot<SignOutScope.() -> Unit>()
        
        coEvery { 
            auth.signOut(capture(scope))
        } just runs
        
        // Set initial state to authenticated
        sessionStatusFlow.value = SessionStatus.Authenticated(testUserSession)
        
        // Act
        val result = repository.signOut()
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify
        coVerify { auth.signOut(any()) }
        
        // Confirm scope captures global sign out
        scope.captured.invoke(mockk<SignOutScope>())
        coVerify { any<SignOutScope>().global = true }
    }
    
    @Test
    fun `resetPassword should send reset password email`() = runTest {
        // Arrange
        val email = "test@example.com"
        
        coEvery { 
            auth.resetPasswordForEmail(email)
        } just runs
        
        // Act
        val result = repository.resetPassword(email)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify
        coVerify { auth.resetPasswordForEmail(email) }
    }
    
    @Test
    fun `isAuthenticated should return authentication status`() = runTest {
        // Test case 1: Not authenticated
        sessionStatusFlow.value = SessionStatus.NotAuthenticated()
        
        // Act
        val result1 = repository.isAuthenticated()
        
        // Assert
        assertFalse(result1)
        
        // Test case 2: Authenticated
        sessionStatusFlow.value = SessionStatus.Authenticated(testUserSession)
        
        // Act
        val result2 = repository.isAuthenticated()
        
        // Assert
        assertTrue(result2)
    }
    
    @Test
    fun `getCurrentUser should return current authenticated user or null`() = runTest {
        // Test case 1: Not authenticated
        sessionStatusFlow.value = SessionStatus.NotAuthenticated()
        
        // Act
        val result1 = repository.getCurrentUser()
        
        // Assert
        assertNull(result1)
        
        // Test case 2: Authenticated
        sessionStatusFlow.value = SessionStatus.Authenticated(testUserSession)
        
        // Act
        val result2 = repository.getCurrentUser()
        
        // Assert
        assertEquals(testUser.id, result2?.id)
        assertEquals(testUser.email, result2?.email)
        assertEquals(testUser.name, result2?.name)
    }
    
    @Test
    fun `observeAuthState should emit auth state changes`() = runTest {
        // Initialize with not authenticated state
        sessionStatusFlow.value = SessionStatus.NotAuthenticated()
        
        // Collect the flow in a test coroutine
        val authStateFlow = repository.observeAuthState()
        
        // Initial state should be null (not authenticated)
        assertNull(authStateFlow.first())
        
        // Change to authenticated state
        sessionStatusFlow.value = SessionStatus.Authenticated(testUserSession)
        
        // Flow should emit the user
        val emittedUser = authStateFlow.first()
        assertEquals(testUser.id, emittedUser?.id)
        assertEquals(testUser.email, emittedUser?.email)
        
        // Change back to not authenticated
        sessionStatusFlow.value = SessionStatus.NotAuthenticated()
        
        // Flow should emit null
        assertNull(authStateFlow.first())
    }
    
    @Test
    fun `getCurrentSession should return current session or null`() = runTest {
        // Test case 1: Not authenticated
        sessionStatusFlow.value = SessionStatus.NotAuthenticated()
        
        // Act
        val result1 = repository.getCurrentSession()
        
        // Assert
        assertNull(result1)
        
        // Test case 2: Authenticated
        sessionStatusFlow.value = SessionStatus.Authenticated(testUserSession)
        
        // Act
        val result2 = repository.getCurrentSession()
        
        // Assert
        assertEquals(testUserSession, result2)
    }
    
    @Test
    fun `updateUserProfile should update user metadata`() = runTest {
        // Arrange
        val updates = mapOf(
            "name" to "Updated Name",
            "avatar_url" to "https://example.com/avatar.jpg"
        )
        
        val updatedUserInfo = testUserInfo.copy(
            userMetadata = testUserInfo.userMetadata.toMutableMap().apply { 
                put("name", "Updated Name")
                put("avatar_url", "https://example.com/avatar.jpg")
            }
        )
        
        val updatedUserSession = testUserSession.copy(user = updatedUserInfo)
        
        coEvery { 
            auth.modifyUser {
                data = updates
            }
        } returns updatedUserInfo
        
        // Set state to authenticated with original user
        sessionStatusFlow.value = SessionStatus.Authenticated(testUserSession)
        
        // Act
        val result = repository.updateUserProfile(updates)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { user ->
            assertEquals("Updated Name", user.name)
            assertEquals("https://example.com/avatar.jpg", user.avatarUrl)
        }
        
        // Verify
        coVerify { 
            auth.modifyUser {
                data = updates
            }
        }
    }
} 
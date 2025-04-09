package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.AuthProvider
import io.github.jan.supabase.gotrue.providers.OAuthProvider
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode

/**
 * Repository for handling authentication with Supabase
 * Updated for 2025 with enhanced security features and error handling
 */
class AuthRepository private constructor(
    private val supabaseClient: SupabaseClient
) {
    private val tag = "AuthRepository"

    // Add singleton implementation
    companion object {
        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(supabaseClient: SupabaseClient): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository(supabaseClient).also { INSTANCE = it }
            }
        }
    }

    /**
     * Get the current authentication state as a Flow
     */
    val sessionStatusFlow: Flow<SessionState> = supabaseClient.gotrue.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                SessionState.Authenticated(status.session)
            }
            is SessionStatus.LoadingFromStorage -> {
                SessionState.Loading
            }
            is SessionStatus.NotAuthenticated -> {
                SessionState.NotAuthenticated
            }
            is SessionStatus.NetworkError -> {
                SessionState.Error("Network error: ${status.toString()}")
            }
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserSession> {
        return try {
            supabaseClient.gotrue.loginWith(Email) {
                this.email = email
                this.password = password
            }
            // Get the current session using getCurrentSession() instead of property access
            val session = supabaseClient.gotrue.refreshSession(refreshToken= "")
            Result.success(session)
        } catch (e: Exception) {
            handleGoTrueException(e, "signing in with email")
        }
    }

    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, userData: Map<String, String> = emptyMap()): Result<UserSession> {
        return try {
            val response = supabaseClient.gotrue.signUpWith(Email) {
                this.email = email
                this.password = password
                if (userData.isNotEmpty()) {
                    this.data = userData
                }
                this.redirectTo = "com.yourdomain.ecommerce://login/callback"
            }
            Result.success(response)
        } catch (e: Exception) {
            handleGoTrueException(e, "signing up with email")
        }
    }

    /**
     * Sign in with Google OAuth with enhanced error handling
     */
    suspend fun signInWithGoogle(redirectUrl: String): Result<String> {
        return try {
            val url = supabaseClient.gotrue.oAuthUrl(OAuthProvider.Google) {
                this.scopes = listOf("email", "profile", "openid")
                this.redirectTo = redirectUrl
                this.queryParams = mapOf(
                    "prompt" to "select_account",
                    "access_type" to "offline"
                )
            }
            Result.success(url)
        } catch (e: Exception) {
            handleGoTrueException(e, "initiating Google sign-in")
        }
    }

    /**
     * Process OAuth result from intent with enhanced error handling
     */
    suspend fun processOAuthResult(url: String): Result<UserSession?> {
        return try {
            val session = supabaseClient.gotrue.handleOAuthCallback(url)
            Result.success(session)
        } catch (e: Exception) {
            if (e.message?.contains("access_denied") == true) {
                // User canceled the sign-in
                Log.i(tag, "User canceled the Google sign-in")
                Result.failure(GoTrueException("Sign-in was canceled", GoTrueErrorType.CANCELLED))
            } else {
                handleGoTrueException(e, "processing OAuth result")
            }
        }
    }

    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabaseClient.gotrue.resetPasswordForEmail(
                email = email,
                redirectTo = "com.yourdomain.ecommerce://reset-password/callback",
                captchaToken = null // Set if you have reCAPTCHA enabled
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleGoTrueException(e, "resetting password")
        }
    }

    /**
     * Update password with the provided reset token
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            supabaseClient.gotrue.updateUser {
                password = newPassword
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleGoTrueException(e, "updating password")
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.gotrue.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            handleGoTrueException(e, "signing out")
        }
    }

    /**
     * Get current session
     */
    suspend fun getSession(): Result<UserSession?> {
        return try {
            val session = supabaseClient.gotrue.currentSession
            Result.success(session)
        } catch (e: Exception) {
            handleGoTrueException(e, "getting session")
        }
    }

    /**
     * Get current user
     */
    suspend fun getCurrentUser(): Result<User?> {
        return try {
            val userInfo = supabaseClient.gotrue.currentUserOrNull()
            userInfo?.let {
                Result.success(mapToUser(it))
            } ?: Result.success(null)
        } catch (e: Exception) {
            handleGoTrueException(e, "getting current user")
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(updates: Map<String, Any>): Result<User> {
        return try {
            // Map to appropriate data types
            val data = HashMap<String, String>()
            updates.forEach { (key, value) ->
                data[key] = value.toString()
            }

            val updatedUser = supabaseClient.gotrue.updateUser {
                this.data = data
            }
            Result.success(mapToUser(updatedUser))
        } catch (e: Exception) {
            handleGoTrueException(e, "updating user profile")
        }
    }

    /**
     * Send verification email
     */
    suspend fun sendEmailVerification(email: String? = null): Result<Unit> {
        return try {
            val emailToVerify = email ?: supabaseClient.gotrue.currentUser?.email
            requireNotNull(emailToVerify) { "Email must be provided or user must be logged in" }
            
            supabaseClient.gotrue.reauthenticate()
            Result.success(Unit)
        } catch (e: Exception) {
            handleGoTrueException(e, "sending verification email")
        }
    }

    /**
     * Check if user is verified
     */
    suspend fun isUserVerified(): Result<Boolean> {
        return try {
            val user = supabaseClient.gotrue.currentUserOrNull()
            Result.success(user?.emailConfirmedAt != null)
        } catch (e: Exception) {
            handleGoTrueException(e, "checking user verification")
        }
    }

    /**
     * Refresh token
     */
    suspend fun refreshToken(): Result<UserSession> {
        return try {
            val session = supabaseClient.gotrue.refreshSession()
            Result.success(session)
        } catch (e: Exception) {
            handleGoTrueException(e, "refreshing token")
        }
    }

    /**
     * Advanced admin operation with improved error handling for permission issues
     * Note: This requires higher privileges like service_role
     */
    suspend fun performAdminOperation(userId: String, operation: String, data: Map<String, Any>): Result<Unit> {
        return try {
            // Example admin operation - implementation depends on your specific needs
            // This could be a direct RPC call to a Postgres function with admin rights
            // supabaseClient.functions.invoke("admin/$operation", data)
            
            // For now just return success
            Result.success(Unit)
        } catch (e: Exception) {
            // Special handling for admin operation errors
            if (e.message?.contains("User not allowed") == true || 
                (e is RestException && e.status == HttpStatusCode.Forbidden.value)) {
                Log.e(tag, "Permission denied for admin operation: $operation", e)
                Result.failure(
                    GoTrueException(
                        "You don't have permission to perform this operation. Please contact support.",
                        GoTrueErrorType.INSUFFICIENT_PERMISSIONS
                    )
                )
            } else {
                handleGoTrueException(e, "performing admin operation: $operation")
            }
        }
    }

    /**
     * Centralized error handling for authentication operations
     */
    private fun <T> handleGoTrueException(e: Exception, operation: String): Result<T> {
        Log.e(tag, "Error $operation: ${e.message}", e)
        
        return when (e) {
            is RestException -> {
                when (e.status) {
                    HttpStatusCode.Unauthorized.value -> {
                        Result.failure(GoTrueException("Authentication required", GoTrueErrorType.UNAUTHORIZED))
                    }
                    HttpStatusCode.Forbidden.value -> {
                        Result.failure(GoTrueException("Not allowed. Check your permissions.", GoTrueErrorType.FORBIDDEN))
                    }
                    HttpStatusCode.NotFound.value -> {
                        Result.failure(GoTrueException("Resource not found", GoTrueErrorType.NOT_FOUND))
                    }
                    429 -> { // Too Many Requests
                        Result.failure(GoTrueException("Too many attempts. Please try again later.", GoTrueErrorType.RATE_LIMITED))
                    }
                    else -> {
                        val message = e.message ?: "Authentication error"
                        when {
                            message.contains("invalid login credentials", ignoreCase = true) -> {
                                Result.failure(GoTrueException("Invalid email or password", GoTrueErrorType.INVALID_CREDENTIALS))
                            }
                            message.contains("User already registered", ignoreCase = true) -> {
                                Result.failure(GoTrueException("Email already registered", GoTrueErrorType.USER_EXISTS))
                            }
                            message.contains("Email not confirmed", ignoreCase = true) -> {
                                Result.failure(GoTrueException("Please confirm your email first", GoTrueErrorType.EMAIL_NOT_CONFIRMED))
                            }
                            else -> {
                                Result.failure(GoTrueException(message, GoTrueErrorType.SERVER_ERROR))
                            }
                        }
                    }
                }
            }
            is ClientRequestException -> {
                Result.failure(GoTrueException("Network error: ${e.message}", GoTrueErrorType.NETWORK_ERROR))
            }
            else -> {
                when {
                    e.message?.contains("invalid_grant") == true -> {
                        Result.failure(GoTrueException("Sign-in failed. Please try again.", GoTrueErrorType.OAUTH_ERROR))
                    }
                    e.message?.contains("requires admin") == true || e.message?.contains("not allowed") == true -> {
                        Result.failure(GoTrueException("Insufficient permissions", GoTrueErrorType.INSUFFICIENT_PERMISSIONS))
                    }
                    else -> {
                        Result.failure(GoTrueException(e.message ?: "Unknown authentication error", GoTrueErrorType.UNKNOWN))
                    }
                }
            }
        }
    }

    /**
     * Map Supabase UserInfo to app User model
     */
    private fun mapToUser(userInfo: UserInfo): User {
        return User(
            id = userInfo.id,
            email = userInfo.email ?: "",
            userName = userInfo.userMetadata?.get("name") as? String ?: "",
            isVerified = userInfo.emailConfirmedAt != null,
            createdAt = userInfo.createdAt,
            lastSignIn = userInfo.lastSignInAt,
            avatarUrl = userInfo.userMetadata?.get("avatar_url") as? String
        )
    }
}

/**
 * Session state for the app
 */
sealed class SessionState {
    object Loading : SessionState()
    data class Authenticated(val session: UserSession) : SessionState()
    object NotAuthenticated : SessionState()
    data class Error(val message: String) : SessionState()
}

/**
 * Custom exceptions for gotrue operations
 */
class GoTrueException(
    message: String,
    val errorType: GoTrueErrorType
) : Exception(message)

/**
 * Enum to categorize gotrue errors for better UI handling
 */
enum class GoTrueErrorType {
    INVALID_CREDENTIALS,     // Wrong email/password
    USER_EXISTS,            // Email already registered
    EMAIL_NOT_CONFIRMED,    // Email not confirmed
    UNAUTHORIZED,           // 401 errors
    FORBIDDEN,              // 403 errors
    NOT_FOUND,             // 404 errors
    RATE_LIMITED,          // Too many attempts
    NETWORK_ERROR,         // Connection issues
    OAUTH_ERROR,           // OAuth specific errors
    CANCELLED,             // User cancelled auth
    INSUFFICIENT_PERMISSIONS, // Not enough permissions for admin operations
    SERVER_ERROR,          // 500 errors
    UNKNOWN                // Anything else
} 
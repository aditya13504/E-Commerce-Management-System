package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.model.User
import com.yourdomain.ecommerce.core.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Implementation of the AuthRepository using Supabase authentication
 */
class AuthRepositoryImpl constructor(
    private val auth: GoTrue
) : AuthRepository {

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        try {
            val session = auth.currentSessionOrNull()
            session?.let { 
                val userInfo = auth.getUserInfo()
                mapUserInfoToUser(userInfo)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting current user")
            null
        }
    }

    override fun observeAuthState(): Flow<User?> {
        return auth.userFlow.map { userInfo ->
            userInfo?.let { mapUserInfoToUser(it) }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            val userInfo = auth.getUserInfo()
            val user = mapUserInfoToUser(userInfo)
            
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Error signing in user with email: $email")
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildMap {
                    put("name", name)
                }
            }
            
            val userInfo = auth.getUserInfo()
            val user = mapUserInfoToUser(userInfo).copy(name = name)
            
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Error signing up user with email: $email")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error signing out")
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error resetting password for email: $email")
            Result.failure(e)
        }
    }

    override fun isAuthenticated(): Boolean {
        return auth.currentSessionOrNull() != null
    }
    
    /**
     * Map Supabase UserInfo to our domain User model
     */
    private fun mapUserInfoToUser(userInfo: UserInfo): User {
        return User(
            id = userInfo.id,
            email = userInfo.email ?: "",
            name = userInfo.userMetadata["name"] as? String ?: "",
            avatarUrl = userInfo.avatarUrl,
            phoneNumber = userInfo.phone,
            isEmailVerified = userInfo.emailConfirmedAt != null,
            createdAt = userInfo.createdAt,
            lastSignInAt = userInfo.lastSignInAt,
            metadata = userInfo.userMetadata.mapValues { it.value.toString() }
        )
    }
} 
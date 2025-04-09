package com.yourdomain.ecommerce.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.sessionStatus
import io.github.jan.supabase.gotrue.SessionStatus
import com.yourdomain.ecommerce.core.data.repository.CustomerRepository
import com.yourdomain.ecommerce.core.data.repository.CustomerRepositoryImpl
import androidx.lifecycle.ViewModelProvider

// Represents the UI state for authentication screens
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val currentUserId: String? = null // Supabase uses String IDs (UUID)
)

class AuthViewModel : ViewModel() {

    private val client = SupabaseClientManager.client
    private val customerRepository = CustomerRepositoryImpl

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check initial auth state
        viewModelScope.launch {
            client.auth.sessionStatus.collect { status ->
                val isAuthenticated = status is SessionStatus.Authenticated
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = isAuthenticated,
                    currentUserId = if (isAuthenticated) client.auth.currentUserOrNull()?.id else null,
                    isLoading = false // Ensure loading is false after initial check
                )
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val session = client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                
                // After successful auth sign up, create customer table entry
                val authUserId = session.user?.id
                if (authUserId != null) {
                    val createCustomerResult = customerRepository.createCustomerForAuthUser(
                        authUserId = authUserId,
                        email = email
                        // Add name here if collected from UI
                    )
                    if (createCustomerResult.isFailure) {
                         // Log or optionally bubble up error to UI 
                        println("Failed to create customer entry after sign up: ${createCustomerResult.exceptionOrNull()?.message}")
                        // Optionally set a specific error state
                        // _uiState.value = _uiState.value.copy(isLoading = false, error = "Account created, but profile setup failed.")
                        // return@launch // Decide if signup is still 'successful' for the user
                    }
                } else {
                     println("Sign up successful but couldn't get auth user ID to create customer entry.")
                     // Set appropriate error state if needed
                }

                _uiState.value = _uiState.value.copy(isLoading = false, error = "Sign up successful! Check email for confirmation.")
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Sign up failed")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
             _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                // Login successful, sessionStatus collector in init will update state
                // _uiState.value = _uiState.value.copy(isLoading = false) // Handled by collector
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
             _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                client.auth.signOut()
                 // Logout successful, sessionStatus collector will update state
                // _uiState.value = _uiState.value.copy(isLoading = false) // Handled by collector
            } catch (e: Exception) {
                 _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Logout failed")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class Factory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val authViewModel = AuthViewModel()
            return authViewModel as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 
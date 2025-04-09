package com.yourdomain.ecommerce.core.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import com.yourdomain.ecommerce.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth as KtorAuth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.json.JSONObject
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A singleton class that manages the Supabase client instance and connection state
 */
class SupabaseClientManager private constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Initializing)
    val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: Flow<AuthState> = _authState.asStateFlow()

    // Changed to var to allow reassignment
    private var httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                encodeDefaults = true
            })
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // 30 seconds
            connectTimeoutMillis = 15000  // 15 seconds
            socketTimeoutMillis = 60000   // 60 seconds
        }
        
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 3)
            exponentialDelay()
        }
        
        install(WebSockets) {
            pingInterval = 30000 // 30 seconds
        }
        
        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    Timber.tag("SupabaseHttpClient").d(message)
                }
            }
        }
        
        install(KtorAuth) {
            // Additional authentication configuration if needed
        }
        
        install(DefaultRequest) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }

    // Create a custom session manager with shared preferences
    private class SharedPreferencesSessionManager(
        private val preferences: SharedPreferences
    ) : SessionManager {
        private val ACCESS_TOKEN_KEY = "access_token"
        private val REFRESH_TOKEN_KEY = "refresh_token"
        private val TOKEN_TYPE_KEY = "token_type"
        private val EXPIRES_AT_KEY = "expires_at"
        
        override suspend fun saveSession(session: UserSession) {
            // Store session components separately for simplicity
            preferences.edit().apply {
                putString(ACCESS_TOKEN_KEY, session.accessToken)
                putString(REFRESH_TOKEN_KEY, session.refreshToken)
                putString(TOKEN_TYPE_KEY, session.tokenType)
                // Just store the timestamp as a long value
                putLong(EXPIRES_AT_KEY, session.expiresIn ?: -1L)
                apply()
            }
        }

        override suspend fun loadSession(): UserSession? {
            // Check if we have the basic required data
            if (!preferences.contains(ACCESS_TOKEN_KEY)) return null
            
            val accessToken = preferences.getString(ACCESS_TOKEN_KEY, null) ?: return null
            val refreshToken = preferences.getString(REFRESH_TOKEN_KEY, null) ?: return null
            val tokenType = preferences.getString(TOKEN_TYPE_KEY, "bearer") ?: return null
            val expiresIn = preferences.getLong(EXPIRES_AT_KEY, -1L).let { if (it != -1L) it else null }
            
            // Create a UserSession with the stored values and null for user since we can't persist that easily
            return expiresIn?.let {
                UserSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    tokenType = tokenType,
                    expiresIn = it,
                    user = null
                )
            }
        }

        suspend fun removeSession() {
            preferences.edit().apply {
                remove(ACCESS_TOKEN_KEY)
                remove(REFRESH_TOKEN_KEY)
                remove(TOKEN_TYPE_KEY)
                remove(EXPIRES_AT_KEY)
                apply()
            }
        }
        
        override suspend fun deleteSession() {
            removeSession()
        }
    }

    // Create and configure Supabase client
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        httpClient = this@SupabaseClientManager.httpClient
        
        install(GoTrue) {
            // Configure GoTrue (authentication)
            // Use a custom session manager implementation
            val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
            sessionManager = SharedPreferencesSessionManager(prefs)
        }
        
        install(Postgrest) {
            // Configure Postgrest features
            defaultSchema = "public"
        }
        
        install(Storage)
        
        install(Realtime) {
            // Configure Realtime settings with Duration
            reconnectDelay = 5000.milliseconds
        }
    }

    init {
        monitorConnection()
        monitorAuthStatus()
    }

    /**
     * Monitors the connection to Supabase
     */
    private fun monitorConnection() {
        scope.launch {
            try {
                try {
                    // Perform a lightweight health check
                    client.postgrest["health_check"].select()
                    _connectionState.value = ConnectionState.Connected
                    Timber.d("Successfully connected to Supabase")
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.Failed("Client initialization failed")
                    Timber.e("Failed to initialize Supabase client")
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed(e.message ?: "Unknown error")
                Timber.e(e, "Error connecting to Supabase: ${e.message}")
            }
        }
    }

    /**
     * Monitors the authentication status
     */
    private fun monitorAuthStatus() {
        scope.launch {
            client.gotrue.sessionStatus.collect { status: SessionStatus ->
                _authState.value = when (status) {
                    is SessionStatus.Authenticated -> {
                        Timber.d("User authenticated: ${status.session.user?.email}")
                        AuthState.Authenticated(status.session)
                    }
                    is SessionStatus.LoadingFromStorage -> {
                        Timber.d("Loading auth from storage")
                        AuthState.Loading
                    }
                    is SessionStatus.NetworkError -> {
                        Timber.e("Auth network error: ${status.toString()}")
                        AuthState.Error(status.toString())
                    }
                    is SessionStatus.NotAuthenticated -> {
                        Timber.d("User not authenticated")
                        AuthState.NotAuthenticated
                    }
                }
            }
        }
    }
    
    /**
     * Check if the client connection is available
     */
    suspend fun isConnectionAvailable(): Boolean {
        return try {
            // Simple connection check
            client.postgrest["health_check"].select()
            true
        } catch (e: Exception) {
            Timber.e(e, "Supabase connection check failed")
            false
        }
    }

    /**
     * Represents the connection state to Supabase
     */
    sealed class ConnectionState {
        object Initializing : ConnectionState()
        object Connected : ConnectionState()
        data class Failed(val reason: String) : ConnectionState()
    }

    /**
     * Represents the authentication state
     */
    sealed class AuthState {
        object NotAuthenticated : AuthState()
        object Loading : AuthState()
        data class Authenticated(val session: UserSession) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    companion object {
        private const val TAG = "SupabaseClientManager"
        
        // Singleton instance
        @Volatile private var instance: SupabaseClientManager? = null
        
        /**
         * Get the SupabaseClientManager instance
         */
        fun getInstance(context: Context): SupabaseClientManager {
            return instance ?: synchronized(this) {
                instance ?: SupabaseClientManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 
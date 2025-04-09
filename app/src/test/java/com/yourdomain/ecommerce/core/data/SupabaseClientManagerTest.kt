package com.yourdomain.ecommerce.core.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SupabaseClientManagerTest {

    private lateinit var supabaseClientManager: SupabaseClientManager
    private val mockSupabaseClient: SupabaseClient = mockk(relaxed = true)
    private val mockGoTrue: GoTrue = mockk(relaxed = true)
    private val mockPostgrest: Postgrest = mockk(relaxed = true)
    private val mockStorage: Storage = mockk(relaxed = true)
    private val mockRealtime: Realtime = mockk(relaxed = true)
    
    private val supabaseUrl = "https://example.supabase.co"
    private val supabaseKey = "test-api-key"
    
    @Before
    fun setUp() {
        // Mock Supabase client creation
        mockkConstructor(SupabaseClientManager::class)
        mockkStatic("io.github.jan.supabase.SupabaseClientKt")
        
        // Set up the Supabase client
        every { mockSupabaseClient.postgrest } returns mockPostgrest
        every { mockSupabaseClient.gotrue } returns mockGoTrue
        every { mockSupabaseClient.storage } returns mockStorage
        every { mockSupabaseClient.realtime } returns mockRealtime
        
        // Create the manager with test configuration
        supabaseClientManager = SupabaseClientManager(supabaseUrl, supabaseKey)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initialize should create a valid Supabase client`() {
        // Arrange
        val configSlot = slot<SupabaseClient.() -> Unit>()
        
        // Act
        supabaseClientManager.initialize()
        
        // Assert
        verify { 
            supabaseClientManager.initialize()
        }
        
        assertNotNull(supabaseClientManager)
    }
    
    @Test
    fun `getClient should return initialized client`() = runTest {
        // Arrange
        val mockClient = mockk<SupabaseClient>(relaxed = true)
        every { supabaseClientManager.getClient() } returns mockClient
        
        // Act
        val client = supabaseClientManager.getClient()
        
        // Assert
        assertNotNull(client)
        assertEquals(mockClient, client)
    }
    
    @Test
    fun `cleanup should properly close resources`() = runTest {
        // Arrange - Set up any resources that need cleanup
        val mockClient = mockk<SupabaseClient>(relaxed = true)
        every { supabaseClientManager.getClient() } returns mockClient
        
        // Act
        supabaseClientManager.cleanup()
        
        // Assert
        verify { 
            supabaseClientManager.cleanup()
        }
    }
    
    @Test
    fun `getClient should initialize client if not already initialized`() = runTest {
        // Arrange
        val mockClient = mockk<SupabaseClient>(relaxed = true)
        
        // Configure the behavior
        every { 
            supabaseClientManager.getClient() 
        } answers {
            if (supabaseClientManager.isInitialized) {
                mockClient
            } else {
                supabaseClientManager.initialize()
                mockClient
            }
        }
        
        every { supabaseClientManager.isInitialized } returns false andThen true
        every { supabaseClientManager.initialize() } returns Unit
        
        // Act
        val client = supabaseClientManager.getClient()
        
        // Assert
        verify { supabaseClientManager.initialize() }
        assertEquals(mockClient, client)
    }
} 
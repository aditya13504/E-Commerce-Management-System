package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.Result
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.core.data.model.Customer
import com.yourdomain.ecommerce.util.MainDispatcherRule
import io.github.jan.supabase.postgrest.PostgrestResult
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: CustomerRepositoryImpl
    private val supabaseClientManager: SupabaseClientManager = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    // Test data
    private val testCustomers = listOf(
        Customer(
            id = "cust1",
            userId = "user1",
            name = "John Doe",
            email = "john@example.com",
            phone = "1234567890",
            address = "123 Main St, Anytown, USA",
            createdAt = "2023-01-01T00:00:00Z",
            updatedAt = "2023-01-01T00:00:00Z"
        ),
        Customer(
            id = "cust2",
            userId = "user2",
            name = "Jane Smith",
            email = "jane@example.com",
            phone = "0987654321",
            address = "456 Oak Ave, Somewhere, USA",
            createdAt = "2023-01-02T00:00:00Z",
            updatedAt = "2023-01-02T00:00:00Z"
        ),
        Customer(
            id = "cust3",
            userId = "user3",
            name = "Bob Johnson",
            email = "bob@example.com",
            phone = "5551234567",
            address = "789 Pine St, Nowhere, USA",
            createdAt = "2023-01-03T00:00:00Z",
            updatedAt = "2023-01-03T00:00:00Z"
        )
    )

    @Before
    fun setUp() {
        repository = CustomerRepositoryImpl(supabaseClientManager, testDispatcher)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getAllCustomers should return customers with pagination`() = runTest {
        // Arrange
        val page = 1
        val pageSize = 10
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .order("createdAt", false)
                .range((page - 1) * pageSize, page * pageSize - 1)
                .decodeList<Customer>()
        } returns testCustomers
        
        // Act
        val result = repository.getAllCustomers(page, pageSize)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customers ->
            assertEquals(3, customers.size)
            assertEquals("cust1", customers[0].id)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .order("createdAt", false)
                .range(0, 9)
                .decodeList<Customer>()
        }
    }
    
    @Test
    fun `getCustomerById should return a specific customer`() = runTest {
        // Arrange
        val customerId = "cust1"
        val expectedCustomer = testCustomers[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .eq("id", customerId)
                .limit(1)
                .decodeSingle<Customer>()
        } returns expectedCustomer
        
        // Act
        val result = repository.getCustomerById(customerId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customer ->
            assertEquals(expectedCustomer, customer)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .eq("id", customerId)
                .limit(1)
                .decodeSingle<Customer>()
        }
    }
    
    @Test
    fun `getCustomerByEmail should return a customer with matching email`() = runTest {
        // Arrange
        val email = "john@example.com"
        val expectedCustomer = testCustomers[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .eq("email", email)
                .limit(1)
                .decodeSingle<Customer>()
        } returns expectedCustomer
        
        // Act
        val result = repository.getCustomerByEmail(email)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customer ->
            assertEquals(expectedCustomer, customer)
            assertEquals(email, customer.email)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .eq("email", email)
                .limit(1)
                .decodeSingle<Customer>()
        }
    }
    
    @Test
    fun `createCustomer should add a new customer and return it`() = runTest {
        // Arrange
        val newCustomer = testCustomers[0].copy(id = "")
        val createdCustomer = testCustomers[0]
        
        val customerSlot = slot<Customer>()
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .insert(capture(customerSlot), returning = PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Customer>()
        } returns createdCustomer
        
        // Act
        val result = repository.createCustomer(newCustomer)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customer ->
            assertEquals(createdCustomer, customer)
        }
        
        // Verify captured customer
        assertEquals(newCustomer, customerSlot.captured)
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .insert(any(), returning = PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Customer>()
        }
    }
    
    @Test
    fun `updateCustomer should modify an existing customer`() = runTest {
        // Arrange
        val customerId = "cust1"
        val updates = mapOf(
            "name" to "John Updated",
            "phone" to "9876543210"
        )
        val updatedCustomer = testCustomers[0].copy(
            name = "John Updated",
            phone = "9876543210"
        )
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .update(updates)
                .eq("id", customerId)
                .returning(PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Customer>()
        } returns updatedCustomer
        
        // Act
        val result = repository.updateCustomer(customerId, updates)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customer ->
            assertEquals("John Updated", customer.name)
            assertEquals("9876543210", customer.phone)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .update(updates)
                .eq("id", customerId)
                .returning(PostgrestResult.Returning.REPRESENTATION)
                .decodeSingle<Customer>()
        }
    }
    
    @Test
    fun `deleteCustomer should remove a customer`() = runTest {
        // Arrange
        val customerId = "cust1"
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .delete()
                .eq("id", customerId)
                .execute()
        } returns mockk(relaxed = true)
        
        // Act
        val result = repository.deleteCustomer(customerId)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .delete()
                .eq("id", customerId)
                .execute()
        }
    }
    
    @Test
    fun `searchCustomers should return customers matching the query`() = runTest {
        // Arrange
        val query = "john"
        val expectedCustomers = testCustomers.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.email.contains(query, ignoreCase = true)
        }
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .or("name.ilike.%$query%,email.ilike.%$query%")
                .decodeList<Customer>()
        } returns expectedCustomers
        
        // Act
        val result = repository.searchCustomers(query)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customers ->
            assertEquals(1, customers.size)
            assertTrue(customers.all { 
                it.name.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true)
            })
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .or("name.ilike.%$query%,email.ilike.%$query%")
                .decodeList<Customer>()
        }
    }
    
    @Test
    fun `getCustomerByUserId should return customer with matching userId`() = runTest {
        // Arrange
        val userId = "user1"
        val expectedCustomer = testCustomers[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .eq("userId", userId)
                .limit(1)
                .decodeSingle<Customer>()
        } returns expectedCustomer
        
        // Act
        val result = repository.getCustomerByUserId(userId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { customer ->
            assertEquals(expectedCustomer, customer)
            assertEquals(userId, customer.userId)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .select()
                .eq("userId", userId)
                .limit(1)
                .decodeSingle<Customer>()
        }
    }
    
    @Test
    fun `countCustomers should return total customer count`() = runTest {
        // Arrange
        coEvery { 
            supabaseClientManager.getClient().from("customers")
                .count()
                .executeSingle<HashMap<String, Int>>()
        } returns hashMapOf("count" to 3)
        
        // Act
        val result = repository.countCustomers()
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { count ->
            assertEquals(3, count)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("customers")
                .count()
                .executeSingle<HashMap<String, Int>>()
        }
    }
} 
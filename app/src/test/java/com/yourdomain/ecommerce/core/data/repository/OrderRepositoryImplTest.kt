package com.yourdomain.ecommerce.core.data.repository

import com.yourdomain.ecommerce.core.data.Result
import com.yourdomain.ecommerce.core.data.remote.SupabaseClientManager
import com.yourdomain.ecommerce.core.data.model.Order
import com.yourdomain.ecommerce.core.data.model.OrderItem
import com.yourdomain.ecommerce.core.data.model.OrderStatus
import com.yourdomain.ecommerce.util.MainDispatcherRule
import io.github.jan.supabase.postgrest.PostgrestResult
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class OrderRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: OrderRepositoryImpl
    private val supabaseClientManager: SupabaseClientManager = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    // Test data
    private val testOrderItems = listOf(
        OrderItem(
            id = "item1",
            orderId = "order1",
            productId = "prod1",
            quantity = 2,
            price = 29.99
        ),
        OrderItem(
            id = "item2",
            orderId = "order1",
            productId = "prod2",
            quantity = 1,
            price = 49.99
        )
    )

    private val testOrders = listOf(
        Order(
            id = "order1",
            customerId = "cust1",
            orderDate = "2023-05-20T14:30:00Z",
            status = OrderStatus.PENDING,
            totalAmount = 109.97,
            items = testOrderItems
        ),
        Order(
            id = "order2",
            customerId = "cust2",
            orderDate = "2023-05-19T10:15:00Z",
            status = OrderStatus.COMPLETED,
            totalAmount = 75.50,
            items = emptyList()
        ),
        Order(
            id = "order3",
            customerId = "cust1",
            orderDate = "2023-05-18T09:00:00Z",
            status = OrderStatus.SHIPPED,
            totalAmount = 45.25,
            items = emptyList()
        )
    )

    @Before
    fun setUp() {
        repository = OrderRepositoryImpl(supabaseClientManager, testDispatcher)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getAllOrders should return orders with pagination`() = runTest {
        // Arrange
        val page = 1
        val pageSize = 10
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .order("orderDate", true)
                .range((page - 1) * pageSize, page * pageSize - 1)
                .decodeList<Order>()
        } returns testOrders
        
        // Act
        val result = repository.getAllOrders(page, pageSize)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { orders ->
            assertEquals(3, orders.size)
            assertEquals("order1", orders[0].id)
            assertEquals(2, orders[0].items.size)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .order("orderDate", true)
                .range(0, 9)
                .decodeList<Order>()
        }
    }
    
    @Test
    fun `getOrderById should return a specific order`() = runTest {
        // Arrange
        val orderId = "order1"
        val expectedOrder = testOrders[0]
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .eq("id", orderId)
                .limit(1)
                .decodeSingle<Order>()
        } returns expectedOrder
        
        // Act
        val result = repository.getOrderById(orderId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { order ->
            assertEquals(expectedOrder, order)
            assertEquals(2, order.items.size)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .eq("id", orderId)
                .limit(1)
                .decodeSingle<Order>()
        }
    }
    
    @Test
    fun `getOrdersByCustomer should return all orders for a customer`() = runTest {
        // Arrange
        val customerId = "cust1"
        val expectedOrders = testOrders.filter { it.customerId == customerId }
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .eq("customerId", customerId)
                .order("orderDate", true)
                .decodeList<Order>()
        } returns expectedOrders
        
        // Act
        val result = repository.getOrdersByCustomer(customerId)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { orders ->
            assertEquals(2, orders.size)
            assertTrue(orders.all { it.customerId == customerId })
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .eq("customerId", customerId)
                .order("orderDate", true)
                .decodeList<Order>()
        }
    }
    
    @Test
    fun `createOrder should add a new order and return it`() = runTest {
        // Arrange
        val newOrder = testOrders[0].copy(id = "")
        val createdOrder = testOrders[0]
        
        // Mock for order creation
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .insert(any<Order>())
                .select()
                .decodeSingle<Order>()
        } returns createdOrder.copy(items = emptyList())
        
        // Mock for order items creation
        coEvery { 
            supabaseClientManager.getClient().from("order_items")
                .insert(any<List<OrderItem>>())
                .select()
                .decodeList<OrderItem>()
        } returns testOrderItems
        
        // Act
        val result = repository.createOrder(newOrder)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { order ->
            assertEquals(createdOrder.id, order.id)
            assertEquals(createdOrder.customerId, order.customerId)
            assertEquals(createdOrder.totalAmount, order.totalAmount, 0.001)
        }
        
        // Verify order creation
        coVerify { 
            supabaseClientManager.getClient().from("orders").insert(any<Order>())
        }
    }
    
    @Test
    fun `updateOrder should modify an existing order`() = runTest {
        // Arrange
        val orderId = "order1"
        val updates = mapOf(
            "status" to OrderStatus.SHIPPED,
            "totalAmount" to 120.99
        )
        val updatedOrder = testOrders[0].copy(
            status = OrderStatus.SHIPPED,
            totalAmount = 120.99
        )
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .update(updates)
                .eq("id", orderId)
                .select()
                .decodeSingle<Order>()
        } returns updatedOrder
        
        // Act
        val result = repository.updateOrder(orderId, updates)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { order ->
            assertEquals(updatedOrder.status, order.status)
            assertEquals(120.99, order.totalAmount, 0.001)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .update(updates)
                .eq("id", orderId)
                .select()
                .decodeSingle<Order>()
        }
    }
    
    @Test
    fun `deleteOrder should remove an order`() = runTest {
        // Arrange
        val orderId = "order1"
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .delete()
                .eq("id", orderId)
                .execute()
        } returns mockk(relaxed = true)
        
        // Act
        val result = repository.deleteOrder(orderId)
        
        // Assert
        assertTrue(result.isSuccess)
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .delete()
                .eq("id", orderId)
                .execute()
        }
    }
    
    @Test
    fun `getOrdersByStatus should return orders with specific status`() = runTest {
        // Arrange
        val status = OrderStatus.PENDING
        val expectedOrders = testOrders.filter { it.status == status }
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .eq("status", status.toString())
                .order("orderDate", true)
                .decodeList<Order>()
        } returns expectedOrders
        
        // Act
        val result = repository.getOrdersByStatus(status.toString())
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { orders ->
            assertEquals(1, orders.size)
            assertEquals(status, orders[0].status)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .eq("status", status.toString())
                .order("orderDate", true)
                .decodeList<Order>()
        }
    }
    
    @Test
    fun `getOrdersByDateRange should return orders within date range`() = runTest {
        // Arrange
        val startDate = LocalDate.of(2023, 5, 18)
        val endDate = LocalDate.of(2023, 5, 20)
        val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
        
        val expectedOrders = testOrders.filter {
            val orderDate = LocalDateTime.parse(it.orderDate, dateTimeFormatter).toLocalDate()
            orderDate in startDate..endDate
        }
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .gte("orderDate", startDate.toString())
                .lte("orderDate", endDate.plusDays(1).toString())
                .order("orderDate", true)
                .decodeList<Order>()
        } returns expectedOrders
        
        // Act
        val result = repository.getOrdersByDateRange(startDate, endDate)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { orders ->
            assertEquals(3, orders.size)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .select("*, items(*)")
                .gte("orderDate", startDate.toString())
                .lte("orderDate", endDate.plusDays(1).toString())
                .order("orderDate", true)
                .decodeList<Order>()
        }
    }
    
    @Test
    fun `getTotalRevenue should calculate revenue correctly`() = runTest {
        // Arrange
        val expectedTotal = testOrders.sumOf { it.totalAmount }
        
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .select("totalAmount")
                .any()
                .decodeList<Order>()
        } returns testOrders
        
        // Act - without date range
        val result = repository.getTotalRevenue(null, null)
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { revenue ->
            assertEquals(expectedTotal, revenue, 0.001)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .select("totalAmount")
                .any()
                .decodeList<Order>()
        }
    }
    
    @Test
    fun `countOrdersByStatus should return count per status`() = runTest {
        // Arrange
        val expectedCounts = mapOf(
            OrderStatus.PENDING.toString() to 1,
            OrderStatus.COMPLETED.toString() to 1,
            OrderStatus.SHIPPED.toString() to 1,
            OrderStatus.CANCELLED.toString() to 0
        )
        
        coEvery { 
            supabaseClientManager.getClient().rpc("count_orders_by_status")
                .executeSingle<Map<String, Int>>()
        } returns expectedCounts
        
        // Act
        val result = repository.countOrdersByStatus()
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { counts ->
            assertEquals(expectedCounts, counts)
            assertEquals(1, counts[OrderStatus.PENDING.toString()])
            assertEquals(1, counts[OrderStatus.COMPLETED.toString()])
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().rpc("count_orders_by_status")
                .executeSingle<Map<String, Int>>()
        }
    }
    
    @Test
    fun `countOrders should return total order count`() = runTest {
        // Arrange
        coEvery { 
            supabaseClientManager.getClient().from("orders")
                .count()
                .executeSingle<HashMap<String, Int>>()
        } returns hashMapOf("count" to 3)
        
        // Act
        val result = repository.countOrders()
        
        // Assert
        assertTrue(result.isSuccess)
        result.onSuccess { count ->
            assertEquals(3, count)
        }
        
        // Verify
        coVerify { 
            supabaseClientManager.getClient().from("orders")
                .count()
                .executeSingle<HashMap<String, Int>>()
        }
    }
} 
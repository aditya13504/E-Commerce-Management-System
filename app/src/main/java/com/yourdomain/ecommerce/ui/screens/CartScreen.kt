package com.yourdomain.ecommerce.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourdomain.ecommerce.ui.viewmodels.CartViewModel
import com.yourdomain.ecommerce.ui.viewmodels.CartUiState
import com.yourdomain.ecommerce.ui.viewmodels.CartItem
import com.yourdomain.ecommerce.AppDestinations // Import destinations
import com.yourdomain.ecommerce.ui.viewmodels.OrderViewModel // Import OrderViewModel
import com.yourdomain.ecommerce.ui.viewmodels.OrderStatus // Import OrderStatus
import androidx.compose.material3.SnackbarHost // Import SnackbarHost
import androidx.compose.material3.SnackbarHostState // Import SnackbarHostState
import androidx.compose.runtime.remember // Import remember
import androidx.compose.runtime.rememberCoroutineScope // Import rememberCoroutineScope
import kotlinx.coroutines.launch // Import launch
import androidx.compose.material.icons.Icons // Import Icons
import androidx.compose.material.icons.filled.Delete // Import Delete icon
import androidx.compose.material.icons.filled.Add // Import Add icon
import androidx.compose.material.icons.filled.Remove // Import Remove icon
import com.yourdomain.ecommerce.ui.viewmodels.AuthViewModel // Import AuthViewModel

@Composable
fun CartScreen(
    navController: NavController,
    cartViewModel: CartViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel() // Get AuthViewModel
) {
    val cartUiState by cartViewModel.uiState.collectAsState()
    val orderUiState by orderViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState() // Get auth state
    val snackbarHostState = remember { SnackbarHostState() } // Add SnackbarHostState
    val scope = rememberCoroutineScope() // Add CoroutineScope

    // Handle order status changes (e.g., show dialogs, navigate, clear cart)
    LaunchedEffect(orderUiState.status, orderUiState.error) { // Observe error too
        when (orderUiState.status) {
            OrderStatus.SUCCESS -> {
                val orderId = orderUiState.createdOrderId
                val message = orderUiState.error ?: "Order placed successfully! Order ID: $orderId" // Show partial success msg if error exists
                scope.launch { 
                    snackbarHostState.showSnackbar(message = message)
                }
                cartViewModel.clearCart()
                // Navigate to Order Confirmation screen
                navController.navigate("${AppDestinations.ORDER_CONFIRMATION_ROUTE}/$orderId") { 
                    popUpTo(AppDestinations.PRODUCT_LIST) // Pop back stack to product list
                }
                orderViewModel.resetOrderState() // Resets status and error
            }
            OrderStatus.FAILURE -> {
                orderUiState.error?.let { errorMsg -> // Only show if error is not null
                    scope.launch { 
                        snackbarHostState.showSnackbar(message = "Order Failed: $errorMsg")
                    }
                    orderViewModel.resetOrderState() // Resets status and error
                } 
            }
            else -> { /* Idle/Processing */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // Add SnackbarHost
        topBar = {
            TopAppBar(title = { Text("Shopping Cart") })
        },
        bottomBar = {
            CartBottomBar(
                cartUiState = cartUiState, 
                orderStatus = orderUiState.status, // Pass order status
                onCheckoutClick = {
                    // Pass current user ID from auth state
                    orderViewModel.placeOrder(cartUiState, authState.currentUserId)
                } // Trigger order placement
            )
        }
    ) { paddingValues ->
        CartContent(
            uiState = cartUiState,
            modifier = Modifier.padding(paddingValues),
            onRemoveItem = cartViewModel::removeFromCart,
            onIncreaseQuantity = cartViewModel::increaseQuantity,
            onDecreaseQuantity = cartViewModel::decreaseQuantity
        )
    }
}

@Composable
fun CartContent(
    uiState: CartUiState,
    modifier: Modifier = Modifier,
    onRemoveItem: (Int) -> Unit,
    onIncreaseQuantity: (Int) -> Unit,
    onDecreaseQuantity: (Int) -> Unit
) {
    if (uiState.items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Your shopping cart is empty.",
                style = MaterialTheme.typography.titleMedium // Make text larger
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.items.values.toList()) { item ->
                CartListItem(
                    item = item,
                    onRemoveClick = { onRemoveItem(item.productId) },
                    onIncreaseQuantity = { onIncreaseQuantity(item.productId) },
                    onDecreaseQuantity = { onDecreaseQuantity(item.productId) }
                )
            }
        }
    }
}

@Composable
fun CartListItem(
    item: CartItem,
    onRemoveClick: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name ?: "Product ID: ${item.productId}",
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        
        // Quantity Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
             IconButton(onClick = onDecreaseQuantity, modifier = Modifier.size(32.dp)) {
                 Icon(Icons.Default.Remove, contentDescription = "Decrease Quantity")
             }
             Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
             IconButton(onClick = onIncreaseQuantity, modifier = Modifier.size(32.dp)) {
                 Icon(Icons.Default.Add, contentDescription = "Increase Quantity")
             }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Price
        Text(
             text = "\$${String.format("%.2f", item.price * item.quantity)}",
             style = MaterialTheme.typography.bodyMedium
        )
        
        // Remove Button
        IconButton(onClick = onRemoveClick) { 
            Icon(Icons.Filled.Delete, contentDescription = "Remove Item")
        }
    }
    Divider()
}

@Composable
fun CartBottomBar(
    cartUiState: CartUiState,
    orderStatus: OrderStatus, // Receive order status
    onCheckoutClick: () -> Unit // Use callback
) {
    val isProcessing = orderStatus == OrderStatus.PROCESSING
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total: \$${String.format("%.2f", cartUiState.totalAmount)}",
                style = MaterialTheme.typography.titleLarge
            )
            Button(
                onClick = onCheckoutClick,
                enabled = cartUiState.items.isNotEmpty() && !isProcessing // Disable if processing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Checkout")
                }
            }
        }
    }
} 
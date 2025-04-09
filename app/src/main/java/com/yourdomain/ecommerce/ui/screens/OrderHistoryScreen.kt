package com.yourdomain.ecommerce.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import all runtime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourdomain.ecommerce.core.data.model.OrderTable
import com.yourdomain.ecommerce.ui.viewmodels.AuthViewModel // Need to get authUserId
import com.yourdomain.ecommerce.ui.viewmodels.OrderHistoryViewModel
import com.yourdomain.ecommerce.ui.viewmodels.OrderHistoryUiState
import androidx.compose.foundation.clickable // Import clickable
import com.yourdomain.ecommerce.AppDestinations
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons // Import Icons
import androidx.compose.material.icons.filled.ErrorOutline // Import error icon
import androidx.compose.ui.text.style.TextAlign // Import TextAlign
import androidx.lifecycle.ViewModelProvider // Import ViewModelProvider

@Composable
fun OrderHistoryScreen(
    navController: NavController, // For potential navigation to order details
    // Use Factory for ViewModels
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory()),
    orderHistoryViewModel: OrderHistoryViewModel = viewModel(factory = OrderHistoryViewModel.Factory())
) {
    val authState by authViewModel.uiState.collectAsState()
    val uiState by orderHistoryViewModel.uiState.collectAsState()

    // Load history when the screen composition starts and authState is ready
    LaunchedEffect(authState.currentUserId) {
        if (authState.currentUserId != null) {
            orderHistoryViewModel.loadOrderHistory(authState.currentUserId)
        } else {
            // Handle case where user is not logged in - perhaps show error or navigate
            orderHistoryViewModel.setError("User not logged in.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Orders") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    // Prominent error display
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { 
                            if (authState.currentUserId != null) { 
                                orderHistoryViewModel.loadOrderHistory(authState.currentUserId) 
                            } else {
                                // Optionally navigate to login or show different message
                                // navController.navigate(AppDestinations.AUTH) 
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.orders.isEmpty() -> {
                    Text(
                        text = "You haven't placed any orders yet.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.orders) { order ->
                            OrderHistoryItem(
                                order = order,
                                onClick = {
                                    order.orderid?.let { id ->
                                        navController.navigate("${AppDestinations.ORDER_DETAIL_ROUTE}/$id")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHistoryItem(order: OrderTable, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order ID: ${order.orderid}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Date: ${order.orderdate ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Total: \$${String.format("%.2f", order.totalamount ?: 0.0)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
} 
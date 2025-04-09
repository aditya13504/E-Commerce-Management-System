package com.yourdomain.ecommerce.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourdomain.ecommerce.ui.viewmodels.OrderDetailViewModel
import com.yourdomain.ecommerce.ui.viewmodels.OrderDetailItem
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

@Composable
fun OrderDetailScreen(
    navController: NavController,
    orderId: Int?,
    detailViewModel: OrderDetailViewModel = viewModel(factory = OrderDetailViewModel.Factory())
) {
    val uiState by detailViewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        if (orderId != null) {
            detailViewModel.loadOrderDetails(orderId)
        } else {
             detailViewModel.setError("Invalid Order ID")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order #${orderId ?: "N/A"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                            if (orderId != null) { 
                                detailViewModel.loadOrderDetails(orderId) 
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.items.isNotEmpty() || uiState.shipment != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Shipment:", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val currentShipment = uiState.shipment 
                            if (currentShipment != null) { 
                                Text("Status: ${currentShipment.deliveryStatus ?: "N/A"}")
                                currentShipment.trackingNumber?.let { trackingNum ->
                                    Text("Tracking: $trackingNum")
                                }
                                currentShipment.shipmentDate?.let { shipmentDate ->
                                    Text("Shipped On: $shipmentDate")
                                }
                            } else {
                                Text("Shipment details not yet available.")
                            }
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        item {
                             Text("Items:", style = MaterialTheme.typography.titleMedium)
                             Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(uiState.items) { detailItem ->
                            OrderDetailListItem(
                                detailItem = detailItem,
                                isRequestingReturn = uiState.returnRequestInProgress.contains(detailItem.item.productId ?: -1),
                                returnRequestError = uiState.returnRequestError?.takeIf { 
                                    uiState.lastFailedReturnProductId == detailItem.item.productId 
                                },
                                onRequestReturn = {
                                    val orderIdInt = detailItem.item.orderId
                                    val productIdInt = detailItem.item.productId
                                    if (orderIdInt != null && productIdInt != null) {
                                        detailViewModel.requestReturnForItem(
                                            orderId = orderIdInt.toIntOrNull(),
                                            productId = productIdInt.toIntOrNull()
                                        )
                                    } else {
                                        println("Error: Cannot request return with null orderId or productId")
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                     Text("Order details not found or order is empty.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun OrderDetailListItem(
    detailItem: OrderDetailItem,
    isRequestingReturn: Boolean,
    returnRequestError: String?,
    onRequestReturn: () -> Unit
) {
    val canRequestReturn = detailItem.returnStatus == null
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${detailItem.productName ?: "(Product Unavailable)"}")
                Text(
                    text = "Unit Price: \$${String.format("%.2f", detailItem.item.itemPrice ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("Qty: ${detailItem.item.quantity ?: 0}", modifier = Modifier.padding(horizontal = 8.dp))
            
            if (detailItem.returnStatus != null) {
                Text(
                    text = "Return: ${detailItem.returnStatus}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Button(
                    onClick = onRequestReturn, 
                    enabled = canRequestReturn && !isRequestingReturn,
                    modifier = Modifier.padding(start = 8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (isRequestingReturn) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Return", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (returnRequestError != null) {
            Text(
                text = "Return Failed: $returnRequestError",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
            )
        }
    }
    Divider(modifier = Modifier.padding(vertical = 4.dp))
} 
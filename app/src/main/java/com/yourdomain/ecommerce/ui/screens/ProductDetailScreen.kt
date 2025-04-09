package com.yourdomain.ecommerce.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourdomain.ecommerce.ui.viewmodels.ProductDetailViewModel
import com.yourdomain.ecommerce.ui.viewmodels.ProductDetailUiState
import com.yourdomain.ecommerce.ui.viewmodels.CartViewModel // Need CartViewModel to add
import androidx.compose.material3.SnackbarHost // Import SnackbarHost
import androidx.compose.material3.SnackbarHostState // Import SnackbarHostState
import androidx.compose.runtime.remember // Import remember
import androidx.compose.runtime.rememberCoroutineScope // Import rememberCoroutineScope
import kotlinx.coroutines.launch // Import launch
import coil.compose.AsyncImage // Import Coil AsyncImage
import androidx.compose.material.icons.filled.ErrorOutline // Import error icon
import androidx.compose.ui.text.style.TextAlign // Import TextAlign
import androidx.lifecycle.ViewModelProvider // Import ViewModelProvider

@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: Int?,
    // Use Factory for ViewModels
    detailViewModel: ProductDetailViewModel = viewModel(factory = ProductDetailViewModel.Factory()), 
    cartViewModel: CartViewModel = viewModel(factory = CartViewModel.Factory())
) {
    val uiState by detailViewModel.uiState.collectAsState()
    // Remove Snackbar related state
    // val snackbarHostState = remember { SnackbarHostState() }
    // val scope = rememberCoroutineScope()

    // Load details when productId changes and is not null
    LaunchedEffect(productId) {
        if (productId != null) {
            detailViewModel.loadProductDetails(productId)
        } else {
            // Error for invalid ID is handled by the error state now
            // scope.launch { snackbarHostState.showSnackbar("Error: Invalid Product ID") }
             detailViewModel.setError("Invalid Product ID") // Set error state directly
            // Consider navigating back or showing error in place
            // navController.popBackStack()
        }
    }

    // Remove Snackbar LaunchedEffect
    // LaunchedEffect(uiState.error) { ... }

    Scaffold(
        // snackbarHost = { SnackbarHost(hostState = snackbarHostState) }, // Remove snackbar
        topBar = {
            TopAppBar(
                title = { Text(uiState.product?.name ?: "Product Details") },
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
                // Show prominent error view if error is present
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
                        // Add Retry button if applicable (e.g., network error)
                        Button(onClick = { 
                            if (productId != null) { 
                                detailViewModel.loadProductDetails(productId) 
                            } else {
                                // Optionally navigate back if ID was invalid
                                navController.popBackStack()
                            }
                        }) {
                            Text("Retry") // Or "Go Back" if ID was invalid
                        }
                    }
                }
                // Show product details if loaded successfully
                uiState.product != null -> {
                    val product = uiState.product!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Product details content (AsyncImage, Texts, Button)
                        // ... (existing content remains the same)
                         AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name ?: "Product image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp) // Adjust height as needed
                                .padding(bottom = 16.dp),
                        )
                        
                        Text(product.name ?: "No Name", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Price: \$${String.format("%.2f", product.price ?: 0.0)}", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Available Stock: ${product.stock ?: 0}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = product.description ?: "No description available.", 
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.weight(1f)) // Push button to bottom
                        Button(
                            onClick = { cartViewModel.addToCart(product) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = (product.stock ?: 0) > 0 // Disable if out of stock
                        ) {
                            Icon(Icons.Filled.AddShoppingCart, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Add to Cart")
                        }
                    }
                }
                 // Show a generic 'Not Found' if not loading, no error, and product is null
                 // This covers the initial state and potentially invalid ID cases if error isn't set
                else -> {
                    Text("Product not found.", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}


package com.yourdomain.ecommerce.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourdomain.ecommerce.AppDestinations
import com.yourdomain.ecommerce.core.data.model.Product
import com.yourdomain.ecommerce.ui.viewmodels.AuthViewModel
import com.yourdomain.ecommerce.ui.viewmodels.CartViewModel
import com.yourdomain.ecommerce.ui.viewmodels.ProductListUiState
import com.yourdomain.ecommerce.ui.viewmodels.ProductListViewModel
import kotlinx.coroutines.launch

@Composable
fun ProductListScreen(
    navController: NavController,
    productListViewModel: ProductListViewModel = viewModel(factory = ProductListViewModel.Factory()),
    cartViewModel: CartViewModel = viewModel(factory = CartViewModel.Factory()),
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory())
) {
    val uiState by productListViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Products") },
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.CART) }) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Shopping Cart")
                    }
                    IconButton(onClick = { navController.navigate(AppDestinations.ORDER_HISTORY) }) {
                        Icon(Icons.Filled.History, contentDescription = "Order History")
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        ProductListContent(
            uiState = uiState,
            onAddToCart = { product -> cartViewModel.addToCart(product) },
            modifier = Modifier.padding(paddingValues),
            onProductClick = { productId ->
                navController.navigate("${AppDestinations.PRODUCT_DETAIL_ROUTE}/$productId")
            },
            onRetry = { productListViewModel.loadProducts() }
        )
    }
}

@Composable
fun ProductListContent(
    uiState: ProductListUiState,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier,
    onProductClick: (Int) -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
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
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
            uiState.products.isEmpty() -> {
                Text(
                    text = "No products available at the moment.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.products) { product ->
                        ProductItem(
                            product = product,
                            onAddToCart = { onAddToCart(product) },
                            onProductClick = { product.productId?.let { id -> onProductClick(id) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onAddToCart: () -> Unit,
    onProductClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProductClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(product.name ?: "No Name", style = MaterialTheme.typography.titleMedium)
                Text("Stock: ${product.stock ?: 0}", style = MaterialTheme.typography.bodySmall)
                Text("\$${String.format("%.2f", product.price ?: 0.0)}", style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(
                onClick = onAddToCart, 
                enabled = (product.stock ?: 0) > 0
            ) {
                 Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "Add to Cart"
                 )
            }
        }
    }
}
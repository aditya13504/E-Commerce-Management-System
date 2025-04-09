package com.yourdomain.ecommerce.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yourdomain.ecommerce.AppDestinations // Import destinations

@Composable
fun OrderConfirmationScreen(
    navController: NavController,
    orderId: Int? // Receive orderId as argument
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Order Confirmation") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your order has been placed successfully!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            if (orderId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Order ID: $orderId",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { 
                // Navigate back to product list, clearing the back stack up to it
                navController.navigate(AppDestinations.PRODUCT_LIST) {
                    popUpTo(AppDestinations.PRODUCT_LIST) {
                        inclusive = true
                    }
                    launchSingleTop = true // Avoid multiple copies of product list
                }
            }) {
                Text("Continue Shopping")
            }
        }
    }
} 
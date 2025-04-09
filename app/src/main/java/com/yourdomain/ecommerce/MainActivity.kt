package com.yourdomain.ecommerce

import androidx.compose.runtime.getValue
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.yourdomain.ecommerce.ui.screens.ProductListScreen
import com.yourdomain.ecommerce.ui.screens.CartScreen
import com.yourdomain.ecommerce.ui.screens.OrderConfirmationScreen
import com.yourdomain.ecommerce.ui.screens.OrderHistoryScreen
import com.yourdomain.ecommerce.ui.screens.ProductDetailScreen
import com.yourdomain.ecommerce.ui.theme.ECommerceTheme
import com.yourdomain.ecommerce.ui.screens.AuthScreen
import com.yourdomain.ecommerce.ui.viewmodels.AuthViewModel
import com.yourdomain.ecommerce.ui.screens.OrderDetailScreen

// Define navigation routes
object AppDestinations {
    const val AUTH = "auth"
    const val PRODUCT_LIST = "productList"
    const val CART = "cart"
    const val ORDER_CONFIRMATION_ROUTE = "orderConfirmation"
    const val ORDER_ID_ARG = "orderId"
    const val ORDER_CONFIRMATION = "$ORDER_CONFIRMATION_ROUTE/{$ORDER_ID_ARG}"
    const val ORDER_HISTORY = "orderHistory"
    const val PRODUCT_DETAIL_ROUTE = "productDetail"
    const val PRODUCT_ID_ARG = "productId"
    const val PRODUCT_DETAIL = "$PRODUCT_DETAIL_ROUTE/{$PRODUCT_ID_ARG}"
    const val ORDER_DETAIL_ROUTE = "orderDetail"
    const val ORDER_DETAIL = "$ORDER_DETAIL_ROUTE/{$ORDER_ID_ARG}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ECommerceApp()
        }
    }
}

@Composable
fun ECommerceApp(authViewModel: AuthViewModel = viewModel()) {
    ECommerceTheme {
        val authState by authViewModel.uiState.collectAsState()
        val navController = rememberNavController()
        
        val startDestination = when (authState.isAuthenticated) {
            true -> AppDestinations.PRODUCT_LIST
            false -> AppDestinations.AUTH
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(AppDestinations.AUTH) {
                    AuthScreen(navController = navController)
                }
                composable(AppDestinations.PRODUCT_LIST) {
                    ProductListScreen(navController = navController)
                }
                composable(AppDestinations.CART) {
                    CartScreen(navController = navController)
                }
                composable(
                    route = AppDestinations.ORDER_CONFIRMATION,
                    arguments = listOf(navArgument(AppDestinations.ORDER_ID_ARG) { type = NavType.IntType; nullable = true })
                ) {
                    val orderId = it.arguments?.getInt(AppDestinations.ORDER_ID_ARG)
                    OrderConfirmationScreen(navController = navController, orderId = orderId)
                }
                composable(AppDestinations.ORDER_HISTORY) {
                    OrderHistoryScreen(navController = navController)
                }
                composable(
                    route = AppDestinations.ORDER_DETAIL,
                    arguments = listOf(navArgument(AppDestinations.ORDER_ID_ARG) { type = NavType.IntType; nullable = false })
                ) {
                    val orderId = it.arguments?.getInt(AppDestinations.ORDER_ID_ARG)
                    OrderDetailScreen(navController = navController, orderId = orderId)
                }
                composable(
                    route = AppDestinations.PRODUCT_DETAIL,
                    arguments = listOf(navArgument(AppDestinations.PRODUCT_ID_ARG) { type = NavType.IntType })
                ) {
                    val productId = it.arguments?.getInt(AppDestinations.PRODUCT_ID_ARG)
                    if (productId != null) {
                        ProductDetailScreen(navController = navController, productId = productId)
                    } else {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ECommerceTheme {
        Text("Preview - App Structure")
    }
} 
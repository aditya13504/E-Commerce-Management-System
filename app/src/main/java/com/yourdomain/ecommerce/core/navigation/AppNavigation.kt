package com.yourdomain.ecommerce.core.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourdomain.ecommerce.presentation.auth.LoginScreen
import com.yourdomain.ecommerce.presentation.auth.RegisterScreen
import com.yourdomain.ecommerce.presentation.cart.CartScreen
import com.yourdomain.ecommerce.presentation.checkout.CheckoutScreen
import com.yourdomain.ecommerce.presentation.orders.OrdersScreen
import com.yourdomain.ecommerce.presentation.product.ProductDetailScreen
import com.yourdomain.ecommerce.presentation.products.ProductsScreen

/**
 * Navigation routes for the app
 */
object AppDestination {
    const val PRODUCTS_ROUTE = "products"
    const val PRODUCT_DETAIL_ROUTE = "product"
    const val CART_ROUTE = "cart"
    const val CHECKOUT_ROUTE = "checkout"
    const val ORDERS_ROUTE = "orders"
    const val LOGIN_ROUTE = "login"
    const val REGISTER_ROUTE = "register"
    
    // Helper function to create product detail route with ID
    fun productDetail(productId: String) = "product/$productId"
}

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestination.LOGIN_ROUTE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = AppDestination.LOGIN_ROUTE) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(AppDestination.REGISTER_ROUTE)
                },
                onNavigateToHome = {
                    navController.navigate(AppDestination.PRODUCTS_ROUTE) {
                        popUpTo(AppDestination.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = AppDestination.REGISTER_ROUTE) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(AppDestination.PRODUCTS_ROUTE) {
                        popUpTo(AppDestination.LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = AppDestination.PRODUCTS_ROUTE) {
            ProductsScreen(
                onNavigateToProductDetail = { productId ->
                    navController.navigate("${AppDestination.PRODUCT_DETAIL_ROUTE}/$productId")
                },
                onNavigateToCart = {
                    navController.navigate(AppDestination.CART_ROUTE)
                },
                onNavigateToOrders = {
                    navController.navigate(AppDestination.ORDERS_ROUTE)
                }
            )
        }
        
        composable(
            route = "${AppDestination.PRODUCT_DETAIL_ROUTE}/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                }
            )
        ) {
            val productId = it.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                onNavigateUp = {
                    navController.navigateUp()
                },
                onAddToCart = { _, _ ->
                    navController.navigate(AppDestination.CART_ROUTE) {
                        // Make sure back navigation from cart returns to products
                        popUpTo(AppDestination.PRODUCTS_ROUTE)
                    }
                }
            )
        }
        
        composable(route = AppDestination.CART_ROUTE) {
            CartScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onNavigateToCheckout = {
                    navController.navigate(AppDestination.CHECKOUT_ROUTE)
                }
            )
        }
        
        composable(route = AppDestination.CHECKOUT_ROUTE) {
            CheckoutScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onCheckoutComplete = {
                    // Navigate to orders after checkout
                    navController.navigate(AppDestination.ORDERS_ROUTE) {
                        popUpTo(AppDestination.PRODUCTS_ROUTE)
                    }
                }
            )
        }
        
        composable(route = AppDestination.ORDERS_ROUTE) {
            OrdersScreen(
                onNavigateUp = {
                    navController.navigateUp()
                },
                onNavigateToOrderDetails = { orderId ->
                    // For future implementation - navigate to order details
                }
            )
        }
    }
} 
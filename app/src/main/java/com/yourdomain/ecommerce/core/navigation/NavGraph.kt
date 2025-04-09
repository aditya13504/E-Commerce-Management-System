package com.yourdomain.ecommerce.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.yourdomain.ecommerce.presentation.authentication.LoginScreen
import com.yourdomain.ecommerce.presentation.authentication.RegisterScreen
import com.yourdomain.ecommerce.presentation.cart.CartScreen
import com.yourdomain.ecommerce.presentation.cart.CheckoutScreen
import com.yourdomain.ecommerce.presentation.cart.OrderConfirmationScreen
import com.yourdomain.ecommerce.presentation.home.HomeScreen
import com.yourdomain.ecommerce.presentation.orders.OrderDetailScreen
import com.yourdomain.ecommerce.presentation.orders.OrdersScreen
import com.yourdomain.ecommerce.presentation.products.ProductDetailScreen
import com.yourdomain.ecommerce.presentation.products.ProductsScreen
import com.yourdomain.ecommerce.presentation.profile.AddressListScreen
import com.yourdomain.ecommerce.presentation.profile.EditAddressScreen
import com.yourdomain.ecommerce.presentation.profile.EditProfileScreen
import com.yourdomain.ecommerce.presentation.profile.ProfileScreen
import com.yourdomain.ecommerce.ui.viewmodels.ProductDetailViewModel

/**
 * Root navigation graph that contains nested navigation
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = AppDestination.Auth.route,
    isUserLoggedIn: Boolean = false
) {
    val actions = remember(navController) { NavigationActions(navController) }
    
    // Determine start destination based on user login status
    val actualStartDestination = if (isUserLoggedIn) {
        AppDestination.Main.route
    } else {
        startDestination
    }
    
    NavHost(
        navController = navController,
        startDestination = actualStartDestination
    ) {
        // Authentication graph
        authenticationGraph(actions)
        
        // Main app graph (includes bottom navigation)
        mainGraph(actions)
    }
}

/**
 * Authentication navigation graph
 */
private fun NavGraphBuilder.authenticationGraph(
    actions: NavigationActions
) {
    navigation(
        startDestination = AppDestination.Login.route,
        route = AppDestination.Auth.route
    ) {
        composable(route = AppDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = actions.navigateToMain,
                onNavigateToRegister = actions.navigateToRegister
            )
        }
        
        composable(route = AppDestination.Register.route) {
            RegisterScreen(
                onRegisterSuccess = actions.navigateToMain,
                onNavigateToLogin = actions.navigateToLogin
            )
        }
    }
}

/**
 * Main app navigation graph containing nested graphs for bottom navigation
 */
private fun NavGraphBuilder.mainGraph(
    actions: NavigationActions
) {
    navigation(
        startDestination = AppDestination.Home.route,
        route = AppDestination.Main.route
    ) {
        // Home screen
        composable(route = AppDestination.Home.route) {
            HomeScreen(
                onProductClick = actions.navigateToProductDetail
            )
        }
        
        // Products screens
        composable(route = AppDestination.Products.route) {
            ProductsScreen(
                onProductClick = actions.navigateToProductDetail
            )
        }
        
        composable(
            route = AppDestination.ProductDetail.route,
            arguments = AppDestination.ProductDetail.navArguments
        ) {
            val viewModel = viewModel<ProductDetailViewModel>(factory = ProductDetailViewModel.Factory())
            ProductDetailScreen(
                onNavigateUp = actions.navigateUp,
                onAddToCart = actions.navigateToCart
            )
        }
        
        // Cart & Checkout screens
        composable(route = AppDestination.Cart.route) {
            CartScreen(
                onCheckout = actions.navigateToCheckout,
                onContinueShopping = actions.navigateToProducts
            )
        }
        
        composable(route = AppDestination.Checkout.route) {
            CheckoutScreen(
                onPlaceOrder = { orderId -> actions.navigateToOrderConfirmation(orderId) },
                onNavigateUp = actions.navigateUp
            )
        }
        
        composable(
            route = AppDestination.OrderConfirmation.route,
            arguments = AppDestination.OrderConfirmation.navArguments
        ) {
            OrderConfirmationScreen(
                onContinueShopping = actions.navigateToHome,
                onViewOrder = { orderId -> actions.navigateToOrderDetail(orderId) }
            )
        }
        
        // Orders screens
        composable(route = AppDestination.Orders.route) {
            OrdersScreen(
                onOrderClick = actions.navigateToOrderDetail
            )
        }
        
        composable(
            route = AppDestination.OrderDetail.route,
            arguments = AppDestination.OrderDetail.navArguments
        ) {
            OrderDetailScreen(
                onNavigateUp = actions.navigateUp
            )
        }
        
        // Profile screens
        composable(route = AppDestination.Profile.route) {
            ProfileScreen(
                onEditProfile = actions.navigateToEditProfile,
                onManageAddresses = actions.navigateToAddressList,
                onLogout = actions.navigateToAuth
            )
        }
        
        composable(route = AppDestination.EditProfile.route) {
            EditProfileScreen(
                onNavigateUp = actions.navigateUp
            )
        }
        
        composable(route = AppDestination.AddressList.route) {
            AddressListScreen(
                onAddAddress = { actions.navigateToEditAddress() },
                onEditAddress = actions.navigateToEditAddress,
                onNavigateUp = actions.navigateUp
            )
        }
        
        composable(
            route = AppDestination.EditAddress.route,
            arguments = AppDestination.EditAddress.navArguments
        ) {
            EditAddressScreen(
                onNavigateUp = actions.navigateUp
            )
        }
    }
}

/**
 * Helper class to encapsulate navigation actions
 */
class NavigationActions(private val navController: NavHostController) {
    
    val navigateUp: () -> Unit = {
        navController.navigateUp()
    }
    
    // Auth navigation
    val navigateToLogin: () -> Unit = {
        navController.navigate(AppDestination.Login.route) {
            popUpTo(AppDestination.Auth.route)
        }
    }
    
    val navigateToRegister: () -> Unit = {
        navController.navigate(AppDestination.Register.route) {
            popUpTo(AppDestination.Auth.route)
        }
    }
    
    // Main navigation
    val navigateToAuth: () -> Unit = {
        navController.navigate(AppDestination.Auth.route) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
        }
    }
    
    val navigateToMain: () -> Unit = {
        navController.navigate(AppDestination.Main.route) {
            popUpTo(navController.graph.id) {
                inclusive = true
            }
        }
    }
    
    // Tab navigation
    val navigateToHome: () -> Unit = {
        navController.navigate(AppDestination.Home.route) {
            popUpTo(AppDestination.Main.route)
        }
    }
    
    val navigateToProducts: () -> Unit = {
        navController.navigate(AppDestination.Products.route) {
            popUpTo(AppDestination.Main.route)
        }
    }
    
    val navigateToCart: () -> Unit = {
        navController.navigate(AppDestination.Cart.route) {
            popUpTo(AppDestination.Main.route)
            launchSingleTop = true
        }
    }
    
    // Nested navigation
    val navigateToProductDetail: (String) -> Unit = { productId ->
        navController.navigate(AppDestination.ProductDetail.createRoute(productId))
    }
    
    val navigateToCheckout: () -> Unit = {
        navController.navigate(AppDestination.Checkout.route)
    }
    
    val navigateToOrderConfirmation: (String) -> Unit = { orderId ->
        navController.navigate(AppDestination.OrderConfirmation.createRoute(orderId)) {
            popUpTo(AppDestination.Cart.route) {
                inclusive = true
            }
        }
    }
    
    val navigateToOrderDetail: (String) -> Unit = { orderId ->
        navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
    }
    
    val navigateToEditProfile: () -> Unit = {
        navController.navigate(AppDestination.EditProfile.route)
    }
    
    val navigateToAddressList: () -> Unit = {
        navController.navigate(AppDestination.AddressList.route)
    }
    
    val navigateToEditAddress: (String? = null) -> Unit = { addressId ->
        navController.navigate(AppDestination.EditAddress.createRoute(addressId))
    }
}
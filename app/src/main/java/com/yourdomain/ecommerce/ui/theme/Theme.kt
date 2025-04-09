package com.yourdomain.ecommerce.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * E-commerce app theme implementation using Material 3 design system.
 * 
 * Features:
 * - Dynamic color support for Android 12+
 * - Light and dark themes with cohesive color palettes
 * - Custom typography with system fonts
 * - Consistent shape system for UI components
 * - Custom spacing and elevation scales
 * - Extended color system for e-commerce specific states
 * 
 * Optimized for Android Studio 2025 and modern app development patterns.
 */

// Light theme color scheme
private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    
    secondary = Coral500,
    onSecondary = White,
    secondaryContainer = Coral100,
    onSecondaryContainer = Coral900,
    
    tertiary = Teal500,
    onTertiary = White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Teal900,
    
    error = Red500,
    onError = White,
    errorContainer = Red100,
    onErrorContainer = Red900,
    
    background = White,
    onBackground = Gray900,
    
    surface = White,
    onSurface = Gray900,
    
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,
    
    outline = Gray400,
    outlineVariant = Gray300
)

// Dark theme color scheme
private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = Blue900,
    primaryContainer = Blue800,
    onPrimaryContainer = Blue100,
    
    secondary = Coral200,
    onSecondary = Coral900,
    secondaryContainer = Coral800,
    onSecondaryContainer = Coral100,
    
    tertiary = Teal200,
    onTertiary = Teal900,
    tertiaryContainer = Teal800,
    onTertiaryContainer = Teal100,
    
    error = Red200,
    onError = Red900,
    errorContainer = Red800,
    onErrorContainer = Red100,
    
    background = Gray900,
    onBackground = Gray100,
    
    surface = Gray900,
    onSurface = Gray100,
    
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray300,
    
    outline = Gray500,
    outlineVariant = Gray700
)

/**
 * Extended colors specific to e-commerce functionality
 */
data class ECommerceColors(
    // Success states for order confirmation, successful payments
    val success: androidx.compose.ui.graphics.Color,
    val onSuccess: androidx.compose.ui.graphics.Color,
    val successContainer: androidx.compose.ui.graphics.Color,
    val onSuccessContainer: androidx.compose.ui.graphics.Color,
    
    // Warning states for low stock, almost expired offers
    val warning: androidx.compose.ui.graphics.Color,
    val onWarning: androidx.compose.ui.graphics.Color,
    val warningContainer: androidx.compose.ui.graphics.Color,
    val onWarningContainer: androidx.compose.ui.graphics.Color,
    
    // Promo colors for sales and special offers
    val promo: androidx.compose.ui.graphics.Color,
    val onPromo: androidx.compose.ui.graphics.Color,
    
    // Rating colors for product reviews
    val ratingActive: androidx.compose.ui.graphics.Color,
    val ratingInactive: androidx.compose.ui.graphics.Color,
    
    // Additional surface variants for cards and layering
    val surfaceHighlight: androidx.compose.ui.graphics.Color,
    val surfaceBright: androidx.compose.ui.graphics.Color,
    val surfaceDim: androidx.compose.ui.graphics.Color
)

/**
 * Spacing scale for consistent layout measurements
 */
data class ECommerceSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp,
    val xxxl: Dp = 48.dp
)

/**
 * Elevation scale for consistent shadow elevations
 */
data class ECommerceElevation(
    val none: Dp = 0.dp,
    val xxs: Dp = 1.dp,
    val xs: Dp = 2.dp,
    val s: Dp = 4.dp,
    val m: Dp = 8.dp,
    val l: Dp = 12.dp,
    val xl: Dp = 16.dp,
    val xxl: Dp = 24.dp
)

/**
 * Custom typography for E-commerce app
 * Uses SF Pro as the primary font for iOS-like premium feel
 * with Roboto as fallback
 */
val ECommerceTypography = Typography(
    // Display styles
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Light,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Light,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    
    // Headline styles
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    
    // Title styles
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    
    // Body styles
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    
    // Label styles
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SfPro,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Custom shapes for E-commerce app
 * Defines rounded corners for different component types
 */
val ECommerceShapes = androidx.compose.material3.Shapes(
    // More rounded corners for small components (buttons, chips)
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    
    // Medium roundness for cards, dialogs
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    
    // Large elements like bottom sheets, expanded dialogs
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    
    // Extra large elements
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

// CompositionLocal providers for custom theme elements
private val LocalECommerceColors = staticCompositionLocalOf {
    ECommerceColors(
        success = Green500,
        onSuccess = White,
        successContainer = Green100,
        onSuccessContainer = Green900,
        warning = Amber500,
        onWarning = Gray900,
        warningContainer = Amber100,
        onWarningContainer = Amber900,
        promo = Coral500,
        onPromo = White,
        ratingActive = Amber500,
        ratingInactive = Gray300,
        surfaceHighlight = Blue50,
        surfaceBright = White,
        surfaceDim = Gray100
    )
}

private val LocalECommerceSpacing = staticCompositionLocalOf { 
    ECommerceSpacing() 
}

private val LocalECommerceElevation = staticCompositionLocalOf { 
    ECommerceElevation() 
}

/**
 * E-commerce app theme composable
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic color (Android 12+)
 * @param content The content to be themed
 */
@Composable
fun ECommerceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Determine color scheme based on dynamic color support and theme
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Extended colors for e-commerce functionality
    val ecommerceColors = if (darkTheme) {
        ECommerceColors(
            success = Green300,
            onSuccess = Green900,
            successContainer = Green800,
            onSuccessContainer = Green100,
            warning = Amber300,
            onWarning = Amber900,
            warningContainer = Amber800,
            onWarningContainer = Amber100,
            promo = Coral300,
            onPromo = Coral900,
            ratingActive = Amber300,
            ratingInactive = Gray700,
            surfaceHighlight = Blue900,
            surfaceBright = Gray800,
            surfaceDim = Gray900
        )
    } else {
        ECommerceColors(
            success = Green500,
            onSuccess = White,
            successContainer = Green100,
            onSuccessContainer = Green900,
            warning = Amber500,
            onWarning = Gray900,
            warningContainer = Amber100,
            onWarningContainer = Amber900,
            promo = Coral500,
            onPromo = White,
            ratingActive = Amber500,
            ratingInactive = Gray300,
            surfaceHighlight = Blue50,
            surfaceBright = White,
            surfaceDim = Gray100
        )
    }
    
    // Apply status bar color
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Set status bar color 
            window.statusBarColor = colorScheme.primary.toArgb()
            
            // Set light/dark status bar icons based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    // Provide custom theme properties
    CompositionLocalProvider(
        LocalECommerceColors provides ecommerceColors,
        LocalECommerceSpacing provides ECommerceSpacing(),
        LocalECommerceElevation provides ECommerceElevation()
    ) {
        // Apply Material 3 theme with our customizations
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ECommerceTypography,
            shapes = ECommerceShapes,
            content = content
        )
    }
}

/**
 * Extension properties to easily access custom theme elements
 */
object ECommerceTheme {
    val colors: ECommerceColors
        @Composable
        @ReadOnlyComposable
        get() = LocalECommerceColors.current
    
    val spacing: ECommerceSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalECommerceSpacing.current
    
    val elevation: ECommerceElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalECommerceElevation.current
} 
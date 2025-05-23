package com.example.finanzaspersonales.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.ExperimentalFoundationApi

// Enhanced color schemes with financial app-appropriate colors
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    // Financial-specific colors
    primaryContainer = Purple80.copy(alpha = 0.3f),
    secondaryContainer = PurpleGrey80.copy(alpha = 0.3f),
    tertiaryContainer = Pink80.copy(alpha = 0.3f),
    // Surface colors for cards and elevated components
    surfaceVariant = Purple80.copy(alpha = 0.1f)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // Financial-specific colors
    primaryContainer = Purple40.copy(alpha = 0.2f),
    secondaryContainer = PurpleGrey40.copy(alpha = 0.2f),
    tertiaryContainer = Pink40.copy(alpha = 0.2f),
    // Surface colors for cards and elevated components
    surfaceVariant = Purple40.copy(alpha = 0.05f)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinanzasPersonalesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    windowSizeClass: WindowSizeClass? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Helper to determine if we should use compact navigation
@Composable
fun shouldUseCompactNavigation(windowSizeClass: WindowSizeClass?): Boolean {
    return windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact
}

// Helper to determine if we should use navigation rail
@Composable
fun shouldUseNavigationRail(windowSizeClass: WindowSizeClass?): Boolean {
    return windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Medium
}

// Helper to determine if we should use navigation drawer
@Composable
fun shouldUseNavigationDrawer(windowSizeClass: WindowSizeClass?): Boolean {
    return windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded
}
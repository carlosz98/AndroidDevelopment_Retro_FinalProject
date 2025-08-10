package com.example.hubretro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme // Composable function
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Explicitly import YOUR Typography OBJECT from Type.kt
import com.example.hubretro.ui.theme.Typography

// Using colors from your Color.kt (ensure these are defined there)
private val DarkRetroColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = RetroDarkBlue, // from Color.kt
    surface = RetroDarkBlue,    // from Color.kt
    onPrimary = Purple40,
    onSecondary = PurpleGrey40,
    onTertiary = Pink40,
    onBackground = RetroTextOffWhite, // from Color.kt
    onSurface = RetroTextOffWhite     // from Color.kt
)

// Using default M3 light colors as a base, customize as needed from Color.kt
private val LightRetroColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // background = Color(0xFFFFFBFE), // Default M3
    // surface = Color(0xFFFFFBFE),    // Default M3
    // onPrimary = Color.White,
    // onSecondary = Color.White,
    // onTertiary = Color.White,
    // onBackground = Color(0xFF1C1B1F),
    // onSurface = Color(0xFF1C1B1F)

    // You can override with your custom light theme colors from Color.kt if you have them
)

@Composable
fun HubRetroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false if you prefer your explicit theme colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkRetroColorScheme
        else -> LightRetroColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar color - you might want to pick a specific color from your scheme
            window.statusBarColor = colorScheme.background.toArgb() // Or colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Uses your Typography object from Type.kt
        content = content
    )
}

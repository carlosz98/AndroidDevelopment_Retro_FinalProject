package com.example.hubretro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // Make sure this is imported for Color.Transparent etc.
import androidx.compose.ui.platform.LocalContext

// Using your custom retro colors for the dark theme
private val DarkRetroColorScheme = darkColorScheme(
    primary = Purple80, // You can later change these to more retro "primary" colors if desired
    secondary = PurpleGrey80,
    tertiary = Pink80,

    background = RetroDarkBlue,         // Your retro background
    surface = RetroDarkBlue,            // Surfaces like cards could also be RetroDarkBlue or a variant
    onPrimary = Purple40,
    onSecondary = PurpleGrey40,
    onTertiary = Pink40,
    onBackground = RetroTextOffWhite,   // Text on your retro background
    onSurface = RetroTextOffWhite       // Text on your retro surfaces
)

// A more standard light theme, but you can customize this with retro light colors too
private val LightRetroColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    background = Color(0xFFFFFBFE),     // Default light background
    surface = Color(0xFFFFFBFE),        // Default light surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),   // Default text on light background
    onSurface = Color(0xFF1C1B1F)       // Default text on light surface

    /* You could override the light theme with retro light colors too:
    background = RetroTextOffWhite,
    onBackground = RetroDarkBlue,
    primary = RetroPlayerOneBlue,
    etc.
    */
)

@Composable
fun HubRetroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // For a strong retro theme, you might want to set dynamicColor to false
    // to enforce your specific palette.
    dynamicColor: Boolean = false, // Set to false to prioritize your retro theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkRetroColorScheme   // Use our DarkRetroColorScheme
        else -> LightRetroColorScheme       // Use our LightRetroColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // This comes from your Type.kt
        content = content
    )
}

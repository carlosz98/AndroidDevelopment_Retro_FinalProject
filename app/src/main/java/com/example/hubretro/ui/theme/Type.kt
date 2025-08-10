package com.example.hubretro.ui.theme

import androidx.compose.material3.Typography // Correct import for the CLASS
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.hubretro.R // Ensure R.font.press_start_2p_regular exists

// Define our Retro Font Family
val RetroFontFamily = FontFamily(
    Font(R.font.press_start_2p_regular, FontWeight.Normal)
    // If you add bold or other styles for this font, define them here:
    // Font(R.font.press_start_2p_bold, FontWeight.Bold)
)

// Define the Typography object that will be used by the MaterialTheme
// Explicitly typed for clarity and safety.
val Typography: androidx.compose.material3.Typography = Typography( // Constructor from Material 3
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // Or RetroFontFamily if default for large body
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp // Adjust size as pixel fonts can be tricky
    ),
    // This style will likely be picked up by Text() by default if not overridden
    bodyMedium = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // A common default size
        lineHeight = 20.sp
    ),
    // Example: For the robot's text or other small labels
    labelSmall = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Normal, // Or FontWeight.Medium
        fontSize = 12.sp, // Pixel fonts might need specific, often smaller, sizes for labels
        lineHeight = 16.sp
    ),
    // Add other styles as needed (e.g., titleLarge, displayMedium)
    // Example for Drawer items or AppBar titles
    titleMedium = TextStyle(
        fontFamily = RetroFontFamily,
        fontWeight = FontWeight.Normal, // Or FontWeight.Bold
        fontSize = 18.sp
    )
)

package com.example.hubretro.ui.theme // Your package name

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.hubretro.R // Make sure this import is present

// Define our Retro Font Family
val RetroFontFamily = FontFamily(
    Font(R.font.press_start_2p_regular, FontWeight.Normal) // Corrected name
)

// Default Typography object
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RetroFontFamily, // Using the retro font for headlines
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp // Pixel fonts often need specific sizing; adjust as needed
    )
    // ... other text styles
)


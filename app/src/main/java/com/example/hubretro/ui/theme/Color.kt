package com.example.hubretro.ui.theme

import androidx.compose.ui.graphics.Color

// Default Material 3 Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Your Custom Retro Colors
val RetroDarkBlue = Color(0xFF2A2A3D)      // For general retro background
val RetroTextOffWhite = Color(0xFFF0F0F0)  // For text on dark retro backgrounds
val AppBarBackground = Color(0xFF1A1A2E)    // Specific for AppBar background
val AppBarTitleColor = Color(0xFFDCDCDC)    // Specific for AppBar title
val RetroBorderColor = Color(0xFF808080)    // For borders around elements
val VaporwavePink = Color(0xFFF72585)      // For pink action

// --- Colors for FeatureNavigationCard & ArticleCard gradients ---
val VaporwaveBlue = Color(0xFF00FFFF)    // Bright cyan/aqua
val VaporwavePurple = Color(0xFF9400D3)  // Dark violet / deep purple
val VaporwaveCyan = Color(0xFF00B7FF)    // A lighter, sky blue cyan
val SynthwaveOrange = Color(0xFFFF8C00)  // Dark orange / sunset orange
val SynthwavePurple = Color(0xFF8A2BE2) // Example: BlueViolet for more options
val VaporwaveGreen = Color(0xFF00FF7F)  // Example: SpringGreen for more options


// --- Gradient Sets for Article Cards ---
val ArticleGradientSet1 = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.75f))
val ArticleGradientSet2 = listOf(VaporwavePurple, VaporwaveCyan.copy(alpha = 0.75f))
val ArticleGradientSet3 = listOf(SynthwaveOrange, VaporwavePink.copy(alpha = 0.70f))
val ArticleGradientSet4 = listOf(VaporwaveBlue, SynthwavePurple.copy(alpha = 0.70f))
val ArticleGradientSet5 = listOf(VaporwaveCyan, VaporwaveGreen.copy(alpha = 0.75f))
val ArticleGradientSet6 = listOf(SynthwavePurple, SynthwaveOrange.copy(alpha = 0.65f))
// Add more sets as you desire, mixing and matching your defined colors

// --- List of all gradient sets for cycling through in the UI ---
val articleGradientColorsList = listOf(
    ArticleGradientSet1,
    ArticleGradientSet2,
    ArticleGradientSet3,
    ArticleGradientSet4,
    ArticleGradientSet5,
    ArticleGradientSet6
    // Add any new sets here as well
)


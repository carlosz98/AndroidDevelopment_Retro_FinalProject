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
val RetroGold = Color(0xFFFFD700)          // <<< --- ADDED THIS LINE --- (Standard Gold)

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

// In your com.example.hubretro.ui.theme.Color.kt

// ... (your existing colors) // This comment seems redundant now with the full list above it.

// --- New Colors Suggested for ProfileScreen & General Use ---

// Let's assume AppBarBackground is your primary dark background
val RetroBackground = AppBarBackground // Or RetroDarkBlue if you prefer for general content

// An alternative background, slightly lighter or different hue for cards/sections
val RetroBackgroundAlt = Color(0xFF2C2C4D) // Example: A slightly lighter desaturated blue/purple than AppBarBackground
// Adjust this hex value to what looks good with your theme.
// Could also be RetroDarkBlue if AppBarBackground is much darker.

// In ui/theme/Color.kt
val RetroTextSecondary = Color(0xFFB0B0B0) // A light grey example

// Specific accent colors based on your existing palette
val RetroAccentPurple = VaporwavePurple // Using your existing VaporwavePurple as a primary purple accent
val RetroAccentBlue = Color(0xFF4A90E2) // A new distinct retro blue, less bright than VaporwaveBlue
// or use VaporwaveCyan if you prefer: Color(0xFF00B7FF)

val VaporwaveTeal = Color(0xFF00DAC6)   // A vibrant teal, distinct from VaporwaveBlue/Cyan

// You can also reuse existing colors if they fit:
// For example, instead of a new VaporwaveTeal, you could use VaporwaveBlue or VaporwaveCyan
// if you want fewer distinct accent colors.

// ... (rest of your Color.kt file like gradient sets) // Also seems redundant here


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

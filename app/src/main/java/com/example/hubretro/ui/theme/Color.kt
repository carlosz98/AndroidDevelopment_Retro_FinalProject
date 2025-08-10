package com.example.hubretro.ui.theme

import androidx.compose.ui.graphics.Color

// Default Material 3 Colors (as provided in your initial file)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- Your Custom Retro & Vaporwave Colors ---

// Core Palette
val RetroDarkBlue = Color(0xFF2A2A3D)      // General retro background
val RetroTextOffWhite = Color(0xFFF0F0F0)  // For text on dark retro backgrounds
val AppBarBackground = Color(0xFF1A1A2E)    // Specific for AppBar background (often used as primary dark)
val AppBarTitleColor = Color(0xFFDCDCDC)    // Specific for AppBar title

// Accents & Borders
val VaporwavePink = Color(0xFFF72585)      // Prominent pink accent
val RetroGold = Color(0xFFFFD700)          // Gold accent
val RetroBorderColor = Color(0xFF808080)    // For general borders

// Blues & Cyans
val VaporwaveBlue = Color(0xFF00FFFF)    // Bright cyan/aqua
val VaporwaveCyan = Color(0xFF00B7FF)    // A lighter, sky blue cyan
val RetroAccentBlue = Color(0xFF4A90E2)  // A more subdued, distinct retro blue (as in your previous Color.kt)

// Purples
val VaporwavePurple = Color(0xFF9400D3)  // Dark violet / deep purple
val SynthwavePurple = Color(0xFF8A2BE2)  // Example: BlueViolet (as in your previous Color.kt)
val RetroDarkPurple = Color(0xFF301934)  // **ADDED**: Dark purple for NewsCard background, etc.
// (Alternative: 0xFF4B0082 - Indigo, 0xFF240046 - Very Dark Purple)
val RetroAccentPurple = VaporwavePurple  // Re-using VaporwavePurple as a primary purple accent (as in your previous Color.kt)


// Greens & Oranges
val SynthwaveOrange = Color(0xFFFF8C00)  // Dark orange / sunset orange
val VaporwaveGreen = Color(0xFF00FF7F)  // Example: SpringGreen (as in your previous Color.kt)
val VaporwaveTeal = Color(0xFF00DAC6)    // A vibrant teal (as in your previous Color.kt)


// --- Semantic Colors (derived or specific use-cases based on your previous Color.kt) ---

// Backgrounds
val RetroBackground = AppBarBackground        // Primary background for screens/sections
val RetroBackgroundAlt = Color(0xFF2C2C4D) // An alternative, slightly lighter or different hue for cards/sections

// Text
val RetroTextPrimary = RetroTextOffWhite   // Primary text color
val RetroTextSecondary = Color(0xFFB0B0B0) // Secondary text (e.g., subtitles, less important info)


// --- Gradient Sets for Article Cards (as provided in your initial file) ---
val ArticleGradientSet1 = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.75f))
val ArticleGradientSet2 = listOf(VaporwavePurple, VaporwaveCyan.copy(alpha = 0.75f))
val ArticleGradientSet3 = listOf(SynthwaveOrange, VaporwavePink.copy(alpha = 0.70f))
val ArticleGradientSet4 = listOf(VaporwaveBlue, SynthwavePurple.copy(alpha = 0.70f))
val ArticleGradientSet5 = listOf(VaporwaveCyan, VaporwaveGreen.copy(alpha = 0.75f))
val ArticleGradientSet6 = listOf(SynthwavePurple, SynthwaveOrange.copy(alpha = 0.65f))
// Add more sets as you desire

// --- List of all gradient sets for cycling through in the UI (as provided) ---
val articleGradientColorsList = listOf(
    ArticleGradientSet1,
    ArticleGradientSet2,
    ArticleGradientSet3,
    ArticleGradientSet4,
    ArticleGradientSet5,
    ArticleGradientSet6
    // Add any new sets here as well
)

// You can add more specific color definitions here as your app grows.
// For example, if you need a specific color for error messages, success indicators, etc.
// val ErrorColor = Color(0xFFFF5252)
// val SuccessColor = Color(0xFF4CAF50)


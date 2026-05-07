package com.example.hubretro.ui.theme

import androidx.compose.ui.graphics.Color

// Default Material 3 Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- Existing Retro & Vaporwave Colors ---
val RetroDarkBlue = Color(0xFF2A2A3D)
val RetroTextOffWhite = Color(0xFFF0F0F0)
val AppBarBackground = Color(0xFF1A1A2E)
val AppBarTitleColor = Color(0xFFDCDCDC)
val VaporwavePink = Color(0xFFF72585)
val RetroGold = Color(0xFFFFD700)
val RetroBorderColor = Color(0xFF808080)
val VaporwaveBlue = Color(0xFF00FFFF)
val VaporwaveCyan = Color(0xFF00B7FF)
val RetroAccentBlue = Color(0xFF4A90E2)
val VaporwavePurple = Color(0xFF9400D3)
val SynthwavePurple = Color(0xFF8A2BE2)
val RetroDarkPurple = Color(0xFF301934)
val RetroAccentPurple = VaporwavePurple
val SynthwaveOrange = Color(0xFFFF8C00)
val VaporwaveGreen = Color(0xFF00FF7F)
val VaporwaveTeal = Color(0xFF00DAC6)
val RetroBackground = AppBarBackground
val RetroBackgroundAlt = Color(0xFF2C2C4D)
val RetroTextPrimary = RetroTextOffWhite
val RetroTextSecondary = Color(0xFFB0B0B0)

// --- Scrapbook / Comic Style Palette ---
val ScrapbookCream = Color(0xFFFAF3E0)
val ScrapbookPaper = Color(0xFFF5E6C8)
val ScrapbookDark = Color(0xFF1A1A1A)
val ScrapbookYellow = Color(0xFFFFD60A)
val ScrapbookYellowDark = Color(0xFFE6B800)
val ScrapbookOrange = Color(0xFFFF6B35)
val ScrapbookRed = Color(0xFFE63946)
val ScrapbookGreen = Color(0xFF2DC653)
val ScrapbookBlue = Color(0xFF0077B6)
val ScrapbookPurple = Color(0xFF7B2FBE)
val ScrapbookBorder = Color(0xFF1A1A1A)
val ScrapbookCardWhite = Color(0xFFFFFFFF)
val ScrapbookShadow = Color(0xFF1A1A1A)
val ScrapbookTextDark = Color(0xFF1A1A1A)
val ScrapbookTextMuted = Color(0xFF666666)

// --- Gradient Sets ---
val ArticleGradientSet1 = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.75f))
val ArticleGradientSet2 = listOf(VaporwavePurple, VaporwaveCyan.copy(alpha = 0.75f))
val ArticleGradientSet3 = listOf(SynthwaveOrange, VaporwavePink.copy(alpha = 0.70f))
val ArticleGradientSet4 = listOf(VaporwaveBlue, SynthwavePurple.copy(alpha = 0.70f))
val ArticleGradientSet5 = listOf(VaporwaveCyan, VaporwaveGreen.copy(alpha = 0.75f))
val ArticleGradientSet6 = listOf(SynthwavePurple, SynthwaveOrange.copy(alpha = 0.65f))
val articleGradientColorsList = listOf(
    ArticleGradientSet1, ArticleGradientSet2, ArticleGradientSet3,
    ArticleGradientSet4, ArticleGradientSet5, ArticleGradientSet6
)
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
val VaporwavePink = Color(0xFF52B788)   // remapped to green theme
val RetroGold = Color(0xFF95D5B2)       // remapped to green theme
val RetroBorderColor = Color(0xFF808080)
val VaporwaveBlue = Color(0xFF74C69D)
val VaporwaveCyan = Color(0xFF40916C)
val RetroAccentBlue = Color(0xFF40916C)
val VaporwavePurple = Color(0xFF2D6A4F)
val SynthwavePurple = Color(0xFF1B4332)
val RetroDarkPurple = Color(0xFF301934)
val RetroAccentPurple = VaporwavePurple
val SynthwaveOrange = Color(0xFF95D5B2)
val VaporwaveGreen = Color(0xFF52B788)
val VaporwaveTeal = Color(0xFF74C69D)
val RetroBackground = AppBarBackground
val RetroBackgroundAlt = Color(0xFF2C2C4D)
val RetroTextPrimary = RetroTextOffWhite
val RetroTextSecondary = Color(0xFFB0B0B0)

// --- Scrapbook / Comic Style Palette ---
val ScrapbookCream = Color(0xFFFAF3E0)
val ScrapbookPaper = Color(0xFFF5E6C8)
val ScrapbookDark = Color(0xFF1A1A1A)
val ScrapbookYellow = Color(0xFFB7E4C7)
val ScrapbookYellowDark = Color(0xFF74C69D)

val ScrapbookOrange = Color(0xFF40916C)
val ScrapbookRed = Color(0xFFE63946)
val ScrapbookGreen = Color(0xFF52B788)
val ScrapbookBlue = Color(0xFF2D6A4F)
val ScrapbookPurple = Color(0xFF1B4332)
val ScrapbookBorder = Color(0xFF1A1A1A)
val ScrapbookCardWhite = Color(0xFFFFFFFF)
val ScrapbookShadow = Color(0xFF1A1A1A)
val ScrapbookTextDark = Color(0xFF1A1A1A)
val ScrapbookTextMuted = Color(0xFF666666)

// --- Comic Glass Palette (swappable accent system) ---
val ComicGlassBg    = Color(0xFFEFF7F2)   // light green-tinted background
val ComicGlassBgAlt = Color(0xFFE5F2EA)   // slightly deeper bg

// Default: Soft Green
val CGreen     = Color(0xFF52B788)
val CGreenMint = Color(0xFF74C69D)
val CGreenDeep = Color(0xFF40916C)

// ── Accent tokens — remapped so GREEN + WHITE stay dominant across the app.
// (Names kept so existing code compiles; red is the only non-green accent, for alerts/LIVE.)
val CAcYellow  = Color(0xFF74C69D)   // was amber  → mint
val CAcYellowD = Color(0xFF2D6A4F)   // was dark amber → forest
val CAcYellowL = Color(0xFFB7E4C7)   // was light amber → pale mint

// Swap option: Red
val CAcRed     = Color(0xFFE63946)
val CAcRedD    = Color(0xFFAA2020)

// Swap option: Blue
val CAcBlue    = Color(0xFF2D6A4F)   // was blue → forest green
val CAcBlueD   = Color(0xFF1B4332)

// Swap option: Purple
val CAcPurple  = Color(0xFF1B4332)   // was purple → deep green
val CAcPurpleD = Color(0xFF081C15)

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
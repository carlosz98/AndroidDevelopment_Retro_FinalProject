package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class MagazineCover(
    val id: String,
    val title: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val webUrl: String? = null,
    val platform: String = "",
    val era: String = ""
)

data class ReadingProgress(
    val magazineId: String = "",
    val magazineTitle: String = "",
    val magazineCoverUrl: String = "",
    val lastReadAt: Long = 0L,
    val percentComplete: Float = 0f
)

data class MagazineShelf(
    val id: String = "",
    val name: String = "",
    val emoji: String = "📚",
    val magazineIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)

data class MagazineRating(
    val magazineId: String = "",
    val userId: String = "",
    val stars: Int = 0,
    val review: String = "",
    val username: String = "",
    val timestamp: Long = 0L
)

data class ReadingChallenge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val type: String,
    val target: Int,
    val current: Int = 0,
    val badgeId: String = "",
    val isComplete: Boolean = false
)

data class MagazineComment(
    val id: String = "",
    val magazineId: String = "",
    val userId: String = "",
    val username: String = "",
    val profilePicUrl: String = "",
    val text: String = "",
    val page: Int? = null,
    val timestamp: Long = 0L
)

// ✅ Vintage Ad data class
data class VintageAd(
    val id: String,
    val headline: String,
    val tagline: String,
    val productName: String,
    val era: String,
    val platform: String,
    val emoji: String,
    val bgColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val details: List<String>
)

// ─── Sample Data ──────────────────────────────────────────────────────────────

val uniqueCoverResourceIds = listOf(
    R.drawable.cover1, R.drawable.cover2, R.drawable.cover3,
    R.drawable.cover4, R.drawable.cover5, R.drawable.cover6,
    R.drawable.cover7, R.drawable.cover8, R.drawable.cover9
)

val sampleMagazineCovers = listOf(
    MagazineCover("1", "GamePro #130", coverImageResId = R.drawable.cover1, platform = "PS1", era = "PS1"),
    MagazineCover("2", "EGM #89", coverImageResId = R.drawable.cover2, platform = "SNES", era = "SNES"),
    MagazineCover("3", "Nintendo Power #55", coverImageResId = R.drawable.cover3, platform = "SNES", era = "SNES"),
    MagazineCover("4", "GameFan Vol 3", coverImageResId = R.drawable.cover4, platform = "SEGA", era = "SEGA"),
    MagazineCover("5", "Retro Gamer #12", coverImageResId = R.drawable.cover5, platform = "NES", era = "NES"),
    MagazineCover("6", "Computer Gaming World", coverImageResId = R.drawable.cover6, platform = "PC", era = "PC"),
    MagazineCover("7", "GameFan Vol 7", coverImageResId = R.drawable.cover7, platform = "PS1", era = "PS1"),
    MagazineCover("8", "EGM Special Edition", coverImageResId = R.drawable.cover8, platform = "N64", era = "N64"),
    MagazineCover("9", "Nintendo Power #77", coverImageResId = R.drawable.cover9, platform = "SNES", era = "SNES"),
    MagazineCover("10", "GamePro #200", coverImageUrl = null, platform = "PS2", era = "PS2"),
    MagazineCover("11", "Electronic Gaming Monthly #110", coverImageUrl = null, platform = "PS1", era = "PS1"),
    MagazineCover("12", "Nintendo Power #100", coverImageUrl = null, platform = "SNES", era = "SNES"),
    MagazineCover("13", "Retro Gamer #42", coverImageUrl = null, platform = "ARCADE", era = "ARCADE"),
    MagazineCover("14", "GameFan Vol 9", coverImageUrl = null, platform = "SEGA", era = "SEGA"),
    MagazineCover("15", "PC Gamer #48", coverImageUrl = null, platform = "PC", era = "PC"),
    MagazineCover("16", "GamePro Special — Holiday '97", coverImageUrl = null, platform = "N64", era = "N64"),
    MagazineCover("17", "EGM² — Collector's Edition", coverImageUrl = null, platform = "GBA", era = "GBA"),
    MagazineCover("18", "Tips & Tricks #66", coverImageUrl = null, platform = "PS1", era = "PS1"),
    MagazineCover("19", "Nintendo Power #55 — Zelda Special", coverImageUrl = null, platform = "SNES", era = "SNES"),
    MagazineCover("20", "Official PlayStation Magazine #22", coverImageUrl = null, platform = "PS1", era = "PS1"),
    MagazineCover("21", "Computer & Video Games #188", coverImageUrl = null, platform = "SEGA", era = "SEGA"),
    MagazineCover("22", "GameInformer #58", coverImageUrl = null, platform = "PS2", era = "PS2"),
    MagazineCover("23", "Retro Gamer #88", coverImageUrl = null, platform = "NES", era = "NES"),
    MagazineCover("24", "Game Player's Vol 6", coverImageUrl = null, platform = "SNES", era = "SNES"),
    MagazineCover("25", "Nintendo Power #120 — Pokémon Gold", coverImageUrl = null, platform = "GBC", era = "GB"),
    MagazineCover("26", "GamePro #168 — Holiday '02", coverImageUrl = null, platform = "PS2", era = "PS2"),
    MagazineCover("27", "Retro Gamer #60 — SEGA Special", coverImageUrl = null, platform = "SEGA", era = "SEGA"),
    MagazineCover("28", "EGM #150 — Halo Issue", coverImageUrl = null, platform = "XBOX", era = "XBOX"),
    MagazineCover("29", "Nintendo Power #145 — Metroid Prime", coverImageUrl = null, platform = "GCN", era = "GCN"),
    MagazineCover("30", "GameFan Vol 12 — RPG Mega Issue", coverImageUrl = null, platform = "PS1", era = "PS1"),
    MagazineCover("31", "PC Gamer #72 — Diablo II Deep Dive", coverImageUrl = null, platform = "PC", era = "PC"),
    MagazineCover("32", "GameInformer #80 — GTA Vice City", coverImageUrl = null, platform = "PS2", era = "PS2"),
    MagazineCover("33", "Official Nintendo Magazine #44", coverImageUrl = null, platform = "GBA", era = "GBA"),
    MagazineCover("34", "Retro Gamer #110 — Rare Retrospective", coverImageUrl = null, platform = "N64", era = "N64"),
    MagazineCover("35", "GamePro #95 — Sonic & Knuckles Cover", coverImageUrl = null, platform = "SEGA", era = "SEGA"),
    MagazineCover("36", "Electronic Games Vol 9 — Arcade Issue", coverImageUrl = null, platform = "ARCADE", era = "ARCADE"),
    MagazineCover("37", "Nintendo Power #30 — SNES Launch", coverImageUrl = null, platform = "SNES", era = "SNES"),
    MagazineCover("38", "Famitsu #400 — Legendary Issue", coverImageUrl = null, platform = "ALL", era = "NES"),
    MagazineCover("39", "GameFan Vol 15 — Chrono Trigger Cover", coverImageUrl = null, platform = "SNES", era = "SNES"),
    MagazineCover("40", "VideoGames & Computer Entertainment #22", coverImageUrl = null, platform = "NES", era = "NES"),
)

val allChallenges = listOf(
    ReadingChallenge("c1", "Monthly Reader", "Read 3 magazines this month", "📅", "MONTHLY", 3),
    ReadingChallenge("c2", "Binge Reader", "Read 5 magazines this month", "🔥", "MONTHLY", 5),
    ReadingChallenge("c3", "SNES Scholar", "Read 3 SNES-era magazines", "🎮", "THEME", 3),
    ReadingChallenge("c4", "PS1 Pioneer", "Read 3 PS1-era magazines", "🎯", "THEME", 3),
    ReadingChallenge("c5", "NES Historian", "Read all NES-era magazines", "🕹️", "THEME", 2),
    ReadingChallenge("c6", "Collector", "Add 5 magazines to a shelf", "🗂️", "THEME", 5),
    ReadingChallenge("c7", "Critic", "Rate 3 different magazines", "⭐", "THEME", 3),
    ReadingChallenge("c8", "Completionist", "Read 10 magazines total", "🏆", "MONTHLY", 10),
)

// ✅ Vintage ads — styled retro ad cards
val vintageAds = listOf(
    VintageAd(
        id = "ad1", headline = "BLAST PROCESSING", tagline = "Your brain can't handle this speed.",
        productName = "MEGA DRIVE ULTRA", era = "1992", platform = "SEGA", emoji = "💥",
        bgColor = ComicGlassBg, accentColor = CAcBlue, textColor = ScrapbookDark,
        details = listOf("68000 CPU @ 7.6MHz", "Blast Processing™", "16-BIT POWER", "AVAILABLE NOW")
    ),
    VintageAd(
        id = "ad2", headline = "NOW IN 16-BIT!", tagline = "The future of gaming is here.",
        productName = "SUPER SYSTEM PRO", era = "1991", platform = "SNES", emoji = "🌟",
        bgColor = ComicGlassBg, accentColor = CAcPurple, textColor = ScrapbookDark,
        details = listOf("Mode 7 Graphics", "8 Channels of Sound", "16-BIT GLORY", "CHRISTMAS 1991")
    ),
    VintageAd(
        id = "ad3", headline = "PORTABLE POWER", tagline = "Take the adventure anywhere.",
        productName = "POCKET WARRIOR", era = "1994", platform = "GBA", emoji = "🎮",
        bgColor = ComicGlassBg, accentColor = CAcYellow, textColor = ScrapbookDark,
        details = listOf("8-Hour Battery Life", "Game Link Cable", "33 LAUNCH TITLES", "ONLY \$89.99")
    ),
    VintageAd(
        id = "ad4", headline = "THE DISC IS HERE", tagline = "650MB of pure gaming bliss.",
        productName = "NEXT GEN STATION", era = "1995", platform = "PS1", emoji = "💿",
        bgColor = ComicGlassBg, accentColor = CAcBlue, textColor = ScrapbookDark,
        details = listOf("CD-ROM Drive", "Polygon Graphics", "3D GAMING", "LAUNCH SPRING '95")
    ),
    VintageAd(
        id = "ad5", headline = "64-BIT REALITY", tagline = "So real you'll forget it's a game.",
        productName = "ULTRA 64 SYSTEM", era = "1996", platform = "N64", emoji = "🌐",
        bgColor = ComicGlassBg, accentColor = CGreenDeep, textColor = CGreenDeep,
        details = listOf("64-Bit Architecture", "Rumble Pack Ready", "4-PLAYER ACTION", "HOLIDAY 1996")
    ),
    VintageAd(
        id = "ad6", headline = "PC MASTER RACE", tagline = "Real gamers use keyboards.",
        productName = "TURBO PC 486DX", era = "1993", platform = "PC", emoji = "💾",
        bgColor = ComicGlassBg, accentColor = CAcYellow, textColor = ScrapbookDark,
        details = listOf("486DX @ 66MHz", "4MB RAM", "256-COLOR VGA", "MS-DOS COMPATIBLE")
    ),
    VintageAd(
        id = "ad7", headline = "8-BIT LEGEND", tagline = "The original. The best. End of story.",
        productName = "CLASSIC CONSOLE NES", era = "1986", platform = "NES", emoji = "🕹️",
        bgColor = ComicGlassBg, accentColor = CAcRed, textColor = ScrapbookDark,
        details = listOf("Millions Sold Worldwide", "300+ Game Library", "ROB THE ROBOT", "ONLY \$99.99")
    ),
    VintageAd(
        id = "ad8", headline = "HANDHELD REVOLUTION", tagline = "Play anywhere. Anytime. Anyone.",
        productName = "GAME BRICK COLOR", era = "1998", platform = "GBC", emoji = "🟢",
        bgColor = ComicGlassBg, accentColor = CGreenDeep, textColor = CGreenDeep,
        details = listOf("Color Screen!", "Game Boy Compatible", "50+ HOURS BATTERY", "POKEMON READY")
    ),
    VintageAd(
        id = "ad9", headline = "EMOTION ENGINE", tagline = "128-bit power. Feel everything.",
        productName = "PLAYSTATION 2", era = "2000", platform = "PS2", emoji = "🎬",
        bgColor = ComicGlassBg, accentColor = CAcBlue, textColor = ScrapbookDark,
        details = listOf("128-Bit Graphics", "DVD Playback Built-In", "BACKWARD COMPATIBLE", "LAUNCH OCT 2000")
    ),
    VintageAd(
        id = "ad10", headline = "CUBE UP", tagline = "Small box. Massive games.",
        productName = "GAMECUBE", era = "2001", platform = "GAMECUBE", emoji = "🟪",
        bgColor = Color(0xFFD3CFE0), accentColor = Color(0xFF5B3FA0), textColor = ScrapbookDark,
        details = listOf("Mini-Disc Format", "4-Player Built-In", "NINTENDO EXCLUSIVE", "HOLIDAY 2001")
    ),
    VintageAd(
        id = "ad11", headline = "LIVE YOUR GAMES", tagline = "Connect. Compete. Conquer.",
        productName = "XBOX", era = "2001", platform = "XBOX", emoji = "❎",
        bgColor = Color(0xFFCDD4C8), accentColor = Color(0xFF1F7A1F), textColor = CGreenDeep,
        details = listOf("Built-In Hard Drive", "Xbox Live Ready", "BROADBAND ONLY", "FALL 2001")
    ),
    VintageAd(
        id = "ad12", headline = "PLAY DIFFERENT", tagline = "Wave it. Swing it. Live it.",
        productName = "WII", era = "2006", platform = "WII", emoji = "🎾",
        bgColor = Color(0xFFE0DCD0), accentColor = Color(0xFF707070), textColor = Color(0xFF2B2B2B),
        details = listOf("Motion Controller", "Wii Sports Included", "FAMILY GAMING", "HOLIDAY 2006")
    ),
    VintageAd(
        id = "ad13", headline = "CELL PROCESSOR", tagline = "The dream of a generation.",
        productName = "PLAYSTATION 3", era = "2006", platform = "PS3", emoji = "🔷",
        bgColor = Color(0xFFC5CAD4), accentColor = Color(0xFF1A3A8F), textColor = ScrapbookDark,
        details = listOf("Blu-Ray Player Built-In", "Cell Processor", "ONLINE GAMING FREE", "NOVEMBER 2006")
    ),
    VintageAd(
        id = "ad14", headline = "DREAM ON", tagline = "It's thinking. Are you?",
        productName = "DREAMCAST", era = "1999", platform = "DREAMCAST", emoji = "🌀",
        bgColor = Color(0xFFD8DEDA), accentColor = CGreen, textColor = Color(0xFF1A2B2A),
        details = listOf("Built-In Modem", "GD-ROM Format", "VMU Memory Cards", "SEPTEMBER 9, 1999")
    ),
    VintageAd(
        id = "ad15", headline = "GAMING ON THE GO", tagline = "PlayStation. In your pocket.",
        productName = "PSP", era = "2004", platform = "PSP", emoji = "📀",
        bgColor = Color(0xFFCED0D8), accentColor = Color(0xFF2A2A40), textColor = ScrapbookDark,
        details = listOf("UMD Movie Playback", "Wide Screen Display", "WI-FI BUILT IN", "MARCH 2005")
    ),
    VintageAd(
        id = "ad16", headline = "TWO SCREENS. ONE WORLD.", tagline = "Touch the future.",
        productName = "NINTENDO DS", era = "2004", platform = "DS", emoji = "✏️",
        bgColor = Color(0xFFDDD6C5), accentColor = CAcYellow, textColor = ScrapbookDark,
        details = listOf("Dual Screen Design", "Touch Stylus Included", "WIRELESS MULTIPLAYER", "NOVEMBER 2004")
    )
)

fun ArchiveItem.toMagazineCover() = MagazineCover(
    id = this.id, title = this.title,
    coverImageResId = null, coverImageUrl = this.thumbnailUrl, webUrl = this.webUrl
)

// ─── Stable per-item randomness helper ─────────────────────────────────────────

fun stableRandomFor(seed: String, range: ClosedFloatingPointRange<Float>): Float {
    val hash = seed.hashCode()
    val normalized = (abs(hash) % 1000) / 1000f
    return range.start + normalized * (range.endInclusive - range.start)
}

fun toArchiveEmbedUrl(webUrl: String): String = webUrl

fun injectHideStyles(view: WebView?) {
    view?.evaluateJavascript("""
        (function() {
            var style = document.createElement('style');
            style.innerHTML = '#oc-hdr, #nav-tophat, .topinblock, header { display: none !important; } body { margin-top: 0 !important; padding-top: 0 !important; }';
            document.head.appendChild(style);
            try { var br = document.querySelector('#bookreader, .BookReader, #BookReader'); if (br) { br.scrollIntoView({ behavior: 'smooth' }); } } catch(e) {}
        })();
    """.trimIndent(), null)
}

// ─── Magazine of the Day ──────────────────────────────────────────────────────

fun getMagazineOfTheDay(magazines: List<MagazineCover>): MagazineCover? {
    if (magazines.isEmpty()) return null
    val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
    return magazines[dayOfYear % magazines.size]
}

// ─── Era → Launch Year Mapping (for anniversary banner) ───────────────────────

fun eraLaunchYear(era: String): Int? = when (era.uppercase()) {
    "NES" -> 1985
    "SNES" -> 1991
    "SEGA" -> 1989
    "PS1" -> 1995
    "N64" -> 1996
    "GBA" -> 2001
    "PC" -> 1981
    "GBC" -> 1998
    "PS2" -> 2000
    "GAMECUBE" -> 2001
    "XBOX" -> 2001
    "WII" -> 2006
    "PS3" -> 2006
    "DREAMCAST" -> 1999
    "PSP" -> 2004
    "DS" -> 2004
    else -> null
}

data class AnniversaryHighlight(
    val title: String,
    val subtitle: String,
    val yearsAgo: Int,
    val era: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null
)

fun getAnniversaryHighlights(magazines: List<MagazineCover>): List<AnniversaryHighlight> {
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    return magazines.mapNotNull { mag ->
        val launchYear = eraLaunchYear(mag.era) ?: return@mapNotNull null
        val yearsAgo = currentYear - launchYear
        if (yearsAgo <= 0) return@mapNotNull null
        AnniversaryHighlight(
            title = mag.title,
            subtitle = "${mag.era} launched in $launchYear",
            yearsAgo = yearsAgo,
            era = mag.era,
            coverImageResId = mag.coverImageResId,
            coverImageUrl = mag.coverImageUrl
        )
    }.distinctBy { it.era }.sortedByDescending { it.yearsAgo }
}

// ─── Shimmer Helper ───────────────────────────────────────────────────────────

@Composable
fun ShimmerMagazineShelf() {
    val shimmerT = rememberInfiniteTransition(label = "shelfShimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shelfShimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.46f)),
        start = androidx.compose.ui.geometry.Offset(shimmerX - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier.weight(1f).aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// ─── Magazine of the Day Card ─────────────────────────────────────────────────

@Composable
fun MagazineOfTheDayCard(magazine: MagazineCover, onRead: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "motdNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val crownScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.12f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "motdCrownScale")
    val btnScale by rememberGlowRange(1f, 1.04f)

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(14.dp).background(Color(0xFFFFD700).copy(alpha = neonAlpha * 0.25f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp)),
            backgroundColor = ComicGlassBg, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Column {
                // Header bar
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color(0xFFFFD700).copy(alpha = 0.05f), Color(0xFFFFD700).copy(alpha = 0.2f)))).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.scale(crownScale)) { Text("👑", fontSize = 22.sp) }
                        Column {
                            Text("MAGAZINE OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                            Text("Today's retro press pick", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (magazine.era.isNotBlank()) {
                            HoloBadge(
                                label = magazine.era,
                                cornerRadius = 6.dp,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                // Content
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.width(90.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f))
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp))
                    ) {
                        when {
                            magazine.coverImageResId != null -> Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            magazine.coverImageUrl != null -> AsyncImage(model = magazine.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(magazine.coverImageUrl).fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📰", fontSize = 28.sp) }
                        }
                        ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.06f)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(magazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                        if (magazine.platform.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text(magazine.platform, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        RetroGlassButton(
                            text = "READ NOW",
                            onClick = onRead,
                            icon = { Icon(Icons.Filled.MenuBook, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Anniversary Banner ────────────────────────────────────────────────────────

@Composable
fun AnniversaryBanner(highlights: List<AnniversaryHighlight>) {
    if (highlights.isEmpty()) return
    val monthName = remember { java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date()) }

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎂", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("THIS MONTH IN GAMING HISTORY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
        }
        Text("Celebrating $monthName console anniversaries", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(highlights, key = { it.era }) { highlight -> AnniversaryCard(highlight = highlight, neonAlpha = neonAlpha) }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

@Composable
fun AnniversaryCard(highlight: AnniversaryHighlight, neonAlpha: Float) {
    val eraColor = eraAccentColor(highlight.era)

    Box(modifier = Modifier.width(180.dp).height(120.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(10.dp).background(eraColor.copy(alpha = neonAlpha * 0.25f), RoundedCornerShape(12.dp)))
        Box(
            modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp))
        ) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Row(modifier = Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f))
                        .border(1.5.dp, eraColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    // Green stripe
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                    when {
                        highlight.coverImageResId != null -> Image(painter = painterResource(id = highlight.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        highlight.coverImageUrl != null -> AsyncImage(model = highlight.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(highlight.coverImageUrl).fillMaxSize())
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📰", fontSize = 22.sp) }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(eraColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("${highlight.yearsAgo} YEARS AGO", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 0.3.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(highlight.era, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Text(highlight.subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, maxLines = 2, lineHeight = 11.sp)
                }
            }
        }
    }
}

// ─── Era Timeline (magazines version) ────────────────────────────────────────

@Composable
fun MagazineEraTimeline(selectedEra: String, onEraSelected: (String) -> Unit) {
    val eras = listOf("NES", "SNES", "SEGA", "PS1", "N64", "GBA", "PC")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Text("ERA FILTER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
            if (selectedEra != "ALL") {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraAccentColor(selectedEra).copy(alpha = 0.15f)).border(1.dp, eraAccentColor(selectedEra).copy(alpha = 0.5f), RoundedCornerShape(6.dp)).clickable { onEraSelected("ALL") }.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(selectedEra, fontFamily = BangersFontFamily, color = eraAccentColor(selectedEra), fontSize = 11.sp)
                        Icon(Icons.Filled.Close, contentDescription = null, tint = eraAccentColor(selectedEra), modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, CGreen.copy(alpha = 0.3f), CGreen.copy(alpha = 0.3f), Color.Transparent))).align(Alignment.Center))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                itemsIndexed(eras) { jumpIndex, era ->
                    Box(modifier = Modifier.jumpIn(jumpIndex)) {
                    val isSelected = selectedEra == era
                    val eraColor = eraAccentColor(era)
                    var pressed by remember { mutableStateOf(false) }
                    val itemScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magEraScale")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(68.dp).scale(itemScale).clickable { pressed = true; onEraSelected(if (selectedEra == era) "ALL" else era) }.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(if (isSelected) 44.dp else 36.dp).clip(CircleShape)
                                .background(if (isSelected) eraColor else Color.White.copy(alpha = 0.46f))
                                .border(width = if (isSelected) 2.dp else 1.dp, brush = if (isSelected) Brush.linearGradient(colors = listOf(eraColor.copy(alpha = neonAlpha), eraColor.copy(alpha = 0.4f), eraColor.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(era, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else ScrapbookDark, fontSize = if (isSelected) 8.sp else 7.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) eraColor else ScrapbookBorder.copy(alpha = 0.4f)))
                    }
                    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                                    }
                }
            }
        }
    }
}

// ─── Shelf Detail — Fanned Stack View ──────────────────────────────────────────

@Composable
fun ShelfDetailScreen(
    shelf: MagazineShelf,
    allMagazines: List<MagazineCover>,
    onClose: () -> Unit,
    onRead: (MagazineCover) -> Unit,
    onRemoveFromShelf: (MagazineCover) -> Unit = {}
) {
    val shelfMagazines = remember(shelf.magazineIds, allMagazines) {
        shelf.magazineIds.mapNotNull { id -> allMagazines.firstOrNull { it.id == id } }
    }
    var bringToFrontId by remember { mutableStateOf<String?>(null) }
    val orderedMagazines = remember(shelfMagazines, bringToFrontId) {
        if (bringToFrontId == null) shelfMagazines
        else shelfMagazines.sortedBy { if (it.id == bringToFrontId) 1 else 0 }
    }

    val glowT = rememberInfiniteTransition(label = "shelfDetailGlow")
    val glowAlpha by glowT.animateFloat(initialValue = 0.15f, targetValue = 0.35f, animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOut), RepeatMode.Reverse), label = "shelfDetailGlowAlpha")

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(ComicGlassBg, ComicGlassBgAlt, ComicGlassBg)))
    ) {
        // Cork/felt texture suggestion via subtle dot noise
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 18.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    drawCircle(color = Color.Black.copy(alpha = 0.06f), radius = 1.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ScrapbookDark) }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(shelf.emoji, fontSize = 20.sp)
                            Text(shelf.name.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 0.5.sp)
                        }
                        Text("Tap a magazine to bring it forward", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("${shelfMagazines.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                    }
                }
            }

            if (shelfMagazines.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📭", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("This shelf is empty", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                        Text("Add magazines from the archive to build your stack", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp))
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Spotlight glow behind the stack
                    Canvas(modifier = Modifier.size(260.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CGreen.copy(alpha = glowAlpha * 0.6f), Color.Transparent),
                                center = center,
                                radius = size.minDimension / 2f
                            ),
                            radius = size.minDimension / 2f
                        )
                    }

                    Box(contentAlignment = Alignment.Center) {
                        orderedMagazines.forEachIndexed { index, magazine ->
                            FannedStackItem(
                                magazine = magazine,
                                stackIndex = index,
                                totalCount = orderedMagazines.size,
                                isFront = index == orderedMagazines.size - 1,
                                onTap = {
                                    if (index == orderedMagazines.size - 1) onRead(magazine)
                                    else bringToFrontId = magazine.id
                                }
                            )
                        }
                    }
                }

                // Front magazine info + actions
                val frontMagazine = orderedMagazines.lastOrNull()
                if (frontMagazine != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, ComicGlassBg.copy(alpha = 0.98f))))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(frontMagazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (frontMagazine.platform.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(frontMagazine.platform, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .clickable { onRead(frontMagazine) }.padding(horizontal = 20.dp, vertical = 11.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                                    Text("READ NOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                                }
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                    .clickable { onRemoveFromShelf(frontMagazine) }.padding(horizontal = 16.dp, vertical = 11.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove from shelf", tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FannedStackItem(
    magazine: MagazineCover,
    stackIndex: Int,
    totalCount: Int,
    isFront: Boolean,
    onTap: () -> Unit
) {
    // Each card fans out from center based on its position in the stack
    val centerIndex = (totalCount - 1) / 2f
    val fanAngle = remember(magazine.id, totalCount) {
        val baseAngle = (stackIndex - centerIndex) * 9f
        baseAngle.coerceIn(-32f, 32f)
    }
    val fanOffsetX = remember(magazine.id, totalCount) {
        ((stackIndex - centerIndex) * 22f).coerceIn(-90f, 90f)
    }

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 1.06f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fanPressScale")
    val frontLift by animateFloatAsState(targetValue = if (isFront) -10f else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "fanFrontLift")
    Box(
        modifier = Modifier
            .offset(x = fanOffsetX.dp, y = frontLift.dp)
            .rotate(fanAngle)
            .scale(pressScale)
            .zIndex(stackIndex.toFloat())
            .size(width = 150.dp, height = 200.dp)
    ) {
        // Drop shadow beneath each card for physical depth
        Box(
            modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 5.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier.matchParentSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(width = if (isFront) 2.5.dp else 1.dp, color = if (isFront) CGreen else ScrapbookBorder.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                .clickable { pressed = true; onTap() }
        ) {
            if (magazine.coverImageResId != null) {
                Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (magazine.coverImageUrl != null) {
                AsyncImage(model = magazine.coverImageUrl, contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(magazine.coverImageUrl).fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    Text(magazine.title, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
                }
            }
            // Non-front cards get a soft dark veil so the front one pops
            if (!isFront) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

// ─── Cover Art Gallery Mode ───────────────────────────────────────────────────

@Composable
fun CoverArtGallery(
    magazines: List<MagazineCover>,
    onClose: () -> Unit,
    onRead: (MagazineCover) -> Unit,
    onBookmarkToggle: (MagazineCover) -> Unit = {},
    favoriteIds: Set<String> = emptySet()
) {
    var currentIndex by remember { mutableStateOf(0) }
    var dragOffset by remember { mutableStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val currentMagazine = magazines.getOrNull(currentIndex) ?: return

    // ✅ Card tilt from drag
    val tiltAngle = (dragOffset / 20f).coerceIn(-12f, 12f)
    val cardScale = 1f - (abs(dragOffset) / 4000f).coerceIn(0f, 0.08f)

    fun goNext() {
        if (currentIndex < magazines.size - 1 && !isAnimating) {
            isAnimating = true
            currentIndex++
            dragOffset = 0f
            isAnimating = false
        }
    }

    fun goPrev() {
        if (currentIndex > 0 && !isAnimating) {
            isAnimating = true
            currentIndex--
            dragOffset = 0f
            isAnimating = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(ComicGlassBg, ComicGlassBgAlt, ComicGlassBg)))
    ) {
        // ✅ Starfield background
        val starAlpha by rememberGlowRange(0.3f, 0.8f)
        Box(modifier = Modifier.fillMaxSize()) {
            repeat(20) { i ->
                val x = (i * 47 % 100).toFloat()
                val y = (i * 31 % 100).toFloat()
                val size = if (i % 3 == 0) 3.dp else 2.dp
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                    Box(modifier = Modifier.padding(start = (x * 3.5).dp, top = (y * 6).dp).size(size).clip(CircleShape).background(CGreen.copy(alpha = starAlpha * (0.3f + i % 3 * 0.2f))))
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ✅ Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ScrapbookDark) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GALLERY MODE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 1.sp)
                        Text("Swipe or tap arrows to browse", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    // Counter
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("${currentIndex + 1} / ${magazines.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                    }
                }
            }

            // ✅ Main card area
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    dragOffset < -80f -> goNext()
                                    dragOffset > 80f -> goPrev()
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, delta -> dragOffset += delta }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // ✅ Ghost cards behind — prev and next
                if (currentIndex > 0) {
                    val prevMag = magazines[currentIndex - 1]
                    Box(
                        modifier = Modifier.offset(x = (-180).dp).size(width = 160.dp, height = 210.dp)
                            .clip(RoundedCornerShape(12.dp)).alpha(0.35f)
                    ) {
                        when {
                            prevMag.coverImageResId != null -> Image(painter = painterResource(id = prevMag.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            prevMag.coverImageUrl != null -> AsyncImage(model = prevMag.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(prevMag.coverImageUrl).fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) { Text("📰", fontSize = 32.sp) }
                        }
                    }
                }
                if (currentIndex < magazines.size - 1) {
                    val nextMag = magazines[currentIndex + 1]
                    Box(
                        modifier = Modifier.offset(x = 180.dp).size(width = 160.dp, height = 210.dp)
                            .clip(RoundedCornerShape(12.dp)).alpha(0.35f)
                    ) {
                        when {
                            nextMag.coverImageResId != null -> Image(painter = painterResource(id = nextMag.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            nextMag.coverImageUrl != null -> AsyncImage(model = nextMag.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(nextMag.coverImageUrl).fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) { Text("📰", fontSize = 32.sp) }
                        }
                    }
                }

                // ✅ Main card with tilt + glow
                Box(contentAlignment = Alignment.Center) {
                    // Neon glow behind card
                    Box(modifier = Modifier.size(width = 240.dp, height = 330.dp).blur(20.dp).background(CGreen.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)))
                    Box(
                        modifier = Modifier.size(width = 230.dp, height = 310.dp)
                            .graphicsLayer {
                                rotationY = tiltAngle
                                scaleX = cardScale
                                scaleY = cardScale
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                    ) {
                        when {
                            currentMagazine.coverImageResId != null -> Image(painter = painterResource(id = currentMagazine.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            currentMagazine.coverImageUrl != null -> AsyncImage(model = currentMagazine.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(currentMagazine.coverImageUrl).fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) { Text("📰", fontSize = 64.sp) }
                        }
                        // Subtle shine overlay
                        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent, Color.Transparent))))
                    }
                }
            }

            // ✅ Info + controls panel
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, ComicGlassBg.copy(alpha = 0.98f))))
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Magazine info
                    Text(currentMagazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 32.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (currentMagazine.platform.isNotBlank()) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.5.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text(currentMagazine.platform, fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                        if (currentMagazine.era.isNotBlank()) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraAccentColor(currentMagazine.era).copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text(currentMagazine.era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation arrows + action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev button
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(if (currentIndex > 0) Color.White.copy(alpha = 0.46f) else Color.Transparent)
                                .border(2.dp, if (currentIndex > 0) ScrapbookDark else Color.Transparent, CircleShape)
                                .clickable(enabled = currentIndex > 0) { goPrev() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = if (currentIndex > 0) ScrapbookDark else ScrapbookDark.copy(alpha = 0.2f), modifier = Modifier.size(28.dp))
                        }

                        // Center action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Bookmark
                            val isBookmarked = favoriteIds.contains(currentMagazine.id)
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(if (isBookmarked) CGreen else Color.White.copy(alpha = 0.46f))
                                    .border(2.dp, ScrapbookDark, CircleShape)
                                    .clickable { onBookmarkToggle(currentMagazine) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                            }

                            // Read button
                            Box(contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(66.dp).blur(12.dp).background(CGreen.copy(alpha = neonAlpha * 0.4f), CircleShape))
                                Box(
                                    modifier = Modifier.size(58.dp).clip(CircleShape)
                                        .background(CGreen)
                                        .border(3.dp, ScrapbookBorder, CircleShape)
                                        .clickable { onRead(currentMagazine) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = "Read", tint = ScrapbookDark, modifier = Modifier.size(28.dp))
                                }
                            }

                            // Share stub
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(2.dp, ScrapbookDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Next button
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(if (currentIndex < magazines.size - 1) Color.White.copy(alpha = 0.46f) else Color.Transparent)
                                .border(2.dp, if (currentIndex < magazines.size - 1) ScrapbookDark else Color.Transparent, CircleShape)
                                .clickable(enabled = currentIndex < magazines.size - 1) { goNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = if (currentIndex < magazines.size - 1) ScrapbookDark else ScrapbookDark.copy(alpha = 0.2f), modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dot indicators
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp), contentPadding = PaddingValues(horizontal = 32.dp)) {
                        items(minOf(magazines.size, 12)) { i ->
                            val isActive = i == currentIndex
                            Box(modifier = Modifier.size(if (isActive) 10.dp else 6.dp).clip(CircleShape).background(if (isActive) CGreen else ScrapbookDark.copy(alpha = 0.25f)))
                        }
                    }
                }
            }
        }
    }
}

// ─── Ad Break Section ─────────────────────────────────────────────────────────

@Composable
fun AdBreakSection() {
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    var expandedAd by remember { mutableStateOf<VintageAd?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("📺", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("AD BREAK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("FROM THE ARCHIVES", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        Text("Tap an ad to view it full screen", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vintageAds, key = { it.id }) { ad ->
                VintageAdCard(ad = ad, neonAlpha = neonAlpha, onClick = { expandedAd = ad })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
    }

    // ✅ Full screen ad overlay
    expandedAd?.let { ad ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { expandedAd = null },
            contentAlignment = Alignment.Center
        ) {
            FullScreenVintageAd(ad = ad, onClose = { expandedAd = null })
        }
    }
}

@Composable
fun VintageAdCard(ad: VintageAd, neonAlpha: Float, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "adCardScale")

    var photoUrl by remember(ad.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(ad.id) {
        val query = "${ad.platform.lowercase()} retro console ${ad.era}"
        val result = fetchHeroPhotoForPage(query)
        photoUrl = result?.imageUrl
    }

    Box(
        modifier = Modifier.width(170.dp).height(240.dp).scale(cardScale)
            .clip(RoundedCornerShape(3.dp))
            .background(ad.bgColor)
            .border(width = 3.dp, color = ad.accentColor, shape = RoundedCornerShape(3.dp))
            .clickable { pressed = true; onClick() }
    ) {
        // Halftone dot texture overlay — print look
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotSpacing = 7.dp.toPx()
            val dotRadius = 0.6.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    drawCircle(color = ad.accentColor.copy(alpha = 0.04f), radius = dotRadius, center = androidx.compose.ui.geometry.Offset(x, y))
                    x += dotSpacing
                }
                y += dotSpacing
            }
        }

        // Faded photo, desaturated feel via low opacity + sepia-ish overlay
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = ad.productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(photoUrl).fillMaxWidth().height(130.dp).align(Alignment.TopCenter),
                alpha = 0.85f
            )
            Box(modifier = Modifier.fillMaxWidth().height(130.dp).align(Alignment.TopCenter).background(ad.bgColor.copy(alpha = 0.18f)))
            Box(modifier = Modifier.fillMaxWidth().height(130.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, ad.bgColor), startY = 90f)))
        }

        Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Spacer(modifier = Modifier.height(if (photoUrl != null) 120.dp else 4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(ad.accentColor).padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text(ad.era, fontFamily = BangersFontFamily, color = ad.bgColor, fontSize = 9.sp, letterSpacing = 0.5.sp)
                }
                Text(ad.platform, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ad.accentColor.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(ad.headline, fontFamily = BangersFontFamily, color = ad.accentColor, fontSize = 17.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(ad.tagline, fontFamily = NunitoFontFamily, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = ad.textColor.copy(alpha = 0.75f), fontSize = 10.sp, lineHeight = 13.sp)

            Spacer(modifier = Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ad.accentColor.copy(alpha = 0.4f)))
            Spacer(modifier = Modifier.height(5.dp))
            Text(ad.productName, fontFamily = BangersFontFamily, color = ad.textColor, fontSize = 10.sp, letterSpacing = 1.sp)
        }

        // Corner price-burst badge
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).rotate(12f)
                .size(36.dp).clip(CircleShape).background(ad.accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text("NEW!", fontFamily = BangersFontFamily, color = ad.bgColor, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

@Composable
fun FullScreenVintageAd(ad: VintageAd, onClose: () -> Unit) {
    var photoUrl by remember(ad.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(ad.id) {
        val query = "${ad.platform.lowercase()} retro console ${ad.era}"
        val result = fetchHeroPhotoForPage(query)
        photoUrl = result?.imageUrl
    }

    Box(
        modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(0.68f)
            .clip(RoundedCornerShape(4.dp))
            .background(ad.bgColor)
            .border(width = 4.dp, color = ad.accentColor, shape = RoundedCornerShape(4.dp))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
    ) {
        // Halftone texture
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotSpacing = 8.dp.toPx()
            val dotRadius = 0.7.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    drawCircle(color = ad.accentColor.copy(alpha = 0.035f), radius = dotRadius, center = androidx.compose.ui.geometry.Offset(x, y))
                    x += dotSpacing
                }
                y += dotSpacing
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Photo banner — top third, like a magazine ad's hero shot
            Box(modifier = Modifier.fillMaxWidth().weight(0.42f)) {
                if (photoUrl != null) {
                    AsyncImage(model = photoUrl, contentDescription = ad.productName, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(photoUrl).fillMaxSize(), alpha = 0.9f)
                    Box(modifier = Modifier.fillMaxSize().background(ad.bgColor.copy(alpha = 0.12f)))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(ad.accentColor.copy(alpha = 0.15f)))
                }
                Box(modifier = Modifier.fillMaxWidth().height(50.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, ad.bgColor))))

                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.background(ad.accentColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(ad.era, fontFamily = BangersFontFamily, color = ad.bgColor, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.background(ad.accentColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(ad.platform, fontFamily = BangersFontFamily, color = ad.bgColor, fontSize = 12.sp)
                    }
                }
            }

            // Body
            Column(
                modifier = Modifier.fillMaxWidth().weight(0.58f).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(ad.headline, fontFamily = BangersFontFamily, color = ad.accentColor, fontSize = 28.sp, textAlign = TextAlign.Center, lineHeight = 30.sp, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(ad.tagline, fontFamily = NunitoFontFamily, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = FontWeight.Bold, color = ad.textColor.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ad.accentColor.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.height(10.dp))
                    ad.details.forEach { detail ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(5.dp).background(ad.accentColor))
                            Text(detail, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ad.textColor.copy(alpha = 0.85f), fontSize = 11.sp, letterSpacing = 0.5.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(ad.accentColor.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(ad.productName, fontFamily = BangersFontFamily, color = ad.accentColor, fontSize = 16.sp, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("ASK FOR IT AT YOUR LOCAL GAME STORE", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ad.textColor.copy(alpha = 0.4f), fontSize = 8.sp, letterSpacing = 1.sp)
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(30.dp).clip(CircleShape).background(ad.accentColor).clickable { onClose() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = ad.bgColor, modifier = Modifier.size(15.dp))
        }
    }
}

// ─── Magazine Reader ──────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MagazineReaderScreen(
    url: String,
    title: String,
    magazineId: String,
    onClose: () -> Unit,
    onProgressUpdate: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val embedUrl = remember(url) { toArchiveEmbedUrl(url) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(embedUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showControls by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showControls = true; delay(3000L); showControls = false }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.apply {
                        javaScriptEnabled = true; domStorageEnabled = true; loadWithOverviewMode = true
                        useWideViewPort = true; builtInZoomControls = true; displayZoomControls = false
                        setSupportZoom(true); mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = true; allowContentAccess = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) { isLoading = false; url?.let { currentUrl = it }; injectHideStyles(view); view?.postDelayed({ injectHideStyles(view) }, 1500); view?.postDelayed({ injectHideStyles(view) }, 3000) }
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) { isLoading = true }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(embedUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter), color = CGreen, trackColor = Color.Transparent) }

        Box(modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showControls = !showControls })

        AnimatedVisibility(visible = showControls, enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { -it } + fadeIn(tween(400)), exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { -it } + fadeOut(tween(300)), modifier = Modifier.align(Alignment.TopStart)) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.0f)))).padding(top = 40.dp, bottom = 32.dp, start = 4.dp, end = 4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "Tap anywhere to show/hide controls", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    IconButton(onClick = { showComments = true }) { Icon(Icons.Filled.Comment, contentDescription = "Comments", tint = CGreen, modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { webViewRef?.reload() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)); try { context.startActivity(intent) } catch (e: Exception) { } }) { Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser", tint = CGreen, modifier = Modifier.size(20.dp)) }
                }
            }
        }
        if (showComments) { MagazineCommentsPanel(magazineId = magazineId, onDismiss = { showComments = false }) }
    }
}

// ─── Continue Reading Strip ───────────────────────────────────────────────────

@Composable
fun ContinueReadingStrip(progressList: List<ReadingProgress>, onResume: (ReadingProgress) -> Unit) {
    if (progressList.isEmpty()) return
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("▶ CONTINUE READING", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("${progressList.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(progressList.take(5), key = { it.magazineId }) { progress ->
                ContinueReadingCard(progress = progress, neonAlpha = neonAlpha, onResume = { onResume(progress) })
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

@Composable
fun ContinueReadingCard(progress: ReadingProgress, neonAlpha: Float = 0.6f, onResume: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "crCardScale")

    Box(modifier = Modifier.width(140.dp).scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onResume() }, backgroundColor = ComicGlassBg, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                    if (progress.magazineCoverUrl.isNotBlank()) { AsyncImage(model = progress.magazineCoverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(progress.magazineCoverUrl).fillMaxSize()) }
                    else { Text("📖", fontSize = 32.sp) }
                    // ✅ Neon progress bar
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(ScrapbookDark.copy(alpha = 0.4f))) {
                        val animProgress by animateFloatAsState(targetValue = progress.percentComplete, animationSpec = tween(800, easing = LinearOutSlowInEasing), label = "crProgress")
                        Box(
                            modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenDeep)))
                        )
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(progress.magazineTitle, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text("${(progress.percentComplete * 100).toInt()}% read", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(6.dp)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        Text("RESUME →", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

@Composable
fun InlineShelfStack(
    shelf: MagazineShelf,
    magazines: List<MagazineCover>,
    neonAlpha: Float,
    onRead: () -> Unit,
    onTapMagazine: () -> Unit
) {
    var frontIndex by remember { mutableStateOf(magazines.size - 1) }
    var dragOffset by remember { mutableStateOf(0f) }

    val frontMagazine = magazines.getOrNull(frontIndex)

    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragOffset < -60f -> if (frontIndex > 0) frontIndex--
                            dragOffset > 60f -> if (frontIndex < magazines.size - 1) frontIndex++
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, delta -> dragOffset += delta }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Spotlight glow — fixed, no square artifact
        Canvas(modifier = Modifier.size(220.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CGreen.copy(alpha = neonAlpha * 0.25f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2f
                ),
                radius = size.minDimension / 2f
            )
        }

        // Fanned stack — show up to 5 magazines
        val displayCount = minOf(magazines.size, 5)
        val visibleMagazines = magazines.takeLast(displayCount)

        visibleMagazines.forEachIndexed { displayIndex, magazine ->
            val isFront = magazine.id == magazines.getOrNull(frontIndex)?.id
            val relativePos = displayIndex - (displayCount - 1) / 2f
            val angle = relativePos * 8f
            val offsetX = relativePos * 24f
            val offsetY = if (isFront) -12f else kotlin.math.abs(relativePos) * 3f

            val animAngle by animateFloatAsState(targetValue = angle, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "stackAngle_$displayIndex")
            val animOffsetX by animateFloatAsState(targetValue = offsetX, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "stackOffX_$displayIndex")
            val animOffsetY by animateFloatAsState(targetValue = offsetY, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "stackOffY_$displayIndex")
            val animScale by animateFloatAsState(targetValue = if (isFront) 1f else 0.88f - kotlin.math.abs(relativePos) * 0.04f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "stackScale_$displayIndex")

            Box(
                modifier = Modifier
                    .offset(x = animOffsetX.dp, y = animOffsetY.dp)
                    .rotate(animAngle)
                    .scale(animScale)
                    .zIndex(if (isFront) displayCount.toFloat() else displayIndex.toFloat())
                    .size(width = 140.dp, height = 186.dp)
            ) {
                // Shadow
                Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 5.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = if (isFront) 0.4f else 0.2f)))
                // Card
                Box(
                    modifier = Modifier.matchParentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(width = if (isFront) 2.dp else 1.dp, color = if (isFront) CGreen else ScrapbookBorder.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                        .clickable { if (isFront) onTapMagazine() else frontIndex = magazines.indexOf(magazine).takeIf { it >= 0 } ?: frontIndex }
                ) {
                    if (magazine.coverImageResId != null) {
                        Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (magazine.coverImageUrl != null) {
                        AsyncImage(model = magazine.coverImageUrl, contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(magazine.coverImageUrl).fillMaxSize())
                    }
                    if (!isFront) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
                    } else {
                        // Shine on front card
                        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.07f), Color.Transparent))))
                    }
                }
            }
        }
    }

    // Front magazine info + actions
    if (frontMagazine != null) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(frontMagazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (frontMagazine.platform.isNotBlank() || frontMagazine.era.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (frontMagazine.era.isNotBlank()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(eraAccentColor(frontMagazine.era).copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(frontMagazine.era, fontFamily = BangersFontFamily, color = eraAccentColor(frontMagazine.era), fontSize = 10.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Swipe hint dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    magazines.forEachIndexed { i, _ ->
                        Box(modifier = Modifier.size(if (i == frontIndex) 8.dp else 5.dp).clip(CircleShape).background(if (i == frontIndex) CGreen else ScrapbookBorder.copy(alpha = 0.4f)))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp))
                        .clickable { onRead() }.padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.MenuBook, contentDescription = null, tint = CGreen, modifier = Modifier.size(15.dp))
                        Text("READ NOW", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("← swipe to browse →", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
        }
    }
}

// ─── My Shelves Section ───────────────────────────────────────────────────────

@Composable
fun MyShelvesSection(
    shelves: List<MagazineShelf>,
    allMagazines: List<MagazineCover> = emptyList(),
    onCreateShelf: () -> Unit,
    onShelfTap: (MagazineShelf) -> Unit
) {
    val shelf = shelves.firstOrNull()
    val shelfMagazines = remember(shelf?.magazineIds, allMagazines) {
        shelf?.magazineIds?.mapNotNull { id -> allMagazines.firstOrNull { it.id == id } } ?: emptyList()
    }
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🗂️ MY COLLECTION", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            if (shelfMagazines.isNotEmpty()) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${shelfMagazines.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                }
            }
        }

        if (shelfMagazines.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.White.copy(alpha = 0.46f), cornerRadius = 12.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("YOUR COLLECTION IS EMPTY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap the bookmark on any magazine cover to add it here", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            shelf?.let { s ->
                InlineShelfStack(
                    shelf = s,
                    magazines = shelfMagazines,
                    neonAlpha = neonAlpha,
                    onRead = { onShelfTap(s) },
                    onTapMagazine = { onShelfTap(s) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

@Composable
fun ShelfCard(shelf: MagazineShelf, allMagazines: List<MagazineCover> = emptyList(), neonAlpha: Float = 0.6f, onTap: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shelfCardScale")

    val shelfMagazines = remember(shelf.magazineIds, allMagazines) {
        shelf.magazineIds.mapNotNull { id -> allMagazines.firstOrNull { it.id == id } }
    }

    Box(modifier = Modifier.width(120.dp).scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp)).clickable { pressed = true; onTap() },
            backgroundColor = ComicGlassBg, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Mini fanned preview, or emoji fallback if empty
                Box(modifier = Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    if (shelfMagazines.isEmpty()) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, CGreen.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(shelf.emoji, fontSize = 22.sp)
                        }
                    } else {
                        shelfMagazines.take(3).forEachIndexed { index, mag ->
                            val angle = (index - 1) * 12f
                            val xOffset = (index - 1) * 14
                            Box(
                                modifier = Modifier
                                    .offset(x = xOffset.dp)
                                    .rotate(angle)
                                    .size(width = 38.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            ) {
                                if (mag.coverImageResId != null) {
                                    Image(painter = painterResource(id = mag.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else if (mag.coverImageUrl != null) {
                                    AsyncImage(model = mag.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(mag.coverImageUrl).fillMaxSize())
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(shelf.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${shelf.magazineIds.size} mags", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

@Composable
fun CreateShelfDialog(onDismiss: () -> Unit, onConfirm: (name: String, emoji: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📚") }
    val emojiOptions = listOf("📚", "🎮", "⭐", "🔥", "🏆", "🎯", "💾", "📺", "🕹️", "🌟")
    AlertDialog(
        modifier = Modifier.comicPop(),
        onDismissRequest = onDismiss, containerColor = ComicGlassBg,
        title = { Text("NEW SHELF", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PICK AN EMOJI", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emojiOptions) { emoji ->
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(if (selectedEmoji == emoji) CGreen else Color.White.copy(alpha = 0.46f)).border(2.dp, ScrapbookBorder, CircleShape).clickable { selectedEmoji = emoji }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 16.sp) }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Shelf name...", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark.copy(alpha = 0.4f)) }, singleLine = true, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookDark, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f), focusedContainerColor = Color.White.copy(alpha = 0.8f), unfocusedContainerColor = Color.White.copy(alpha = 0.6f), cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            if (name.isNotBlank()) {
                RetroGlassButton(text = "CREATE", onClick = { onConfirm(name.trim(), selectedEmoji) }, cornerRadius = 8.dp)
            } else {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark.copy(alpha = 0.3f)).padding(horizontal = 16.dp, vertical = 8.dp)) { Text("CREATE", fontFamily = BangersFontFamily, color = CGreen.copy(alpha = 0.35f), fontSize = 16.sp) }
            }
        },
        dismissButton = {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onDismiss() }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp) }
        }
    )
}

// ─── Star Rating Bar ──────────────────────────────────────────────────────────

@Composable
fun StarRatingBar(rating: Int, onRate: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            var pressed by remember { mutableStateOf(false) }
            val starScale by animateFloatAsState(targetValue = if (pressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "starScale_$i")
            Icon(imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = "$i stars", tint = if (i <= rating) CGreenDeep else ScrapbookDark.copy(alpha = 0.2f), modifier = Modifier.size(28.dp).scale(starScale).clickable { pressed = true; onRate(i) })
            LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(200); pressed = false } }
        }
    }
}

// ─── Ratings Section ──────────────────────────────────────────────────────────

@Composable
fun MagazineRatingsSection(magazineId: String, currentUserId: String) {
    var ratings by remember { mutableStateOf<List<MagazineRating>>(emptyList()) }
    var myRating by remember { mutableStateOf(0) }
    var myReview by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showReviewField by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(magazineId) {
        try {
            val docs = FirebaseFirestore.getInstance().collection("magazine_ratings").whereEqualTo("magazineId", magazineId).orderBy("timestamp", Query.Direction.DESCENDING).limit(20).get().await()
            ratings = docs.documents.mapNotNull { doc -> val data = doc.data ?: return@mapNotNull null; MagazineRating(magazineId = data["magazineId"] as? String ?: "", userId = data["userId"] as? String ?: "", stars = (data["stars"] as? Long)?.toInt() ?: 0, review = data["review"] as? String ?: "", username = data["username"] as? String ?: "", timestamp = data["timestamp"] as? Long ?: 0L) }
            val mine = ratings.firstOrNull { it.userId == currentUserId }
            myRating = mine?.stars ?: 0; myReview = mine?.review ?: ""
        } catch (e: Exception) { } finally { isLoading = false }
    }

    val avgRating = if (ratings.isEmpty()) 0f else ratings.map { it.stars }.average().toFloat()
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("⭐ RATINGS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
            if (ratings.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(String.format("%.1f", avgRating), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                    Icon(Icons.Filled.Star, contentDescription = null, tint = CGreenDeep, modifier = Modifier.size(18.dp))
                    Text("(${ratings.size})", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (currentUserId.isNotBlank()) {
            ScrapbookCard(modifier = Modifier.fillMaxWidth().border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp)), backgroundColor = Color.White.copy(alpha = 0.46f), cornerRadius = 12.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("YOUR RATING", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    StarRatingBar(rating = myRating, onRate = { star -> myRating = star; showReviewField = true })
                    if (showReviewField || myReview.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = myReview, onValueChange = { myReview = it }, placeholder = { Text("Write a review...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) }, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CGreen, unfocusedBorderColor = ScrapbookBorder, focusedContainerColor = ComicGlassBg, unfocusedContainerColor = ComicGlassBg, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth(), maxLines = 3)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (myRating > 0) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(8.dp)).clickable(enabled = myRating > 0 && !isSubmitting) {
                            isSubmitting = true
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@clickable
                            FirebaseFirestore.getInstance().collection("magazine_ratings").document("${uid}_$magazineId").set(hashMapOf("magazineId" to magazineId, "userId" to uid, "stars" to myRating, "review" to myReview, "username" to (FirebaseAuth.getInstance().currentUser?.displayName ?: ""), "timestamp" to System.currentTimeMillis())).addOnSuccessListener { isSubmitting = false; showReviewField = false }.addOnFailureListener { isSubmitting = false }
                        }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            if (isSubmitting) { CircularProgressIndicator(color = CGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) }
                            else { Text("SUBMIT RATING", fontFamily = BangersFontFamily, color = CGreen, fontSize = 15.sp) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (!isLoading && ratings.isNotEmpty()) {
            Text("COMMUNITY REVIEWS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            ratings.filter { it.userId != currentUserId }.take(5).forEach { rating -> ReviewCard(rating = rating); Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun ReviewCard(rating: MagazineRating) {
    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 10.dp, shadowOffset = 2.dp) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rating.username.ifBlank { "Anonymous" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..5) { Icon(imageVector = if (i <= rating.stars) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = null, tint = if (i <= rating.stars) CGreenDeep else ScrapbookDark.copy(alpha = 0.15f), modifier = Modifier.size(14.dp)) }
                }
            }
            if (rating.review.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text(rating.review, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, lineHeight = 18.sp) }
        }
    }
}

// ─── Reading Challenges ───────────────────────────────────────────────────────

@Composable
fun ReadingChallengesSection(readCount: Int, ratingCount: Int, shelfItemCount: Int, readEras: List<String>) {
    val challenges = remember(readCount, ratingCount, shelfItemCount, readEras) {
        allChallenges.map { challenge ->
            val current = when (challenge.id) {
                "c1", "c2", "c8" -> readCount
                "c3" -> readEras.count { it == "SNES" }
                "c4" -> readEras.count { it == "PS1" }
                "c5" -> readEras.count { it == "NES" }
                "c6" -> shelfItemCount
                "c7" -> ratingCount
                else -> 0
            }
            challenge.copy(current = current.coerceAtMost(challenge.target), isComplete = current >= challenge.target)
        }
    }
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🏆 READING CHALLENGES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Complete challenges to earn badges!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))
        val completed = challenges.count { it.isComplete }
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp)).padding(12.dp)) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("🏆", fontSize = 24.sp)
                Column(modifier = Modifier.weight(1f)) {
                    // Green stripe
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                    Text("$completed / ${challenges.size} COMPLETED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    val overallProgress = completed.toFloat() / challenges.size
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookBorder.copy(alpha = 0.3f))) {
                        // Green stripe
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                        val animProg by animateFloatAsState(targetValue = overallProgress, animationSpec = tween(1000, easing = LinearOutSlowInEasing), label = "overallProg")
                        Box(modifier = Modifier.fillMaxWidth(animProg).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(colors = listOf(CGreen, CGreen.copy(alpha = 0.7f)))))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("MONTHLY", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        challenges.filter { it.type == "MONTHLY" }.forEach { challenge -> ChallengeCard(challenge = challenge, neonAlpha = neonAlpha); Spacer(modifier = Modifier.height(8.dp)) }
        Spacer(modifier = Modifier.height(4.dp))
        Text("THEME", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        challenges.filter { it.type == "THEME" }.forEach { challenge -> ChallengeCard(challenge = challenge, neonAlpha = neonAlpha); Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
fun ChallengeCard(challenge: ReadingChallenge, neonAlpha: Float = 0.6f) {
    val completionScale by animateFloatAsState(targetValue = if (challenge.isComplete) 1f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "challengeScale")
    ScrapbookCard(
        modifier = Modifier.fillMaxWidth().scale(completionScale).then(if (challenge.isComplete) Modifier.border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp)) else Modifier),
        backgroundColor = if (challenge.isComplete) CGreen.copy(alpha = 0.12f) else ComicGlassBg,
        borderColor = if (challenge.isComplete) CGreenDeep else ScrapbookBorder,
        cornerRadius = 10.dp, shadowOffset = 2.dp
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (challenge.isComplete) CGreen else Color.White.copy(alpha = 0.46f)).border(2.dp, if (challenge.isComplete) ScrapbookBorder else ScrapbookBorder.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Text(if (challenge.isComplete) "✅" else challenge.emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(challenge.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                Text(challenge.description, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(3.dp))) {
                    val progress = if (challenge.target > 0) challenge.current.toFloat() / challenge.target else 0f
                    val animProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(800, easing = LinearOutSlowInEasing), label = "challengeProg")
                    Box(
                        modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .then(
                                if (challenge.isComplete)
                                    Modifier.background(CGreen)
                                else
                                    Modifier.background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenDeep)))
                            )
                    )                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("${challenge.current}/${challenge.target}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (challenge.isComplete) CGreen else ScrapbookTextMuted, fontSize = 11.sp)
            }
            if (challenge.isComplete) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text("DONE!", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp) }
            }
        }
    }
}

// ─── Comments Panel ───────────────────────────────────────────────────────────

@Composable
fun MagazineCommentsPanel(magazineId: String, onDismiss: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var comments by remember { mutableStateOf<List<MagazineComment>>(emptyList()) }
    var newComment by remember { mutableStateOf("") }
    var selectedPage by remember { mutableStateOf<Int?>(null) }
    var pageInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(magazineId) {
        try {
            val docs = FirebaseFirestore.getInstance().collection("magazine_comments").whereEqualTo("magazineId", magazineId).orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().await()
            comments = docs.documents.mapNotNull { doc -> val data = doc.data ?: return@mapNotNull null; MagazineComment(id = doc.id, magazineId = data["magazineId"] as? String ?: "", userId = data["userId"] as? String ?: "", username = data["username"] as? String ?: "", profilePicUrl = data["profilePicUrl"] as? String ?: "", text = data["text"] as? String ?: "", page = (data["page"] as? Long)?.toInt(), timestamp = data["timestamp"] as? Long ?: 0L) }.filter { it.text.isNotBlank() }
        } catch (e: Exception) { } finally { isLoading = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).align(Alignment.BottomCenter).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(ComicGlassBg)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("💬 COMMENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Text("${comments.size}", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark, modifier = Modifier.size(20.dp)) }
                    }
                }
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CGreenDeep, modifier = Modifier.size(28.dp)) }
                } else if (comments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("💬", fontSize = 36.sp); Spacer(modifier = Modifier.height(8.dp)); Text("No comments yet — be the first!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) } }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(comments, key = { it.id }) { comment -> CommentCard(comment = comment) }
                    }
                }
                if (currentUser != null) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f)).border(BorderStroke(1.dp, ScrapbookBorder)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Page #", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            OutlinedTextField(value = pageInput, onValueChange = { pageInput = it; selectedPage = it.toIntOrNull() }, placeholder = { Text("optional", fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted) }, singleLine = true, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CGreen, unfocusedBorderColor = ScrapbookBorder, focusedContainerColor = ComicGlassBg, unfocusedContainerColor = ComicGlassBg, cursorColor = ScrapbookDark), shape = RoundedCornerShape(6.dp), modifier = Modifier.width(80.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                            OutlinedTextField(value = newComment, onValueChange = { newComment = it }, placeholder = { Text("Add a comment...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) }, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CGreen, unfocusedBorderColor = ScrapbookBorder, focusedContainerColor = ComicGlassBg, unfocusedContainerColor = ComicGlassBg, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f), maxLines = 3)
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (newComment.isNotBlank()) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f)).border(2.dp, ScrapbookBorder, CircleShape).clickable(enabled = newComment.isNotBlank() && !isSubmitting) {
                                isSubmitting = true
                                val uid = currentUser.uid
                                FirebaseFirestore.getInstance().collection("magazine_comments").add(hashMapOf("magazineId" to magazineId, "userId" to uid, "username" to (currentUser.displayName ?: ""), "profilePicUrl" to (currentUser.photoUrl?.toString() ?: ""), "text" to newComment.trim(), "page" to pageInput.toIntOrNull(), "timestamp" to System.currentTimeMillis())).addOnSuccessListener { ref -> comments = listOf(MagazineComment(id = ref.id, magazineId = magazineId, userId = uid, username = currentUser.displayName ?: "", text = newComment.trim(), page = pageInput.toIntOrNull(), timestamp = System.currentTimeMillis())) + comments; newComment = ""; pageInput = ""; isSubmitting = false }.addOnFailureListener { isSubmitting = false }
                            }, contentAlignment = Alignment.Center) {
                                if (isSubmitting) { CircularProgressIndicator(color = CGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp) }
                                else { Icon(Icons.Filled.Send, contentDescription = "Send", tint = CGreen, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentCard(comment: MagazineComment) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
            if (comment.profilePicUrl.isNotBlank()) { AsyncImage(model = comment.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(comment.profilePicUrl).fillMaxSize()) }
            else { Text(comment.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp) }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(comment.username.ifBlank { "Anonymous" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                if (comment.page != null) { Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.3f)).padding(horizontal = 6.dp, vertical = 1.dp)) { Text("p.${comment.page}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp) } }
            }
            Text(comment.text, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// ─── Because You Read ─────────────────────────────────────────────────────────

@Composable
fun BecauseYouReadSection(readHistory: List<String>, allMagazines: List<MagazineCover>, onMagazineTap: (MagazineCover) -> Unit) {
    if (readHistory.isEmpty()) return
    val lastReadId = readHistory.lastOrNull() ?: return
    val lastRead = allMagazines.firstOrNull { it.id == lastReadId } ?: return
    val recommendations = remember(readHistory, allMagazines) {
        allMagazines.filter { it.id !in readHistory }.sortedByDescending { mag -> var score = 0; if (mag.platform == lastRead.platform) score += 3; if (mag.era == lastRead.era) score += 2; score }.take(6)
    }
    if (recommendations.isEmpty()) return
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("✨", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("BECAUSE YOU READ", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                Text(lastRead.title, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(recommendations, key = { _, it -> it.id }) { index, magazine ->
                SpringEntrance(delayMs = index * 70) {
                    Box(modifier = Modifier.width(80.dp)) { MagazineCoverItem(magazine = magazine, onClick = { onMagazineTap(magazine) }, modifier = Modifier.fillMaxWidth()) }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
// ─── Magazines Screen ─────────────────────────────────────────────────────────

@Composable
fun MagazinesScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null,
    authViewModel: AuthViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val magazinesState by contentViewModel.magazinesState.collectAsState()
    val isLoadingMore by contentViewModel.isLoadingMoreMagazines.collectAsState()
    val hasMore by contentViewModel.hasMoreMagazines.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })

    var searchVisible by remember { mutableStateOf(false) }
    var addToShelfMagazine by remember { mutableStateOf<MagazineCover?>(null) }
    var shelfDetailFor by remember { mutableStateOf<MagazineShelf?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var selectedMagazine by remember { mutableStateOf<MagazineCover?>(null) }
    var readerVisible by remember { mutableStateOf(false) }
    var openingMagazine by remember { mutableStateOf<MagazineCover?>(null) }
    var visibleRows by remember { mutableStateOf(3) }
    var galleryMode by remember { mutableStateOf(false) }  // ✅ Gallery toggle
    var selectedEra by remember { mutableStateOf("ALL") }  // ✅ Era filter

    var readingProgress by remember { mutableStateOf<List<ReadingProgress>>(emptyList()) }
    var readHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var shelves by remember { mutableStateOf<List<MagazineShelf>>(emptyList()) }
    var showCreateShelf by remember { mutableStateOf(false) }
    var showRatingsFor by remember { mutableStateOf<MagazineCover?>(null) }
    var ratingCount by remember { mutableStateOf(0) }
    var shelfItemCount by remember { mutableStateOf(0) }
    var activeTab by remember { mutableStateOf("BROWSE") }

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val db = FirebaseFirestore.getInstance()
                val progressDocs = db.collection("users").document(uid).collection("reading_progress").orderBy("lastReadAt", Query.Direction.DESCENDING).limit(10).get().await()
                readingProgress = progressDocs.documents.mapNotNull { doc -> val data = doc.data ?: return@mapNotNull null; ReadingProgress(magazineId = data["magazineId"] as? String ?: "", magazineTitle = data["magazineTitle"] as? String ?: "", magazineCoverUrl = data["magazineCoverUrl"] as? String ?: "", lastReadAt = data["lastReadAt"] as? Long ?: 0L, percentComplete = (data["percentComplete"] as? Double)?.toFloat() ?: 0f) }
                readHistory = readingProgress.map { it.magazineId }
                val shelfDocs = db.collection("users").document(uid).collection("magazine_shelves").orderBy("createdAt", Query.Direction.ASCENDING).get().await()
                shelves = shelfDocs.documents.mapNotNull { doc -> val data = doc.data ?: return@mapNotNull null; MagazineShelf(id = doc.id, name = data["name"] as? String ?: "", emoji = data["emoji"] as? String ?: "📚", magazineIds = (data["magazineIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(), createdAt = data["createdAt"] as? Long ?: 0L) }
                shelfItemCount = shelves.sumOf { it.magazineIds.size }
                val ratingDocs = db.collection("magazine_ratings").whereEqualTo("userId", uid).get().await()
                ratingCount = ratingDocs.size()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(searchQuery) { delay(600); if (searchQuery != lastSearched) { lastSearched = searchQuery; visibleRows = 3; contentViewModel.fetchMagazines(searchQuery) } }

    val archiveMagazines = when (val state = magazinesState) {
        is ContentState.Success -> state.items.map { it.toMagazineCover() }
        else -> emptyList()
    }

    // ✅ Era filtered magazines
    val filteredArchiveMagazines = remember(archiveMagazines, selectedEra) {
        if (selectedEra == "ALL") archiveMagazines
        else archiveMagazines.filter { it.era.equals(selectedEra, ignoreCase = true) }
    }

    val allMagazinesForGallery = remember(sampleMagazineCovers, archiveMagazines) { sampleMagazineCovers + archiveMagazines }

    val magazinesPerShelf = 4
    val allShelves = filteredArchiveMagazines.chunked(magazinesPerShelf)
    val visibleShelves = allShelves.take(visibleRows)

    val magazineOfTheDay = remember(allMagazinesForGallery) { getMagazineOfTheDay(sampleMagazineCovers.ifEmpty { allMagazinesForGallery }) }

    fun openReader(magazine: MagazineCover) {
        if (!magazine.webUrl.isNullOrBlank()) {
            selectedMagazine = magazine
            openingMagazine = magazine   // book-open animation first, then the reader
            currentUser?.uid?.let { uid ->
                FirebaseFirestore.getInstance().collection("users").document(uid).collection("reading_progress").document(magazine.id).set(hashMapOf("magazineId" to magazine.id, "magazineTitle" to magazine.title, "magazineCoverUrl" to (magazine.coverImageUrl ?: ""), "lastReadAt" to System.currentTimeMillis(), "percentComplete" to 0.1f))
            }
        }
    }

    if (showCreateShelf && currentUser != null) {
        CreateShelfDialog(onDismiss = { showCreateShelf = false }, onConfirm = { name, emoji ->
            val uid = currentUser!!.uid; val shelfId = System.currentTimeMillis().toString()
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("magazine_shelves").document(shelfId).set(hashMapOf("name" to name, "emoji" to emoji, "magazineIds" to emptyList<String>(), "createdAt" to System.currentTimeMillis())).addOnSuccessListener { shelves = shelves + MagazineShelf(id = shelfId, name = name, emoji = emoji, createdAt = System.currentTimeMillis()) }
            showCreateShelf = false
        })
    }

    // ✅ Gallery mode — full screen
    if (galleryMode) {
        CoverArtGallery(
            magazines = allMagazinesForGallery,
            onClose = { galleryMode = false },
            onRead = { mag -> galleryMode = false; openReader(mag) },
            onBookmarkToggle = { magazine -> addToShelfMagazine = magazine },
            favoriteIds = favoriteIds
        )
        return
    }

    // ✅ Shelf detail — full screen
    shelfDetailFor?.let { shelf ->
        ShelfDetailScreen(
            shelf = shelf,
            allMagazines = allMagazinesForGallery,
            onClose = { shelfDetailFor = null },
            onRead = { mag -> shelfDetailFor = null; openReader(mag) },
            onRemoveFromShelf = { mag ->
                currentUser?.uid?.let { uid ->
                    val updatedIds = shelf.magazineIds.filter { it != mag.id }
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .collection("magazine_shelves").document(shelf.id)
                        .update("magazineIds", updatedIds)
                        .addOnSuccessListener {
                            shelves = shelves.map { if (it.id == shelf.id) it.copy(magazineIds = updatedIds) else it }
                            shelfDetailFor = shelfDetailFor?.copy(magazineIds = updatedIds)
                        }
                }
            }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Toolbar (Gallery + Search buttons) ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(BorderStroke(1.dp, ScrapbookBorder))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Gallery toggle
                    var galleryPressed by remember { mutableStateOf(false) }
                    val galleryScale by animateFloatAsState(targetValue = if (galleryPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "galleryBtnScale")
                    Box(
                        modifier = Modifier.scale(galleryScale).clip(RoundedCornerShape(10.dp))
                            .background(CGreen.copy(alpha = 0.12f))
                            .border(1.dp, CGreen.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { galleryPressed = true; galleryMode = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.GridView, contentDescription = null, tint = CGreen, modifier = Modifier.size(14.dp))
                            Text("GALLERY", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp)
                        }
                    }
                    LaunchedEffect(galleryPressed) { if (galleryPressed) { kotlinx.coroutines.delay(150); galleryPressed = false } }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Random magazine
                    var luckyPressed by remember { mutableStateOf(false) }
                    val luckyScale by animateFloatAsState(targetValue = if (luckyPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "luckyScale")
                    Box(
                        modifier = Modifier.scale(luckyScale).size(36.dp).clip(CircleShape)
                            .background(CGreen.copy(alpha = 0.12f))
                            .border(1.dp, CGreen.copy(alpha = 0.6f), CircleShape)
                            .clickable { luckyPressed = true; val allMags = sampleMagazineCovers + archiveMagazines; allMags.filter { !it.webUrl.isNullOrBlank() }.randomOrNull()?.let { openReader(it) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎲", fontSize = 16.sp)
                    }
                    LaunchedEffect(luckyPressed) { if (luckyPressed) { kotlinx.coroutines.delay(150); luckyPressed = false } }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Search
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(CGreen.copy(alpha = 0.12f))
                            .border(1.dp, CGreen.copy(alpha = 0.6f), CircleShape)
                            .clickable { searchVisible = !searchVisible; if (!searchVisible) { searchQuery = ""; focusManager.clearFocus(); contentViewModel.fetchMagazines() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search", tint = CGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Tab strip — white glass + green shadow on selected
            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f)).border(BorderStroke(2.dp, ScrapbookDark))) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    listOf("BROWSE", "SHELVES", "CHALLENGES").forEach { tab ->
                        val isSelected = activeTab == tab
                        var tabPressed by remember { mutableStateOf(false) }
                        val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tab_$tab")
                        Box(modifier = Modifier.weight(1f).scale(tabScale)) {
                            if (isSelected) {
                                Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(10.dp)).background(CGreen))
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.46f) else Color.Transparent)
                                    .then(if (isSelected) Modifier.border(1.5.dp, ScrapbookDark, RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable { tabPressed = true; activeTab = tab }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(tab, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookTextMuted, fontSize = 14.sp, letterSpacing = 0.5.sp)
                            }
                        }
                        LaunchedEffect(tabPressed) { if (tabPressed) { kotlinx.coroutines.delay(150); tabPressed = false } }
                    }
                }
            }

            // Search bar
            AnimatedVisibility(visible = searchVisible, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search retro magazines...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = ""; contentViewModel.fetchMagazines() }) { Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp)) } } }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }), textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CGreen, unfocusedBorderColor = ScrapbookBorder, focusedContainerColor = ComicGlassBg, unfocusedContainerColor = ComicGlassBg, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(horizontal = 16.dp, vertical = 8.dp))
            }

            when (activeTab) {
                "SHELVES" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            MyShelvesSection(shelves = shelves, allMagazines = allMagazinesForGallery, onCreateShelf = { showCreateShelf = true }, onShelfTap = { shelf -> shelfDetailFor = shelf })
                            Spacer(modifier = Modifier.height(16.dp))
                            if (currentUser == null) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Sign in to create shelves!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center) }
                            }
                        }
                    }
                }

                "CHALLENGES" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
                        item {
                            ReadingChallengesSection(readCount = readHistory.size, ratingCount = ratingCount, shelfItemCount = shelfItemCount, readEras = readHistory.mapNotNull { id -> sampleMagazineCovers.firstOrNull { it.id == id }?.era ?: archiveMagazines.firstOrNull { it.id == id }?.era })
                        }
                    }
                }

                else -> { // BROWSE
                    when (val state = magazinesState) {
                        is ContentState.Loading -> {
                            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                                items(4) { ShimmerMagazineShelf() }
                            }
                        }
                        is ContentState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                GameOverState(message = state.message, onRetry = { contentViewModel.fetchMagazines() })
                            }
                        }
                        is ContentState.Success -> {
                            val isSearching = searchQuery.isNotBlank()
                            if (isSearching) {
                                LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                    items(state.items, key = { it.id }) { item -> ScrapbookMagazineGridItem(item = item, isBookmarked = favoriteIds.contains(item.id), onBookmarkToggle = { favoritesViewModel?.toggleFavorite(item.toFavoriteItem()) }, onClick = { openReader(item.toMagazineCover()) }) }
                                }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 1.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(0.1.dp), modifier = Modifier.fillMaxSize()) {

                                    // ─── Page Hero ────────────────────────────────────
                                    item {
                                        RetroHubPageHero(
                                            config = magazinesHeroConfig,
                                            onCtaClick = { /* already on magazines */ }
                                        )
                                    }
                                    item {
                                        RetroHubPageTicker(config = magazinesHeroConfig)
                                    }

                                    // ✅ Continue Reading
                                    if (readingProgress.isNotEmpty()) {
                                        item { Spacer(modifier = Modifier.height(8.dp)); ContinueReadingStrip(progressList = readingProgress, onResume = { progress -> val mag = archiveMagazines.firstOrNull { it.id == progress.magazineId } ?: sampleMagazineCovers.firstOrNull { it.id == progress.magazineId }; mag?.let { openReader(it) } }) }
                                    }

                                    // ✅ Magazine of the Day
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        magazineOfTheDay?.let { MagazineOfTheDayCard(magazine = it, onRead = { openReader(it) }) }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                                    }

                                    // ✅ Era timeline — inside scroll
                                    item { MagazineEraTimeline(selectedEra = selectedEra, onEraSelected = { selectedEra = it }) }

// ✅ Anniversary banner — "X years ago this month"
                                    item {
                                        val anniversaryHighlights = remember(allMagazinesForGallery) { getAnniversaryHighlights(allMagazinesForGallery) }
                                        AnniversaryBanner(highlights = anniversaryHighlights)
                                    }

// ✅ Ad Break
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AdBreakSection()
                                    }

                                    // Shelf rows
                                    itemsIndexed(visibleShelves) { index, shelfMagazines ->
                                        if (index == 0) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("📚 ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                                                if (selectedEra != "ALL") {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraAccentColor(selectedEra).copy(alpha = 0.15f)).border(1.dp, eraAccentColor(selectedEra).copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                                        Text(selectedEra, fontFamily = BangersFontFamily, color = eraAccentColor(selectedEra), fontSize = 11.sp)
                                                    }
                                                } else {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                                        Text("${filteredArchiveMagazines.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                        }
                                        ShelfRow(magazinesOnShelf = shelfMagazines, shelfImageResId = R.drawable.shelf, favoriteIds = favoriteIds, onBookmarkToggle = { magazine -> addToShelfMagazine = magazine }, onMagazineClick = { magazine -> openReader(magazine) }, magazinesPerShelf = magazinesPerShelf)                                    }

                                    // ✅ Because You Read
                                    item { BecauseYouReadSection(readHistory = readHistory, allMagazines = sampleMagazineCovers + archiveMagazines, onMagazineTap = { openReader(it) }) }

                                    // Load more
                                    val totalRows = allShelves.size
                                    val canShowMore = visibleRows < totalRows || hasMore
                                    if (canShowMore) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                                if (isLoadingMore) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Loading more...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) }
                                                } else {
                                                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp)).clickable { val newVisible = visibleRows + 3; visibleRows = newVisible; if (newVisible >= totalRows && hasMore) contentViewModel.loadMoreMagazines() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text("📚", fontSize = 16.sp)
                                                            Text("VIEW MORE MAGAZINES", fontFamily = BangersFontFamily, color = CGreen, fontSize = 16.sp, letterSpacing = 1.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> { }
                    }
                }
            }
        }

        // ✅ Reader overlay
        AnimatedVisibility(visible = readerVisible, enter = slideInVertically(tween(500, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)), exit = slideOutVertically(tween(400, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)), modifier = Modifier.fillMaxSize()) {
            selectedMagazine?.let { magazine -> MagazineReaderScreen(url = magazine.webUrl ?: "", title = magazine.title, magazineId = magazine.id, onClose = { readerVisible = false; selectedMagazine = null }) }
        }
        // ✅ Book-open animation (cover flips open, then the reader slides in)
        openingMagazine?.let { mag ->
            MagazineOpenAnimation(magazine = mag, onFinished = {
                openingMagazine = null
                readerVisible = true
            })
        }
        // ✅ Add to shelf picker overlay
        addToShelfMagazine?.let { mag ->
            AddToShelfPicker(
                magazine = mag,
                shelves = shelves,
                onAddToShelf = { shelf ->
                    currentUser?.uid?.let { uid ->
                        if (mag.id !in shelf.magazineIds) {
                            val updatedIds = shelf.magazineIds + mag.id
                            FirebaseFirestore.getInstance()
                                .collection("users").document(uid)
                                .collection("magazine_shelves").document(shelf.id)
                                .update("magazineIds", updatedIds)
                                .addOnSuccessListener {
                                    shelves = shelves.map {
                                        if (it.id == shelf.id) it.copy(magazineIds = updatedIds) else it
                                    }
                                    shelfItemCount = shelves.sumOf { it.magazineIds.size }
                                }
                        }
                    }
                    addToShelfMagazine = null
                },
                onCreateNewShelf = {
                    addToShelfMagazine = null
                    showCreateShelf = true
                },
                onDismiss = { addToShelfMagazine = null }
            )
        }

        // ✅ Ratings overlay
        if (showRatingsFor != null && currentUser != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { showRatingsFor = null }) {
                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).align(Alignment.BottomCenter).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(ComicGlassBg).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐ RATE & REVIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { showRatingsFor = null }) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark, modifier = Modifier.size(20.dp)) }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            MagazineRatingsSection(magazineId = showRatingsFor!!.id, currentUserId = currentUser!!.uid)
                        }
                    }
                }
            }
        }
    } // closes main Box
} // closes MagazinesScreen

// ─── Add to Shelf Picker ────────────────────────────────────────────────────────

@Composable
fun AddToShelfPicker(
    magazine: MagazineCover,
    shelves: List<MagazineShelf>,
    onAddToShelf: (MagazineShelf) -> Unit,
    onCreateNewShelf: () -> Unit,
    onDismiss: () -> Unit
) {
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f).align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(ComicGlassBg)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Handle bar
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookBorder))

                // Header
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(colors = listOf(CGreen, CGreenMint, CGreen)))
                        .border(BorderStroke(1.dp, ScrapbookBorder))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ADD TO SHELF", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                            Text(magazine.title, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Create new shelf option — always first
                    item {
                        var pressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "newShelfScale")
                        Box(
                            modifier = Modifier.fillMaxWidth().scale(scale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(12.dp))
                                .clickable { pressed = true; onCreateNewShelf() }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(CGreen).border(2.dp, ScrapbookDark, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                                }
                                Text("CREATE NEW SHELF", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                            }
                        }
                        LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                    }

                    if (shelves.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                Text("No shelves yet — create one above!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                            }
                        }
                    }

                    // Existing shelves
                    items(shelves, key = { it.id }) { shelf ->
                        val alreadyAdded = magazine.id in shelf.magazineIds
                        var pressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shelfPickItem_${shelf.id}")

                        Box(
                            modifier = Modifier.fillMaxWidth().scale(scale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (alreadyAdded) CGreen.copy(alpha = 0.08f) else ComicGlassBg)
                                .border(width = 1.5.dp, color = if (alreadyAdded) CGreen.copy(alpha = 0.5f) else ScrapbookBorder.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                                .clickable(enabled = !alreadyAdded) { pressed = true; onAddToShelf(shelf) }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, CGreen.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(shelf.emoji, fontSize = 18.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(shelf.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp)
                                    Text("${shelf.magazineIds.size} magazines", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                }
                                if (alreadyAdded) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("ADDED ✓", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 10.sp)
                                    }
                                } else {
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                    }
                }
            }
        }
    }
}

// ─── Unchanged composables ─────────────────────────────────────────────────────

@Composable
fun ScrapbookMagazineGridItem(item: ArchiveItem, onClick: () -> Unit, isBookmarked: Boolean = false, onBookmarkToggle: () -> Unit = {}, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clickable(onClick = onClick), backgroundColor = ComicGlassBg, cornerRadius = 8.dp, shadowOffset = 2.dp) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(model = item.thumbnailUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(item.thumbnailUrl).fillMaxSize().background(Color.White.copy(alpha = 0.92f)))
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).clip(CircleShape).background(CGreen).border(1.dp, ScrapbookBorder, CircleShape)) { IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(22.dp)) { Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(12.dp)) } }
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(ScrapbookDark.copy(alpha = 0.75f)).padding(4.dp)) { Text(text = item.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable
fun ArchiveMagazineGridItem(item: ArchiveItem, onClick: () -> Unit, isBookmarked: Boolean = false, onBookmarkToggle: () -> Unit = {}, modifier: Modifier = Modifier) { ScrapbookMagazineGridItem(item = item, onClick = onClick, isBookmarked = isBookmarked, onBookmarkToggle = onBookmarkToggle, modifier = modifier) }

@Composable
fun MagazineCoverItem(
    magazine: MagazineCover,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    heightBoost: Float = 0f // 0f = normal, positive = taller (staggered shelf look)
) {
    var pressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    var showTitle by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magTiltScale_${magazine.id}")
    val animTiltX by animateFloatAsState(targetValue = if (pressed) tiltX else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magTiltX_${magazine.id}")
    val animTiltY by animateFloatAsState(targetValue = if (pressed) tiltY else 0f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magTiltY_${magazine.id}")
    // Glow pulse
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "mgPress_${magazine.id}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "mgShOff_${magazine.id}")

    // Stable slight rotation + height boost per magazine, so the shelf doesn't look machine-aligned
    val baseRotation = remember(magazine.id) { stableRandomFor(magazine.id, -1.6f..1.6f) }
    val eraColor = if (magazine.era.isNotBlank()) eraAccentColor(magazine.era) else CGreen

    Box(
        modifier = modifier
            .padding(bottom = (heightBoost * -1).coerceAtLeast(0f).dp)
            .rotate(baseRotation)
            .scale(cardScale)
            .graphicsLayer {
                rotationX = animTiltX
                rotationY = animTiltY
                cameraDistance = 14f * density
            }
    ) {
        // Glow halo
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(6.dp)).background(CGreen))
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .pointerInput(magazine.id) {
                    detectTapGestures(
                        onPress = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            tiltX = ((offset.y - centerY) / centerY * -8f).coerceIn(-8f, 8f)
                            tiltY = ((offset.x - centerX) / centerX * 8f).coerceIn(-8f, 8f)
                            pressed = true
                            showTitle = true
                            tryAwaitRelease()
                            pressed = false
                            onClick()
                        }
                    )
                },
            backgroundColor = ComicGlassBg,
            cornerRadius = 6.dp,
            shadowOffset = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Cover art — unobstructed by default
                if (magazine.coverImageResId != null) {
                    Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (magazine.coverImageUrl != null) {
                    AsyncImage(model = magazine.coverImageUrl, contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(magazine.coverImageUrl).fillMaxSize().background(Color.White.copy(alpha = 0.92f)))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                        Text(text = magazine.title, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, textAlign = TextAlign.Center, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                    }
                }

                // ComicShimmer overlay
                ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 6.dp)

                // Era spine accent — thin colored strip down the left edge
                if (magazine.era.isNotBlank()) {
                    Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().width(4.dp).background(eraColor))
                }

                // Dog-ear corner fold — replaces circular bookmark badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(RoundedCornerShape(bottomStart = 10.dp))
                        .background(if (isBookmarked) CGreen else Color.Black.copy(alpha = 0.35f))
                        .clickable { onBookmarkToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) ScrapbookDark else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(11.dp).align(Alignment.Center)
                    )
                }

                // Title strip — only on press/reveal, not permanent
                AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { it / 2 },
                    exit = fadeOut(tween(400)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark.copy(alpha = 0.85f)).padding(4.dp)) {
                        Text(
                            text = magazine.title,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                LaunchedEffect(showTitle) {
                    if (showTitle) { kotlinx.coroutines.delay(1800); showTitle = false }
                }
            }
        }
    }
}

@Composable
fun ShelfRow(
    magazinesOnShelf: List<MagazineCover>,
    shelfImageResId: Int,
    onMagazineClick: (MagazineCover) -> Unit,
    magazinesPerShelf: Int,
    favoriteIds: Set<String> = emptySet(),
    onBookmarkToggle: (MagazineCover) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.BottomCenter) {
        Image(
            painter = painterResource(id = shelfImageResId),
            contentDescription = "Magazine Shelf",
            modifier = Modifier.fillMaxWidth().matchParentSize(),
            contentScale = ContentScale.FillBounds
        )

        // Subtle diagonal light sweep — ties section to the vintage scan-line motif used elsewhere
        val sweepT = rememberInfiniteTransition(label = "shelfSweep")
        val sweepOffset by sweepT.animateFloat(
            initialValue = -1f, targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
            label = "shelfSweepOffset"
        )
        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.04f), Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(sweepOffset * 800f, 0f),
                    end = androidx.compose.ui.geometry.Offset(sweepOffset * 800f + 300f, 600f)
                )
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            magazinesOnShelf.forEachIndexed { index, magazine ->
                // Stable stagger: every other cover sits slightly taller, like real uneven spines
                val heightBoost = remember(magazine.id) { if (index % 2 == 0) stableRandomFor(magazine.id, 0f..6f) else 0f }
                SpringEntrance(modifier = Modifier.weight(1f), delayMs = index * 70) {
                    MagazineCoverItem(
                        magazine = magazine,
                        onClick = { onMagazineClick(magazine) },
                        isBookmarked = favoriteIds.contains(magazine.id),
                        onBookmarkToggle = { onBookmarkToggle(magazine) },
                        modifier = Modifier.fillMaxWidth(),
                        heightBoost = heightBoost
                    )
                }
            }
            if (magazinesOnShelf.size < magazinesPerShelf) {
                for (i in 0 until (magazinesPerShelf - magazinesOnShelf.size)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}


// ─── Book-open animation: the cover swings open on its spine ──────────────────

@Composable
fun MagazineOpenAnimation(magazine: MagazineCover, onFinished: () -> Unit) {
    val appear = remember { Animatable(0.6f) }
    val open = remember { Animatable(0f) }
    val dim = remember { Animatable(0f) }
    val haptic = rememberTapHaptic()

    LaunchedEffect(magazine.id) {
        launch { dim.animateTo(1f, tween(200)) }
        appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 420f))
        haptic()
        Chiptune.play(Chiptune.Sfx.POP)
        open.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * dim.value))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(width = 210.dp, height = 290.dp)
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
        ) {
            // Inside pages (revealed as the cover opens)
            Box(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(4.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(magazine.title.uppercase(), fontFamily = BangersFontFamily, fontSize = 18.sp,
                        color = ScrapbookDark, maxLines = 2)
                    repeat(9) { i ->
                        Box(Modifier.fillMaxWidth(if (i % 3 == 2) 0.6f else 1f).height(6.dp)
                            .clip(RoundedCornerShape(3.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                    }
                }
            }
            // Fluttering pages that follow the cover
            for (p in 1..3) {
                val lag = (open.value - p * 0.08f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = -100f * lag
                            cameraDistance = 14f * density
                            alpha = if (lag > 0f && lag < 1f) 1f else 0f
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF7F7F2))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                )
            }
            // The cover
            Box(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        rotationY = -100f * open.value
                        cameraDistance = 14f * density
                    }
                    .clip(RoundedCornerShape(4.dp))
                    .background(CGreen)
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(4.dp))
            ) {
                when {
                    magazine.coverImageResId != null -> Image(
                        painter = painterResource(id = magazine.coverImageResId),
                        contentDescription = magazine.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    !magazine.coverImageUrl.isNullOrBlank() -> AsyncImage(
                        model = magazine.coverImageUrl,
                        contentDescription = magazine.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.halftoneReveal(magazine.coverImageUrl).fillMaxSize()
                    )
                    else -> Text("📰", fontSize = 48.sp, modifier = Modifier.align(Alignment.Center))
                }
                // spine shadow
                Box(Modifier.fillMaxHeight().width(10.dp).background(
                    Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))))
            }
        }
    }
}

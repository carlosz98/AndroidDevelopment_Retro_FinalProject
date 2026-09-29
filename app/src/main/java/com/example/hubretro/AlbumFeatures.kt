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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke

// ─── Album Color Palette ──────────────────────────────────────────────────────
// Albums page uses the app-wide scrapbook/parchment palette

val VinylDark    = ScrapbookDark       // ScrapbookDark — text + icons on bright surfaces
val VinylDeep    = ComicGlassBg       // ComicGlassBg — page / sheet backgrounds
val VinylSurface = ComicGlassBg       // warm parchment light — secondary surfaces
val VinylCard    = ComicGlassBg       // Color.White.copy(alpha = 0.46f) — card backgrounds
val VinylAmber   = CGreen       // CGreen — primary accent
val VinylAmberDim= CAcYellow       // warm amber — secondary accent
val VinylOrange  = CAcYellow       // warm amber — secondary CTA
val VinylCream   = ScrapbookDark       // ScrapbookDark — primary text on parchment
val VinylGroove  = ComicGlassBg       // ScrapbookBorder — card borders / dividers
val VinylLabel   = CGreen       // CGreen

// ─── Vibe → Mood Color ────────────────────────────────────────────────────────

fun vibeMoodColor(vibe: String): Color = when (vibe.uppercase()) {
    "NOSTALGIC" -> ComicGlassBg   // warm parchment
    "HYPE"      -> ComicGlassBg   // yellow-tinted cream
    "CHILL"     -> ComicGlassBg   // very subtle mint cream
    "EPIC"      -> ComicGlassBg   // amber-tinted cream
    "CUTE"      -> ComicGlassBg   // light pink-tinted cream
    else        -> ComicGlassBg   // ComicGlassBg
}

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val year: Int? = null,
    val webPlaybackUrl: String? = null,
    val era: String = "OTHER",
    val vibe: String = "CHILL",
    val playCount: Int = 0,
    val reactions: Map<String, Int> = emptyMap()
)

data class NowPlayingState(
    val title: String = "",
    val artist: String = "",
    val coverResId: Int? = null,
    val coverUrl: String? = null,
    val isPlaying: Boolean = false,
    val albumId: String = ""
)

data class AlbumTrack(
    val id: String,
    val title: String,
    val filename: String,
    val duration: String = "",
    val trackNumber: Int = 0,
    val playUrl: String = ""
)

data class CommunityListener(
    val uid: String = "",
    val username: String = "",
    val profilePicUrl: String? = null,
    val habboUsername: String = "",
    val habboRegion: String = "habbo.com",
    val albumTitle: String = "",
    val albumId: String = "",
    val timeAgo: String = ""
)

// ─── Era Color Helper ─────────────────────────────────────────────────────────
// NOTE: renamed from eraAccentColor -> albumEraColor. SharedComposables.kt already
// declares eraAccentColor(gameName: String) for games; same-signature name collision
// with this album-console version was causing ambiguity errors at every call site.

fun albumEraColor(era: String): Color = when (era.uppercase()) {
    "NES"      -> CAcRed
    "SNES"     -> CAcPurple
    "PS1"      -> ScrapbookTextMuted
    "PS2"      -> CAcBlue
    "N64"      -> CGreenDeep
    "GCN"      -> CAcPurple
    "GBA"      -> CGreen
    "NDS"      -> CAcYellow
    "PC"       -> CGreenDeep
    else       -> VinylAmber
}

// ─── Vibe Data ────────────────────────────────────────────────────────────────

data class AlbumVibe(val label: String, val emoji: String, val color: Color)

val albumVibes = listOf(
    AlbumVibe("ALL",       "🎵", VinylAmber),
    AlbumVibe("NOSTALGIC", "😢", CAcBlue),
    AlbumVibe("HYPE",      "🔥", CAcRed),
    AlbumVibe("CHILL",     "😌", CGreen),
    AlbumVibe("EPIC",      "⚔️", ScrapbookDark),
    AlbumVibe("CUTE",      "🌸", CAcRed)
)

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleAlbums = listOf(
    Album(id = "album1", title = "Gunbound", artist = "Synth Rider", coverImageResId = R.drawable.ostcover1, year = 1984, webPlaybackUrl = "https://archive.org/details/gunbound-soundtrack", era = "NES", vibe = "NOSTALGIC", playCount = 42, reactions = mapOf("🔥" to 12, "❤️" to 8, "🎮" to 5)),
    Album(id = "album2", title = "Pokemon Diamond & Pearl", artist = "Grid Runner", coverImageResId = R.drawable.ostcover2, year = 1988, webPlaybackUrl = "https://archive.org/details/pkmn-dppt-soundtrack", era = "NDS", vibe = "CUTE", playCount = 88, reactions = mapOf("🔥" to 31, "❤️" to 44, "🎮" to 19)),
    Album(id = "album3", title = "The Legend of Zelda: The Wind Waker", artist = "Chrome Catalyst", coverImageResId = R.drawable.ostcover3, year = 1991, webPlaybackUrl = "https://archive.org/details/the-legend-of-zelda-the-wind-waker-ost", era = "GCN", vibe = "EPIC", playCount = 76, reactions = mapOf("🔥" to 28, "❤️" to 35, "🎮" to 22)),
    Album(id = "album4", title = "Undertale", artist = "Vector Voyager", coverImageResId = R.drawable.ostcover4, year = 1986, webPlaybackUrl = "https://archive.org/details/undertaleost_202004", era = "PC", vibe = "NOSTALGIC", playCount = 95, reactions = mapOf("🔥" to 41, "❤️" to 52, "🎮" to 18)),
    Album(id = "album5", title = "Lego Harry Potter Years 1-4", artist = "Bit Shifter", coverImageResId = R.drawable.ostcover5, year = 1982, webPlaybackUrl = "https://archive.org/details/lego-harry-potter-years-1-4", era = "PS2", vibe = "CHILL", playCount = 33, reactions = mapOf("🔥" to 7, "❤️" to 15, "🎮" to 6)),
    Album(id = "album6", title = "Final Fantasy VII", artist = "Analog Hero", coverImageResId = R.drawable.ostcover6, year = 1987, webPlaybackUrl = "https://archive.org/details/final_fantasy_vii_soundtrack", era = "PS1", vibe = "EPIC", playCount = 112, reactions = mapOf("🔥" to 55, "❤️" to 61, "🎮" to 33)),
    Album(id = "album7", title = "The Sims", artist = "Digital Nomad", coverImageResId = R.drawable.ostcover7, year = 1985, webPlaybackUrl = "https://archive.org/details/simsmusic", era = "PC", vibe = "CHILL", playCount = 67, reactions = mapOf("🔥" to 22, "❤️" to 38, "🎮" to 11)),
    Album(id = "album8",  title = "Super Mario World",                   artist = "Koji Kondo",          coverImageUrl = "https://archive.org/services/img/SMWOriginalSoundVersion",  year = 1990, webPlaybackUrl = "https://archive.org/details/SMWOriginalSoundVersion",       era = "SNES", vibe = "NOSTALGIC", playCount = 134, reactions = mapOf("🔥" to 67,  "❤️" to 89,  "🎮" to 42)),
    Album(id = "album9",  title = "Chrono Trigger",                        artist = "Yasunori Mitsuda",    coverImageUrl = "https://archive.org/services/img/ChronoTriggerOST",         year = 1995, webPlaybackUrl = "https://archive.org/details/ChronoTriggerOST",              era = "SNES", vibe = "EPIC",      playCount = 201, reactions = mapOf("🔥" to 98,  "❤️" to 143, "🎮" to 61)),
    Album(id = "album10", title = "Mega Man 2",                             artist = "Takashi Tateishi",    coverImageUrl = "https://archive.org/services/img/mega-man-2-ost",           year = 1988, webPlaybackUrl = "https://archive.org/details/mega-man-2-ost",                era = "NES",  vibe = "HYPE",      playCount = 89,  reactions = mapOf("🔥" to 44,  "❤️" to 27,  "🎮" to 38)),
    Album(id = "album11", title = "Super Metroid",                          artist = "Kenji Yamamoto",      coverImageUrl = "https://archive.org/services/img/super-metroid-ost",        year = 1994, webPlaybackUrl = "https://archive.org/details/super-metroid-ost",             era = "SNES", vibe = "EPIC",      playCount = 77,  reactions = mapOf("🔥" to 38,  "❤️" to 29,  "🎮" to 24)),
    Album(id = "album12", title = "EarthBound",                             artist = "Hirokazu Tanaka",     coverImageUrl = "https://archive.org/services/img/earthbound-ost",           year = 1994, webPlaybackUrl = "https://archive.org/details/earthbound-ost",                era = "SNES", vibe = "CHILL",     playCount = 115, reactions = mapOf("🔥" to 52,  "❤️" to 78,  "🎮" to 34)),
    Album(id = "album13", title = "Sonic CD",                               artist = "Naofumi Hataya",      coverImageUrl = "https://archive.org/services/img/sonic-cd-ost",             year = 1993, webPlaybackUrl = "https://archive.org/details/sonic-cd-ost",                  era = "SEGA", vibe = "HYPE",      playCount = 93,  reactions = mapOf("🔥" to 48,  "❤️" to 31,  "🎮" to 26)),
    Album(id = "album14", title = "Street Fighter II",                      artist = "Yoko Shimomura",      coverImageUrl = "https://archive.org/services/img/street-fighter-2-ost",    year = 1991, webPlaybackUrl = "https://archive.org/details/street-fighter-2-ost",          era = "SNES", vibe = "HYPE",      playCount = 107, reactions = mapOf("🔥" to 59,  "❤️" to 41,  "🎮" to 47)),
    Album(id = "album15", title = "Donkey Kong Country",                    artist = "David Wise",          coverImageUrl = "https://archive.org/services/img/donkey-kong-country-ost",  year = 1994, webPlaybackUrl = "https://archive.org/details/donkey-kong-country-ost",       era = "SNES", vibe = "NOSTALGIC", playCount = 166, reactions = mapOf("🔥" to 79,  "❤️" to 112, "🎮" to 55)),
    Album(id = "album16", title = "GoldenEye 007",                          artist = "Grant Kirkhope",      coverImageUrl = "https://archive.org/services/img/goldeneye-007-ost",        year = 1997, webPlaybackUrl = "https://archive.org/details/goldeneye-007-ost",             era = "N64",  vibe = "EPIC",      playCount = 58,  reactions = mapOf("🔥" to 29,  "❤️" to 19,  "🎮" to 33)),
    Album(id = "album17", title = "Castlevania: Symphony of the Night",    artist = "Michiru Yamane",      coverImageUrl = "https://archive.org/services/img/castlevania-sotn-ost",     year = 1997, webPlaybackUrl = "https://archive.org/details/castlevania-sotn-ost",          era = "PS1",  vibe = "EPIC",      playCount = 148, reactions = mapOf("🔥" to 74,  "❤️" to 96,  "🎮" to 52)),
    Album(id = "album18", title = "Tetris (Game Boy)",                      artist = "Hirokazu Tanaka",     coverImageUrl = "https://archive.org/services/img/tetris-gb-ost",            year = 1989, webPlaybackUrl = "https://archive.org/details/tetris-gb-ost",                 era = "GB",   vibe = "HYPE",      playCount = 212, reactions = mapOf("🔥" to 101, "❤️" to 155, "🎮" to 67)),
    Album(id = "album19", title = "Secret of Mana",                         artist = "Hiroki Kikuta",       coverImageUrl = "https://archive.org/services/img/secret-of-mana-ost",      year = 1993, webPlaybackUrl = "https://archive.org/details/secret-of-mana-ost",            era = "SNES", vibe = "EPIC",      playCount = 139, reactions = mapOf("🔥" to 66,  "❤️" to 98,  "🎮" to 44)),
    Album(id = "album20", title = "Banjo-Kazooie",                          artist = "Grant Kirkhope",      coverImageUrl = "https://archive.org/services/img/banjo-kazooie-ost",        year = 1998, webPlaybackUrl = "https://archive.org/details/banjo-kazooie-ost",             era = "N64",  vibe = "NOSTALGIC", playCount = 183, reactions = mapOf("🔥" to 87,  "❤️" to 131, "🎮" to 59)),
    Album(id = "album21", title = "Spyro the Dragon",                       artist = "Stewart Copeland",    coverImageUrl = "https://archive.org/services/img/spyro-the-dragon-ost",    year = 1998, webPlaybackUrl = "https://archive.org/details/spyro-the-dragon-ost",          era = "PS1",  vibe = "CHILL",     playCount = 97,  reactions = mapOf("🔥" to 43,  "❤️" to 71,  "🎮" to 28)),
    Album(id = "album22", title = "F-Zero X",                               artist = "Taro Bando",          coverImageUrl = "https://archive.org/services/img/f-zero-x-ost",            year = 1998, webPlaybackUrl = "https://archive.org/details/f-zero-x-ost",                  era = "N64",  vibe = "HYPE",      playCount = 74,  reactions = mapOf("🔥" to 38,  "❤️" to 22,  "🎮" to 41)),
    Album(id = "album23", title = "Kirby's Dream Land",                     artist = "Jun Ishikawa",        coverImageUrl = "https://archive.org/services/img/kirby-dream-land-ost",    year = 1992, webPlaybackUrl = "https://archive.org/details/kirby-dream-land-ost",          era = "GB",   vibe = "CHILL",     playCount = 118, reactions = mapOf("🔥" to 51,  "❤️" to 87,  "🎮" to 33)),
    Album(id = "album24", title = "Doom (1993)",                            artist = "Bobby Prince",        coverImageUrl = "https://archive.org/services/img/doom-ost-1993",           year = 1993, webPlaybackUrl = "https://archive.org/details/doom-ost-1993",                 era = "PC",   vibe = "HYPE",      playCount = 156, reactions = mapOf("🔥" to 82,  "❤️" to 48,  "🎮" to 77)),
    Album(id = "album25", title = "Crash Bandicoot 2",                      artist = "Mark Mothersbaugh",   coverImageUrl = "https://archive.org/services/img/crash-bandicoot-2-ost",   year = 1997, webPlaybackUrl = "https://archive.org/details/crash-bandicoot-2-ost",         era = "PS1",  vibe = "NOSTALGIC", playCount = 122, reactions = mapOf("🔥" to 58,  "❤️" to 84,  "🎮" to 39)),
    Album(id = "album26", title = "Diddy Kong Racing",                      artist = "David Wise",          coverImageUrl = "https://archive.org/services/img/diddy-kong-racing-ost",   year = 1997, webPlaybackUrl = "https://archive.org/details/diddy-kong-racing-ost",         era = "N64",  vibe = "HYPE",      playCount = 91,  reactions = mapOf("🔥" to 45,  "❤️" to 63,  "🎮" to 37)),
    Album(id = "album27", title = "Xenogears",                              artist = "Yasunori Mitsuda",    coverImageUrl = "https://archive.org/services/img/xenogears-ost",           year = 1998, webPlaybackUrl = "https://archive.org/details/xenogears-ost",                 era = "PS1",  vibe = "EPIC",      playCount = 173, reactions = mapOf("🔥" to 81,  "❤️" to 124, "🎮" to 58)),
    Album(id = "album28", title = "Pokémon Red & Blue",                     artist = "Junichi Masuda",      coverImageUrl = "https://archive.org/services/img/pokemon-red-blue-ost",   year = 1996, webPlaybackUrl = "https://archive.org/details/pokemon-red-blue-ost",          era = "GB",   vibe = "NOSTALGIC", playCount = 298, reactions = mapOf("🔥" to 142, "❤️" to 201, "🎮" to 88)),
    Album(id = "album29", title = "Star Fox 64",                            artist = "Koji Kondo",          coverImageUrl = "https://archive.org/services/img/star-fox-64-ost",         year = 1997, webPlaybackUrl = "https://archive.org/details/star-fox-64-ost",                era = "N64",  vibe = "EPIC",      playCount = 84,  reactions = mapOf("🔥" to 39,  "❤️" to 51,  "🎮" to 46)),
    Album(id = "album30", title = "Resident Evil 2",                        artist = "Masami Ueda",         coverImageUrl = "https://archive.org/services/img/resident-evil-2-ost",    year = 1998, webPlaybackUrl = "https://archive.org/details/resident-evil-2-ost",           era = "PS1",  vibe = "DARK",      playCount = 67,  reactions = mapOf("🔥" to 31,  "❤️" to 18,  "🎮" to 44))
)

val albumEraFilters = listOf("ALL", "NES", "SNES", "PS1", "PS2", "N64", "GCN", "GBA", "NDS", "PC", "OTHER")

fun getChillZoneGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11  -> "Good morning, gamer ☀️"
        in 12..16 -> "Afternoon session? 🎮"
        in 17..20 -> "Evening vibes loading... 🌆"
        in 21..23 -> "Late night session? 🌙"
        else      -> "Can't sleep? 👾 We've got you."
    }
}

fun getAlbumOfTheDay(): Album {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return sampleAlbums[dayOfYear % sampleAlbums.size]
}
// ─── Vinyl Record Composable ──────────────────────────────────────────────────

@Composable
fun VinylRecord(
    album: Album,
    size: androidx.compose.ui.unit.Dp = 180.dp,
    isSpinning: Boolean = false,
    isEjected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val eraColor = albumEraColor(album.era)
    val spinT = rememberInfiniteTransition(label = "vinylSpin_${album.id}")
    val spinAngle by spinT.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "vinylAngle_${album.id}"
    )
    val currentAngle = if (isSpinning) spinAngle else 0f

    val ejectOffset by animateFloatAsState(
        targetValue = if (isEjected) -(size.value * 0.28f) else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "vinylEject_${album.id}"
    )

    Box(
        modifier = Modifier
            .size(size)
            .offset(x = ejectOffset.dp)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer vinyl disc
        Canvas(modifier = Modifier.fillMaxSize().rotate(currentAngle)) {
            val cx = size.toPx() / 2f
            val cy = size.toPx() / 2f
            val outerR = size.toPx() / 2f

            // Main black vinyl
            drawCircle(color = ScrapbookDark, radius = outerR, center = Offset(cx, cy))

            // Groove rings — subtle concentric circles
            val grooveColor = Color.White.copy(alpha = 0.04f)
            for (i in 1..12) {
                val r = outerR * (0.45f + i * 0.043f)
                drawCircle(color = grooveColor, radius = r, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx()))
            }

            // Era-colored ring near label
            drawCircle(
                color = eraColor.copy(alpha = 0.5f),
                radius = outerR * 0.44f,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Label area background
            drawCircle(color = Color(0xFF2A1A08), radius = outerR * 0.38f, center = Offset(cx, cy))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(VinylAmber.copy(alpha = 0.3f), Color(0xFF1A0E04)),
                    center = Offset(cx, cy),
                    radius = outerR * 0.38f
                ),
                radius = outerR * 0.38f,
                center = Offset(cx, cy)
            )

            // Center hole
            drawCircle(color = Color(0xFF050302), radius = outerR * 0.06f, center = Offset(cx, cy))
        }

        // Cover art as label
        val labelSize = size * 0.36f
        Box(
            modifier = Modifier
                .size(labelSize)
                .rotate(currentAngle)
                .clip(CircleShape)
                .background(Color(0xFF1A0E04)),
            contentAlignment = Alignment.Center
        ) {
            when {
                album.coverImageResId != null -> Image(
                    painter = painterResource(id = album.coverImageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    alpha = 0.85f
                )
                album.coverImageUrl != null -> AsyncImage(
                    model = album.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize().clip(CircleShape),
                    alpha = 0.85f
                )
                else -> Text(
                    text = album.title.take(1),
                    fontFamily = BangersFontFamily,
                    color = VinylAmber,
                    fontSize = (size.value * 0.12f).sp
                )
            }
            // Label overlay — center hole
            Box(modifier = Modifier.size(labelSize * 0.18f).clip(CircleShape).background(Color(0xFF050302)))
        }

        // Shine overlay on vinyl
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.toPx() * 0.3f, size.toPx() * 0.2f),
                    radius = size.toPx() * 0.4f
                ),
                radius = size.toPx() / 2f,
                center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            )
        }
    }
}

// ─── Vinyl Crate Item ─────────────────────────────────────────────────────────

@Composable
fun VinylCrateItem(
    album: Album,
    isNowPlaying: Boolean = false,
    isBookmarked: Boolean = false,
    onPlay: () -> Unit,
    onBookmark: () -> Unit
) {
    val eraColor = albumEraColor(album.era)
    val neonT = rememberInfiniteTransition(label = "crateItem_${album.id}")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    var ejected by remember { mutableStateOf(isNowPlaying) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "crateScale_${album.id}"
    )

    // When isNowPlaying changes externally, sync eject state
    LaunchedEffect(isNowPlaying) { ejected = isNowPlaying }

    // Vinyl eject offset — slides out to the right from behind sleeve
    val vinylOffsetX by animateFloatAsState(
        targetValue = if (ejected) 90f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "vinylEject_${album.id}"
    )
    val vinylAlpha by animateFloatAsState(
        targetValue = if (ejected) 1f else 0.0f,
        animationSpec = tween(300),
        label = "vinylAlpha_${album.id}"
    )

    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        // Ambient glow when playing
        if (isNowPlaying) {
            Box(
                modifier = Modifier.matchParentSize().padding(6.dp)
                    .blur(18.dp)
                    .background(VinylAmber.copy(alpha = neonAlpha * 0.25f), RoundedCornerShape(16.dp))
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VinylCard)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Sleeve + Vinyl stack ──────────────────────────────────────
                Box(
                    modifier = Modifier.size(width = 120.dp, height = 90.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Vinyl record — slides out from behind sleeve on eject
                    Box(
                        modifier = Modifier
                            .offset(x = vinylOffsetX.dp)
                            .alpha(if (ejected) 1f else 0f)
                            .zIndex(0f)
                    ) {
                        VinylRecord(
                            album = album,
                            size = 90.dp,
                            isSpinning = isNowPlaying,
                            onClick = {}
                        )
                    }

                    // Sleeve — always on top, cover art visible
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .zIndex(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(VinylSurface)
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(8.dp))
                            .clickable {
                                pressed = true
                                if (!ejected) {
                                    ejected = true
                                    onPlay()
                                } else {
                                    onPlay()
                                }
                            }
                    ) {
                        // Cover art filling the sleeve
                        when {
                            album.coverImageResId != null -> Image(
                                painter = painterResource(id = album.coverImageResId),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            album.coverImageUrl != null -> AsyncImage(
                                model = album.coverImageUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize()
                            )
                            else -> Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.radialGradient(colors = listOf(eraColor.copy(alpha = 0.3f), VinylSurface))
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(album.title.take(1), fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 28.sp)
                            }
                        }

                        // Sleeve spine accent on left edge
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(eraColor.copy(alpha = 0.7f))
                        )

                        // Play overlay hint when not ejected
                        if (!ejected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(VinylAmber.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = VinylDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // "NOW PLAYING" badge when ejected
                        if (isNowPlaying) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VinylAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▶", fontFamily = BangersFontFamily, color = VinylDark, fontSize = 7.sp, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                            }
                        }
                    }
                }

                // ── Track info ────────────────────────────────────────────────
                Column(modifier = Modifier.weight(1f)) {
                    if (isNowPlaying) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val dotT = rememberInfiniteTransition(label = "playDot_${album.id}")
                            val dotA by dotT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "dotA_${album.id}")
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VinylAmber.copy(alpha = dotA)))
                            Text("NOW SPINNING", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    } else if (ejected) {
                        Text("QUEUED", fontFamily = BangersFontFamily, color = VinylAmber.copy(alpha = 0.5f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(album.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Text(album.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.5f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    album.year?.let { Text("$it", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.28f), fontSize = 10.sp) }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (album.era.isNotBlank() && album.era != "OTHER") {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(eraColor.copy(alpha = 0.15f)).border(1.dp, eraColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(album.era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 9.sp)
                            }
                        }
                        val vibeData = albumVibes.firstOrNull { it.label == album.vibe }
                        vibeData?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(it.color.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("${it.emoji} ${it.label}", fontFamily = BangersFontFamily, color = it.color.copy(alpha = 0.7f), fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    AlbumReactionBar(albumId = album.id, initialReactions = album.reactions)
                }

                // ── Action buttons ────────────────────────────────────────────
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(if (ejected) Brush.linearGradient(colors = listOf(VinylAmber, VinylOrange)) else Brush.linearGradient(colors = listOf(VinylSurface, VinylSurface)))
                            .border(1.dp, if (ejected) VinylAmberDim else VinylGroove, CircleShape)
                            .clickable {
                                if (!ejected) { ejected = true }
                                onPlay()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = if (ejected) VinylDark else VinylAmber, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape)
                            .background(if (isBookmarked) VinylAmber.copy(alpha = 0.2f) else VinylGroove)
                            .border(1.dp, if (isBookmarked) VinylAmber else VinylGroove, CircleShape)
                            .clickable { onBookmark() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isBookmarked) VinylAmber else VinylCream.copy(alpha = 0.35f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Vinyl Crate Grid Card (2-column grid version) ───────────────────────────

@Composable
fun VinylCrateGridCard(
    album: Album,
    isNowPlaying: Boolean = false,
    isBookmarked: Boolean = false,
    onPlay: () -> Unit,
    onBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eraColor = albumEraColor(album.era)
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "crateGridScale_${album.id}"
    )
    // Glow pulse + translate-on-press
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press_${album.id}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shOff_${album.id}")
    // Bookmark tap — subtle scale pulse
    var bookmarkPopped by remember { mutableStateOf(false) }
    val bookmarkScale by animateFloatAsState(
        targetValue = if (bookmarkPopped) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bookmarkPop_${album.id}"
    )
    LaunchedEffect(bookmarkPopped) {
        if (bookmarkPopped) { delay(300); bookmarkPopped = false }
    }

    Box(modifier = modifier.scale(cardScale).graphicsLayer { rotationZ = ((album.title.hashCode() % 5) - 2) * 0.5f }) {
        // Glow halo
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        // White glass card
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(
                    width = if (isNowPlaying) 2.dp else 1.5.dp,
                    color = if (isNowPlaying) VinylAmber else ScrapbookDark,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { pressed = true; onPlay() }
        ) {
            Column {
                // Cover art area
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(VinylSurface)
                ) {
                    when {
                        album.coverImageResId != null -> Image(
                            painter = painterResource(id = album.coverImageResId),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        album.coverImageUrl != null -> AsyncImage(
                            model = album.coverImageUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize()
                        )
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("🎵", fontSize = 32.sp)
                        }
                    }
                    // Scrim
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, VinylDark.copy(alpha = 0.6f)))))
                    // Play button
                    Box(
                        modifier = Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(VinylAmber, VinylOrange)))
                            .border(2.dp, VinylAmberDim, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = VinylDark, modifier = Modifier.size(24.dp))
                    }
                    // Era badge top-left
                    if (album.era.isNotBlank() && album.era != "OTHER") {
                        Box(
                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(eraColor.copy(alpha = 0.85f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(album.era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                        }
                    }
                    // Bookmark top-right with pop animation
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .scale(bookmarkScale)
                                .clip(CircleShape)
                                .background(if (isBookmarked) VinylAmber else Color.Black.copy(alpha = 0.35f))
                                .clickable {
                                    if (!isBookmarked) bookmarkPopped = true
                                    onBookmark()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isBookmarked) VinylDark else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                // Info
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(
                        album.title,
                        fontFamily = BangersFontFamily,
                        color = VinylCream,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        album.artist,
                        fontFamily = NunitoFontFamily,
                        color = VinylCream.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    album.year?.let {
                        Text(it.toString(), fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.25f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Now Playing Bar ──────────────────────────────────────────────────────────

@Composable
fun NowPlayingBar(
    state: NowPlayingState,
    tracks: List<AlbumTrack>,
    selectedTrack: AlbumTrack?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit
) {
    val neonT = rememberInfiniteTransition(label = "nowPlayingNeon")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "nowPlayingNeonAlpha"
    )

    // EQ animation — only when playing
    val eqT = rememberInfiniteTransition(label = "barEq")
    val eqHeights = (0..4).map { i ->
        eqT.animateFloat(
            initialValue = 3f,
            targetValue = (10 + i * 3).toFloat(),
            animationSpec = infiniteRepeatable(
                tween(280 + i * 70, easing = EaseInOut),
                RepeatMode.Reverse
            ),
            label = "barEq_$i"
        )
    }

    // Vinyl spin
    val vinylT = rememberInfiniteTransition(label = "barVinyl")
    val vinylAngle by vinylT.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (isPlaying) 2800 else 8000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "barVinylAngle"
    )

    // Progress through track list
    val currentIndex = tracks.indexOfFirst { it.id == selectedTrack?.id }
    val hasPrev = currentIndex > 0
    val hasNext = currentIndex < tracks.size - 1

    Box(modifier = Modifier.fillMaxWidth()) {
        // Scanline texture
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color.White.copy(alpha = 0.012f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += 3f
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(VinylSurface, VinylDeep)))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(0.dp))
        ) {
            // ── Track progress bar (thin amber line at top) ───────────────
            if (tracks.isNotEmpty() && currentIndex >= 0) {
                val progress = (currentIndex + 1).toFloat() / tracks.size.toFloat()
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(VinylGroove)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(VinylAmber, VinylOrange)
                                )
                            )
                    )
                }
            }

            // ── Main row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Spinning vinyl disc
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(ScrapbookDark)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    VinylAmber.copy(alpha = neonAlpha),
                                    VinylAmber.copy(alpha = 0.2f),
                                    VinylAmber.copy(alpha = neonAlpha)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).rotate(vinylAngle),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF1A1207)))
                        // Groove rings
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (i in 1..5) {
                                val r = size.width / 2f * (0.5f + i * 0.08f)
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.03f),
                                    radius = r,
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                                )
                            }
                        }
                        when {
                            state.coverResId != null -> Image(
                                painter = painterResource(id = state.coverResId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(26.dp).clip(CircleShape),
                                alpha = 0.9f
                            )
                            state.coverUrl != null -> AsyncImage(
                                model = state.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.halftoneReveal(state.coverUrl).size(26.dp).clip(CircleShape),
                                alpha = 0.9f
                            )
                        }
                    }
                    // Center hole
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF050302)))
                }

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    // Track name
                    Text(
                        selectedTrack?.title ?: state.title,
                        fontFamily = BangersFontFamily,
                        color = VinylCream,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.artist.isNotBlank()) {
                            Text(
                                state.artist,
                                fontFamily = NunitoFontFamily,
                                color = VinylCream.copy(alpha = 0.45f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (tracks.isNotEmpty() && currentIndex >= 0) {
                            Text(
                                "${currentIndex + 1}/${tracks.size}",
                                fontFamily = BangersFontFamily,
                                color = VinylAmber.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // ── Controls ──────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // EQ when playing
                    if (isPlaying) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(16.dp).padding(end = 4.dp)
                        ) {
                            eqHeights.forEachIndexed { i, heightState ->
                                val h by heightState
                                Box(
                                    modifier = Modifier.width(2.dp).height(h.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(VinylAmber.copy(alpha = 0.5f + i * 0.1f))
                                )
                            }
                        }
                    }

                    // Previous
                    Box(
                        modifier = Modifier.size(34.dp)
                            .clip(CircleShape)
                            .background(if (hasPrev) VinylSurface else Color.Transparent)
                            .border(
                                1.dp,
                                if (hasPrev) VinylGroove else Color.Transparent,
                                CircleShape
                            )
                            .clickable(enabled = hasPrev) { onPrevious() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (hasPrev) VinylCream.copy(alpha = 0.8f) else VinylCream.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Play / Pause — main CTA button
                    Box(
                        modifier = Modifier.size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(VinylAmber, VinylOrange)
                                )
                            )
                            .border(2.dp, VinylAmberDim, CircleShape)
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow behind button
                        Box(
                            modifier = Modifier.size(42.dp)
                                .blur(8.dp)
                                .background(VinylAmber.copy(alpha = neonAlpha * 0.4f), CircleShape)
                        )
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = VinylDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Next
                    Box(
                        modifier = Modifier.size(34.dp)
                            .clip(CircleShape)
                            .background(if (hasNext) VinylSurface else Color.Transparent)
                            .border(
                                1.dp,
                                if (hasNext) VinylGroove else Color.Transparent,
                                CircleShape
                            )
                            .clickable(enabled = hasNext) { onNext() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = if (hasNext) VinylCream.copy(alpha = 0.8f) else VinylCream.copy(alpha = 0.2f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Expand chevron
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = VinylCream.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp).padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

// ─── Era Timeline ─────────────────────────────────────────────────────────────

@Composable
fun EraTimeline(selectedEra: String, onEraSelected: (String) -> Unit) {
    val eras = listOf("NES", "SNES", "N64", "GBA", "GCN", "NDS", "PS1", "PS2", "PC")
    val neonT = rememberInfiniteTransition(label = "eraTimelineNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ERA TIMELINE", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 16.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedEra != "ALL") {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(albumEraColor(selectedEra).copy(alpha = 0.15f)).border(1.dp, albumEraColor(selectedEra).copy(alpha = 0.5f), RoundedCornerShape(6.dp)).clickable { onEraSelected("ALL") }.padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(selectedEra, fontFamily = BangersFontFamily, color = albumEraColor(selectedEra), fontSize = 11.sp)
                        Icon(Icons.Filled.Close, contentDescription = null, tint = albumEraColor(selectedEra), modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, VinylAmber.copy(alpha = 0.3f), VinylAmber.copy(alpha = 0.3f), Color.Transparent))).align(Alignment.Center))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                itemsIndexed(eras) { jumpIndex, era ->
                    Box(modifier = Modifier.jumpIn(jumpIndex)) {
                    val isSelected = selectedEra == era
                    val eraColor = albumEraColor(era)
                    var pressed by remember { mutableStateOf(false) }
                    val itemScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "eraScale")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp).scale(itemScale).clickable { pressed = true; onEraSelected(if (selectedEra == era) "ALL" else era) }.padding(vertical = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                GlowPulse(
                                    modifier = Modifier.matchParentSize(),
                                    glowColor = eraColor,
                                    cornerRadius = 22.dp,
                                    maxAlpha = 0.4f
                                )
                            }
                            Box(
                                modifier = Modifier.size(if (isSelected) 44.dp else 36.dp).clip(CircleShape)
                                    .background(if (isSelected) eraColor else VinylSurface)
                                    .border(width = if (isSelected) 2.dp else 1.dp, brush = if (isSelected) Brush.linearGradient(colors = listOf(eraColor.copy(alpha = neonAlpha), eraColor.copy(alpha = 0.4f), eraColor.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(VinylGroove, VinylGroove)), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(era, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else VinylCream.copy(alpha = 0.6f), fontSize = if (isSelected) 9.sp else 8.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isSelected) eraColor else VinylGroove))
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                                    }
                }
            }
        }
    }
}

// ─── Vibe Selector ────────────────────────────────────────────────────────────

@Composable
fun AlbumVibeSelector(selectedVibe: String, onVibeSelected: (String) -> Unit, neonAlpha: Float) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(albumVibes) { jumpIndex, vibe ->
            Box(modifier = Modifier.jumpIn(jumpIndex)) {
            val isSelected = selectedVibe == vibe.label
            var pressed by remember { mutableStateOf(false) }
            val chipScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "vibeScale")
            Box(modifier = Modifier.scale(chipScale)) {
                if (isSelected) {
                    GlowPulse(
                        modifier = Modifier.matchParentSize(),
                        glowColor = vibe.color,
                        cornerRadius = 20.dp,
                        maxAlpha = 0.4f
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) vibe.color.copy(alpha = 0.2f) else VinylSurface)
                        .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(20.dp))
                        .clickable { pressed = true; onVibeSelected(vibe.label) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(vibe.emoji, fontSize = 13.sp)
                        Text(vibe.label, fontFamily = BangersFontFamily, color = if (isSelected) vibe.color else VinylCream.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                    }
        }
    }
}

// ─── Album Reaction Bar ───────────────────────────────────────────────────────

@Composable
fun AlbumReactionBar(albumId: String, initialReactions: Map<String, Int> = emptyMap()) {
    val reactionEmojis = listOf("🔥", "❤️", "🎮")
    var reactions by remember { mutableStateOf(initialReactions.toMutableMap()) }
    var userReacted by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        reactionEmojis.forEach { emoji ->
            val count = reactions[emoji] ?: 0
            val isReacted = userReacted == emoji
            var popped by remember { mutableStateOf(false) }
            val popScale by animateFloatAsState(targetValue = if (popped) 1.35f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "pop_$emoji")
            Box(
                modifier = Modifier.scale(popScale).clip(RoundedCornerShape(16.dp))
                    .background(if (isReacted) VinylAmber.copy(alpha = 0.15f) else VinylGroove)
                    .border(1.dp, if (isReacted) VinylAmber.copy(alpha = 0.7f) else VinylGroove, RoundedCornerShape(16.dp))
                    .clickable {
                        popped = true
                        if (userReacted == emoji) { reactions = reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 1) - 1 }; userReacted = null }
                        else { userReacted?.let { prev -> reactions = reactions.toMutableMap().apply { this[prev] = (this[prev] ?: 1) - 1 } }; reactions = reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 0) + 1 }; userReacted = emoji }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(emoji, fontSize = 12.sp)
                    Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) VinylAmber else VinylCream.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            }
            LaunchedEffect(popped) { if (popped) { delay(200); popped = false } }
        }
    }
}

// ─── Weekly Chart ─────────────────────────────────────────────────────────────

data class ChartEntry(val album: Album, val position: Int, val previousPosition: Int?, val isNew: Boolean = false)

@Composable
fun WeeklyChart(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "chartNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val chartEntries = remember(albums) {
        val sorted = albums.sortedByDescending { it.playCount }
        sorted.mapIndexed { index, album ->
            ChartEntry(
                album = album,
                position = index + 1,
                previousPosition = if (index == 0) 3 else if (index == 1) 1 else if (index == 2) null else index + 2,
                isNew = index == 2
            )
        }.take(7)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Text("📊", fontSize = 18.sp)
            Text("THIS WEEK'S CHART", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(VinylAmber.copy(alpha = 0.15f)).border(1.dp, VinylAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("RETRO GAMING", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Based on plays & reactions this week", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        chartEntries.forEachIndexed { i, entry ->
            var pressed by remember { mutableStateOf(false) }
            val rowScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chartRow_$i")
            val eraColor = albumEraColor(entry.album.era)
            val medalColor = when (entry.position) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> VinylGroove }

            val movementText = when {
                entry.isNew -> "NEW"
                entry.previousPosition == null -> "—"
                entry.previousPosition > entry.position -> "▲${entry.previousPosition - entry.position}"
                entry.previousPosition < entry.position -> "▼${entry.position - entry.previousPosition}"
                else -> "="
            }
            val movementColor = when {
                entry.isNew -> VinylAmber
                entry.previousPosition != null && entry.previousPosition > entry.position -> CGreen
                entry.previousPosition != null && entry.previousPosition < entry.position -> CAcRed
                else -> VinylCream.copy(alpha = 0.3f)
            }

            Box(modifier = Modifier.fillMaxWidth().scale(rowScale).padding(vertical = 3.dp)) {
                // Subtle green hover shadow
                Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(10.dp)).background(CGreen.copy(alpha = 0.18f)))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (entry.position == 1) VinylAmber.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.44f))
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                        .clickable { pressed = true; onAlbumClick(entry.album) }
                        .padding(10.dp)
                ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Position number
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(medalColor.copy(alpha = 0.15f)).border(1.5.dp, medalColor.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("${entry.position}", fontFamily = BangersFontFamily, color = medalColor, fontSize = 14.sp)
                    }
                    // Cover
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(VinylSurface)) {
                        when {
                            entry.album.coverImageResId != null -> Image(painter = painterResource(id = entry.album.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            entry.album.coverImageUrl != null -> AsyncImage(model = entry.album.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(entry.album.coverImageUrl).fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("♪", color = VinylAmber, fontSize = 18.sp) }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.album.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(entry.album.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(eraColor.copy(alpha = 0.15f)).border(1.dp, eraColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text(entry.album.era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 8.sp)
                        }
                        Text(movementText, fontFamily = BangersFontFamily, color = movementColor, fontSize = 11.sp)
                    }
                }
                } // end inner Box
            } // end outer Box
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
        }
    }
}
// ─── Now Spinning Hero ────────────────────────────────────────────────────────
// Large vinyl as the page's identity piece — shown at top of page

@Composable
fun NowSpinningHero(nowPlaying: NowPlayingState?, albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "heroNeon")
    val neonAlpha by rememberGlowRange(0.3f, 1f)

    val displayAlbum = remember(nowPlaying, albums) {
        if (nowPlaying != null) albums.firstOrNull { it.id == nowPlaying.albumId } ?: albums.firstOrNull()
        else albums.firstOrNull()
    }

    if (displayAlbum == null) return

    Box(
        modifier = Modifier.fillMaxWidth().height(240.dp)
            .background(Brush.verticalGradient(colors = listOf(ComicGlassBg, Color.White.copy(alpha = 0.46f))))
    ) {
        // Subtle radial glow behind record
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(VinylAmber.copy(alpha = neonAlpha * 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.5f),
                    radius = size.height * 0.7f
                ),
                radius = size.height * 0.7f,
                center = Offset(size.width * 0.3f, size.height * 0.5f)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            VinylRecord(
                album = displayAlbum,
                size = 180.dp,
                isSpinning = nowPlaying != null,
                onClick = { onAlbumClick(displayAlbum) }
            )

            Column(modifier = Modifier.weight(1f)) {
                if (nowPlaying != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val dotT = rememberInfiniteTransition(label = "heroLive")
                        val dotA by dotT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "heroDotA")
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(VinylOrange.copy(alpha = dotA)))
                        Text("NOW SPINNING", fontFamily = BangersFontFamily, color = VinylOrange, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Text("UP NEXT", fontFamily = BangersFontFamily, color = VinylAmber.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(displayAlbum.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, lineHeight = 24.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(displayAlbum.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.55f), fontSize = 13.sp)
                displayAlbum.year?.let { Text("$it", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.3f), fontSize = 11.sp) }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (displayAlbum.era.isNotBlank() && displayAlbum.era != "OTHER") {
                        val ec = albumEraColor(displayAlbum.era)
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ec.copy(alpha = 0.15f)).border(1.dp, ec.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(displayAlbum.era, fontFamily = BangersFontFamily, color = ec, fontSize = 9.sp)
                        }
                    }
                    val vibeData = albumVibes.firstOrNull { it.label == displayAlbum.vibe }
                    vibeData?.let {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(it.color.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("${it.emoji} ${it.label}", fontFamily = BangersFontFamily, color = it.color.copy(alpha = 0.7f), fontSize = 9.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(colors = listOf(VinylAmber, VinylOrange)))
                        .clickable { onAlbumClick(displayAlbum) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = VinylDark, modifier = Modifier.size(18.dp))
                        Text(if (nowPlaying != null) "PLAYING" else "PLAY", fontFamily = BangersFontFamily, color = VinylDark, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Social listening stamps
                val stampNames = remember { listOf("Don", "Topín", "Fab", "Carol") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    stampNames.take(3).forEachIndexed { i, name ->
                        Box(
                            modifier = Modifier
                                .zIndex((3 - i).toFloat())
                                .size(26.dp).clip(CircleShape)
                                .background(listOf(CAcPurple, CGreenMint, CAcRed)[i % 3])
                                .border(2.dp, ComicGlassBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.take(1), fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text("${stampNames.size} friends listened this week", fontFamily = NunitoFontFamily,
                    color = VinylCream.copy(alpha = 0.5f), fontSize = 10.sp)
            }
        }
    }
}

// ─── Album of the Day Card ─────────────────────────────────────────────────────

@Composable
fun AlbumOfTheDayCard(album: Album, onPlay: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "aotdNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val crownScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.12f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "crownScale")
    val eraColor = albumEraColor(album.era)

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(14.dp).background(Color(0xFFFFD700).copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)))
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VinylCard)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700).copy(alpha = 0.12f), Color.Transparent, Color(0xFFFFD700).copy(alpha = 0.12f)))).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.scale(crownScale)) { Text("👑", fontSize = 22.sp) }
                    Column {
                        Text("ALBUM OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                        Text("Today's retro pick", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (album.era.isNotBlank() && album.era != "OTHER") {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraColor.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(album.era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) }
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                VinylRecord(album = album, size = 90.dp, isSpinning = false, onClick = onPlay)
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp)
                    Text(album.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.55f), fontSize = 12.sp)
                    album.year?.let { Text("$it", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.3f), fontSize = 11.sp) }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        album.reactions.entries.take(3).forEach { reactionEntry ->
                    val emoji = reactionEntry.key; val count = reactionEntry.value
                            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(VinylGroove).padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text(emoji, fontSize = 11.sp); Text("$count", fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.6f), fontSize = 10.sp) }
                        }
                    }
                }
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Brush.linearGradient(colors = listOf(VinylAmber, VinylOrange))).clickable { onPlay() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = VinylDark, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ─── Composer Spotlight ───────────────────────────────────────────────────────

data class ComposerSpotlight(
    val name: String,
    val emoji: String,
    val bio: String,
    val knownFor: List<String>,
    val era: String,
    val quote: String
)

val composerSpotlights = listOf(
    ComposerSpotlight(
        name = "Nobuo Uematsu",
        emoji = "🎹",
        bio = "Self-taught pianist who wrote the Final Fantasy series almost entirely alone. His orchestral pieces redefined what game music could be.",
        knownFor = listOf("Final Fantasy VII", "Final Fantasy VI", "FF X"),
        era = "PS1",
        quote = "\"I make music that I would want to listen to.\""
    ),
    ComposerSpotlight(
        name = "Koji Kondo",
        emoji = "🍄",
        bio = "Nintendo's legendary composer. The Super Mario theme is the most recognized piece of music on earth — written on a deadline in days.",
        knownFor = listOf("Super Mario Bros.", "The Legend of Zelda", "Mario 64"),
        era = "NES",
        quote = "\"Music should be something that makes you want to move.\""
    ),
    ComposerSpotlight(
        name = "Yuzo Koshiro",
        emoji = "🕹️",
        bio = "The genius behind Streets of Rage. Programmed his own music composition tool to push the Sega Genesis beyond its hardware limits.",
        knownFor = listOf("Streets of Rage", "ActRaiser", "Ys"),
        era = "SNES",
        quote = "\"The hardware limit is just a starting point.\""
    ),
    ComposerSpotlight(
        name = "Toby Fox",
        emoji = "🐶",
        bio = "Made the entire Undertale soundtrack solo in a bedroom. Megalovania became a cultural phenomenon without any marketing budget.",
        knownFor = listOf("Undertale", "Deltarune", "Homestuck"),
        era = "PC",
        quote = "\"If you keep trying, your dreams will come true.\""
    )
)

@Composable
fun ComposerSpotlightCard(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "composerNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val dayOfYear = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val composer = remember { composerSpotlights[dayOfYear % composerSpotlights.size] }
    val eraColor = albumEraColor(composer.era)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Text("🎼", fontSize = 18.sp)
            Text("COMPOSER SPOTLIGHT", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraColor.copy(alpha = 0.15f)).border(1.dp, eraColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(composer.era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 9.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(12.dp).background(eraColor.copy(alpha = neonAlpha * 0.1f), RoundedCornerShape(16.dp)))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VinylCard)
                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(Brush.radialGradient(colors = listOf(eraColor.copy(alpha = 0.3f), VinylSurface)))
                            .border(2.dp, eraColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(composer.emoji, fontSize = 32.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(composer.name, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp)
                        Text("TODAY'S FEATURED COMPOSER", fontFamily = NunitoFontFamily, color = VinylAmber.copy(alpha = 0.7f), fontSize = 10.sp, letterSpacing = 0.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(composer.bio, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Quote card — torn paper feel
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VinylSurface)
                        .border(1.dp, VinylAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("❝", fontFamily = BangersFontFamily, color = VinylAmber.copy(alpha = 0.5f), fontSize = 24.sp)
                        Text(composer.quote, fontFamily = NunitoFontFamily, fontStyle = FontStyle.Italic, color = VinylCream.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("KNOWN FOR", fontFamily = BangersFontFamily, color = VinylAmber.copy(alpha = 0.6f), fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    composer.knownFor.forEach { title ->
                        val matchingAlbum = albums.firstOrNull { it.title.contains(title, ignoreCase = true) }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(VinylSurface)
                                .border(1.dp, eraColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { matchingAlbum?.let { onAlbumClick(it) } }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(title, fontFamily = BangersFontFamily, color = if (matchingAlbum != null) eraColor else VinylCream.copy(alpha = 0.4f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ─── Memory Jukebox ───────────────────────────────────────────────────────────

@Composable
fun MemoryJukebox(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    var currentAlbum by remember { mutableStateOf<Album?>(null) }

    val neonT = rememberInfiniteTransition(label = "jukeboxNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val coinScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "coinScale")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Text("🎰", fontSize = 18.sp)
            Text("MEMORY JUKEBOX", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Drop a coin and discover a random retro track", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(12.dp).background(VinylOrange.copy(alpha = neonAlpha * 0.08f), RoundedCornerShape(16.dp)))
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VinylCard)
                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!revealed || currentAlbum == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(VinylAmber.copy(alpha = 0.1f)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(VinylAmber.copy(alpha = neonAlpha), VinylOrange.copy(alpha = 0.3f), VinylAmber.copy(alpha = neonAlpha))), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text("🪙", fontSize = 36.sp, modifier = Modifier.scale(coinScale))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("INSERT COIN", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 22.sp, letterSpacing = 2.sp)
                    Text("Tap to reveal a random retro track", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(colors = listOf(VinylAmber, VinylOrange)))
                            .clickable { currentAlbum = albums.random(); revealed = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🪙", fontSize = 18.sp)
                            Text("DROP COIN", fontFamily = BangersFontFamily, color = VinylDark, fontSize = 20.sp, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    val album = currentAlbum!!
                    val eraColor = albumEraColor(album.era)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        VinylRecord(album = album, size = 100.dp, isSpinning = true, onClick = { onAlbumClick(album) })
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(VinylAmber).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("🎰 YOUR TRACK", fontFamily = BangersFontFamily, color = VinylDark, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(album.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                            Text(album.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.5f), fontSize = 11.sp)
                            album.year?.let { Text("$it", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.3f), fontSize = 10.sp) }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (album.era.isNotBlank() && album.era != "OTHER") {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(eraColor.copy(alpha = 0.15f)).border(1.dp, eraColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(album.era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RetroGlassButton(
                            text = "PLAY NOW",
                            onClick = { onAlbumClick(album) },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp)) }
                        )
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(VinylSurface).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp)).clickable { revealed = false; currentAlbum = null }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("🪙", fontSize = 13.sp); Text("TRY AGAIN", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 15.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Community Listening Room ─────────────────────────────────────────────────

@Composable
fun CommunityListeningRoom(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "clrNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val sampleListeners = remember {
        listOf(
            CommunityListener(uid = "u1", username = "Don Carlos", albumTitle = "Final Fantasy VII", albumId = "album6", timeAgo = "2m ago"),
            CommunityListener(uid = "u2", username = "Topin99", albumTitle = "Undertale", albumId = "album4", timeAgo = "5m ago"),
            CommunityListener(uid = "u3", username = "HomicidalYellio", albumTitle = "Pokemon Diamond & Pearl", albumId = "album2", timeAgo = "11m ago"),
            CommunityListener(uid = "u4", username = "Fabriko98", albumTitle = "The Sims", albumId = "album7", timeAgo = "18m ago"),
            CommunityListener(uid = "u5", username = "Carollerm", albumTitle = "Wind Waker", albumId = "album3", timeAgo = "25m ago")
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎧", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text("LISTENING NOW", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val liveDotT = rememberInfiniteTransition(label = "liveDot")
                val liveDotA by liveDotT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "liveDotAlpha")
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CAcRed.copy(alpha = liveDotA)))
                Text("LIVE", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("See what the community is vibing to right now", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        sampleListeners.forEach { listener ->
            val album = albums.firstOrNull { it.id == listener.albumId }
            var pressed by remember { mutableStateOf(false) }
            val rowScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "listenerRow_${listener.uid}")

            Box(
                modifier = Modifier.fillMaxWidth().scale(rowScale).padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VinylCard)
                    .border(1.dp, VinylGroove, RoundedCornerShape(12.dp))
                    .clickable { pressed = true; album?.let { onAlbumClick(it) } }
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(VinylSurface)
                            .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(VinylAmber.copy(alpha = neonAlpha * 0.6f), VinylAmber.copy(alpha = 0.15f), VinylAmber.copy(alpha = neonAlpha * 0.6f))), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(listener.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(listener.username, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎵", fontSize = 10.sp)
                            Text(listener.albumTitle, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.5f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(listener.timeAgo, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.35f), fontSize = 10.sp)
                        if (album != null) {
                            val ec = albumEraColor(album.era)
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ec.copy(alpha = 0.12f)).border(1.dp, ec.copy(alpha = 0.35f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text(album.era, fontFamily = BangersFontFamily, color = ec, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
        }
    }
}

// ─── Genre Radar ──────────────────────────────────────────────────────────────

@Composable
fun AlbumGenreRadar(albums: List<Album>) {
    val neonT = rememberInfiniteTransition(label = "radarNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val vibeScores = remember(albums) {
        val scores = mutableMapOf<String, Float>()
        albumVibes.filter { it.label != "ALL" }.forEach { vibe -> scores[vibe.label] = 0f }
        albums.forEach { album ->
            val totalReactions = album.reactions.values.sum().toFloat().coerceAtLeast(1f)
            val boost = album.reactions["🔥"]?.toFloat() ?: 0f
            scores[album.vibe] = (scores[album.vibe] ?: 0f) + totalReactions + boost * 0.5f
        }
        val maxScore = scores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        scores.mapValues { (_, v) -> (v / maxScore).coerceIn(0.1f, 1f) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Text("🎯", fontSize = 16.sp)
            Text("COMMUNITY VIBE", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("What the community is feeling this week", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(VinylCard)
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            val dominantVibe = vibeScores.maxByOrNull { it.value }?.key ?: "CHILL"
            val dominantVibeData = albumVibes.firstOrNull { it.label == dominantVibe }

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(dominantVibeData?.color?.copy(alpha = 0.12f) ?: VinylAmber.copy(alpha = 0.08f))
                    .border(1.dp, dominantVibeData?.color?.copy(alpha = 0.3f) ?: VinylAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dominantVibeData?.emoji ?: "🎵", fontSize = 22.sp)
                    Column {
                        Text("THIS WEEK'S DOMINANT VIBE", fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(dominantVibe, fontFamily = BangersFontFamily, color = dominantVibeData?.color ?: VinylAmber, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            albumVibes.filter { it.label != "ALL" }.forEach { vibe ->
                val score = vibeScores[vibe.label] ?: 0f
                val animatedScore by animateFloatAsState(targetValue = score, animationSpec = tween(1000, delayMillis = albumVibes.indexOf(vibe) * 100, easing = LinearOutSlowInEasing), label = "vibeBar_${vibe.label}")
                val percentage = (score * 100).toInt()

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(vibe.emoji, fontSize = 16.sp)
                    Text(vibe.label, fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.width(80.dp))
                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(VinylSurface)) {
                        Box(modifier = Modifier.fillMaxWidth(animatedScore).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(colors = listOf(vibe.color.copy(alpha = 0.5f), vibe.color))))
                    }
                    Text("$percentage%", fontFamily = BangersFontFamily, color = vibe.color, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

// ─── Section Divider ─────────────────────────────────────────────────────────

@Composable
fun VinylSectionDivider() {
    Canvas(modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 16.dp)) {
        val w = size.width
        val midY = size.height / 2f
        val waveAmplitude = 4.dp.toPx()
        val segments = 40
        val segmentWidth = w / segments
        var x = 0f
        var prevY = midY
        while (x < w) {
            val progress = x / w
            val y = midY + waveAmplitude * kotlin.math.sin(progress * 12f * kotlin.math.PI.toFloat())
            drawLine(color = VinylAmber.copy(alpha = 0.15f), start = Offset(x, prevY), end = Offset(x + segmentWidth, y), strokeWidth = 1.5.dp.toPx())
            prevY = y
            x += segmentWidth
        }
    }
}

// ─── Archive Album Item ───────────────────────────────────────────────────────

@Composable
fun ArchiveAlbumItem(item: ArchiveItem, onClick: () -> Unit, favoritesViewModel: FavoritesViewModel? = null, modifier: Modifier = Modifier) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(item.id)
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "archiveScale")

    Box(modifier = modifier.scale(cardScale)) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(VinylCard)
                .border(1.dp, VinylGroove, RoundedCornerShape(14.dp))
                .clickable { pressed = true; onClick() }
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(VinylSurface)) {
                    AsyncImage(model = item.thumbnailUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(item.thumbnailUrl).fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, VinylDark.copy(alpha = 0.7f)))))
                    Box(modifier = Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(Brush.linearGradient(colors = listOf(VinylAmber, VinylOrange))).border(2.dp, VinylAmberDim, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = VinylDark, modifier = Modifier.size(24.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(VinylAmber.copy(alpha = 0.15f)).border(1.dp, VinylAmber.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("ARCHIVE", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 9.sp)
                    }
                    if (favoritesViewModel != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(VinylAmber).border(2.dp, VinylAmberDim, CircleShape)) {
                            IconButton(onClick = { favoritesViewModel.toggleFavorite(item.toFavoriteItem()) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = VinylDark, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(item.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                    item.creator?.let { Text(it, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.45f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    item.year?.let { Text(it, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.25f), fontSize = 10.sp) }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

// ─── Internet Archive Track Fetcher ──────────────────────────────────────────

suspend fun fetchTracksForAlbum(webPlaybackUrl: String): List<AlbumTrack> {
    return try {
        val identifier = webPlaybackUrl
            .removePrefix("https://archive.org/details/")
            .removePrefix("http://archive.org/details/")
            .split("/").first()
            .split("?").first()
            .trim()

        if (identifier.isBlank()) return emptyList()

        val metaUrl = "https://archive.org/metadata/$identifier"
        val connection = java.net.URL(metaUrl).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "RetroHub/1.0")
        connection.connect()

        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val root = org.json.JSONObject(response)
        val files = root.optJSONArray("files") ?: return emptyList()

        val audioExtensions = listOf(".mp3", ".ogg", ".flac", ".wav", ".m4a", ".opus")
        val tracks = mutableListOf<AlbumTrack>()
        var trackNum = 1

        for (i in 0 until files.length()) {
            val file = files.optJSONObject(i) ?: continue
            val name = file.optString("name", "")
            if (name.isBlank()) continue
            if (audioExtensions.none { name.lowercase().endsWith(it) }) continue
            // Skip derivative/sample files
            if (name.contains("_sample") || name.contains("_64kb") || name.contains("_128kb") || name.contains(".thumbs/")) continue

            val rawTitle = file.optString("title", "").ifBlank {
                // Fall back to filename without extension
                name.substringAfterLast("/").substringBeforeLast(".")
                    .replace("_", " ").replace("-", " ").trim()
            }

            val rawLength = file.optString("length", "")
            val duration = rawLength.toFloatOrNull()?.let { secs ->
                val m = secs.toInt() / 60
                val s = secs.toInt() % 60
                "$m:${s.toString().padStart(2, '0')}"
            } ?: rawLength.take(7).ifBlank { "" }

            tracks.add(
                AlbumTrack(
                    id = "${identifier}_$i",
                    title = rawTitle.take(80),
                    filename = name,
                    duration = duration,
                    trackNumber = trackNum,
                    playUrl = "https://archive.org/download/$identifier/${java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")}"
                )
            )
            trackNum++
        }

        // Sort by track number if available, then by name
        tracks.sortWith(compareBy(
            { file ->
                val t = root.optJSONArray("files")
                    ?.let { f -> (0 until f.length()).mapNotNull { f.optJSONObject(it) }.firstOrNull { it.optString("name") == file.filename } }
                t?.optString("track", "99")?.toIntOrNull() ?: 99
            },
            { it.filename }
        ))

        tracks.take(50)
    } catch (e: Exception) {
        android.util.Log.e("AlbumTrack", "Failed to fetch tracks: ${e.message}", e)
        emptyList()
    }
}

// ─── Album Reviews Panel ─────────────────────────────────────────────────────

data class AlbumReview(
    val username: String,
    val avatar: String,
    val rating: Int,
    val text: String,
    val timeAgo: String,
    val avatarColor: Color
)

@Composable
fun AlbumReviewsPanel() {
    val sampleReviews = remember {
        listOf(
            AlbumReview("Don Carlos",       "D", 5, "Absolute banger. This soundtrack still gives me chills every time. A certified retro masterpiece.", "2h ago",  CAcPurple),
            AlbumReview("Topín99",          "T", 4, "Perfect for late night sessions. Melancholy but hopeful — underrated gem of its era.", "5h ago",  CGreenMint),
            AlbumReview("HomicidalYellio",  "H", 5, "This is what childhood sounds like. I cry every time the title screen hits.", "1d ago",  CAcRed),
            AlbumReview("Fabriko98",        "F", 3, "Good but the second half drags. Still top tier for the era though.", "2d ago",  CGreen),
            AlbumReview("Carollerm",        "C", 5, "No notes. Play it loud. Timeless.", "3d ago",  CAcRed)
        )
    }
    var userRating by remember { mutableStateOf(0) }
    var userReviewText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Community rating summary
        item {
            val avgRating = sampleReviews.map { it.rating }.average()
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(1.dp, VinylGroove, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format("%.1f", avgRating), fontFamily = BangersFontFamily, color = VinylCream, fontSize = 34.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (1..5).forEach { i -> Text(if (i <= avgRating.toInt()) "★" else "☆", color = VinylAmber, fontSize = 14.sp) }
                        }
                        Text("${sampleReviews.size} reviews", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 10.sp)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        (5 downTo 1).forEach { star ->
                            val count = sampleReviews.count { it.rating == star }
                            val ratio = count.toFloat() / sampleReviews.size.toFloat()
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("$star★", fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.45f), fontSize = 10.sp, modifier = Modifier.width(22.dp))
                                Box(modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(VinylGroove)) {
                                    val animR by animateFloatAsState(targetValue = ratio, animationSpec = tween(600, delayMillis = (5 - star) * 80, easing = LinearOutSlowInEasing), label = "rBar_$star")
                                    Box(modifier = Modifier.fillMaxWidth(animR).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(VinylAmber))
                                }
                                Text("$count", fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 10.sp, modifier = Modifier.width(14.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
        }

        // Drop your take
        item {
            if (!submitted) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(1.dp, VinylGroove, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✍️ DROP YOUR TAKE", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 14.sp, letterSpacing = 0.5.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { i ->
                            Text(
                                if (i <= userRating) "★" else "☆",
                                color = if (i <= userRating) VinylAmber else VinylCream.copy(alpha = 0.2f),
                                fontSize = 26.sp,
                                modifier = Modifier.clickable { userRating = i }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = userReviewText,
                        onValueChange = { userReviewText = it },
                        placeholder = { Text("What did you think?", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.35f), fontSize = 12.sp) },
                        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = VinylCream),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VinylAmber,
                            unfocusedBorderColor = VinylGroove,
                            focusedContainerColor = ComicGlassBg,
                            unfocusedContainerColor = ComicGlassBg,
                            cursorColor = VinylAmber
                        ),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2, maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val canPost = userRating > 0 && userReviewText.isNotBlank()
                    if (canPost) {
                        RetroGlassButton(
                            text = "POST REVIEW",
                            onClick = { submitted = true; focusManager.clearFocus() },
                            modifier = Modifier.fillMaxWidth(),
                            burstText = "BAM!"
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(VinylGroove, VinylGroove)))
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("POST REVIEW", fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.35f), fontSize = 14.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VinylAmber.copy(alpha = 0.12f))
                        .border(1.dp, VinylAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✅  Review posted! Thanks for sharing.", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }

        // Community reviews
        items(sampleReviews, key = { it.username }) { review ->
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(1.dp, VinylGroove, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(review.avatarColor)
                                .border(2.dp, VinylGroove, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text(review.avatar, fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(review.username, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                                (1..5).forEach { i -> Text(if (i <= review.rating) "★" else "☆", color = VinylAmber, fontSize = 11.sp) }
                            }
                        }
                        Text(review.timeAgo, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.35f), fontSize = 10.sp)
                    }
                    Text(review.text, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

// ─── Album Player Sheet ───────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AlbumPlayerSheet(
    album: Album? = null,
    archiveItem: ArchiveItem? = null,
    onClose: () -> Unit,
    onNowPlayingUpdate: (NowPlayingState) -> Unit = {},
    onTracksLoaded: (List<AlbumTrack>) -> Unit = {},          // ← new
    onTrackSelected: (AlbumTrack) -> Unit = {},               // ← new
    onWebViewReady: (WebView) -> Unit = {},                   // ← new
    playbackActive: Boolean = true                            // global play/pause state (NowPlayingViewModel.isPlaying)
) {
    val context = LocalContext.current
    val title = album?.title ?: archiveItem?.title ?: ""
    val artist = album?.artist ?: archiveItem?.creator ?: ""
    val era = album?.era ?: "OTHER"
    val eraColor = albumEraColor(era)
    val webPlaybackUrl = album?.webPlaybackUrl ?: archiveItem?.webUrl ?: ""

    var tracks by remember { mutableStateOf<List<AlbumTrack>>(emptyList()) }
    var isLoadingTracks by remember { mutableStateOf(true) }
    var selectedTrack by remember { mutableStateOf<AlbumTrack?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var fetchError by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Tracklist, 1 = Reviews

    val neonT = rememberInfiniteTransition(label = "sheetNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    LaunchedEffect(webPlaybackUrl) {
        if (webPlaybackUrl.isNotBlank()) {
            isLoadingTracks = true
            fetchError = false
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                fetchTracksForAlbum(webPlaybackUrl)
            }
            tracks = result
            if (result.isNotEmpty()) {
                onTracksLoaded(result)          // ← expose to AlbumsScreen
            }
            isLoadingTracks = false
        } else {
            isLoadingTracks = false
            fetchError = true
        }
    }

    LaunchedEffect(selectedTrack) {
        selectedTrack?.let {
            onNowPlayingUpdate(
                NowPlayingState(
                    title = it.title,
                    artist = artist,
                    coverResId = album?.coverImageResId,
                    coverUrl = album?.coverImageUrl ?: archiveItem?.thumbnailUrl,
                    isPlaying = true,
                    albumId = album?.id ?: ""
                )
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClose() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(VinylDeep)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VinylGroove)
            )

            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(VinylAmber, VinylOrange, VinylAmber)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (album != null) {
                        VinylRecord(
                            album = album,
                            size = 56.dp,
                            isSpinning = selectedTrack != null,
                            onClick = {}
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape)
                                .background(VinylSurface)
                                .border(2.dp, VinylAmber.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Text("♪", color = VinylAmber, fontSize = 22.sp) }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontFamily = BangersFontFamily, color = VinylDark, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (artist.isNotBlank()) {
                            Text(artist, fontFamily = NunitoFontFamily, color = VinylDark.copy(alpha = 0.65f), fontSize = 12.sp)
                        }
                        if (era.isNotBlank() && era != "OTHER") {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(VinylDark.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(era, fontFamily = BangersFontFamily, color = VinylDark, fontSize = 9.sp)
                            }
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = VinylDark, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Vinyl slides out of its sleeve
            val revealAlbum = album ?: archiveItem?.let {
                Album(
                    id = it.id,
                    title = it.title,
                    artist = it.creator ?: "",
                    coverImageUrl = it.thumbnailUrl
                )
            }
            if (revealAlbum != null) {
                VinylSleeveReveal(
                    album = revealAlbum,
                    isPlaying = playbackActive && selectedTrack != null
                )
            }

            // Now Playing strip
            AnimatedVisibility(
                visible = selectedTrack != null,
                enter = expandVertically(tween(300)),
                exit = shrinkVertically(tween(200))
            ) {
                val currentTrack = selectedTrack
                if (currentTrack != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(VinylSurface)
                            .border(BorderStroke(1.dp, VinylAmber.copy(alpha = neonAlpha * 0.4f)))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val eqT = rememberInfiniteTransition(label = "sheetEq")
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(18.dp)
                            ) {
                                (0..4).forEach { i ->
                                    val h by eqT.animateFloat(
                                        initialValue = 4f,
                                        targetValue = (10 + i * 3).toFloat(),
                                        animationSpec = infiniteRepeatable(
                                            tween(280 + i * 70, easing = EaseInOut),
                                            RepeatMode.Reverse
                                        ),
                                        label = "sheetEq_$i"
                                    )
                                    Box(
                                        modifier = Modifier.width(3.dp).height(h.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(VinylAmber.copy(alpha = 0.7f + i * 0.06f))
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("NOW PLAYING", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 9.sp, letterSpacing = 1.sp)
                                Text(currentTrack.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                // Mini waveform
                                val waveT = rememberInfiniteTransition(label = "waveform")
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.height(14.dp)
                                ) {
                                    (0..14).forEach { i ->
                                        val h by waveT.animateFloat(
                                            initialValue = (3 + (i * 7) % 10).toFloat(),
                                            targetValue = (8 + (i * 5) % 6).toFloat(),
                                            animationSpec = infiniteRepeatable(
                                                tween(200 + i * 30, easing = EaseInOut),
                                                RepeatMode.Reverse
                                            ),
                                            label = "w_$i"
                                        )
                                        Box(
                                            modifier = Modifier.width(3.dp).height(h.dp)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(VinylAmber.copy(alpha = 0.35f + (i % 3) * 0.1f))
                                        )
                                    }
                                }
                            }

                            if (currentTrack.duration.isNotBlank()) {
                                Text(currentTrack.duration, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Tab row — Tracklist / Reviews
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(VinylSurface)
                    .border(BorderStroke(1.dp, VinylGroove))
            ) {
                listOf("🎵  TRACKLIST", "⭐  REVIEWS").forEachIndexed { i, label ->
                    val selected = activeTab == i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = i }
                            .padding(vertical = 11.dp)
                            .drawBehind {
                                if (selected) drawLine(
                                    color = CGreen,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontFamily = BangersFontFamily,
                            color = if (selected) VinylCream else VinylCream.copy(alpha = 0.35f),
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Track list body
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (activeTab == 1) {
                    AlbumReviewsPanel()
                } else if (isLoadingTracks) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ThreeDotsAnimation(color = VinylAmber)
                            Text(
                                "Loading tracklist...",
                                fontFamily = NunitoFontFamily,
                                color = VinylCream.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (tracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("📀", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No tracks found", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "This archive entry may not have audio files",
                                fontFamily = NunitoFontFamily,
                                color = VinylCream.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(VinylAmber, VinylOrange)
                                        )
                                    )
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webPlaybackUrl))
                                        try { context.startActivity(intent) } catch (e: Exception) { }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = VinylDark, modifier = Modifier.size(16.dp))
                                    Text("OPEN IN BROWSER", fontFamily = BangersFontFamily, color = VinylDark, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${tracks.size} TRACKS",
                                    fontFamily = BangersFontFamily,
                                    color = VinylAmber.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "Tap to play",
                                    fontFamily = NunitoFontFamily,
                                    color = VinylCream.copy(alpha = 0.3f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
                            val isPlaying = selectedTrack?.id == track.id
                            var pressed by remember { mutableStateOf(false) }
                            val rowScale by animateFloatAsState(
                                targetValue = if (pressed) 0.97f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "trackRow_$index"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(rowScale)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isPlaying -> VinylAmber.copy(alpha = 0.12f)
                                            index % 2 == 0 -> VinylCard
                                            else -> VinylSurface.copy(alpha = 0.5f)
                                        }
                                    )
                                    .border(
                                        width = if (isPlaying) 1.dp else 0.dp,
                                        color = if (isPlaying) VinylAmber.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        pressed = true
                                        selectedTrack = track
                                        webViewRef?.loadUrl(track.playUrl)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.widthIn(min = 28.dp).height(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isPlaying) {
                                            val eqT2 = rememberInfiniteTransition(label = "trackEq_$index")
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                            MiniSpinningVinyl(size = 18.dp, spinning = playbackActive)
                                            Row(
                                                verticalAlignment = Alignment.Bottom,
                                                horizontalArrangement = Arrangement.spacedBy(1.dp),
                                                modifier = Modifier.height(16.dp)
                                            ) {
                                                (0..2).forEach { i ->
                                                    val h by eqT2.animateFloat(
                                                        initialValue = 3f,
                                                        targetValue = if (playbackActive) (8 + i * 3).toFloat() else 3.5f,
                                                        animationSpec = infiniteRepeatable(
                                                            tween(250 + i * 60, easing = EaseInOut),
                                                            RepeatMode.Reverse
                                                        ),
                                                        label = "tEq_${index}_$i"
                                                    )
                                                    Box(
                                                        modifier = Modifier.width(3.dp).height(h.dp)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(VinylAmber)
                                                    )
                                                }
                                            }
                                            } // end vinyl + equalizer Row
                                        } else {
                                            Text(
                                                "${track.trackNumber}",
                                                fontFamily = BangersFontFamily,
                                                color = VinylCream.copy(alpha = 0.3f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Text(
                                        track.title,
                                        fontFamily = if (isPlaying) BangersFontFamily else NunitoFontFamily,
                                        fontWeight = if (isPlaying) null else FontWeight.Medium,
                                        color = if (isPlaying) VinylAmber else VinylCream.copy(alpha = 0.8f),
                                        fontSize = if (isPlaying) 14.sp else 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (track.duration.isNotBlank()) {
                                        Text(
                                            track.duration,
                                            fontFamily = NunitoFontFamily,
                                            color = VinylCream.copy(alpha = 0.3f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                        }
                    }
                }
            }

            // Embedded WebView player strip
            AnimatedVisibility(
                visible = selectedTrack != null,
                enter = expandVertically(tween(300)),
                exit = shrinkVertically(tween(200))
            ) {
                val currentTrack = selectedTrack
                if (currentTrack != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(BorderStroke(2.dp, ScrapbookDark))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎵", fontSize = 12.sp)
                                Text(
                                    "INTERNET ARCHIVE PLAYER",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentTrack.playUrl))
                                try { context.startActivity(intent) } catch (e: Exception) { }
                            }) {
                                Text(
                                    "OPEN IN BROWSER",
                                    fontFamily = BangersFontFamily,
                                    color = CGreenDeep,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                    }
                                    webViewClient = WebViewClient()
                                    webChromeClient = WebChromeClient()
                                    loadUrl(currentTrack.playUrl)
                                    webViewRef = this
                                    onWebViewReady(this)
                                }
                            },
                            update = { view ->
                                if (view.url != currentTrack.playUrl) {
                                    view.loadUrl(currentTrack.playUrl)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Era Cartridge Badge ─────────────────────────────────────────────────────

@Composable
fun EraCartridgeBadge(era: String, modifier: Modifier = Modifier) {
    if (era.isBlank() || era == "OTHER") return
    val eraColor = albumEraColor(era)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.horizontalGradient(listOf(eraColor.copy(alpha = 0.18f), eraColor.copy(alpha = 0.08f))))
            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            // tiny cartridge notch accent
            Box(modifier = Modifier.size(width = 3.dp, height = 8.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(eraColor.copy(alpha = 0.6f)))
            Text(era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 10.sp, letterSpacing = 0.5.sp)
        }
    }
}

// ─── Crate Dig Strip ──────────────────────────────────────────────────────────

@Composable
fun CrateDigStrip(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val featured = remember(albums) { albums.sortedByDescending { it.playCount }.take(6) }
    val neonT = rememberInfiniteTransition(label = "crateDigNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Text("🎧", fontSize = 18.sp)
            Text("CRATE DIG", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                .background(VinylAmber.copy(alpha = 0.15f))
                .border(1.dp, VinylAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("THIS WEEK", fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 9.sp)
            }
        }
        Text("Top picks hand-curated from the community", fontFamily = NunitoFontFamily,
            color = VinylCream.copy(alpha = 0.45f), fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(featured, key = { _, it -> it.id }) { index, album ->
                val eraColor = albumEraColor(album.era)
                var pressed by remember { mutableStateOf(false) }
                val tiltX by animateFloatAsState(targetValue = if (pressed) -8f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tiltX_${album.id}")
                val tiltY by animateFloatAsState(targetValue = if (pressed) 6f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tiltY_${album.id}")
                val cardScale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cdScale_${album.id}")

                SpringEntrance(delayMs = index * 70) {
                    Box(modifier = Modifier.width(140.dp)) {
                        // Comic shadow
                        Box(
                            modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                                .clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f))
                        )
                        // Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationX = tiltX
                                    rotationY = tiltY
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    cameraDistance = 8f * density
                                }
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                                .pointerInput(album.id) {
                                    detectTapGestures(
                                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                                        onTap = { onAlbumClick(album) }
                                    )
                                }
                        ) {
                            Column {
                                // Cover art
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp)
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                                    .background(VinylSurface)) {
                                    when {
                                        album.coverImageResId != null -> Image(
                                            painter = painterResource(id = album.coverImageResId),
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        album.coverImageUrl != null -> AsyncImage(
                                            model = album.coverImageUrl,
                                            contentDescription = album.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize()
                                        )
                                        else -> Box(modifier = Modifier.fillMaxSize().background(
                                            Brush.radialGradient(listOf(eraColor.copy(0.3f), VinylSurface))),
                                            contentAlignment = Alignment.Center) {
                                            Text(album.title.take(1), fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 40.sp)
                                        }
                                    }
                                    // Gloss shine overlay
                                    Box(modifier = Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), endY = 60f)))
                                    // Comic shimmer layer
                                    ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp)
                                    // Play overlay
                                    Box(modifier = Modifier.align(Alignment.Center).size(36.dp).clip(CircleShape)
                                        .background(VinylAmber.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = VinylDark, modifier = Modifier.size(20.dp))
                                    }
                                    // Era badge top-left
                                    EraCartridgeBadge(album.era, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
                                }
                                // Info
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(album.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 13.sp,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                                    Text(album.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.5f),
                                        fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("▶ ${album.playCount}", fontFamily = BangersFontFamily, color = VinylAmber.copy(alpha = 0.8f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── OST Mood Radio ───────────────────────────────────────────────────────────

@Composable
fun OSTMoodRadio(selectedVibe: String, albums: List<Album>, onPlay: (Album) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "moodRadioNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val pulseScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "mrPulse")

    val vibeData = albumVibes.firstOrNull { it.label == selectedVibe } ?: albumVibes.first()
    val radioQueue = remember(selectedVibe, albums) {
        if (selectedVibe == "ALL") albums.shuffled().take(5)
        else albums.filter { it.vibe == selectedVibe }.shuffled().take(5).ifEmpty { albums.shuffled().take(5) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(VinylAmber.copy(alpha = neonAlpha)))
            Text("📻", fontSize = 18.sp)
            Text("OST MOOD RADIO", fontFamily = BangersFontFamily, color = VinylCream, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Auto-queued for your current vibe", fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.45f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(12.dp)
                .background(vibeData.color.copy(alpha = neonAlpha * 0.12f), RoundedCornerShape(16.dp)))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Green stripe
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                // Vibe header pill — white glass + stamp tilt + green shadow
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.graphicsLayer { rotationZ = -1.5f }) {
                        Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(20.dp)).background(CGreen))
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(vibeData.emoji, fontSize = 16.sp)
                                Text(vibeData.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                            }
                        }
                    }
                    Text("${radioQueue.size} TRACKS QUEUED", fontFamily = BangersFontFamily,
                        color = VinylCream.copy(alpha = 0.4f), fontSize = 10.sp, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Mini queue preview
                radioQueue.take(3).forEachIndexed { i, album ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${i + 1}", fontFamily = BangersFontFamily, color = VinylCream.copy(alpha = 0.3f), fontSize = 12.sp, modifier = Modifier.width(16.dp), textAlign = TextAlign.Center)
                        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(VinylSurface).border(1.dp, VinylGroove, RoundedCornerShape(6.dp))) {
                            when {
                                album.coverImageResId != null -> Image(painterResource(album.coverImageResId), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                album.coverImageUrl != null -> AsyncImage(album.coverImageUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(album.title.take(1), fontFamily = BangersFontFamily, color = VinylAmber, fontSize = 14.sp) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.title, fontFamily = BangersFontFamily, color = VinylCream, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(album.artist, fontFamily = NunitoFontFamily, color = VinylCream.copy(alpha = 0.45f), fontSize = 10.sp)
                        }
                        EraCartridgeBadge(album.era)
                    }
                }
                if (radioQueue.size > 3) {
                    Text("+ ${radioQueue.size - 3} more", fontFamily = NunitoFontFamily,
                        color = VinylCream.copy(alpha = 0.35f), fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 26.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TUNE IN button
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .scale(pulseScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(VinylAmber, VinylOrange)))
                        .clickable { radioQueue.firstOrNull()?.let { onPlay(it) } }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📻", fontSize = 18.sp)
                        Text("TUNE IN", fontFamily = BangersFontFamily, color = VinylDark, fontSize = 20.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

// ─── PS2 Memory Card Tab ──────────────────────────────────────────────────────

@Composable
fun PS2SaveSlot(
    album: Album,
    isSelected: Boolean,
    onClick: () -> Unit,
    slotIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    // Staggered entrance — each slot slides in from the left with semi-random delay
    val staggerDelays = remember {
        listOf(0L, 90L, 50L, 140L, 30L, 120L, 70L, 180L, 10L, 100L, 160L, 60L, 200L, 40L, 130L, 80L)
    }
    val staggerMs = staggerDelays.getOrElse(slotIndex) { (slotIndex * 45L) % 220L }

    var entered by remember { mutableStateOf(false) }
    val enterX by animateFloatAsState(
        targetValue = if (entered) 0f else -90f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "ps2SlotX_${album.id}"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "ps2SlotAlpha_${album.id}"
    )
    LaunchedEffect(slotIndex) {
        delay(staggerMs)
        entered = true
    }

    // Per-slot idle float — continuous gentle rocking on both axes, different speed per slot
    val idleT = rememberInfiniteTransition(label = "idleFloat_${album.id}")
    val periodY = remember(slotIndex) { 2600 + (slotIndex * 373) % 1400 }
    val periodX = remember(slotIndex) { 3100 + (slotIndex * 291) % 1600 }
    val maxY = remember(slotIndex) {
        listOf(18f, -16f, 20f, -14f, 17f, -19f, 15f, -20f, 16f, -18f, 19f, -15f, 20f, -17f, 14f, -19f)
            .getOrElse(slotIndex) { if (slotIndex % 2 == 0) 17f else -17f }
    }
    val maxX = remember(slotIndex) {
        listOf(10f, -12f, 8f, -11f, 12f, -9f, 11f, -10f, 9f, -12f, 10f, -8f, 12f, -11f, 9f, -10f)
            .getOrElse(slotIndex) { if (slotIndex % 3 == 0) 10f else -10f }
    }
    val rawTiltY by idleT.animateFloat(
        initialValue = -kotlin.math.abs(maxY), targetValue = kotlin.math.abs(maxY),
        animationSpec = infiniteRepeatable(tween(periodY, easing = EaseInOut), RepeatMode.Reverse),
        label = "idleTiltY_${album.id}"
    )
    val rawTiltX by idleT.animateFloat(
        initialValue = -kotlin.math.abs(maxX), targetValue = kotlin.math.abs(maxX),
        animationSpec = infiniteRepeatable(tween(periodX, easing = EaseInOut), RepeatMode.Reverse),
        label = "idleTiltX_${album.id}"
    )
    // selectedFraction: 0 = idle tilt active, 1 = snapped flat (selected)
    val selectedFraction by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "selectedFrac_${album.id}"
    )
    val slotScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "slotScale_${album.id}"
    )
    Box(
        modifier = modifier
            .padding(5.dp)
            .graphicsLayer {
                scaleX = slotScale
                scaleY = slotScale
                translationX = enterX
                alpha = enterAlpha
                rotationY = rawTiltY * (1f - selectedFraction)
                rotationX = rawTiltX * (1f - selectedFraction)
                cameraDistance = 8f * density
            }
            .aspectRatio(1f)
            .shadow(if (isSelected) 16.dp else 4.dp, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        when {
            album.coverImageResId != null -> Image(
                painter = painterResource(id = album.coverImageResId),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            album.coverImageUrl != null -> AsyncImage(
                model = album.coverImageUrl,
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize()
            )
            else -> Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg), contentAlignment = Alignment.Center) {
                Text("🎵", fontSize = 20.sp)
            }
        }
        // White gloss strip at top
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.25f).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)))
        )
        // Selection white spotlight glow
        if (isSelected) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)))
            )
        }
    }
}

@Composable
fun PS2MemoryCardTab(
    savedAlbums: List<Album>,
    onAlbumPlay: (Album) -> Unit,
    onAlbumRemove: (Album) -> Unit,
    ps2ContextAlbum: Album?,
    onContextAlbumChange: (Album?) -> Unit,
    onDismiss: () -> Unit = {},
    userName: String? = null
) {
    // Authentic PS2 XMB gray gradient — NOT dark navy
    val ps2BgTop    = Color(0xFFD4D4D4)
    val ps2BgBot    = Color(0xFF5A5A5A)
    val ps2Gold     = CGreenMint
    val ps2Cyan     = Color(0xFF00CED1)
    val ps2White    = Color(0xFFF0F0F0)
    val ps2Dim      = Color(0xFFAAAAAA)
    val ps2HeaderBg = Color(0xFF000000).copy(alpha = 0.55f)

    val totalKb = (savedAlbums.size * 137 + 512).coerceAtLeast(512)
    val freeKb  = (8192 - totalKb).coerceAtLeast(0)

    // Slow rotation for the selected album art
    val spinT = rememberInfiniteTransition(label = "ps2Spin")
    val spinDeg by spinT.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)),
        label = "spinDeg"
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Layered PS2 chrome background ────────────────────────────────────
        // 1. Base: horizontal silver-to-charcoal sweep
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.horizontalGradient(
                0.0f to Color(0xFFDCDCDC),
                0.45f to Color(0xFF9A9A9A),
                1.0f to ScrapbookDark
            )
        ))
        // 2. Vertical sheen: lighter band across the middle height
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.10f),
                0.35f to Color.White.copy(alpha = 0.18f),
                0.55f to Color.Transparent,
                1.0f to Color.Black.copy(alpha = 0.20f)
            )
        ))
        // 3. Top-left radial light bloom — chrome reflection
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(80f, 80f),
                radius = 480f
            )
        ))
        // 4. Subtle diagonal specular streak
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.07f), Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(0f, 200f),
                end = androidx.compose.ui.geometry.Offset(900f, 0f)
            )
        ))

        Column(modifier = Modifier.fillMaxSize()) {

            // ── PS2 header bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(ps2HeaderBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini memory card icon
                Box(
                    modifier = Modifier.size(22.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CGreenDeep.copy(alpha = 0.3f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Memory Card (PS2)/1",
                        fontFamily = NunitoFontFamily,
                        color = ps2White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        "$freeKb KB Free",
                        fontFamily = NunitoFontFamily,
                        color = ps2Dim,
                        fontSize = 10.sp
                    )
                }
                if (!userName.isNullOrBlank()) {
                    Text(
                        userName,
                        fontFamily = NunitoFontFamily,
                        color = ps2Gold,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("💾", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = ps2White, fontSize = 13.sp, fontFamily = NunitoFontFamily)
                }
            }

            if (savedAlbums.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💾", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No Saved Albums", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Bookmark albums to save them here", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 12.sp)
                    }
                }
            } else {
                // ── 4-column grid of vinyl sleeves ──────────────────────────
                LazyColumn(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 96.dp)) {
                    itemsIndexed(savedAlbums.chunked(4)) { rowIndex, row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEachIndexed { colIndex, album ->
                                PS2SaveSlot(
                                    album = album,
                                    isSelected = ps2ContextAlbum?.id == album.id,
                                    onClick = { onContextAlbumChange(if (ps2ContextAlbum?.id == album.id) null else album) },
                                    slotIndex = rowIndex * 4 + colIndex,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // ── Expanded context view (inline split: spinning album left, info right) ──
        AnimatedVisibility(
            visible = ps2ContextAlbum != null,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180))
        ) {
            val album = ps2ContextAlbum ?: return@AnimatedVisibility

            // Smooth vignette — radial gradient darkens toward center, soft edges
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to Color.Black.copy(alpha = 0.72f),
                            0.6f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.25f)
                        )
                    )
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        onContextAlbumChange(null)
                    }
            )

            // Centered split card
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left: album art with white spotlight glow + slow spin
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // White glow halo behind art
                        Box(
                            modifier = Modifier.size(170.dp)
                                .blur(40.dp)
                                .background(Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                        // Spinning album art — Y-axis rotation (left → right, not clockwise)
                        Box(
                            modifier = Modifier.size(150.dp)
                                .graphicsLayer {
                                    rotationY = spinDeg
                                    cameraDistance = 10f * density
                                }
                                .shadow(24.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            when {
                                album.coverImageResId != null -> Image(
                                    painter = painterResource(id = album.coverImageResId),
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                album.coverImageUrl != null -> AsyncImage(
                                    model = album.coverImageUrl,
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize()
                                )
                                else -> Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg), contentAlignment = Alignment.Center) {
                                    Text("🎵", fontSize = 36.sp)
                                }
                            }
                        }
                    }

                    // Right: album info + actions
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            album.title,
                            fontFamily = BangersFontFamily,
                            color = ps2Gold,
                            fontSize = 17.sp,
                            lineHeight = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            album.artist,
                            fontFamily = NunitoFontFamily,
                            color = ps2White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${album.era}  ·  ${album.year ?: "—"}",
                            fontFamily = NunitoFontFamily,
                            color = ps2Dim,
                            fontSize = 11.sp
                        )
                        Text(
                            "${(album.title.length * 3 + 47)}kb",
                            fontFamily = NunitoFontFamily,
                            color = ps2Dim,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "▶  play",
                            fontFamily = NunitoFontFamily,
                            color = ps2White,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onAlbumPlay(album); onContextAlbumChange(null) }
                        )
                        Text(
                            "Copy",
                            fontFamily = NunitoFontFamily,
                            color = ps2Cyan,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onContextAlbumChange(null) }
                        )
                        Text(
                            "delete",
                            fontFamily = NunitoFontFamily,
                            color = ps2White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onAlbumRemove(album); onContextAlbumChange(null) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Albums Screen ────────────────────────────────────────────────────────────

@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null,
    authViewModel: AuthViewModel = viewModel(),
    nowPlayingViewModel: NowPlayingViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val nowPlaying by nowPlayingViewModel.nowPlaying.collectAsState()
    val currentTracks by nowPlayingViewModel.tracks.collectAsState()
    val currentSelectedTrack by nowPlayingViewModel.selectedTrack.collectAsState()
    val isPlaying by nowPlayingViewModel.isPlaying.collectAsState()
    val albumsState by contentViewModel.albumsState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })

    var searchVisible by remember { mutableStateOf(false) }
    // Lifted player state — shared between NowPlayingBar and AlbumPlayerSheet
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }
    var playerVisible by remember { mutableStateOf(false) }
    var selectedEra by remember { mutableStateOf("ALL") }
    var selectedVibe by remember { mutableStateOf("ALL") }
    var featuredCommunityAlbum by remember { mutableStateOf(sampleAlbums.random()) }
    var featuredArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }
    var showMemoryCard by remember { mutableStateOf(false) }
    var showAllCommunity by remember { mutableStateOf(false) }
    var ps2ContextAlbum by remember { mutableStateOf<Album?>(null) }

    val albumOfTheDay = remember { getAlbumOfTheDay() }

    // Mood lighting — background shifts with selected vibe
    val moodColor = vibeMoodColor(selectedVibe)
    val animatedMoodColor by animateColorAsState(
        targetValue = moodColor,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "moodBg"
    )

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    LaunchedEffect(albumsState) {
        if (albumsState is ContentState.Success) {
            val items = (albumsState as ContentState.Success).items
            if (items.isNotEmpty()) featuredArchiveItem = items.random()
        }
    }

    LaunchedEffect(searchQuery) {
        delay(600); if (searchQuery != lastSearched) {
        lastSearched = searchQuery; contentViewModel.fetchAlbums(searchQuery)
    }
    }

    val filteredCommunityAlbums = remember(searchQuery, selectedEra, selectedVibe) {
        sampleAlbums.filter { album ->
            val matchesSearch = searchQuery.isBlank() || album.title.contains(
                searchQuery,
                ignoreCase = true
            ) || album.artist.contains(searchQuery, ignoreCase = true)
            val matchesEra = selectedEra == "ALL" || album.era == selectedEra
            val matchesVibe = selectedVibe == "ALL" || album.vibe == selectedVibe
            matchesSearch && matchesEra && matchesVibe
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedMoodColor,
                        ComicGlassBg,
                        ComicGlassBg
                    )
                )
            )
    ) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // Now Playing bar
            AnimatedVisibility(
                visible = nowPlaying != null,
                enter = expandVertically(tween(300)),
                exit = shrinkVertically(tween(300))
            ) {
                nowPlaying?.let { state ->
                    NowPlayingBar(
                        state = state,
                        tracks = currentTracks,
                        selectedTrack = currentSelectedTrack,
                        isPlaying = isPlaying,
                        onPlayPause = { nowPlayingViewModel.playPause() },
                        onNext = { nowPlayingViewModel.skipNext() },
                        onPrevious = { nowPlayingViewModel.skipPrevious() },
                        onExpand = { playerVisible = true }
                    )
                }
            }

            // Search bar
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search albums or artists...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            color = VinylCream.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = VinylAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""; contentViewModel.fetchAlbums()
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = VinylCream.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = VinylCream
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VinylAmber,
                        unfocusedBorderColor = VinylGroove,
                        focusedContainerColor = VinylSurface,
                        unfocusedContainerColor = VinylSurface,
                        cursorColor = VinylAmber
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(VinylDeep)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── Memory Card button row ──
            Row(
                modifier = Modifier.fillMaxWidth().background(VinylDeep)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ComicGlassBgAlt)
                        .clickable { showMemoryCard = true }
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("💾", fontSize = 13.sp)
                        Text(
                            "MEMORY CARD",
                            fontFamily = BangersFontFamily,
                            color = CGreenMint,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(VinylGroove.copy(alpha = 0.4f)))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                // Now Spinning Hero — vinyl identity piece
                item {
                    NowSpinningHero(
                        nowPlaying = nowPlaying,
                        albums = sampleAlbums,
                        onAlbumClick = {
                            selectedAlbum = it; selectedArchiveItem = null; playerVisible = true
                        }
                    )
                }

                // Toolbar row — search + gallery
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(VinylDeep)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getChillZoneGreeting(),
                            fontFamily = NunitoFontFamily,
                            color = VinylCream.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(VinylSurface)
                                .border(1.dp, VinylAmber.copy(alpha = 0.4f), CircleShape)
                                .clickable { searchVisible = !searchVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = VinylAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Era timeline
                item {
                    EraTimeline(selectedEra = selectedEra, onEraSelected = { selectedEra = it })
                }

                // Vibe selector
                item {
                    AlbumVibeSelector(
                        selectedVibe = selectedVibe,
                        onVibeSelected = { selectedVibe = it },
                        neonAlpha = neonAlpha
                    )
                }

                item { VinylSectionDivider() }

                // Album of the Day
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AlbumOfTheDayCard(
                        album = albumOfTheDay,
                        onPlay = {
                            selectedAlbum = albumOfTheDay; selectedArchiveItem =
                            null; playerVisible = true
                        })
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { VinylSectionDivider() }

                // Weekly Chart
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyChart(
                        albums = sampleAlbums,
                        onAlbumClick = {
                            selectedAlbum = it; selectedArchiveItem = null; playerVisible = true
                        })
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item { VinylSectionDivider() }

                // Community Listening Room
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    CommunityListeningRoom(
                        albums = sampleAlbums,
                        onAlbumClick = {
                            selectedAlbum = it; selectedArchiveItem = null; playerVisible = true
                        })
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item { VinylSectionDivider() }

                // Memory Jukebox
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MemoryJukebox(
                        albums = sampleAlbums,
                        onAlbumClick = {
                            selectedAlbum = it; selectedArchiveItem = null; playerVisible = true
                        })
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item { VinylSectionDivider() }

                // Genre Radar
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AlbumGenreRadar(albums = sampleAlbums)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item { VinylSectionDivider() }

                // OST Mood Radio
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OSTMoodRadio(
                        selectedVibe = selectedVibe,
                        albums = sampleAlbums,
                        onPlay = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item { VinylSectionDivider() }

                // Community albums header + grid
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RetroSectionHeader(title = "CRATE COLLECTION", emoji = "🎮")
                }

                if (filteredCommunityAlbums.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎵", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No albums match your filters",
                                    fontFamily = NunitoFontFamily,
                                    color = VinylCream.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(VinylAmber.copy(alpha = 0.15f)).border(
                                            1.dp,
                                            VinylAmber.copy(alpha = 0.4f),
                                            RoundedCornerShape(8.dp)
                                        ).clickable { selectedEra = "ALL"; selectedVibe = "ALL" }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "CLEAR FILTERS",
                                        fontFamily = BangersFontFamily,
                                        color = VinylAmber,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val displayedCommunity = if (showAllCommunity) filteredCommunityAlbums else filteredCommunityAlbums.take(6)
                    items(displayedCommunity.chunked(2), key = { chunk -> "community_${chunk.first().id}" }) { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { album ->
                                VinylCrateGridCard(
                                    album = album,
                                    isNowPlaying = nowPlaying?.albumId == album.id,
                                    isBookmarked = favoriteIds.contains(album.id),
                                    onPlay = { selectedAlbum = album; selectedArchiveItem = null; playerVisible = true },
                                    onBookmark = { favoritesViewModel?.toggleFavorite(album.toFavoriteItem()) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    if (!showAllCommunity && filteredCommunityAlbums.size > 6) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(VinylAmber.copy(alpha = 0.12f))
                                        .border(1.dp, VinylAmber.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable { showAllCommunity = true }
                                        .padding(horizontal = 28.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "SHOW MORE  ▼",
                                        fontFamily = BangersFontFamily,
                                        color = VinylAmber,
                                        fontSize = 16.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item { VinylSectionDivider() }

                // Archive header + grid
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RetroSectionHeader(title = "INTERNET ARCHIVE", emoji = "📦")
                }

                when (val state = albumsState) {
                    is ContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ThreeDotsAnimation(color = VinylAmber)
                                    Text(
                                        "Loading from archive...",
                                        fontFamily = NunitoFontFamily,
                                        color = VinylCream.copy(alpha = 0.4f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    is ContentState.Error -> {
                        item { GameOverState(message = state.message, onRetry = { contentViewModel.fetchAlbums() }) }
                    }

                    is ContentState.Success -> {
                        items(
                            state.items.chunked(2),
                            key = { chunk -> "archive_${chunk.first().id}" }) { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunk.forEach { item ->
                                    ArchiveAlbumItem(
                                        item = item,
                                        onClick = {
                                            selectedArchiveItem = item; selectedAlbum =
                                            null; playerVisible = true
                                        },
                                        favoritesViewModel = favoritesViewModel,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        // 💾 Memory Card full-screen overlay
        AnimatedVisibility(
            visible = showMemoryCard,
            enter = slideInVertically(tween(380, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(280)) { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            val memorySavedAlbums = remember(favoriteIds) {
                sampleAlbums.filter { favoriteIds.contains(it.id) }
            }
            PS2MemoryCardTab(
                savedAlbums = if (memorySavedAlbums.isEmpty()) sampleAlbums else memorySavedAlbums,
                onAlbumPlay = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true; showMemoryCard = false },
                onAlbumRemove = { favoritesViewModel?.toggleFavorite(it.toFavoriteItem()) },
                ps2ContextAlbum = ps2ContextAlbum,
                onContextAlbumChange = { ps2ContextAlbum = it },
                onDismiss = { showMemoryCard = false },
                userName = currentUser?.displayName ?: currentUser?.email?.substringBefore("@")
            )
        }

        // 🎲 Surprise Me FAB — hidden while Memory Card overlay is open
        if (!playerVisible && !showMemoryCard) {
            val fabGlow by rememberGlowRange(0.3f, 0.7f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp)
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .align(Alignment.Center)
                        .blur(12.dp)
                        .background(VinylAmber.copy(alpha = fabGlow), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VinylAmber, VinylOrange)))
                        .border(2.dp, VinylAmberDim, CircleShape)
                        .clickable {
                            selectedAlbum = sampleAlbums.random()
                            selectedArchiveItem = null
                            playerVisible = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎲", fontSize = 24.sp)
                }
            }
        }

        // ✅ Player sheet overlay
        AnimatedVisibility(
            visible = playerVisible,
            enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            AlbumPlayerSheet(
                album = selectedAlbum,
                archiveItem = selectedArchiveItem,
                onClose = { playerVisible = false },
                onNowPlayingUpdate = { state ->
                    nowPlayingViewModel.updateNowPlaying(state)
                },
                onTracksLoaded = { loaded ->
                    nowPlayingViewModel.updateTracks(loaded)
                },
                onTrackSelected = { track ->
                    nowPlayingViewModel.selectTrack(track)
                },
                onWebViewReady = { wv ->
                    nowPlayingViewModel.webViewRef = wv
                },
                playbackActive = isPlaying
            )
        }
    } // closes main Box
} // closes AlbumsScreen

// ─── Vinyl slides out of its sleeve (player header) ───────────────────────────

@Composable
fun VinylSleeveReveal(album: Album, isPlaying: Boolean) {
    val slide = remember(album.id) { Animatable(0f) }
    val angle = remember { Animatable(0f) }

    LaunchedEffect(album.id) {
        slide.snapTo(0f)
        kotlinx.coroutines.delay(250)
        slide.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 220f))
        Chiptune.play(Chiptune.Sfx.NOISE)   // needle drop
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                angle.animateTo(angle.value + 360f, tween(2400, easing = LinearEasing))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Record (drawn first so it sits behind the sleeve)
        Box(
            modifier = Modifier
                .size(124.dp)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = (6.dp.toPx() + slide.value * 84.dp.toPx())
                    rotationZ = angle.value
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(Color(0xFF111111), radius = r)
                for (i in 1..6) {
                    drawCircle(
                        Color.White.copy(alpha = 0.07f),
                        radius = r * (0.45f + i * 0.08f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
                // shine
                drawArc(
                    Color.White.copy(alpha = 0.12f), startAngle = -60f, sweepAngle = 40f, useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = size
                )
                drawCircle(CGreen, radius = r * 0.34f)
                drawCircle(ScrapbookDark, radius = r * 0.34f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                drawCircle(Color.White, radius = r * 0.05f)
            }
        }
        // Sleeve
        Box(modifier = Modifier.size(132.dp).align(Alignment.CenterStart)) {
            Box(
                modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp)).background(CGreen)
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(2.5.dp, ScrapbookDark, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    album.coverImageResId != null -> androidx.compose.foundation.Image(
                        painter = painterResource(id = album.coverImageResId),
                        contentDescription = album.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    !album.coverImageUrl.isNullOrBlank() -> AsyncImage(
                        model = album.coverImageUrl,
                        contentDescription = album.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.halftoneReveal(album.coverImageUrl).fillMaxSize()
                    )
                    else -> Text("♪", fontFamily = BangersFontFamily, fontSize = 40.sp, color = ScrapbookDark)
                }
            }
        }
        // Label to the right
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).width(92.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(if (isPlaying) "NOW SPINNING" else "READY", fontFamily = BangersFontFamily,
                fontSize = 13.sp, color = CGreenDeep, letterSpacing = 1.sp)
            Text("33⅓ RPM", fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted)
        }
    }
}

// ─── Tiny spinning vinyl for the playing track row ────────────────────────────

@Composable
fun MiniSpinningVinyl(size: androidx.compose.ui.unit.Dp, spinning: Boolean) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) { angle.animateTo(angle.value + 360f, tween(1600, easing = LinearEasing)) }
        }
    }
    Canvas(modifier = Modifier.size(size).graphicsLayer { rotationZ = angle.value }) {
        val r = this.size.minDimension / 2f
        drawCircle(Color(0xFF111111), radius = r)
        drawCircle(Color.White.copy(alpha = 0.12f), radius = r * 0.7f,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx()))
        drawCircle(CGreen, radius = r * 0.35f)
        drawLine(Color.White.copy(alpha = 0.5f), center, androidx.compose.ui.geometry.Offset(center.x + r * 0.9f, center.y),
            strokeWidth = 1.dp.toPx())
    }
}

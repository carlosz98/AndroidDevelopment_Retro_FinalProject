package com.example.hubretro

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─── Color tokens ─────────────────────────────────────────────────────────────
// All colors live in ui/theme/Color.kt — these are referenced throughout.
// The ones below are shared helpers used across multiple feature files.

// Color tokens are defined in ui/theme/Color.kt

// ─── Dimension constants ──────────────────────────────────────────────────────

private val GAME_IMAGE_HEIGHT = 160.dp
private val GAME_LABEL_HEIGHT = 40.dp

// ─── Data models (shared across feature files) ────────────────────────────────

data class GamingPlatform(
    val name: String,
    val iconResId: Int,
    val color: Color,
    val prefix: String
)

val gamingPlatforms = listOf(
    GamingPlatform("PlayStation", R.drawable.ic_playstation, Color(0xFF003791), "PSN"),
    GamingPlatform("Xbox",        R.drawable.ic_xbox,        Color(0xFF107C10), "Xbox"),
    GamingPlatform("Steam",       R.drawable.ic_steam,       CGreenDeep, "Steam"),
    GamingPlatform("Nintendo",    R.drawable.ic_nintendo,    Color(0xFFE4000F), "Nintendo")
)

data class Game(
    val name: String,
    val imageResId: Int? = null,
    val coverUrl: String? = null,
    val platform: String? = null   // e.g. "PlayStation 3", "Wii", "Xbox 360", "PC"
)

// ─── ArticleItem (shared across ProfileScreen, ActivityViewModel, ArticlesFeatures) ──
// NOTE: This must be the ONLY declaration of ArticleItem in the whole project.
// It was previously declared twice in this file (redeclaration error) and must
// NOT be redeclared in ArticlesFeatures.kt — delete it there if present.

data class ArticleItem(
    val id: String = "",
    val title: String = "",
    val snippet: String = "",
    val fullContent: String = "",
    val date: String = "",
    val author: String = "",
    val authorUid: String? = null,
    val imageResId: Int? = null,
    val imageUrl: String? = null,
    val youtubeVideoId: String? = null,
    val viewCount: Int = 0,
    val category: String = "GAMING",
    val reactions: Map<String, Int> = emptyMap(),
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val commentCount: Int = 0,
    val readingTimeMinutes: Int = 0,
    val sourceUrl: String = "",
    val sourceName: String = "",
    val isNewsArticle: Boolean = false,
    val publishedAt: Long = 0L          // epoch millis — used for "newest first" + NEW badges
)

data class ActivityItem(
    val id: String = "",
    val description: String = "",
    val timeAgo: String = "",
    val type: String = "BOOKMARK",
    val itemTitle: String = "",
    val itemSnippet: String = "",
    val itemImageUrl: String = "",
    val targetUsername: String = "",
    val userProfilePicUrl: String? = null
)

data class UserProfile(
    val username: String = "Don Carlos",
    val userHandle: String = "@logodzip",
    val bio: String = "Retro enthusiast 🎮",
    val profilePictureResId: Int = R.drawable.profile1,
    val bannerImageResId: Int = R.drawable.banner1,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val topGames: List<Game> = listOf(
        Game("Fez", R.drawable.game1), Game("Final Fantasy XIII", R.drawable.game2),
        Game("Final Fantasy X", R.drawable.game3), Game("Infamous Second Son", R.drawable.game4),
        Game("Minecraft", R.drawable.game5), Game("Cyberpunk 2077", R.drawable.game6)
    ),
    val topSoundtracks: List<Soundtrack> = listOf(
        Soundtrack("Minecraft OST", "C418", R.drawable.vinyl1),
        Soundtrack("The Sims OST", "EA", R.drawable.vinyl2),
        Soundtrack("Undertale OST", "Toby Fox", R.drawable.vinyl3)
    )
)

data class GenreScore(val genre: String, val score: Float)

data class NavCardData(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val tagline: String,
    val imageResId: Int,
    val accentColor: Color,
    val isComingSoon: Boolean = false,
    val onClick: () -> Unit
)

data class StatItemData(
    val emoji: String,
    val count: Int,
    val label: String,
    val tagline: String,
    val accentColor: Color,
    val glowColor: Color
)

data class HomeStats(
    val userCount: Int = 0,
    val articleCount: Int = 0,
    val albumCount: Int = 0
)

data class LiveTickerItem(
    val username: String,
    val action: String,
    val subject: String,
    val emoji: String
)

// ─── Pure helper functions ────────────────────────────────────────────────────

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0).replace(".0M", "M")
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0).replace(".0K", "K")
    else -> count.toString()
}

fun formatMemberSince(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return "Joined " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

fun formatEpochMillisToReadableDate(epochMillis: Long): String =
    try { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMillis)) }
    catch (e: Exception) { "Date N/A" }

fun timeAgoFromMillis(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    return when {
        diff < 60_000       -> "just now"
        diff < 3_600_000    -> "${diff / 60_000}m ago"
        diff < 86_400_000   -> "${diff / 3_600_000}h ago"
        diff < 604_800_000  -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

// ─── Post / story type helpers ────────────────────────────────────────────────

fun postTypeColor(type: String): Color = when (type) {
    "game"       -> CAcPurple
    "rating"     -> CAcYellow
    "ost"        -> CAcRed
    "screenshot" -> CAcBlue
    "article"    -> CGreenDeep
    else         -> CGreenDeep
}

fun postTypeEmoji(type: String): String = when (type) {
    "game" -> "🎮"; "rating" -> "⭐"; "ost" -> "🎵"
    "screenshot" -> "📸"; "article" -> "📰"; else -> "💬"
}

fun postTypeLabel(type: String): String = when (type) {
    "game" -> "GAME"; "rating" -> "RATING"; "ost" -> "OST"
    "screenshot" -> "SCREENSHOT"; "article" -> "ARTICLE"; else -> "POST"
}

fun storyTypeColor(type: String): Color = when (type) {
    "now_playing"  -> CAcPurple
    "screenshot"   -> CAcBlue
    "ost"          -> CAcRed
    "achievement"  -> CAcYellow
    else           -> CGreenDeep
}

fun storyTypeEmoji(type: String): String = when (type) {
    "now_playing" -> "🎮"; "screenshot" -> "📸"
    "ost" -> "🎵"; "achievement" -> "🏆"; else -> "💬"
}

// ─── Game / genre helpers ─────────────────────────────────────────────────────

fun detectGenreForGame(gameName: String): String {
    val n = gameName.lowercase()
    return when {
        n.contains("final fantasy") || n.contains("zelda") || n.contains("chrono") ||
                n.contains("persona") || n.contains("dragon") || n.contains("earthbound") ||
                n.contains("undertale") || n.contains("pokemon") -> "RPG"
        n.contains("sonic") || n.contains("infamous") || n.contains("god of war") ||
                n.contains("devil may cry") || n.contains("cyberpunk") || n.contains("batman") -> "ACTION"
        n.contains("mario") || n.contains("fez") || n.contains("kirby") ||
                n.contains("crash") || n.contains("hollow knight") || n.contains("celeste") -> "PLATFORMER"
        n.contains("doom") || n.contains("halo") || n.contains("half-life") ||
                n.contains("metroid") || n.contains("quake") -> "SHOOTER"
        n.contains("minecraft") || n.contains("uncharted") || n.contains("tomb raider") ||
                n.contains("shadow") || n.contains("ico") -> "ADVENTURE"
        n.contains("pac-man") || n.contains("tetris") || n.contains("street fighter") ||
                n.contains("sims") || n.contains("mega man") || n.contains("galaga") -> "ARCADE"
        else -> "GAMING"
    }
}

fun eraAccentColor(gameName: String): Color {
    val n = gameName.lowercase()
    return when {
        n.contains("final fantasy") || n.contains("zelda") || n.contains("chrono") ||
                n.contains("persona") || n.contains("dragon") || n.contains("earthbound") ||
                n.contains("undertale") || n.contains("pokemon") -> CAcPurple
        n.contains("sonic") || n.contains("infamous") || n.contains("god of war") ||
                n.contains("devil may cry") || n.contains("cyberpunk") || n.contains("batman") -> CAcRed
        n.contains("mario") || n.contains("fez") || n.contains("kirby") ||
                n.contains("crash") || n.contains("hollow knight") || n.contains("celeste") -> CAcBlue
        n.contains("doom") || n.contains("halo") || n.contains("half-life") ||
                n.contains("metroid") || n.contains("quake") -> CGreenDeep
        n.contains("minecraft") || n.contains("uncharted") || n.contains("tomb raider") ||
                n.contains("shadow") || n.contains("ico") -> CGreenDeep
        n.contains("pac-man") || n.contains("tetris") || n.contains("street fighter") ||
                n.contains("sims") || n.contains("mega man") || n.contains("galaga") -> CAcYellow
        else -> CAcYellow
    }
}

// Alias kept for any call sites that use gameAccentColor
fun gameAccentColor(gameName: String): Color = eraAccentColor(gameName)

fun detectGenresFromGames(games: List<Game>): List<GenreScore> {
    val genreMap = mapOf(
        "final fantasy" to "RPG", "pokemon" to "RPG", "chrono" to "RPG", "zelda" to "RPG",
        "earthbound" to "RPG", "undertale" to "RPG", "persona" to "RPG", "dragon quest" to "RPG",
        "sonic" to "Action", "devil may cry" to "Action", "god of war" to "Action",
        "batman" to "Action", "infamous" to "Action", "cyberpunk" to "Action",
        "mario" to "Platformer", "kirby" to "Platformer", "crash" to "Platformer",
        "fez" to "Platformer", "hollow knight" to "Platformer",
        "doom" to "Shooter", "halo" to "Shooter", "half-life" to "Shooter",
        "minecraft" to "Adventure", "uncharted" to "Adventure", "tomb raider" to "Adventure",
        "pac-man" to "Arcade", "street fighter" to "Arcade", "tetris" to "Arcade",
        "sims" to "Arcade", "mega man" to "Arcade"
    )
    val scores = mutableMapOf("RPG" to 0f, "Action" to 0f, "Platformer" to 0f,
        "Shooter" to 0f, "Adventure" to 0f, "Arcade" to 0f)
    games.forEach { game ->
        val n = game.name.lowercase()
        genreMap.forEach { genreEntry -> val k = genreEntry.key; val v = genreEntry.value; if (n.contains(k)) scores[v] = (scores[v] ?: 0f) + 1f }
    }
    val total = scores.values.sum()
    if (total == 0f) {
        scores["RPG"] = 0.6f; scores["Action"] = 0.8f; scores["Platformer"] = 0.5f
        scores["Shooter"] = 0.3f; scores["Adventure"] = 0.7f; scores["Arcade"] = 0.4f
    } else {
        val max = scores.values.max()
        scores.keys.forEach { key -> scores[key] = (scores[key] ?: 0f) / max }
    }
    return scores.map { GenreScore(it.key, it.value) }
}

val genreSlideshowGames = mapOf(
    "RPG"        to listOf("Final Fantasy VII", "Final Fantasy X", "Skyrim", "Persona 5", "Chrono Trigger", "Dragon Quest XI"),
    "Action"     to listOf("Infamous Second Son", "God of War", "Devil May Cry 5", "Bayonetta", "Sonic the Hedgehog", "Cyberpunk 2077"),
    "Platformer" to listOf("Super Mario World", "Fez", "Hollow Knight", "Crash Bandicoot", "Kirby Super Star", "Celeste"),
    "Shooter"    to listOf("Doom", "Halo Combat Evolved", "Half-Life 2", "GoldenEye 007", "Quake", "Metroid Prime"),
    "Adventure"  to listOf("Tomb Raider", "Uncharted 4", "Minecraft", "The Legend of Zelda Ocarina of Time", "Shadow of the Colossus", "Ico"),
    "Arcade"     to listOf("Pac-Man", "Street Fighter II", "Tetris", "Donkey Kong", "Galaga", "Space Invaders")
)

// ─── Habbo helpers ────────────────────────────────────────────────────────────

val habboRegions = listOf(
    Triple("🇺🇸 COM",  "habbo.com",    "US"),
    Triple("🇪🇸 ES",   "habbo.es",     "ES"),
    Triple("🇧🇷 BR",   "habbo.com.br", "BR"),
    Triple("🇫🇮 FI",   "habbo.fi",     "FI"),
    Triple("🇩🇪 DE",   "habbo.de",     "DE"),
    Triple("🇫🇷 FR",   "habbo.fr",     "FR"),
    Triple("🇮🇹 IT",   "habbo.it",     "IT"),
    Triple("🇳🇱 NL",   "habbo.nl",     "NL"),
    Triple("🇸🇪 SE",   "habbo.se",     "SE"),
    Triple("🇩🇰 DK",   "habbo.dk",     "DK"),
    Triple("🇳🇴 NO",   "habbo.no",     "NO"),
    Triple("🇹🇷 TR",   "habbo.com.tr", "TR")
)

fun habboAvatarUrl(username: String, domain: String, action: String = "std", size: String = "l") =
    "https://www.$domain/habbo-imaging/avatarimage?user=${username.trim()}&action=$action&direction=2&head_direction=2&size=$size&gesture=sml"

// ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ──────
// ─── UTILITY COMPOSABLES ─────────────────────────────────────────────────────
// ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ──────

// ─── ShimmerBox ───────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Dp = 12.dp) {
    val t = rememberInfiniteTransition(label = "shimmer")
    val offset by t.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerOffset"
    )
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))
        .background(Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.46f)),
            start = Offset(offset * 1000f, 0f),
            end   = Offset((offset + 1f) * 1000f, 0f)
        )))
}

// ─── PulsingDot ───────────────────────────────────────────────────────────────

@Composable
fun PulsingDot(color: Color = CGreen, size: Dp = 10.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val scale  by t.animateFloat(initialValue = 0.8f, targetValue = 1.4f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "ps")
    val alpha  by t.animateFloat(initialValue = 1f, targetValue = 0.4f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "pa")
    Box(modifier = Modifier.size(size).scale(scale).clip(CircleShape).background(color.copy(alpha = alpha)))
}

// ─── Three Dots Loading ───────────────────────────────────────────────────────

@Composable
fun ThreeDotsAnimation(color: Color = CGreen, dotSize: Dp = 8.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val t = rememberInfiniteTransition(label = "dot_$i")
            val y by t.animateFloat(0f, -10f,
                infiniteRepeatable(tween(400, delayMillis = i * 120, easing = EaseInOut), RepeatMode.Reverse),
                label = "dotY_$i")
            Box(modifier = Modifier.size(dotSize).offset(y = y.dp).clip(CircleShape).background(color))
        }
    }
}

// ─── WavePixelDivider ─────────────────────────────────────────────────────────

@Composable
fun WavePixelDivider() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        val colors = listOf(ScrapbookDark, CGreen, ScrapbookDark, CGreen,
            ScrapbookDark, CGreen, ScrapbookDark, CGreen)
        repeat(40) { i ->
            val h = (4f + kotlin.math.abs(kotlin.math.sin(i * 0.8)) * 10f).dp
            Box(modifier = Modifier.weight(1f).height(h).align(Alignment.CenterVertically)
                .background(colors[i % colors.size]))
        }
    }
}

// ─── FloatingEmojiDivider ─────────────────────────────────────────────────────

@Composable
fun FloatingEmojiDivider(emojis: List<String> = listOf("★", "🎮", "★", "🕹️", "★")) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        emojis.forEach { e ->
            Text(text = e, fontSize = if (e == "🎮" || e == "🕹️") 20.sp else 14.sp,
                color = if (e == "★") CGreen else Color.Unspecified,
                modifier = Modifier.padding(horizontal = 10.dp))
        }
    }
}

// ─── StaggeredSection ────────────────────────────────────────────────────────

@Composable
fun StaggeredSection(index: Int, content: @Composable () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 100L); started = true }
    val alpha by animateFloatAsState(targetValue = if (started) 1f else 0f, animationSpec = tween(400), label = "ss_$index")
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) { content() }
}

// ─── AnimatedCounter ─────────────────────────────────────────────────────────

@Composable
fun AnimatedCounter(target: Int, durationMs: Int = 1200): Int {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(target) {
        val steps = 35; val inc = target / steps.toFloat()
        repeat(steps) { i -> delay((durationMs / steps).toLong()); count = ((i + 1) * inc).toInt().coerceAtMost(target) }
        count = target
    }
    return count
}

// ─── ClickTransitionOverlay ───────────────────────────────────────────────────

@Composable
fun ClickTransitionOverlay(visible: Boolean) {
    val alpha by animateFloatAsState(targetValue = if (visible) 0.35f else 0f,
        animationSpec = tween(180, easing = EaseOut), label = "cto")
    if (alpha > 0f) Box(modifier = Modifier.fillMaxSize().background(CGreen.copy(alpha = alpha)))
}

// ─── HomeSectionTitle ─────────────────────────────────────────────────────────

@Composable
fun HomeSectionTitle(title: String, action: String = "", onAction: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        // Gradient accent bar
        Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(listOf(CGreen, CGreen.copy(alpha = 0.4f)))))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp,
            letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        if (action.isNotBlank()) {
            AeroGlassPill(accentColor = ScrapbookDark, modifier = Modifier.clickable { onAction() }) {
                Text(action, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ──────
// ─── AERO GLASS DESIGN SYSTEM (new this session) ─────────────────────────────
// ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ──────

// ─── Paper grain texture ──────────────────────────────────────────────────────

@Composable
fun PaperGrainBackground(modifier: Modifier = Modifier) {
    // Cached tiled dots (matches RetroLabel D texture)
    HalftoneDots(modifier, spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
}

// ─── ComicShimmer ─────────────────────────────────────────────────────────────
// Diagonal light-streak that sweeps across any glass panel automatically.
// Layer this as the LAST child of a Box (renders on top of glass, under border).

@Composable
fun ComicShimmer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.22f,
    durationMs: Int = 2200,
    delayMs: Int = 800
) {
    val shimT = rememberInfiniteTransition(label = "shimmer")
    val shimX by shimT.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = LinearEasing, delayMillis = delayMs),
            RepeatMode.Restart
        ),
        label = "shimX"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.6f),
                        Color.Transparent
                    ),
                    start = Offset(shimX, 0f),
                    end = Offset(shimX + 280f, 400f)
                )
            )
    )
}

// ─── SpringEntrance ───────────────────────────────────────────────────────────
// Wrap a card/section in this and it pops in with a bouncy spring when it
// first appears on screen. Zero layout impact — just animates scale+alpha.

@Composable
fun SpringEntrance(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    content: @Composable BoxScope.() -> Unit
) {
    // "Dealt card" entrance: slides up with a slight tilt and settles (delay capped so long lists don't lag)
    Box(modifier = modifier.dealIn(delayMs = delayMs, seed = delayMs / 10), content = content)
}

// ─── HoloBadge ────────────────────────────────────────────────────────────────
// Animated holographic-foil badge — cycles through green palette like a shiny
// trading card. Use for "GAME OF THE DAY", score chips, edition labels.

@Composable
fun HoloBadge(
    label: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    fontSize: TextUnit = 11.sp
) {
    val holoT = rememberInfiniteTransition(label = "holo")
    val holoOff by holoT.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "holoOff"
    )
    val shift = holoOff * 600f
    val holoBrush = Brush.horizontalGradient(
        colors = listOf(CGreenDeep, CGreen, CGreenMint, Color(0xFFB7E4C7), CGreen, CGreenDeep),
        startX = shift - 300f,
        endX = shift + 300f
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(holoBrush)
            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(cornerRadius))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = fontSize,
            letterSpacing = 1.sp
        )
    }
}

// ─── GlowPulse ────────────────────────────────────────────────────────────────
// Breathing neon-glow border — put this BEHIND a bordered Box to add a soft
// pulsing halo around it. Use for active tabs, live indicators, selected items.

@Composable
fun GlowPulse(
    modifier: Modifier = Modifier,
    glowColor: Color = CGreen,
    cornerRadius: Dp = 12.dp,
    maxAlpha: Float = 0.55f,
    blurRadius: Dp = 10.dp
) {
    val glowT = rememberInfiniteTransition(label = "glow")
    val glowA by glowT.animateFloat(
        initialValue = 0.15f,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Box(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(cornerRadius + 4.dp))
            .background(glowColor.copy(alpha = glowA))
            .blur(blurRadius)
    )
}

// ─── ScanlineOverlay ──────────────────────────────────────────────────────────
// Retro CRT scanline effect — drifting horizontal stripes over any panel.
// Use on hero banners, video thumbnails, score displays.

@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineAlpha: Float = 0.07f,
    driftSpeedMs: Int = 3000
) {
    val scanT = rememberInfiniteTransition(label = "scan")
    val driftY by scanT.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(driftSpeedMs, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "scanDrift"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val lineH = 2f; val gap = 6f; val step = lineH + gap
        var y = driftY % step - step
        while (y < size.height) {
            drawRect(
                color = Color.Black.copy(alpha = lineAlpha),
                topLeft = Offset(0f, y),
                size = Size(size.width, lineH)
            )
            y += step
        }
    }
}

// ─── InkRipple ────────────────────────────────────────────────────────────────
// Comic-book ink-ring ripple on tap — expanding hollow circle that fades out.
// Trigger by calling rippleTrigger() on tap; pass the returned trigger state.

@Composable
fun rememberInkRipple(): () -> Unit {
    var ripple by remember { mutableStateOf(false) }
    return { ripple = !ripple }
}

@Composable
fun InkRippleEffect(
    modifier: Modifier = Modifier,
    trigger: Boolean,
    color: Color = ScrapbookDark,
    onDone: () -> Unit = {}
) {
    val progress by animateFloatAsState(
        targetValue = if (trigger) 1f else 0f,
        animationSpec = tween(380, easing = EaseOut),
        label = "inkRipple",
        finishedListener = { if (it >= 1f) onDone() }
    )
    if (progress > 0f) {
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val r = size.minDimension * progress
            drawCircle(
                color = color.copy(alpha = (1f - progress) * 0.45f),
                radius = r,
                style = Stroke(width = 3f + (1f - progress) * 6f)
            )
        }
    }
}

// ─── ComicGlassButton ────────────────────────────────────────────────────────
// The canonical button style: pill shape, 2.5dp black border, 4dp offset shadow,
// spring scale on press, comic burst ripple on tap.

@Composable
fun ComicGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    emoji: String = "",
    filled: Boolean = true,          // true = dark fill; false = glass white
    accentColor: Color = ScrapbookDark,
    enabled: Boolean = true,
    cornerRadius: Dp = 50.dp
) {
    var pressed by remember { mutableStateOf(false) }
    var burst   by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cgb_scale"
    )
    val burstAnim by animateFloatAsState(
        targetValue = if (burst) 1f else 0f,
        animationSpec = tween(420, easing = EaseOut),
        label = "cgb_burst",
        finishedListener = { burst = false }
    )

    Box(modifier = modifier.scale(scale)) {
        // 4dp comic shadow offset
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(ScrapbookDark.copy(alpha = 0.18f))
        )
        // Button body
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    if (filled) Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))
                    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.44f)))
                )
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(cornerRadius))
                .clickable(enabled = enabled) {
                    pressed = true
                    burst = true
                    onClick()
                }
                .padding(horizontal = 20.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (emoji.isNotBlank()) Text(emoji, fontSize = 15.sp)
                Text(
                    label,
                    fontFamily = BangersFontFamily,
                    color = if (filled) ScrapbookDark else ScrapbookDark,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
            // Comic burst overlay (drawn with Canvas)
            if (burstAnim > 0f) {
                val burstAlpha = (1f - burstAnim) * 0.55f
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    val r = size.minDimension * burstAnim * 1.8f
                    drawCircle(color = Color.White.copy(alpha = burstAlpha), radius = r)
                    // Starburst lines
                    val cx = size.width / 2f; val cy = size.height / 2f
                    repeat(8) { i ->
                        val angle = Math.toRadians(i * 45.0)
                        val len = r * 0.6f * (1f - burstAnim * 0.3f)
                        drawLine(
                            color = Color.White.copy(alpha = burstAlpha * 0.7f),
                            start = Offset(cx + (r * 0.4f * kotlin.math.cos(angle)).toFloat(), cy + (r * 0.4f * kotlin.math.sin(angle)).toFloat()),
                            end   = Offset(cx + (len * kotlin.math.cos(angle)).toFloat(), cy + (len * kotlin.math.sin(angle)).toFloat()),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── ComicGlassCard ──────────────────────────────────────────────────────────
// Drop-in card wrapper: glass panel + 2.5dp border + 4dp comic shadow + green stripe.

@Composable
fun ComicGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    showStripe: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cgcard_scale"
    )
    Box(modifier = modifier.scale(scale)) {
        // Comic shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(ScrapbookDark.copy(alpha = 0.12f))
        )
        // Glass panel
        Box(modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(cornerRadius))
            .then(if (onClick != null) Modifier.clickable { pressed = true; onClick() } else Modifier)
        ) {
            Column(content = {
                // Animated gradient stripe
                if (showStripe) {
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                }
                content()
            })
            // Shimmer sweep
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = cornerRadius)
        }
    }
    if (onClick != null) LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── AeroGlassCard ───────────────────────────────────────────────────────────

@Composable
fun AeroGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White,
    glowAlpha: Float = 0.45f,
    cornerRadius: Dp = 20.dp,
    showOffsetShadow: Boolean = true,
    useGlossyDark: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        if (showOffsetShadow) {
            Box(modifier = Modifier.matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(ScrapbookDark.copy(alpha = 0.12f)))
        }
        if (useGlossyDark) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Brush.verticalGradient(listOf(
                        ComicGlassBg, Color.White.copy(alpha = 0.44f), ComicGlassBg
                    )))
                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(cornerRadius)),
                content = content
            )
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = cornerRadius)
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(cornerRadius)),
                content = content
            )
            // Top gloss dome (glass-white only)
            Box(modifier = Modifier.matchParentSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f, endY = 200f
                ))
            )
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = cornerRadius)
        }
    }
}

// ─── AeroGlassPill ───────────────────────────────────────────────────────────

@Composable
fun AeroGlassPill(
    modifier: Modifier = Modifier,
    accentColor: Color,
    cornerRadius: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Comic shadow
        Box(modifier = Modifier.matchParentSize()
            .offset(x = 4.dp, y = 4.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(ScrapbookDark.copy(alpha = 0.18f)))
        Box(modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.52f))
            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(cornerRadius))
        ) {
            Box(modifier = Modifier.matchParentSize()
                .background(Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                    startY = 0f, endY = 30f
                )))
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

// ─── StoryProgressArc ────────────────────────────────────────────────────────

@Composable
fun StoryProgressArc(color: Color, progress: Float = 1f) {
    val t = rememberInfiniteTransition(label = "arcT")
    val glowAlpha by rememberGlowRange(0.6f, 1f)
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 3.5f; val inset = stroke / 2f
        drawArc(color = color.copy(alpha = 0.15f), startAngle = -90f, sweepAngle = 360f,
            useCenter = false, topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke), style = Stroke(width = stroke))
        drawArc(color = color.copy(alpha = glowAlpha), startAngle = -90f, sweepAngle = 360f * progress,
            useCenter = false, topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke), style = Stroke(width = stroke))
    }
}

// ─── UnreadNotificationBadge ──────────────────────────────────────────────────

@Composable
fun UnreadNotificationBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(modifier = modifier.size(18.dp).clip(CircleShape).background(CAcRed),
        contentAlignment = Alignment.Center) {
        Text(if (count > 9) "9+" else "$count", fontFamily = BangersFontFamily,
            color = Color.White, fontSize = 9.sp)
    }
}

// ─── NeonGlowBox ─────────────────────────────────────────────────────────────

@Composable
fun NeonGlowBox(
    modifier: Modifier = Modifier,
    glowColor: Color = CGreen,
    cornerRadius: Dp = 12.dp,
    glowAlpha: Float = 0.6f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.matchParentSize().padding(2.dp)
            .clip(RoundedCornerShape(cornerRadius + 2.dp))
            .background(glowColor.copy(alpha = glowAlpha * 0.3f))
            .blur(8.dp))
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(cornerRadius)),
            content = content)
    }
}

// ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ──────
// ─── SCRAPBOOK DESIGN SYSTEM ─────────────────────────────────────────────────
// ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ─── ──────

// ─── ScrapbookCard ───────────────────────────────────────────────────────────

@Composable
fun ScrapbookCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ComicGlassBg,
    borderColor: Color = ScrapbookBorder,
    cornerRadius: Dp = 14.dp,
    shadowOffset: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        if (shadowOffset > 0.dp) {
            Box(modifier = Modifier.matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(borderColor.copy(alpha = 0.25f)))
        }
        Box(modifier = Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(cornerRadius)),
            content = content)
    }
}

// ─── ScrapbookSectionHeader ───────────────────────────────────────────────────

@Composable
fun ScrapbookSectionHeader(title: String, emoji: String) {
    val glowAlpha by rememberGlowRange(0.3f, 1f)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp))
            .background(CGreen.copy(alpha = glowAlpha)))
        Spacer(modifier = Modifier.width(10.dp))
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val dotT = rememberInfiniteTransition(label = "hdot_$title$i")
                val dotY by dotT.animateFloat(0f, -4f,
                    infiniteRepeatable(tween(400, delayMillis = i * 130, easing = EaseInOut), RepeatMode.Reverse),
                    label = "hdotY_$i")
                Box(modifier = Modifier.size(5.dp).offset(y = dotY.dp).clip(CircleShape)
                    .background(CGreen.copy(alpha = glowAlpha)))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f), thickness = 2.dp)
    }
}

// ─── RetroSectionHeader — RetroLabel D style (reusable across all screens) ───

@Composable
fun RetroSectionHeader(title: String, emoji: String) {
    val glowAlpha by rememberGlowPhase(0.45f)
    val tapHaptic = rememberTapHaptic()
    var pressed by remember { mutableStateOf(false) }
    // Slides in from the left with a little overshoot each time it appears
    val slideIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) { slideIn.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f)) }
    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 4f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rshPress"
    )
    val shadowOffset by animateFloatAsState(
        targetValue = if (pressed) 0f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rshShadowOff"
    )
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        .graphicsLayer {
            translationX = (1f - slideIn.value) * -60.dp.toPx()
            rotationZ = (1f - slideIn.value) * -3f
            alpha = slideIn.value.coerceIn(0f, 1f)
        }) {
        Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 7.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CGreen.copy(alpha = if (pressed) 0f else glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOffset.dp, y = shadowOffset.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CGreen.copy(alpha = if (pressed) 0.3f else 1f)))
        Box(modifier = Modifier.fillMaxWidth()
            .offset(x = pressAnim.dp, y = pressAnim.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
            .clickable { pressed = true; tapHaptic() }
            .padding(horizontal = 14.dp, vertical = 9.dp)) {
            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.3.dp, color = Color.Black.copy(alpha = 0.13f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, fontSize = 18.sp)
                Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── RetroGlassButton — white glass + dots + glow pulse + translate-on-press ──

@Composable
fun RetroGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    cornerRadius: Dp = 10.dp,
    burstText: String? = null   // e.g. "POW!" — fires a comic burst + strong haptic on tap
) {
    val glowAlpha by rememberGlowPhase(0.4f)
    val tapHaptic = rememberTapHaptic()
    val burst = rememberBurstState()
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(
        targetValue = if (pressed) 4f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rgbPress"
    )
    val shadowOff by animateFloatAsState(
        targetValue = if (pressed) 0f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rgbShadowOff"
    )
    Box(modifier = modifier) {
        Box(modifier = Modifier.matchParentSize().offset(x = 7.dp, y = 7.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(CGreen.copy(alpha = if (pressed) 0f else glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(CGreen.copy(alpha = if (pressed) 0.3f else 1f)))
        Box(modifier = Modifier.fillMaxWidth()
            .offset(x = pressAnim.dp, y = pressAnim.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(cornerRadius))
            .clickable {
                pressed = true
                if (burstText != null) burst.fire(burstText) else tapHaptic()
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center) {
            HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                icon?.invoke()
                Text(text, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
            }
        }
        if (burstText != null) ComicBurst(burst, Modifier.align(Alignment.Center))
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── HalftoneBackground — micro dot background for screen canvases ────────────

@Composable
fun HalftoneBackground(modifier: Modifier = Modifier) {
    HalftoneDots(modifier, spacing = 8.dp, dotRadius = 1.2.dp, color = Color(0xFF1B4332).copy(alpha = 0.13f))
}

// ─── ScrapbookInputField ──────────────────────────────────────────────────────

@Composable
fun ScrapbookInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ScrapbookDark, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f),
                focusedContainerColor = Color.White.copy(alpha = 0.8f), unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                cursorColor = ScrapbookDark, focusedTextColor = ScrapbookDark, unfocusedTextColor = ScrapbookDark
            ),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── ScrapbookStatCard ────────────────────────────────────────────────────────

@Composable
fun ScrapbookStatCard(value: String, label: String, neonAlpha: Float = 0.6f, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "statScale")
    Box(modifier = Modifier.width(82.dp).scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onClick() },
            backgroundColor = Color.White.copy(alpha = 0.46f), cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Box(modifier = Modifier.fillMaxWidth().border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    RollingCounterText(value, androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 22.sp))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(label, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted,
                        fontSize = 7.5.sp, textAlign = TextAlign.Center, maxLines = 1, letterSpacing = 0.5.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── ScrapbookXPProgressBar ───────────────────────────────────────────────────

@Composable
fun ScrapbookXPProgressBar(xp: Int, level: RetroLevel, progress: Float, xpToNext: Int) {
    // Fills from 0 every time it appears, with a sparkle trail riding the leading edge
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        delay(250)
        fill.animateTo(progress.coerceIn(0f, 1f), tween(1400, easing = FastOutSlowInEasing))
    }
    val sparkT = rememberInfiniteTransition(label = "xpSpark")
    val sparkPhase by sparkT.animateFloat(0f, 1f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "xpSparkPhase")
    val shimmerOffset by sparkT.animateFloat(-1f, 2f,
        infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "xpShimmerOff")
    val barColor = if (level.level == 1) CGreen else level.color

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("${level.emoji} ${level.title}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.8f))
                .border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                RollingCounterText("$xp XP", androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(18.dp)) {
            // hard shadow
            Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
                .clip(RoundedCornerShape(9.dp)).background(ScrapbookDark))
            Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.85f))
                .border(2.dp, ScrapbookDark, RoundedCornerShape(9.dp))) {
                Box(modifier = Modifier.fillMaxWidth(fill.value).fillMaxHeight()
                    .background(Brush.horizontalGradient(colors = listOf(CGreen, barColor))))
                HalftoneDots(Modifier.fillMaxWidth(fill.value).fillMaxHeight(), spacing = 5.dp, dotRadius = 1.dp,
                    color = Color.White.copy(alpha = 0.35f))
                if (fill.value > 0f) {
                    Box(modifier = Modifier.fillMaxWidth(fill.value).fillMaxHeight()
                        .background(Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent),
                            start = Offset(shimmerOffset * 200f, 0f),
                            end   = Offset((shimmerOffset + 0.5f) * 200f, 0f))))
                }
            }
            // sparkle trail at the leading edge
            if (fill.value > 0.02f) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    val headX = size.width * fill.value
                    val cy = size.height / 2f
                    repeat(5) { i ->
                        val t = (sparkPhase + i / 5f) % 1f
                        val x = headX - t * 36.dp.toPx()
                        val y = cy + kotlin.math.sin((t * 6.28f) + i) * size.height * 0.45f
                        val r = (1f - t) * 3.dp.toPx()
                        drawCircle(Color.White.copy(alpha = 1f - t), radius = r, center = Offset(x, y))
                        drawCircle(CAcYellowL.copy(alpha = (1f - t) * 0.8f), radius = r * 0.55f, center = Offset(x, y))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val nextText = if (level.maxXP == Int.MAX_VALUE) "MAX LEVEL REACHED 🌟" else "→ NEXT LEVEL: $xpToNext XP away"
        Text(nextText, fontFamily = NunitoFontFamily, color = if (level.maxXP == Int.MAX_VALUE) barColor else ScrapbookTextMuted,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
    }
}

// ─── BadgeShelf + BadgeItem ───────────────────────────────────────────────────

@Composable
fun BadgeShelf(badges: List<Badge>) {
    // Holographic trading cards: tilt the phone to see the foil, tap to flip
    val tilt = rememberDeviceTilt()
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(badges, key = { it.id }) { badge -> HoloBadgeCard(badge, tilt) }
    }
}

@Composable
fun BadgeItem(badge: Badge) {
    val pulseT = rememberInfiniteTransition(label = "badge_${badge.id}")
    val pulseScale by pulseT.animateFloat(1f, if (badge.isEarned) 1.06f else 1f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "badgePulse")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp).scale(pulseScale)) {
        Box(modifier = Modifier.size(56.dp)) {
            if (badge.isEarned) Box(modifier = Modifier.matchParentSize().clip(CircleShape).background(badge.color.copy(alpha = 0.2f)))
            ScrapbookCard(modifier = Modifier.fillMaxSize(),
                backgroundColor = if (badge.isEarned) badge.color.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.46f),
                borderColor = if (badge.isEarned) badge.color else ScrapbookBorder.copy(alpha = 0.2f),
                cornerRadius = 28.dp, shadowOffset = if (badge.isEarned) 3.dp else 1.dp) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(badge.emoji, fontSize = 22.sp, color = if (badge.isEarned) Color.Unspecified else Color.Black.copy(alpha = 0.2f))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(badge.name, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
            color = if (badge.isEarned) ScrapbookDark else ScrapbookTextMuted.copy(alpha = 0.5f),
            fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
    }
}

// ─── PlatformBubble ──────────────────────────────────────────────────────────

@Composable
fun PlatformBubble(platform: GamingPlatform, username: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val profileUrl = when (platform.name) {
        "PlayStation" -> "https://psnprofiles.com/$username"
        "Xbox"        -> "https://xboxgamertag.com/search/$username"
        "Steam"       -> "https://steamcommunity.com/id/$username"
        "Nintendo"    -> "https://www.nintendo.com/search/#q=$username"
        else -> null
    }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "platformBubbleScale")
    Box(modifier = modifier.scale(scale)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
            .clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
        Box(modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
            .clickable { pressed = true; profileUrl?.let { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } catch (e: Exception) { } } }
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(platform.color.copy(alpha = 0.12f))
                    .border(1.5.dp, platform.color.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(platform.name.uppercase(), fontFamily = BangersFontFamily, color = platform.color, fontSize = 10.sp, letterSpacing = 1.sp)
                    Text("@$username", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (profileUrl != null) Icon(Icons.Filled.OpenInBrowser, null, tint = platform.color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.20f)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── StreamBubbles ───────────────────────────────────────────────────────────

@Composable
fun StreamBubbles(twitchUsername: String, youtubeUsername: String, context: Context) {
    if (twitchUsername.isBlank() && youtubeUsername.isBlank()) return
    val neonT = rememberInfiniteTransition(label = "streamNeon")
    val neonAlpha by neonT.animateFloat(0.5f, 1f,
        infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse), label = "streamNeonAlpha")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (twitchUsername.isNotBlank()) {
            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (pressed) 0.93f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "twitchScale")
            Box(modifier = Modifier.scale(scale).clip(RoundedCornerShape(14.dp)).background(CGreen)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/$twitchUsername"))) } catch (e: Exception) { } }
                .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = neonAlpha)))
                    Column {
                        Text("TWITCH", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("@$twitchUsername", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.OpenInBrowser, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
        }
        if (youtubeUsername.isNotBlank()) {
            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (pressed) 0.93f else 1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ytScale")
            Box(modifier = Modifier.scale(scale).clip(RoundedCornerShape(14.dp)).background(CAcRed)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@$youtubeUsername"))) } catch (e: Exception) { } }
                .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 10.sp) }
                    Column {
                        Text("YOUTUBE", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Text("@$youtubeUsername", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    Icon(Icons.Filled.OpenInBrowser, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
        }
    }
}

// ─── AccordionSection ────────────────────────────────────────────────────────

@Composable
fun AccordionSection(
    emoji: String,
    title: String,
    accentColor: Color = CGreen,
    isExpanded: Boolean,
    completionFraction: Float = 0f,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val neonAlpha by rememberGlowRange(0.3f, 0.9f)
    val arrowRotation by animateFloatAsState(if (isExpanded) 180f else 0f, tween(300, easing = EaseInOut), label = "arrowRotation_$title")
    val headerShape = if (isExpanded) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp) else RoundedCornerShape(12.dp)
    val contentShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().clip(headerShape).background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, headerShape)
            .clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.width(3.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(accentColor.copy(alpha = if (isExpanded) neonAlpha else 0.5f)))
                    Text(emoji, fontSize = 18.sp)
                    Text(title, fontFamily = BangersFontFamily, color = if (isExpanded) ScrapbookDark else ScrapbookDark.copy(alpha = 0.7f), fontSize = 17.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (completionFraction > 0f) {
                        Box(modifier = Modifier.width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookDark.copy(alpha = 0.12f))) {
                            Box(modifier = Modifier.fillMaxWidth(completionFraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(accentColor.copy(alpha = 0.8f)))
                        }
                    } else {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)).border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, null, tint = accentColor, modifier = Modifier.size(12.dp))
                        }
                    }
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = arrowRotation })
                }
            }
        }
        AnimatedVisibility(visible = isExpanded,
            enter = expandVertically(tween(300, easing = EaseInOut)) + fadeIn(tween(300)),
            exit  = shrinkVertically(tween(300, easing = EaseInOut)) + fadeOut(tween(200))) {
            Box(modifier = Modifier.fillMaxWidth().clip(contentShape).background(Color.White.copy(alpha = 0.3f))
                .border(2.5.dp, ScrapbookDark, contentShape).padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            }
        }
    }
}

// ─── RetroTerminalInput ───────────────────────────────────────────────────────

@Composable
fun RetroTerminalInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxChars: Int = 0,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("▶ $label", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 11.sp, letterSpacing = 1.sp)
            if (maxChars > 0) Text("${value.length}/$maxChars", fontFamily = BangersFontFamily,
                color = if (value.length > maxChars * 0.8) CAcRed.copy(alpha = 0.8f) else ScrapbookTextMuted, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.7f))
            .border(1.5.dp, if (value.isNotBlank()) CGreen else ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { var processed = it; if (maxChars > 0 && processed.length > maxChars) processed = processed.take(maxChars); onValueChange(processed) },
                singleLine = singleLine,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    cursorColor = ScrapbookDark, focusedTextColor = ScrapbookDark, unfocusedTextColor = ScrapbookDark
                ),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── PlatformInputWithVerify ──────────────────────────────────────────────────

@Composable
fun PlatformInputWithVerify(
    value: String,
    onValueChange: (String) -> Unit,
    platform: GamingPlatform,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var verifyState by remember { mutableStateOf<String?>(null) }
    val profileUrl = when (platform.name) {
        "PlayStation" -> if (value.isNotBlank()) "https://psnprofiles.com/$value" else null
        "Xbox"        -> if (value.isNotBlank()) "https://xboxgamertag.com/search/$value" else null
        "Steam"       -> if (value.isNotBlank()) "https://steamcommunity.com/id/$value" else null
        "Nintendo"    -> if (value.isNotBlank()) "https://www.nintendo.com/search/#q=$value" else null
        else -> null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(platform.color.copy(alpha = 0.15f)).border(1.5.dp, platform.color.copy(alpha = 0.5f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(22.dp))
            }
            RetroTerminalInput(value = value, onValueChange = { onValueChange(it); verifyState = null }, label = platform.name.uppercase(), modifier = Modifier.weight(1f))
            if (value.isNotBlank() && profileUrl != null) {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(when (verifyState) { "ok" -> CGreen.copy(alpha = 0.2f); "fail" -> CAcRed.copy(alpha = 0.2f); else -> platform.color.copy(alpha = 0.15f) })
                    .border(1.dp, when (verifyState) { "ok" -> CGreen; "fail" -> CAcRed; else -> platform.color.copy(alpha = 0.5f) }, RoundedCornerShape(8.dp))
                    .clickable { verifyState = "checking"; try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(profileUrl))); verifyState = "ok" } catch (e: Exception) { verifyState = "fail" } }
                    .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    when (verifyState) {
                        "checking" -> ThreeDotsAnimation(color = platform.color, dotSize = 4.dp)
                        "ok"   -> Icon(Icons.Filled.Check, null, tint = CGreen, modifier = Modifier.size(16.dp))
                        "fail" -> Icon(Icons.Filled.Close, null, tint = CAcRed, modifier = Modifier.size(16.dp))
                        else   -> Text("↗", fontFamily = BangersFontFamily, color = platform.color, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ─── EditCard ─────────────────────────────────────────────────────────────────

@Composable
fun EditCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(14.dp)).padding(16.dp)) {
        Column(content = content)
    }
}

// ─── BioCard ─────────────────────────────────────────────────────────────────

@Composable
fun BioCard(bioText: String, profilePicUrl: String? = null, habboUsername: String = "", habboRegion: String = "habbo.com") {
    val neonAlpha by rememberGlowRange(0.3f, 0.8f)
    var displayedText by remember { mutableStateOf("") }
    var typewriterDone by remember { mutableStateOf(false) }
    LaunchedEffect(bioText) {
        displayedText = ""; typewriterDone = false
        for (i in bioText.indices) { displayedText = bioText.substring(0, i + 1); delay(18L) }
        typewriterDone = true
    }
    val bioTags = remember(bioText) {
        val tags = mutableListOf<Pair<String, String>>()
        val lower = bioText.lowercase()
        if (lower.contains("retro") || lower.contains("vintage")) tags.add("🕹️" to "RETRO")
        if (lower.contains("gamer") || lower.contains("gaming") || lower.contains("game")) tags.add("🎮" to "GAMER")
        if (lower.contains("music") || lower.contains("soundtrack") || lower.contains("ost")) tags.add("🎵" to "MUSIC LOVER")
        if (lower.contains("pixel") || lower.contains("art")) tags.add("🖼️" to "PIXEL ART")
        if (lower.contains("habbo")) tags.add("🏨" to "HABBO")
        if (lower.contains("collector")) tags.add("📦" to "COLLECTOR")
        if (lower.contains("streamer") || lower.contains("twitch") || lower.contains("youtube")) tags.add("📺" to "STREAMER")
        if (lower.contains("developer") || lower.contains("dev") || lower.contains("coder")) tags.add("💻" to "DEVELOPER")
        tags.take(4)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CGreen.copy(alpha = 0.25f), Color.Transparent, CGreen.copy(alpha = 0.12f))))
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                        Text("ABOUT ME", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 2.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { repeat(3) { Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(CGreen.copy(alpha = 0.6f))) } }
                    }
                }
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("\"", fontFamily = BangersFontFamily, color = CGreen.copy(alpha = 0.35f), fontSize = 72.sp, lineHeight = 52.sp, modifier = Modifier.offset(y = (-8).dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = if (typewriterDone) bioText else displayedText, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 15.sp, lineHeight = 23.sp)
                        if (bioTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                bioTags.forEach { tagPair ->
                                    val emoji = tagPair.first; val label = tagPair.second
                                    Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CGreen.copy(alpha = 0.18f)).border(1.dp, CGreen.copy(alpha = 0.55f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text(emoji, fontSize = 10.sp); Text(label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── ActivityStreakSection ────────────────────────────────────────────────────

@Composable
fun ActivityStreakSection(activities: List<ActivityItem>, joinedDate: Long) {
    val streakDays    = remember(activities) { if (activities.isEmpty()) 0 else (activities.size / 2).coerceIn(1, 30) }
    val longestStreak = remember(activities) { (streakDays + (0..5).random()).coerceIn(streakDays, 60) }
    val heatmapData   = remember(activities) {
        List(49) { index -> when {
            index % 7 == 0 -> 0f
            activities.size > index / 3 -> (0.4f + (index % 3) * 0.2f).coerceIn(0f, 1f)
            index % 5 == 0 -> 0.6f; index % 3 == 0 -> 0.3f; else -> 0f
        }}
    }
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚡ ACTIVITY STREAK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreakStatBox("$streakDays", "CURRENT\nSTREAK", "🔥", Modifier.weight(1f))
                    StreakStatBox("$longestStreak", "LONGEST\nSTREAK", "🏆", Modifier.weight(1f))
                    StreakStatBox("${activities.size}", "TOTAL\nACTIVITIES", "📊", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("LAST 7 WEEKS", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M","T","W","T","F","S","S").forEach { day ->
                        Text(day, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (week in 0 until 7) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (day in 0 until 7) {
                                val intensity = heatmapData.getOrElse(week * 7 + day) { 0f }
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp))
                                    .background(when { intensity >= 0.8f -> ScrapbookDark; intensity >= 0.5f -> CGreenDeep; intensity >= 0.2f -> CGreen.copy(alpha = 0.6f); else -> Color.White.copy(alpha = 0.46f) })
                                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.1f), RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Less", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    listOf(Color.White.copy(alpha = 0.46f), CGreen.copy(alpha = 0.6f), CGreenDeep, ScrapbookDark).forEach { color ->
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color).border(0.5.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    }
                    Text("More", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun StreakStatBox(value: String, label: String, emoji: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 10.dp, shadowOffset = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 20.sp); Spacer(modifier = Modifier.height(4.dp))
                RollingCounterText(value, androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = CGreen, fontSize = 24.sp))
                Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
            }
        }
    }
}

// ─── RealActivitySection ─────────────────────────────────────────────────────

@Composable
fun RealActivitySection(
    activities: List<ActivityItem>,
    isLoading: Boolean,
    username: String,
    profilePicUrl: String?,
    onArticleClick: ((ActivityItem) -> Unit)? = null
) {
    when {
        isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Loading activity...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
        }
        activities.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text("No activity yet!\nStart bookmarking or writing articles.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
        else -> Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            activities.take(5).forEach { RealActivityFeedItem(it, username, profilePicUrl, onArticleClick) }
        }
    }
}

@Composable
fun RealActivityFeedItem(
    activity: ActivityItem,
    username: String,
    profilePicUrl: String?,
    onArticleClick: ((ActivityItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activityIcon = when (activity.type) { "ARTICLE" -> Icons.Filled.Create; "FOLLOW" -> Icons.Filled.PersonAdd; "JOINED" -> Icons.Filled.Star; else -> Icons.Filled.Bookmark }
    Box(modifier = modifier.padding(vertical = 6.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ComicGlassBg, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Column {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, CGreen.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                        if (!profilePicUrl.isNullOrBlank()) AsyncImage(model = profilePicUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(profilePicUrl).fillMaxSize())
                        else Icon(Icons.Filled.Person, null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(activityIcon, null, tint = CGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(activity.description, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activity.timeAgo, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                }
                if (activity.type == "ARTICLE" && activity.itemTitle.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 12.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(1.5.dp, CGreen.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .clickable { onArticleClick?.invoke(activity) }.padding(10.dp)
                    ) {
                        Column {
                            Text(activity.itemTitle, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("READ ARTICLE →", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── PinnedArticleSection ─────────────────────────────────────────────────────

@Composable
fun PinnedArticleSection(
    pinnedArticle: ArticleItem?,
    isEditing: Boolean,
    userArticles: List<ArticleItem>,
    onPin: (ArticleItem) -> Unit,
    onUnpin: () -> Unit
) {
    if (pinnedArticle == null && !isEditing) return
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = CGreen.copy(alpha = 0.15f),
            borderColor = CGreenDeep, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📌 PINNED ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                    if (isEditing && pinnedArticle != null) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CAcRed.copy(alpha = 0.15f)).border(1.dp, CAcRed, RoundedCornerShape(6.dp)).clickable { onUnpin() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("UNPIN", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (pinnedArticle != null) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Column {
                            if (!pinnedArticle.imageUrl.isNullOrBlank()) { AsyncImage(model = pinnedArticle.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(pinnedArticle.imageUrl).fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))); Spacer(modifier = Modifier.height(8.dp)) }
                            Text(pinnedArticle.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(pinnedArticle.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        }
                    }
                } else if (isEditing && userArticles.isNotEmpty()) {
                    Text("Pick an article to pin:", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userArticles.take(5).forEach { article ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ComicGlassBg).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onPin(article) }.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📝", fontSize = 16.sp)
                                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else if (isEditing) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("You haven't written any articles yet.\nWrite one to pin it here!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

// ─── Legacy aliases ───────────────────────────────────────────────────────────

@Composable fun ProfileSectionHeader(title: String, emoji: String, color: Color = CGreen) { ScrapbookSectionHeader(title, emoji) }
@Composable fun ProfileSectionTitle(title: String) { Text(title.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
@Composable fun RetroInputField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) { ScrapbookInputField(value, onValueChange, label, modifier) }
@Composable fun XPProgressBar(xp: Int, level: RetroLevel, progress: Float) { ScrapbookXPProgressBar(xp, level, progress, getXpToNextLevel(xp)) }
@Composable fun StatCard(value: String, label: String, color: Color = CGreen, onClick: () -> Unit) { ScrapbookStatCard(value, label, onClick = onClick) }
@Composable fun PlatformInputField(value: String, onValueChange: (String) -> Unit, platform: GamingPlatform, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        ScrapbookInputField(value, onValueChange, platform.name, Modifier.weight(1f))
    }
}

// ─── YoutubePlayerCard ───────────────────────────────────────────────────────

@Composable
fun YoutubePlayerCard(
    youtubeVideoId: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
) {
    var youTubePlayerRef by remember { mutableStateOf<com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer?>(null) }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView(ctx).apply {
                lifecycleOwner.lifecycle.addObserver(this)
                addYouTubePlayerListener(object : com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                        youTubePlayerRef = youTubePlayer
                        if (autoPlay) {
                            youTubePlayer.loadVideo(youtubeVideoId, 0f)
                        } else {
                            youTubePlayer.cueVideo(youtubeVideoId, 0f)
                        }
                    }
                })
            }
        },
        update = { _ ->
            youTubePlayerRef?.let { player ->
                if (autoPlay) player.loadVideo(youtubeVideoId, 0f)
                else player.cueVideo(youtubeVideoId, 0f)
            }
        }
    )
}

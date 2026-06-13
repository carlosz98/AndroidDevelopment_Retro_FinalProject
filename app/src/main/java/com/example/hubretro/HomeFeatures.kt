package com.example.hubretro

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.data.models.NewsItem
import com.example.hubretro.ui.news.NewsViewModel
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectTapGestures

// ─── Constants ────────────────────────────────────────────────────────────────

val ALBUMS_CARD_IMAGE = R.drawable.ostcover6
val MAGAZINES_CARD_IMAGE = R.drawable.cover1
val ARTICLES_CARD_IMAGE = R.drawable.article1
val PROFILE_CARD_IMAGE = R.drawable.p1
val STREAMS_CARD_IMAGE = R.drawable.article1
val GAMES_CARD_IMAGE = R.drawable.game1

private const val UNSPLASH_ACCESS_KEY = "XA9XiS2ImfdYo10GVl2mSUQgut4-vPFS2FHKacYR8sA"

// ─── Data Models ──────────────────────────────────────────────────────────────

data class UnsplashPhoto(
    val imageUrl: String,
    val photographerName: String,
    val photographerUsername: String,
    val description: String
)

data class HomeStats(
    val userCount: Int = 0,
    val articleCount: Int = 0,
    val albumCount: Int = sampleAlbums.size
)

data class StatItemData(
    val emoji: String,
    val count: Int,
    val label: String,
    val tagline: String,
    val accentColor: Color,
    val glowColor: Color
)

data class NavCardData(
    val title: String,
    val emoji: String,
    val subtitle: String,
    val tagline: String,
    @DrawableRes val imageResId: Int,
    val accentColor: Color,
    val isComingSoon: Boolean = false,
    val onClick: () -> Unit
)

// ─── Content Lists ────────────────────────────────────────────────────────────

val retroFacts = listOf(
    "The first commercially sold video game was Computer Space in 1971.",
    "The original Game Boy had a battery life of about 15 hours.",
    "Pac-Man was originally called Puck Man in Japan.",
    "The NES was released in Japan as the Famicom in 1983.",
    "Super Mario Bros. was designed to be easy to pick up and hard to put down.",
    "The first Easter egg in a video game was hidden in Atari's Adventure (1980).",
    "Tetris was designed by Soviet software engineer Alexey Pajitnov in 1984.",
    "The SNES had a 16-bit processor running at 3.58 MHz.",
    "Doom (1993) was so popular it was installed on more PCs than Windows 95.",
    "The PlayStation was originally designed as a CD add-on for the Super Nintendo.",
    "Space Invaders caused a coin shortage in Japan when it launched in 1978.",
    "The first 3D video game was Battlezone, released by Atari in 1980.",
    "Legend of Zelda cartridges had a battery inside to save game data.",
    "Sonic the Hedgehog was designed to rival Mario's popularity.",
    "The N64 controller had an analog stick — revolutionary for its time."
)

val retroQuotes = listOf(
    "\"It's dangerous to go alone! Take this.\" — The Legend of Zelda",
    "\"Do a barrel roll!\" — Star Fox 64",
    "\"The cake is a lie.\" — Portal",
    "\"Stay a while and listen.\" — Diablo II",
    "\"Hey! Listen!\" — Navi, Ocarina of Time",
    "\"War. War never changes.\" — Fallout",
    "\"I used to be an adventurer like you...\" — Skyrim Guard",
    "\"It's super effective!\" — Pokémon",
    "\"Thank you Mario! But our princess is in another castle!\" — Super Mario Bros",
    "\"Rise from your grave!\" — Altered Beast"
)

val todayInRetroGaming = listOf(
    "On this day in 1985, Super Mario Bros. launched in Japan and changed gaming forever.",
    "On this day in 1989, the Game Boy was released — 118 million units would follow.",
    "On this day in 1991, Sonic the Hedgehog debuted on the Sega Genesis.",
    "On this day in 1996, the Nintendo 64 launched in Japan with Super Mario 64.",
    "On this day in 1998, The Legend of Zelda: Ocarina of Time released to universal acclaim.",
    "On this day in 1993, Doom was released as shareware and defined a generation of shooters.",
    "On this day in 1994, the PlayStation launched in Japan, selling 100,000 units in one day.",
    "On this day in 2001, the Game Boy Advance launched with 6 titles.",
    "On this day in 1977, the Atari 2600 launched — the first truly successful home console.",
    "On this day in 1980, Pac-Man made its arcade debut in Japan."
)

val tickerItems = listOf(
    "🎮 Space Invaders (1978)",
    "⭐ Pac-Man sold 400,000 cabinets",
    "🕹️ Atari 2600 — 1977",
    "🏆 Super Mario Bros — 40M copies",
    "🎵 Final Fantasy VII OST — iconic",
    "📺 Nintendo sold 61M NES units",
    "🔥 Sonic vs Mario — the great rivalry",
    "💾 Zelda had the first save battery",
    "🌟 Tetris — 500M+ copies sold",
    "🎲 GoldenEye 007 — FPS legend",
    "👾 Doom defined the shooter genre",
    "🕹️ Game Boy — 118M units sold"
)

// ─── Network Helpers ──────────────────────────────────────────────────────────

suspend fun fetchUnsplashRetroPhoto(): UnsplashPhoto? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.unsplash.com/photos/random?query=retro+gaming+vintage+arcade&orientation=landscape&client_id=$UNSPLASH_ACCESS_KEY")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext null
        val json = JSONObject(body)
        val urls = json.getJSONObject("urls")
        val user = json.getJSONObject("user")
        val description = json.optString("description")
            .ifBlank { json.optString("alt_description") }
            .ifBlank { "Retro Gaming" }
        UnsplashPhoto(
            imageUrl = urls.getString("regular"),
            photographerName = user.getString("name"),
            photographerUsername = user.getString("username"),
            description = description.replaceFirstChar { it.uppercase() }.take(60)
        )
    } catch (e: Exception) {
        Log.e("Unsplash", "Error: ${e.message}")
        null
    }
}

suspend fun fetchHomeStats(): HomeStats = withContext(Dispatchers.IO) {
    try {
        val firestore = FirebaseFirestore.getInstance()
        val users = firestore.collection("users").get().await().size()
        val articles = firestore.collection("articles").get().await().size()
        HomeStats(userCount = users, articleCount = articles, albumCount = sampleAlbums.size)
    } catch (e: Exception) { HomeStats() }
}

fun formatEpochMillisToReadableDate(epochMillis: Long): String {
    return try {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(epochMillis))
    } catch (e: Exception) { "Date N/A" }
}
// ─── ScrapbookCard ────────────────────────────────────────────────────────────

@Composable
fun ScrapbookCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ScrapbookCardWhite,
    borderColor: Color = ScrapbookBorder,
    borderWidth: Dp = 2.dp,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .offset(x = shadowOffset, y = shadowOffset)
            .background(ScrapbookShadow.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius))
    ) {
        Box(
            modifier = Modifier
                .offset(x = -shadowOffset, y = -shadowOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(backgroundColor)
                .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius)),
            content = content
        )
    }
}

// ─── ShimmerBox ───────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Dp = 12.dp) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerOffset"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(ScrapbookPaper, Color.White.copy(alpha = 0.9f), ScrapbookPaper),
                    start = androidx.compose.ui.geometry.Offset(shimmerOffset * 1000f, 0f),
                    end = androidx.compose.ui.geometry.Offset((shimmerOffset + 1f) * 1000f, 0f)
                )
            )
    )
}

// ─── PulsingDot ───────────────────────────────────────────────────────────────

@Composable
fun PulsingDot(color: Color = Color(0xFF00C853), size: Dp = 10.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ─── WavePixelDivider ─────────────────────────────────────────────────────────

@Composable
fun WavePixelDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val baseColors = listOf(
            ScrapbookDark, ScrapbookYellow, ScrapbookDark, ScrapbookYellow,
            ScrapbookDark, ScrapbookYellow, ScrapbookDark, ScrapbookYellow
        )
        repeat(40) { i ->
            val sinValue = kotlin.math.sin(i.toDouble() * 0.8).toFloat()
            val heightFraction = if (sinValue < 0f) -sinValue else sinValue
            val pixelHeight = (4f + heightFraction * 10f).dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(pixelHeight)
                    .align(Alignment.CenterVertically)
                    .background(baseColors[i % baseColors.size])
            )
        }
    }
}

// ─── FloatingEmojiDivider (static) ───────────────────────────────────────────

@Composable
fun FloatingEmojiDivider(emojis: List<String> = listOf("★", "🎮", "★", "🕹️", "★")) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                fontSize = if (emoji == "🎮" || emoji == "🕹️") 20.sp else 14.sp,
                color = if (emoji == "★") ScrapbookYellow else Color.Unspecified,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    }
}

// ─── RetroTickerTape ──────────────────────────────────────────────────────────

@Composable
fun RetroTickerTape() {
    val tickerText = tickerItems.joinToString("   ★   ")
    val transition = rememberInfiniteTransition(label = "ticker")
    val offset by transition.animateFloat(
        initialValue = 1f, targetValue = -2f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "tickerOffset"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScrapbookDark)
            .border(BorderStroke(1.dp, ScrapbookYellow.copy(alpha = 0.3f)))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .offset(x = (offset * 400f).dp)
        ) {
            repeat(2) {
                Text(
                    text = tickerText,
                    fontFamily = BangersFontFamily,
                    color = ScrapbookYellow,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ─── StaggeredSection ─────────────────────────────────────────────────────────

@Composable
fun StaggeredSection(index: Int, content: @Composable () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index * 100L); started = true }
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(400),
        label = "section_alpha_$index"
    )
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) { content() }
}

// ─── AnimatedCounter ──────────────────────────────────────────────────────────

@Composable
fun AnimatedCounter(target: Int, durationMs: Int = 1200): Int {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(target) {
        val steps = 35
        val stepDelay = durationMs / steps
        val increment = target / steps.toFloat()
        repeat(steps) { i ->
            delay(stepDelay.toLong())
            count = ((i + 1) * increment).toInt().coerceAtMost(target)
        }
        count = target
    }
    return count
}

// ─── ClickTransitionOverlay ───────────────────────────────────────────────────

@Composable
fun ClickTransitionOverlay(visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.35f else 0f,
        animationSpec = tween(180, easing = EaseOut),
        label = "overlayAlpha"
    )
    if (alpha > 0f) {
        Box(modifier = Modifier.fillMaxSize().background(ScrapbookYellow.copy(alpha = alpha)))
    }
}

// ─── HomeSectionTitle (static) ────────────────────────────────────────────────

@Composable
fun HomeSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ScrapbookYellow)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 27.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
// ─── HomeStatCard ─────────────────────────────────────────────────────────────

@Composable
fun HomeStatCard(item: StatItemData, isLoading: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "statEnter_${item.label}"
    )
    val enterOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(400, easing = LinearOutSlowInEasing),
        label = "statOffset_${item.label}"
    )
    LaunchedEffect(Unit) { visible = true }

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "statPress_${item.label}"
    )

    Box(
        modifier = modifier
            .offset(y = enterOffset.dp)
            .graphicsLayer { alpha = enterAlpha }
            .scale(pressScale)
            .clickable { pressed = true }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(item.accentColor.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ScrapbookDark)
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            item.glowColor.copy(alpha = 0.7f),
                            item.accentColor.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.2f))
                        .border(1.5.dp, item.glowColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(item.emoji, fontSize = 22.sp) }
                if (isLoading) {
                    ShimmerBox(modifier = Modifier.width(52.dp).height(30.dp), cornerRadius = 6.dp)
                } else {
                    val animCount = AnimatedCounter(target = item.count)
                    Text(
                        text = formatCount(animCount),
                        fontFamily = BangersFontFamily,
                        color = item.glowColor,
                        fontSize = 30.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Text(item.label, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center, maxLines = 1)
                Text(item.tagline, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── HomeStatsSection ─────────────────────────────────────────────────────────

@Composable
fun HomeStatsSection(stats: HomeStats, isLoading: Boolean) {
    var gamesCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            gamesCount = FirebaseFirestore.getInstance().collection("games").get().await().size()
        } catch (e: Exception) { gamesCount = 128 }
    }

    val statItems = listOf(
        StatItemData("👥", if (isLoading) 0 else stats.userCount, "EXPLORERS", "retro fans", Color(0xFF1565C0), Color(0xFF64B5F6)),
        StatItemData("📝", if (isLoading) 0 else stats.articleCount, "ARTICLES", "written", Color(0xFF6A1B9A), Color(0xFFCE93D8)),
        StatItemData("🎵", if (isLoading) 0 else stats.albumCount, "ALBUMS", "soundtracks", Color(0xFF2E7D32), Color(0xFF81C784)),
        StatItemData("🎮", if (isLoading) 0 else gamesCount, "GAMES", "tracked", Color(0xFFBF360C), Color(0xFFFF8A65))
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookYellow))
                Text("📊 BY THE NUMBERS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, letterSpacing = 1.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ScrapbookDark)
                    .border(1.5.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("🕹️", fontSize = 11.sp)
                    Text("EST. 2026", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeStatCard(statItems[0], isLoading, Modifier.weight(1f))
                HomeStatCard(statItems[1], isLoading, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeStatCard(statItems[2], isLoading, Modifier.weight(1f))
                HomeStatCard(statItems[3], isLoading, Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ScrapbookDark)
                .border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulsingDot(color = Color(0xFF00C853), size = 9.dp)
                    Text("RETROHUB IS LIVE", fontFamily = BangersFontFamily, color = Color(0xFF00C853), fontSize = 13.sp, letterSpacing = 1.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("🌍", fontSize = 13.sp)
                    Text(
                        text = if (isLoading) "loading..." else "${formatCount(stats.userCount)} members strong",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
// ─── HomeScreen ───────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStreams: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    onNavigateToGames: () -> Unit = {},
    onNavigateToRetroBytes: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToMarketplace: () -> Unit = {},
    newsViewModel: NewsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val newsItemsList by newsViewModel.newsItems.collectAsState()
    val isLoadingNews by newsViewModel.isLoading.collectAsState()
    val newsErrorMessage by newsViewModel.error.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var currentQuoteIndex by remember { mutableStateOf(retroQuotes.indices.random()) }
    var todayFactIndex by remember { mutableStateOf(todayInRetroGaming.indices.random()) }
    var unsplashPhoto by remember { mutableStateOf<UnsplashPhoto?>(null) }
    var isLoadingPhoto by remember { mutableStateOf(true) }
    var homeStats by remember { mutableStateOf(HomeStats()) }
    var isLoadingStats by remember { mutableStateOf(true) }
    var showClickOverlay by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun navigateWithTransition(action: () -> Unit) {
        pendingNavAction = action
        showClickOverlay = true
    }

    LaunchedEffect(showClickOverlay) {
        if (showClickOverlay) {
            delay(180); showClickOverlay = false
            delay(60); pendingNavAction?.invoke(); pendingNavAction = null
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchAllUsers()
        isLoadingPhoto = true
        unsplashPhoto = fetchUnsplashRetroPhoto()
        isLoadingPhoto = false
        homeStats = fetchHomeStats()
        isLoadingStats = false
    }

    LaunchedEffect(Unit) {
        while (true) { delay(15000L); currentQuoteIndex = (currentQuoteIndex + 1) % retroQuotes.size }
    }

    val recentActivity = remember(allUsers) { allUsers.take(6) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrapbookCream)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            MagazineCoverHero(
                photo = unsplashPhoto,
                isLoading = isLoadingPhoto,
                onNavigateToDiscover = { navigateWithTransition(onNavigateToDiscover) }
            )
            RetroTickerTape()
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 0) {
                    HomeStatsSection(stats = homeStats, isLoading = isLoadingStats)
                }
                Spacer(modifier = Modifier.height(24.dp))
                FloatingEmojiDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 1) {
                    TodayInRetroSection(
                        fact = todayInRetroGaming[todayFactIndex],
                        onNext = { todayFactIndex = (todayFactIndex + 1) % todayInRetroGaming.size }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 2) {
                    RetroQuoteCard(quote = retroQuotes[currentQuoteIndex])
                }
                Spacer(modifier = Modifier.height(24.dp))
                WavePixelDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 3) {
                    ExploreRetroHubSection(
                        onNavigateToAlbums = { navigateWithTransition(onNavigateToAlbums) },
                        onNavigateToMagazines = { navigateWithTransition(onNavigateToMagazines) },
                        onNavigateToArticles = { navigateWithTransition(onNavigateToArticles) },
                        onNavigateToProfile = { navigateWithTransition(onNavigateToProfile) },
                        onNavigateToGames = { navigateWithTransition(onNavigateToGames) },
                        onNavigateToStreams = { navigateWithTransition(onNavigateToStreams) },
                        onNavigateToEvents = { navigateWithTransition(onNavigateToEvents) },
                        onNavigateToMarketplace = { navigateWithTransition(onNavigateToMarketplace) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                FloatingEmojiDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 4) {
                    HomeGameOfDaySection(
                        onNavigateToGames = { navigateWithTransition(onNavigateToGames) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                if (recentActivity.isNotEmpty()) {
                    StaggeredSection(index = 5) {
                        CommunityActivitySection(
                            users = recentActivity,
                            onUserTap = { navigateWithTransition(onNavigateToDiscover) }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                WavePixelDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 6) {
                    Column {
                        HomeSectionTitle(title = "🎵 FEATURED ALBUMS")
                        Spacer(modifier = Modifier.height(10.dp))
                        FeaturedAlbumsCarousel(onNavigateToAlbums = { navigateWithTransition(onNavigateToAlbums) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 7) {
                    Column {
                        HomeSectionTitle(title = "📰 FEATURED MAGAZINES")
                        Spacer(modifier = Modifier.height(10.dp))
                        FeaturedMagazinesCarousel(onNavigateToMagazines = { navigateWithTransition(onNavigateToMagazines) })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                FloatingEmojiDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 8) {
                    FeaturedStreamsSection(onNavigateToStreams = { navigateWithTransition(onNavigateToStreams) })
                }
                Spacer(modifier = Modifier.height(24.dp))
                WavePixelDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 9) {
                    HomeRetroBytesPreview(onOpenFeed = { navigateWithTransition(onNavigateToRetroBytes) })
                }
                Spacer(modifier = Modifier.height(24.dp))
                WavePixelDivider()
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 10) {
                    NewsSection(
                        newsItems = newsItemsList,
                        isLoading = isLoadingNews,
                        errorMessage = newsErrorMessage,
                        onRetry = { newsViewModel.fetchNews() }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                StaggeredSection(index = 11) {
                    CopyrightFooter(
                        name = "Carlos Zabala",
                        blogUrl = "https://charlysblog.framer.website"
                    )
                }
            }
        }
        ClickTransitionOverlay(visible = showClickOverlay)
    }
}
// ─── MagazineCoverHero ────────────────────────────────────────────────────────

@Composable
fun MagazineCoverHero(
    photo: UnsplashPhoto?,
    isLoading: Boolean,
    onNavigateToDiscover: () -> Unit
) {
    val today = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }
    val issueNumber = remember {
        val cal = Calendar.getInstance()
        "VOL.${cal.get(Calendar.YEAR)} NO.${cal.get(Calendar.DAY_OF_YEAR)}"
    }
    val btnT = rememberInfiniteTransition(label = "heroBtn")
    val btnScale by btnT.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse),
        label = "heroBtnScale"
    )

    Box(modifier = Modifier.fillMaxWidth().height(540.dp).background(ScrapbookCream)) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp).border(3.dp, ScrapbookDark, RoundedCornerShape(4.dp))) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Masthead
                Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 44.sp, letterSpacing = 5.sp, lineHeight = 46.sp)
                            Text(issueNumber, fontFamily = NunitoFontFamily, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            listOf("RETRO", "GAMING", "UNIVERSE").forEachIndexed { i, word ->
                                Text(word, fontFamily = BangersFontFamily, color = if (i == 2) ScrapbookYellow else Color.White, fontSize = 12.sp, letterSpacing = 3.sp)
                            }
                        }
                    }
                }
                // Date strip
                Box(modifier = Modifier.fillMaxWidth().background(ScrapbookYellow).padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(today.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(ScrapbookDark).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("DAILY EDITION", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                }
                // Photo area
                Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))) {
                    when {
                        isLoading -> {
                            ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
                            Box(modifier = Modifier.fillMaxSize().background(ScrapbookDark.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ScrapbookYellow, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Loading today's cover...", fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookDark, fontSize = 15.sp)
                                }
                            }
                        }
                        photo != null -> {
                            val kbT = rememberInfiniteTransition(label = "heroKB")
                            val heroScale by kbT.animateFloat(
                                initialValue = 1f, targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(keyframes { durationMillis = 16000; 1f at 0; 1.08f at 8000; 1f at 16000 }, RepeatMode.Restart),
                                label = "heroScale"
                            )
                            val heroPanX by kbT.animateFloat(
                                initialValue = -10f, targetValue = 10f,
                                animationSpec = infiniteRepeatable(keyframes { durationMillis = 20000; -10f at 0; 10f at 10000; -10f at 20000 }, RepeatMode.Restart),
                                label = "heroPanX"
                            )
                            AsyncImage(
                                model = photo.imageUrl,
                                contentDescription = photo.description,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().scale(heroScale).offset(x = heroPanX.dp)
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)), startY = 160f)))
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("📷 ${photo.photographerName}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 11.sp)
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookYellow).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("TODAY'S COVER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, letterSpacing = 2.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(photo.description.ifBlank { "Retro Gaming Daily" }.replaceFirstChar { it.uppercase() }, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp)).clickable { onNavigateToDiscover() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                    Text("EXPLORE NOW →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(ScrapbookDark, Color(0xFF2A2A4A)))), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎮", fontSize = 72.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Your retro gaming universe", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 24.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp)).clickable { onNavigateToDiscover() }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                        Text("EXPLORE NOW →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("★", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 20.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp))
    }
}

// ─── TodayInRetroSection ──────────────────────────────────────────────────────

@Composable
fun TodayInRetroSection(fact: String, onNext: () -> Unit) {
    val today = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, cornerRadius = 14.dp, shadowOffset = 5.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(ScrapbookYellow).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📅", fontSize = 20.sp)
                            Text("TODAY IN RETRO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                        }
                        IconButton(onClick = onNext, modifier = Modifier.size(30.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.12f))) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Next", tint = ScrapbookDark, modifier = Modifier.size(17.dp))
                        }
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookYellow.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(today.uppercase(), fontFamily = NunitoFontFamily, color = ScrapbookYellow, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(fact, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp, lineHeight = 27.sp)
                }
            }
        }
    }
}

// ─── RetroQuoteCard ───────────────────────────────────────────────────────────

@Composable
fun RetroQuoteCard(quote: String) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookYellow, cornerRadius = 12.dp, shadowOffset = 5.dp) {
            Row(modifier = Modifier.padding(16.dp)) {
                Text("\"", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.18f), fontSize = 90.sp, lineHeight = 65.sp, modifier = Modifier.offset(y = (-10).dp))
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("QUOTE OF THE MOMENT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, letterSpacing = 2.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(quote, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookDark, fontSize = 17.sp, lineHeight = 25.sp, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}
// ─── ExploreRetroHubSection ───────────────────────────────────────────────────

@Composable
fun ExploreRetroHubSection(
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToGames: () -> Unit,
    onNavigateToStreams: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToMarketplace: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // All nav cards defined here
    val defaultCards = listOf(
        NavCardData("ALBUMS", "🎵", "Retro soundtracks", "500+ OSTs", ALBUMS_CARD_IMAGE, ScrapbookYellow, false, onNavigateToAlbums),
        NavCardData("MAGAZINES", "📰", "Vintage issues", "Classic gaming press", MAGAZINES_CARD_IMAGE, ScrapbookOrange, false, onNavigateToMagazines),
        NavCardData("ARTICLES", "📝", "Community writes", "Retro stories", ARTICLES_CARD_IMAGE, ScrapbookBlue, false, onNavigateToArticles),
        NavCardData("PROFILE", "👤", "Your corner", "Your retro identity", PROFILE_CARD_IMAGE, ScrapbookPurple, false, onNavigateToProfile)
    )

    val extraCards = listOf(
        NavCardData("GAMES", "🎮", "Browse classics", "IGDB powered", GAMES_CARD_IMAGE, Color(0xFF00838F), false, onNavigateToGames),
        NavCardData("STREAMS", "📺", "Watch live", "Twitch & YouTube", ALBUMS_CARD_IMAGE, Color(0xFF9146FF), false, onNavigateToStreams),
        NavCardData("EVENTS", "🎪", "Coming soon", "Retro gaming events", MAGAZINES_CARD_IMAGE, Color(0xFFE53935), true, onNavigateToEvents),
        NavCardData("MARKETPLACE", "🛒", "Coming soon", "Trade retro games", ARTICLES_CARD_IMAGE, Color(0xFF2E7D32), true, onNavigateToMarketplace)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookYellow))
                Text("🕹️ EXPLORE RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 1.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ScrapbookDark)
                    .border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("${defaultCards.size + extraCards.size} SECTIONS", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }

        // Default 4 cards (always visible) — 2x2 grid
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VisualNavCard(card = defaultCards[0], modifier = Modifier.weight(1f))
                VisualNavCard(card = defaultCards[1], modifier = Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VisualNavCard(card = defaultCards[2], modifier = Modifier.weight(1f))
                VisualNavCard(card = defaultCards[3], modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Extra cards — staggered pop in when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300, easing = LinearOutSlowInEasing)) + fadeIn(tween(300)),
            exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(0.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    extraCards.take(2).forEachIndexed { index, card ->
                        var cardVisible by remember { mutableStateOf(false) }
                        val cardAlpha by animateFloatAsState(
                            targetValue = if (cardVisible) 1f else 0f,
                            animationSpec = tween(300, delayMillis = index * 80),
                            label = "extraCardAlpha_$index"
                        )
                        val cardOffset by animateFloatAsState(
                            targetValue = if (cardVisible) 0f else 20f,
                            animationSpec = tween(300, delayMillis = index * 80, easing = LinearOutSlowInEasing),
                            label = "extraCardOffset_$index"
                        )
                        LaunchedEffect(isExpanded) { if (isExpanded) { delay(index * 80L); cardVisible = true } else cardVisible = false }
                        Box(modifier = Modifier.weight(1f).offset(y = cardOffset.dp).graphicsLayer { alpha = cardAlpha }) {
                            VisualNavCard(card = card, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    extraCards.drop(2).forEachIndexed { index, card ->
                        var cardVisible by remember { mutableStateOf(false) }
                        val cardAlpha by animateFloatAsState(
                            targetValue = if (cardVisible) 1f else 0f,
                            animationSpec = tween(300, delayMillis = (index + 2) * 80),
                            label = "extraCardAlpha2_$index"
                        )
                        val cardOffset by animateFloatAsState(
                            targetValue = if (cardVisible) 0f else 20f,
                            animationSpec = tween(300, delayMillis = (index + 2) * 80, easing = LinearOutSlowInEasing),
                            label = "extraCardOffset2_$index"
                        )
                        LaunchedEffect(isExpanded) { if (isExpanded) { delay((index + 2) * 80L); cardVisible = true } else cardVisible = false }
                        Box(modifier = Modifier.weight(1f).offset(y = cardOffset.dp).graphicsLayer { alpha = cardAlpha }) {
                            VisualNavCard(card = card, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Expand/collapse button
        var btnPressed by remember { mutableStateOf(false) }
        val btnScale by animateFloatAsState(
            targetValue = if (btnPressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "exploreExpandBtnScale"
        )
        val arrowRotation by animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = tween(300, easing = EaseInOut),
            label = "exploreArrowRotation"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .scale(btnScale)
                .clip(RoundedCornerShape(12.dp))
                .background(ScrapbookDark)
                .border(1.5.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clickable { btnPressed = true; isExpanded = !isExpanded }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isExpanded) "SHOW LESS" else "SHOW MORE SECTIONS",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookYellow,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ScrapbookYellow,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }
        LaunchedEffect(btnPressed) { if (btnPressed) { delay(150); btnPressed = false } }
    }
}

// ─── VisualNavCard (tilt + scale on press, count badge, EXPLORE pill, COMING SOON) ───

@Composable
fun VisualNavCard(
    card: NavCardData,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navCardScale_${card.title}"
    )
    val animTiltX by animateFloatAsState(
        targetValue = if (pressed) tiltX else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tiltX_${card.title}"
    )
    val animTiltY by animateFloatAsState(
        targetValue = if (pressed) tiltY else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tiltY_${card.title}"
    )

    Box(
        modifier = modifier
            .scale(cardScale)
            .graphicsLayer {
                rotationX = animTiltX
                rotationY = animTiltY
                cameraDistance = 12f * density
            }
    ) {
        // Colored shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(card.accentColor.copy(alpha = 0.3f))
        )

        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            tiltX = ((offset.y - centerY) / centerY * -6f).coerceIn(-6f, 6f)
                            tiltY = ((offset.x - centerX) / centerX * 6f).coerceIn(-6f, 6f)
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                            if (!card.isComingSoon) card.onClick()
                        }
                    )
                },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 14.dp,
            shadowOffset = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background image
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                    Image(
                        painter = painterResource(id = card.imageResId),
                        contentDescription = card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.4f
                    )
                }

                // Color overlay
                Box(modifier = Modifier.fillMaxSize().background(card.accentColor.copy(alpha = 0.55f)))

                // Bottom gradient
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))

                // Diagonal stripe texture
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.04f), Color.White.copy(alpha = 0f)),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(300f, 300f)
                        )
                    )
                )

                // Top left — emoji circle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(card.emoji, fontSize = 20.sp) }

                // Top right — EXPLORE pill or COMING SOON
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (card.isComingSoon) Color.Black.copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (card.isComingSoon) ScrapbookYellow.copy(alpha = 0.6f)
                            else Color.White.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (card.isComingSoon) "SOON" else "EXPLORE →",
                        fontFamily = BangersFontFamily,
                        color = if (card.isComingSoon) ScrapbookYellow else Color.White,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Bottom info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Tagline / content count
                    Text(
                        text = card.tagline,
                        fontFamily = NunitoFontFamily,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = card.title,
                        fontFamily = BangersFontFamily,
                        color = Color.White,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp,
                        lineHeight = 26.sp
                    )
                }

                // COMING SOON overlay
                if (card.isComingSoon) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}
// ─── HomeGameOfDaySection ─────────────────────────────────────────────────────

@Composable
fun HomeGameOfDaySection(onNavigateToGames: () -> Unit = {}) {
    var game by remember { mutableStateOf<RetroGameOfDay?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isLoading = true
        game = fetchRetroGameOfDay()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookYellow))
                Text("🎮 GAME OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 1.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("👑", fontSize = 11.sp)
                    Text("DAILY PICK", fontFamily = BangersFontFamily, color = Color(0xFFFFD700), fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            when {
                isLoading -> ShimmerBox(modifier = Modifier.fillMaxWidth().height(380.dp), cornerRadius = 16.dp)
                game != null -> RetroGameOfDayCard(game = game!!, onViewInDatabase = onNavigateToGames)
                else -> { }
            }
        }
    }
}

// ─── RetroGameOfDayCard (movie poster layout) ─────────────────────────────────

@Composable
fun DiscoverGameOfDayCard(game: RetroGameOfDay, onViewInDatabase: () -> Unit) {

    // Platform badge from release year
    val platformBadge = remember(game.releaseYear) {
        when {
            (game.releaseYear ?: 0) < 1984 -> Pair("🕹️", "ARCADE ERA")
            (game.releaseYear ?: 0) in 1985..1990 -> Pair("🟥", "NES ERA")
            (game.releaseYear ?: 0) in 1991..1995 -> Pair("🟣", "16-BIT ERA")
            (game.releaseYear ?: 0) in 1996..2000 -> Pair("💿", "PS1 / N64 ERA")
            (game.releaseYear ?: 0) in 2001..2005 -> Pair("🔵", "PS2 ERA")
            else -> Pair("🎮", "RETRO ERA")
        }
    }

    val platformColor = remember(game.releaseYear) {
        when {
            (game.releaseYear ?: 0) < 1984 -> Color(0xFFEF6C00)
            (game.releaseYear ?: 0) in 1985..1990 -> Color(0xFFE4000F)
            (game.releaseYear ?: 0) in 1991..1995 -> Color(0xFF7B2FBE)
            (game.releaseYear ?: 0) in 1996..2000 -> Color(0xFF003087)
            (game.releaseYear ?: 0) in 2001..2005 -> Color(0xFF00439C)
            else -> ScrapbookYellow
        }
    }

    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "gotdPressScale"
    )

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "gotdEnterAlpha"
    )
    val enterOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "gotdEnterOffset"
    )
    LaunchedEffect(Unit) { delay(100); visible = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = enterOffset.dp)
            .graphicsLayer { alpha = enterAlpha }
            .scale(pressScale)
    ) {
        // Colored shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(platformColor.copy(alpha = 0.2f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ScrapbookDark)
                .border(2.dp, platformColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { pressed = true; onViewInDatabase() }
        ) {
            Column {
                // ─── Hero cover image ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    if (!game.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(colors = listOf(platformColor.copy(alpha = 0.4f), ScrapbookDark))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = platformColor, fontSize = 56.sp)
                        }
                    }

                    // Scanline texture
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(40) {
                            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.05f)))
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }

                    // Bottom gradient
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.9f)))))

                    // Top left badges
                    Row(
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Game of the Day crown badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFD700))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("👑", fontSize = 10.sp)
                                Text("GAME OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        }

                        // Platform era badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(platformColor.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${platformBadge.first} ${platformBadge.second}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 0.5.sp)
                        }
                    }

                    // Top right — rating
                    game.rating?.let { rating ->
                        val ratingColor = when {
                            rating >= 80 -> Color(0xFF00C853)
                            rating >= 60 -> Color(0xFFFFB300)
                            else -> Color(0xFFFF5252)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ratingColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${(rating / 10).toInt()}/10", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Bottom of image — game name
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = game.name,
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 26.sp,
                            lineHeight = 30.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        game.releaseYear?.let {
                            Text(
                                text = "$it",
                                fontFamily = NunitoFontFamily,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ─── Info panel below image ───────────────────────────────────
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Rating bar
                    game.rating?.let { rating ->
                        val ratingColor = when {
                            rating >= 80 -> Color(0xFF00C853)
                            rating >= 60 -> Color(0xFFFFB300)
                            else -> Color(0xFFFF5252)
                        }
                        val ratingLabel = when {
                            rating >= 80 -> "⭐ OUTSTANDING"
                            rating >= 70 -> "👍 GREAT"
                            rating >= 60 -> "✅ GOOD"
                            else -> "😐 MIXED"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ratingLabel, fontFamily = BangersFontFamily, color = ratingColor, fontSize = 14.sp)
                                Text("${String.format("%.1f", rating)}/100", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ratingColor, fontSize = 12.sp)
                            }
                            val animRating by animateFloatAsState(
                                targetValue = (rating / 100.0).toFloat().coerceIn(0f, 1f),
                                animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                                label = "gotdRatingBar"
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.1f))) {
                                Box(modifier = Modifier.fillMaxWidth(animRating).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(colors = listOf(ratingColor.copy(alpha = 0.7f), ratingColor))))
                            }
                        }
                    }

                    // Summary
                    game.summary?.let { summary ->
                        Text(
                            text = summary,
                            fontFamily = NunitoFontFamily,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Divider
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

                    // CTA button
                    var btnPressed by remember { mutableStateOf(false) }
                    val btnScale by animateFloatAsState(
                        targetValue = if (btnPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "gotdBtnScale"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(btnScale)
                            .clip(RoundedCornerShape(10.dp))
                            .background(platformColor.copy(alpha = 0.15f))
                            .border(1.5.dp, platformColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .clickable { btnPressed = true; onViewInDatabase() }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎮", fontSize = 16.sp)
                            Text("VIEW IN DATABASE →", fontFamily = BangersFontFamily, color = platformColor, fontSize = 15.sp, letterSpacing = 1.sp)
                        }
                    }
                    LaunchedEffect(btnPressed) { if (btnPressed) { delay(150); btnPressed = false } }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}
// ─── CommunityActivitySection ─────────────────────────────────────────────────

@Composable
fun CommunityActivitySection(users: List<UserProfileData>, onUserTap: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionTitle(title = "👥 COMMUNITY")
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 14.dp, shadowOffset = 5.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("WHO'S ON RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                            Text("Fellow retro explorers", fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookDark, fontSize = 14.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            PulsingDot()
                            Text("LIVE", fontFamily = BangersFontFamily, color = Color(0xFF00C853), fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(users, key = { it.uid }) { user -> PolaroidUserCard(user = user, onTap = onUserTap) }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { onUserTap() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FIND MORE PEOPLE →", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

// ─── PolaroidUserCard ─────────────────────────────────────────────────────────

@Composable
fun PolaroidUserCard(user: UserProfileData, onTap: () -> Unit) {
    val rotation = remember { (-3..3).random().toFloat() }
    Box(modifier = Modifier.width(76.dp).rotate(rotation).clickable { onTap() }) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(Color.White).border(1.5.dp, ScrapbookBorder.copy(alpha = 0.5f), RoundedCornerShape(3.dp)).padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(68.dp).background(ScrapbookPaper), contentAlignment = Alignment.Center) {
                if (!user.profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(model = user.profilePictureUrl, contentDescription = user.username, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(user.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 30.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(user.username, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookDark, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        }
    }
}

// ─── FeaturedAlbumsCarousel ───────────────────────────────────────────────────

@Composable
fun FeaturedAlbumsCarousel(onNavigateToAlbums: () -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(sampleAlbums.take(5), key = { it.id }) { album ->
            var pressed by remember { mutableStateOf(false) }
            val cardScale by animateFloatAsState(
                targetValue = if (pressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "albumScale_${album.id}"
            )
            val kbT = rememberInfiniteTransition(label = "kb_${album.id}")
            val albumIndex = sampleAlbums.indexOfFirst { it.id == album.id }.coerceAtLeast(0)
            val albumDuration = 10000 + albumIndex * 2000
            val kbScale by kbT.animateFloat(
                initialValue = 1f, targetValue = 1.07f,
                animationSpec = infiniteRepeatable(keyframes { durationMillis = albumDuration; 1f at 0; 1.07f at albumDuration / 2; 1f at albumDuration }, RepeatMode.Restart),
                label = "kbScale_${album.id}"
            )
            Box(modifier = Modifier.width(160.dp).scale(cardScale)) {
                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
                    Column(modifier = Modifier.clickable { pressed = true; onNavigateToAlbums() }) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentAlignment = Alignment.Center) {
                            if (album.coverImageResId != null) {
                                Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
                        }
                        Column(modifier = Modifier.fillMaxWidth().height(64.dp).padding(9.dp), verticalArrangement = Arrangement.Center) {
                            Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                            Text(album.artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
        }
        item {
            Box(modifier = Modifier.width(80.dp).height(228.dp)) {
                ScrapbookCard(modifier = Modifier.fillMaxSize(), backgroundColor = ScrapbookYellow, cornerRadius = 12.dp, shadowOffset = 4.dp) {
                    Box(modifier = Modifier.fillMaxSize().clickable { onNavigateToAlbums() }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", color = ScrapbookDark, fontSize = 30.sp, fontFamily = BangersFontFamily)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("SEE\nALL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ─── FeaturedMagazinesCarousel ────────────────────────────────────────────────

@Composable
fun FeaturedMagazinesCarousel(onNavigateToMagazines: () -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(sampleMagazineCovers.take(5), key = { it.id }) { magazine ->
            var pressed by remember { mutableStateOf(false) }
            val cardScale by animateFloatAsState(
                targetValue = if (pressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "magScale_${magazine.id}"
            )
            val kbT = rememberInfiniteTransition(label = "kb_mag_${magazine.id}")
            val magIndex = sampleMagazineCovers.indexOfFirst { it.id == magazine.id }.coerceAtLeast(0)
            val magDuration = 11000 + magIndex * 1500
            val kbScale by kbT.animateFloat(
                initialValue = 1f, targetValue = 1.06f,
                animationSpec = infiniteRepeatable(keyframes { durationMillis = magDuration; 1f at 0; 1.06f at magDuration / 2; 1f at magDuration }, RepeatMode.Restart),
                label = "kbScaleMag_${magazine.id}"
            )
            Box(modifier = Modifier.width(115.dp).scale(cardScale)) {
                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 4.dp) {
                    Column(modifier = Modifier.clickable { pressed = true; onNavigateToMagazines() }) {
                        Box(modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)), contentAlignment = Alignment.BottomStart) {
                            if (magazine.coverImageResId != null) {
                                Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                        }
                        Text(magazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
        }
        item {
            Box(modifier = Modifier.width(80.dp).height(183.dp)) {
                ScrapbookCard(modifier = Modifier.fillMaxSize(), backgroundColor = ScrapbookOrange, cornerRadius = 10.dp, shadowOffset = 4.dp) {
                    Box(modifier = Modifier.fillMaxSize().clickable { onNavigateToMagazines() }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", color = Color.White, fontSize = 28.sp, fontFamily = BangersFontFamily)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("SEE\nALL", fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ─── FeaturedStreamsSection ───────────────────────────────────────────────────

@Composable
fun FeaturedStreamsSection(onNavigateToStreams: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionTitle(title = "📺 STREAMS & VIDEOS")
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ScrapbookCard(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToStreams() },
                backgroundColor = ScrapbookDark, cornerRadius = 14.dp, shadowOffset = 5.dp
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.horizontalGradient(colors = listOf(Color(0xFF9146FF), Color(0xFFBB00FF), Color(0xFFFF0000))))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PulsingDot(color = Color.White, size = 9.dp)
                                Text("LIVE NOW", fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp)
                            }
                            Text("▶ WATCH", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp)
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Watch live retro gaming streams and classic gaming videos from the community.", fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFF9146FF)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { onNavigateToStreams() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                                Text("🔴 TWITCH", fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp)
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFF0000)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { onNavigateToStreams() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                                Text("▶ YOUTUBE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
// ─── NewsSection ──────────────────────────────────────────────────────────────

@Composable
fun NewsSection(
    newsItems: List<NewsItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        HomeSectionTitle(title = "📡 LATEST NEWS")
        Spacer(modifier = Modifier.height(12.dp))
        when {
            isLoading -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp), cornerRadius = 12.dp) }
            }
            errorMessage != null -> Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ScrapbookDark), shape = RoundedCornerShape(8.dp)) {
                    Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                }
            }
            newsItems.isEmpty() && !isLoading -> Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No news articles found at the moment.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(newsItems, key = { it.id }) { newsItem -> NewsItemCard(newsItem = newsItem) }
            }
        }
    }
}

// ─── NewsItemCard ─────────────────────────────────────────────────────────────

@Composable
fun NewsItemCard(newsItem: NewsItem, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "newsCardScale"
    )
    Box(modifier = modifier.scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable {
                pressed = true
                if (newsItem.sourceUrl.isNotBlank()) {
                    try { uriHandler.openUri(newsItem.sourceUrl) } catch (e: Exception) { }
                }
            },
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp
        ) {
            Column {
                newsItem.imageUrl?.let {
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        AsyncImage(model = it, contentDescription = newsItem.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(5.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(5.dp)).padding(horizontal = 9.dp, vertical = 4.dp)) {
                            Text(newsItem.sourceName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    }
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(newsItem.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 21.sp, letterSpacing = 0.5.sp, maxLines = 2, lineHeight = 25.sp)
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(newsItem.summary, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookTextDark, fontSize = 14.sp, lineHeight = 21.sp, maxLines = 3)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (newsItem.imageUrl == null) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(newsItem.sourceName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                            }
                        } else { Spacer(modifier = Modifier.width(1.dp)) }
                        Text(formatEpochMillisToReadableDate(newsItem.publishedDate), fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
}

// ─── CopyrightFooter ──────────────────────────────────────────────────────────

@Composable
fun CopyrightFooter(name: String, blogUrl: String, modifier: Modifier = Modifier) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val uriHandler = LocalUriHandler.current
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, borderColor = ScrapbookDark, cornerRadius = 12.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("© $currentYear $name. All Rights Reserved.", fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                val annotatedString = buildAnnotatedString {
                    append("Visit my blog: ")
                    pushStringAnnotation(tag = "URL", annotation = blogUrl)
                    withStyle(style = SpanStyle(color = ScrapbookYellow, textDecoration = TextDecoration.Underline, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold)) {
                        append("charlysblog.framer.website")
                    }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = TextStyle(textAlign = TextAlign.Center, fontSize = 14.sp, fontFamily = NunitoFontFamily, color = Color.White),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                            try { uriHandler.openUri(it.item) } catch (e: Exception) { }
                        }
                    }
                )
            }
        }
    }
}
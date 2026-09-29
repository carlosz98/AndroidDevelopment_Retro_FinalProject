package com.example.hubretro

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import okhttp3.OkHttpClient
import okhttp3.Request

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// ─── Constants ────────────────────────────────────────────────────────────────

private val CLAUDE_API_KEY get() = BuildConfig.CLAUDE_API_KEY
private const val CLAUDE_MODEL = "claude-sonnet-4-6"
private val UNSPLASH_KEY get() = BuildConfig.UNSPLASH_ACCESS_KEY

// ─── Date Helper ──────────────────────────────────────────────────────────────

fun todayDateKey(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

// ─── Per-page Cache ───────────────────────────────────────────────────────────

object PageHeroCache {
    data class HeroEntry(
        val date: String,
        val query: String,
        val photo: UnsplashPhoto?
    )

    private val cache = mutableMapOf<String, HeroEntry>()

    fun get(pageId: String): HeroEntry? {
        val entry = cache[pageId] ?: return null
        return if (entry.date == todayDateKey()) entry else null
    }

    fun set(pageId: String, query: String, photo: UnsplashPhoto?) {
        cache[pageId] = HeroEntry(
            date = todayDateKey(),
            query = query,
            photo = photo
        )
    }
}

// ─── PageHeroConfig ───────────────────────────────────────────────────────────

data class PageHeroConfig(
    val pageId: String,
    val masthead: String,
    val issueLabel: String,
    val tagline: String,
    val subtitle: String,
    val ctaText: String,
    val accentColor: Color,
    val fallbackQueries: List<String>,
    val claudePrompt: String,
    val tickerItems: List<String>
)

// ─── Page Configs ─────────────────────────────────────────────────────────────

val albumsHeroConfig = PageHeroConfig(
    pageId = "albums",
    masthead = "RETROHUB ALBUMS",
    issueLabel = "MUSIC EDITION",
    tagline = "From 8-bit to symphony",
    subtitle = "500+ retro game soundtracks",
    ctaText = "BROWSE ALBUMS →",
    accentColor = CAcPurple,
    fallbackQueries = listOf(
        "retro vinyl record music neon",
        "cassette tape mixtape vintage",
        "synthwave music studio neon",
        "8-bit chiptune arcade sound",
        "vintage record player nostalgia"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro gaming music page hero background. Think vinyl records, chiptune, game soundtracks, nostalgic music. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "🎵 Final Fantasy VII OST · iconic",
        "🎶 Minecraft C418 · 500M plays",
        "🎵 Undertale OST · Toby Fox masterpiece",
        "🎹 Super Mario Bros theme · 1985",
        "🎵 Tetris theme · most recognized melody",
        "🎼 Zelda overworld · 38 years strong",
        "🎸 Doom OST · heavy metal pioneer",
        "🎹 Castlevania · Bloody Tears forever",
        "🎵 Street Fighter II · iconic stage themes",
        "🎶 Sonic Green Hill Zone · instant classic"
    )
)

val magazinesHeroConfig = PageHeroConfig(
    pageId = "magazines",
    masthead = "RETROHUB PRESS",
    issueLabel = "PRINT EDITION",
    tagline = "The golden age of gaming press",
    subtitle = "Vintage gaming magazines preserved",
    ctaText = "READ MAGAZINES →",
    accentColor = CAcRed,
    fallbackQueries = listOf(
        "vintage magazine newsstand retro print",
        "old newspaper stack nostalgia",
        "retro print media collection",
        "vintage bookstore magazine rack",
        "paper texture retro print"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro gaming magazine page hero background. Think vintage print, newsstand, paper magazines, collectors. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "📰 Nintendo Power · first issue 1988",
        "📰 EGM · Electronic Gaming Monthly 1989",
        "📰 GamePro · 150+ iconic issues",
        "📰 Official PlayStation Magazine · 1997",
        "📰 Edge Magazine · 30 years of gaming",
        "📰 Game Informer · still publishing",
        "📰 GameFan · import gaming pioneer",
        "📰 Mean Machines · Sega's finest era",
        "📰 Retro Gamer · celebrating the classics",
        "📰 GamesTM · UK gaming royalty"
    )
)

val articlesHeroConfig = PageHeroConfig(
    pageId = "articles",
    masthead = "RETROHUB WRITES",
    issueLabel = "COMMUNITY EDITION",
    tagline = "Stories from the retro community",
    subtitle = "Written by gamers, for gamers",
    ctaText = "READ ARTICLES →",
    accentColor = CAcBlue,
    fallbackQueries = listOf(
        "vintage typewriter writing nostalgia desk",
        "old notebook pen writing desk",
        "retro desk lamp writing",
        "vintage paper letter writing",
        "cozy writing nook nostalgia"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro gaming community writing page hero background. Think typewriter, writing, nostalgia, stories, gaming culture. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "📝 Share your retro gaming story",
        "✍️ Community written · daily new articles",
        "📖 Retro memories · preserved forever",
        "🖊️ Write your first article today",
        "📰 From NES to PS5 · all eras covered",
        "✍️ Gaming history · through your eyes",
        "📝 Top rated articles this week",
        "🖊️ Join 500+ community writers",
        "📖 Discover hidden gaming gems",
        "✍️ Your story matters · write it here"
    )
)

val gameDatabaseHeroConfig = PageHeroConfig(
    pageId = "gamedatabase",
    masthead = "RETROHUB GAMES",
    issueLabel = "DATABASE EDITION",
    tagline = "Every retro game ever made",
    subtitle = "IGDB powered retro game database",
    ctaText = "BROWSE GAMES →",
    accentColor = CGreenDeep,
    fallbackQueries = listOf(
        "arcade cabinet retro video game neon",
        "retro game cartridges collection",
        "vintage console controller neon",
        "classic arcade machine row",
        "retro gaming setup neon lights"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro video game database page hero background. Think arcade cabinets, cartridges, classic consoles, NES SNES PlayStation. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "🕹️ Powered by IGDB database",
        "🎮 1000+ retro games catalogued",
        "👾 NES · SNES · PS1 · N64 · PS2",
        "🏆 Rated and reviewed by fans",
        "🎲 Random game roulette · try your luck",
        "🕹️ Time machine · search by year",
        "🎮 Ken Burns edition covers",
        "👾 Arcade to home console · all eras",
        "🏆 Game of the day · daily picks",
        "🎮 Add games to your top 6"
    )
)

val discoverHeroConfig = PageHeroConfig(
    pageId = "discover",
    masthead = "RETROHUB DISCOVER",
    issueLabel = "COMMUNITY EDITION",
    tagline = "Find your retro tribe",
    subtitle = "News, people, deals & more",
    ctaText = "EXPLORE NOW →",
    accentColor = CGreenDeep,
    fallbackQueries = listOf(
        "retro gaming community friends neon arcade",
        "people playing video games together",
        "arcade hall crowd retro",
        "gaming convention crowd neon",
        "friends gaming night retro"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro gaming community discover page hero background. Think people gaming together, arcade halls, retro gaming culture, community. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "👥 Join the global retro community",
        "🔍 Find fellow retro explorers",
        "📡 Live gaming news · updated daily",
        "🔥 Trending this week in retro",
        "🌍 Retro fans across the globe",
        "👾 Follow top retro players",
        "🔴 Live streams happening now",
        "🏆 Weekly leaderboard updates",
        "📰 Breaking retro gaming news",
        "🎮 Deals · sales · bargain finds"
    )
)

val streamsHeroConfig = PageHeroConfig(
    pageId = "streams",
    masthead = "RETROHUB LIVE",
    issueLabel = "STREAMS EDITION",
    tagline = "Watch retro gaming live",
    subtitle = "Twitch · YouTube · Community streams",
    ctaText = "WATCH LIVE →",
    accentColor = CAcPurple,
    fallbackQueries = listOf(
        "streaming setup neon gaming room live",
        "gaming chair desk neon setup",
        "live broadcast studio neon",
        "twitch streamer desk setup",
        "neon gaming room aesthetic"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro gaming live streams page hero background. Think streaming setup, neon lights, gaming room, live broadcast. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "🔴 Live retro streams now",
        "📺 Twitch · YouTube · RetroHub",
        "🎮 Watch classic gameplay live",
        "👾 Community streamers broadcasting",
        "🔴 NES marathons · PS1 speedruns",
        "📺 Retro gaming 24/7",
        "🎮 Follow your favourite streamers",
        "👾 Chat with live retro fans",
        "🔴 New streams starting every hour",
        "📺 From Pong to PS2 · all live"
    )
)

val retroBytesHeroConfig = PageHeroConfig(
    pageId = "retrobytes",
    masthead = "RETROBYTES",
    issueLabel = "SHORTS EDITION",
    tagline = "Retro gaming in 60 seconds",
    subtitle = "Swipe through retro gaming shorts",
    ctaText = "WATCH SHORTS →",
    accentColor = CAcRed,
    fallbackQueries = listOf(
        "mobile phone gaming short video neon",
        "smartphone vertical video gaming",
        "mobile gaming neon aesthetic",
        "phone screen gaming closeup",
        "vertical video content neon"
    ),
    claudePrompt = "Give me a single Unsplash search query of 3-5 words for a retro gaming short video page hero background. Think mobile phone, TikTok style, quick clips, gaming highlights. Be creative and visual. Return only the query words, nothing else, no quotes, no punctuation.",
    tickerItems = listOf(
        "📱 RetroBytes · 60 second retro",
        "🎮 Swipe for more retro shorts",
        "👾 New videos added daily",
        "🕹️ Retro gaming in 60 seconds",
        "📱 Like · Share · Repeat",
        "🎮 Best retro moments · short form",
        "👾 Community favourite clips",
        "🕹️ NES to PS2 · all in shorts",
        "📱 Powered by YouTube Shorts",
        "🎮 Tap to play · swipe to explore"
    )
)

val marketplaceHeroConfig = PageHeroConfig(
    pageId = "marketplace",
    masthead = "RETROMARKET",
    issueLabel = "DEAL HUNTER EDITION",
    tagline = "Find it. Buy it. Own it.",
    subtitle = "Search eBay, Amazon, Mercari & more",
    ctaText = "START HUNTING →",
    accentColor = CAcYellow,
    fallbackQueries = listOf(
        "retro gaming collectibles market",
        "vintage game store shelf",
        "retro toy shop collectibles",
        "flea market vintage games",
        "game collection display shelf"
    ),
    claudePrompt = "Generate a 3-5 word Unsplash search query for retro gaming marketplace, collectibles, vintage game store, or retro shopping aesthetic. Return only the query.",
    tickerItems = listOf(
        "🕹️ RETRO DEALS LIVE",
        "💰 BEST PRICES FOUND",
        "📦 SEALED COPIES AVAILABLE",
        "🎮 NES · SNES · PS1 · N64",
        "💎 RARE FINDS DAILY",
        "🏆 GRAIL ALERT ACTIVE",
        "🔍 SEARCH 5 MARKETS AT ONCE"
    )
)

// ─── Claude AI Query Fetch ────────────────────────────────────────────────────

suspend fun fetchClaudeQueryForPage(config: PageHeroConfig): String =
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val body = JSONObject().apply {
                put("model", CLAUDE_MODEL)
                put("max_tokens", 30)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", config.claudePrompt)
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("x-api-key", CLAUDE_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            val query = json
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9 ]"), "")
                .trim()

            if (query.isBlank() || query.length < 3) config.fallbackQueries.random()
            else query
        } catch (e: Exception) {
            Log.e("PageHero", "Claude query failed for ${config.pageId}: ${e.message}")
            config.fallbackQueries.random()
        }
    }

// ─── Unsplash Hero Photo Fetch ────────────────────────────────────────────────

suspend fun fetchHeroPhotoForPage(query: String): UnsplashPhoto? =
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://api.unsplash.com/photos/random?query=$encodedQuery&orientation=landscape&client_id=$UNSPLASH_KEY")
                .build()
            val response = client.newCall(request).execute()
            Log.d("PageHero", "Unsplash code=${response.code} for query='$query'")
            if (!response.isSuccessful) {
                val errBody = response.body?.string()
                Log.e("PageHero", "Unsplash NON-200 (${response.code}): $errBody")
                return@withContext null
            }
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
            Log.e("PageHero", "Unsplash fetch threw: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

// ─── Combined Fetch ───────────────────────────────────────────────────────────

suspend fun loadPageHero(config: PageHeroConfig): UnsplashPhoto? {
    PageHeroCache.get(config.pageId)?.let { return it.photo }
    val query = fetchClaudeQueryForPage(config)
    val photo = fetchHeroPhotoForPage(query)
    PageHeroCache.set(config.pageId, query, photo)
    return photo
}

// ─── RetroHubPageHero ─────────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
fun RetroHubPageHero(
    config: PageHeroConfig,
    onCtaClick: () -> Unit = {}
) {
    val today = remember {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    }
    val issueNumber = remember {
        val cal = Calendar.getInstance()
        "VOL.${cal.get(Calendar.YEAR)} NO.${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    var photo by remember { mutableStateOf<UnsplashPhoto?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(config.pageId) {
        isLoading = true
        photo = loadPageHero(config)
        isLoading = false
    }

    val btnScale by rememberGlowRange(1f, 1.03f)

    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "pageHeroEnter_${config.pageId}"
    )
    LaunchedEffect(Unit) { delay(50); visible = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .background(ComicGlassBg)
            .graphicsLayer { alpha = enterAlpha }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .border(3.dp, ScrapbookDark, RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ─── Masthead ─────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(BorderStroke(2.dp, ScrapbookDark))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = config.masthead,
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = if (config.masthead.length > 16) 32.sp else 44.sp,
                                letterSpacing = 5.sp,
                                lineHeight = 46.sp
                            )
                            Text(
                                text = issueNumber,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            listOf("RETRO", "GAMING", "UNIVERSE").forEachIndexed { i, word ->
                                Text(
                                    text = word,
                                    fontFamily = BangersFontFamily,
                                    color = if (i == 2) CGreenDeep else ScrapbookDark,
                                    fontSize = 12.sp,
                                    letterSpacing = 3.sp
                                )
                            }
                        }
                    }
                }

                // ─── Date strip ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CGreen)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = today.uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(ScrapbookDark)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = config.tagline.uppercase(),
                                fontFamily = BangersFontFamily,
                                color = CGreen,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // ─── Photo area ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                ) {
                    when {
                        isLoading -> {
                            ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ScrapbookDark.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = config.accentColor,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Loading today's cover...",
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ScrapbookDark,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        photo != null -> {
                            val kbT = rememberInfiniteTransition(label = "pageKB_${config.pageId}")
                            val heroScale by kbT.animateFloat(
                                initialValue = 1f, targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(
                                    keyframes {
                                        durationMillis = 16000
                                        1f at 0; 1.08f at 8000; 1f at 16000
                                    },
                                    RepeatMode.Restart
                                ),
                                label = "pageHeroScale_${config.pageId}"
                            )
                            val heroPanX by kbT.animateFloat(
                                initialValue = -10f, targetValue = 10f,
                                animationSpec = infiniteRepeatable(
                                    keyframes {
                                        durationMillis = 20000
                                        -10f at 0; 10f at 10000; -10f at 20000
                                    },
                                    RepeatMode.Restart
                                ),
                                label = "pageHeroPanX_${config.pageId}"
                            )

                            AsyncImage(
                                model = photo!!.imageUrl,
                                contentDescription = photo!!.description,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(heroScale)
                                    .offset(x = heroPanX.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                            startY = 160f
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(config.accentColor.copy(alpha = 0.15f))
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📷 ${photo!!.photographerName}",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(config.accentColor)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "TODAY'S COVER",
                                        fontFamily = BangersFontFamily,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        letterSpacing = 2.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = config.subtitle,
                                    fontFamily = BangersFontFamily,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .scale(btnScale)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(config.accentColor)
                                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { onCtaClick() }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = config.ctaText,
                                        fontFamily = BangersFontFamily,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(config.accentColor.copy(alpha = 0.6f), ScrapbookDark)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎮", fontSize = 72.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = config.subtitle,
                                        fontFamily = BangersFontFamily,
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .scale(btnScale)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(config.accentColor)
                                            .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable { onCtaClick() }
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = config.ctaText,
                                            fontFamily = BangersFontFamily,
                                            color = Color.White,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "★",
            fontFamily = BangersFontFamily,
            color = config.accentColor,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
        )
    }
}

// ─── RetroHubPageTicker ───────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
fun RetroHubPageTicker(config: PageHeroConfig) {
    val tickerText = config.tickerItems.joinToString("   ★   ")
    val transition = rememberInfiniteTransition(label = "pageTicker_${config.pageId}")
    val offset by transition.animateFloat(
        initialValue = 1f,
        targetValue = -2f,
        animationSpec = infiniteRepeatable(
            tween(28000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "pageTickerOffset_${config.pageId}"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(CGreen, CGreenMint, CGreen)))
            .border(BorderStroke(2.dp, ScrapbookDark))
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
                    color = ScrapbookDark,
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
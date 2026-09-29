package com.example.hubretro

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ─── Icon helpers ─────────────────────────────────────────────────────────────

private val IconInfo: ImageVector get() = Icons.Filled.Info
private val IconSearch: ImageVector get() = Icons.Filled.Search
private val IconClose: ImageVector get() = Icons.Filled.Close
private val IconViewList: ImageVector get() = Icons.Filled.ViewList
private val IconGridView: ImageVector get() = Icons.Filled.GridView
private val IconChevronRight: ImageVector get() = Icons.Filled.ChevronRight
private val IconArrowBack: ImageVector get() = Icons.Filled.ArrowBack

// ─── Platform data ────────────────────────────────────────────────────────────

data class GamePlatform(
    val id: String,
    val label: String,
    val emoji: String,
    val accentColor: Color,
    val igdbPlatformId: Int,
    val queries: List<String>
)

val gamePlatforms = listOf(
    GamePlatform("all", "ALL", "🕹️", CGreen, -1, listOf(
        "mario", "zelda", "sonic", "pokemon", "final fantasy", "mega man",
        "castlevania", "metroid", "street fighter", "mortal kombat",
        "donkey kong", "kirby", "star fox", "earthbound", "chrono trigger"
    )),
    GamePlatform("nes", "NES", "🎮", Color(0xFFE4000F), 18, listOf(
        "mario", "zelda", "metroid", "mega man", "castlevania", "contra",
        "duck hunt", "excitebike", "punch out", "battletoads", "ninja gaiden",
        "kirby", "bionic commando", "ghosts n goblins", "double dragon"
    )),
    GamePlatform("snes", "SNES", "🎮", Color(0xFF7B2FBE), 19, listOf(
        "super mario", "zelda link", "donkey kong country", "final fantasy",
        "chrono trigger", "super metroid", "earthbound", "yoshi", "kirby super",
        "street fighter", "super castlevania", "star fox", "mega man x",
        "secret of mana", "super punch out", "pilot wings", "f-zero"
    )),
    GamePlatform("sega", "SEGA", "💿", CAcBlue, 29, listOf(
        "sonic", "streets of rage", "golden axe", "altered beast", "phantasy star",
        "shining force", "ecco dolphin", "earthworm jim", "vectorman", "ristar",
        "comix zone", "gunstar heroes", "beyond oasis", "toejam earl", "shinobi"
    )),
    GamePlatform("ps1", "PS1", "💙", CAcBlue, 7, listOf(
        "final fantasy vii", "resident evil", "crash bandicoot", "spyro",
        "metal gear solid", "castlevania symphony", "tekken", "gran turismo",
        "twisted metal", "silent hill", "parasite eve", "vagrant story",
        "suikoden", "xenogears", "legend of dragoon", "coolboarders"
    )),
    GamePlatform("n64", "N64", "🌐", Color(0xFF009AC7), 4, listOf(
        "super mario 64", "zelda ocarina", "goldeneye", "banjo kazooie",
        "donkey kong 64", "star fox 64", "mario kart 64", "paper mario",
        "majoras mask", "conkers bad fur day", "perfect dark", "pokemon stadium",
        "yoshi story", "kirby 64", "f-zero x", "bomberman 64"
    )),
    GamePlatform("gba", "GBA", "📱", Color(0xFF8B4513), 24, listOf(
        "pokemon fire red", "zelda minish", "metroid fusion", "mario advance",
        "castlevania aria", "golden sun", "fire emblem", "tactics ogre",
        "advance wars", "kirby nightmare", "mega man zero", "mother 3",
        "wario ware", "yoshi island gba", "sonic advance", "final fantasy tactics"
    )),
    GamePlatform("gamecube", "GAMECUBE", "🟣", Color(0xFF6A0DAD), 21, listOf(
        "super mario sunshine", "zelda wind waker", "metroid prime", "luigi mansion",
        "pikmin", "super smash melee", "mario kart double dash", "star fox adventures",
        "resident evil 4", "eternal darkness", "beyond good evil", "viewtiful joe",
        "tales of symphonia", "fire emblem path", "paper mario thousand", "f-zero gx"
    )),
    GamePlatform("ps2", "PS2", "🔵", Color(0xFF00439C), 8, listOf(
        "god of war", "shadow colossus", "ico", "kingdom hearts", "devil may cry",
        "grand theft auto san andreas", "metal gear solid 2", "silent hill 2",
        "final fantasy x", "persona 3", "okami", "jak daxter", "ratchet clank",
        "sly cooper", "burnout revenge", "katamari damacy", "dragon quest viii"
    )),
    GamePlatform("ps3", "PS3", "⚫", ComicGlassBg, 9, listOf(
        "uncharted", "the last of us", "god of war 3", "demon souls", "red dead redemption",
        "heavy rain", "beyond two souls", "journey", "flower", "littlebigplanet",
        "metal gear solid 4", "infamous", "resistance fall of man", "killzone 2",
        "valkyria chronicles", "ni no kuni", "tales of graces"
    )),
    GamePlatform("wii", "WII", "⚪", ComicGlassBg, 5, listOf(
        "super mario galaxy", "zelda twilight princess", "wii sports", "mario kart wii",
        "new super mario bros wii", "metroid other m", "xenoblade chronicles",
        "the last story", "pandoras tower", "donkey kong country returns",
        "kirby epic yarn", "mario party 8", "fire emblem radiant dawn",
        "super paper mario", "okami wii", "mad world", "no more heroes"
    )),
    GamePlatform("arcade", "ARCADE", "🕹️", CAcYellow, -1, listOf(
        "pac man", "galaga", "space invaders", "donkey kong arcade", "street fighter 2",
        "mortal kombat arcade", "tekken arcade", "time crisis", "virtua fighter",
        "out run", "daytona usa", "house of the dead", "metal slug", "king of fighters",
        "neo geo", "bubble bobble", "rainbow islands", "frogger", "centipede"
    ))
)

val gameGenreFilters = listOf(
    "ALL", "Role-playing (RPG)", "Action", "Platform", "Shooter",
    "Adventure", "Fighting", "Racing", "Sport", "Puzzle", "Strategy", "Arcade", "Music"
)

// ─── YouTube helper ────────────────────────────────────────────────────────────

suspend fun searchYouTubeTrailer(gameName: String): String? {
    val query = "${gameName} official trailer".replace(" ", "+")

    // 1️⃣ Try YouTube Data API v3
    try {
        val url = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet&q=$query&type=video&maxResults=1" +
                "&key=${BuildConfig.YOUTUBE_API_KEY}"
        val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
        val response = client.newCall(Request.Builder().url(url).build()).execute()
        val body = response.body?.string() ?: ""
        android.util.Log.d("Trailer", "YT API status=${response.code} body=${body.take(200)}")
        val json = JSONObject(body)
        if (!json.has("error")) {
            val items = json.optJSONArray("items")
            val id = items?.getJSONObject(0)?.getJSONObject("id")?.optString("videoId")
            if (!id.isNullOrBlank()) {
                android.util.Log.d("Trailer", "YT API found: $id")
                return id
            }
        } else {
            android.util.Log.w("Trailer", "YT API error: ${json.getJSONObject("error").optString("message")}")
        }
    } catch (e: Exception) {
        android.util.Log.w("Trailer", "YT API failed: ${e.message}")
    }

    // 2️⃣ Fallback: Invidious public API (no key needed)
    val invInstances = listOf("https://inv.nadeko.net", "https://invidious.io", "https://y.com.sb")
    for (instance in invInstances) {
        try {
            val url = "$instance/api/v1/search?q=$query&type=video&fields=videoId&page=1"
            val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: continue
            android.util.Log.d("Trailer", "Invidious $instance status=${response.code}")
            val arr = org.json.JSONArray(body)
            val id = arr.optJSONObject(0)?.optString("videoId")
            if (!id.isNullOrBlank()) {
                android.util.Log.d("Trailer", "Invidious found: $id")
                return id
            }
        } catch (e: Exception) {
            android.util.Log.w("Trailer", "Invidious $instance failed: ${e.message}")
        }
    }

    android.util.Log.w("Trailer", "No trailer found for: $gameName")
    return null
}

fun platformAccentColor(platformId: String): Color =
    gamePlatforms.find { it.id == platformId }?.accentColor ?: CGreen

fun ratingColor(rating: Double): Color = when {
    rating >= 80.0 -> CGreen
    rating >= 60.0 -> CGreenDeep
    else -> CAcRed
}

fun igdbGenreAbbr(genre: String): String = when {
    genre.contains("Role-playing", ignoreCase = true) || genre.contains("RPG", ignoreCase = true) -> "RPG"
    genre.contains("Action", ignoreCase = true) -> "ACT"
    genre.contains("Platform", ignoreCase = true) -> "PLT"
    genre.contains("Shooter", ignoreCase = true) -> "SHT"
    genre.contains("Adventure", ignoreCase = true) -> "ADV"
    genre.contains("Fighting", ignoreCase = true) -> "FGT"
    genre.contains("Racing", ignoreCase = true) -> "RAC"
    genre.contains("Sport", ignoreCase = true) -> "SPT"
    genre.contains("Puzzle", ignoreCase = true) -> "PZL"
    genre.contains("Strategy", ignoreCase = true) -> "STR"
    genre.contains("Simulation", ignoreCase = true) -> "SIM"
    genre.contains("Arcade", ignoreCase = true) -> "ARC"
    genre.contains("Music", ignoreCase = true) -> "MUS"
    genre.contains("Horror", ignoreCase = true) -> "HRR"
    else -> genre.take(3).uppercase()
}

fun igdbGameModeIcon(mode: String): String = when {
    mode.contains("Single", ignoreCase = true) -> "🎮"
    mode.contains("Multi", ignoreCase = true) -> "👥"
    mode.contains("Co-op", ignoreCase = true) || mode.contains("Co op", ignoreCase = true) -> "🤝"
    mode.contains("Battle", ignoreCase = true) -> "⚔️"
    mode.contains("MMO", ignoreCase = true) -> "🌐"
    else -> "🎮"
}

// ─── Shimmer cards ─────────────────────────────────────────────────────────────

@Composable
fun ShimmerGameCard() {
    val shimmerT = rememberInfiniteTransition(label = "gameShimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "gameShimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ComicGlassBg, Color.White.copy(alpha = 0.88f), ComicGlassBg),
        start = androidx.compose.ui.geometry.Offset(shimmerX - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.75f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
        }
    }
}

@Composable
fun ShimmerGameGridCard() {
    val shimmerT = rememberInfiniteTransition(label = "gameGridShimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "gameGridShimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ComicGlassBg, Color.White.copy(alpha = 0.88f), ComicGlassBg),
        start = androidx.compose.ui.geometry.Offset(shimmerX - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f)
    )
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(12.dp)).background(shimmerBrush))
}

// ─── IGDB Rating Bar ──────────────────────────────────────────────────────────

@Composable
fun IGDBRatingBar(rating: Double, compact: Boolean = true) {
    val score = (rating / 10.0).toInt().coerceIn(0, 10)
    val color = ratingColor(rating)
    if (compact) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("$score/10", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
            }
            Text(
                when {
                    rating >= 80.0 -> "GREAT"
                    rating >= 60.0 -> "GOOD"
                    rating >= 40.0 -> "OK"
                    else -> "MIXED"
                },
                fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = color, fontSize = 10.sp
            )
        }
    } else {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(color), contentAlignment = Alignment.Center) {
                    Text("$score", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp)
                }
                Column {
                    Text(
                        when {
                            rating >= 80.0 -> "🏆 OUTSTANDING"
                            rating >= 70.0 -> "⭐ GREAT"
                            rating >= 60.0 -> "👍 GOOD"
                            rating >= 50.0 -> "😐 MIXED"
                            else -> "👎 POOR"
                        },
                        fontFamily = BangersFontFamily, color = color, fontSize = 18.sp
                    )
                    Text("IGDB Score — ${String.format("%.1f", rating)}/100", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(4.dp))) {
                val animRating by animateFloatAsState(
                    targetValue = (rating / 100.0).toFloat().coerceIn(0f, 1f),
                    animationSpec = tween(1200, easing = LinearOutSlowInEasing),
                    label = "ratingAnim"
                )
                Box(modifier = Modifier.fillMaxWidth(animRating).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(colors = listOf(color, color.copy(alpha = 0.7f)))))
            }
        }
    }
}

// ─── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeaderLabel(title: String, count: Int, neonAlpha: Float, accentColor: Color = CGreen) {
    val (emoji, labelText) = remember(title) {
        val parts = title.split(" ", limit = 2)
        if (parts.first().any { it.code > 127 }) parts.first() to (parts.getOrElse(1) { "" })
        else "🎮" to title
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        RetroSectionHeader(title = labelText, emoji = emoji)
        if (count > 0) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp).clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.12f)).border(1.5.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("$count games", fontFamily = BangersFontFamily, color = accentColor, fontSize = 12.sp)
            }
        }
    }
}

// ─── Game of the Day ──────────────────────────────────────────────────────────

@Composable
fun GameOfTheDayCard(game: IGDBGame, onRead: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gotdCardScale")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).scale(cardScale)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(18.dp))
            .clickable { pressed = true; onRead() }
    ) {
        // Green stripe across the top of the card
        Box(modifier = Modifier.fillMaxWidth().height(5.dp)
            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Cover
            Box(
                modifier = Modifier.size(width = 100.dp, height = 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComicGlassBg)
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (game.coverUrl != null) {
                    AsyncImage(model = game.coverUrl, contentDescription = game.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Text("🎮", fontSize = 40.sp)
                }
                game.esrbRating?.let {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .clip(RoundedCornerShape(4.dp)).background(ScrapbookDark)
                        .padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(it, fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(CGreen)
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("★ GAME OF THE DAY", fontFamily = BangersFontFamily,
                        color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark,
                    fontSize = 20.sp, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(game.developer, game.releaseYear?.toString()).joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(sub, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                        color = CGreenDeep, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(6.dp))
                game.rating?.let { IGDBRatingBar(rating = it, compact = true) }
                if (game.genres.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        game.genres.take(3).forEach { genre ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(5.dp))
                                .background(CGreen.copy(alpha = 0.15f))
                                .border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text(igdbGenreAbbr(genre), fontFamily = BangersFontFamily,
                                    color = CGreenDeep, fontSize = 10.sp)
                            }
                        }
                    }
                }
                game.summary?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(it, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.7f),
                        fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("TAP TO OPEN ▶", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 12.sp, letterSpacing = 1.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Hall of Fame ─────────────────────────────────────────────────────────────

@Composable
fun HallOfFameSection(games: List<IGDBGame>, accentColor: Color, onClick: (IGDBGame) -> Unit) {
    if (games.isEmpty()) return
    val topGames = games.filter { (it.rating ?: 0.0) >= 70.0 }.sortedByDescending { it.rating }.take(5)
    if (topGames.isEmpty()) return
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen))
                Text("🏆 HALL OF FAME", fontFamily = BangersFontFamily, color = ScrapbookDark,
                    fontSize = 18.sp, letterSpacing = 1.sp)
            }
            Text("TOP ${topGames.size}", fontFamily = BangersFontFamily,
                color = CGreen, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(topGames) { index, game ->
                var pressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "hofScale$index")
                Box(modifier = Modifier.width(120.dp).scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(1.5.dp, if (index == 0) CGreen else accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { pressed = true; onClick(game) }
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(ComicGlassBg), contentAlignment = Alignment.Center) {
                            if (game.coverUrl != null) {
                                AsyncImage(model = game.coverUrl, contentDescription = game.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                            } else { Text("🎮", fontSize = 28.sp) }
                            // Rank badge
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (index == 0) CGreen else ScrapbookDark.copy(alpha = 0.75f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text("#${index + 1}", fontFamily = BangersFontFamily,
                                    color = if (index == 0) ScrapbookDark else Color.White, fontSize = 10.sp)
                            }
                            // Rating badge top-right
                            game.rating?.let { rating ->
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                                    .clip(RoundedCornerShape(5.dp)).background(ratingColor(rating))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily,
                                        color = Color.White, fontSize = 8.sp)
                                }
                            }
                            // Glossy specular
                            Box(modifier = Modifier.fillMaxWidth().height(1.5.dp)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent)))
                                .align(Alignment.TopCenter))
                        }
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark,
                                fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                            game.releaseYear?.let {
                                Text("$it", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 9.sp)
                            }
                        }
                    }
                }
                LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }
            }
        }
    }
}

// ─── Time Machine ─────────────────────────────────────────────────────────────

@Composable
fun TimeMachineSection(onYearSelected: (Int) -> Unit) {
    val years = (1985..2005).toList()
    var selectedYear by remember { mutableStateOf(1995) }
    val neonT = rememberInfiniteTransition(label = "tmNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("⏰", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("TIME MACHINE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                Text("What was hot in $selectedYear?", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
            }
            var searchPressed by remember { mutableStateOf(false) }
            val btnScale by animateFloatAsState(targetValue = if (searchPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tmBtnScale")
            Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp)).clickable { searchPressed = true; onYearSelected(selectedYear) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("GO", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
            }
            LaunchedEffect(searchPressed) { if (searchPressed) { delay(150); searchPressed = false } }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(years) { jumpIndex, year ->
                Box(modifier = Modifier.jumpIn(jumpIndex)) {
                val isSelected = selectedYear == year
                var pressed by remember { mutableStateOf(false) }
                val chipScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "yearChip_$year")
                Box(
                    modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) CGreen else Color.White.copy(alpha = 0.46f))
                        .border(1.dp, if (isSelected) CGreenDeep else ScrapbookDark.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .clickable { pressed = true; selectedYear = year }.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("'${year.toString().takeLast(2)}", fontFamily = BangersFontFamily,
                        color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                            }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

// ─── Animated item wrappers ───────────────────────────────────────────────────

@Composable
fun GameGridItemAnimated(index: Int, game: IGDBGame, accentColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        GameGridCard(game = game, accentColor = accentColor, onClick = onClick)
    }
}

@Composable
fun GameListItemAnimated(index: Int, game: IGDBGame, accentColor: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
        GameListCard(game = game, accentColor = accentColor, onClick = onClick)
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GameDatabaseScreen(modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf("ALL") }
    var isGridView by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<IGDBGame?>(null) }
    var showHigherLower by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { GameCollection.ensureListening() }
    var surpriseGame by remember { mutableStateOf<IGDBGame?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var selectedPlatformId by remember { mutableStateOf("all") }
    var timeMachineYear by remember { mutableStateOf<Int?>(null) }
    var timeMachineResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isTimeMachineLoading by remember { mutableStateOf(false) }

    val platformGames = remember { mutableStateMapOf<String, List<IGDBGame>>() }
    val platformLoading = remember { mutableStateMapOf<String, Boolean>() }

    val neonT = rememberInfiniteTransition(label = "gameNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    fun loadPlatformGames(platformId: String) {
        if (platformGames.containsKey(platformId)) return
        scope.launch {
            platformLoading[platformId] = true
            val platform = gamePlatforms.find { it.id == platformId } ?: return@launch
            try {
                coroutineScope {
                    val results = platform.queries.map { query ->
                        async {
                            try { IGDBRepository.searchGames(query) }
                            catch (e: Exception) { emptyList<IGDBGame>() }
                        }
                    }.awaitAll()
                    platformGames[platformId] = results.flatten()
                        .distinctBy { it.id }
                        .sortedByDescending { it.rating ?: 0.0 }
                }
            } catch (e: Exception) { }
            platformLoading[platformId] = false
        }
    }

    LaunchedEffect(selectedPlatformId) {
        if (!hasSearched && timeMachineYear == null) loadPlatformGames(selectedPlatformId)
    }
    LaunchedEffect(Unit) { loadPlatformGames("all") }
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600); isSearching = true; hasSearched = true
            try { searchResults = IGDBRepository.searchGames(searchQuery) }
            catch (e: Exception) { searchResults = emptyList() }
            isSearching = false
        } else if (searchQuery.isBlank()) { searchResults = emptyList(); hasSearched = false }
    }

    // Handle surprise game navigation
    surpriseGame?.let { game ->
        selectedGame = game
        surpriseGame = null
    }

    // Shared-element morph: the tapped cover flies into the detail hero
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
        targetState = selectedGame,
        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
        label = "gameDetailMorph"
    ) { shownGame ->
    CompositionLocalProvider(
        LocalSharedScope provides this@SharedTransitionLayout,
        LocalAnimScope provides this@AnimatedContent
    ) {
    if (shownGame != null) {
        GameDetailScreen(game = shownGame, onBack = { selectedGame = null })
    } else if (showHigherLower) {
        HigherOrLowerGame(
            pool = platformGames.values.flatten(),
            onClose = { showHigherLower = false },
            onOpenGame = { g -> showHigherLower = false; selectedGame = g }
        )
    } else {

    val currentPlatformGames = platformGames[selectedPlatformId] ?: emptyList()
    val isCurrentlyLoading = platformLoading[selectedPlatformId] == true
    val currentPlatform = gamePlatforms.find { it.id == selectedPlatformId }!!

    val displayGames = when {
        hasSearched -> searchResults
        timeMachineYear != null -> timeMachineResults
        else -> currentPlatformGames
    }

    val filteredGames = remember(displayGames, selectedGenre) {
        if (selectedGenre == "ALL") displayGames
        else displayGames.filter { game ->
            game.genres.any { it.contains(selectedGenre, ignoreCase = true) }
        }
    }

    // Shake-to-surprise (see ShakeDetector in MainActivity)
    LaunchedEffect(SurpriseBus.pending, filteredGames.size) {
        if (SurpriseBus.pending && filteredGames.isNotEmpty()) {
            SurpriseBus.pending = false
            surpriseGame = filteredGames.random()
        }
    }

    val gameOfTheDay = remember(currentPlatformGames) {
        val highRated = currentPlatformGames.filter { (it.rating ?: 0.0) >= 70.0 }
        if (highRated.isEmpty()) null
        else highRated[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) % highRated.size]
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) { // main column

            // ─── Toolbar ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f))
            ) {
                Column {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Game count badge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Powered by IGDB • ${if (!hasSearched) "${currentPlatformGames.size} games" else "${searchResults.size} results"}",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ScrapbookDark.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Lucky button
                    var luckyPressed by remember { mutableStateOf(false) }
                    val luckyScale by animateFloatAsState(targetValue = if (luckyPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "luckyScale")
                    Box(
                        modifier = Modifier.scale(luckyScale).size(38.dp).clip(CircleShape)
                            .background(CGreen.copy(alpha = 0.15f))
                            .border(1.dp, CGreenDeep.copy(alpha = 0.4f), CircleShape)
                            .clickable { luckyPressed = true; displayGames.randomOrNull()?.let { selectedGame = it } },
                        contentAlignment = Alignment.Center
                    ) { Text("🎲", fontSize = 18.sp) }
                    LaunchedEffect(luckyPressed) { if (luckyPressed) { delay(150); luckyPressed = false } }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Grid/List toggle
                    var viewPressed by remember { mutableStateOf(false) }
                    val viewScale by animateFloatAsState(targetValue = if (viewPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "viewScale")
                    Box(
                        modifier = Modifier.scale(viewScale).size(38.dp).clip(CircleShape)
                            .background(CGreen.copy(alpha = 0.15f))
                            .border(1.dp, CGreenDeep.copy(alpha = 0.4f), CircleShape)
                            .clickable { viewPressed = true; isGridView = !isGridView },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (isGridView) IconViewList else IconGridView, contentDescription = null, tint = CGreen, modifier = Modifier.size(18.dp))
                    }
                    LaunchedEffect(viewPressed) { if (viewPressed) { delay(150); viewPressed = false } }
                }
                // Yellow accent stripe at bottom of toolbar row
                Box(modifier = Modifier.fillMaxWidth().height(2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, CGreen, CGreenMint, CGreen, Color.Transparent)))
                    .align(Alignment.BottomCenter))
                }
                // Search bar inside toolbar Column
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search any game...", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark.copy(alpha = 0.45f)) },
                    leadingIcon = {
                        if (isSearching) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp)
                        else Icon(IconSearch, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; focusManager.clearFocus() }) {
                                Icon(IconClose, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CGreen, unfocusedBorderColor = CGreenDeep.copy(alpha = 0.3f), focusedContainerColor = ComicGlassBg, unfocusedContainerColor = ComicGlassBg, cursorColor = CGreenDeep, focusedTextColor = ScrapbookDark, unfocusedTextColor = ScrapbookDark.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )
                }
            }

            // Platform tab strip
            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f)).border(BorderStroke(1.dp, CGreenDeep.copy(alpha = 0.3f)))) {
                LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(gamePlatforms) { jumpIndex, platform ->
                        Box(modifier = Modifier.jumpIn(jumpIndex)) {
                        val isSelected = selectedPlatformId == platform.id
                        var tabPressed by remember { mutableStateOf(false) }
                        val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "platformTab_${platform.id}")
                        Box(
                            modifier = Modifier.scale(tabScale).clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) platform.accentColor else Color.White.copy(alpha = 0.46f))
                                .border(2.dp, if (isSelected) platform.accentColor else CGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable { tabPressed = true; selectedPlatformId = platform.id; if (hasSearched) { searchQuery = ""; hasSearched = false }; timeMachineYear = null; loadPlatformGames(platform.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(platform.emoji, fontSize = 12.sp)
                                Text(platform.label, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else ScrapbookDark, fontSize = 12.sp)
                            }
                        }
                        LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                                            }
                    }
                }
            }

            // Genre filter chips
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(gameGenreFilters) { jumpIndex, genre ->
                    Box(modifier = Modifier.jumpIn(jumpIndex)) {
                    val isSelected = selectedGenre == genre
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "genreChip_$genre")
                    Box(
                        modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) CGreen else Color.White.copy(alpha = 0.46f))
                            .border(width = if (isSelected) 1.5.dp else 1.dp, color = if (isSelected) CGreenDeep else CGreenDeep.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                            .clickable { pressed = true; selectedGenre = genre }.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(if (genre == "ALL") "ALL" else igdbGenreAbbr(genre), fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                                    }
                }
            }

            // Content
            when {
                isCurrentlyLoading && !hasSearched && timeMachineYear == null -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                        item { NowLoadingIndicator(label = "LOADING CARTRIDGES") }
                        items(count = 6) { if (isGridView) ShimmerGameGridCard() else ShimmerGameCard() }
                    }
                }
                isTimeMachineLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NowLoadingIndicator(label = "WARPING TO $timeMachineYear")
                    }
                }
                filteredGames.isEmpty() && !hasSearched && !isCurrentlyLoading && timeMachineYear == null && !isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GameOverState(
                            message = "IGDB unavailable — could not load games. Check your connection.",
                            onRetry = { platformGames.clear(); platformLoading.clear(); loadPlatformGames(selectedPlatformId) }
                        )
                    }
                }
                filteredGames.isEmpty() && (hasSearched || timeMachineYear != null) && !isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text("🎮", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No games found", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp)
                            Text("Try a different search", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                        }
                    }
                }
                else -> {
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (!hasSearched && timeMachineYear == null && gameOfTheDay != null) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    RetroHubPageHero(
                                        config = gameDatabaseHeroConfig,
                                        onCtaClick = { /* already on game database */ }
                                    )
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    RetroHubPageTicker(config = gameDatabaseHeroConfig)
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GameOfTheDayCard(game = gameOfTheDay, onRead = { selectedGame = gameOfTheDay })
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    HigherLowerEntryCard(onPlay = { showHigherLower = true })
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HallOfFameSection(
                                        games = currentPlatformGames,
                                        accentColor = currentPlatform.accentColor,
                                        onClick = { selectedGame = it }
                                    )
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    TimeMachineSection(onYearSelected = { year ->
                                        timeMachineYear = year; isTimeMachineLoading = true
                                        scope.launch {
                                            try {
                                                val results = IGDBRepository.searchGames("$year")
                                                timeMachineResults = results.filter { it.releaseYear == year }.ifEmpty { results }
                                            } catch (e: Exception) { timeMachineResults = emptyList() }
                                            isTimeMachineLoading = false
                                        }
                                    })
                                }
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                                    SectionHeaderLabel(title = "${currentPlatform.emoji} ${currentPlatform.label} GAMES", count = filteredGames.size, neonAlpha = neonAlpha, accentColor = currentPlatform.accentColor)
                                }
                            }
                            itemsIndexed(
                                items = filteredGames,
                                key = { _, g -> g.id.toString() }
                            ) { index, game ->
                                GameGridItemAnimated(index = index, game = game, accentColor = currentPlatform.accentColor, onClick = { selectedGame = game })
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (!hasSearched && timeMachineYear == null && gameOfTheDay != null) {
                                item {
                                    RetroHubPageHero(
                                        config = gameDatabaseHeroConfig,
                                         onCtaClick = { /* already on game database */ }
                                    )
                                }
                                item {
                                    RetroHubPageTicker(config = gameDatabaseHeroConfig)
                                }
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GameOfTheDayCard(game = gameOfTheDay, onRead = { selectedGame = gameOfTheDay })
                                }
                                item { HigherLowerEntryCard(onPlay = { showHigherLower = true }) }
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HallOfFameSection(
                                        games = currentPlatformGames,
                                        accentColor = currentPlatform.accentColor,
                                        onClick = { selectedGame = it }
                                    )
                                }
                                item {
                                    TimeMachineSection(onYearSelected = { year ->
                                        timeMachineYear = year; isTimeMachineLoading = true
                                        scope.launch {
                                            try {
                                                val results = IGDBRepository.searchGames("$year")
                                                timeMachineResults = results.filter { it.releaseYear == year }.ifEmpty { results }
                                            } catch (e: Exception) { timeMachineResults = emptyList() }
                                            isTimeMachineLoading = false
                                        }
                                    })
                                }
                                item { SectionHeaderLabel(title = "${currentPlatform.emoji} ${currentPlatform.label} GAMES", count = filteredGames.size, neonAlpha = neonAlpha, accentColor = currentPlatform.accentColor) }
                            }
                            if (hasSearched) {
                                item { SectionHeaderLabel(title = "🔍 SEARCH RESULTS", count = filteredGames.size, neonAlpha = neonAlpha, accentColor = CGreen) }
                            }
                            if (timeMachineYear != null && !isTimeMachineLoading) {
                                item { SectionHeaderLabel(title = "⏰ GAMES FROM $timeMachineYear", count = filteredGames.size, neonAlpha = neonAlpha, accentColor = CGreen) }
                            }
                            itemsIndexed(
                                items = filteredGames,
                                key = { _: Int, g: IGDBGame -> g.id.toString() }
                            ) { index: Int, game: IGDBGame ->
                                GameListItemAnimated(index = index, game = game, accentColor = currentPlatform.accentColor, onClick = { selectedGame = game })
                            }
                        }
                    }
                }
            }
        } // end main column

        // Surprise Me FAB
        if (filteredGames.isNotEmpty() && !isSearching) {
            FloatingActionButton(
                onClick = { if (filteredGames.isNotEmpty()) surpriseGame = filteredGames.random() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = CGreen,
                contentColor = ScrapbookDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("🎲", fontSize = 22.sp)
            }
        }
    }
    } // else (browser)
    } // CompositionLocalProvider
    } // AnimatedContent
    } // SharedTransitionLayout
}

// ─── Game List Card ────────────────────────────────────────────────────────────

@Composable
fun GameListCard(game: IGDBGame, accentColor: Color = CGreen, onClick: () -> Unit) {
    val glowAlpha by rememberGlowPhase(0.4f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "listPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "listShadow")
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(14.dp)).background(accentColor.copy(alpha = glowAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(accentColor))
        Box(
            modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { pressed = true; onClick() }
        ) {
            // Glossy specular line
            Box(modifier = Modifier.fillMaxWidth().height(2.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.7f), Color.Transparent)))
                .align(Alignment.TopCenter))
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Cover art, shaped like the game's real cartridge / case
                val collectionStatus = GameCollection.entries[game.id]?.status
                Box(modifier = Modifier.size(width = 70.dp, height = 80.dp)) {
                    CartridgeFrame(gameMediaFor(game.platforms), Modifier.fillMaxSize()) {
                        if (game.coverUrl != null) {
                            AsyncImage(model = game.coverUrl, contentDescription = game.name,
                                contentScale = ContentScale.Crop, modifier = Modifier.sharedCover("game-cover-${game.id}").fillMaxSize())
                        } else {
                            Text("🎮", fontSize = 24.sp)
                        }
                    }
                    if (collectionStatus != null) {
                        CollectionStampBadge(collectionStatus, Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-2).dp))
                    }
                    // ESRB badge
                    if (game.esrbRating != null) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(ScrapbookDark.copy(alpha = 0.85f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)) {
                            Text(game.esrbRating, fontFamily = BangersFontFamily, color = CGreen, fontSize = 7.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark,
                        fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    // Developer + year row
                    if (game.developer != null || game.releaseYear != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            game.developer?.let {
                                Text(it, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                    color = accentColor.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            }
                            game.releaseYear?.let {
                                Text("$it", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                    color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    // Rating + game modes
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        game.rating?.let { rating ->
                            val rColor = ratingColor(rating)
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(rColor.copy(alpha = 0.12f))
                                .border(1.dp, rColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily,
                                    color = rColor, fontSize = 11.sp)
                            }
                        }
                        game.gameModes.take(3).forEach { mode ->
                            Text(igdbGameModeIcon(mode), fontSize = 12.sp)
                        }
                    }
                    // Genre chips
                    if (game.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            game.genres.take(3).forEach { genre ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                    Text(igdbGenreAbbr(genre), fontFamily = BangersFontFamily,
                                        color = accentColor.copy(alpha = 0.9f), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
                Icon(IconChevronRight, contentDescription = null,
                    tint = accentColor.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Game Grid Card ────────────────────────────────────────────────────────────

@Composable
fun GameGridCard(game: IGDBGame, accentColor: Color = CGreen, onClick: () -> Unit) {
    val glowAlpha by rememberGlowPhase(0.4f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gridPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gridShadow")
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(6.dp, 6.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(alpha = glowAlpha * 0.25f)))
        Box(modifier = Modifier.matchParentSize().offset(shadowOff.dp, shadowOff.dp).clip(RoundedCornerShape(12.dp)).background(accentColor))
        Box(modifier = Modifier.fillMaxWidth().offset(y = pressAnim.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
            .clickable { pressed = true; onClick() }
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(ComicGlassBg),
                    contentAlignment = Alignment.Center) {
                    CartridgeFrame(gameMediaFor(game.platforms), Modifier.fillMaxSize().padding(4.dp)) {
                        if (game.coverUrl != null) {
                            AsyncImage(model = game.coverUrl, contentDescription = game.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.sharedCover("game-cover-${game.id}").fillMaxSize())
                        } else {
                            Text("🎮", fontSize = 28.sp)
                        }
                    }
                    GameCollection.entries[game.id]?.status?.let { st ->
                        CollectionStampBadge(st, Modifier.align(Alignment.TopStart).padding(4.dp))
                    }
                    // Glossy specular on cover
                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent)))
                        .align(Alignment.TopCenter))
                    // Gradient overlay at bottom
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.55f)))))
                    // Rating badge top-right
                    game.rating?.let { rating ->
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(ratingColor(rating))
                            .padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily,
                                color = Color.White, fontSize = 9.sp)
                        }
                    }
                    // Year badge bottom-left
                    game.releaseYear?.let { year ->
                        Text("$year", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp,
                            modifier = Modifier.align(Alignment.BottomStart).padding(5.dp))
                    }
                }
                // Title + genre row
                Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark,
                        fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                    if (game.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(game.genres.take(2).joinToString(" · ") { igdbGenreAbbr(it) },
                            fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                            color = accentColor.copy(alpha = 0.7f), fontSize = 9.sp)
                    }
                }
            }
            // Glossy line at very top of card
            Box(modifier = Modifier.fillMaxWidth().height(1.5.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent)))
                .align(Alignment.TopCenter))
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Game Detail Screen ────────────────────────────────────────────────────────

@Composable
fun GameDetailScreen(game: IGDBGame, onBack: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🕹️ INFO", "🎬 TRAILER", "⭐ COMMUNITY")
    var trailerVideoId by remember { mutableStateOf<String?>(null) }
    var isLoadingTrailer by remember { mutableStateOf(false) }
    var trailerExpanded by remember { mutableStateOf(false) }
    var myRating by remember { mutableStateOf(0) }
    var isSubmittingRating by remember { mutableStateOf(false) }

    // Eagerly load trailer on screen open: IGDB video ID first, then YouTube search fallback
    LaunchedEffect(game.id) {
        if (!isLoadingTrailer) {
            isLoadingTrailer = true
            trailerVideoId = game.videoIds.firstOrNull() ?: searchYouTubeTrailer(game.name)
            isLoadingTrailer = false
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            // Already loading eagerly above; nothing extra needed
        }
        if (selectedTab == 2) {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val myDoc = FirebaseFirestore.getInstance()
                        .collection("game_ratings").document(game.id.toString())
                        .collection("user_ratings").document(uid).get().await()
                    myRating = (myDoc.getLong("stars") ?: 0L).toInt()
                }
            } catch (e: Exception) { }
        }
    }

    val neonT = rememberInfiniteTransition(label = "detailNeon")
    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "detailKenBurns")
    val pulseScale by rememberGlowRange(1f, 1.04f)

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {

            item {
                // Hero
                Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize().scale(kenBurns).blur(8.dp), alpha = 0.4f)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(ComicGlassBg.copy(alpha = 0.15f), ComicGlassBg.copy(alpha = 0.92f)))))
                    val scanT = rememberInfiniteTransition(label = "heroScan")
                    val scanY by scanT.animateFloat(initialValue = -340f, targetValue = 340f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "heroScanY")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).offset(y = scanY.dp).background(CGreen.copy(alpha = 0.08f)))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 44.dp, start = 12.dp).clip(CircleShape).background(CGreen).border(2.dp, ScrapbookBorder, CircleShape).clickable { onBack() }.padding(8.dp)) {
                        Icon(IconArrowBack, contentDescription = "Back", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }
                    Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box {
                            Box(modifier = Modifier.size(110.dp).blur(20.dp).background(CGreen.copy(alpha = neonAlpha * 0.4f), RoundedCornerShape(14.dp)))
                            Box(modifier = Modifier.size(110.dp).offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                            Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))) {
                                // Green stripe
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                                if (game.coverUrl != null) AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.sharedCover("game-cover-${game.id}").fillMaxSize())
                                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 40.sp) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                            // Green stripe
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("RETRO CLASSIC", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            TypewriterText(game.name, androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, lineHeight = 30.sp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                game.releaseYear?.let { year ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("📅", fontSize = 11.sp)
                                        Text("$year", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                                game.rating?.let { rating ->
                                    val rColor = ratingColor(rating)
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(rColor.copy(alpha = 0.2f)).border(1.dp, rColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily, color = rColor, fontSize = 12.sp)
                                    }
                                }
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.07f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text("IGDB #${game.id}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Tab strip (ink underline)
                InkTabRow(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            when (selectedTab) {

                // INFO TAB
                0 -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // ── My Collection (Owned / Playing / Beaten / Wishlist) ──
                        CollectionPicker(game = game)

                        // ── Trailer preview card ──────────────────────────────
                        val thumbUrl = trailerVideoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                        ) {
                            // Green stripe
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                            if (trailerExpanded && trailerVideoId != null) {
                                // Green stripe
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                                // Inline player
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Green stripe
                                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                                    YoutubePlayerCard(
                                        youtubeVideoId = trailerVideoId!!,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                                        lifecycleOwner = lifecycleOwner
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { trailerExpanded = false }.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("▼", color = CAcRed, fontFamily = BangersFontFamily, fontSize = 12.sp)
                                        Text("COLLAPSE", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 1.sp)
                                    }
                                }
                            } else {
                                // Thumbnail + play button
                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                        .clickable { if (trailerVideoId != null) trailerExpanded = true else selectedTab = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumbUrl != null) {
                                        AsyncImage(model = thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(thumbUrl).fillMaxSize().clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)))
                                        // Tint overlay
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(ScrapbookDark.copy(alpha = 0.35f)))
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(ScrapbookDark.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                                            Text("🎬", fontSize = 40.sp)
                                        }
                                    }
                                    // Play button
                                    Box(
                                        modifier = Modifier.size(56.dp).clip(CircleShape)
                                            .background(CAcRed)
                                            .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("▶", color = Color.White, fontSize = 22.sp)
                                    }
                                    if (isLoadingTrailer) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp), strokeWidth = 3.dp)
                                    }
                                }
                                // Label row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val dotScale by neonT.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "trailerDot")
                                    Box(modifier = Modifier.size(8.dp).scale(dotScale).clip(CircleShape).background(CAcRed))
                                    Text(
                                        if (trailerVideoId != null) "OFFICIAL TRAILER" else if (isLoadingTrailer) "LOADING TRAILER..." else "NO TRAILER FOUND",
                                        fontFamily = BangersFontFamily,
                                        color = ScrapbookDark,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (trailerVideoId != null) {
                                        Text("TAP TO PLAY ▶", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 10.sp, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }

                        // Rating card
                        game.rating?.let { rating ->
                            val rColor = ratingColor(rating)
                            val score = (rating / 10.0).toInt()
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(colors = listOf(rColor.copy(alpha = 0.15f), ComicGlassBg)))
                                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(
                                        modifier = Modifier.size(72.dp).scale(pulseScale).clip(CircleShape)
                                            .background(Brush.radialGradient(colors = listOf(rColor.copy(alpha = 0.3f), rColor.copy(alpha = 0.05f))))
                                            .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(rColor.copy(alpha = neonAlpha), rColor.copy(alpha = 0.3f))), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$score", fontFamily = BangersFontFamily, color = rColor, fontSize = 30.sp, lineHeight = 30.sp)
                                            Text("/10", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = rColor.copy(alpha = 0.6f), fontSize = 10.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(when { rating >= 80.0 -> "🏆 OUTSTANDING"; rating >= 70.0 -> "⭐ GREAT"; rating >= 60.0 -> "👍 GOOD"; rating >= 50.0 -> "😐 MIXED"; else -> "👎 POOR" }, fontFamily = BangersFontFamily, color = rColor, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("IGDB Score: ${String.format("%.1f", rating)}/100", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookDark.copy(alpha = 0.08f))) {
                                            val animRating by animateFloatAsState(targetValue = (rating / 100.0).toFloat().coerceIn(0f, 1f), animationSpec = tween(1400, easing = LinearOutSlowInEasing), label = "ratingBar")
                                            Box(modifier = Modifier.fillMaxWidth(animRating).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(colors = listOf(rColor.copy(alpha = 0.6f), rColor))))
                                        }
                                    }
                                }
                            }
                        }

                        // Fun facts strip — dark + yellow only
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                Triple("🎮", "PLATFORM", "RETRO"),
                                Triple("🌍", "REGION", "GLOBAL"),
                                Triple("👾", "ERA", game.releaseYear?.let { if (it < 1990) "8-BIT" else if (it < 2000) "16-BIT" else "3D ERA" } ?: "CLASSIC")
                            ).forEach { statTriple ->
                                val emoji = statTriple.first; val label = statTriple.second; val value = statTriple.third
                                Column(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.92f))
                                        .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(value, fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 13.sp)
                                    Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 9.sp, letterSpacing = 1.sp)
                                }
                            }
                        }

                        // THE STORY — unchanged, already yellow
                        if (!game.summary.isNullOrBlank()) {
                            var showFullSummary by remember { mutableStateOf(false) }
                            val words = game.summary.split(" ")
                            val isLong = words.size > 40
                            val displayText = if (!isLong || showFullSummary) game.summary else words.take(40).joinToString(" ") + "..."
                            val firstChar = displayText.firstOrNull()?.toString() ?: ""
                            val restText = if (displayText.length > 1) displayText.substring(1) else ""

                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))) {
                                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(CGreen.copy(alpha = 0.12f), Color.Transparent))).padding(horizontal = 18.dp, vertical = 14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("THE STORY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 2.sp)
                                            Text("Game overview & lore", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 11.sp)
                                        }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen.copy(alpha = 0.1f)).border(1.dp, CGreen.copy(alpha = 0.25f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("pg. 01", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 10.sp)
                                        }
                                    }
                                }
                                HorizontalDivider(color = ScrapbookDark.copy(alpha = 0.1f))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val readMins = (game.summary.split(" ").size / 200).coerceAtLeast(1)
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark.copy(alpha = 0.06f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("⏱", fontSize = 11.sp)
                                            Text("$readMins min read", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
                                        }
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CGreen.copy(alpha = 0.08f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("📖 LORE", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 10.sp, letterSpacing = 1.sp)
                                    }
                                }
                                Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                        if (firstChar.isNotBlank()) {
                                            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(colors = listOf(CGreen.copy(alpha = 0.2f), CGreen.copy(alpha = 0.04f)))).border(1.dp, CGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                Text(firstChar, fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 32.sp)
                                            }
                                        }
                                        Text(restText, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.75f), fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                                    }
                                    if (!isLong || showFullSummary) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CGreen.copy(alpha = 0.05f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(10.dp)).padding(14.dp)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Box(modifier = Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                                    Text("\"${game.name} remains one of the most iconic titles of its era — a testament to the creativity of its time.\"", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp, lineHeight = 20.sp, fontStyle = FontStyle.Italic)
                                                }
                                            }
                                        }
                                    if (isLong) {
                                        var readMorePressed by remember { mutableStateOf(false) }
                                        val readMoreScale by animateFloatAsState(targetValue = if (readMorePressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "readMoreScale")
                                        Box(modifier = Modifier.fillMaxWidth().scale(readMoreScale).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { readMorePressed = true; showFullSummary = !showFullSummary }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(if (showFullSummary) "▲  COLLAPSE" else "▼  READ MORE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, letterSpacing = 1.sp)
                                        }
                                        LaunchedEffect(readMorePressed) { if (readMorePressed) { delay(150); readMorePressed = false } }
                                    }
                                }
                            }
                        }
                        } // end if (!game.summary.isNullOrBlank())

                        // Reaction bar — dark + yellow only
                        var gameReactions by remember { mutableStateOf(mapOf("🔥" to 0, "❤️" to 0, "🎮" to 0, "👾" to 0)) }
                        var userReaction by remember { mutableStateOf<String?>(null) }
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                Text("REACT TO THIS GAME", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                gameReactions.forEach { reactionPair ->
                                    val emoji = reactionPair.key; val count = reactionPair.value
                                    val isReacted = userReaction == emoji
                                    var popped by remember { mutableStateOf(false) }
                                    val popScale by animateFloatAsState(targetValue = if (popped) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "pop_$emoji")
                                    Box(
                                        modifier = Modifier.scale(popScale).clip(RoundedCornerShape(20.dp))
                                            .background(if (isReacted) CGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.46f))
                                            .border(1.5.dp, if (isReacted) CGreenDeep else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                            .clickable {
                                                popped = true
                                                gameReactions = gameReactions.toMutableMap().apply {
                                                    if (isReacted) { this[emoji] = (this[emoji] ?: 1) - 1; userReaction = null }
                                                    else { userReaction?.let { prev -> this[prev] = (this[prev] ?: 1) - 1 }; this[emoji] = (this[emoji] ?: 0) + 1; userReaction = emoji }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(emoji, fontSize = 15.sp)
                                            Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) CGreenDeep else ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp)
                                        }
                                    }
                                    LaunchedEffect(popped) { if (popped) { delay(200); popped = false } }
                                }
                            }
                        }

                        // Did You Know — dark + yellow, no purple
                        val didYouKnowFacts = remember {
                            listOf(
                                "🕹️ This game was part of a golden era of gaming that shaped the entire industry.",
                                "👾 Games from this era were often coded by single developers working alone.",
                                "📼 Physical cartridges had very limited memory — developers had to be incredibly creative.",
                                "🏆 Completing these games without guides was considered a major achievement.",
                                "💾 Save states didn't exist — you played until you won or started completely over.",
                                "🎵 Chiptune soundtracks were composed to work within extreme memory limits.",
                                "🖥️ Early game manuals were often 50+ pages — reading was part of the experience."
                            )
                        }
                        var currentFact by remember { mutableStateOf(didYouKnowFacts.random()) }
                        var factVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { delay(400); factVisible = true }
                        val factAlpha by animateFloatAsState(targetValue = if (factVisible) 1f else 0f, animationSpec = tween(700), label = "factAlpha")
                        var refreshPressed by remember { mutableStateOf(false) }
                        val refreshScale by animateFloatAsState(targetValue = if (refreshPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "refreshScale")

                        Column(
                            modifier = Modifier.fillMaxWidth().alpha(factAlpha)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                Text("DID YOU KNOW?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier.scale(refreshScale).size(30.dp).clip(CircleShape)
                                        .background(CGreen.copy(alpha = 0.12f))
                                        .border(1.dp, CGreen.copy(alpha = 0.4f), CircleShape)
                                        .clickable { refreshPressed = true; currentFact = didYouKnowFacts.filter { it != currentFact }.random() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↻", color = CGreenDeep, fontSize = 16.sp)
                                }
                                LaunchedEffect(refreshPressed) { if (refreshPressed) { delay(150); refreshPressed = false } }
                            }
                            Text(currentFact, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.75f), fontSize = 13.sp, lineHeight = 20.sp)
                        }

                        // Gaming Timeline — dark + yellow only
                        game.releaseYear?.let { year ->
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                    Text("GAMING TIMELINE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
                                }
                                val eraItems = buildList {
                                    add(Triple("1977", "🕹️", "Atari era begins"))
                                    add(Triple("1983", "💥", "Video game crash"))
                                    add(Triple("1985", "🍄", "NES launches globally"))
                                    if (year in 1989..2000) add(Triple("1989", "📱", "Game Boy drops"))
                                    add(Triple("1990", "🌟", "16-bit console wars"))
                                    if (year in 1994..2002) add(Triple("1994", "💿", "PlayStation arrives"))
                                    if (year in 1996..2005) add(Triple("1996", "🌐", "N64 era begins"))
                                    add(Triple("$year", "🎮", game.name))
                                }.distinctBy { it.first }.sortedBy { it.first.toIntOrNull() ?: 9999 }

                                eraItems.forEachIndexed { index, (eraYear, emoji, label) ->
                                    val isCurrentGame = eraYear == "$year"
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                                            if (index > 0) Box(modifier = Modifier.width(2.dp).height(14.dp).background(if (isCurrentGame) CGreen.copy(alpha = 0.5f) else ScrapbookDark.copy(alpha = 0.1f)))
                                            Box(
                                                modifier = Modifier.size(if (isCurrentGame) 20.dp else 12.dp).clip(CircleShape)
                                                    .background(if (isCurrentGame) CGreen else ScrapbookDark.copy(alpha = 0.12f))
                                                    .then(if (isCurrentGame) Modifier.border(2.dp, CGreen.copy(alpha = neonAlpha), CircleShape) else Modifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCurrentGame) Text("★", fontSize = 10.sp, color = ScrapbookDark)
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrentGame) CGreen.copy(alpha = 0.1f) else Color.Transparent)
                                                .then(if (isCurrentGame) Modifier.border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)) else Modifier)
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(emoji, fontSize = 14.sp)
                                            Text(eraYear, fontFamily = BangersFontFamily, color = if (isCurrentGame) CGreenDeep else ScrapbookDark.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.width(38.dp))
                                            Text(label, fontFamily = NunitoFontFamily, color = if (isCurrentGame) ScrapbookDark else ScrapbookDark.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = if (isCurrentGame) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }

                        // Retro Meter — dark + yellow, no random colors
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                Text("RETRO METER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                                Text("📟", fontSize = 16.sp)
                            }
                            val meters = listOf(
                                "NOSTALGIA" to (game.releaseYear?.let { ((2024 - it).coerceIn(0, 40).toFloat() / 40f) } ?: 0.8f),
                                "DIFFICULTY" to (game.rating?.let { r -> (1f - (r.toFloat() / 100f).coerceIn(0f, 1f)) * 0.7f + 0.2f } ?: 0.6f),
                                "INFLUENCE" to (game.rating?.let { r -> (r.toFloat() / 100f).coerceIn(0f, 1f) } ?: 0.5f),
                                "REPLAY VALUE" to 0.75f
                            )
                            meters.forEach { meterPair ->
                                val label = meterPair.first; val value = meterPair.second
                                val animValue by animateFloatAsState(targetValue = value, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "meter_$label")
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.width(90.dp))
                                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.08f))) {
                                        Box(modifier = Modifier.fillMaxWidth(animValue).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(colors = listOf(CGreen.copy(alpha = 0.5f), CGreen))))
                                    }
                                    Text("${(value * 100f).toInt()}%", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                }
                            }
                        }

                        // Game Specs — unchanged, already uses DetailRowDark which is yellow
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                                Text("GAME SPECS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            game.releaseYear?.let { DetailRowDark("📅  Release Year", "$it", neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f)) }
                            game.rating?.let { DetailRowDark("🎯  IGDB Score", "${String.format("%.1f", it)}/100", neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f)) }
                            if (game.developer != null) { DetailRowDark("🏢  Developer", game.developer, neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f)) }
                            if (game.genres.isNotEmpty()) { DetailRowDark("🎭  Genres", game.genres.joinToString(", "), neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f)) }
                            if (game.gameModes.isNotEmpty()) { DetailRowDark("👾  Modes", game.gameModes.joinToString(" · ") { igdbGameModeIcon(it) + " " + it }, neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f)) }
                            if (game.esrbRating != null) { DetailRowDark("🔞  ESRB", game.esrbRating, neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f)) }
                            DetailRowDark("🆔  IGDB ID", "#${game.id}", neonAlpha)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f))
                            DetailRowDark("🗓️  Era", game.releaseYear?.let { if (it < 1990) "8-Bit Era" else if (it < 2000) "16-Bit Era" else "3D Era" } ?: "Classic", neonAlpha)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = ScrapbookDark.copy(alpha = 0.1f))
                            DetailRowDark("📊  Status", "RETRO CLASSIC", neonAlpha)
                        }

                        // ── Connections to other pages ──
                        GameConnectionsSection(game = game)
                    }
                }

                // TRAILER TAB
                1 -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                            Text("TRAILER & GAMEPLAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                        }
                        when {
                            isLoadingTrailer -> Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = CGreen, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                                    Text("📡  Searching for trailer...", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp)
                                    Text("Scanning YouTube archives", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 11.sp)
                                }
                            }
                            trailerVideoId != null -> Box(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Green stripe
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val dotScale by neonT.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "dotScale")
                                    Box(modifier = Modifier.size(10.dp).scale(dotScale).clip(CircleShape).background(CAcRed))
                                    Text("NOW PLAYING", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 11.sp, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("YouTube", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 10.sp)
                                }
                                Text("${game.name} — Trailer / Gameplay", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val videoId = trailerVideoId ?: return@Column
                                YoutubePlayerCard(youtubeVideoId = videoId, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)), lifecycleOwner = lifecycleOwner)
                            } // end Column
                            } // end shadow Box
                            else -> Box(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                // Green stripe
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📺", fontSize = 44.sp)
                                    Text("NO TRAILER FOUND", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 16.sp, letterSpacing = 1.sp)
                                    Text("This game may predate online video archives", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 11.sp)
                                }
                            } // end inner Box
                            } // end shadow Box
                        }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = 0.08f)).border(1.dp, CGreen.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("💡", fontSize = 18.sp)
                            Text("Trailers are sourced from YouTube and may vary by game.", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }

                // COMMUNITY TAB
                2 -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                            Text("COMMUNITY HUB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                        }
                        Box(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Green stripe
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⭐", fontSize = 22.sp)
                                Column {
                                    Text("RATE THIS GAME", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                                    Text("How would you score it?", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                val filledIcon = Icons.Filled.Star
                                val outlinedIcon = Icons.Filled.Star
                                for (i in 1..5) {
                                    var starPressed by remember { mutableStateOf(false) }
                                    val starScale by animateFloatAsState(targetValue = if (starPressed) 1.4f else if (i <= myRating) 1.1f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "star_$i")
                                    val starIcon = if (i <= myRating) filledIcon else outlinedIcon
                                    Icon(imageVector = starIcon, contentDescription = null, tint = if (i <= myRating) CGreenDeep else ScrapbookDark.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp).scale(starScale).clickable {
                                            starPressed = true; myRating = i
                                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@clickable
                                            isSubmittingRating = true
                                            FirebaseFirestore.getInstance().collection("game_ratings").document(game.id.toString()).collection("user_ratings").document(uid)
                                                .set(mapOf("stars" to i, "timestamp" to System.currentTimeMillis()))
                                                .addOnSuccessListener { isSubmittingRating = false }.addOnFailureListener { isSubmittingRating = false }
                                        })
                                    LaunchedEffect(starPressed) { if (starPressed) { delay(200); starPressed = false } }
                                }
                                if (isSubmittingRating) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                            if (myRating > 0) {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(CGreen.copy(alpha = 0.12f)).border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✅", fontSize = 14.sp)
                                    Text("You rated this $myRating/5 stars", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = CGreenDeep, fontSize = 13.sp)
                                }
                            }
                        } // end community Column
                        } // end shadow Box
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors = listOf(CGreen.copy(alpha = 0.12f), ComicGlassBg))).border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 24.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ADD TO MY TOP GAMES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                                Text("Go to Profile → Edit → Top Games to showcase this game", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 17.sp)
                            }
                            Text("→", fontFamily = BangersFontFamily, color = CGreen.copy(alpha = neonAlpha), fontSize = 20.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(CAcRed.copy(alpha = 0.12f)).border(1.dp, CAcRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("📣", fontSize = 24.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SHARE WITH FRIENDS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                                Text("Tell your RetroHub crew about this gem", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp, lineHeight = 17.sp)
                            }
                            Text("→", fontFamily = BangersFontFamily, color = CAcRed.copy(alpha = neonAlpha), fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Detail row for dark theme ─────────────────────────────────────────────────

@Composable
fun DetailRowDark(label: String, value: String, neonAlpha: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp)
        Text(value, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
    }
}

@Composable
fun GameDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 14.sp)
        Text(value, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
    }
}

@Composable
fun GameInfoCard(title: String, content: String, backgroundColor: Color) {
    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = backgroundColor, cornerRadius = 10.dp, shadowOffset = 2.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(content, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
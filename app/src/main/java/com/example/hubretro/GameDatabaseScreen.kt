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
    GamePlatform("all", "ALL", "🕹️", ScrapbookYellow, -1, listOf(
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
    GamePlatform("sega", "SEGA", "💿", Color(0xFF0066CC), 29, listOf(
        "sonic", "streets of rage", "golden axe", "altered beast", "phantasy star",
        "shining force", "ecco dolphin", "earthworm jim", "vectorman", "ristar",
        "comix zone", "gunstar heroes", "beyond oasis", "toejam earl", "shinobi"
    )),
    GamePlatform("ps1", "PS1", "💙", Color(0xFF003087), 7, listOf(
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
    GamePlatform("ps3", "PS3", "⚫", Color(0xFF1A1A2E), 9, listOf(
        "uncharted", "the last of us", "god of war 3", "demon souls", "red dead redemption",
        "heavy rain", "beyond two souls", "journey", "flower", "littlebigplanet",
        "metal gear solid 4", "infamous", "resistance fall of man", "killzone 2",
        "valkyria chronicles", "ni no kuni", "tales of graces"
    )),
    GamePlatform("wii", "WII", "⚪", Color(0xFF888888), 5, listOf(
        "super mario galaxy", "zelda twilight princess", "wii sports", "mario kart wii",
        "new super mario bros wii", "metroid other m", "xenoblade chronicles",
        "the last story", "pandoras tower", "donkey kong country returns",
        "kirby epic yarn", "mario party 8", "fire emblem radiant dawn",
        "super paper mario", "okami wii", "mad world", "no more heroes"
    )),
    GamePlatform("arcade", "ARCADE", "🕹️", Color(0xFFFF6B00), -1, listOf(
        "pac man", "galaga", "space invaders", "donkey kong arcade", "street fighter 2",
        "mortal kombat arcade", "tekken arcade", "time crisis", "virtua fighter",
        "out run", "daytona usa", "house of the dead", "metal slug", "king of fighters",
        "neo geo", "bubble bobble", "rainbow islands", "frogger", "centipede"
    ))
)

val gameGenreFilters = listOf(
    "ALL", "RPG", "ACTION", "PLATFORMER", "SHOOTER",
    "ADVENTURE", "ARCADE", "SPORTS", "PUZZLE", "FIGHTING", "RACING"
)

// ─── YouTube helper ────────────────────────────────────────────────────────────

suspend fun searchYouTubeTrailer(gameName: String): String? {
    return try {
        val query = "${gameName} official trailer gameplay".replace(" ", "+")
        val url = "https://www.googleapis.com/youtube/v3/search" +
                "?part=snippet&q=$query&type=video&maxResults=1" +
                "&key=AIzaSyDEqbT2eB-iVVCJi8XL4qlcror2zzoi9pI"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        items.getJSONObject(0).getJSONObject("id").optString("videoId")
    } catch (e: Exception) { null }
}

fun platformAccentColor(platformId: String): Color =
    gamePlatforms.find { it.id == platformId }?.accentColor ?: ScrapbookYellow

fun ratingColor(rating: Double): Color = when {
    rating >= 80.0 -> ScrapbookGreen
    rating >= 60.0 -> ScrapbookYellowDark
    else -> ScrapbookRed
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
        colors = listOf(ScrapbookPaper, Color.White.copy(alpha = 0.85f), ScrapbookPaper),
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
        colors = listOf(ScrapbookPaper, Color.White.copy(alpha = 0.85f), ScrapbookPaper),
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
                Text("$score/10", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
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
                    Text("$score", fontFamily = BangersFontFamily, color = Color.White, fontSize = 28.sp)
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
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookPaper).border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(4.dp))) {
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
fun SectionHeaderLabel(title: String, count: Int, neonAlpha: Float, accentColor: Color = ScrapbookYellow) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(accentColor.copy(alpha = neonAlpha)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        if (count > 0) {
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("$count", fontFamily = BangersFontFamily, color = accentColor, fontSize = 13.sp)
            }
        }
    }
}

// ─── Game of the Day ──────────────────────────────────────────────────────────

@Composable
fun GameOfTheDayCard(game: IGDBGame, onRead: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "gotdNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "gotdNeonAlpha")
    val crownScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.12f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "gotdCrownScale")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "gotdKenBurns")
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gotdCardScale")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(16.dp).background(Color(0xFFFFD700).copy(alpha = neonAlpha * 0.25f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().scale(cardScale).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFFFFD700).copy(alpha = neonAlpha), Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFFFFD700).copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)),
            backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(ScrapbookDark, Color(0xFF1A1A2E)))))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.scale(crownScale)) { Text("👑", fontSize = 20.sp) }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFD700)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("GAME OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                    game.rating?.let { rating ->
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(ratingColor(rating)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        game.releaseYear?.let { Text("$it", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    game.summary?.let {
                        Text(it, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                    }
                    var btnPressed by remember { mutableStateOf(false) }
                    val btnScale by animateFloatAsState(targetValue = if (btnPressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gotdBtnScale")
                    Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFD700)).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { btnPressed = true; pressed = true; onRead() }.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(IconInfo, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                            Text("VIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                        }
                    }
                    LaunchedEffect(btnPressed) { if (btnPressed) { delay(150); btnPressed = false } }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Time Machine ─────────────────────────────────────────────────────────────

@Composable
fun TimeMachineSection(onYearSelected: (Int) -> Unit) {
    val years = (1985..2005).toList()
    var selectedYear by remember { mutableStateOf(1995) }
    val neonT = rememberInfiniteTransition(label = "tmNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "tmNeonAlpha")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("⏰", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("TIME MACHINE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                Text("What was hot in $selectedYear?", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
            }
            var searchPressed by remember { mutableStateOf(false) }
            val btnScale by animateFloatAsState(targetValue = if (searchPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tmBtnScale")
            Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(width = 1.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp)).clickable { searchPressed = true; onYearSelected(selectedYear) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("GO", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
            }
            LaunchedEffect(searchPressed) { if (searchPressed) { delay(150); searchPressed = false } }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(years) { year ->
                val isSelected = selectedYear == year
                var pressed by remember { mutableStateOf(false) }
                val chipScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "yearChip_$year")
                Box(
                    modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                        .then(if (isSelected) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(8.dp)) else Modifier.border(1.dp, ScrapbookBorder, RoundedCornerShape(8.dp)))
                        .clickable { pressed = true; selectedYear = year }.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("$year", fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 13.sp)
                }
                LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
    }
}

// ─── Animated item wrappers ───────────────────────────────────────────────────

@Composable
fun GameGridItemAnimated(index: Int, game: IGDBGame, accentColor: Color, onClick: () -> Unit) {
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(index) {
        delay(index * 30L)
        enterAnim.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
    }
    Box(modifier = Modifier.scale(enterAnim.value).alpha(enterAnim.value)) {
        GameGridCard(game = game, accentColor = accentColor, onClick = onClick)
    }
}

@Composable
fun GameListItemAnimated(index: Int, game: IGDBGame, accentColor: Color, onClick: () -> Unit) {
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(index) {
        delay(index * 40L)
        enterAnim.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
    }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).scale(enterAnim.value).alpha(enterAnim.value)) {
        GameListCard(game = game, accentColor = accentColor, onClick = onClick)
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

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
    var hasSearched by remember { mutableStateOf(false) }
    var selectedPlatformId by remember { mutableStateOf("all") }
    var timeMachineYear by remember { mutableStateOf<Int?>(null) }
    var timeMachineResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isTimeMachineLoading by remember { mutableStateOf(false) }

    val platformGames = remember { mutableStateMapOf<String, List<IGDBGame>>() }
    val platformLoading = remember { mutableStateMapOf<String, Boolean>() }

    val neonT = rememberInfiniteTransition(label = "gameNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "gameNeonAlpha")

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

    if (selectedGame != null) {
        GameDetailScreen(game = selectedGame!!, onBack = { selectedGame = null })
        return
    }

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
        else {
            val keywords = when (selectedGenre) {
                "RPG" -> listOf("fantasy", "legend", "quest", "rpg", "role", "pokemon", "dragon", "fire emblem", "persona", "tales", "xenoblade")
                "ACTION" -> listOf("sonic", "contra", "action", "batman", "devil", "god", "ninja", "shinobi", "gunstar", "metal gear", "uncharted", "infamous")
                "PLATFORMER" -> listOf("mario", "kirby", "crash", "banjo", "donkey", "rayman", "jak", "ratchet", "sly", "sonic", "yoshi")
                "SHOOTER" -> listOf("doom", "halo", "quake", "metroid", "contra", "gunstar", "resistance", "killzone")
                "ADVENTURE" -> listOf("zelda", "adventure", "link", "tomb", "uncharted", "ico", "shadow", "beyond", "journey")
                "ARCADE" -> listOf("pac", "galaga", "space", "castlevania", "metal slug", "king of fighters", "neo geo")
                "SPORTS" -> listOf("tennis", "soccer", "football", "basketball", "golf", "burnout", "gran turismo", "nba", "fifa")
                "PUZZLE" -> listOf("puzzle", "tetris", "columns", "block", "brain", "katamari", "wario ware")
                "FIGHTING" -> listOf("street fighter", "mortal kombat", "tekken", "soul", "king of fighters", "smash", "virtua fighter")
                "RACING" -> listOf("mario kart", "gran turismo", "f-zero", "outrun", "burnout", "daytona", "wipeout", "ridge racer")
                else -> emptyList()
            }
            displayGames.filter { game -> keywords.any { game.name.lowercase().contains(it) } }
        }
    }

    val gameOfTheDay = remember(currentPlatformGames) {
        val highRated = currentPlatformGames.filter { (it.rating ?: 0.0) >= 70.0 }
        if (highRated.isEmpty()) null
        else highRated[java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR) % highRated.size]
    }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)) {
                val scanT = rememberInfiniteTransition(label = "gameScan")
                val scanX by scanT.animateFloat(initialValue = -400f, targetValue = 400f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "gameScanX")
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.15f), Color.Transparent), startX = scanX, endX = scanX + 200f)))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        val shimmerT = rememberInfiniteTransition(label = "gameTitleShimmer")
                        val shimmerX by shimmerT.animateFloat(initialValue = -300f, targetValue = 600f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "gameTitleShimmerX")
                        Box {
                            Text("GAME DATABASE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp, letterSpacing = 2.sp)
                            Text("GAME DATABASE", fontFamily = BangersFontFamily, fontSize = 28.sp, letterSpacing = 2.sp, style = TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 100f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, 0f))))
                        }
                        Text("Powered by IGDB • ${if (!hasSearched) "${currentPlatformGames.size} games" else "${searchResults.size} results"}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    var luckyPressed by remember { mutableStateOf(false) }
                    val luckyScale by animateFloatAsState(targetValue = if (luckyPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "luckyScale")
                    Box(modifier = Modifier.scale(luckyScale).size(38.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape).clickable { luckyPressed = true; displayGames.randomOrNull()?.let { selectedGame = it } }, contentAlignment = Alignment.Center) { Text("🎲", fontSize = 18.sp) }
                    LaunchedEffect(luckyPressed) { if (luckyPressed) { delay(150); luckyPressed = false } }
                    Spacer(modifier = Modifier.width(8.dp))
                    var viewPressed by remember { mutableStateOf(false) }
                    val viewScale by animateFloatAsState(targetValue = if (viewPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "viewScale")
                    Box(modifier = Modifier.scale(viewScale).size(38.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape).clickable { viewPressed = true; isGridView = !isGridView }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (isGridView) IconViewList else IconGridView, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
                    }
                    LaunchedEffect(viewPressed) { if (viewPressed) { delay(150); viewPressed = false } }
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search any game...", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted) },
                leadingIcon = {
                    if (isSearching) CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp)
                    else Icon(IconSearch, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; focusManager.clearFocus() }) {
                            Icon(IconClose, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Platform tab strip
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).border(BorderStroke(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.3f)))) {
                LazyRow(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(gamePlatforms) { platform ->
                        val isSelected = selectedPlatformId == platform.id
                        var tabPressed by remember { mutableStateOf(false) }
                        val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "platformTab_${platform.id}")
                        Box(
                            modifier = Modifier.scale(tabScale).clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) platform.accentColor else Color.Transparent)
                                .then(if (isSelected) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(platform.accentColor.copy(alpha = neonAlpha), platform.accentColor.copy(alpha = 0.3f), platform.accentColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp)) else Modifier)
                                .clickable { tabPressed = true; selectedPlatformId = platform.id; if (hasSearched) { searchQuery = ""; hasSearched = false }; timeMachineYear = null; loadPlatformGames(platform.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(platform.emoji, fontSize = 12.sp)
                                Text(platform.label, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f), fontSize = 12.sp)
                            }
                        }
                        LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                    }
                }
            }

            // Genre filter chips
            LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(gameGenreFilters) { genre ->
                    val isSelected = selectedGenre == genre
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "genreChip_$genre")
                    Box(
                        modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                            .then(if (isSelected) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(20.dp)) else Modifier.border(1.dp, ScrapbookBorder, RoundedCornerShape(20.dp)))
                            .clickable { pressed = true; selectedGenre = genre }.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(genre, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 12.sp)
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                }
            }

            // Content
            when {
                isCurrentlyLoading && !hasSearched && timeMachineYear == null -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                        items(count = 6) { if (isGridView) ShimmerGameGridCard() else ShimmerGameCard() }
                    }
                }
                isTimeMachineLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⏰", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Traveling to $timeMachineYear...", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                        }
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GameOfTheDayCard(game = gameOfTheDay, onRead = { selectedGame = gameOfTheDay })
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GameOfTheDayCard(game = gameOfTheDay, onRead = { selectedGame = gameOfTheDay })
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
                                item { SectionHeaderLabel(title = "🔍 SEARCH RESULTS", count = filteredGames.size, neonAlpha = neonAlpha, accentColor = ScrapbookYellow) }
                            }
                            if (timeMachineYear != null && !isTimeMachineLoading) {
                                item { SectionHeaderLabel(title = "⏰ GAMES FROM $timeMachineYear", count = filteredGames.size, neonAlpha = neonAlpha, accentColor = ScrapbookYellow) }
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
        }
    }
}

// ─── Game List Card ────────────────────────────────────────────────────────────

@Composable
fun GameListCard(game: IGDBGame, accentColor: Color = ScrapbookYellow, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "listCardScale")
    val neonT = rememberInfiniteTransition(label = "listCardNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "listCardNeonAlpha")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "listKenBurns")

    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(width = 1.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = neonAlpha * 0.3f), accentColor.copy(alpha = 0.08f), accentColor.copy(alpha = neonAlpha * 0.3f))), shape = RoundedCornerShape(12.dp))
                .clickable { pressed = true; onClick() },
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookPaper).border(2.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns))
                    } else {
                        Text("🎮", fontSize = 32.sp)
                    }
                    game.rating?.let { rating ->
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(10.dp).clip(CircleShape).background(ratingColor(rating)).border(1.dp, Color.White, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp)
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        game.releaseYear?.let { year ->
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookPaper).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("$year", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp)
                            }
                        }
                        game.rating?.let { IGDBRatingBar(rating = it, compact = true) }
                    }
                    game.summary?.let { summary ->
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(summary, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    }
                }
                Icon(IconChevronRight, contentDescription = null, tint = accentColor.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Game Grid Card ────────────────────────────────────────────────────────────

@Composable
fun GameGridCard(game: IGDBGame, accentColor: Color = ScrapbookYellow, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gridCardScale")
    val neonT = rememberInfiniteTransition(label = "gridCardNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "gridCardNeonAlpha")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "gridKenBurns")

    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = neonAlpha * 0.5f), accentColor.copy(alpha = 0.1f), accentColor.copy(alpha = neonAlpha * 0.5f))), shape = RoundedCornerShape(12.dp))
                .clickable { pressed = true; onClick() },
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper), contentAlignment = Alignment.Center) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns))
                    } else {
                        Text("🎮", fontSize = 28.sp)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))))
                    game.rating?.let { rating ->
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).clip(RoundedCornerShape(5.dp)).background(ratingColor(rating)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                        }
                    }
                    game.releaseYear?.let { year ->
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.75f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                            Text("$year", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().padding(6.dp)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                }
            }
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
    var myRating by remember { mutableStateOf(0) }
    var isSubmittingRating by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && trailerVideoId == null && !isLoadingTrailer) {
            isLoadingTrailer = true
            trailerVideoId = searchYouTubeTrailer(game.name)
            isLoadingTrailer = false
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
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "detailNeonAlpha")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "detailKenBurns")
    val pulseScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.04f, animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse), label = "pulseScale")

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookDark)) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {

            item {
                // Hero
                Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns).blur(8.dp), alpha = 0.4f)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF0A0A1A).copy(alpha = 0.5f), Color(0xFF0A0A1A).copy(alpha = 0.98f)))))
                    val scanT = rememberInfiniteTransition(label = "heroScan")
                    val scanY by scanT.animateFloat(initialValue = -340f, targetValue = 340f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "heroScanY")
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).offset(y = scanY.dp).background(ScrapbookYellow.copy(alpha = 0.08f)))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 44.dp, start = 12.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape).clickable { onBack() }.padding(8.dp)) {
                        Icon(IconArrowBack, contentDescription = "Back", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }
                    Row(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box {
                            Box(modifier = Modifier.size(110.dp).blur(20.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), RoundedCornerShape(14.dp)))
                            Box(modifier = Modifier.size(110.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookPaper).border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(14.dp))) {
                                if (game.coverUrl != null) AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 40.sp) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookYellow.copy(alpha = 0.15f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("RETRO CLASSIC", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                game.releaseYear?.let { year ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("📅", fontSize = 11.sp)
                                        Text("$year", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                                game.rating?.let { rating ->
                                    val rColor = ratingColor(rating)
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(rColor.copy(alpha = 0.2f)).border(1.dp, rColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("${(rating / 10.0).toInt()}/10", fontFamily = BangersFontFamily, color = rColor, fontSize = 12.sp)
                                    }
                                }
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text("IGDB #${game.id}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Tab strip
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D1F)).border(BorderStroke(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.25f)))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            var tabPressed by remember { mutableStateOf(false) }
                            val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tab_$index")
                            Box(
                                modifier = Modifier.weight(1f).scale(tabScale).clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ScrapbookYellow else Color.White.copy(alpha = 0.05f))
                                    .then(if (!isSelected) Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable { tabPressed = true; selectedTab = index }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(tab, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.4f), fontSize = 13.sp, letterSpacing = 0.5.sp)
                            }
                            LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                        }
                    }
                }
            }

            when (selectedTab) {

                // INFO TAB
                0 -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Rating card
                        game.rating?.let { rating ->
                            val rColor = ratingColor(rating)
                            val score = (rating / 10.0).toInt()
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(colors = listOf(rColor.copy(alpha = 0.15f), Color(0xFF0D0D1F))))
                                    .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(rColor.copy(alpha = neonAlpha), rColor.copy(alpha = 0.2f), rColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp))
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
                                        Text("IGDB Score: ${String.format("%.1f", rating)}/100", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.08f))) {
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
                            ).forEach { (emoji, label, value) ->
                                Column(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0D0D1F))
                                        .border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(value, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                                    Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp, letterSpacing = 1.sp)
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

                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))) {
                                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = 0.12f), Color.Transparent))).padding(horizontal = 18.dp, vertical = 14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("THE STORY", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 2.sp)
                                            Text("Game overview & lore", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                                        }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookYellow.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.25f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text("pg. 01", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.6f), fontSize = 10.sp)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val readMins = (game.summary.split(" ").size / 200).coerceAtLeast(1)
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.05f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("⏱", fontSize = 11.sp)
                                            Text("$readMins min read", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                        }
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow.copy(alpha = 0.08f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("📖 LORE", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.6f), fontSize = 10.sp, letterSpacing = 1.sp)
                                    }
                                }
                                Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                        if (firstChar.isNotBlank()) {
                                            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = 0.04f)))).border(1.dp, ScrapbookYellow.copy(alpha = 0.25f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                Text(firstChar, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 32.sp)
                                            }
                                        }
                                        Text(restText, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp, lineHeight = 22.sp, modifier = Modifier.weight(1f))
                                    }
                                    if (!isLong || showFullSummary) {
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ScrapbookYellow.copy(alpha = 0.05f)).border(width = 1.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), ScrapbookYellow.copy(alpha = 0.1f), ScrapbookYellow.copy(alpha = neonAlpha * 0.4f))), shape = RoundedCornerShape(10.dp)).padding(14.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Box(modifier = Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                                Text("\"${game.name} remains one of the most iconic titles of its era — a testament to the creativity of its time.\"", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp, lineHeight = 20.sp, fontStyle = FontStyle.Italic)
                                            }
                                        }
                                    }
                                    if (isLong) {
                                        var readMorePressed by remember { mutableStateOf(false) }
                                        val readMoreScale by animateFloatAsState(targetValue = if (readMorePressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "readMoreScale")
                                        Box(modifier = Modifier.fillMaxWidth().scale(readMoreScale).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.04f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).clickable { readMorePressed = true; showFullSummary = !showFullSummary }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(if (showFullSummary) "▲  COLLAPSE" else "▼  READ MORE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp, letterSpacing = 1.sp)
                                        }
                                        LaunchedEffect(readMorePressed) { if (readMorePressed) { delay(150); readMorePressed = false } }
                                    }
                                }
                            }
                        }

                        // Reaction bar — dark + yellow only
                        var gameReactions by remember { mutableStateOf(mapOf("🔥" to 0, "❤️" to 0, "🎮" to 0, "👾" to 0)) }
                        var userReaction by remember { mutableStateOf<String?>(null) }
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("REACT TO THIS GAME", fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp, letterSpacing = 1.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                gameReactions.forEach { (emoji, count) ->
                                    val isReacted = userReaction == emoji
                                    var popped by remember { mutableStateOf(false) }
                                    val popScale by animateFloatAsState(targetValue = if (popped) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "pop_$emoji")
                                    Box(
                                        modifier = Modifier.scale(popScale).clip(RoundedCornerShape(20.dp))
                                            .background(if (isReacted) ScrapbookYellow.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                            .border(1.5.dp, if (isReacted) ScrapbookYellow.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
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
                                            Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) ScrapbookYellow else Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
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
                                .background(Color(0xFF0D0D1F))
                                .border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("DID YOU KNOW?", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier.scale(refreshScale).size(30.dp).clip(CircleShape)
                                        .background(ScrapbookYellow.copy(alpha = 0.12f))
                                        .border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), CircleShape)
                                        .clickable { refreshPressed = true; currentFact = didYouKnowFacts.filter { it != currentFact }.random() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↻", color = ScrapbookYellow, fontSize = 16.sp)
                                }
                                LaunchedEffect(refreshPressed) { if (refreshPressed) { delay(150); refreshPressed = false } }
                            }
                            Text(currentFact, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 20.sp)
                        }

                        // Gaming Timeline — dark + yellow only
                        game.releaseYear?.let { year ->
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                    Text("GAMING TIMELINE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp)
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
                                            if (index > 0) Box(modifier = Modifier.width(2.dp).height(14.dp).background(if (isCurrentGame) ScrapbookYellow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)))
                                            Box(
                                                modifier = Modifier.size(if (isCurrentGame) 20.dp else 12.dp).clip(CircleShape)
                                                    .background(if (isCurrentGame) ScrapbookYellow else Color.White.copy(alpha = 0.12f))
                                                    .then(if (isCurrentGame) Modifier.border(2.dp, ScrapbookYellow.copy(alpha = neonAlpha), CircleShape) else Modifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isCurrentGame) Text("★", fontSize = 10.sp, color = ScrapbookDark)
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrentGame) ScrapbookYellow.copy(alpha = 0.1f) else Color.Transparent)
                                                .then(if (isCurrentGame) Modifier.border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp)) else Modifier)
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(emoji, fontSize = 14.sp)
                                            Text(eraYear, fontFamily = BangersFontFamily, color = if (isCurrentGame) ScrapbookYellow else Color.White.copy(alpha = 0.35f), fontSize = 13.sp, modifier = Modifier.width(38.dp))
                                            Text(label, fontFamily = NunitoFontFamily, color = if (isCurrentGame) Color.White else Color.White.copy(alpha = 0.35f), fontSize = 12.sp, fontWeight = if (isCurrentGame) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }

                        // Retro Meter — dark + yellow, no random colors
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0D0D1F))
                                .border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.3f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("RETRO METER", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                                Text("📟", fontSize = 16.sp)
                            }
                            val meters = listOf(
                                "NOSTALGIA" to (game.releaseYear?.let { ((2024 - it).coerceIn(0, 40).toFloat() / 40f) } ?: 0.8f),
                                "DIFFICULTY" to (game.rating?.let { r -> (1f - (r.toFloat() / 100f).coerceIn(0f, 1f)) * 0.7f + 0.2f } ?: 0.6f),
                                "INFLUENCE" to (game.rating?.let { r -> (r.toFloat() / 100f).coerceIn(0f, 1f) } ?: 0.5f),
                                "REPLAY VALUE" to 0.75f
                            )
                            meters.forEach { (label, value) ->
                                val animValue by animateFloatAsState(targetValue = value, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "meter_$label")
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.width(90.dp))
                                    Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.06f))) {
                                        Box(modifier = Modifier.fillMaxWidth(animValue).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = 0.5f), ScrapbookYellow))))
                                    }
                                    Text("${(value * 100f).toInt()}%", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                }
                            }
                        }

                        // Game Specs — unchanged, already uses DetailRowDark which is yellow
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                                Text("GAME SPECS", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            game.releaseYear?.let { DetailRowDark("📅  Release Year", "$it", neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f)) }
                            game.rating?.let { DetailRowDark("🎯  IGDB Score", "${String.format("%.1f", it)}/100", neonAlpha); HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f)) }
                            DetailRowDark("🆔  IGDB ID", "#${game.id}", neonAlpha)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))
                            DetailRowDark("🗓️  Era", game.releaseYear?.let { if (it < 1990) "8-Bit Era" else if (it < 2000) "16-Bit Era" else "3D Era" } ?: "Classic", neonAlpha)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))
                            DetailRowDark("📊  Status", "RETRO CLASSIC", neonAlpha)
                        }
                    }
                }

                // TRAILER TAB
                1 -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                            Text("TRAILER & GAMEPLAY", fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, letterSpacing = 1.sp)
                        }
                        when {
                            isLoadingTrailer -> Box(modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = ScrapbookYellow, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                                    Text("📡  Searching for trailer...", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.7f), fontSize = 14.sp)
                                    Text("Scanning YouTube archives", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                                }
                            }
                            trailerVideoId != null -> Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookRed.copy(alpha = neonAlpha * 0.8f), ScrapbookRed.copy(alpha = 0.2f), ScrapbookRed.copy(alpha = neonAlpha * 0.8f))), shape = RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val dotScale by neonT.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "dotScale")
                                    Box(modifier = Modifier.size(10.dp).scale(dotScale).clip(CircleShape).background(ScrapbookRed))
                                    Text("NOW PLAYING", fontFamily = BangersFontFamily, color = ScrapbookRed, fontSize = 11.sp, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("YouTube", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                                }
                                Text("${game.name} — Trailer / Gameplay", fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                YoutubePlayerCard(youtubeVideoId = trailerVideoId, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)), lifecycleOwner = lifecycleOwner)
                            }
                            else -> Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📺", fontSize = 44.sp)
                                    Text("NO TRAILER FOUND", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp, letterSpacing = 1.sp)
                                    Text("This game may predate online video archives", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookYellow.copy(alpha = 0.08f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("💡", fontSize = 18.sp)
                            Text("Trailers are sourced from YouTube and may vary by game.", fontFamily = NunitoFontFamily, color = ScrapbookYellow.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }

                // COMMUNITY TAB
                2 -> item {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                            Text("COMMUNITY HUB", fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, letterSpacing = 1.sp)
                        }
                        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.6f), ScrapbookYellow.copy(alpha = 0.1f), ScrapbookYellow.copy(alpha = neonAlpha * 0.6f))), shape = RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("⭐", fontSize = 22.sp)
                                Column {
                                    Text("RATE THIS GAME", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp)
                                    Text("How would you score it?", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                val filledIcon = Icons.Filled.Star
                                val outlinedIcon = Icons.Filled.Star
                                for (i in 1..5) {
                                    var starPressed by remember { mutableStateOf(false) }
                                    val starScale by animateFloatAsState(targetValue = if (starPressed) 1.4f else if (i <= myRating) 1.1f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "star_$i")
                                    val starIcon = if (i <= myRating) filledIcon else outlinedIcon
                                    Icon(imageVector = starIcon, contentDescription = null, tint = if (i <= myRating) ScrapbookYellowDark else Color.White.copy(alpha = 0.12f),
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
                                if (isSubmittingRating) CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                            if (myRating > 0) {
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookGreen.copy(alpha = 0.12f)).border(1.dp, ScrapbookGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✅", fontSize = 14.sp)
                                    Text("You rated this $myRating/5 stars", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookGreen, fontSize = 13.sp)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = 0.12f), Color(0xFF0D0D1F)))).border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookYellow.copy(alpha = 0.15f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 24.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ADD TO MY TOP GAMES", fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp)
                                Text("Go to Profile → Edit → Top Games to showcase this game", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, lineHeight = 17.sp)
                            }
                            Text("→", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = neonAlpha), fontSize = 20.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF0D0D1F)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookRed.copy(alpha = 0.12f)).border(1.dp, ScrapbookRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("📣", fontSize = 24.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SHARE WITH FRIENDS", fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp)
                                Text("Tell your RetroHub crew about this gem", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, lineHeight = 17.sp)
                            }
                            Text("→", fontFamily = BangersFontFamily, color = ScrapbookRed.copy(alpha = neonAlpha), fontSize = 20.sp)
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
        Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
        Text(value, fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = neonAlpha), fontSize = 16.sp)
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
            Text(content, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
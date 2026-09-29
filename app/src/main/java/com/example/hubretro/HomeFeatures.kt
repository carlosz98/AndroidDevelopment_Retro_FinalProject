package com.example.hubretro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import android.media.MediaPlayer
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ─── Resource constants ───────────────────────────────────────────────────────

val ALBUMS_CARD_IMAGE    = R.drawable.ostcover6
val MAGAZINES_CARD_IMAGE = R.drawable.cover1
val ARTICLES_CARD_IMAGE  = R.drawable.article1
val PROFILE_CARD_IMAGE   = R.drawable.p1
val STREAMS_CARD_IMAGE   = R.drawable.article1
val GAMES_CARD_IMAGE     = R.drawable.game1

private val UNSPLASH_ACCESS_KEY get() = BuildConfig.UNSPLASH_ACCESS_KEY

// ─── Data model unique to HomeFeatures ───────────────────────────────────────

data class UnsplashPhoto(
    val imageUrl: String,
    val photographerName: String,
    val photographerUsername: String,
    val description: String
)

// ─── Static content lists ─────────────────────────────────────────────────────

val retroQuotes = listOf(
    "\"It's dangerous to go alone! Take this.\" — The Legend of Zelda",
    "\"Do a barrel roll!\" — Star Fox 64",
    "\"The cake is a lie.\" — Portal",
    "\"Stay a while and listen.\" — Diablo II",
    "\"Hey! Listen!\" — Navi, Ocarina of Time",
    "\"War. War never changes.\" — Fallout",
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
    "On this day in 1977, the Atari 2600 launched — the first truly successful home console.",
    "On this day in 1980, Pac-Man made its arcade debut in Japan."
)

val staticTickerItems = listOf(
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

val moodTags = listOf(
    "🔥 Hyped", "😤 Frustrated", "😍 Nostalgic", "🤯 Amazed",
    "😎 Chill", "💀 Tilted", "🏆 Winning", "😴 Grinding"
)
// ─── Network helpers ──────────────────────────────────────────────────────────

suspend fun fetchUnsplashRetroPhoto(): UnsplashPhoto? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.unsplash.com/photos/random?query=retro+gaming+vintage+arcade&orientation=landscape&client_id=$UNSPLASH_ACCESS_KEY")
            .build()
        val body = client.newCall(request).execute().body?.string() ?: return@withContext null
        val json = JSONObject(body)
        val urls = json.getJSONObject("urls")
        val user = json.getJSONObject("user")
        UnsplashPhoto(
            imageUrl = urls.getString("regular"),
            photographerName = user.getString("name"),
            photographerUsername = user.getString("username"),
            description = json.optString("description")
                .ifBlank { json.optString("alt_description") }
                .ifBlank { "Retro Gaming" }
                .replaceFirstChar { it.uppercase() }.take(60)
        )
    } catch (e: Exception) { null }
}

suspend fun fetchHomeStats(): HomeStats = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val users    = db.collection("users").get().await().size()
        val articles = db.collection("articles").get().await().size()
        HomeStats(userCount = users, articleCount = articles, albumCount = sampleAlbums.size)
    } catch (e: Exception) { HomeStats() }
}

suspend fun fetchLiveTickerItems(): List<LiveTickerItem> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val docs = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(12).get().await()
        docs.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val username = data["authorUsername"] as? String ?: return@mapNotNull null
            val type     = data["authorType"] as? String ?: data["type"] as? String ?: "text"
            val gameName = data["gameName"] as? String ?: ""
            val rating   = (data["rating"] as? Long)?.toInt() ?: 0
            val (action, subject, emoji) = when (type) {
                "game"       -> Triple("is playing", gameName.ifBlank { "a retro game" }, "🎮")
                "rating"     -> Triple("rated", "${gameName.ifBlank { "a game" }} ${"⭐".repeat(rating.coerceIn(1,5))}", "⭐")
                "ost"        -> Triple("vibing to", data["ostName"] as? String ?: "a retro OST", "🎵")
                "screenshot" -> Triple("shared a screenshot of", gameName.ifBlank { "a retro game" }, "📸")
                else         -> Triple("posted", "\"${(data["content"] as? String ?: "").take(28)}...\"", "💬")
            }
            LiveTickerItem(username = username, action = action, subject = subject, emoji = emoji)
        }
    } catch (e: Exception) { emptyList() }
}

// ─── Live Ticker Tape ─────────────────────────────────────────────────────────

@Composable
fun RetroTickerTape() {
    var liveItems by remember { mutableStateOf<List<LiveTickerItem>>(emptyList()) }
    LaunchedEffect(Unit) { liveItems = fetchLiveTickerItems() }

    val displayText = if (liveItems.isNotEmpty()) {
        liveItems.joinToString("   ★   ") { "${it.emoji} ${it.username} ${it.action} ${it.subject}" }
    } else {
        staticTickerItems.joinToString("   ★   ")
    }

    val t = rememberInfiniteTransition(label = "ticker")
    val offset by t.animateFloat(1f, -2f,
        infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart), label = "tickerOffset")

    Box(modifier = Modifier.fillMaxWidth()
        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenDeep)))
        .border(BorderStroke(1.5.dp, CGreen.copy(alpha = 0.55f)))
        .padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.wrapContentWidth(unbounded = true).offset(x = (offset * 400f).dp)) {
            repeat(2) {
                Text(displayText, fontFamily = BangersFontFamily, color = Color.White,
                    fontSize = 13.sp, letterSpacing = 1.sp, maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// ─── Stories Bar ──────────────────────────────────────────────────────────────

@Composable
fun StoriesBar(
    myStory: RetroStory?,
    stories: List<RetroStory>,
    currentUser: UserProfileData?,
    isLoading: Boolean,
    onAddStory: () -> Unit,
    onViewStory: (RetroStory) -> Unit
) {
    var gameOfDay by remember { mutableStateOf<RetroGameOfDay?>(null) }
    LaunchedEffect(Unit) { gameOfDay = fetchRetroGameOfDay() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(CGreen))
                Text("STORIES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                if (stories.isNotEmpty()) {
                    AeroGlassPill(accentColor = CGreen) {
                        Text("${stories.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PulsingDot(color = CGreen, size = 7.dp)
                Text("LIVE", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp, letterSpacing = 1.sp)
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (gameOfDay != null) item { GameOfDayStoryCard(game = gameOfDay!!) }
            item {
                FacebookYourStoryCard(
                    myStory = myStory, currentUser = currentUser,
                    onAdd = onAddStory, onView = { myStory?.let { onViewStory(it) } }
                )
            }
            if (isLoading) {
                items(4) { ShimmerStoryCircle() }
            } else {
                itemsIndexed(stories, key = { _, s -> s.id }) { index, story ->
                    SpringEntrance(delayMs = index * 60) {
                        Box(modifier = Modifier.width(100.dp).height(155.dp)) {
                            Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                                .clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                            FacebookStoryCircle(story = story, onTap = { onViewStory(story) })
                        }
                    }
                }
            }
        }
    }
}

// ─── Game of the Day Story Card ───────────────────────────────────────────────

@Composable
fun GameOfDayStoryCard(game: RetroGameOfDay) {
    val accentColor = CAcYellow
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shadow")

    Box(modifier = Modifier.width(100.dp).height(155.dp)) {
        // Comic shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp)
            .clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
        Box(modifier = Modifier.fillMaxSize().offset(x = pressAnim.dp, y = pressAnim.dp).clickable { pressed = true }) {
        if (!game.coverUrl.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                AsyncImage(model = game.coverUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize().blur(3.dp), alpha = 0.35f)
                ScanlineOverlay(modifier = Modifier.matchParentSize(), lineAlpha = 0.06f)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(CGreenDeep, CGreen))))
        }
        Box(modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(ComicGlassBg.copy(alpha = 0.52f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
        )
        Box(modifier = Modifier.fillMaxWidth().height(80.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), startY = 0f, endY = 80f)))
        GameOfDayArcRing(accentColor = accentColor)
        Column(modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            HoloBadge(label = "🏆 DAILY", cornerRadius = 6.dp, fontSize = 8.sp)
            if (!game.coverUrl.isNullOrBlank()) {
                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))) {
                    AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
                }
            } else {
                Text("🎮", fontSize = 32.sp)
            }
            Text(game.name.take(14), fontFamily = BangersFontFamily, color = ScrapbookDark,
                fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 12.sp)
        }
        ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.18f)
        } // clickable Box
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

@Composable
fun GameOfDayArcRing(accentColor: Color) {
    val t = rememberInfiniteTransition(label = "godArc")
    val sweep by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "godSweep")
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 3f; val inset = stroke / 2f
        drawArc(color = accentColor.copy(alpha = 0.25f), startAngle = 0f, sweepAngle = 360f,
            useCenter = false, topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke), style = Stroke(width = stroke))
        drawArc(color = accentColor, startAngle = -90f, sweepAngle = sweep,
            useCenter = false, topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke), style = Stroke(width = stroke))
    }
}

// ─── Your Story Card ──────────────────────────────────────────────────────────

@Composable
fun FacebookYourStoryCard(myStory: RetroStory?, currentUser: UserProfileData?, onAdd: () -> Unit, onView: () -> Unit) {
    val hasStory = myStory != null
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shadow")

    Box(modifier = Modifier.width(100.dp).height(155.dp)) {
        if (hasStory) GlowPulse(modifier = Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 14.dp, maxAlpha = 0.5f)
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp)
            .clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(modifier = Modifier.fillMaxSize().offset(x = pressAnim.dp, y = pressAnim.dp)
            .clickable { pressed = true; if (hasStory) onView() else onAdd() }) {
        Box(modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color.White, CGreenMint.copy(alpha = 0.35f))))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
        )
        Box(modifier = Modifier.fillMaxWidth().height(80.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent), startY = 0f, endY = 80f)))
        if (hasStory) StoryProgressArc(color = CGreen)
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).border(2.5.dp, ScrapbookDark, CircleShape))
                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center) {
                    when {
                        !currentUser?.profilePictureUrl.isNullOrBlank() ->
                            AsyncImage(model = currentUser!!.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(currentUser!!.profilePictureUrl).fillMaxSize())
                        currentUser?.habboUsername?.isNotBlank() == true ->
                            AsyncImage(model = habboAvatarUrl(currentUser.habboUsername, currentUser.habboRegion), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.halftoneReveal(habboAvatarUrl(currentUser.habboUsername, currentUser.habboRegion)).fillMaxSize())
                        else -> Text(currentUser?.username?.take(1)?.uppercase() ?: "?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    }
                }
                Box(modifier = Modifier.size(18.dp).clip(CircleShape)
                    .background(if (hasStory) CGreen else CGreen)
                    .border(2.dp, ScrapbookBorder.copy(alpha = 0.5f), CircleShape).align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center) {
                    if (hasStory) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    else Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(if (hasStory) "Your Story" else "Add Story", fontFamily = BangersFontFamily,
                color = ScrapbookDark, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.20f)
        } // clickable Box
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Friends' Story Circle ────────────────────────────────────────────────────

@Composable
fun FacebookStoryCircle(story: RetroStory, onTap: () -> Unit) {
    val accentColor = storyTypeColor(story.type)
    val typeEmoji   = storyTypeEmoji(story.type)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shadow")

    Box(modifier = Modifier.width(100.dp).height(155.dp)) {
        GlowPulse(modifier = Modifier.matchParentSize(), glowColor = accentColor, cornerRadius = 14.dp, maxAlpha = 0.4f)
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp)
            .clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
        Box(modifier = Modifier.fillMaxSize().offset(x = pressAnim.dp, y = pressAnim.dp).clickable { pressed = true; onTap() }) {
        if (story.gameCoverUrl.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))) {
                AsyncImage(model = story.gameCoverUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(story.gameCoverUrl).fillMaxSize().blur(4.dp), alpha = 0.3f)
                ScanlineOverlay(modifier = Modifier.matchParentSize(), lineAlpha = 0.07f)
            }
        }
        Box(modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(ComicGlassBg.copy(alpha = 0.50f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
        )
        Box(modifier = Modifier.fillMaxWidth().height(80.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), startY = 0f, endY = 80f)))
        StoryProgressArc(color = accentColor)
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(22.dp)
            .clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
            .border(1.5.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), accentColor.copy(alpha = 0.5f))), CircleShape),
            contentAlignment = Alignment.Center) {
            Text(typeEmoji, fontSize = 10.sp)
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 7.dp, bottom = 24.dp)) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f))
                .border(1.5.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.8f), accentColor.copy(alpha = 0.6f))), CircleShape),
                contentAlignment = Alignment.Center) {
                when {
                    story.authorPicUrl.isNotBlank() ->
                        AsyncImage(model = story.authorPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(story.authorPicUrl).fillMaxSize())
                    else -> Text(story.authorUsername.take(1).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 11.sp)
                }
            }
        }
        Text(story.authorUsername, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp, start = 4.dp, end = 4.dp))
        ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.18f)
        } // clickable Box
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

@Composable
fun ShimmerStoryCircle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        ShimmerBox(modifier = Modifier.width(100.dp).height(155.dp), cornerRadius = 14.dp)
    }
}
// ─── Create Post Box ──────────────────────────────────────────────────────────

@Composable
fun CreatePostBox(currentUser: UserProfileData?, onOpenComposer: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(CGreen))
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        val ringA by rememberGlowRange(0.5f, 1f)
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(
                                CGreen.copy(alpha = ringA), CGreen.copy(alpha = ringA * 0.6f), CGreen.copy(alpha = ringA)
                            ))))
                        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center) {
                            when {
                                !currentUser?.profilePictureUrl.isNullOrBlank() ->
                                    AsyncImage(model = currentUser!!.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(currentUser!!.profilePictureUrl).fillMaxSize())
                                currentUser?.habboUsername?.isNotBlank() == true ->
                                    AsyncImage(model = habboAvatarUrl(currentUser.habboUsername, currentUser.habboRegion), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.halftoneReveal(habboAvatarUrl(currentUser.habboUsername, currentUser.habboRegion)).fillMaxSize())
                                else -> Text(currentUser?.username?.take(1)?.uppercase() ?: "?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.5f))
                        .border(1.5.dp, ScrapbookDark.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                        .clickable { onOpenComposer() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("What are you playing?", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("🎮", fontSize = 16.sp)
                        }
                    }
                }
                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(Triple("📷", "Photo", CAcBlue), Triple("🎮", "Game", CAcPurple),
                        Triple("⭐", "Rate", CAcYellow), Triple("🎵", "OST", CAcRed)
                    ).forEach { actionTriple ->
                        val emoji = actionTriple.first; val label = actionTriple.second; val color = actionTriple.third
                        Box {
                            Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
                                .clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
                            Box(modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(
                                    color.copy(alpha = 0.13f), color.copy(alpha = 0.07f)
                                )))
                                .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { onOpenComposer() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(emoji, fontSize = 13.sp)
                                    Text(label, fontFamily = BangersFontFamily, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 12.dp, alpha = 0.22f)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── PostCard ─────────────────────────────────────────────────────────────────

@Composable
fun PostCard(
    post: RetroPost,
    currentUserId: String,
    postViewModel: PostViewModel,
    onCommentTap: () -> Unit = {},
    onAuthorTap: () -> Unit = {}
) {
    val context     = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(if (visible) 0f else 24f, tween(350, easing = LinearOutSlowInEasing), label = "postEnter_${post.id}")
    val enterAlpha  by animateFloatAsState(if (visible) 1f else 0f,  tween(300), label = "postAlpha_${post.id}")
    LaunchedEffect(Unit) { visible = true }

    // Music preview player
    var musicPlayer     by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    DisposableEffect(post.id) { onDispose { musicPlayer?.release(); musicPlayer = null } }

    val typeColor = postTypeColor(post.type)
    val typeEmoji = postTypeEmoji(post.type)
    val typeLabel = postTypeLabel(post.type)
    val appliedFilter = remember(post.filter) { RetroFilter.values().find { it.name == post.filter } ?: RetroFilter.NONE }
    val myReaction = post.userReactions[currentUserId] ?: ""

    val neonAlpha by rememberGlowRange(0.3f, 0.8f)

    val isRepost = post.type == "repost"
    val moodRotation = remember(post.id) { listOf(-8f, -5f, 5f, 8f, -3f, 3f).random() }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        .offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }) {
        // Comic shadow
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        // Glass card
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            // Green stripe + Type badge overlay
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            // Corner type tag — top-left sticker like the reference
            Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp).zIndex(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(typeColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(typeEmoji, fontSize = 10.sp)
                        Text(typeLabel, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    }
                }
            }
            Column {
                // Header — top padding clears the corner tag sticker
                Row(modifier = Modifier.fillMaxWidth().clickable { onAuthorTap() }
                    .padding(start = 14.dp, end = 14.dp, top = 44.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(typeColor, typeColor.copy(alpha = 0.3f), typeColor))))
                        Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f)).border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center) {
                            if (post.authorPicUrl.isNotBlank())
                                AsyncImage(model = post.authorPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(post.authorPicUrl).fillMaxSize())
                            else Text(post.authorUsername.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(post.authorUsername, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 0.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(post.authorHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                            Text("·", color = ScrapbookTextMuted, fontSize = 11.sp)
                            Text(timeAgoFromMillis(post.timestamp), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // Repost nested card
                if (isRepost && post.content.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f))
                        .border(1.5.dp, typeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("🔁", fontSize = 11.sp)
                                Text("REPOSTED", fontFamily = BangersFontFamily, color = typeColor, fontSize = 10.sp, letterSpacing = 1.sp)
                            }
                            Text(post.content, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.80f),
                                fontSize = 13.sp, lineHeight = 19.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Media (image or animated GIF)
                val hasMedia = post.imageUrl.isNotBlank() || post.gifUrl.isNotBlank()
                if (hasMedia) {
                    Box(modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp))) {
                        if (post.gifUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(post.gifUrl).decoderFactory(GifDecoder.Factory()).build(),
                                contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("GIF", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        } else {
                            AsyncImage(model = post.imageUrl, contentDescription = null,
                                contentScale = ContentScale.Crop, colorFilter = appliedFilter.toColorFilter(), modifier = Modifier.halftoneReveal(post.imageUrl).fillMaxSize())
                            if (post.filter == "SCANLINE" || post.filter == "VHS") {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    repeat(40) { Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.2f))); Spacer(modifier = Modifier.height(3.dp)) }
                                }
                            }
                            if (post.filter == "VHS") Text("REC ●", fontFamily = BangersFontFamily, color = Color.Red.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
                            if (post.filter.isNotBlank() && post.filter != "NONE") {
                                val fe = RetroFilter.values().find { it.name == post.filter }
                                fe?.let {
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("${it.emoji} ${it.label}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)))))
                    }
                }

                // Game panel
                if (post.gameName.isNotBlank() && post.type in listOf("game", "rating", "ost")) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).border(1.5.dp, typeColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))) {
                                    if (post.gameCoverUrl.isNotBlank())
                                        AsyncImage(model = post.gameCoverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(post.gameCoverUrl).fillMaxSize())
                                    else Box(modifier = Modifier.fillMaxSize().background(typeColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Text("🎮", fontSize = 22.sp) }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    AeroGlassPill(accentColor = typeColor) {
                                        Text(post.type.uppercase(), fontFamily = BangersFontFamily, color = typeColor, fontSize = 9.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(post.gameName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (post.type == "rating" && post.rating > 0) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            repeat(post.rating) { Text("⭐", fontSize = 14.sp) }
                                            repeat(5 - post.rating) { Text("☆", fontSize = 14.sp, color = ScrapbookTextMuted) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Title
                if (post.title.isNotBlank()) {
                    Text(post.title, fontFamily = BangersFontFamily, color = ScrapbookDark,
                        fontSize = 19.sp, letterSpacing = 0.4.sp, lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 14.dp).padding(top = if (hasMedia) 10.dp else 0.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Content
                if (post.content.isNotBlank() && !isRepost) {
                    Text(post.content, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.82f),
                        fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Standalone star rating (no game panel)
                if (post.rating > 0 && post.gameName.isBlank()) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(post.rating) { Text("⭐", fontSize = 15.sp) }
                        repeat(5 - post.rating) { Text("☆", fontSize = 15.sp, color = ScrapbookTextMuted) }
                        Text("${post.rating}/5", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Music pill with 30s preview
                val hasSong = post.songTitle.isNotBlank() || post.musicTrack.isNotBlank()
                if (hasSong) {
                    val songLabel = if (post.songTitle.isNotBlank()) post.songTitle else post.musicTrack.take(28)
                    val artistLabel = post.songArtist.ifBlank { "" }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CGreen.copy(alpha = 0.10f))
                        .border(1.dp, CGreen.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (post.songArtworkUrl.isNotBlank()) {
                            AsyncImage(model = post.songArtworkUrl, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.halftoneReveal(post.songArtworkUrl).size(32.dp).clip(RoundedCornerShape(4.dp)))
                        } else {
                            Text("🎵", fontSize = 18.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(songLabel, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (artistLabel.isNotBlank())
                                Text(artistLabel, fontFamily = NunitoFontFamily, color = CGreen, fontSize = 11.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (post.songPreviewUrl.isNotBlank()) {
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(CGreen.copy(alpha = 0.15f))
                                .border(1.dp, CGreen.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    if (isPlayingPreview) {
                                        musicPlayer?.pause(); isPlayingPreview = false
                                    } else {
                                        musicPlayer?.release()
                                        musicPlayer = MediaPlayer().apply {
                                            setDataSource(post.songPreviewUrl)
                                            setOnPreparedListener { start(); isPlayingPreview = true }
                                            setOnCompletionListener { isPlayingPreview = false }
                                            prepareAsync()
                                        }
                                    }
                                }, contentAlignment = Alignment.Center) {
                                Icon(if (isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    null, tint = CGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Mood sticker + Hashtag stickers
                val hasStickers = post.moodTag.isNotBlank() || post.hashtags.isNotEmpty()
                if (hasStickers) {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) {
                        // Hashtag stickers (left side)
                        if (post.hashtags.isNotEmpty()) {
                            Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                post.hashtags.take(3).forEachIndexed { index, tag ->
                                    val tagRotation = listOf(-4f, 3f, -2f)[index % 3]
                                    Box(
                                        modifier = Modifier
                                            .rotate(tagRotation)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            "#${tag.trimStart('#')}",
                                            fontFamily = BangersFontFamily,
                                            color = ScrapbookDark,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                        // Mood sticker (right side)
                        if (post.moodTag.isNotBlank()) {
                            Box(modifier = Modifier.align(Alignment.CenterEnd).rotate(moodRotation)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.75f))
                                .border(1.5.dp, ScrapbookDark.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(post.moodTag, fontFamily = BangersFontFamily, color = typeColor, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 14.dp))

                // Reactions
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val topReacters = remember(post.userReactions) { post.userReactions.entries.take(3).map { it.key } }
                    if (topReacters.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.width((topReacters.size * 18 + 4).dp).height(22.dp)) {
                                topReacters.forEachIndexed { index, uid ->
                                    Box(modifier = Modifier.size(22.dp).offset(x = (index * 14).dp).clip(CircleShape)
                                        .background(listOf(typeColor, CGreen, CAcRed)[index % 3].copy(alpha = 0.8f))
                                        .border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(uid.take(1).uppercase(), fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                            val totalReactions = post.reactions.values.sum()
                            if (totalReactions > 0) Text("$totalReactions reaction${if (totalReactions > 1) "s" else ""}",
                                fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf("🔥", "❤️", "🕹️", "👾").forEach { emoji ->
                                val count = post.reactions[emoji] ?: 0
                                val isReacted = myReaction == emoji
                                var rPressed by remember { mutableStateOf(false) }
                                val rScale by animateFloatAsState(if (rPressed) 1.35f else 1f,
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "rScale_${post.id}_$emoji")
                                val rBurst = rememberBurstState()
                                Box {
                                Box(modifier = Modifier.scale(rScale).clip(RoundedCornerShape(18.dp))
                                    .then(if (isReacted) Modifier.background(Color.White.copy(alpha = 0.92f))
                                        .border(1.5.dp, ScrapbookDark.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                    else Modifier)
                                    .clickable {
                                        rPressed = true
                                        if (!isReacted) rBurst.fire(
                                            when (emoji) { "🔥" -> "HOT!"; "❤️" -> "LOVE!"; "🕹️" -> "PLAY!"; else -> "ZAP!" },
                                            when (emoji) { "🔥" -> CAcYellowL; "❤️" -> CGreenMint; "🕹️" -> CGreen; else -> Color.White }
                                        )
                                        postViewModel.reactToPost(post.id, emoji)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(emoji, fontSize = 15.sp)
                                        if (count > 0) RollingCounterText("$count", TextStyle(fontFamily = BangersFontFamily, color = if (isReacted) typeColor else ScrapbookTextMuted, fontSize = 12.sp))
                                    }
                                }
                                ComicBurst(rBurst, Modifier.align(Alignment.Center), burstSize = 84.dp)
                                }
                                LaunchedEffect(rPressed) { if (rPressed) { delay(200); rPressed = false } }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.clickable { onCommentTap() },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.ChatBubbleOutline, null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp))
                                if (post.commentCount > 0) Text("${post.commentCount}", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            }
                            Icon(Icons.Filled.Share, null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── PostFeed ─────────────────────────────────────────────────────────────────

@Composable
fun PostFeed(
    posts: List<RetroPost>,
    isLoading: Boolean,
    currentUserId: String,
    currentUserProfile: UserProfileData? = null,
    postViewModel: PostViewModel,
    onNavigateToProfile: (String) -> Unit = {},
    onEmpty: @Composable () -> Unit = {}
) {
    when {
        isLoading -> Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(20.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(180.dp), cornerRadius = 20.dp)
                }
            }
        }
        posts.isEmpty() -> onEmpty()
        else -> {
            val pinnedPost = remember(posts) {
                posts.maxByOrNull { it.reactions.values.sum() + it.commentCount }?.takeIf { it.reactions.values.sum() > 0 }
            }
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                pinnedPost?.let { pinned ->
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                        Box(modifier = Modifier.align(Alignment.TopStart).offset(y = (-8).dp).zIndex(10f).padding(start = 12.dp)
                            .clip(RoundedCornerShape(6.dp)).background(CGreen).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📌", fontSize = 10.sp)
                                Text("MOST REACTED THIS WEEK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp, letterSpacing = 0.5.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    PostCardWithSocial(post = pinnedPost, currentUserId = currentUserId,
                        currentUserProfile = currentUserProfile, postViewModel = postViewModel, onNavigateToProfile = onNavigateToProfile)
                }
                posts.filter { it.id != pinnedPost?.id }.forEach { post ->
                    PostCardWithSocial(post = post, currentUserId = currentUserId,
                        currentUserProfile = currentUserProfile, postViewModel = postViewModel, onNavigateToProfile = onNavigateToProfile)
                }
            }
        }
    }
}

// ─── Empty Feed ───────────────────────────────────────────────────────────────

@Composable
fun EmptyFeedState(onDiscover: () -> Unit) {
    val neonAlpha by rememberGlowRange(0.3f, 0.9f)
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("👾", fontSize = 56.sp)
                Text("YOUR FEED IS EMPTY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp)
                Text("Follow other retro gamers to see their posts, stories and game ratings here!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep))).border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).clickable { onDiscover() }.padding(horizontal = 24.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👥", fontSize = 16.sp)
                        Text("FIND PEOPLE TO FOLLOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent), startY = 0f, endY = 60f)))
    }
}

// ─── SocialHubPanel ───────────────────────────────────────────────────────────

@Composable
fun SocialHubPanel(
    myStory: RetroStory?,
    stories: List<RetroStory>,
    currentUser: UserProfileData?,
    isLoadingStories: Boolean,
    onAddStory: () -> Unit,
    onViewStory: (RetroStory) -> Unit,
    onOpenComposer: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            StoriesBar(myStory = myStory, stories = stories, currentUser = currentUser,
                isLoading = isLoadingStories, onAddStory = onAddStory, onViewStory = onViewStory)
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(10.dp))
            CreatePostBox(currentUser = currentUser, onOpenComposer = onOpenComposer)
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
    onNavigateToCheckpoints: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel(),
    postViewModel: PostViewModel = viewModel()
) {
    val allUsers         by authViewModel.allUsers.collectAsState()
    val currentUser      by authViewModel.currentUser.collectAsState()
    val userProfile      by authViewModel.userProfile.collectAsState()
    val followingUids    by authViewModel.followingUids.collectAsState()
    val posts            by postViewModel.posts.collectAsState()
    val stories          by postViewModel.stories.collectAsState()
    val myStory          by postViewModel.myStory.collectAsState()
    val isLoadingPosts   by postViewModel.isLoadingPosts.collectAsState()
    val isLoadingStories by postViewModel.isLoadingStories.collectAsState()

    var currentQuoteIndex by remember { mutableStateOf(retroQuotes.indices.random()) }
    var todayFactIndex    by remember { mutableStateOf(todayInRetroGaming.indices.random()) }
    var unsplashPhoto     by remember { mutableStateOf<UnsplashPhoto?>(null) }
    var isLoadingPhoto    by remember { mutableStateOf(true) }
    var homeStats         by remember { mutableStateOf(HomeStats()) }
    var isLoadingStats    by remember { mutableStateOf(true) }
    var showClickOverlay  by remember { mutableStateOf(false) }
    var pendingNavAction  by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPostComposer  by remember { mutableStateOf(false) }
    var showStoryComposer by remember { mutableStateOf(false) }
    var viewingStory      by remember { mutableStateOf<RetroStory?>(null) }

    fun navigateWithTransition(action: () -> Unit) { pendingNavAction = action; showClickOverlay = true }

    LaunchedEffect(showClickOverlay) {
        if (showClickOverlay) { delay(180); showClickOverlay = false; delay(60); pendingNavAction?.invoke(); pendingNavAction = null }
    }
    LaunchedEffect(Unit) {
        authViewModel.fetchAllUsers()
        isLoadingPhoto = true; unsplashPhoto = fetchUnsplashRetroPhoto(); isLoadingPhoto = false
        homeStats = fetchHomeStats(); isLoadingStats = false
    }
    LaunchedEffect(userProfile) { userProfile?.let { postViewModel.setCurrentUserProfile(it) } }
    LaunchedEffect(followingUids) { postViewModel.fetchFeedPosts(followingUids); postViewModel.fetchStories(followingUids) }
    LaunchedEffect(Unit) { while (true) { delay(15000L); currentQuoteIndex = (currentQuoteIndex + 1) % retroQuotes.size } }

    val recentActivity = remember(allUsers) { allUsers.take(8) }
    val suggestedUsers = remember(allUsers, followingUids) { allUsers.filter { it.uid !in followingUids }.take(6) }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        // Dot texture sits BEHIND the feed (was drawn on top of every card before)
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        val homeListState = rememberLazyListState()
        LazyColumn(state = homeListState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {

            item {
                SocialHubPanel(myStory = myStory, stories = stories, currentUser = userProfile,
                    isLoadingStories = isLoadingStories,
                    onAddStory = { showStoryComposer = true }, onViewStory = { viewingStory = it },
                    onOpenComposer = { showPostComposer = true })
            }
            item { RetroTickerTape() }
            // 🗓️ Weekly update: this week's games, DLC, top stories & community articles
            item(key = "weekly_update") {
                WeeklyUpdateCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                SpringEntrance {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(CGreen))
                            Text("📡 COMMUNITY FEED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 1.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.92f))
                            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                            .clickable { showPostComposer = true }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Add, null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                                Text("POST", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
            item {
                PostFeed(posts = posts, isLoading = isLoadingPosts, currentUserId = currentUser?.uid ?: "",
                    currentUserProfile = userProfile, postViewModel = postViewModel,
                    onNavigateToProfile = { navigateWithTransition(onNavigateToProfile) },
                    onEmpty = { EmptyFeedState(onDiscover = { navigateWithTransition(onNavigateToDiscover) }) })
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (suggestedUsers.isNotEmpty()) {
                item {
                    WhoToFollowSection(users = suggestedUsers, followingUids = followingUids,
                        authViewModel = authViewModel, onDiscover = { navigateWithTransition(onNavigateToDiscover) })
                    Spacer(modifier = Modifier.height(24.dp)); WavePixelDivider(); Spacer(modifier = Modifier.height(24.dp))
                }
            }
            item(key = "hero") {
                // Parallax: hero scrolls slower than the feed and fades as it leaves
                Column(modifier = Modifier.graphicsLayer {
                    val off = homeListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "hero" }?.offset ?: 0
                    if (off < 0) {
                        translationY = -off * 0.4f
                        alpha = (1f + off / 1100f).coerceIn(0.35f, 1f)
                    }
                }) {
                StaggeredSection(0) { MagazineCoverHero(photo = unsplashPhoto, isLoading = isLoadingPhoto, onNavigateToDiscover = { navigateWithTransition(onNavigateToDiscover) }) }; Spacer(modifier = Modifier.height(24.dp))
                }
            }
            item { StaggeredSection(1) { HomeGameOfDaySection(onNavigateToGames = { navigateWithTransition(onNavigateToGames) }) }; Spacer(modifier = Modifier.height(24.dp)); FloatingEmojiDivider(); Spacer(modifier = Modifier.height(24.dp)) }
            item { StaggeredSection(2) { UpcomingReleasesSection(onNavigateToGames = { navigateWithTransition(onNavigateToGames) }) }; Spacer(modifier = Modifier.height(24.dp)); WavePixelDivider(); Spacer(modifier = Modifier.height(24.dp)) }
            item {
                StaggeredSection(3) { TodayInRetroSection(fact = todayInRetroGaming[todayFactIndex], onNext = { todayFactIndex = (todayFactIndex + 1) % todayInRetroGaming.size }) }
                Spacer(modifier = Modifier.height(20.dp))
                StaggeredSection(4) { RetroQuoteCard(quote = retroQuotes[currentQuoteIndex]) }
                Spacer(modifier = Modifier.height(24.dp)); WavePixelDivider(); Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                StaggeredSection(5) {
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
                Spacer(modifier = Modifier.height(24.dp)); FloatingEmojiDivider(); Spacer(modifier = Modifier.height(24.dp))
            }
            if (recentActivity.isNotEmpty()) {
                item { StaggeredSection(6) { CommunityActivitySection(users = recentActivity, onUserTap = { navigateWithTransition(onNavigateToDiscover) }) }; Spacer(modifier = Modifier.height(24.dp)); WavePixelDivider(); Spacer(modifier = Modifier.height(24.dp)) }
            }
            item { StaggeredSection(7) { Column { RetroSectionHeader("FEATURED ALBUMS", "🎵"); Text("SEE ALL →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { navigateWithTransition(onNavigateToAlbums) }); Spacer(modifier = Modifier.height(12.dp)); FeaturedAlbumsCarousel(onNavigateToAlbums = { navigateWithTransition(onNavigateToAlbums) }) } }; Spacer(modifier = Modifier.height(24.dp)) }
            item { StaggeredSection(8) { Column { RetroSectionHeader("FEATURED MAGAZINES", "📰"); Text("SEE ALL →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { navigateWithTransition(onNavigateToMagazines) }); Spacer(modifier = Modifier.height(12.dp)); FeaturedMagazinesCarousel(onNavigateToMagazines = { navigateWithTransition(onNavigateToMagazines) }) } }; Spacer(modifier = Modifier.height(24.dp)); FloatingEmojiDivider(); Spacer(modifier = Modifier.height(24.dp)) }
            item {
                StaggeredSection(9) {
                    CheckpointHomeSection(onNavigateToCheckpoints = { navigateWithTransition(onNavigateToCheckpoints) })
                }
                Spacer(modifier = Modifier.height(24.dp)); FloatingEmojiDivider(); Spacer(modifier = Modifier.height(24.dp))
            }
            item { StaggeredSection(11) { HomeStatsSection(stats = homeStats, isLoading = isLoadingStats) }; Spacer(modifier = Modifier.height(24.dp)); WavePixelDivider(); Spacer(modifier = Modifier.height(24.dp)) }
            item { StaggeredSection(12) { HomeRetroBytesPreview(onOpenFeed = { navigateWithTransition(onNavigateToRetroBytes) }) }; Spacer(modifier = Modifier.height(24.dp)); WavePixelDivider(); Spacer(modifier = Modifier.height(24.dp)) }
            item { StaggeredSection(13) { CopyrightFooter(name = "Carlos Zabala", blogUrl = "https://charlysblog.framer.website") }; Spacer(modifier = Modifier.height(24.dp)) }
        }

        ClickTransitionOverlay(visible = showClickOverlay)
        viewingStory?.let { StoryViewerDialog(story = it, onDismiss = { viewingStory = null }, postViewModel = postViewModel) }
        if (showPostComposer) PostComposerSheet(currentUser = userProfile, postViewModel = postViewModel, onDismiss = { showPostComposer = false; postViewModel.refreshFeed(followingUids) })
        if (showStoryComposer) StoryComposerSheet(currentUser = userProfile, postViewModel = postViewModel, onDismiss = { showStoryComposer = false; postViewModel.fetchStories(followingUids) })
    }
}

// ─── MagazineCoverHero ────────────────────────────────────────────────────────

@Composable
fun MagazineCoverHero(photo: UnsplashPhoto?, isLoading: Boolean, onNavigateToDiscover: () -> Unit) {
    val today = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }
    val issueNumber = remember { val cal = Calendar.getInstance(); "VOL.${cal.get(Calendar.YEAR)} NO.${cal.get(Calendar.DAY_OF_YEAR)}" }
    val btnScale by rememberGlowRange(1f, 1.03f)

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.25f)))
        Box(modifier = Modifier.fillMaxWidth().height(480.dp).clip(RoundedCornerShape(16.dp)).border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp))) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.92f)).border(BorderStroke(2.dp, ScrapbookDark)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.12f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("RETROHUB DAILY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, letterSpacing = 3.sp, lineHeight = 30.sp)
                            Text(issueNumber, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            listOf("RETRO", "GAMING", "UNIVERSE").forEachIndexed { i, word ->
                                Text(word, fontFamily = BangersFontFamily, color = if (i == 2) CGreenDeep else ScrapbookDark, fontSize = 11.sp, letterSpacing = 3.sp)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep))).padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(today.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, letterSpacing = 1.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(ScrapbookDark).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("DAILY EDITION", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))) {
                    when {
                        isLoading -> {
                            ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp)
                            Box(modifier = Modifier.fillMaxSize().background(ScrapbookDark.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = CGreen, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Loading cover...", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 14.sp)
                                }
                            }
                        }
                        photo != null -> {
                            val kbT = rememberInfiniteTransition(label = "heroKB")
                            val heroScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 16000; 1f at 0; 1.08f at 8000; 1f at 16000 }, RepeatMode.Restart), label = "heroScale")
                            val heroPanX by kbT.animateFloat(initialValue = -10f, targetValue = 10f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 20000; -10f at 0; 10f at 10000; -10f at 20000 }, RepeatMode.Restart), label = "heroPanX")
                            AsyncImage(model = photo.imageUrl, contentDescription = photo.description, contentScale = ContentScale.Crop,
                                modifier = Modifier.halftoneReveal(photo.imageUrl).fillMaxSize().scale(heroScale).offset(x = heroPanX.dp))
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 120f)))
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("📷 ${photo.photographerName}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp)
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(CGreen).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("TODAY'S COVER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 2.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(photo.description.ifBlank { "Retro Gaming Daily" }.replaceFirstChar { it.uppercase() },
                                    fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(10.dp))
                                Box {
                                    Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                                        .clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                                    Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { onNavigateToDiscover() }) {
                                        Text("EXPLORE NOW →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp, letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                                        ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 10.dp)
                                    }
                                }
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CGreenMint, ComicGlassBg))), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎮", fontSize = 64.sp); Spacer(modifier = Modifier.height(12.dp))
                                    Text("Your retro gaming universe", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Box {
                                        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                                            .clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                                        Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { onNavigateToDiscover() }) {
                                            Text("EXPLORE NOW →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp,
                                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                                            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 10.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ScanlineOverlay(modifier = Modifier.matchParentSize(), lineAlpha = 0.06f)
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 16.dp)
        }
    }
}
// ─── WhoToFollowSection ───────────────────────────────────────────────────────

@Composable
fun WhoToFollowSection(users: List<UserProfileData>, followingUids: Set<String>, authViewModel: AuthViewModel, onDiscover: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance { Column { RetroSectionHeader("WHO TO FOLLOW", "👥"); Text("SEE ALL →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { onDiscover() }) } }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(users, key = { _, u -> u.uid }) { index, user ->
                SpringEntrance(delayMs = index * 60) {
                    WhoToFollowCard(user = user, isFollowing = followingUids.contains(user.uid),
                        onFollow = { authViewModel.followUser(user.uid) }, onUnfollow = { authViewModel.unfollowUser(user.uid) })
                }
            }
        }
    }
}

@Composable
fun WhoToFollowCard(user: UserProfileData, isFollowing: Boolean, onFollow: () -> Unit, onUnfollow: () -> Unit) {
    var localFollowing by remember(isFollowing) { mutableStateOf(isFollowing) }
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "wtfPress_${user.uid}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "wtfShadow_${user.uid}")

    Box(modifier = Modifier.width(155.dp)) {
        GlowPulse(modifier = Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 16.dp, maxAlpha = 0.4f)
        // Comic shadow
        Box(modifier = Modifier.fillMaxWidth().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp).clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Avatar ring
                Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(68.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(CGreen, CGreen, CGreen))))
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        when {
                            user.habboUsername.isNotBlank() -> AsyncImage(model = habboAvatarUrl(user.habboUsername, user.habboRegion), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.halftoneReveal(habboAvatarUrl(user.habboUsername, user.habboRegion)).fillMaxSize())
                            user.profilePictureUrl.isNotBlank() -> AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(user.profilePictureUrl).fillMaxSize())
                            else -> Text(user.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp)
                        }
                    }
                }
                Text(user.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(user.userHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                HoloBadge(label = "${formatCount(user.followersCount)} FOLLOWERS", cornerRadius = 10.dp, fontSize = 10.sp)
                // Follow button with shadow + shimmer
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
                        .clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.18f)))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (localFollowing) ScrapbookDark.copy(alpha = 0.08f) else CGreen)
                        .border(1.5.dp, if (localFollowing) ScrapbookBorder.copy(alpha = 0.4f) else ScrapbookDark, RoundedCornerShape(12.dp))
                        .clickable { pressed = true; if (localFollowing) { localFollowing = false; onUnfollow() } else { localFollowing = true; onFollow() } }
                        .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(if (localFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd, null,
                                tint = if (localFollowing) ScrapbookTextMuted else ScrapbookDark, modifier = Modifier.size(14.dp))
                            Text(if (localFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily,
                                color = if (localFollowing) ScrapbookTextMuted else ScrapbookDark, fontSize = 13.sp)
                        }
                        ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 12.dp, alpha = 0.25f)
                    }
                }
            }
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 16.dp, alpha = 0.15f)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── HomeStatCard + HomeStatsSection ─────────────────────────────────────────

@Composable
fun HomeStatCard(item: StatItemData, isLoading: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    val alpha  by animateFloatAsState(if (visible) 1f else 0f, tween(400), label = "statAlpha_${item.label}")
    val offset by animateFloatAsState(if (visible) 0f else 20f, tween(400, easing = LinearOutSlowInEasing), label = "statOff_${item.label}")
    LaunchedEffect(Unit) { visible = true }
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "statPress_${item.label}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "statShadow_${item.label}")

    Box(modifier = modifier.offset(y = offset.dp).graphicsLayer { this.alpha = alpha }.clickable { pressed = true }) {
        GlowPulse(modifier = Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 14.dp, maxAlpha = 0.35f)
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(item.accentColor.copy(alpha = 0.2f)).border(1.5.dp, item.glowColor.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(item.emoji, fontSize = 22.sp)
                }
                if (isLoading) ShimmerBox(modifier = Modifier.width(52.dp).height(30.dp), cornerRadius = 6.dp)
                else {
                    // Slot-machine roll from 0 to the real value
                    var shown by remember { mutableStateOf("0") }
                    LaunchedEffect(item.count) { delay(200); shown = formatCount(item.count) }
                    RollingCounterText(shown, TextStyle(fontFamily = BangersFontFamily, color = item.glowColor, fontSize = 30.sp, letterSpacing = 1.sp))
                }
                Text(item.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center, maxLines = 1)
                Text(item.tagline, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
            }
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.16f)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

@Composable
fun HomeStatsSection(stats: HomeStats, isLoading: Boolean) {
    var gamesCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { try { gamesCount = FirebaseFirestore.getInstance().collection("games").get().await().size() } catch (e: Exception) { gamesCount = 128 } }
    val statItems = listOf(
        StatItemData("👥", if (isLoading) 0 else stats.userCount,    "EXPLORERS", "retro fans",    CAcBlue,   CAcBlue.copy(alpha = 0.7f)),
        StatItemData("📝", if (isLoading) 0 else stats.articleCount, "ARTICLES",  "written",       CAcPurple, CAcPurple.copy(alpha = 0.7f)),
        StatItemData("🎵", if (isLoading) 0 else stats.albumCount,   "ALBUMS",    "soundtracks",   CGreenDeep, CGreen),
        StatItemData("🎮", if (isLoading) 0 else gamesCount,         "GAMES",     "tracked",       CAcRed,    CAcRed.copy(alpha = 0.7f))
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(CGreen))
                    Text("📊 BY THE NUMBERS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 1.sp)
                }
                AeroGlassPill(accentColor = ScrapbookDark) {
                    Text("🕹️", fontSize = 11.sp); Spacer(modifier = Modifier.width(4.dp))
                    Text("EST. 2026", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { HomeStatCard(statItems[0], isLoading, Modifier.weight(1f)); HomeStatCard(statItems[1], isLoading, Modifier.weight(1f)) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { HomeStatCard(statItems[2], isLoading, Modifier.weight(1f)); HomeStatCard(statItems[3], isLoading, Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint)))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { PulsingDot(color = CGreen, size = 9.dp); Text("RETROHUB IS LIVE", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp, letterSpacing = 1.sp) }
                    Text(if (isLoading) "loading..." else "${formatCount(stats.userCount)} members strong", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.20f)
            }
        }
    }
}

// ─── UpcomingReleasesSection ──────────────────────────────────────────────────

@Composable
fun UpcomingReleasesSection(onNavigateToGames: () -> Unit) {
    var upcomingGames by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { isLoading = true; try { upcomingGames = IGDBRepository.searchGames("upcoming 2025 2026").take(8) } catch (e: Exception) { }; isLoading = false }
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance { Column { RetroSectionHeader("UPCOMING GAMES", "📅"); Text("SEE ALL →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { onNavigateToGames() }) } }
        Spacer(modifier = Modifier.height(12.dp))
        if (isLoading) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(5) { ShimmerBox(modifier = Modifier.width(130.dp).height(190.dp), cornerRadius = 14.dp) } }
        } else if (upcomingGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                    // Green stripe
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("🎮", fontSize = 36.sp); Text("Check back soon!", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp) }
                }
            }
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(upcomingGames, key = { _, g -> g.id }) { index, game ->
                    SpringEntrance(delayMs = index * 60) { UpcomingGameCard(game = game, onTap = onNavigateToGames) }
                }
                item {
                    Box(modifier = Modifier.width(80.dp).height(190.dp)) {
                        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                            .background(CGreen)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).clickable { onNavigateToGames() }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("→", color = ScrapbookDark, fontSize = 26.sp, fontFamily = BangersFontFamily); Text("SEE\nALL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, textAlign = TextAlign.Center) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingGameCard(game: IGDBGame, onTap: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "upcomingPress_${game.id}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "upcomingShadow_${game.id}")
    val accentColor = gameAccentColor(game.name)
    val neonAlpha by rememberGlowRange(0.3f, 0.8f)
    Box(modifier = Modifier.width(130.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp).clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
            .clickable { pressed = true; onTap() }) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))) {
                    if (!game.coverUrl.isNullOrBlank()) AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
                    else Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.4f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 28.sp) }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    ScanlineOverlay(modifier = Modifier.fillMaxSize(), lineAlpha = 0.06f)
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                        HoloBadge(label = "UPCOMING", cornerRadius = 6.dp, fontSize = 7.sp)
                    }
                    game.releaseYear?.let { Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.75f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text("$it", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) } }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                    Text(detectGenreForGame(game.name), fontFamily = NunitoFontFamily, color = accentColor.copy(alpha = 0.85f), fontSize = 10.sp)
                }
            }
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.18f)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── TodayInRetroSection ──────────────────────────────────────────────────────

@Composable
fun TodayInRetroSection(fact: String, onNext: () -> Unit) {
    val today = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("📅", fontSize = 20.sp); Text("TODAY IN RETRO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp) }
                        IconButton(onClick = onNext, modifier = Modifier.size(32.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.12f))) { Icon(Icons.Filled.Refresh, null, tint = ScrapbookDark, modifier = Modifier.size(17.dp)) }
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    AeroGlassPill(accentColor = CGreen) { Text(today.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 1.sp) }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(fact, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookDark, fontSize = 17.sp, lineHeight = 26.sp)
                }
            }
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.15f)
        }
    }
}

// ─── RetroQuoteCard ───────────────────────────────────────────────────────────

@Composable
fun RetroQuoteCard(quote: String) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.94f)).border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))) {
            Row(modifier = Modifier.padding(16.dp)) {
                Text("\"", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.15f), fontSize = 90.sp, lineHeight = 65.sp, modifier = Modifier.offset(y = (-10).dp))
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    HoloBadge(label = "QUOTE OF THE MOMENT", cornerRadius = 6.dp, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(quote, fontFamily = NunitoFontFamily, fontWeight = FontWeight.ExtraBold, color = ScrapbookDark, fontSize = 16.sp, lineHeight = 24.sp, fontStyle = FontStyle.Italic)
                }
            }
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 14.dp, alpha = 0.18f)
        }
    }
}

// ─── HomeGameOfDaySection ─────────────────────────────────────────────────────

@Composable
fun HomeGameOfDaySection(onNavigateToGames: () -> Unit = {}) {
    var game by remember { mutableStateOf<RetroGameOfDay?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { isLoading = true; game = fetchRetroGameOfDay(); isLoading = false }
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance { RetroSectionHeader("GAME OF THE DAY", "🎮") }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            when { isLoading -> ShimmerBox(modifier = Modifier.fillMaxWidth().height(380.dp), cornerRadius = 20.dp); game != null -> RetroGameOfDayCard(game = game!!, onViewInDatabase = onNavigateToGames); else -> {} }
        }
    }
}

// ─── CommunityActivitySection ─────────────────────────────────────────────────

@Composable
fun CommunityActivitySection(users: List<UserProfileData>, onUserTap: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance { Column { RetroSectionHeader("COMMUNITY", "👥"); Text("FIND MORE →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { onUserTap() }) } }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                ) {
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("WHO'S ON RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                            Text("Fellow retro explorers", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        AeroGlassPill(accentColor = CGreen) {
                            PulsingDot(size = 7.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(users, key = { _, u -> u.uid }) { index, user ->
                            SpringEntrance(delayMs = index * 60) { PolaroidUserCard(user = user, onTap = onUserTap) }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                            .clickable { onUserTap() }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FIND MORE PEOPLE →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                    }
                }
                }
            }
        }
    }
}

@Composable
fun PolaroidUserCard(user: UserProfileData, onTap: () -> Unit) {
    val rotation = remember { (-4..4).random().toFloat() }
    Box(modifier = Modifier.width(76.dp).rotate(rotation)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
            .clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color.White)
            .border(1.5.dp, ScrapbookBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable { onTap() }.padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth().height(68.dp).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                if (!user.profilePictureUrl.isNullOrBlank()) AsyncImage(model = user.profilePictureUrl, contentDescription = user.username, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(user.profilePictureUrl).fillMaxSize())
                else Text(user.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 30.sp)
                ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 0.dp, alpha = 0.20f)
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
        itemsIndexed(sampleAlbums.take(5), key = { _, a -> a.id }) { index, album ->
            var pressed by remember { mutableStateOf(false) }
            val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "albumPress_${album.id}")
            val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "albumShadow_${album.id}")
            val kbT = rememberInfiniteTransition(label = "kb_${album.id}")
            val idx = sampleAlbums.indexOfFirst { it.id == album.id }.coerceAtLeast(0)
            val kbScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 10000 + idx * 2000; 1f at 0; 1.07f at (5000 + idx * 1000); 1f at (10000 + idx * 2000) }, RepeatMode.Restart), label = "kbScale_${album.id}")
            SpringEntrance(delayMs = index * 60) {
            Box(modifier = Modifier.width(160.dp)) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp))
                    .background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                    .clickable { pressed = true; onNavigateToAlbums() }
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(0.dp))) {
                            if (album.coverImageResId != null) Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
                            ScanlineOverlay(modifier = Modifier.fillMaxSize(), lineAlpha = 0.06f)
                            ComicShimmer(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, alpha = 0.16f, durationMs = 2800)
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                            Text(album.artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
            } // SpringEntrance
        }
        item {
            Box(modifier = Modifier.width(80.dp).height(220.dp)) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp)).clickable { onNavigateToAlbums() }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("→", color = ScrapbookDark, fontSize = 28.sp, fontFamily = BangersFontFamily); Spacer(modifier = Modifier.height(4.dp)); Text("SEE\nALL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

// ─── FeaturedMagazinesCarousel ────────────────────────────────────────────────

@Composable
fun FeaturedMagazinesCarousel(onNavigateToMagazines: () -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(sampleMagazineCovers.take(5), key = { _, m -> m.id }) { index, magazine ->
            var pressed by remember { mutableStateOf(false) }
            val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magPress_${magazine.id}")
            val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "magShadow_${magazine.id}")
            val kbT = rememberInfiniteTransition(label = "kb_mag_${magazine.id}")
            val idx = sampleMagazineCovers.indexOfFirst { it.id == magazine.id }.coerceAtLeast(0)
            val kbScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 11000 + idx * 1500; 1f at 0; 1.06f at (5500 + idx * 750); 1f at (11000 + idx * 1500) }, RepeatMode.Restart), label = "kbScaleMag_${magazine.id}")
            SpringEntrance(delayMs = index * 60) {
            Box(modifier = Modifier.width(115.dp)) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(12.dp))
                    .background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                    .clickable { pressed = true; onNavigateToMagazines() }
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                            .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                        Box(modifier = Modifier.fillMaxWidth().height(145.dp).clip(RoundedCornerShape(0.dp))) {
                            if (magazine.coverImageResId != null) Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))))
                            ScanlineOverlay(modifier = Modifier.fillMaxSize(), lineAlpha = 0.06f)
                            ComicShimmer(modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, alpha = 0.16f, durationMs = 2600)
                        }
                        Text(magazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
            } // SpringEntrance
        }
        item {
            Box(modifier = Modifier.width(80.dp).height(183.dp)) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable { onNavigateToMagazines() }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("→", color = ScrapbookDark, fontSize = 26.sp, fontFamily = BangersFontFamily); Spacer(modifier = Modifier.height(4.dp)); Text("SEE\nALL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, textAlign = TextAlign.Center) }
                }
            }
        }
    }
}

// ─── FeaturedStreamsSection ───────────────────────────────────────────────────

@Composable
fun FeaturedStreamsSection(onNavigateToStreams: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance { Column { RetroSectionHeader("STREAMS & VIDEOS", "📺"); Text("WATCH →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { onNavigateToStreams() }) } }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).clickable { onNavigateToStreams() }) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))).padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { PulsingDot(color = CAcRed, size = 9.dp); Text("LIVE NOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp) }
                            Text("▶ WATCH", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Watch live retro gaming streams and classic gaming videos from the community.", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 15.sp, lineHeight = 23.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable { onNavigateToStreams() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) { Text("🔴 TWITCH", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp) }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(CGreenDeep).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable { onNavigateToStreams() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) { Text("▶ YOUTUBE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ─── ExploreRetroHubSection ───────────────────────────────────────────────────

@Composable
fun ExploreRetroHubSection(
    onNavigateToAlbums: () -> Unit, onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit, onNavigateToProfile: () -> Unit,
    onNavigateToGames: () -> Unit, onNavigateToStreams: () -> Unit,
    onNavigateToEvents: () -> Unit, onNavigateToMarketplace: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val defaultCards = listOf(
        NavCardData("ALBUMS",    "🎵", "Retro soundtracks", "500+ OSTs",            ALBUMS_CARD_IMAGE,    CGreen, false, onNavigateToAlbums),
        NavCardData("MAGAZINES", "📰", "Vintage issues",    "Classic gaming press", MAGAZINES_CARD_IMAGE, CAcYellow, false, onNavigateToMagazines),
        NavCardData("ARTICLES",  "📝", "Community writes",  "Retro stories",        ARTICLES_CARD_IMAGE,  CAcBlue,   false, onNavigateToArticles),
        NavCardData("PROFILE",   "👤", "Your corner",       "Your retro identity",  PROFILE_CARD_IMAGE,   CAcPurple, false, onNavigateToProfile)
    )
    val extraCards = listOf(
        NavCardData("GAMES",       "🎮", "Browse classics", "IGDB powered",        GAMES_CARD_IMAGE,     CGreen, false, onNavigateToGames),
        NavCardData("STREAMS",     "📺", "Watch live",      "Twitch & YouTube",    ALBUMS_CARD_IMAGE,    CAcPurple, false, onNavigateToStreams),
        NavCardData("EVENTS",      "🎪", "Coming soon",     "Retro gaming events", MAGAZINES_CARD_IMAGE, CAcRed, true,  onNavigateToEvents),
        NavCardData("MARKETPLACE", "🛒", "Coming soon",     "Trade retro games",   ARTICLES_CARD_IMAGE,  CGreenDeep, true,  onNavigateToMarketplace)
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.width(5.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(CGreen))
                    Text("🕹️ EXPLORE RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                }
                AeroGlassPill(accentColor = ScrapbookDark) {
                    Text("${defaultCards.size + extraCards.size} SECTIONS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { VisualNavCard(card = defaultCards[0], modifier = Modifier.weight(1f)); VisualNavCard(card = defaultCards[1], modifier = Modifier.weight(1f)) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { VisualNavCard(card = defaultCards[2], modifier = Modifier.weight(1f)); VisualNavCard(card = defaultCards[3], modifier = Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(300, easing = LinearOutSlowInEasing)) + fadeIn(tween(300)), exit = shrinkVertically(tween(250)) + fadeOut(tween(200))) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    extraCards.take(2).forEachIndexed { i, card ->
                        var cv by remember { mutableStateOf(false) }
                        val ca by animateFloatAsState(if (cv) 1f else 0f, tween(300, i * 80), label = "ec_$i")
                        val co by animateFloatAsState(if (cv) 0f else 20f, tween(300, i * 80, LinearOutSlowInEasing), label = "eo_$i")
                        LaunchedEffect(isExpanded) { if (isExpanded) { delay(i * 80L); cv = true } else cv = false }
                        Box(modifier = Modifier.weight(1f).offset(y = co.dp).graphicsLayer { alpha = ca }) { VisualNavCard(card = card, modifier = Modifier.fillMaxWidth()) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    extraCards.drop(2).forEachIndexed { i, card ->
                        var cv by remember { mutableStateOf(false) }
                        val ca by animateFloatAsState(if (cv) 1f else 0f, tween(300, (i + 2) * 80), label = "ec2_$i")
                        val co by animateFloatAsState(if (cv) 0f else 20f, tween(300, (i + 2) * 80, LinearOutSlowInEasing), label = "eo2_$i")
                        LaunchedEffect(isExpanded) { if (isExpanded) { delay((i + 2) * 80L); cv = true } else cv = false }
                        Box(modifier = Modifier.weight(1f).offset(y = co.dp).graphicsLayer { alpha = ca }) { VisualNavCard(card = card, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        var btnPressed by remember { mutableStateOf(false) }
        val btnScale by animateFloatAsState(if (btnPressed) 0.95f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "expandBtnScale")
        val arrowRot by animateFloatAsState(if (isExpanded) 180f else 0f, tween(300, easing = EaseInOut), label = "arrowRot")
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(modifier = Modifier.fillMaxWidth().scale(btnScale).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).clickable { btnPressed = true; isExpanded = !isExpanded }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isExpanded) "SHOW LESS" else "SHOW MORE SECTIONS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = ScrapbookDark, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = arrowRot })
                }
            }
        }
        LaunchedEffect(btnPressed) { if (btnPressed) { delay(150); btnPressed = false } }
    }
}

@Composable
fun VisualNavCard(card: NavCardData, modifier: Modifier = Modifier) {
    var pressed by remember { mutableStateOf(false) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "navCardPress_${card.title}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "navCardShadow_${card.title}")
    val atX by animateFloatAsState(if (pressed) tiltX else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tiltX_${card.title}")
    val atY by animateFloatAsState(if (pressed) tiltY else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tiltY_${card.title}")
    Box(modifier = modifier.graphicsLayer { rotationX = atX; rotationY = atY; cameraDistance = 12f * density }) {
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().offset(x = pressAnim.dp, y = pressAnim.dp).height(185.dp).clip(RoundedCornerShape(16.dp)).border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onPress = { offset ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    tiltX = ((offset.y - cy) / cy * -6f).coerceIn(-6f, 6f); tiltY = ((offset.x - cx) / cx * 6f).coerceIn(-6f, 6f)
                    pressed = true; tryAwaitRelease(); pressed = false
                    if (!card.isComingSoon) card.onClick()
                })
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) { Image(painter = painterResource(id = card.imageResId), contentDescription = card.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.35f) }
            Box(modifier = Modifier.fillMaxSize().background(card.accentColor.copy(alpha = 0.6f)))
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))))
            ScanlineOverlay(modifier = Modifier.fillMaxSize(), lineAlpha = 0.06f)
            ComicShimmer(modifier = Modifier.fillMaxSize(), cornerRadius = 16.dp, alpha = 0.18f, durationMs = 3000)
            Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) { Text(card.emoji, fontSize = 20.sp) }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(20.dp))
                .background(if (card.isComingSoon) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f))
                .border(1.dp, if (card.isComingSoon) CGreen.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (card.isComingSoon) "SOON" else "EXPLORE →", fontFamily = BangersFontFamily, color = if (card.isComingSoon) CGreen else Color.White, fontSize = 9.sp, letterSpacing = 0.5.sp)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp)) {
                Text(card.tagline, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(card.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, letterSpacing = 1.sp, lineHeight = 26.sp)
            }
            if (card.isComingSoon) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }
    }
}

// ─── CheckpointHomeSection ────────────────────────────────────────────────────

@Composable
fun CheckpointHomeSection(onNavigateToCheckpoints: () -> Unit) {
    val viewModel: CheckpointViewModel = viewModel()
    val checkpoints by viewModel.checkpoints.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        SpringEntrance { Column { RetroSectionHeader("CHECKPOINTS", "🏁"); Text("SEE ALL →", color = CGreen, fontFamily = BangersFontFamily, modifier = Modifier.padding(start = 16.dp).clickable { onNavigateToCheckpoints() }) } }
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(2) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(80.dp), cornerRadius = 16.dp) }
            }
        } else if (checkpoints.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                        .clickable { onNavigateToCheckpoints() }
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🏁", fontSize = 28.sp)
                        Column {
                            Text("SHARE A CHECKPOINT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            Text("Post your first milestone or memory", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                checkpoints.take(3).forEachIndexed { index, cp ->
                    SpringEntrance(delayMs = index * 60) {
                        CheckpointPreviewCard(checkpoint = cp, onClick = onNavigateToCheckpoints)
                    }
                }
                if (checkpoints.size > 3) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                .clickable { onNavigateToCheckpoints() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏁", fontSize = 14.sp)
                                Text(
                                    "SEE ALL ${checkpoints.size} CHECKPOINTS",
                                    fontFamily = BangersFontFamily, color = ScrapbookDark,
                                    fontSize = 14.sp, letterSpacing = 0.5.sp
                                )
                                Icon(Icons.Filled.ChevronRight, null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── CopyrightFooter ─────────────────────────────────────────────────────────

@Composable
fun CopyrightFooter(name: String, blogUrl: String, modifier: Modifier = Modifier) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val pulseT = rememberInfiniteTransition(label = "footerPulse")
    val glowAlpha by pulseT.animateFloat(initialValue = 0.3f, targetValue = 0.7f, animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOut), RepeatMode.Reverse), label = "footerGlow")

    val retroTips = remember {
        listOf(
            "🎮 Save your progress. Real life has no checkpoints.",
            "👾 Every expert was once a player on level 1.",
            "🕹️ Insert coin to continue the adventure.",
            "⭐ Collect enough stars and anything is possible.",
            "🏁 Checkpoint reached. You made it this far — keep going!"
        )
    }
    val tipIndex = remember { (0..4).random() }

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Comic shadow
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Brand
                Text("🕹️ RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, letterSpacing = 3.sp)
                Text("Your retro gaming community", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)

                // Divider
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, CGreen.copy(alpha = 0.7f), Color.Transparent))))

                // Retro tip of the session
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.05f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(retroTips[tipIndex], fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                }

                // Social links row
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (label in listOf("💬 Discord", "🐦 Twitter", "▶️ YouTube")) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(CGreen.copy(alpha = 0.18f))
                                .border(1.dp, CGreen.copy(alpha = 0.50f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 11.sp)
                        }
                    }
                }

                // Thin divider
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, ScrapbookBorder.copy(alpha = 0.4f), Color.Transparent))))

                // Copyright
                Text(
                    "© $currentYear $name · All Rights Reserved",
                    fontFamily = NunitoFontFamily, fontSize = 11.sp,
                    color = ScrapbookTextMuted, textAlign = TextAlign.Center
                )
                Text("Made with ❤️ for retro lovers", fontFamily = NunitoFontFamily, fontSize = 10.sp, color = ScrapbookDark.copy(alpha = 0.35f))
            }
        }
    }
}

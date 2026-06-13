package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.draw.alpha

// ─── Brand colors ─────────────────────────────────────────────────────────────
val TwitchPurple = Color(0xFF9146FF)
val YouTubeRed = Color(0xFFFF0000)
val TikTokPink = Color(0xFFFF0050)
val TikTokCyan = Color(0xFF00F2EA)

// ─── TikTok creator data ──────────────────────────────────────────────────────
data class TikTokCreator(
    val id: String,
    val username: String,
    val displayName: String,
    val niche: String,
    val followers: String,
    val emoji: String,
    val accentColor: Color,
    val profileUrl: String,
    val tags: List<String>
)

val retrogamingTikTokCreators = listOf(
    TikTokCreator("t1", "@thenintendoking", "The Nintendo King", "Nintendo history & reviews", "2.1M", "👑", Color(0xFFE4000F), "https://www.tiktok.com/@thenintendoking", listOf("NES", "SNES", "N64")),
    TikTokCreator("t2", "@retrogaminghistory", "Retro Gaming History", "Deep dives into gaming history", "890K", "🕹️", Color(0xFF9146FF), "https://www.tiktok.com/@retrogaminghistory", listOf("SEGA", "PS1", "ARCADE")),
    TikTokCreator("t3", "@segafan_official", "SEGA Fan Official", "SEGA Genesis & Dreamcast content", "1.4M", "💿", Color(0xFF0066CC), "https://www.tiktok.com/@segafan_official", listOf("SEGA", "DREAMCAST")),
    TikTokCreator("t4", "@pixelnostalgia", "Pixel Nostalgia", "Retro pixel art & game reviews", "670K", "🎨", Color(0xFF00C851), "https://www.tiktok.com/@pixelnostalgia", listOf("PIXEL", "INDIE")),
    TikTokCreator("t5", "@arcadelegends", "Arcade Legends", "Classic arcade game showcases", "1.1M", "🕹️", Color(0xFFFF8C00), "https://www.tiktok.com/@arcadelegends", listOf("ARCADE", "CLASSIC")),
    TikTokCreator("t6", "@ps1memories", "PS1 Memories", "PlayStation 1 nostalgia content", "780K", "💙", Color(0xFF003791), "https://www.tiktok.com/@ps1memories", listOf("PS1", "PS2")),
    TikTokCreator("t7", "@gameboycollector", "GameBoy Collector", "Handheld gaming history", "560K", "🎮", Color(0xFF8B4513), "https://www.tiktok.com/@gameboycollector", listOf("GBA", "HANDHELD")),
    TikTokCreator("t8", "@retroboxart", "Retro Box Art", "Vintage game box art appreciation", "430K", "🖼️", Color(0xFFFF1493), "https://www.tiktok.com/@retroboxart", listOf("ART", "COLLECTING"))
)

// ─── Stream hype reactions ─────────────────────────────────────────────────────
data class StreamHype(
    val fire: Int = 0,
    val heart: Int = 0,
    val controller: Int = 0
)

// ─── Stream categories ─────────────────────────────────────────────────────────
val streamCategories = listOf("ALL", "RETRO", "RPG", "PLATFORMER", "FIGHTING", "RACING", "ARCADE")

// ─── Shimmer stream card ───────────────────────────────────────────────────────
@Composable
fun ShimmerStreamCard() {
    val shimmerT = rememberInfiniteTransition(label = "streamShimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "streamShimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ScrapbookPaper, Color.White.copy(alpha = 0.85f), ScrapbookPaper),
        start = androidx.compose.ui.geometry.Offset(shimmerX - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f)
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(shimmerBrush))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).background(ScrapbookCardWhite).padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth(0.8f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            }
        }
    }
}

// ─── Pulsing LIVE badge ────────────────────────────────────────────────────────
@Composable
fun PulsingLiveBadge(small: Boolean = false) {
    val pulseT = rememberInfiniteTransition(label = "livePulse")
    val pulseScale by pulseT.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "livePulseScale"
    )
    val dotAlpha by pulseT.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "liveDotAlpha"
    )
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFCC0000))
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.scale(pulseScale).size(if (small) 5.dp else 7.dp).clip(CircleShape).background(Color.White.copy(alpha = dotAlpha)))
            Text("LIVE", fontFamily = BangersFontFamily, color = Color.White, fontSize = if (small) 9.sp else 11.sp)
        }
    }
}

// ─── Stream Hype Board ─────────────────────────────────────────────────────────
@Composable
fun StreamHypeBoard(streamId: String) {
    var hype by remember { mutableStateOf(StreamHype()) }
    var myReactions by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(streamId) {
        try {
            val doc = FirebaseFirestore.getInstance().collection("stream_hype").document(streamId).get().await()
            if (doc.exists()) {
                hype = StreamHype(
                    fire = (doc.getLong("fire") ?: 0).toInt(),
                    heart = (doc.getLong("heart") ?: 0).toInt(),
                    controller = (doc.getLong("controller") ?: 0).toInt()
                )
            }
        } catch (e: Exception) { }
    }

    val neonT = rememberInfiniteTransition(label = "hypeNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "hypeNeonAlpha")

    fun react(type: String) {
        if (myReactions.contains(type)) return
        myReactions = myReactions + type
        val update = when (type) {
            "fire" -> hype.copy(fire = hype.fire + 1)
            "heart" -> hype.copy(heart = hype.heart + 1)
            "controller" -> hype.copy(controller = hype.controller + 1)
            else -> hype
        }
        hype = update
        try {
            FirebaseFirestore.getInstance().collection("stream_hype").document(streamId)
                .set(mapOf("fire" to update.fire, "heart" to update.heart, "controller" to update.controller))
        } catch (e: Exception) { }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(ScrapbookDark)
            .border(width = 1.dp, brush = Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), ScrapbookYellow.copy(alpha = 0.1f), ScrapbookYellow.copy(alpha = neonAlpha * 0.4f))), shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("HYPE", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(10.dp))
            listOf(Triple("fire", "🔥", hype.fire), Triple("heart", "❤️", hype.heart), Triple("controller", "🎮", hype.controller)).forEach { (type, emoji, count) ->
                val isReacted = myReactions.contains(type)
                var pressed by remember { mutableStateOf(false) }
                val btnScale by animateFloatAsState(targetValue = if (pressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "hypeBtn_$type")
                Row(
                    modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(8.dp))
                        .background(if (isReacted) ScrapbookYellow.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (isReacted) ScrapbookYellow.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .clickable { pressed = true; react(type) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(emoji, fontSize = 13.sp)
                    Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) ScrapbookYellow else Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

// ─── Featured Live Hero ────────────────────────────────────────────────────────
@Composable
fun FeaturedStreamHero(stream: TwitchStream, onClick: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "heroNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "heroNeonAlpha")
    val kenBurnsScale by neonT.animateFloat(initialValue = 1.0f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "heroKenBurns")

    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "heroCardScale")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        // Neon glow behind
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(16.dp).background(TwitchPurple.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)))
        Box(
            modifier = Modifier.fillMaxWidth().scale(cardScale)
                .clip(RoundedCornerShape(16.dp))
                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(TwitchPurple.copy(alpha = neonAlpha), TwitchPurple.copy(alpha = 0.3f), TwitchPurple.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp))
                .clickable { pressed = true; onClick() }
        ) {
            // Ken Burns thumbnail
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(model = stream.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurnsScale))
                // Gradient overlay
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
                // Top badges
                Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PulsingLiveBadge()
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("👑 FEATURED", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                    }
                }
                // Viewer count
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
                        Text("${formatViewerCount(stream.viewerCount)}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                    }
                }
                // Bottom info
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    Text(stream.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stream.userName, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = TwitchPurple, fontSize = 13.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow.copy(alpha = 0.9f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text(stream.gameName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            StreamHypeBoard(streamId = stream.id)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Category Filter ───────────────────────────────────────────────────────────
@Composable
fun StreamCategoryFilter(selected: String, onSelect: (String) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "catNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "catNeonAlpha")
    val categoryEmojis = mapOf("ALL" to "🕹️", "RETRO" to "👾", "RPG" to "⚔️", "PLATFORMER" to "🎮", "FIGHTING" to "👊", "RACING" to "🏎️", "ARCADE" to "🕹️")

    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(streamCategories) { cat ->
            val isSelected = selected == cat
            var pressed by remember { mutableStateOf(false) }
            val chipScale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "catChip_$cat")
            Box(
                modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                    .then(if (isSelected) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(20.dp)) else Modifier.border(1.dp, ScrapbookBorder, RoundedCornerShape(20.dp)))
                    .clickable { pressed = true; onSelect(cat) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("${categoryEmojis[cat] ?: "🎮"} $cat", fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 12.sp)
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
        }
    }
}

// ─── Game Filter ───────────────────────────────────────────────────────────────
@Composable
fun GameFilterBar(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        placeholder = { Text("Filter by game...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp)) },
        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextDark),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}

// ─── Clip of the Day ──────────────────────────────────────────────────────────
@Composable
fun ClipOfTheDayCard(video: YouTubeVideo, onClick: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "clipNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "clipNeonAlpha")
    val crownScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.12f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "clipCrownScale")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "clipKenBurns")

    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "clipCardScale")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(14.dp).background(YouTubeRed.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().scale(cardScale)
                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(YouTubeRed.copy(alpha = neonAlpha), YouTubeRed.copy(alpha = 0.3f), YouTubeRed.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)),
            backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AsyncImage(model = video.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns))
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                    // Crown badge
                    Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.scale(crownScale)) { Text("👑", fontSize = 20.sp) }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(YouTubeRed).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("CLIP OF THE DAY", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                    // Play button
                    Box(modifier = Modifier.size(56.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Text("▶", color = Color.White, fontSize = 22.sp)
                    }
                    // Date
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(video.publishedAt, fontFamily = NunitoFontFamily, color = Color.White, fontSize = 10.sp)
                    }
                }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(video.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(video.channelTitle, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = YouTubeRed, fontSize = 13.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(YouTubeRed).border(1.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { pressed = true; onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("WATCH NOW", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── TikTok Creator Card ──────────────────────────────────────────────────────
@Composable
fun TikTokCreatorCard(creator: TikTokCreator, neonAlpha: Float, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tiktokCardScale")

    Box(
        modifier = Modifier.width(180.dp).scale(cardScale)
            .clip(RoundedCornerShape(16.dp))
            .background(ScrapbookDark)
            .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(creator.accentColor.copy(alpha = neonAlpha), creator.accentColor.copy(alpha = 0.3f), creator.accentColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp))
            .clickable { pressed = true; onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar circle with emoji
            Box(modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(creator.accentColor.copy(alpha = 0.4f), ScrapbookDark)))
                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(creator.accentColor.copy(alpha = neonAlpha), creator.accentColor.copy(alpha = 0.3f), creator.accentColor.copy(alpha = neonAlpha))), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(creator.emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(creator.username, fontFamily = BangersFontFamily, color = creator.accentColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(creator.displayName, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            // Follower count badge
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(creator.accentColor.copy(alpha = 0.15f)).border(1.dp, creator.accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("${creator.followers} followers", fontFamily = BangersFontFamily, color = creator.accentColor, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(creator.niche, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // Tags
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(creator.tags.take(2)) { tag ->
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tag, fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            // TikTok button
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .background(Brush.horizontalGradient(colors = listOf(TikTokPink, TikTokCyan)))
                .padding(vertical = 8.dp), contentAlignment = Alignment.Center
            ) {
                Text("VIEW ON TIKTOK", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}
// ─── Main Screen ──────────────────────────────────────────────────────────────
@Composable
fun StreamsScreen(
    modifier: Modifier = Modifier,
    streamsViewModel: StreamsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val twitchState by streamsViewModel.twitchStreams.collectAsState()
    val youtubeState by streamsViewModel.youtubeVideos.collectAsState()
    val communityStreamers by streamsViewModel.communityStreamers.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🔴 LIVE", "🎬 VIDEOS", "📱 TIKTOK", "👥 COMMUNITY")

    val neonT = rememberInfiniteTransition(label = "streamsNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "streamsNeonAlpha")

    // Live count from Twitch state
    val liveCount = when (val s = twitchState) {
        is StreamsState.Success<*> -> (s.data as? List<TwitchStream>)?.size ?: 0
        else -> 0
    }

    LaunchedEffect(allUsers) { streamsViewModel.loadCommunityStreamers(allUsers) }
    LaunchedEffect(Unit) { authViewModel.fetchAllUsers() }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Golden gradient header
            // ─── Page Hero ────────────────────────────────────────────────────
            // Hero goes inside each tab's LazyColumn as first item
            // Toolbar with live count + refresh stays here
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Live count badge
                    if (liveCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                val dotPulse by neonT.animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "dotPulse")
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.Red.copy(alpha = dotPulse)))
                                Text("$liveCount LIVE", fontFamily = BangersFontFamily, color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Refresh button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), CircleShape)
                            .clickable { streamsViewModel.fetchAll() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ✅ Tab strip — dark neon style
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).border(BorderStroke(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.3f)))) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        var tabPressed by remember { mutableStateOf(false) }
                        val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tab_$index")
                        Box(
                            modifier = Modifier.weight(1f).scale(tabScale).clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ScrapbookYellow else Color.Transparent)
                                .clickable { tabPressed = true; selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.45f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                        LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                    }
                }
            }

            // Content
            when (selectedTab) {
                0 -> TwitchStreamsTab(state = twitchState)
                1 -> YouTubeVideosTab(state = youtubeState)
                2 -> TikTokTab()
                3 -> CommunityStreamersTab(streamers = communityStreamers)
            }
        }
    }
}

// ─── Twitch Streams Tab ───────────────────────────────────────────────────────
@Composable
fun TwitchStreamsTab(state: StreamsState) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("ALL") }
    var gameFilter by remember { mutableStateOf("") }

    when (state) {
        is StreamsState.Loading -> {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(3) { ShimmerStreamCard() }
            }
        }
        is StreamsState.Error -> StreamsErrorState(message = state.message)
        is StreamsState.Empty -> StreamsEmptyState(emoji = "📺", title = "NO LIVE STREAMS", subtitle = "No retro gaming streams live right now.\nCheck back later!")
        is StreamsState.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            val streams = state.data as List<TwitchStream>
            val featuredStream = streams.maxByOrNull { it.viewerCount }

            val filteredStreams = remember(streams, selectedCategory, gameFilter) {
                streams.filter { stream ->
                    val categoryMatch = selectedCategory == "ALL" || stream.gameName.contains(selectedCategory, ignoreCase = true)
                    val gameMatch = gameFilter.isBlank() || stream.gameName.contains(gameFilter, ignoreCase = true) || stream.title.contains(gameFilter, ignoreCase = true)
                    categoryMatch && gameMatch
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {

                // ─── Page Hero ────────────────────────────────────────────────
                item {
                    RetroHubPageHero(
                        config = streamsHeroConfig,
                        onCtaClick = { /* already on streams */ }
                    )
                }
                item {
                    RetroHubPageTicker(config = streamsHeroConfig)
                }
                // Featured hero
                featuredStream?.let { hero ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        FeaturedStreamHero(stream = hero, onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/${hero.userName}")))
                        })
                    }
                }

                // Section label + filters
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    val neonT = rememberInfiniteTransition(label = "liveNeon")
                    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "liveNeonAlpha")
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(Color.Red.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ALL STREAMS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("${filteredStreams.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    StreamCategoryFilter(selected = selectedCategory, onSelect = { selectedCategory = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    GameFilterBar(query = gameFilter, onQueryChange = { gameFilter = it })
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Stream cards
                itemsIndexed(filteredStreams, key = { _, s -> s.id }) { index, stream ->
                    val enterAnim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay(index * 60L); enterAnim.animateTo(1f, tween(350, easing = LinearOutSlowInEasing)) }
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).scale(enterAnim.value).alpha(enterAnim.value)) {
                        TwitchStreamCard(stream = stream, onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/${stream.userName}")))
                        })
                    }
                }

                if (filteredStreams.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("NO STREAMS MATCH", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                Text("Try a different category or game filter", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── YouTube Videos Tab ───────────────────────────────────────────────────────
@Composable
fun YouTubeVideosTab(state: StreamsState) {
    val context = LocalContext.current

    when (state) {
        is StreamsState.Loading -> {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(3) { ShimmerStreamCard() }
            }
        }
        is StreamsState.Error -> StreamsErrorState(message = state.message)
        is StreamsState.Empty -> StreamsEmptyState(emoji = "🎬", title = "NO VIDEOS FOUND", subtitle = "Could not load retro gaming videos.\nCheck back later!")
        is StreamsState.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            val videos = state.data as List<YouTubeVideo>
            val clipOfDay = remember(videos) {
                val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
                if (videos.isNotEmpty()) videos[dayOfYear % videos.size] else null
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {

                // Clip of the Day
                clipOfDay?.let { clip ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ClipOfTheDayCard(video = clip, onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clip.videoUrl)))
                        })
                    }
                }

                // Section header
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    val neonT = rememberInfiniteTransition(label = "ytNeon")
                    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "ytNeonAlpha")
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(YouTubeRed.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🎬 RETRO GAMING VIDEOS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("${videos.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                    val enterAnim = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay(index * 60L); enterAnim.animateTo(1f, tween(350, easing = LinearOutSlowInEasing)) }
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).scale(enterAnim.value).alpha(enterAnim.value)) {
                        YouTubeVideoCard(video = video, onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl)))
                        })
                    }
                }
            }
        }
    }
}

// ─── TikTok Tab ───────────────────────────────────────────────────────────────
@Composable
fun TikTokTab() {
    val context = LocalContext.current
    val neonT = rememberInfiniteTransition(label = "tiktokNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "tiktokNeonAlpha")
    val scanT = rememberInfiniteTransition(label = "tiktokScan")
    val scanY by scanT.animateFloat(initialValue = -200f, targetValue = 800f, animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart), label = "tiktokScanY")

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {

        // Header card
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark)
                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(TikTokPink.copy(alpha = neonAlpha), TikTokCyan.copy(alpha = neonAlpha * 0.5f), TikTokPink.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp))
            ) {
                // Scan line
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).offset(y = scanY.dp).background(TikTokPink.copy(alpha = 0.2f)))
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // TikTok logo-style icon
                    Box(contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(TikTokCyan.copy(alpha = 0.2f)).border(1.dp, TikTokCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)))
                        Text("📱", fontSize = 26.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TIKTOK RETRO GAMING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp)
                        Text("Top retro gaming creators to follow", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(TikTokPink.copy(alpha = 0.2f)).border(1.dp, TikTokPink.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${retrogamingTikTokCreators.size} CREATORS", fontFamily = BangersFontFamily, color = TikTokPink, fontSize = 10.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(TikTokCyan.copy(alpha = 0.2f)).border(1.dp, TikTokCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("CURATED", fontFamily = BangersFontFamily, color = TikTokCyan, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(colors = listOf(TikTokPink, TikTokCyan))))
                Spacer(modifier = Modifier.width(8.dp))
                Text("📱 CREATORS TO FOLLOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tap any card to open their TikTok profile", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Creator cards in horizontal rows of 2
        val rows = retrogamingTikTokCreators.chunked(2)
        itemsIndexed(rows) { rowIndex, rowCreators ->
            val enterAnim = remember { Animatable(0f) }
            LaunchedEffect(Unit) { delay(rowIndex * 80L); enterAnim.animateTo(1f, tween(350, easing = LinearOutSlowInEasing)) }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).scale(enterAnim.value).alpha(enterAnim.value), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowCreators.forEach { creator ->
                    Box(modifier = Modifier.weight(1f)) {
                        TikTokCreatorCard(creator = creator, neonAlpha = neonAlpha, onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(creator.profileUrl))) } catch (e: Exception) { }
                        })
                    }
                }
                if (rowCreators.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Disclaimer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookPaper).border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).padding(12.dp)) {
                Text("📋 Creator list is curated by the RetroHub team. Follower counts are approximate. Tap any card to visit their TikTok profile.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Community Streamers Tab ──────────────────────────────────────────────────
@Composable
fun CommunityStreamersTab(streamers: List<CommunityStreamer>) {
    val context = LocalContext.current
    val neonT = rememberInfiniteTransition(label = "communityNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "communityNeonAlpha")

    if (streamers.isEmpty()) {
        StreamsEmptyState(emoji = "👥", title = "NO COMMUNITY STREAMERS", subtitle = "No RetroHub members have linked their\nTwitch or YouTube yet.\nAdd yours in your profile!")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("👥 RETROHUB STREAMERS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("${streamers.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                    }
                }
                // Live streamers first hint
                val liveCount = streamers.count { it.isLive }
                if (liveCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFCC0000).copy(alpha = 0.1f)).border(1.dp, Color(0xFFCC0000).copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val dotPulse by neonT.animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "commDotPulse")
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color.Red.copy(alpha = dotPulse)))
                            Text("$liveCount member${if (liveCount > 1) "s" else ""} streaming live right now!", fontFamily = BangersFontFamily, color = Color(0xFFCC0000), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            val sortedStreamers = streamers.sortedByDescending { it.isLive }
            itemsIndexed(sortedStreamers, key = { _, s -> s.uid }) { index, streamer ->
                val enterAnim = remember { Animatable(0f) }
                LaunchedEffect(Unit) { delay(index * 50L); enterAnim.animateTo(1f, tween(300, easing = LinearOutSlowInEasing)) }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).scale(enterAnim.value).alpha(enterAnim.value)) {
                    CommunityStreamerCard(
                        streamer = streamer, neonAlpha = neonAlpha,
                        onTwitchClick = { if (streamer.twitchUsername.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/${streamer.twitchUsername}"))) },
                        onYouTubeClick = { if (streamer.youtubeUsername.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@${streamer.youtubeUsername}"))) }
                    )
                }
            }
        }
    }
}
// ─── Twitch Stream Card ───────────────────────────────────────────────────────
@Composable
fun TwitchStreamCard(stream: TwitchStream, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "twitchCardScale")
    val neonT = rememberInfiniteTransition(label = "twitchCardNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "twitchCardNeonAlpha")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "twitchKenBurns")

    Column(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(TwitchPurple.copy(alpha = neonAlpha * 0.4f), TwitchPurple.copy(alpha = 0.1f), TwitchPurple.copy(alpha = neonAlpha * 0.4f))), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                .clickable { pressed = true; onClick() },
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookDark)) {
                    AsyncImage(model = stream.thumbnailUrl, contentDescription = stream.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns))
                    // Gradient overlay
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    // LIVE badge
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) { PulsingLiveBadge() }
                    // Viewer count
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
                            Text("${formatViewerCount(stream.viewerCount)}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    // Twitch badge
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(TwitchPurple).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("TWITCH", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                    }
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stream.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stream.userName, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = TwitchPurple, fontSize = 13.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(stream.gameName, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        // ✅ Hype board attached below card
        StreamHypeBoard(streamId = stream.id)
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── YouTube Video Card ───────────────────────────────────────────────────────
@Composable
fun YouTubeVideoCard(video: YouTubeVideo, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ytCardScale")
    val neonT = rememberInfiniteTransition(label = "ytCardNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "ytCardNeonAlpha")
    val kenBurns by neonT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse), label = "ytKenBurns")
    val playScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "ytPlayScale")

    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(YouTubeRed.copy(alpha = neonAlpha * 0.4f), YouTubeRed.copy(alpha = 0.1f), YouTubeRed.copy(alpha = neonAlpha * 0.4f))), shape = RoundedCornerShape(12.dp))
                .clickable { pressed = true; onClick() },
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookDark)) {
                    AsyncImage(model = video.thumbnailUrl, contentDescription = video.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kenBurns))
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                    // Play button
                    Box(modifier = Modifier.size(52.dp).align(Alignment.Center).scale(playScale).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f)).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                        Text("▶", color = Color.White, fontSize = 20.sp)
                    }
                    // YouTube badge
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(YouTubeRed).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("YOUTUBE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                    }
                    // Date
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(video.publishedAt, fontFamily = NunitoFontFamily, color = Color.White, fontSize = 10.sp)
                    }
                }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(video.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(video.channelTitle, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = YouTubeRed, fontSize = 13.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Community Streamer Card ──────────────────────────────────────────────────
@Composable
fun CommunityStreamerCard(streamer: CommunityStreamer, neonAlpha: Float = 0.6f, onTwitchClick: () -> Unit, onYouTubeClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "communityCardScale")

    // Fetch Habbo if available
    var habboUsername by remember { mutableStateOf("") }
    var habboRegion by remember { mutableStateOf("habbo.com") }
    LaunchedEffect(streamer.uid) {
        try {
            val doc = FirebaseFirestore.getInstance().collection("users").document(streamer.uid).get().await()
            habboUsername = doc.getString("habboUsername") ?: ""
            habboRegion = doc.getString("habboRegion") ?: "habbo.com"
        } catch (e: Exception) { }
    }

    Box(modifier = Modifier.fillMaxWidth().scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .then(if (streamer.isLive) Modifier.border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(Color.Red.copy(alpha = neonAlpha), Color.Red.copy(alpha = 0.3f), Color.Red.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp)) else Modifier),
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ✅ Habbo avatar or profile pic
                    Box(modifier = Modifier.size(56.dp)) {
                        if (habboUsername.isNotBlank()) {
                            val avatarUrl = "https://$habboRegion/habbo-imaging/avatarimage?user=$habboUsername&action=std&direction=2&head_direction=2&gesture=sml&size=m"
                            AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        } else {
                            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(ScrapbookPaper).border(2.dp, if (streamer.isLive) Color.Red.copy(alpha = 0.5f) else ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                                if (streamer.profilePicUrl.isNotBlank()) {
                                    AsyncImage(model = streamer.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                } else {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                        // Live dot
                        if (streamer.isLive) {
                            val dotPulse by rememberInfiniteTransition(label = "commDot_${streamer.uid}").animateFloat(initialValue = 0.5f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "commDotAlpha")
                            Box(modifier = Modifier.size(12.dp).align(Alignment.TopEnd).clip(CircleShape).background(Color.Red.copy(alpha = dotPulse)).border(1.5.dp, Color.White, CircleShape))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(streamer.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (streamer.isLive) PulsingLiveBadge(small = true)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (streamer.twitchUsername.isNotBlank()) {
                                var tPressed by remember { mutableStateOf(false) }
                                val tScale by animateFloatAsState(targetValue = if (tPressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "twitchBtnScale")
                                Box(modifier = Modifier.scale(tScale).clip(RoundedCornerShape(6.dp)).background(TwitchPurple).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).clickable { tPressed = true; onTwitchClick() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("TWITCH", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                                }
                                LaunchedEffect(tPressed) { if (tPressed) { delay(150); tPressed = false } }
                            }
                            if (streamer.youtubeUsername.isNotBlank()) {
                                var yPressed by remember { mutableStateOf(false) }
                                val yScale by animateFloatAsState(targetValue = if (yPressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ytBtnScale")
                                Box(modifier = Modifier.scale(yScale).clip(RoundedCornerShape(6.dp)).background(YouTubeRed).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).clickable { yPressed = true; onYouTubeClick() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("YOUTUBE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                                }
                                LaunchedEffect(yPressed) { if (yPressed) { delay(150); yPressed = false } }
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Empty & Error States ─────────────────────────────────────────────────────
@Composable
fun StreamsEmptyState(emoji: String, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ScrapbookCard(modifier = Modifier.padding(32.dp), backgroundColor = ScrapbookCardWhite, cornerRadius = 16.dp, shadowOffset = 4.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text(emoji, fontSize = 52.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun StreamsErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ScrapbookCard(modifier = Modifier.padding(32.dp), backgroundColor = ScrapbookCardWhite, cornerRadius = 16.dp, shadowOffset = 4.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Text("⚠️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
fun formatViewerCount(count: Int): String = when {
    count >= 1000 -> "${count / 1000}K"
    else -> count.toString()
}

// Keep old EmptyState/ErrorState for backward compat
@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) = StreamsEmptyState(emoji, title, subtitle)

@Composable
fun ErrorState(message: String) = StreamsErrorState(message)


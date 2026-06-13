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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

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

// ─── Community Listener ───────────────────────────────────────────────────────

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

fun eraAccentColor(era: String): Color = when (era.uppercase()) {
    "NES" -> Color(0xFFE53935)
    "SNES" -> Color(0xFF8E24AA)
    "PS1" -> Color(0xFF546E7A)
    "PS2" -> Color(0xFF1565C0)
    "N64" -> Color(0xFF2E7D32)
    "GCN" -> Color(0xFF6A1B9A)
    "GBA" -> Color(0xFF00838F)
    "NDS" -> Color(0xFFEF6C00)
    "PC" -> Color(0xFF00695C)
    else -> ScrapbookYellow
}

// ─── Vibe Helper ──────────────────────────────────────────────────────────────

data class AlbumVibe(val label: String, val emoji: String, val color: Color)

val albumVibes = listOf(
    AlbumVibe("ALL", "🎵", ScrapbookYellow),
    AlbumVibe("NOSTALGIC", "😢", Color(0xFF5C6BC0)),
    AlbumVibe("HYPE", "🔥", Color(0xFFE53935)),
    AlbumVibe("CHILL", "😌", Color(0xFF26A69A)),
    AlbumVibe("EPIC", "⚔️", Color(0xFF8D6E63)),
    AlbumVibe("CUTE", "🌸", Color(0xFFEC407A))
)

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleAlbums = listOf(
    Album(id = "album1", title = "Gunbound", artist = "Synth Rider", coverImageResId = R.drawable.ostcover1, year = 1984, webPlaybackUrl = "https://archive.org/details/gunbound-soundtrack", era = "NES", vibe = "NOSTALGIC", playCount = 42, reactions = mapOf("🔥" to 12, "❤️" to 8, "🎮" to 5)),
    Album(id = "album2", title = "Pokemon Diamond & Pearl", artist = "Grid Runner", coverImageResId = R.drawable.ostcover2, year = 1988, webPlaybackUrl = "https://archive.org/details/pkmn-dppt-soundtrack", era = "NDS", vibe = "CUTE", playCount = 88, reactions = mapOf("🔥" to 31, "❤️" to 44, "🎮" to 19)),
    Album(id = "album3", title = "The Legend of Zelda: The Wind Waker", artist = "Chrome Catalyst", coverImageResId = R.drawable.ostcover3, year = 1991, webPlaybackUrl = "https://archive.org/details/the-legend-of-zelda-the-wind-waker-ost", era = "GCN", vibe = "EPIC", playCount = 76, reactions = mapOf("🔥" to 28, "❤️" to 35, "🎮" to 22)),
    Album(id = "album4", title = "Undertale", artist = "Vector Voyager", coverImageResId = R.drawable.ostcover4, year = 1986, webPlaybackUrl = "https://archive.org/details/undertaleost_202004", era = "PC", vibe = "NOSTALGIC", playCount = 95, reactions = mapOf("🔥" to 41, "❤️" to 52, "🎮" to 18)),
    Album(id = "album5", title = "Lego Harry Potter Years 1-4", artist = "Bit Shifter", coverImageResId = R.drawable.ostcover5, year = 1982, webPlaybackUrl = "https://archive.org/details/lego-harry-potter-years-1-4", era = "PS2", vibe = "CHILL", playCount = 33, reactions = mapOf("🔥" to 7, "❤️" to 15, "🎮" to 6)),
    Album(id = "album6", title = "Final Fantasy VII", artist = "Analog Hero", coverImageResId = R.drawable.ostcover6, year = 1987, webPlaybackUrl = "https://archive.org/details/final_fantasy_vii_soundtrack", era = "PS1", vibe = "EPIC", playCount = 112, reactions = mapOf("🔥" to 55, "❤️" to 61, "🎮" to 33)),
    Album(id = "album7", title = "The Sims", artist = "Digital Nomad", coverImageResId = R.drawable.ostcover7, year = 1985, webPlaybackUrl = "https://archive.org/details/simsmusic", era = "PC", vibe = "CHILL", playCount = 67, reactions = mapOf("🔥" to 22, "❤️" to 38, "🎮" to 11))
)

val albumEraFilters = listOf("ALL", "NES", "SNES", "PS1", "PS2", "N64", "GCN", "GBA", "NDS", "PC", "OTHER")

// ─── Time-aware Greeting ──────────────────────────────────────────────────────

fun getChillZoneGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning, gamer ☀️"
        in 12..16 -> "Afternoon session? 🎮"
        in 17..20 -> "Evening vibes loading... 🌆"
        in 21..23 -> "Late night session? 🌙"
        else -> "Can't sleep? 👾 We've got you."
    }
}

// ─── Album of the Day ─────────────────────────────────────────────────────────

fun getAlbumOfTheDay(): Album {
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return sampleAlbums[dayOfYear % sampleAlbums.size]
}

// ─── Now Playing Bar ──────────────────────────────────────────────────────────

@Composable
fun NowPlayingBar(state: NowPlayingState, onClick: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "nowPlayingNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "nowPlayingNeonAlpha")
    val dotAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "dotAlpha")

    val eqT = rememberInfiniteTransition(label = "eq")
    val eqHeights = (0..4).map { i ->
        eqT.animateFloat(initialValue = 4f, targetValue = (14 + i * 3).toFloat(), animationSpec = infiniteRepeatable(tween(300 + i * 80, easing = EaseInOut), RepeatMode.Reverse), label = "eq_$i")
    }

    val vinylT = rememberInfiniteTransition(label = "barVinyl")
    val vinylAngle by vinylT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "barVinylAngle")

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().blur(8.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.1f)))
        Box(
            modifier = Modifier.fillMaxWidth().background(ScrapbookDark)
                .border(width = 1.dp, brush = Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.6f), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha * 0.6f))), shape = RoundedCornerShape(0.dp))
                .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // ✅ Spinning vinyl
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(ScrapbookDark)
                        .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).rotate(vinylAngle), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF1A1A1A)))
                        when {
                            state.coverResId != null -> Image(painter = painterResource(id = state.coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(34.dp).clip(CircleShape), alpha = 0.8f)
                            state.coverUrl != null -> AsyncImage(model = state.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(34.dp).clip(CircleShape), alpha = 0.8f)
                            else -> Text("♪", color = ScrapbookYellow, fontSize = 14.sp)
                        }
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ScrapbookDark))
                }

                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.artist.isNotBlank()) Text(state.artist, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(20.dp)) {
                    eqHeights.forEachIndexed { i, heightState -> val h by heightState; Box(modifier = Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = 0.7f + i * 0.06f))) }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = dotAlpha)))
                    Text("PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── Era Timeline ─────────────────────────────────────────────────────────────

@Composable
fun EraTimeline(selectedEra: String, onEraSelected: (String) -> Unit) {
    val eras = listOf("NES", "SNES", "N64", "GBA", "GCN", "NDS", "PS1", "PS2", "PC")
    val neonT = rememberInfiniteTransition(label = "eraTimelineNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "eraTimelineNeonAlpha")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ERA TIMELINE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(8.dp))
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
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = 0.3f), Color.Transparent))).align(Alignment.Center))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(0.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                items(eras) { era ->
                    val isSelected = selectedEra == era
                    val eraColor = eraAccentColor(era)
                    var pressed by remember { mutableStateOf(false) }
                    val itemScale by animateFloatAsState(targetValue = if (pressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "eraScale")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp).scale(itemScale).clickable { pressed = true; onEraSelected(if (selectedEra == era) "ALL" else era) }.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(if (isSelected) 44.dp else 36.dp).clip(CircleShape)
                                .background(if (isSelected) eraColor else ScrapbookCardWhite)
                                .border(width = if (isSelected) 2.dp else 1.dp, brush = if (isSelected) Brush.linearGradient(colors = listOf(eraColor.copy(alpha = neonAlpha), eraColor.copy(alpha = 0.4f), eraColor.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(era, fontFamily = BangersFontFamily, color = if (isSelected) Color.White else ScrapbookDark, fontSize = if (isSelected) 9.sp else 8.sp, textAlign = TextAlign.Center)
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

// ─── Vibe Selector ────────────────────────────────────────────────────────────

@Composable
fun AlbumVibeSelector(selectedVibe: String, onVibeSelected: (String) -> Unit, neonAlpha: Float) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(albumVibes) { vibe ->
            val isSelected = selectedVibe == vibe.label
            var pressed by remember { mutableStateOf(false) }
            val chipScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "vibeScale")
            Box(
                modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                    .border(width = if (isSelected) 2.dp else 1.dp, brush = if (isSelected) Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = RoundedCornerShape(20.dp))
                    .clickable { pressed = true; onVibeSelected(vibe.label) }.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(vibe.emoji, fontSize = 13.sp)
                    Text(vibe.label, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 12.sp)
                }
            }
            LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
        }
    }
}

// ─── Album Reaction Bar ───────────────────────────────────────────────────────

@Composable
fun AlbumReactionBar(albumId: String, initialReactions: Map<String, Int> = emptyMap()) {
    val reactionEmojis = listOf("🔥", "❤️", "🎮")
    var reactions by remember { mutableStateOf(initialReactions.toMutableMap()) }
    var userReacted by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reactionEmojis.forEach { emoji ->
            val count = reactions[emoji] ?: 0
            val isReacted = userReacted == emoji
            var popped by remember { mutableStateOf(false) }
            val popScale by animateFloatAsState(targetValue = if (popped) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "pop_$emoji")
            Box(
                modifier = Modifier.scale(popScale).clip(RoundedCornerShape(20.dp))
                    .background(if (isReacted) ScrapbookYellow.copy(alpha = 0.15f) else ScrapbookPaper)
                    .border(1.5.dp, if (isReacted) ScrapbookYellow.copy(alpha = 0.8f) else ScrapbookBorder.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable {
                        popped = true
                        if (userReacted == emoji) { reactions = reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 1) - 1 }; userReacted = null }
                        else { userReacted?.let { prev -> reactions = reactions.toMutableMap().apply { this[prev] = (this[prev] ?: 1) - 1 } }; reactions = reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 0) + 1 }; userReacted = emoji }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(emoji, fontSize = 13.sp)
                    Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) ScrapbookYellow else ScrapbookTextMuted, fontSize = 12.sp)
                }
            }
            LaunchedEffect(popped) { if (popped) { kotlinx.coroutines.delay(200); popped = false } }
        }
    }
}

// ─── Community Listening Room ─────────────────────────────────────────────────

@Composable
fun CommunityListeningRoom(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val neonT = rememberInfiniteTransition(label = "clrNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "clrNeonAlpha")

    // ✅ Sample community listeners — in production fetch from Firestore
    val sampleListeners = remember {
        listOf(
            CommunityListener(uid = "u1", username = "Don Carlos", habboUsername = "", profilePicUrl = null, albumTitle = "Final Fantasy VII", albumId = "album6", timeAgo = "2m ago"),
            CommunityListener(uid = "u2", username = "Topin99", habboUsername = "", profilePicUrl = null, albumTitle = "Undertale", albumId = "album4", timeAgo = "5m ago"),
            CommunityListener(uid = "u3", username = "HomicidalYellio", habboUsername = "", profilePicUrl = null, albumTitle = "Pokemon Diamond & Pearl", albumId = "album2", timeAgo = "11m ago"),
            CommunityListener(uid = "u4", username = "Fabriko98", habboUsername = "", profilePicUrl = null, albumTitle = "The Sims", albumId = "album7", timeAgo = "18m ago"),
            CommunityListener(uid = "u5", username = "Carollerm", habboUsername = "", profilePicUrl = null, albumTitle = "Wind Waker", albumId = "album3", timeAgo = "25m ago")
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎧 LISTENING NOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            // ✅ Live dot
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ScrapbookRed.copy(alpha = neonAlpha)))
                Text("LIVE", fontFamily = BangersFontFamily, color = ScrapbookRed, fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("See what the community is vibing to right now", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(10.dp))

        sampleListeners.forEach { listener ->
            val album = albums.firstOrNull { it.id == listener.albumId }
            var pressed by remember { mutableStateOf(false) }
            val rowScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "listenerRow")

            Box(
                modifier = Modifier.fillMaxWidth().scale(rowScale).padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScrapbookCardWhite)
                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .clickable { pressed = true; album?.let { onAlbumClick(it) } }
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // ✅ Avatar — Habbo if linked, profile pic if available, initial letter fallback
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .background(ScrapbookDark)
                            .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.7f), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha * 0.7f))), shape = CircleShape),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        when {
                            listener.habboUsername.isNotBlank() -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(habboAvatarUrl(listener.habboUsername, listener.habboRegion))
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            listener.profilePicUrl != null -> {
                                AsyncImage(model = listener.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            }
                            else -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(listener.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(listener.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🎵", fontSize = 10.sp)
                            Text(listener.albumTitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(listener.timeAgo, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                        if (album != null) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(eraAccentColor(album.era).copy(alpha = 0.15f)).border(1.dp, eraAccentColor(album.era).copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(album.era, fontFamily = BangersFontFamily, color = eraAccentColor(album.era), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
        }
    }
}

// ─── Memory Jukebox ───────────────────────────────────────────────────────────

@Composable
fun MemoryJukebox(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    var currentAlbum by remember { mutableStateOf<Album?>(null) }
    var isFlipping by remember { mutableStateOf(false) }

    val neonT = rememberInfiniteTransition(label = "jukeboxNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "jukeboxNeonAlpha")
    val coinScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.08f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "coinScale")

    val flipAngle by animateFloatAsState(
        targetValue = if (revealed) 0f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "flipAngle"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎰 MEMORY JUKEBOX", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Drop a coin and discover a random retro track", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(14.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.1f), RoundedCornerShape(16.dp)))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScrapbookDark)
                    .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!revealed || currentAlbum == null) {
                    // ✅ Insert coin state
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = 0.1f)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = CircleShape), contentAlignment = Alignment.Center) {
                        Text("🪙", fontSize = 36.sp, modifier = Modifier.scale(if (!isFlipping) coinScale else 1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("INSERT COIN", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 22.sp, letterSpacing = 2.sp)
                    Text("Tap to reveal a random retro track", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = 0.8f), ScrapbookYellow, ScrapbookYellow.copy(alpha = 0.8f))))
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                isFlipping = true
                                currentAlbum = albums.random()
                                revealed = true
                                isFlipping = false
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🪙", fontSize = 18.sp)
                            Text("DROP COIN", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    // ✅ Revealed album
                    val album = currentAlbum!!
                    val eraColor = eraAccentColor(album.era)

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ScrapbookDark.copy(alpha = 0.5f))
                            .border(1.dp, eraColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(eraColor.copy(alpha = neonAlpha), eraColor.copy(alpha = 0.3f), eraColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))) {
                            when {
                                album.coverImageResId != null -> Image(painter = painterResource(id = album.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                album.coverImageUrl != null -> AsyncImage(model = album.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("♪", fontSize = 28.sp) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookYellow).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("🎰 YOUR TRACK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(album.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                            Text(album.artist, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            album.year?.let { Text("$it", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp) }
                        }
                        if (album.era.isNotBlank() && album.era != "OTHER") {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraColor.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(album.era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // ✅ Play button
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = 0.8f), ScrapbookYellow, ScrapbookYellow.copy(alpha = 0.8f))))
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                .clickable { onAlbumClick(album) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                                Text("PLAY NOW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                            }
                        }
                        // ✅ Try again button
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ScrapbookDark.copy(alpha = 0.5f))
                                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.5f), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha * 0.5f))), shape = RoundedCornerShape(10.dp))
                                .clickable { revealed = false; currentAlbum = null }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🪙", fontSize = 14.sp)
                                Text("TRY AGAIN", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Genre Radar ──────────────────────────────────────────────────────────────

@Composable
fun AlbumGenreRadar(albums: List<Album>) {
    val neonT = rememberInfiniteTransition(label = "radarNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "radarNeonAlpha")

    // ✅ Calculate vibe distribution from reactions + vibe field
    val vibeScores = remember(albums) {
        val scores = mutableMapOf<String, Float>()
        albumVibes.filter { it.label != "ALL" }.forEach { vibe -> scores[vibe.label] = 0f }
        albums.forEach { album ->
            val totalReactions = album.reactions.values.sum().toFloat().coerceAtLeast(1f)
            val boost = album.reactions["🔥"]?.toFloat() ?: 0f
            val currentScore = scores[album.vibe] ?: 0f
            scores[album.vibe] = currentScore + totalReactions + boost * 0.5f
        }
        val maxScore = scores.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        scores.mapValues { (_, v) -> (v / maxScore).coerceIn(0.1f, 1f) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("🎯 COMMUNITY VIBE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("What the community is feeling this week", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(10.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.08f), RoundedCornerShape(16.dp)))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScrapbookDark)
                    .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.5f), ScrapbookYellow.copy(alpha = 0.1f), ScrapbookYellow.copy(alpha = neonAlpha * 0.5f))), shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // ✅ Dominant vibe badge
                val dominantVibe = vibeScores.maxByOrNull { it.value }?.key ?: "CHILL"
                val dominantVibeData = albumVibes.firstOrNull { it.label == dominantVibe }

                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(dominantVibeData?.color?.copy(alpha = 0.15f) ?: ScrapbookYellow.copy(alpha = 0.1f))
                        .border(1.dp, dominantVibeData?.color?.copy(alpha = 0.4f) ?: ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(dominantVibeData?.emoji ?: "🎵", fontSize = 22.sp)
                        Column {
                            Text("THIS WEEK'S DOMINANT VIBE", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 1.sp)
                            Text(dominantVibe, fontFamily = BangersFontFamily, color = dominantVibeData?.color ?: ScrapbookYellow, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ✅ Bar chart for each vibe
                albumVibes.filter { it.label != "ALL" }.forEach { vibe ->
                    val score = vibeScores[vibe.label] ?: 0f
                    val animatedScore by animateFloatAsState(targetValue = score, animationSpec = tween(1000, delayMillis = albumVibes.indexOf(vibe) * 100, easing = LinearOutSlowInEasing), label = "vibeBar_${vibe.label}")
                    val percentage = (score * 100).toInt()

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(vibe.emoji, fontSize = 16.sp)
                        Text(vibe.label, fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.width(80.dp))
                        Box(modifier = Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.05f))) {
                            Box(
                                modifier = Modifier.fillMaxWidth(animatedScore).fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Brush.horizontalGradient(colors = listOf(vibe.color.copy(alpha = 0.6f), vibe.color)))
                            )
                        }
                        Text("$percentage%", fontFamily = BangersFontFamily, color = vibe.color, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

// ─── Featured Album Hero Card ─────────────────────────────────────────────────

@Composable
fun FeaturedAlbumHeroCard(title: String, artist: String, coverResId: Int? = null, coverUrl: String? = null, year: String = "", badge: String = "🎵 FEATURED", era: String = "OTHER", onPlay: () -> Unit) {
    val eraColor = eraAccentColor(era)
    val neonT = rememberInfiniteTransition(label = "featuredNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "featuredNeonAlpha")
    val btnScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "featuredBtnScale")

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(16.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.15f), RoundedCornerShape(16.dp)))
        ScrapbookCard(modifier = Modifier.fillMaxWidth().border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)), backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                when {
                    coverResId != null -> Image(painter = painterResource(id = coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.4f)
                    coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.4f)
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.98f)))))
                val scanT = rememberInfiniteTransition(label = "heroScan")
                val scanY by scanT.animateFloat(initialValue = -240f, targetValue = 240f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "heroScanY")
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).offset(y = scanY.dp).background(ScrapbookYellow.copy(alpha = 0.12f)))
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(badge, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp) }
                        if (era.isNotBlank() && era != "OTHER") { Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraColor.copy(alpha = 0.8f)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp) } }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper).border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.4f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))) {
                            when {
                                coverResId != null -> Image(painter = painterResource(id = coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("♪", fontSize = 28.sp) }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 26.sp)
                            Text(artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            if (year.isNotBlank()) Text(year, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.scale(btnScale)) {
                            Box(modifier = Modifier.size(60.dp).blur(12.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.5f), CircleShape))
                            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(ScrapbookYellow).border(3.dp, ScrapbookBorder, CircleShape).clickable { onPlay() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(30.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Album of the Day Card ────────────────────────────────────────────────────

@Composable
fun AlbumOfTheDayCard(album: Album, onPlay: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "aotdNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "aotdNeonAlpha")
    val crownScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.12f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "crownScale")
    val eraColor = eraAccentColor(album.era)

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(14.dp).background(Color(0xFFFFD700).copy(alpha = neonAlpha * 0.25f), RoundedCornerShape(16.dp)))
        ScrapbookCard(modifier = Modifier.fillMaxWidth().border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFFFFD700).copy(alpha = neonAlpha), Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFFFFD700).copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)), backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700).copy(alpha = 0.2f), Color(0xFFFFD700).copy(alpha = 0.05f), Color(0xFFFFD700).copy(alpha = 0.2f))).let { it }).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.scale(crownScale)) { Text("👑", fontSize = 22.sp) }
                        Column {
                            Text("ALBUM OF THE DAY", fontFamily = BangersFontFamily, color = Color(0xFFFFD700), fontSize = 16.sp, letterSpacing = 1.sp)
                            Text("Today's retro pick", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (album.era.isNotBlank() && album.era != "OTHER") { Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraColor.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(album.era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) } }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFFFFD700).copy(alpha = neonAlpha), Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFFFFD700).copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))) {
                        when {
                            album.coverImageResId != null -> Image(painter = painterResource(id = album.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            album.coverImageUrl != null -> AsyncImage(model = album.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("♪", fontSize = 32.sp) }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                        Text(album.artist, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        album.year?.let { Text("$it", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.35f), fontSize = 11.sp) }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            album.reactions.entries.take(3).forEach { (emoji, count) ->
                                Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text(emoji, fontSize = 11.sp); Text("$count", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp) }
                            }
                        }
                    }
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFD700)).border(2.dp, ScrapbookDark, CircleShape).clickable { onPlay() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(26.dp)) }
                }
            }
        }
    }
}

// ─── Community Favorites Row ──────────────────────────────────────────────────

@Composable
fun CommunityFavoritesRow(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    val sorted = remember(albums) { albums.sortedByDescending { it.playCount }.take(5) }
    val neonT = rememberInfiniteTransition(label = "favNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "favNeonAlpha")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("⭐ MOST PLAYED", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        sorted.forEachIndexed { index, album ->
            var pressed by remember { mutableStateOf(false) }
            val rowScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "favRow_$index")
            val eraColor = eraAccentColor(album.era)
            val medalColor = when (index) { 0 -> Color(0xFFFFD700); 1 -> Color(0xFFC0C0C0); 2 -> Color(0xFFCD7F32); else -> ScrapbookBorder }
            Box(modifier = Modifier.fillMaxWidth().scale(rowScale).padding(vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookCardWhite).border(1.dp, eraColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).clickable { pressed = true; onAlbumClick(album) }.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(medalColor.copy(alpha = 0.15f)).border(1.5.dp, medalColor, CircleShape), contentAlignment = Alignment.Center) { Text("${index + 1}", fontFamily = BangersFontFamily, color = medalColor, fontSize = 13.sp) }
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper)) {
                        when {
                            album.coverImageResId != null -> Image(painter = painterResource(id = album.coverImageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            album.coverImageUrl != null -> AsyncImage(model = album.coverImageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("♪", fontSize = 18.sp) }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, eraColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(album.era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 9.sp) }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(11.dp)); Text("${album.playCount}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp) }
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
        }
    }
}

// ─── Album List Item ──────────────────────────────────────────────────────────

@Composable
fun AlbumListItem(album: Album, onAlbumClick: (Album) -> Unit, favoritesViewModel: FavoritesViewModel? = null, isNowPlaying: Boolean = false, modifier: Modifier = Modifier) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(album.id)
    val eraColor = eraAccentColor(album.era)
    val neonT = rememberInfiniteTransition(label = "albumNeon_${album.id}")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "albumNeonAlpha")
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "albumCardScale")

    Box(modifier = modifier.scale(cardScale)) {
        if (isNowPlaying) { Box(modifier = Modifier.matchParentSize().padding(3.dp).blur(10.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.3f), RoundedCornerShape(12.dp))) }
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { pressed = true; onAlbumClick(album) }
                .then(if (isNowPlaying) Modifier.border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp)) else Modifier),
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    if (album.coverImageResId != null) { Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) }
                    else if (album.coverImageUrl != null) { AsyncImage(model = album.coverImageUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) }
                    else { Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper), contentAlignment = Alignment.Center) { Text("♪", color = ScrapbookDark, fontSize = 36.sp) } }
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.Center)) {
                        Box(modifier = Modifier.size(52.dp).blur(10.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), CircleShape))
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(24.dp)) }
                    }
                    if (album.era.isNotBlank() && album.era != "OTHER") {
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark).border(1.dp, eraColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(album.era, fontFamily = BangersFontFamily, color = eraColor, fontSize = 9.sp) }
                    }
                    if (isNowPlaying) { Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("▶ PLAYING", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 8.sp) } }
                    if (favoritesViewModel != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(if (isNowPlaying) PaddingValues(top = 26.dp, end = 6.dp) else PaddingValues(6.dp)).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                            IconButton(onClick = { favoritesViewModel.toggleFavorite(album.toFavoriteItem()) }, modifier = Modifier.size(28.dp)) { Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Text(album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    album.year?.let { Text(it.toString(), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.6f), fontSize = 10.sp) }
                    Spacer(modifier = Modifier.height(8.dp))
                    AlbumReactionBar(albumId = album.id, initialReactions = album.reactions)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(180); pressed = false } }
}

// ─── Archive Album Item ───────────────────────────────────────────────────────

@Composable
fun ArchiveAlbumItem(item: ArchiveItem, onClick: () -> Unit, favoritesViewModel: FavoritesViewModel? = null, modifier: Modifier = Modifier) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(item.id)
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "archiveAlbumScale")
    Box(modifier = modifier.scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onClick() }, backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper)) {
                    AsyncImage(model = item.thumbnailUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                    Box(modifier = Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(24.dp)) }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp) }
                    if (favoritesViewModel != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                            IconButton(onClick = { favoritesViewModel.toggleFavorite(item.toFavoriteItem()) }, modifier = Modifier.size(28.dp)) { Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(item.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    item.creator?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    item.year?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.6f), fontSize = 10.sp) }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(180); pressed = false } }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, color: Color) {
    val glowT = rememberInfiniteTransition(label = "sectionGlow_$title")
    val glowAlpha by glowT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "sectionGlowAlpha")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = glowAlpha)))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Divider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f), thickness = 2.dp)
    }
}

// ─── Album Player Screen ──────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AlbumPlayerScreen(album: Album? = null, archiveItem: ArchiveItem? = null, onClose: () -> Unit, onNowPlayingUpdate: (NowPlayingState) -> Unit = {}) {
    val context = LocalContext.current
    val title = album?.title ?: archiveItem?.title ?: ""
    val artist = album?.artist ?: archiveItem?.creator ?: ""
    val coverResId = album?.coverImageResId
    val coverUrl = album?.coverImageUrl ?: archiveItem?.thumbnailUrl
    val url = album?.webPlaybackUrl ?: archiveItem?.webUrl ?: ""
    val year = album?.year?.toString() ?: archiveItem?.year ?: ""
    val era = album?.era ?: "OTHER"
    val eraColor = eraAccentColor(era)

    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(url) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val neonT = rememberInfiniteTransition(label = "playerNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "playerNeonAlpha")

    LaunchedEffect(title) { onNowPlayingUpdate(NowPlayingState(title = title, artist = artist, coverResId = coverResId, coverUrl = coverUrl, isPlaying = true, albumId = album?.id ?: "")) }

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ScrapbookDark) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (artist.isNotBlank()) Text(artist, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { webViewRef?.reload() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)); try { context.startActivity(intent) } catch (e: Exception) { } }) { Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open", tint = ScrapbookDark, modifier = Modifier.size(20.dp)) }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark)) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    when {
                        coverResId != null -> Image(painter = painterResource(id = coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.3f)
                        coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.3f)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(ScrapbookDark.copy(alpha = 0.95f), ScrapbookDark.copy(alpha = 0.7f)))))
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper).border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))) {
                            when {
                                coverResId != null -> Image(painter = painterResource(id = coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("♪", color = ScrapbookDark, fontSize = 32.sp) }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NOW PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                            if (artist.isNotBlank()) Text(artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            if (year.isNotBlank()) Text(year, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("🎵 INTERNET ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp) }
                                if (era.isNotBlank() && era != "OTHER") { Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(eraColor.copy(alpha = 0.8f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text(era, fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) } }
                            }
                        }
                    }
                }
            }
            if (isLoading) { Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).padding(8.dp), contentAlignment = Alignment.Center) { ThreeDotsAnimation(color = ScrapbookYellow, dotSize = 7.dp) } }
            HorizontalDivider(color = ScrapbookYellow.copy(alpha = neonAlpha * 0.3f))
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.apply { javaScriptEnabled = true; domStorageEnabled = true; loadWithOverviewMode = true; useWideViewPort = true; builtInZoomControls = true; displayZoomControls = false; setSupportZoom(true); mediaPlaybackRequiresUserGesture = false }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) { isLoading = false; url?.let { currentUrl = it } }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) { isLoading = true }
                        }
                        webChromeClient = WebChromeClient()
                        if (url.isNotBlank()) loadUrl(url)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

// ─── Albums Screen ────────────────────────────────────────────────────────────

@Composable
fun AlbumsScreen(modifier: Modifier = Modifier, contentViewModel: ContentViewModel = viewModel(), favoritesViewModel: FavoritesViewModel? = null, authViewModel: AuthViewModel = viewModel()) {
    val focusManager = LocalFocusManager.current
    val albumsState by contentViewModel.albumsState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val firebaseProfile by authViewModel.userProfile.collectAsState()

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }
    var playerVisible by remember { mutableStateOf(false) }
    var nowPlaying by remember { mutableStateOf<NowPlayingState?>(null) }
    var selectedEra by remember { mutableStateOf("ALL") }
    var selectedVibe by remember { mutableStateOf("ALL") }
    var featuredCommunityAlbum by remember { mutableStateOf(sampleAlbums.random()) }
    var featuredArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }

    val albumOfTheDay = remember { getAlbumOfTheDay() }

    val neonT = rememberInfiniteTransition(label = "albumsNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "albumsNeonAlpha")

    LaunchedEffect(albumsState) {
        if (albumsState is ContentState.Success) {
            val items = (albumsState as ContentState.Success).items
            if (items.isNotEmpty()) featuredArchiveItem = items.random()
        }
    }

    LaunchedEffect(searchQuery) { kotlinx.coroutines.delay(600); if (searchQuery != lastSearched) { lastSearched = searchQuery; contentViewModel.fetchAlbums(searchQuery) } }

    val filteredCommunityAlbums = remember(searchQuery, selectedEra, selectedVibe) {
        sampleAlbums.filter { album ->
            val matchesSearch = searchQuery.isBlank() || album.title.contains(searchQuery, ignoreCase = true) || album.artist.contains(searchQuery, ignoreCase = true)
            val matchesEra = selectedEra == "ALL" || album.era == selectedEra
            val matchesVibe = selectedVibe == "ALL" || album.vibe == selectedVibe
            matchesSearch && matchesEra && matchesVibe
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {



            // ✅ Now playing bar
            AnimatedVisibility(visible = nowPlaying != null, enter = expandVertically(tween(300)), exit = shrinkVertically(tween(300))) {
                nowPlaying?.let { state -> NowPlayingBar(state = state, onClick = { playerVisible = true }) }
            }

            // ✅ Search bar
            AnimatedVisibility(visible = searchVisible, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search albums or artists...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) }, leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) }, trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = ""; contentViewModel.fetchAlbums() }) { Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp)) } } }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }), textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 8.dp))
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 80.dp), modifier = Modifier.fillMaxSize()) {

                // ─── Page Hero ────────────────────────────────────────────────
                item {
                    RetroHubPageHero(
                        config = albumsHeroConfig,
                        onCtaClick = { /* already on albums */ }
                    )
                }
                item {
                    RetroHubPageTicker(config = albumsHeroConfig)
                }

                // ✅ Era timeline — now inside LazyColumn so it scrolls
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    EraTimeline(selectedEra = selectedEra, onEraSelected = { selectedEra = it })
                }

                // ✅ Vibe selector
                item { AlbumVibeSelector(selectedVibe = selectedVibe, onVibeSelected = { selectedVibe = it }, neonAlpha = neonAlpha) }

                // ✅ Album of the Day
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AlbumOfTheDayCard(album = albumOfTheDay, onPlay = { selectedAlbum = albumOfTheDay; selectedArchiveItem = null; playerVisible = true })
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Featured Today
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("⭐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("FEATURED TODAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        var shufflePressed by remember { mutableStateOf(false) }
                        val shuffleScale by animateFloatAsState(targetValue = if (shufflePressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shuffleFeat")
                        Box(modifier = Modifier.scale(shuffleScale).clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.7f), RoundedCornerShape(6.dp)).clickable { shufflePressed = true; featuredCommunityAlbum = sampleAlbums.random(); val items = (albumsState as? ContentState.Success)?.items; if (!items.isNullOrEmpty()) featuredArchiveItem = items.random() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Filled.Refresh, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(14.dp)); Text("SHUFFLE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 12.sp) }
                        }
                        LaunchedEffect(shufflePressed) { if (shufflePressed) { kotlinx.coroutines.delay(150); shufflePressed = false } }
                    }
                    FeaturedAlbumHeroCard(title = featuredCommunityAlbum.title, artist = featuredCommunityAlbum.artist, coverResId = featuredCommunityAlbum.coverImageResId, coverUrl = featuredCommunityAlbum.coverImageUrl, year = featuredCommunityAlbum.year?.toString() ?: "", badge = "🎮 COMMUNITY PICK", era = featuredCommunityAlbum.era, onPlay = { selectedAlbum = featuredCommunityAlbum; selectedArchiveItem = null; playerVisible = true })
                    featuredArchiveItem?.let { archiveItem ->
                        Spacer(modifier = Modifier.height(4.dp))
                        FeaturedAlbumHeroCard(title = archiveItem.title, artist = archiveItem.creator ?: "Internet Archive", coverUrl = archiveItem.thumbnailUrl, year = archiveItem.year ?: "", badge = "📦 ARCHIVE PICK", onPlay = { selectedArchiveItem = archiveItem; selectedAlbum = null; playerVisible = true })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Community Listening Room
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    CommunityListeningRoom(albums = sampleAlbums, onAlbumClick = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true })
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Memory Jukebox
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    MemoryJukebox(albums = sampleAlbums, onAlbumClick = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true })
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Most Played
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    CommunityFavoritesRow(albums = sampleAlbums, onAlbumClick = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true })
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Genre Radar
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    AlbumGenreRadar(albums = sampleAlbums)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Community albums header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🎮", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMMUNITY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("${filteredCommunityAlbums.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp) }
                    }
                }

                if (filteredCommunityAlbums.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎵", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No albums match your filters", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { selectedEra = "ALL"; selectedVibe = "ALL" }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("CLEAR FILTERS", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp) }
                            }
                        }
                    }
                } else {
                    items(filteredCommunityAlbums.chunked(2), key = { chunk -> "community_${chunk.first().id}" }) { chunk ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEach { album -> AlbumListItem(album = album, onAlbumClick = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true }, favoritesViewModel = favoritesViewModel, isNowPlaying = nowPlaying?.albumId == album.id, modifier = Modifier.weight(1f)) }
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ✅ Archive header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📦", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INTERNET ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                    }
                }

                when (val state = albumsState) {
                    is ContentState.Loading -> { item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { ThreeDotsAnimation(); Text("Loading from archive...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp) } } } }
                    is ContentState.Error -> { item { Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(state.message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 13.sp, textAlign = TextAlign.Center); Spacer(modifier = Modifier.height(12.dp)); Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { contentViewModel.fetchAlbums() }.padding(horizontal = 24.dp, vertical = 10.dp)) { Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp) } } } }
                    is ContentState.Success -> { items(state.items.chunked(2), key = { chunk -> "archive_${chunk.first().id}" }) { chunk -> Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { chunk.forEach { item -> ArchiveAlbumItem(item = item, onClick = { selectedArchiveItem = item; selectedAlbum = null; playerVisible = true }, favoritesViewModel = favoritesViewModel, modifier = Modifier.weight(1f)) }; if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f)) } } }
                    else -> { }
                }
            }
        }

        // ✅ Player overlay
        AnimatedVisibility(visible = playerVisible, enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)), exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)), modifier = Modifier.fillMaxSize()) {
            AlbumPlayerScreen(album = selectedAlbum, archiveItem = selectedArchiveItem, onClose = { playerVisible = false }, onNowPlayingUpdate = { state -> nowPlaying = state })
        }
    }
}


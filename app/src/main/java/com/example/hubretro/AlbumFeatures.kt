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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.*
// ─── Data Classes ─────────────────────────────────────────────────────────────

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val year: Int? = null,
    val webPlaybackUrl: String? = null,
    val era: String = "OTHER"
)

data class RelaxVideo(
    val id: String,
    val title: String,
    val youtubeId: String,
    val mood: String = "Chill"
)

data class NowPlayingState(
    val title: String = "",
    val artist: String = "",
    val coverResId: Int? = null,
    val coverUrl: String? = null,
    val isPlaying: Boolean = false
)

// ─── Sample Data ──────────────────────────────────────────────────────────────

val sampleAlbums = listOf(
    Album(id = "album1", title = "Gunbound", artist = "Synth Rider", coverImageResId = R.drawable.ostcover1, year = 1984, webPlaybackUrl = "https://archive.org/details/gunbound-soundtrack", era = "NES"),
    Album(id = "album2", title = "Pokemon Diamond & Pearl", artist = "Grid Runner", coverImageResId = R.drawable.ostcover2, year = 1988, webPlaybackUrl = "https://archive.org/details/pkmn-dppt-soundtrack", era = "NDS"),
    Album(id = "album3", title = "The Legend of Zelda: The Wind Waker", artist = "Chrome Catalyst", coverImageResId = R.drawable.ostcover3, year = 1991, webPlaybackUrl = "https://archive.org/details/the-legend-of-zelda-the-wind-waker-ost", era = "GCN"),
    Album(id = "album4", title = "Undertale", artist = "Vector Voyager", coverImageResId = R.drawable.ostcover4, year = 1986, webPlaybackUrl = "https://archive.org/details/undertaleost_202004", era = "PC"),
    Album(id = "album5", title = "Lego Harry Potter Years 1-4", artist = "Bit Shifter", coverImageResId = R.drawable.ostcover5, year = 1982, webPlaybackUrl = "https://archive.org/details/lego-harry-potter-years-1-4", era = "PS2"),
    Album(id = "album6", title = "Final Fantasy VII", artist = "Analog Hero", coverImageResId = R.drawable.ostcover6, year = 1987, webPlaybackUrl = "https://archive.org/details/final_fantasy_vii_soundtrack", era = "PS1"),
    Album(id = "album7", title = "The Sims", artist = "Digital Nomad", coverImageResId = R.drawable.ostcover7, year = 1985, webPlaybackUrl = "https://archive.org/details/simsmusic", era = "PC")
)

val albumEraFilters = listOf("ALL", "NES", "SNES", "PS1", "PS2", "N64", "GCN", "GBA", "NDS", "PC", "OTHER")

val relaxPlaylist = listOf(
    RelaxVideo(id = "r1", title = "Cozy Retro Gaming Lounge", youtubeId = "xjVDj2U_Y9M", mood = "Cozy"),
    RelaxVideo(id = "r2", title = "Late Night BGM Session", youtubeId = "s9A0xloTEA0", mood = "Calm"),
    RelaxVideo(id = "r3", title = "Retro Chill Beats", youtubeId = "NeSMbZmROFA", mood = "Relaxing"),
    RelaxVideo(id = "r4", title = "Pixel Café Ambience", youtubeId = "rTO2MN7jrWk", mood = "Cozy"),
    RelaxVideo(id = "r5", title = "Evening Game Room Vibes", youtubeId = "hkERj1yxN6c", mood = "Evening")
)

// ─── Now Playing Bar ──────────────────────────────────────────────────────────

@Composable
fun NowPlayingBar(state: NowPlayingState, onClick: () -> Unit) {
    val neonT = rememberInfiniteTransition(label = "nowPlayingNeon")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "nowPlayingNeonAlpha"
    )
    val dotAlpha by neonT.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        // ✅ Neon glow behind bar
        Box(modifier = Modifier.matchParentSize().blur(8.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.15f)))
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(ScrapbookDark)
                .border(width = 1.dp, brush = Brush.horizontalGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.6f), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha * 0.6f))), shape = RoundedCornerShape(0.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookPaper)
                        .border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state.coverResId != null -> Image(painter = painterResource(id = state.coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        state.coverUrl != null -> AsyncImage(model = state.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else -> Text("♪", color = ScrapbookYellow, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.artist.isNotBlank()) Text(state.artist, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // ✅ Pulsing dot
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = dotAlpha)))
                    Text("PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── Featured Album Hero Card ─────────────────────────────────────────────────

@Composable
fun FeaturedAlbumHeroCard(
    title: String,
    artist: String,
    coverResId: Int? = null,
    coverUrl: String? = null,
    year: String = "",
    badge: String = "🎵 FEATURED",
    onPlay: () -> Unit
) {
    val neonT = rememberInfiniteTransition(label = "featuredNeon")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse),
        label = "featuredNeonAlpha"
    )
    val btnScale by neonT.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse),
        label = "featuredBtnScale"
    )

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        // ✅ Neon glow behind card
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(16.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.15f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)),
            backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                when {
                    coverResId != null -> Image(painter = painterResource(id = coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.4f)
                    coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.4f)
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.98f)))))

                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    // Badge
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(badge, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Cover art with neon border
                        Box(
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper)
                                .border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.4f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))
                        ) {
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

                        // ✅ Play button with neon glow + pulse
                        Box(modifier = Modifier.scale(btnScale)) {
                            Box(modifier = Modifier.size(60.dp).blur(12.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.5f), CircleShape))
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(ScrapbookYellow)
                                    .border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookDark, ScrapbookDark)), shape = CircleShape)
                                    .clickable { onPlay() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Relaxation Section ───────────────────────────────────────────────────────

@Composable
fun RelaxationSection(
    currentUserId: String,
    username: String,
    savedSpotIds: Set<String>,
    onAddToSpot: (RelaxVideo) -> Unit,
    onRemoveFromSpot: (RelaxVideo) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isExpanded by remember { mutableStateOf(false) }
    var currentVideoIndex by remember { mutableStateOf(0) }
    var isShuffled by remember { mutableStateOf(false) }
    var shuffleSeed by remember { mutableStateOf(0) }

    val displayPlaylist = remember(isShuffled, shuffleSeed) {
        if (isShuffled) relaxPlaylist.shuffled() else relaxPlaylist
    }
    val currentVideo = displayPlaylist[currentVideoIndex.coerceIn(0, displayPlaylist.size - 1)]

    val cabinDark  = Color(0xFF1C1008)
    val cabinBrown = Color(0xFF3D2208)
    val cabinWood  = Color(0xFF6B3A10)
    val cabinAmber = Color(0xFFD47C1A)
    val cabinGold  = Color(0xFFFFB347)
    val cabinLight = Color(0xFFFFE0A0)
    val cabinFire  = Color(0xFFFF6B1A)
    val cabinGlow  = Color(0xFFFF9500)

    // ✅ Fireplace glow animation
    val fireT = rememberInfiniteTransition(label = "fireGlow")
    val fireAlpha by fireT.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "fireAlpha"
    )
    val fireScale by fireT.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "fireScale"
    )

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(20.dp)).background(cabinBrown.copy(alpha = 0.6f)).height(if (isExpanded) 700.dp else 100.dp))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(colors = listOf(cabinBrown, cabinDark)))
                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(cabinFire.copy(alpha = fireAlpha), cabinAmber, cabinFire.copy(alpha = fireAlpha))), shape = RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ✅ Header
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Brush.horizontalGradient(colors = listOf(cabinFire.copy(alpha = 0.15f), cabinAmber.copy(alpha = 0.08f), cabinFire.copy(alpha = 0.15f))))
                        .clickable { isExpanded = !isExpanded }.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        // ✅ Fireplace icon with glow
                        Box(modifier = Modifier.scale(fireScale)) {
                            Box(modifier = Modifier.size(64.dp).blur(12.dp).background(cabinFire.copy(alpha = fireAlpha * 0.4f), RoundedCornerShape(14.dp)))
                            Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(Brush.radialGradient(colors = listOf(cabinFire.copy(alpha = 0.4f), cabinDark))).border(2.dp, cabinAmber, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎮", fontSize = 22.sp)
                                    Text("BGM", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 9.sp, letterSpacing = 2.sp)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("CHECKPOINT LOUNGE", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 22.sp, letterSpacing = 0.5.sp)
                            Text("🔥 Sit back. You've earned this save point.", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = cabinLight.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (!isExpanded) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(relaxPlaylist) { video ->
                                        val isActive = video.id == currentVideo.id
                                        var pressed by remember { mutableStateOf(false) }
                                        val chipScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "moodChip")
                                        Box(modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(6.dp)).background(if (isActive) cabinAmber else cabinWood.copy(alpha = 0.6f)).border(1.dp, if (isActive) cabinGold else cabinAmber.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).clickable { pressed = true }.padding(horizontal = 8.dp, vertical = 3.dp)) {
                                            Text(if (isActive) "▶ ${video.mood}" else video.mood, fontFamily = BangersFontFamily, color = if (isActive) cabinDark else cabinLight.copy(alpha = 0.6f), fontSize = 9.sp)
                                        }
                                        LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                                    }
                                }
                            }
                        }

                        // ✅ Expand button with fire glow
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (isExpanded) Brush.verticalGradient(listOf(cabinFire, cabinAmber)) else Brush.verticalGradient(listOf(cabinWood, cabinBrown)))
                                .border(2.dp, cabinAmber, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = if (isExpanded) Color.White else cabinGold, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(400, easing = LinearOutSlowInEasing)), exit = shrinkVertically(tween(300))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(color = cabinAmber.copy(alpha = 0.3f))

                        // Now playing card
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(colors = listOf(cabinDark, cabinBrown, cabinDark))).border(2.dp, cabinAmber, RoundedCornerShape(16.dp)).padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(cabinWood, cabinDark))).border(3.dp, cabinAmber, CircleShape), contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(cabinGold, cabinAmber))))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(cabinFire).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text("🔥 NOW PLAYING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 1.sp)
                                        }
                                        Text("${currentVideoIndex + 1}/${displayPlaylist.size}", fontFamily = BangersFontFamily, color = cabinAmber.copy(alpha = 0.6f), fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(currentVideo.title, fontFamily = BangersFontFamily, color = cabinGold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.5.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎮", fontSize = 10.sp)
                                        Text("✨ ${currentVideo.mood}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = cabinLight.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                                // Shuffle button
                                var shufflePressed by remember { mutableStateOf(false) }
                                val shuffleScale by animateFloatAsState(targetValue = if (shufflePressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shuffleScale")
                                Box(
                                    modifier = Modifier.scale(shuffleScale).size(42.dp).clip(RoundedCornerShape(10.dp))
                                        .background(if (isShuffled) Brush.verticalGradient(listOf(cabinFire, cabinAmber)) else Brush.verticalGradient(listOf(cabinWood, cabinBrown)))
                                        .border(2.dp, cabinAmber, RoundedCornerShape(10.dp))
                                        .clickable { shufflePressed = true; isShuffled = !isShuffled; shuffleSeed++; currentVideoIndex = 0 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (isShuffled) Color.White else cabinGold, modifier = Modifier.size(20.dp))
                                }
                                LaunchedEffect(shufflePressed) { if (shufflePressed) { kotlinx.coroutines.delay(150); shufflePressed = false } }
                            }
                        }

                        // ✅ YouTube player with fire border
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cabinDark).border(width = 3.dp, brush = Brush.horizontalGradient(colors = listOf(cabinFire.copy(alpha = fireAlpha), cabinGold, cabinFire.copy(alpha = fireAlpha))), shape = RoundedCornerShape(16.dp)).padding(3.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
                                YoutubePlayerCard(youtubeVideoId = currentVideo.youtubeId, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), lifecycleOwner = lifecycleOwner)
                            }
                        }

                        // ✅ PREV / NEXT controls with press scale
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            var prevPressed by remember { mutableStateOf(false) }
                            var nextPressed by remember { mutableStateOf(false) }
                            val prevScale by animateFloatAsState(targetValue = if (prevPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "prevScale")
                            val nextScale by animateFloatAsState(targetValue = if (nextPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "nextScale")

                            Box(modifier = Modifier.weight(1f).scale(prevScale).clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(colors = listOf(cabinWood, cabinBrown))).border(2.dp, cabinAmber, RoundedCornerShape(14.dp)).clickable { prevPressed = true; currentVideoIndex = (currentVideoIndex - 1 + displayPlaylist.size) % displayPlaylist.size }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = cabinGold, modifier = Modifier.size(22.dp)); Text("PREV", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 18.sp, letterSpacing = 1.sp) }
                            }
                            Box(modifier = Modifier.weight(1f).scale(nextScale).clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(colors = listOf(cabinFire, cabinAmber))).border(2.dp, cabinGold, RoundedCornerShape(14.dp)).clickable { nextPressed = true; currentVideoIndex = (currentVideoIndex + 1) % displayPlaylist.size }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("NEXT", fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp); Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(22.dp)) }
                            }
                            LaunchedEffect(prevPressed) { if (prevPressed) { kotlinx.coroutines.delay(150); prevPressed = false } }
                            LaunchedEffect(nextPressed) { if (nextPressed) { kotlinx.coroutines.delay(150); nextPressed = false } }
                        }

                        // ✅ Add to Chill Zone with press scale
                        if (currentUserId.isNotBlank()) {
                            val isSaved = savedSpotIds.contains(currentVideo.id)
                            var chillPressed by remember { mutableStateOf(false) }
                            val chillScale by animateFloatAsState(targetValue = if (chillPressed) 0.96f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chillScale")
                            Box(
                                modifier = Modifier.scale(chillScale).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(if (isSaved) Brush.horizontalGradient(colors = listOf(cabinWood, cabinBrown)) else Brush.horizontalGradient(colors = listOf(cabinFire, cabinGlow, cabinAmber)))
                                    .border(width = 3.dp, brush = if (isSaved) Brush.linearGradient(colors = listOf(cabinAmber.copy(alpha = 0.5f), cabinAmber.copy(alpha = 0.5f))) else Brush.linearGradient(colors = listOf(cabinGold, cabinGold)), shape = RoundedCornerShape(14.dp))
                                    .clickable { chillPressed = true; if (isSaved) onRemoveFromSpot(currentVideo) else onAddToSpot(currentVideo) }
                                    .padding(vertical = 15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(if (isSaved) "☕" else "🎮", fontSize = 20.sp)
                                    Text(if (isSaved) "SAVED TO CHILL ZONE ✓" else "ADD TO MY CHILL ZONE", fontFamily = BangersFontFamily, color = if (isSaved) cabinAmber else Color.White, fontSize = 18.sp, letterSpacing = 0.5.sp)
                                }
                            }
                            LaunchedEffect(chillPressed) { if (chillPressed) { kotlinx.coroutines.delay(150); chillPressed = false } }
                        }

                        // Playlist label
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🎵", fontSize = 16.sp)
                                Text("PLAYLIST", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 20.sp, letterSpacing = 1.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isShuffled) cabinFire.copy(alpha = 0.3f) else cabinWood.copy(alpha = 0.5f)).border(1.dp, cabinAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(if (isShuffled) "🔀 SHUFFLED" else "📋 IN ORDER", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 11.sp)
                            }
                        }

                        // ✅ Playlist cards with press scale
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                            items(displayPlaylist, key = { it.id }) { video ->
                                val isActive = video.id == currentVideo.id
                                val isSavedItem = savedSpotIds.contains(video.id)
                                val idx = displayPlaylist.indexOf(video)
                                var cardPressed by remember { mutableStateOf(false) }
                                val cardScale by animateFloatAsState(targetValue = if (cardPressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "playlistCard")
                                Box(
                                    modifier = Modifier.width(150.dp).scale(cardScale).clip(RoundedCornerShape(14.dp))
                                        .background(if (isActive) Brush.verticalGradient(colors = listOf(cabinFire, cabinBrown)) else Brush.verticalGradient(colors = listOf(cabinBrown, cabinDark)))
                                        .border(if (isActive) 2.dp else 1.dp, if (isActive) cabinGold else cabinAmber.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                        .clickable { cardPressed = true; currentVideoIndex = idx }.padding(12.dp)
                                ) {
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (isActive) Brush.radialGradient(colors = listOf(cabinGold, cabinFire)) else Brush.radialGradient(colors = listOf(cabinWood, cabinDark))).border(1.dp, if (isActive) cabinGold else cabinAmber.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                                Text(if (isActive) "▶" else "${idx + 1}", fontFamily = BangersFontFamily, color = if (isActive) cabinDark else cabinAmber, fontSize = 11.sp)
                                            }
                                            if (isSavedItem) { Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(cabinAmber.copy(alpha = 0.2f)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("☕", fontSize = 10.sp) } }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(video.title, fontFamily = BangersFontFamily, color = if (isActive) Color.White else cabinLight.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp, letterSpacing = 0.3.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(if (isActive) cabinGold.copy(alpha = 0.25f) else cabinWood.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                            Text("✨ ${video.mood}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (isActive) cabinGold else cabinAmber.copy(alpha = 0.7f), fontSize = 10.sp)
                                        }
                                    }
                                }
                                LaunchedEffect(cardPressed) { if (cardPressed) { kotlinx.coroutines.delay(150); cardPressed = false } }
                            }
                        }

                        // Footer
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Brush.horizontalGradient(colors = listOf(cabinFire.copy(alpha = 0.1f), cabinAmber.copy(alpha = 0.05f), cabinFire.copy(alpha = 0.1f)))).border(1.dp, cabinAmber.copy(alpha = 0.25f), RoundedCornerShape(12.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                            Text("🔥  Progress saved. Rest mode: ON. You deserve this.", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = cabinAmber.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Chill Zone Profile Section ───────────────────────────────────────────────

@Composable
fun ChillZoneProfileSection(username: String, savedVideos: List<RelaxVideo>) {
    if (savedVideos.isEmpty()) return
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedIndex by remember { mutableStateOf(0) }
    val selectedVideo = savedVideos[selectedIndex.coerceIn(0, savedVideos.size - 1)]

    val cabinDark  = Color(0xFF1C1008); val cabinBrown = Color(0xFF3D2208)
    val cabinWood  = Color(0xFF6B3A10); val cabinAmber = Color(0xFFD47C1A)
    val cabinGold  = Color(0xFFFFB347); val cabinLight = Color(0xFFFFE0A0)
    val cabinFire  = Color(0xFFFF6B1A)

    val fireT = rememberInfiniteTransition(label = "chillFire")
    val fireAlpha by fireT.animateFloat(initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "chillFireAlpha")

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(20.dp)).background(cabinBrown.copy(alpha = 0.5f)).height(420.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(colors = listOf(cabinBrown, cabinDark))).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(cabinFire.copy(alpha = fireAlpha), cabinAmber, cabinFire.copy(alpha = fireAlpha))), shape = RoundedCornerShape(20.dp))) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Brush.horizontalGradient(colors = listOf(cabinFire.copy(alpha = 0.2f), cabinAmber.copy(alpha = 0.1f), cabinFire.copy(alpha = 0.2f)))).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(Brush.radialGradient(colors = listOf(cabinFire.copy(alpha = 0.4f), cabinDark))).border(2.dp, cabinAmber, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("☕", fontSize = 22.sp); Text("ZONE", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 9.sp, letterSpacing = 1.sp) }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("☕ ${username}'s Chill Zone", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp)
                            Text("${savedVideos.size} vibe${if (savedVideos.size != 1) "s" else ""} saved to this lounge", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = cabinLight.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(colors = listOf(cabinDark, cabinBrown, cabinDark))).border(2.dp, cabinAmber, RoundedCornerShape(14.dp)).padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(cabinWood, cabinDark))).border(2.dp, cabinAmber, CircleShape), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(cabinGold, cabinAmber))))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(cabinFire.copy(alpha = 0.3f)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("🔥 VIBING TO", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 9.sp, letterSpacing = 2.sp) }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(selectedVideo.title, fontFamily = BangersFontFamily, color = cabinGold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, letterSpacing = 0.3.sp)
                                Text("✨ ${selectedVideo.mood}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = cabinLight.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cabinDark).border(width = 3.dp, brush = Brush.horizontalGradient(colors = listOf(cabinFire.copy(alpha = fireAlpha), cabinGold, cabinFire.copy(alpha = fireAlpha))), shape = RoundedCornerShape(16.dp)).padding(3.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))) {
                            YoutubePlayerCard(youtubeVideoId = selectedVideo.youtubeId, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), lifecycleOwner = lifecycleOwner)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("🎵", fontSize = 16.sp); Text("SAVED VIBES", fontFamily = BangersFontFamily, color = cabinGold, fontSize = 20.sp, letterSpacing = 1.sp) }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(savedVideos, key = { it.id }) { video ->
                            val isActive = video.id == selectedVideo.id
                            var pressed by remember { mutableStateOf(false) }
                            val cardScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "savedVibe")
                            Box(modifier = Modifier.width(140.dp).scale(cardScale).clip(RoundedCornerShape(14.dp)).background(if (isActive) Brush.verticalGradient(listOf(cabinFire, cabinBrown)) else Brush.verticalGradient(listOf(cabinBrown, cabinDark))).border(if (isActive) 2.dp else 1.dp, if (isActive) cabinGold else cabinAmber.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).clickable { pressed = true; selectedIndex = savedVideos.indexOf(video) }.padding(12.dp)) {
                                Column {
                                    Text(if (isActive) "▶ NOW" else "☕", fontFamily = BangersFontFamily, color = if (isActive) cabinGold else cabinAmber.copy(alpha = 0.6f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(video.title, fontFamily = BangersFontFamily, color = if (isActive) Color.White else cabinLight.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(if (isActive) cabinGold.copy(alpha = 0.2f) else cabinWood.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                        Text(video.mood, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (isActive) cabinGold else cabinAmber.copy(alpha = 0.7f), fontSize = 10.sp)
                                    }
                                }
                            }
                            LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(cabinFire.copy(alpha = 0.08f)).border(1.dp, cabinAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("🔥 This is ${username.split(" ").first()}'s personal save point.", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = cabinAmber.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ─── Album List Item ──────────────────────────────────────────────────────────

@Composable
fun AlbumListItem(album: Album, onAlbumClick: (Album) -> Unit, favoritesViewModel: FavoritesViewModel? = null, modifier: Modifier = Modifier) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(album.id)

    // ✅ Neon glow + press scale
    val neonT = rememberInfiniteTransition(label = "albumNeon_${album.id}")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "albumNeonAlpha")
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "albumCardScale")

    Box(modifier = modifier.scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onAlbumClick(album) }, backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    if (album.coverImageResId != null) {
                        Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                    } else if (album.coverImageUrl != null) {
                        AsyncImage(model = album.coverImageUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                    } else {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper), contentAlignment = Alignment.Center) { Text("♪", color = ScrapbookDark, fontSize = 36.sp) }
                    }
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

                    // ✅ Play button with neon glow
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.Center)) {
                        Box(modifier = Modifier.size(52.dp).blur(10.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), CircleShape))
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(ScrapbookYellow).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookDark, ScrapbookDark)), shape = CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(24.dp))
                        }
                    }

                    if (album.era.isNotBlank() && album.era != "OTHER") {
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(album.era, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp)
                        }
                    }
                    if (favoritesViewModel != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                            IconButton(onClick = { favoritesViewModel.toggleFavorite(album.toFavoriteItem()) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Text(album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    album.year?.let { Text(it.toString(), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.6f), fontSize = 10.sp) }
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
                    Box(modifier = Modifier.align(Alignment.Center).size(44.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(24.dp))
                    }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp)
                    }
                    if (favoritesViewModel != null) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                            IconButton(onClick = { favoritesViewModel.toggleFavorite(item.toFavoriteItem()) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                            }
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
fun AlbumPlayerScreen(
    album: Album? = null,
    archiveItem: ArchiveItem? = null,
    onClose: () -> Unit,
    onNowPlayingUpdate: (NowPlayingState) -> Unit = {}
) {
    val context = LocalContext.current
    val title = album?.title ?: archiveItem?.title ?: ""
    val artist = album?.artist ?: archiveItem?.creator ?: ""
    val coverResId = album?.coverImageResId
    val coverUrl = album?.coverImageUrl ?: archiveItem?.thumbnailUrl
    val url = album?.webPlaybackUrl ?: archiveItem?.webUrl ?: ""
    val year = album?.year?.toString() ?: archiveItem?.year ?: ""

    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(url) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // ✅ Neon for player header
    val neonT = rememberInfiniteTransition(label = "playerNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "playerNeonAlpha")

    LaunchedEffect(title) {
        onNowPlayingUpdate(NowPlayingState(title = title, artist = artist, coverResId = coverResId, coverUrl = coverUrl, isPlaying = true))
    }

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ✅ Dark header with neon accent
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).border(BorderStroke(2.dp, ScrapbookYellow.copy(alpha = 0.3f))).padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookYellow.copy(alpha = 0.8f), ScrapbookYellow, ScrapbookYellow.copy(alpha = 0.8f), Color.Transparent))))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (artist.isNotBlank()) Text(artist, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { webViewRef?.reload() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) }
                    IconButton(onClick = { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)); try { context.startActivity(intent) } catch (e: Exception) { } }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open", tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ✅ Cover art panel with neon border
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark)) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    when {
                        coverResId != null -> Image(painter = painterResource(id = coverResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.3f)
                        coverUrl != null -> AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.3f)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(ScrapbookDark.copy(alpha = 0.95f), ScrapbookDark.copy(alpha = 0.7f)))))
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper)
                                .border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp))
                        ) {
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
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("🎵 INTERNET ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // ✅ Loading indicator with 3 dots
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().background(ScrapbookDark).padding(8.dp), contentAlignment = Alignment.Center) {
                    ThreeDotsAnimation(color = ScrapbookYellow, dotSize = 7.dp)
                }
            }
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
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null,
    authViewModel: AuthViewModel = viewModel()
) {
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
    var savedSpotIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var featuredCommunityAlbum by remember { mutableStateOf(sampleAlbums.random()) }
    var featuredArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }

    // ✅ Global neon pulse
    val neonT = rememberInfiniteTransition(label = "albumsNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "albumsNeonAlpha")

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(uid).collection("chill_zone").get().await()
                savedSpotIds = doc.documents.mapNotNull { it.getString("videoId") }.toSet()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(albumsState) {
        if (albumsState is ContentState.Success) {
            val items = (albumsState as ContentState.Success).items
            if (items.isNotEmpty()) featuredArchiveItem = items.random()
        }
    }

    LaunchedEffect(searchQuery) { kotlinx.coroutines.delay(600); if (searchQuery != lastSearched) { lastSearched = searchQuery; contentViewModel.fetchAlbums(searchQuery) } }

    val filteredCommunityAlbums = remember(searchQuery, selectedEra) {
        sampleAlbums.filter { album ->
            val matchesSearch = searchQuery.isBlank() || album.title.contains(searchQuery, ignoreCase = true) || album.artist.contains(searchQuery, ignoreCase = true)
            val matchesEra = selectedEra == "ALL" || album.era == selectedEra
            matchesSearch && matchesEra
        }
    }

    fun saveToChillZone(video: RelaxVideo) {
        val uid = currentUser?.uid ?: return
        savedSpotIds = savedSpotIds + video.id
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("chill_zone").document(video.id).set(mapOf("videoId" to video.id, "title" to video.title, "youtubeId" to video.youtubeId, "mood" to video.mood))
    }

    fun removeFromChillZone(video: RelaxVideo) {
        val uid = currentUser?.uid ?: return
        savedSpotIds = savedSpotIds - video.id
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("chill_zone").document(video.id).delete()
    }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Dark header with neon accent line + shimmer
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ALBUMS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 32.sp, letterSpacing = 2.sp)
                        Text("Retro game soundtracks", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(ScrapbookDark)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { searchVisible = !searchVisible; if (!searchVisible) { searchQuery = ""; focusManager.clearFocus(); contentViewModel.fetchAlbums() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search", tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                    }
                }
            }

            AnimatedVisibility(visible = nowPlaying != null, enter = expandVertically(tween(300)), exit = shrinkVertically(tween(300))) {
                nowPlaying?.let { state -> NowPlayingBar(state = state, onClick = { playerVisible = true }) }
            }

            AnimatedVisibility(visible = searchVisible, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search albums or artists...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = ""; contentViewModel.fetchAlbums() }) { Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp)) } } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ✅ Era chips with neon selected border
            LazyRow(modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(albumEraFilters) { era ->
                    val isSelected = selectedEra == era
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "eraChipScale")
                    Box(
                        modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                            .border(width = if (isSelected) 2.dp else 1.5.dp, brush = if (isSelected) Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = RoundedCornerShape(20.dp))
                            .clickable { pressed = true; selectedEra = era }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(era, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 13.sp)
                    }
                    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.fillMaxSize()) {

                // ✅ Featured Today header with neon glow bar
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
                        Box(
                            modifier = Modifier.scale(shuffleScale).clip(RoundedCornerShape(6.dp))
                                .background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = neonAlpha * 0.7f), RoundedCornerShape(6.dp))
                                .clickable { shufflePressed = true; featuredCommunityAlbum = sampleAlbums.random(); val items = (albumsState as? ContentState.Success)?.items; if (!items.isNullOrEmpty()) featuredArchiveItem = items.random() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(14.dp))
                                Text("SHUFFLE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 12.sp)
                            }
                        }
                        LaunchedEffect(shufflePressed) { if (shufflePressed) { kotlinx.coroutines.delay(150); shufflePressed = false } }
                    }
                    FeaturedAlbumHeroCard(title = featuredCommunityAlbum.title, artist = featuredCommunityAlbum.artist, coverResId = featuredCommunityAlbum.coverImageResId, coverUrl = featuredCommunityAlbum.coverImageUrl, year = featuredCommunityAlbum.year?.toString() ?: "", badge = "🎮 COMMUNITY PICK", onPlay = { selectedAlbum = featuredCommunityAlbum; selectedArchiveItem = null; playerVisible = true })
                    featuredArchiveItem?.let { archiveItem ->
                        Spacer(modifier = Modifier.height(4.dp))
                        FeaturedAlbumHeroCard(title = archiveItem.title, artist = archiveItem.creator ?: "Internet Archive", coverUrl = archiveItem.thumbnailUrl, year = archiveItem.year ?: "", badge = "📦 ARCHIVE PICK", onPlay = { selectedArchiveItem = archiveItem; selectedAlbum = null; playerVisible = true })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    RelaxationSection(currentUserId = currentUser?.uid ?: "", username = firebaseProfile?.username ?: "", savedSpotIds = savedSpotIds, onAddToSpot = { saveToChillZone(it) }, onRemoveFromSpot = { removeFromChillZone(it) })
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }

                // ✅ Community section header with neon glow bar
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🎮", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMMUNITY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("${filteredCommunityAlbums.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
                        }
                    }
                }

                if (filteredCommunityAlbums.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🎵", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (selectedEra == "ALL") "No albums found for \"$searchQuery\"" else "No $selectedEra albums found", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(filteredCommunityAlbums.chunked(2), key = { chunk -> "community_${chunk.first().id}" }) { chunk ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEach { album -> AlbumListItem(album = album, onAlbumClick = { selectedAlbum = it; selectedArchiveItem = null; playerVisible = true }, favoritesViewModel = favoritesViewModel, modifier = Modifier.weight(1f)) }
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ✅ Archive section header with neon glow bar
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
                    is ContentState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ThreeDotsAnimation()
                                    Text("Loading from archive...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    is ContentState.Error -> {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { contentViewModel.fetchAlbums() }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                                    Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    is ContentState.Success -> {
                        items(state.items.chunked(2), key = { chunk -> "archive_${chunk.first().id}" }) { chunk ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                chunk.forEach { item -> ArchiveAlbumItem(item = item, onClick = { selectedArchiveItem = item; selectedAlbum = null; playerVisible = true }, favoritesViewModel = favoritesViewModel, modifier = Modifier.weight(1f)) }
                                if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    else -> { }
                }
            }
        }

        // Player overlay
        AnimatedVisibility(
            visible = playerVisible,
            enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            AlbumPlayerScreen(album = selectedAlbum, archiveItem = selectedArchiveItem, onClose = { playerVisible = false }, onNowPlayingUpdate = { state -> nowPlaying = state })
        }
    }
}
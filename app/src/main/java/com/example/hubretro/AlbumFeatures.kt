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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ScrapbookCard(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = ScrapbookDark,
            cornerRadius = 16.dp,
            shadowOffset = 5.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Background cover art — dimmed
                when {
                    coverResId != null -> Image(
                        painter = painterResource(id = coverResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.4f
                    )
                    coverUrl != null -> AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.4f
                    )
                }

                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ScrapbookDark.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 12.sp
                        )
                    }

                    // Bottom — cover + info + play
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cover art
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ScrapbookPaper)
                                .border(3.dp, ScrapbookYellow, RoundedCornerShape(10.dp))
                        ) {
                            when {
                                coverResId != null -> Image(
                                    painter = painterResource(id = coverResId),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                coverUrl != null -> AsyncImage(
                                    model = coverUrl,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { Text("♪", fontSize = 28.sp, color = Color.White) }
                            }
                        }

                        // Title + artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontFamily = BangersFontFamily,
                                color = Color.White,
                                fontSize = 22.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 26.sp
                            )
                            Text(
                                text = artist,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            if (year.isNotBlank()) {
                                Text(
                                    text = year,
                                    fontFamily = NunitoFontFamily,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Play button
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ScrapbookYellow)
                                .border(3.dp, ScrapbookBorder, CircleShape)
                                .clickable { onPlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = ScrapbookDark,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Now Playing Bar ──────────────────────────────────────────────────────────

@Composable
fun NowPlayingBar(
    state: NowPlayingState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScrapbookDark)
            .border(BorderStroke(1.dp, ScrapbookYellow.copy(alpha = 0.5f)))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover art
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScrapbookPaper)
                    .border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.coverResId != null -> Image(
                        painter = painterResource(id = state.coverResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    state.coverUrl != null -> AsyncImage(
                        model = state.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Text("♪", color = ScrapbookYellow, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontFamily = BangersFontFamily,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.artist.isNotBlank()) {
                    Text(
                        text = state.artist,
                        fontFamily = NunitoFontFamily,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ScrapbookYellow.copy(alpha = alpha))
                )
                Text(
                    text = "PLAYING",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookYellow,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Open player",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
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

    LaunchedEffect(title) {
        onNowPlayingUpdate(
            NowPlayingState(
                title = title,
                artist = artist,
                coverResId = coverResId,
                coverUrl = coverUrl,
                isPlaying = true
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ScrapbookDark)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (artist.isNotBlank()) {
                            Text(text = artist, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        try { context.startActivity(intent) } catch (e: Exception) { }
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Now playing strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    when {
                        coverResId != null -> Image(
                            painter = painterResource(id = coverResId),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.3f
                        )
                        coverUrl != null -> AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.3f
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ScrapbookDark.copy(alpha = 0.95f),
                                        ScrapbookDark.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ScrapbookPaper)
                                .border(3.dp, ScrapbookYellow, RoundedCornerShape(12.dp))
                        ) {
                            when {
                                coverResId != null -> Image(
                                    painter = painterResource(id = coverResId),
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                coverUrl != null -> AsyncImage(
                                    model = coverUrl,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { Text("♪", color = ScrapbookDark, fontSize = 32.sp) }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "NOW PLAYING", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp, letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                            if (artist.isNotBlank()) {
                                Text(text = artist, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            }
                            if (year.isNotBlank()) {
                                Text(text = year, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ScrapbookYellow)
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = "🎵 INTERNET ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ScrapbookYellowDark, trackColor = ScrapbookPaper)
            }

            Divider(color = ScrapbookBorder.copy(alpha = 0.2f))

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
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            mediaPlaybackRequiresUserGesture = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { currentUrl = it }
                            }
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                            }
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

// ─── Album List Item ──────────────────────────────────────────────────────────

@Composable
fun AlbumListItem(
    album: Album,
    onAlbumClick: (Album) -> Unit,
    favoritesViewModel: FavoritesViewModel? = null,
    modifier: Modifier = Modifier
) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(album.id)

    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onAlbumClick(album) },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 4.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    when {
                        album.coverImageResId != null -> Image(
                            painter = painterResource(id = album.coverImageResId),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        )
                        album.coverImageUrl != null -> AsyncImage(
                            model = album.coverImageUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        )
                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(ScrapbookPaper),
                            contentAlignment = Alignment.Center
                        ) { Text("♪", color = ScrapbookDark, fontSize = 36.sp) }
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )

                    // Play button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(24.dp))
                    }

                    // Era badge
                    if (album.era.isNotBlank() && album.era != "OTHER") {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ScrapbookDark)
                                .border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = album.era, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp)
                        }
                    }

                    // Bookmark
                    if (favoritesViewModel != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(ScrapbookYellow)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                        ) {
                            IconButton(
                                onClick = { favoritesViewModel.toggleFavorite(album.toFavoriteItem()) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ScrapbookDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Text(text = album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    album.year?.let { year ->
                        Text(text = year.toString(), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─── Archive Album Item ───────────────────────────────────────────────────────

@Composable
fun ArchiveAlbumItem(
    item: ArchiveItem,
    onClick: () -> Unit,
    favoritesViewModel: FavoritesViewModel? = null,
    modifier: Modifier = Modifier
) {
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })
    val isBookmarked = favoriteIds.contains(item.id)

    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 4.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(ScrapbookPaper)
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )
                    // Play button
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = ScrapbookDark, modifier = Modifier.size(24.dp))
                    }
                    // Archive badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ScrapbookDark)
                            .border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 9.sp)
                    }
                    // Bookmark
                    if (favoritesViewModel != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(ScrapbookYellow)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                        ) {
                            IconButton(
                                onClick = { favoritesViewModel.toggleFavorite(item.toFavoriteItem()) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ScrapbookDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = item.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    item.creator?.let { Text(text = it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    item.year?.let { Text(text = it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.6f), fontSize = 10.sp) }
                }
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Divider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f), thickness = 2.dp)
    }
}

// ─── Albums Screen ────────────────────────────────────────────────────────────

@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null
) {
    val focusManager = LocalFocusManager.current
    val albumsState by contentViewModel.albumsState.collectAsState()

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }
    var playerVisible by remember { mutableStateOf(false) }
    var nowPlaying by remember { mutableStateOf<NowPlayingState?>(null) }
    var selectedEra by remember { mutableStateOf("ALL") }

    // ✅ Featured picks — random each session
    var featuredCommunityAlbum by remember { mutableStateOf(sampleAlbums.random()) }
    var featuredArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }

    // ✅ Set featured archive album once loaded
    LaunchedEffect(albumsState) {
        if (albumsState is ContentState.Success) {
            val items = (albumsState as ContentState.Success).items
            if (items.isNotEmpty() && featuredArchiveItem == null) {
                featuredArchiveItem = items.random()
            }
        }
    }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(600)
        if (searchQuery != lastSearched) {
            lastSearched = searchQuery
            contentViewModel.fetchAlbums(searchQuery)
        }
    }

    val filteredCommunityAlbums = remember(searchQuery, selectedEra) {
        sampleAlbums.filter { album ->
            val matchesSearch = searchQuery.isBlank() ||
                    album.title.contains(searchQuery, ignoreCase = true) ||
                    album.artist.contains(searchQuery, ignoreCase = true)
            val matchesEra = selectedEra == "ALL" || album.era == selectedEra
            matchesSearch && matchesEra
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ALBUMS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 32.sp, letterSpacing = 2.sp)
                        Text(text = "Retro game soundtracks", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) {
                            searchQuery = ""
                            focusManager.clearFocus()
                            contentViewModel.fetchAlbums()
                        }
                    }) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ✅ Now Playing bar
            AnimatedVisibility(
                visible = nowPlaying != null,
                enter = expandVertically(tween(300)),
                exit = shrinkVertically(tween(300))
            ) {
                nowPlaying?.let { state ->
                    NowPlayingBar(state = state, onClick = { playerVisible = true })
                }
            }

            // Search bar
            AnimatedVisibility(visible = searchVisible, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search albums or artists...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; contentViewModel.fetchAlbums() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookDark,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ✅ Era filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(albumEraFilters) { era ->
                    val isSelected = selectedEra == era
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedEra = era }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = era,
                            fontFamily = BangersFontFamily,
                            color = if (isSelected) ScrapbookYellow else ScrapbookDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                // ✅ Featured Today section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FEATURED TODAY",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                        // Shuffle button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ScrapbookPaper)
                                .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    featuredCommunityAlbum = sampleAlbums.random()
                                    val items = (albumsState as? ContentState.Success)?.items
                                    if (!items.isNullOrEmpty()) {
                                        featuredArchiveItem = items.random()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Shuffle", tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                                Text("SHUFFLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                            }
                        }
                    }

                    // ✅ Community featured hero card
                    FeaturedAlbumHeroCard(
                        title = featuredCommunityAlbum.title,
                        artist = featuredCommunityAlbum.artist,
                        coverResId = featuredCommunityAlbum.coverImageResId,
                        coverUrl = featuredCommunityAlbum.coverImageUrl,
                        year = featuredCommunityAlbum.year?.toString() ?: "",
                        badge = "🎮 COMMUNITY PICK",
                        onPlay = {
                            selectedAlbum = featuredCommunityAlbum
                            selectedArchiveItem = null
                            playerVisible = true
                        }
                    )

                    // ✅ Archive featured hero card — only when loaded
                    featuredArchiveItem?.let { archiveItem ->
                        Spacer(modifier = Modifier.height(4.dp))
                        FeaturedAlbumHeroCard(
                            title = archiveItem.title,
                            artist = archiveItem.creator ?: "Internet Archive",
                            coverUrl = archiveItem.thumbnailUrl,
                            year = archiveItem.year ?: "",
                            badge = "📦 ARCHIVE PICK",
                            onPlay = {
                                selectedArchiveItem = archiveItem
                                selectedAlbum = null
                                playerVisible = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = ScrapbookBorder.copy(alpha = 0.15f)
                    )
                }

                // ✅ Community section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎮", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMMUNITY",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ScrapbookYellow)
                                .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${filteredCommunityAlbums.size}",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 14.sp
                            )
                        }
                    }
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
                                    text = if (selectedEra == "ALL") "No albums found for \"$searchQuery\""
                                    else "No $selectedEra albums found",
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        filteredCommunityAlbums.chunked(2),
                        key = { chunk -> "community_${chunk.first().id}" }
                    ) { chunk ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunk.forEach { album ->
                                AlbumListItem(
                                    album = album,
                                    onAlbumClick = {
                                        selectedAlbum = it
                                        selectedArchiveItem = null
                                        playerVisible = true
                                    },
                                    favoritesViewModel = favoritesViewModel,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ✅ Archive section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📦", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INTERNET ARCHIVE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                when (val state = albumsState) {
                    is ContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Loading from archive...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    is ContentState.Error -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(state.message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 13.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookDark)
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                        .clickable { contentViewModel.fetchAlbums() }
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    is ContentState.Success -> {
                        items(
                            state.items.chunked(2),
                            key = { chunk -> "archive_${chunk.first().id}" }
                        ) { chunk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunk.forEach { item ->
                                    ArchiveAlbumItem(
                                        item = item,
                                        onClick = {
                                            selectedArchiveItem = item
                                            selectedAlbum = null
                                            playerVisible = true
                                        },
                                        favoritesViewModel = favoritesViewModel,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    else -> { }
                }
            }
        }

        // ✅ Slide-up player
        AnimatedVisibility(
            visible = playerVisible,
            enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            AlbumPlayerScreen(
                album = selectedAlbum,
                archiveItem = selectedArchiveItem,
                onClose = { playerVisible = false },
                onNowPlayingUpdate = { state -> nowPlaying = state }
            )
        }
    }
}
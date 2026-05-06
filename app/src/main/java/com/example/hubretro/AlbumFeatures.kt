package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*

// --- Data Structures ---
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val year: Int? = null,
    val webPlaybackUrl: String? = null
)

// --- Sample Community Albums ---
val sampleAlbums = listOf(
    Album(
        id = "album1",
        title = "Gunbound",
        artist = "Synth Rider",
        coverImageResId = R.drawable.ostcover1,
        year = 1984,
        webPlaybackUrl = "https://archive.org/details/gunbound-soundtrack"
    ),
    Album(
        id = "album2",
        title = "Pokemon Diamond & Pearl",
        artist = "Grid Runner",
        coverImageResId = R.drawable.ostcover2,
        year = 1988,
        webPlaybackUrl = "https://archive.org/details/pkmn-dppt-soundtrack"
    ),
    Album(
        id = "album3",
        title = "The Legend of Zelda: The Wind Waker",
        artist = "Chrome Catalyst",
        coverImageResId = R.drawable.ostcover3,
        year = 1991,
        webPlaybackUrl = "https://archive.org/details/the-legend-of-zelda-the-wind-waker-ost"
    ),
    Album(
        id = "album4",
        title = "Undertale",
        artist = "Vector Voyager",
        coverImageResId = R.drawable.ostcover4,
        year = 1986,
        webPlaybackUrl = "https://archive.org/details/undertaleost_202004"
    ),
    Album(
        id = "album5",
        title = "Lego Harry Potter Years 1-4",
        artist = "Bit Shifter",
        coverImageResId = R.drawable.ostcover5,
        year = 1982,
        webPlaybackUrl = "https://archive.org/details/lego-harry-potter-years-1-4"
    ),
    Album(
        id = "album6",
        title = "Final Fantasy VII",
        artist = "Analog Hero",
        coverImageResId = R.drawable.ostcover6,
        year = 1987,
        webPlaybackUrl = "https://archive.org/details/final_fantasy_vii_soundtrack"
    ),
    Album(
        id = "album7",
        title = "The Sims",
        artist = "Digital Nomad",
        coverImageResId = R.drawable.ostcover7,
        year = 1985,
        webPlaybackUrl = "https://archive.org/details/simsmusic"
    )
)

// --- In-App Album Player (WebView sliding up from bottom) ---
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AlbumPlayerScreen(
    album: Album? = null,
    archiveItem: ArchiveItem? = null,
    onClose: () -> Unit
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Header bar ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1A1A2E), Color(0xFF12122A))
                        )
                    )
                    .padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Close player",
                            tint = RetroTextOffWhite
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (artist.isNotBlank()) {
                            Text(
                                text = artist,
                                fontFamily = RetroFontFamily,
                                color = VaporwaveCyan,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = RetroTextOffWhite.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    ) {
                        Icon(
                            Icons.Filled.OpenInBrowser,
                            contentDescription = "Open in browser",
                            tint = VaporwavePink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- Album cover + info strip ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12122A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover art
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RetroDarkPurple)
                        .border(2.dp, VaporwavePink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
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
                        ) {
                            Text(
                                "♪",
                                color = VaporwavePink,
                                fontSize = 28.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = artist,
                            fontFamily = RetroFontFamily,
                            color = VaporwaveCyan,
                            fontSize = 11.sp
                        )
                    }
                    if (year.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = year,
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                VaporwavePink.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🎵 INTERNET ARCHIVE",
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Loading bar
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = VaporwavePink,
                    trackColor = Color(0xFF12122A)
                )
            }

            // Divider
            Divider(color = VaporwavePink.copy(alpha = 0.2f))

            // --- WebView player ---
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
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                isLoading = true
                            }
                        }
                        webChromeClient = WebChromeClient()
                        if (url.isNotBlank()) loadUrl(url)
                        webViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

// --- Community Album Item ---
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
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable { onAlbumClick(album) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                if (album.coverImageResId != null) {
                    Image(
                        painter = painterResource(id = album.coverImageResId),
                        contentDescription = "${album.title} cover art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "♪",
                            color = VaporwavePink,
                            fontSize = 28.sp
                        )
                    }
                }

                // Bookmark button on cover
                if (favoritesViewModel != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                    ) {
                        IconButton(
                            onClick = {
                                favoritesViewModel.toggleFavorite(album.toFavoriteItem())
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked)
                                    Icons.Filled.Bookmark
                                else
                                    Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (isBookmarked) VaporwavePink
                                else RetroTextOffWhite.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = RetroTextOffWhite,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(
                        color = VaporwavePink.copy(alpha = 0.7f),
                        offset = Offset(x = 2f, y = 2f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )

            Text(
                text = album.artist,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    color = VaporwaveCyan.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )

            album.year?.let { year ->
                Text(
                    text = year.toString(),
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        fontSize = 10.sp,
                        color = RetroTextOffWhite.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

// --- Archive Album Item ---
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

    Column(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF3A3A3A))
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Archive badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(
                        VaporwaveCyan.copy(alpha = 0.85f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ARCHIVE",
                    fontFamily = RetroFontFamily,
                    color = Color.Black,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bookmark button
            if (favoritesViewModel != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    IconButton(
                        onClick = {
                            favoritesViewModel.toggleFavorite(item.toFavoriteItem())
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked)
                                Icons.Filled.Bookmark
                            else
                                Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isBookmarked) VaporwavePink
                            else RetroTextOffWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = RetroTextOffWhite,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = VaporwaveCyan.copy(alpha = 0.7f),
                    offset = Offset(x = 2f, y = 2f),
                    blurRadius = 4f
                )
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .fillMaxWidth()
        )

        item.creator?.let { creator ->
            Text(
                text = creator,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    color = VaporwavePink.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )
        }

        item.year?.let { year ->
            Text(
                text = year,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    fontSize = 10.sp,
                    color = RetroTextOffWhite.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}

// --- Section Header ---
@Composable
fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = RetroFontFamily,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f)
        )
    }
}

// --- Albums Screen ---
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

    // Player state
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    var selectedArchiveItem by remember { mutableStateOf<ArchiveItem?>(null) }
    var playerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(600)
        if (searchQuery != lastSearched) {
            lastSearched = searchQuery
            contentViewModel.fetchAlbums(searchQuery)
        }
    }

    val filteredCommunityAlbums = remember(searchQuery) {
        if (searchQuery.isBlank()) sampleAlbums
        else sampleAlbums.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Title Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(40.dp))

                Text(
                    text = "ALBUMS",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = VaporwavePink.copy(alpha = 0.7f),
                            offset = Offset(x = 3f, y = 3f),
                            blurRadius = 5f
                        ),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) {
                            searchQuery = ""
                            focusManager.clearFocus()
                            contentViewModel.fetchAlbums()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searchVisible) "Close search" else "Search albums",
                        tint = if (searchVisible) VaporwavePink else RetroTextOffWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // --- Search Bar ---
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search albums or artists...",
                            fontFamily = RetroFontFamily,
                            fontSize = 12.sp,
                            color = RetroTextOffWhite.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = VaporwavePink,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                contentViewModel.fetchAlbums()
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = RetroTextOffWhite.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(
                        fontFamily = RetroFontFamily,
                        fontSize = 13.sp,
                        color = RetroTextOffWhite
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaporwavePink,
                        unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                        focusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                        unfocusedContainerColor = RetroDarkPurple.copy(alpha = 0.8f),
                        cursorColor = VaporwavePink
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // --- Content ---
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Community Section
                item {
                    SectionHeader(title = "COMMUNITY", color = VaporwavePink)
                }

                if (filteredCommunityAlbums.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No community albums found",
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
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
                                    onAlbumClick = { clickedAlbum ->
                                        selectedAlbum = clickedAlbum
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

                // Archive Section
                item {
                    SectionHeader(title = "INTERNET ARCHIVE", color = VaporwaveCyan)
                }

                when (val state = albumsState) {
                    is ContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = VaporwaveCyan,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Loading soundtracks...",
                                        fontFamily = RetroFontFamily,
                                        color = RetroTextOffWhite.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    is ContentState.Error -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.message,
                                        fontFamily = RetroFontFamily,
                                        color = SynthwaveOrange,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { contentViewModel.fetchAlbums() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VaporwaveCyan
                                        ),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "RETRY",
                                            fontFamily = RetroFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
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

        // --- Slide-up Album Player Overlay ---
        AnimatedVisibility(
            visible = playerVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.fillMaxSize()
        ) {
            AlbumPlayerScreen(
                album = selectedAlbum,
                archiveItem = selectedArchiveItem,
                onClose = {
                    playerVisible = false
                    selectedAlbum = null
                    selectedArchiveItem = null
                }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun AlbumsScreenPreview() {
    HubRetroTheme {
        AlbumsScreen()
    }
}
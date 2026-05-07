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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val year: Int? = null,
    val webPlaybackUrl: String? = null
)

val sampleAlbums = listOf(
    Album(id = "album1", title = "Gunbound", artist = "Synth Rider", coverImageResId = R.drawable.ostcover1, year = 1984, webPlaybackUrl = "https://archive.org/details/gunbound-soundtrack"),
    Album(id = "album2", title = "Pokemon Diamond & Pearl", artist = "Grid Runner", coverImageResId = R.drawable.ostcover2, year = 1988, webPlaybackUrl = "https://archive.org/details/pkmn-dppt-soundtrack"),
    Album(id = "album3", title = "The Legend of Zelda: The Wind Waker", artist = "Chrome Catalyst", coverImageResId = R.drawable.ostcover3, year = 1991, webPlaybackUrl = "https://archive.org/details/the-legend-of-zelda-the-wind-waker-ost"),
    Album(id = "album4", title = "Undertale", artist = "Vector Voyager", coverImageResId = R.drawable.ostcover4, year = 1986, webPlaybackUrl = "https://archive.org/details/undertaleost_202004"),
    Album(id = "album5", title = "Lego Harry Potter Years 1-4", artist = "Bit Shifter", coverImageResId = R.drawable.ostcover5, year = 1982, webPlaybackUrl = "https://archive.org/details/lego-harry-potter-years-1-4"),
    Album(id = "album6", title = "Final Fantasy VII", artist = "Analog Hero", coverImageResId = R.drawable.ostcover6, year = 1987, webPlaybackUrl = "https://archive.org/details/final_fantasy_vii_soundtrack"),
    Album(id = "album7", title = "The Sims", artist = "Digital Nomad", coverImageResId = R.drawable.ostcover7, year = 1985, webPlaybackUrl = "https://archive.org/details/simsmusic")
)

// --- Album Player Screen ---
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
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header bar
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
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Close player",
                            tint = ScrapbookDark
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (artist.isNotBlank()) {
                            Text(
                                text = artist,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookDark.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = ScrapbookDark.copy(alpha = 0.6f),
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
                            tint = ScrapbookDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Album cover info strip
            Box(modifier = Modifier.fillMaxWidth()) {
                ScrapbookCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 12.dp,
                    shadowOffset = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookPaper)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
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
                                    Text("♪", color = ScrapbookDark, fontSize = 28.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = title,
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (artist.isNotBlank()) {
                                Text(
                                    text = artist,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            if (year.isNotBlank()) {
                                Text(
                                    text = year,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ScrapbookYellow)
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🎵 INTERNET ARCHIVE",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ScrapbookYellowDark,
                    trackColor = ScrapbookPaper
                )
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
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAlbumClick(album) },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    if (album.coverImageResId != null) {
                        Image(
                            painter = painterResource(id = album.coverImageResId),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ScrapbookPaper),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♪", color = ScrapbookDark, fontSize = 28.sp)
                        }
                    }
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
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                    else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ScrapbookDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = album.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = album.artist,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    album.year?.let { year ->
                        Text(
                            text = year.toString(),
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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

    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        .background(ScrapbookPaper)
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
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ScrapbookDark)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ARCHIVE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookYellow,
                            fontSize = 8.sp
                        )
                    }
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
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                    else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ScrapbookDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = item.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    item.creator?.let { creator ->
                        Text(
                            text = creator,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item.year?.let { year ->
                        Text(
                            text = year,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// Section Header — kept for backward compatibility
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
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 22.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = ScrapbookBorder.copy(alpha = 0.2f),
            thickness = 2.dp
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ALBUMS",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
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
                        }
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

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
                            fontFamily = NunitoFontFamily,
                            fontSize = 13.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = ScrapbookDark,
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
                                    contentDescription = "Clear",
                                    tint = ScrapbookTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        color = ScrapbookTextDark
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookDark,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookCream)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { SectionHeader(title = "COMMUNITY", color = ScrapbookDark) }

                if (filteredCommunityAlbums.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No community albums found",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 13.sp,
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

                item { SectionHeader(title = "INTERNET ARCHIVE", color = ScrapbookDark) }

                when (val state = albumsState) {
                    is ContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ScrapbookYellowDark,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    is ContentState.Error -> {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookDark)
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                        .clickable { contentViewModel.fetchAlbums() }
                                        .padding(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        "RETRY",
                                        fontFamily = BangersFontFamily,
                                        color = ScrapbookYellow,
                                        fontSize = 18.sp
                                    )
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

        // Slide-up player
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

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0)
@Composable
fun AlbumsScreenPreview() {
    HubRetroTheme {
        AlbumsScreen()
    }
}
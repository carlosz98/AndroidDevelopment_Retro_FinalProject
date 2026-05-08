package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlinx.coroutines.delay

// 1. Data Class
data class MagazineCover(
    val id: String,
    val title: String,
    val coverImageResId: Int? = null,
    val coverImageUrl: String? = null,
    val webUrl: String? = null
)

val uniqueCoverResourceIds = listOf(
    R.drawable.cover1, R.drawable.cover2, R.drawable.cover3,
    R.drawable.cover4, R.drawable.cover5, R.drawable.cover6,
    R.drawable.cover7, R.drawable.cover8, R.drawable.cover9
)

val sampleMagazineCovers = List(9) { i ->
    MagazineCover(
        id = (i + 1).toString(),
        title = "Retro Magazine ${i + 1}",
        coverImageResId = if (i < uniqueCoverResourceIds.size) uniqueCoverResourceIds[i] else R.drawable.cover1,
        webUrl = null
    )
}

fun ArchiveItem.toMagazineCover() = MagazineCover(
    id = this.id,
    title = this.title,
    coverImageResId = null,
    coverImageUrl = this.thumbnailUrl,
    webUrl = this.webUrl
)

// 2. URL transform — no change needed
fun toArchiveEmbedUrl(webUrl: String): String = webUrl

// 3. Minimal CSS injection — unchanged working version
fun injectHideStyles(view: WebView?) {
    view?.evaluateJavascript(
        """
        (function() {
            var style = document.createElement('style');
            style.innerHTML = `
                #oc-hdr,
                #nav-tophat,
                .topinblock,
                header {
                    display: none !important;
                }
                body {
                    margin-top: 0 !important;
                    padding-top: 0 !important;
                }
            `;
            document.head.appendChild(style);
            try {
                var br = document.querySelector('#bookreader, .BookReader, #BookReader');
                if (br) { br.scrollIntoView({ behavior: 'smooth' }); }
            } catch(e) {}
        })();
        """.trimIndent(),
        null
    )
}

// 4. Magazine Reader — unchanged
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MagazineReaderScreen(
    url: String,
    title: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val embedUrl = remember(url) { toArchiveEmbedUrl(url) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(embedUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showControls by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showControls = true
        delay(3000L)
        showControls = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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
                        allowFileAccess = true
                        allowContentAccess = true
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/112.0.0.0 Mobile Safari/537.36"
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            url?.let { currentUrl = it }
                            injectHideStyles(view)
                            view?.postDelayed({ injectHideStyles(view) }, 1500)
                            view?.postDelayed({ injectHideStyles(view) }, 3000)
                        }
                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: android.graphics.Bitmap?
                        ) { isLoading = true }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(embedUrl)
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = ScrapbookYellow,
                trackColor = Color.Transparent
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showControls = !showControls }
        )

        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(tween(400, easing = LinearOutSlowInEasing)) { -it } + fadeIn(tween(400)),
            exit = slideOutVertically(tween(300, easing = FastOutLinearInEasing)) { -it } + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.92f),
                                Color.Black.copy(alpha = 0.0f)
                            )
                        )
                    )
                    .padding(top = 40.dp, bottom = 32.dp, start = 4.dp, end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Tap anywhere to show/hide controls",
                            fontFamily = NunitoFontFamily,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { webViewRef?.reload() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.7f),
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
                            tint = ScrapbookYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// 5. Magazines Screen
@Composable
fun MagazinesScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null
) {
    val focusManager = LocalFocusManager.current
    val magazinesState by contentViewModel.magazinesState.collectAsState()
    val isLoadingMore by contentViewModel.isLoadingMoreMagazines.collectAsState()
    val hasMore by contentViewModel.hasMoreMagazines.collectAsState()

    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var selectedMagazine by remember { mutableStateOf<MagazineCover?>(null) }
    var readerVisible by remember { mutableStateOf(false) }
    var visibleRows by remember { mutableStateOf(3) }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(600)
        if (searchQuery != lastSearched) {
            lastSearched = searchQuery
            visibleRows = 3
            contentViewModel.fetchMagazines(searchQuery)
        }
    }

    val isSearching = searchQuery.isNotBlank()

    val archiveMagazines = when (val state = magazinesState) {
        is ContentState.Success -> state.items.map { it.toMagazineCover() }
        else -> emptyList()
    }

    val magazinesPerShelf = 4
    val allShelves = archiveMagazines.chunked(magazinesPerShelf)
    val visibleShelves = allShelves.take(visibleRows)

    fun openReader(magazine: MagazineCover) {
        if (!magazine.webUrl.isNullOrBlank()) {
            selectedMagazine = magazine
            readerVisible = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Scrapbook Header
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
                        text = "MAGAZINES",
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
                                contentViewModel.fetchMagazines()
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

            // Search bar
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
                            "Search retro magazines...",
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
                                contentViewModel.fetchMagazines()
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

            // Content
            when (val state = magazinesState) {
                is ContentState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = ScrapbookYellowDark,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading magazines...",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                is ContentState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
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
                                    .clickable { contentViewModel.fetchMagazines() }
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
                    if (isSearching) {
                        // Grid view when searching — 4 columns
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                ScrapbookMagazineGridItem(
                                    item = item,
                                    isBookmarked = favoriteIds.contains(item.id),
                                    onBookmarkToggle = {
                                        favoritesViewModel?.toggleFavorite(item.toFavoriteItem())
                                    },
                                    onClick = { openReader(item.toMagazineCover()) }
                                )
                            }
                        }
                    } else {
                        // Shelf view — 4 per shelf + View More
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 1.dp,
                                bottom = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.1.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(visibleShelves) { _, shelfMagazines ->
                                ShelfRow(
                                    magazinesOnShelf = shelfMagazines,
                                    shelfImageResId = R.drawable.shelf,
                                    favoriteIds = favoriteIds,
                                    onBookmarkToggle = { magazine ->
                                        val archiveItem = state.items.find { it.id == magazine.id }
                                        archiveItem?.let {
                                            favoritesViewModel?.toggleFavorite(it.toFavoriteItem())
                                        }
                                    },
                                    onMagazineClick = { magazine -> openReader(magazine) },
                                    magazinesPerShelf = magazinesPerShelf
                                )
                            }

                            val totalRows = allShelves.size
                            val canShowMore = visibleRows < totalRows || hasMore

                            if (canShowMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoadingMore) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    color = ScrapbookYellowDark,
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    "Loading more...",
                                                    fontFamily = NunitoFontFamily,
                                                    color = ScrapbookTextMuted,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(ScrapbookYellow.copy(alpha = 0.2f))
                                                    .border(
                                                        2.dp,
                                                        ScrapbookBorder,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        val newVisible = visibleRows + 3
                                                        visibleRows = newVisible
                                                        if (newVisible >= totalRows && hasMore) {
                                                            contentViewModel.loadMoreMagazines()
                                                        }
                                                    }
                                                    .padding(vertical = 14.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "📚 VIEW MORE MAGAZINES",
                                                    fontFamily = BangersFontFamily,
                                                    color = ScrapbookDark,
                                                    fontSize = 16.sp,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> { }
            }
        }

        // Reader overlay
        AnimatedVisibility(
            visible = readerVisible,
            enter = slideInVertically(tween(500, easing = LinearOutSlowInEasing)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(400, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedMagazine?.let { magazine ->
                MagazineReaderScreen(
                    url = magazine.webUrl ?: "",
                    title = magazine.title,
                    onClose = {
                        readerVisible = false
                        selectedMagazine = null
                    }
                )
            }
        }
    }
}

// 6. ✅ Scrapbook Magazine Grid Item
@Composable
fun ScrapbookMagazineGridItem(
    item: ArchiveItem,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clickable(onClick = onClick),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 8.dp,
            shadowOffset = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScrapbookPaper)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(ScrapbookYellow)
                        .border(1.dp, ScrapbookBorder, CircleShape)
                ) {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = ScrapbookDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ScrapbookDark.copy(alpha = 0.75f))
                        .padding(4.dp)
                ) {
                    Text(
                        text = item.title,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Keep old name for backward compatibility
@Composable
fun ArchiveMagazineGridItem(
    item: ArchiveItem,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ScrapbookMagazineGridItem(
        item = item,
        onClick = onClick,
        isBookmarked = isBookmarked,
        onBookmarkToggle = onBookmarkToggle,
        modifier = modifier
    )
}

// 7. Magazine Cover Item
@Composable
fun MagazineCoverItem(
    magazine: MagazineCover,
    onClick: () -> Unit,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clickable(onClick = onClick),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 8.dp,
            shadowOffset = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (magazine.coverImageResId != null) {
                    Image(
                        painter = painterResource(id = magazine.coverImageResId),
                        contentDescription = magazine.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (magazine.coverImageUrl != null) {
                    AsyncImage(
                        model = magazine.coverImageUrl,
                        contentDescription = magazine.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ScrapbookPaper)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ScrapbookPaper),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = magazine.title,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            textAlign = TextAlign.Center,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(ScrapbookYellow)
                        .border(1.dp, ScrapbookBorder, CircleShape)
                ) {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = ScrapbookDark,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ScrapbookDark.copy(alpha = 0.75f))
                        .padding(4.dp)
                ) {
                    Text(
                        text = magazine.title,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// 8. Shelf Row — updated for scrapbook style
@Composable
fun ShelfRow(
    magazinesOnShelf: List<MagazineCover>,
    shelfImageResId: Int,
    onMagazineClick: (MagazineCover) -> Unit,
    magazinesPerShelf: Int,
    favoriteIds: Set<String> = emptySet(),
    onBookmarkToggle: (MagazineCover) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = shelfImageResId),
            contentDescription = "Magazine Shelf",
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            magazinesOnShelf.forEach { magazine ->
                MagazineCoverItem(
                    magazine = magazine,
                    onClick = { onMagazineClick(magazine) },
                    isBookmarked = favoriteIds.contains(magazine.id),
                    onBookmarkToggle = { onBookmarkToggle(magazine) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (magazinesOnShelf.size < magazinesPerShelf) {
                for (i in 0 until (magazinesPerShelf - magazinesOnShelf.size)) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
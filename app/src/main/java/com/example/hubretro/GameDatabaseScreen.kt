package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

val gameGenreFilters = listOf(
    "ALL", "RPG", "ACTION", "PLATFORMER",
    "SHOOTER", "ADVENTURE", "ARCADE", "SPORTS", "PUZZLE"
)

// ─── Popular game categories to load on start ─────────────────────────────────

val popularGameQueries = listOf(
    "mario", "zelda", "sonic", "donkey kong",
    "pokemon", "metroid", "castlevania", "street fighter"
)

// ─── YouTube search helper ────────────────────────────────────────────────────

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

// ─── Game Database Screen ─────────────────────────────────────────────────────

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

    // ✅ Popular games — loaded by category
    var popularGames by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isLoadingPopular by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        isLoadingPopular = true
        loadingProgress = 0
        try {
            coroutineScope {
                val results = popularGameQueries.map { query ->
                    async {
                        try {
                            val games = IGDBRepository.searchGames(query)
                            loadingProgress++
                            games
                        } catch (e: Exception) {
                            loadingProgress++
                            emptyList()
                        }
                    }
                }.awaitAll()
                popularGames = results.flatten()
                    .distinctBy { it.id }
                    .sortedByDescending { it.rating ?: 0.0 }
                    .take(40)
            }
        } catch (e: Exception) { }
        finally { isLoadingPopular = false }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearching = true
            hasSearched = true
            try {
                searchResults = IGDBRepository.searchGames(searchQuery)
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally { isSearching = false }
        } else if (searchQuery.isBlank()) {
            searchResults = emptyList()
            hasSearched = false
        }
    }

    if (selectedGame != null) {
        GameDetailScreen(
            game = selectedGame!!,
            onBack = { selectedGame = null }
        )
        return
    }

    val displayGames = if (hasSearched) searchResults else popularGames

    // ✅ Genre filter applied client-side using name keywords
    val filteredGames = remember(displayGames, selectedGenre) {
        if (selectedGenre == "ALL") displayGames
        else {
            val keywords = when (selectedGenre) {
                "RPG" -> listOf("fantasy", "legend", "quest", "rpg", "role", "pokemon", "dragon")
                "ACTION" -> listOf("sonic", "contra", "action", "batman", "spider", "devil", "god")
                "PLATFORMER" -> listOf("mario", "kirby", "crash", "banjo", "donkey", "rayman")
                "SHOOTER" -> listOf("doom", "halo", "quake", "metroid", "contra", "gunstar")
                "ADVENTURE" -> listOf("zelda", "adventure", "link", "tomb", "uncharted")
                "ARCADE" -> listOf("pac", "street fighter", "tetris", "galaga", "space", "castlevania")
                "SPORTS" -> listOf("tennis", "soccer", "football", "basketball", "golf", "racing")
                "PUZZLE" -> listOf("puzzle", "tetris", "columns", "block", "brain")
                else -> emptyList()
            }
            displayGames.filter { game ->
                keywords.any { game.name.lowercase().contains(it) }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Header
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
                        Text(
                            text = "🎮 GAME DATABASE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 28.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Powered by IGDB",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookDark.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle view",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ✅ Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search any retro game...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = ScrapbookDark,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; focusManager.clearFocus() }) {
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ✅ Genre filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gameGenreFilters) { genre ->
                    val isSelected = selectedGenre == genre
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                            .clickable { selectedGenre = genre }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            fontFamily = BangersFontFamily,
                            color = if (isSelected) ScrapbookYellow else ScrapbookDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Section label + count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasSearched) "SEARCH RESULTS" else "POPULAR RETRO GAMES",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                if (filteredGames.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookYellow)
                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "${filteredGames.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                    }
                }
            }

            // ✅ Content
            when {
                isLoadingPopular && !hasSearched -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading games... $loadingProgress/${popularGameQueries.size}",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(ScrapbookPaper)
                                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(
                                            (loadingProgress.toFloat() / popularGameQueries.size)
                                                .coerceIn(0f, 1f)
                                        )
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ScrapbookYellowDark)
                                )
                            }
                        }
                    }
                }
                filteredGames.isEmpty() && hasSearched && !isSearching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text("🎮", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No games found for \"$searchQuery\"",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                filteredGames.isEmpty() && selectedGenre != "ALL" -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text("🕹️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No $selectedGenre games in current list.\nTry searching for one!",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredGames, key = { it.id }) { game ->
                            GameGridCard(game = game, onClick = { selectedGame = game })
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredGames, key = { it.id }) { game ->
                            GameListCard(game = game, onClick = { selectedGame = game })
                        }
                    }
                }
            }
        }
    }
}

// ─── Game List Card ───────────────────────────────────────────────────────────

@Composable
fun GameListCard(game: IGDBGame, onClick: () -> Unit) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🎮", fontSize = 32.sp)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        game.releaseYear?.let { year ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ScrapbookPaper)
                                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "$year", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 11.sp)
                            }
                        }
                        game.rating?.let { rating ->
                            // ✅ Better rating display
                            IGDBRatingBar(rating = rating)
                        }
                    }
                    game.summary?.let { summary ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = summary, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── Game Grid Card ───────────────────────────────────────────────────────────

@Composable
fun GameGridCard(game: IGDBGame, onClick: () -> Unit) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(ScrapbookPaper),
                    contentAlignment = Alignment.Center
                ) {
                    if (game.coverUrl != null) {
                        AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🎮", fontSize = 36.sp)
                    }
                    // Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))))
                    )
                    // ✅ Rating badge top-right
                    game.rating?.let { rating ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        rating >= 80 -> ScrapbookGreen
                                        rating >= 60 -> ScrapbookYellowDark
                                        else -> ScrapbookRed
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${(rating / 10).toInt()}/10",
                                fontFamily = BangersFontFamily,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                    // Year at bottom
                    game.releaseYear?.let { year ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(ScrapbookDark.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "$year", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                }
            }
        }
    }
}

// ─── IGDB Rating Bar ─────────────────────────────────────────────────────────

@Composable
fun IGDBRatingBar(rating: Double, compact: Boolean = true) {
    val score = (rating / 10).toInt().coerceIn(0, 10)
    val color = when {
        rating >= 80 -> ScrapbookGreen
        rating >= 60 -> ScrapbookYellowDark
        else -> ScrapbookRed
    }
    if (compact) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$score/10",
                    fontFamily = BangersFontFamily,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            Text(
                text = when {
                    rating >= 80 -> "GREAT"
                    rating >= 60 -> "GOOD"
                    rating >= 40 -> "OK"
                    else -> "MIXED"
                },
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 10.sp
            )
        }
    } else {
        // Full rating display for detail screen
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$score",
                        fontFamily = BangersFontFamily,
                        color = Color.White,
                        fontSize = 28.sp
                    )
                }
                Column {
                    Text(
                        text = when {
                            rating >= 80 -> "🏆 OUTSTANDING"
                            rating >= 70 -> "⭐ GREAT"
                            rating >= 60 -> "👍 GOOD"
                            rating >= 50 -> "😐 MIXED"
                            else -> "👎 POOR"
                        },
                        fontFamily = BangersFontFamily,
                        color = color,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "IGDB Score — ${String.format("%.1f", rating)}/100",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ScrapbookPaper)
                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((rating / 100f).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

// ─── Game Detail Screen ───────────────────────────────────────────────────────

@Composable
fun GameDetailScreen(game: IGDBGame, onBack: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("INFO", "TRAILER", "SCREENSHOTS")

    // ✅ YouTube trailer
    var trailerVideoId by remember { mutableStateOf<String?>(null) }
    var isLoadingTrailer by remember { mutableStateOf(false) }

    // ✅ Load trailer when trailer tab selected
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && trailerVideoId == null && !isLoadingTrailer) {
            isLoadingTrailer = true
            trailerVideoId = searchYouTubeTrailer(game.name)
            isLoadingTrailer = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                // ✅ Hero section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (game.coverUrl != null) {
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.6f
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(ScrapbookDark))
                    }

                    // Dark gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ScrapbookDark.copy(alpha = 0.3f),
                                        ScrapbookDark.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )

                    // Back button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 40.dp, start = 8.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { onBack() }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }

                    // Bottom content
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cover art
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ScrapbookPaper)
                                .border(3.dp, ScrapbookYellow, RoundedCornerShape(12.dp))
                        ) {
                            if (game.coverUrl != null) {
                                AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("🎮", fontSize = 32.sp)
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, lineHeight = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                game.releaseYear?.let { year ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(text = "$year", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ScrapbookYellow)
                                        .border(1.dp, ScrapbookBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = "IGDB #${game.id}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // ✅ Tab strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ScrapbookCardWhite)
                        .border(BorderStroke(1.dp, ScrapbookBorder.copy(alpha = 0.2f)))
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selectedTab == index) ScrapbookYellow else ScrapbookCardWhite)
                                .clickable { selectedTab = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tab, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ✅ Tab content
            when (selectedTab) {
                0 -> {
                    // INFO tab
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Rating section
                            game.rating?.let { rating ->
                                Text("IGDB RATING", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                                Box {
                                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp) {
                                        Box(modifier = Modifier.padding(16.dp)) {
                                            IGDBRatingBar(rating = rating, compact = false)
                                        }
                                    }
                                }
                            }

                            // About section
                            if (!game.summary.isNullOrBlank()) {
                                Text("ABOUT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                                Box {
                                    ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp) {
                                        Text(
                                            text = game.summary,
                                            fontFamily = NunitoFontFamily,
                                            color = ScrapbookTextDark,
                                            fontSize = 15.sp,
                                            lineHeight = 23.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }

                            // Details card
                            Text("DETAILS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                            Box {
                                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        game.releaseYear?.let {
                                            GameDetailRow("Release Year", "$it")
                                            HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.1f))
                                        }
                                        game.rating?.let {
                                            GameDetailRow("IGDB Score", "${String.format("%.1f", it)}/100")
                                            HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.1f))
                                        }
                                        GameDetailRow("IGDB ID", "#${game.id}")
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TRAILER tab
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🎬 TRAILER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)

                            when {
                                isLoadingTrailer -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ScrapbookDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = ScrapbookYellow, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Finding trailer...", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                        }
                                    }
                                }
                                trailerVideoId != null -> {
                                    // ✅ YouTube player
                                    Box {
                                        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, cornerRadius = 12.dp) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(ScrapbookRed)
                                                    )
                                                    Text(
                                                        "${game.name} — Trailer / Gameplay",
                                                        fontFamily = BangersFontFamily,
                                                        color = Color.White,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                YoutubePlayerCard(
                                                    youtubeVideoId = trailerVideoId,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(16f / 9f)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    lifecycleOwner = lifecycleOwner
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ScrapbookPaper)
                                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("📺", fontSize = 36.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No trailer found for this game", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // ✅ Pro tip card
                            Box {
                                ScrapbookCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = ScrapbookYellow.copy(alpha = 0.2f),
                                    borderColor = ScrapbookYellowDark,
                                    cornerRadius = 10.dp
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("💡", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Trailers are sourced from YouTube. Results may vary by game.",
                                            fontFamily = NunitoFontFamily,
                                            color = ScrapbookDark,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // SCREENSHOTS tab
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📸 SCREENSHOTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)

                            // ✅ Show cover art large + game info as screenshot-like cards
                            // Since IGDB free tier doesn't include screenshots endpoint,
                            // we show the cover art prominently with game details
                            Box {
                                ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
                                    Column {
                                        if (game.coverUrl != null) {
                                            AsyncImage(
                                                model = game.coverUrl,
                                                contentDescription = game.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(300.dp)
                                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                                    .background(ScrapbookDark)
                                            )
                                        }
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("COVER ART", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, letterSpacing = 1.sp)
                                            Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }

                            // ✅ Game info cards styled like screenshot frames
                            GameInfoCard(
                                title = "🎮 GAMEPLAY OVERVIEW",
                                content = game.summary ?: "No gameplay description available for this title.",
                                backgroundColor = ScrapbookCardWhite
                            )
                            GameInfoCard(
                                title = "📅 RELEASE INFO",
                                content = buildString {
                                    append("Released in ")
                                    append(game.releaseYear?.toString() ?: "unknown year")
                                    append(".\n")
                                    game.rating?.let {
                                        append("Rated ${String.format("%.1f", it)}/100 on IGDB by the gaming community.")
                                    }
                                },
                                backgroundColor = ScrapbookYellow.copy(alpha = 0.1f)
                            )

                            // ✅ Note about screenshots
                            Box {
                                ScrapbookCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = ScrapbookPaper,
                                    cornerRadius = 10.dp
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("ℹ️", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Full screenshot gallery requires IGDB Pro. Search YouTube for gameplay footage!",
                                            fontFamily = NunitoFontFamily,
                                            color = ScrapbookTextMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Game Info Card ───────────────────────────────────────────────────────────

@Composable
fun GameInfoCard(
    title: String,
    content: String,
    backgroundColor: Color
) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = backgroundColor,
            cornerRadius = 10.dp,
            shadowOffset = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = content, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

// ─── Game Detail Row ──────────────────────────────────────────────────────────

@Composable
fun GameDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 14.sp)
        Text(text = value, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
    }
}
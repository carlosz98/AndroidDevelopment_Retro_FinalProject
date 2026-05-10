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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay

val gameGenreFilters = listOf(
    "ALL", "RPG", "ACTION", "PLATFORMER",
    "SHOOTER", "ADVENTURE", "ARCADE", "SPORTS", "PUZZLE"
)

val gamePlatformFilters = listOf(
    "ALL", "NES", "SNES", "N64", "PS1",
    "PS2", "SEGA", "GAMEBOY", "ARCADE"
)

@Composable
fun GameDatabaseScreen(modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var selectedGenre by remember { mutableStateOf("ALL") }
    var selectedPlatform by remember { mutableStateOf("ALL") }
    var isGridView by remember { mutableStateOf(false) }
    var selectedGame by remember { mutableStateOf<IGDBGame?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    // Default popular retro games to show on load
    var popularGames by remember { mutableStateOf<List<IGDBGame>>(emptyList()) }
    var isLoadingPopular by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingPopular = true
        try {
            popularGames = IGDBRepository.searchGames("mario")
                .plus(IGDBRepository.searchGames("zelda"))
                .plus(IGDBRepository.searchGames("sonic"))
                .distinctBy { it.id }
                .take(20)
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
            } finally {
                isSearching = false
            }
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
                    Text(
                        text = "🎮 GAME DATABASE",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    // Toggle view
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Filled.ViewList
                            else Icons.Filled.GridView,
                            contentDescription = "Toggle view",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Search bar
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
                            "Search retro games...",
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                color = ScrapbookDark,
                                modifier = Modifier.size(20.dp).padding(2.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = ScrapbookDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                focusManager.clearFocus()
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Genre filters
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

            // Section label
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
                if (displayGames.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookYellow)
                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${displayGames.size}",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Results
            when {
                isLoadingPopular && !hasSearched -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = ScrapbookYellowDark,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Loading games...",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                displayGames.isEmpty() && hasSearched && !isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
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
                isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp,
                            top = 4.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayGames, key = { it.id }) { game ->
                            GameGridCard(
                                game = game,
                                onClick = { selectedGame = game }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayGames, key = { it.id }) { game ->
                            GameListCard(
                                game = game,
                                onClick = { selectedGame = game }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameListCard(game: IGDBGame, onClick: () -> Unit) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (game.coverUrl != null) {
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🎮", fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.name,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        game.releaseYear?.let { year ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ScrapbookPaper)
                                    .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$year",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        game.rating?.let { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = ScrapbookYellowDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = String.format("%.1f", rating / 10.0),
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookDark,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    game.summary?.let { summary ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summary,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }
                Text(
                    "→",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark.copy(alpha = 0.3f),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

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
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🎮", fontSize = 36.sp)
                    }
                    // Rating badge
                    game.rating?.let { rating ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ScrapbookYellow)
                                .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "★ ${String.format("%.0f", rating / 10.0)}",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = game.name,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    game.releaseYear?.let { year ->
                        Text(
                            text = "$year",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameDetailScreen(game: IGDBGame, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                // Hero image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (game.coverUrl != null) {
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(ScrapbookPaper),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎮", fontSize = 64.sp)
                        }
                    }
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.8f)
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
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Title on image
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = game.name,
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 28.sp,
                            lineHeight = 32.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            game.releaseYear?.let { year ->
                                Text(
                                    text = "$year",
                                    fontFamily = NunitoFontFamily,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp
                                )
                            }
                            game.rating?.let { rating ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ScrapbookYellow)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "★ ${String.format("%.1f", rating / 10.0)}/10",
                                        fontFamily = BangersFontFamily,
                                        color = ScrapbookDark,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                if (!game.summary.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ABOUT",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            ScrapbookCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = ScrapbookCardWhite,
                                cornerRadius = 12.dp
                            ) {
                                Text(
                                    text = game.summary,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextDark,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Stats
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "DETAILS",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        ScrapbookCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = ScrapbookCardWhite,
                            cornerRadius = 12.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                game.releaseYear?.let { year ->
                                    GameDetailRow(label = "Release Year", value = "$year")
                                }
                                game.rating?.let { rating ->
                                    GameDetailRow(
                                        label = "Rating",
                                        value = "${String.format("%.1f", rating / 10.0)}/10"
                                    )
                                }
                                GameDetailRow(label = "IGDB ID", value = "#${game.id}")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GameDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = ScrapbookTextMuted,
            fontSize = 14.sp
        )
        Text(
            text = value,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 16.sp
        )
    }
}
package com.example.hubretro

import android.util.Log
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.data.models.NewsItem
import com.example.hubretro.ui.news.NewsViewModel
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- Constants ---
val WELCOME_IMAGE_RESOURCE_ID = R.drawable.welcome
val ALBUMS_CARD_IMAGE = R.drawable.ostcover6
val MAGAZINES_CARD_IMAGE = R.drawable.cover1
val ARTICLES_CARD_IMAGE = R.drawable.article1
val PROFILE_CARD_IMAGE = R.drawable.p1

// --- Retro Facts ---
val retroFacts = listOf(
    "The first commercially sold video game was Computer Space in 1971.",
    "The original Game Boy had a battery life of about 15 hours.",
    "Pac-Man was originally called Puck Man in Japan.",
    "The NES was released in Japan as the Famicom in 1983.",
    "Super Mario Bros. was designed to be easy to pick up and hard to put down.",
    "The first Easter egg in a video game was hidden in Atari's Adventure (1980).",
    "Tetris was designed by Soviet software engineer Alexey Pajitnov in 1984.",
    "The SNES had a 16-bit processor running at 3.58 MHz.",
    "Doom (1993) was so popular it was installed on more PCs than Windows 95.",
    "The PlayStation was originally designed as a CD add-on for the Super Nintendo.",
    "Space Invaders caused a coin shortage in Japan when it launched in 1978.",
    "The first 3D video game was Battlezone, released by Atari in 1980.",
    "Legend of Zelda cartridges had a battery inside to save game data.",
    "Sonic the Hedgehog was designed to rival Mario's popularity.",
    "The N64 controller had an analog stick — revolutionary for its time."
)

// --- Retro Quotes ---
val retroQuotes = listOf(
    "\"It's dangerous to go alone! Take this.\" — The Legend of Zelda",
    "\"Do a barrel roll!\" — Star Fox 64",
    "\"The cake is a lie.\" — Portal",
    "\"Stay a while and listen.\" — Diablo II",
    "\"Hey! Listen!\" — Navi, Ocarina of Time",
    "\"War. War never changes.\" — Fallout",
    "\"I used to be an adventurer like you...\" — Skyrim Guard",
    "\"It's super effective!\" — Pokémon",
    "\"Thank you Mario! But our princess is in another castle!\" — Super Mario Bros",
    "\"Rise from your grave!\" — Altered Beast"
)

// --- Main Home Screen ---
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit,
    onNavigateToProfile: () -> Unit,
    newsViewModel: NewsViewModel = viewModel()
) {
    val newsItemsList by newsViewModel.newsItems.collectAsState()
    val isLoadingNews by newsViewModel.isLoading.collectAsState()
    val newsErrorMessage by newsViewModel.error.collectAsState()

    // Rotating fact/quote state
    var currentFactIndex by remember { mutableStateOf((0 until retroFacts.size).random()) }
    var currentQuoteIndex by remember { mutableStateOf((0 until retroQuotes.size).random()) }

    // Random game picker state
    var pickedGame by remember { mutableStateOf<String?>(null) }
    var isSpinning by remember { mutableStateOf(false) }

    // Auto-rotate facts every 12 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(12000L)
            currentFactIndex = (currentFactIndex + 1) % retroFacts.size
        }
    }

    // Auto-rotate quotes every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L)
            currentQuoteIndex = (currentQuoteIndex + 1) % retroQuotes.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // --- Header ---
        Text(
            text = "HOME",
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
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
                .fillMaxWidth()
        )

        // --- Welcome Section ---
        WelcomeSection(
            imageModel = WELCOME_IMAGE_RESOURCE_ID,
            title = "WELCOME",
            description = "Dive into the digital past with RetroHub! Explore curated collections of classic game soundtracks, vintage tech magazines, and insightful articles celebrating the golden era of computing and gaming."
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- Retro Quote of the Day ---
        RetroQuoteCard(quote = retroQuotes[currentQuoteIndex])

        Spacer(modifier = Modifier.height(24.dp))

        // --- Visual Navigation Section ---
        HomeSectionTitle(title = "EXPLORE RETROHUB")
        Spacer(modifier = Modifier.height(12.dp))
        VisualNavGrid(
            onNavigateToAlbums = onNavigateToAlbums,
            onNavigateToMagazines = onNavigateToMagazines,
            onNavigateToArticles = onNavigateToArticles,
            onNavigateToProfile = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- Featured Albums Carousel ---
        HomeSectionTitle(title = "🎵 FEATURED ALBUMS")
        Spacer(modifier = Modifier.height(10.dp))
        FeaturedAlbumsCarousel(onNavigateToAlbums = onNavigateToAlbums)

        Spacer(modifier = Modifier.height(28.dp))

        // --- Featured Magazines Carousel ---
        HomeSectionTitle(title = "📰 FEATURED MAGAZINES")
        Spacer(modifier = Modifier.height(10.dp))
        FeaturedMagazinesCarousel(onNavigateToMagazines = onNavigateToMagazines)

        Spacer(modifier = Modifier.height(28.dp))

        // --- Did You Know? ---
        DidYouKnowCard(
            fact = retroFacts[currentFactIndex],
            onNext = { currentFactIndex = (currentFactIndex + 1) % retroFacts.size }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- Random Game Picker ---
        RandomGamePicker(
            pickedGame = pickedGame,
            isSpinning = isSpinning,
            onSpin = {
                isSpinning = true
                pickedGame = null
            },
            onSpinComplete = { game ->
                pickedGame = game
                isSpinning = false
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // --- Latest News (moved lower) ---
        NewsSection(
            newsItems = newsItemsList,
            isLoading = isLoadingNews,
            errorMessage = newsErrorMessage,
            onRetry = { newsViewModel.fetchNews() }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- Copyright Footer ---
        CopyrightFooter(
            name = "Carlos Zabala",
            blogUrl = "https://charlysblog.framer.website"
        )
    }
}

// --- Section Title ---
@Composable
fun HomeSectionTitle(title: String) {
    Text(
        text = title,
        style = TextStyle(
            fontFamily = RetroFontFamily,
            color = VaporwaveTeal,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = RetroAccentBlue.copy(alpha = 0.5f),
                offset = Offset(2f, 2f),
                blurRadius = 3f
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

// --- Retro Quote Card ---
@Composable
fun RetroQuoteCard(quote: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        VaporwavePurple.copy(alpha = 0.6f),
                        RetroDarkPurple.copy(alpha = 0.8f)
                    )
                )
            )
            .border(1.dp, VaporwavePink.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "💬 QUOTE OF THE MOMENT",
                fontFamily = RetroFontFamily,
                color = VaporwavePink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quote,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

// --- Visual Nav Grid ---
@Composable
fun VisualNavGrid(
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val items = listOf(
        Triple("ALBUMS", ALBUMS_CARD_IMAGE, onNavigateToAlbums),
        Triple("MAGAZINES", MAGAZINES_CARD_IMAGE, onNavigateToMagazines),
        Triple("ARTICLES", ARTICLES_CARD_IMAGE, onNavigateToArticles),
        Triple("PROFILE", PROFILE_CARD_IMAGE, onNavigateToProfile)
    )

    val gradients = listOf(
        listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.7f)),
        listOf(VaporwavePurple, VaporwaveCyan.copy(alpha = 0.7f)),
        listOf(SynthwaveOrange, VaporwavePink.copy(alpha = 0.7f)),
        listOf(RetroGold, VaporwavePink.copy(alpha = 0.6f))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row — 2 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.take(2).forEachIndexed { index, (title, imageRes, onClick) ->
                VisualNavCard(
                    title = title,
                    imageResId = imageRes,
                    gradient = gradients[index],
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Second row — 2 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.drop(2).forEachIndexed { index, (title, imageRes, onClick) ->
                VisualNavCard(
                    title = title,
                    imageResId = imageRes,
                    gradient = gradients[index + 2],
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// --- Individual Visual Nav Card ---
@Composable
fun VisualNavCard(
    title: String,
    @DrawableRes imageResId: Int,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradient))
            .border(
                1.5.dp,
                gradient.first().copy(alpha = glowAlpha),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Background image with overlay
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Title at bottom
        Text(
            text = title,
            fontFamily = RetroFontFamily,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    Color.Black.copy(alpha = 0.8f),
                    Offset(1f, 1f),
                    2f
                )
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }
}

// --- Featured Albums Carousel ---
@Composable
fun FeaturedAlbumsCarousel(onNavigateToAlbums: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sampleAlbums.take(5), key = { it.id }) { album ->
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RetroDarkPurple.copy(alpha = 0.7f))
                    .border(1.dp, VaporwavePink.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable { onNavigateToAlbums() }
            ) {
                Column {
                    if (album.coverImageResId != null) {
                        Image(
                            painter = painterResource(id = album.coverImageResId),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                        )
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = album.title,
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.sp
                        )
                        Text(
                            text = album.artist,
                            fontFamily = RetroFontFamily,
                            color = VaporwaveCyan.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(155.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VaporwavePink.copy(alpha = 0.15f))
                    .border(1.dp, VaporwavePink.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable { onNavigateToAlbums() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("→", color = VaporwavePink, fontSize = 22.sp)
                    Text(
                        "SEE ALL",
                        fontFamily = RetroFontFamily,
                        color = VaporwavePink,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// --- Featured Magazines Carousel ---
@Composable
fun FeaturedMagazinesCarousel(onNavigateToMagazines: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sampleMagazineCovers.take(5), key = { it.id }) { magazine ->
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RetroDarkPurple.copy(alpha = 0.6f))
                    .border(1.dp, VaporwavePurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onNavigateToMagazines() }
            ) {
                Column {
                    if (magazine.coverImageResId != null) {
                        Image(
                            painter = painterResource(id = magazine.coverImageResId),
                            contentDescription = magazine.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        )
                    }
                    Text(
                        text = magazine.title,
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite,
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VaporwavePurple.copy(alpha = 0.15f))
                    .border(1.dp, VaporwavePurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable { onNavigateToMagazines() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("→", color = VaporwavePurple, fontSize = 20.sp)
                    Text(
                        "SEE ALL",
                        fontFamily = RetroFontFamily,
                        color = VaporwavePurple,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// --- Did You Know Card ---
@Composable
fun DidYouKnowCard(
    fact: String,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A2A1A),
                        RetroDarkPurple.copy(alpha = 0.9f)
                    )
                )
            )
            .border(1.dp, VaporwaveGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🕹️ DID YOU KNOW?",
                    fontFamily = RetroFontFamily,
                    color = VaporwaveGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Next fact",
                        tint = VaporwaveGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = fact,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

// --- Random Game Picker ---
@Composable
fun RandomGamePicker(
    pickedGame: String?,
    isSpinning: Boolean,
    onSpin: () -> Unit,
    onSpinComplete: (String) -> Unit
) {
    val retrogames = listOf(
        "Super Mario Bros", "The Legend of Zelda", "Sonic the Hedgehog",
        "Mega Man 2", "Castlevania", "Contra", "Street Fighter II",
        "Final Fantasy VI", "Chrono Trigger", "Earthbound",
        "Donkey Kong Country", "Super Metroid", "Kirby's Adventure",
        "Teenage Mutant Ninja Turtles", "Pac-Man", "Space Invaders",
        "Tetris", "Pokémon Red", "GoldenEye 007", "Banjo-Kazooie",
        "Ocarina of Time", "Star Fox 64", "Wave Race 64",
        "Crash Bandicoot", "Spyro the Dragon", "Metal Gear Solid",
        "Resident Evil 2", "Tony Hawk's Pro Skater", "Doom",
        "Quake", "Half-Life", "Age of Empires II"
    )

    var displayGame by remember { mutableStateOf("???") }

    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            repeat(20) {
                displayGame = retrogames.random()
                delay(80L + it * 8L) // Slow down over time
            }
            val finalGame = retrogames.random()
            displayGame = finalGame
            onSpinComplete(finalGame)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF2A1A3E)
                    )
                )
            )
            .border(1.dp, SynthwaveOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🎲 RANDOM GAME PICKER",
                fontFamily = RetroFontFamily,
                color = SynthwaveOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Game display box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, SynthwaveOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSpinning) displayGame else (pickedGame ?: "Press SPIN!"),
                    fontFamily = RetroFontFamily,
                    color = if (pickedGame != null && !isSpinning) SynthwaveOrange
                    else RetroTextOffWhite,
                    fontSize = if (isSpinning) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSpin,
                enabled = !isSpinning,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SynthwaveOrange,
                    disabledContainerColor = SynthwaveOrange.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSpinning) "SPINNING..." else "SPIN!",
                    fontFamily = RetroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }

            if (pickedGame != null && !isSpinning) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tonight's retro pick! 🎮",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --- News Section ---
@Composable
fun NewsSection(
    newsItems: List<NewsItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        HomeSectionTitle(title = "📡 LATEST NEWS")
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VaporwavePink)
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = SynthwaveOrange,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = VaporwavePink),
                        shape = CircleShape
                    ) {
                        Text("RETRY", fontFamily = RetroFontFamily, color = RetroTextOffWhite)
                    }
                }
            }
            newsItems.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No news articles found at the moment.",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(newsItems, key = { it.id }) { newsItem ->
                        NewsItemCard(newsItem = newsItem)
                        Divider(color = VaporwavePink.copy(alpha = 0.3f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

// --- News Item Card ---
@Composable
fun NewsItemCard(newsItem: NewsItem, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                if (newsItem.sourceUrl.isNotBlank()) {
                    try {
                        uriHandler.openUri(newsItem.sourceUrl)
                    } catch (e: Exception) {
                        Log.e("NewsItemCard", "Could not open URI: ${newsItem.sourceUrl}", e)
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroDarkPurple.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, VaporwavePink.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            newsItem.imageUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = newsItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = newsItem.title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = newsItem.summary,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = newsItem.sourceName,
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = VaporwaveCyan.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = formatEpochMillisToReadableDate(newsItem.publishedDate),
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// --- Date formatter ---
fun formatEpochMillisToReadableDate(epochMillis: Long): String {
    return try {
        val date = Date(epochMillis)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        "Date N/A"
    }
}

// --- Welcome Section ---
@Composable
fun WelcomeSection(
    imageModel: Any,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val imageShape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(180.dp)
                .clip(imageShape)
                .border(
                    BorderStroke(width = 2.dp, color = VaporwavePink),
                    shape = imageShape
                )
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = VaporwavePink.copy(alpha = 0.6f),
                    offset = Offset(x = 2f, y = 2f),
                    blurRadius = 4f
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = description,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.85f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// --- Copyright Footer ---
@Composable
fun CopyrightFooter(name: String, blogUrl: String, modifier: Modifier = Modifier) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "© $currentYear $name. All Rights Reserved.",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                fontSize = 12.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        val annotatedString = buildAnnotatedString {
            append("Visit my blog: ")
            pushStringAnnotation(tag = "URL", annotation = blogUrl)
            withStyle(
                style = SpanStyle(
                    color = Color.White,
                    textDecoration = TextDecoration.Underline,
                    fontFamily = RetroFontFamily,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append("charlysblog.framer.website")
            }
            pop()
        }
        ClickableText(
            text = annotatedString,
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontFamily = RetroFontFamily,
                color = Color.White
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        Log.e("CopyrightFooter", "Could not open URI: ${annotation.item}", e)
                    }
                }
            }
        )
    }
}

// --- Previews ---
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun HomeScreenPreview() {
    HubRetroTheme {
        HomeScreen(
            onNavigateToAlbums = {},
            onNavigateToMagazines = {},
            onNavigateToArticles = {},
            onNavigateToProfile = {}
        )
    }
}
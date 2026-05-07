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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

// --- Scrapbook Card helper ---
@Composable
fun ScrapbookCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ScrapbookCardWhite,
    borderColor: Color = ScrapbookBorder,
    borderWidth: Dp = 2.dp,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .offset(x = shadowOffset, y = shadowOffset)
            .background(
                ScrapbookShadow.copy(alpha = 0.15f),
                RoundedCornerShape(cornerRadius)
            )
    ) {
        Box(
            modifier = Modifier
                .offset(x = -shadowOffset, y = -shadowOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(backgroundColor)
                .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius)),
            content = content
        )
    }
}

// --- Section Title ---
@Composable
fun HomeSectionTitle(title: String) {
    Text(
        text = title,
        fontFamily = BangersFontFamily,
        color = ScrapbookDark,
        fontSize = 24.sp,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

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

    var currentFactIndex by remember { mutableStateOf((0 until retroFacts.size).random()) }
    var currentQuoteIndex by remember { mutableStateOf((0 until retroQuotes.size).random()) }
    var pickedGame by remember { mutableStateOf<String?>(null) }
    var isSpinning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(12000L)
            currentFactIndex = (currentFactIndex + 1) % retroFacts.size
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L)
            currentQuoteIndex = (currentQuoteIndex + 1) % retroQuotes.size
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // --- Header ---
        ScrapbookHeader()

        Spacer(modifier = Modifier.height(16.dp))

        // --- Welcome Hero ---
        WelcomeSection(
            imageModel = WELCOME_IMAGE_RESOURCE_ID,
            title = "WELCOME, EXPLORER!",
            description = "Dive into the digital past with RetroHub! Explore curated collections of classic game soundtracks, vintage tech magazines, and articles celebrating the golden era of gaming."
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Quote Card ---
        RetroQuoteCard(quote = retroQuotes[currentQuoteIndex])

        Spacer(modifier = Modifier.height(24.dp))

        // --- Explore Nav ---
        HomeSectionTitle(title = "EXPLORE RETROHUB")
        Spacer(modifier = Modifier.height(12.dp))
        VisualNavGrid(
            onNavigateToAlbums = onNavigateToAlbums,
            onNavigateToMagazines = onNavigateToMagazines,
            onNavigateToArticles = onNavigateToArticles,
            onNavigateToProfile = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Featured Albums ---
        HomeSectionTitle(title = "🎵 FEATURED ALBUMS")
        Spacer(modifier = Modifier.height(10.dp))
        FeaturedAlbumsCarousel(onNavigateToAlbums = onNavigateToAlbums)

        Spacer(modifier = Modifier.height(24.dp))

        // --- Featured Magazines ---
        HomeSectionTitle(title = "📰 FEATURED MAGAZINES")
        Spacer(modifier = Modifier.height(10.dp))
        FeaturedMagazinesCarousel(onNavigateToMagazines = onNavigateToMagazines)

        Spacer(modifier = Modifier.height(24.dp))

        // --- Did You Know ---
        DidYouKnowCard(
            fact = retroFacts[currentFactIndex],
            onNext = { currentFactIndex = (currentFactIndex + 1) % retroFacts.size }
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(24.dp))

        // --- Latest News ---
        NewsSection(
            newsItems = newsItemsList,
            isLoading = isLoadingNews,
            errorMessage = newsErrorMessage,
            onRetry = { newsViewModel.fetchNews() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Footer ---
        CopyrightFooter(
            name = "Carlos Zabala",
            blogUrl = "https://charlysblog.framer.website"
        )
    }
}

// --- Scrapbook Header ---
@Composable
fun ScrapbookHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScrapbookYellow)
            .border(
                BorderStroke(3.dp, ScrapbookBorder),
            )
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RETROHUB",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 48.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Your retro gaming universe",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = ScrapbookDark.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(180.dp),
            cornerRadius = 12.dp
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
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 28.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookPaper,
            cornerRadius = 12.dp
        ) {
            Text(
                text = description,
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Normal,
                color = ScrapbookTextDark,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// --- Quote Card ---
@Composable
fun RetroQuoteCard(quote: String) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookYellow,
            cornerRadius = 12.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💬 QUOTE OF THE MOMENT",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = quote,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = ScrapbookTextDark,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontStyle = FontStyle.Italic
                )
            }
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

    val colors = listOf(
        ScrapbookYellow,
        ScrapbookOrange,
        ScrapbookBlue,
        ScrapbookPurple
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.take(2).forEachIndexed { index, (title, imageRes, onClick) ->
                VisualNavCard(
                    title = title,
                    imageResId = imageRes,
                    accentColor = colors[index],
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.drop(2).forEachIndexed { index, (title, imageRes, onClick) ->
                VisualNavCard(
                    title = title,
                    imageResId = imageRes,
                    accentColor = colors[index + 2],
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// --- Individual Nav Card ---
@Composable
fun VisualNavCard(
    title: String,
    @DrawableRes imageResId: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable(onClick = onClick),
            backgroundColor = ScrapbookCardWhite,
            borderColor = ScrapbookBorder,
            cornerRadius = 14.dp,
            shadowOffset = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.3f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor.copy(alpha = 0.6f))
                )
                // Title label at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ScrapbookDark)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = BangersFontFamily,
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
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
            Box(modifier = Modifier.width(120.dp)) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 10.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(
                        modifier = Modifier.clickable { onNavigateToAlbums() }
                    ) {
                        if (album.coverImageResId != null) {
                            Image(
                                painter = painterResource(id = album.coverImageResId),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = album.title,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookTextDark,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = album.artist,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(160.dp),
            ) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = ScrapbookYellow,
                    cornerRadius = 10.dp,
                    shadowOffset = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onNavigateToAlbums() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", color = ScrapbookDark, fontSize = 22.sp,
                                fontFamily = BangersFontFamily)
                            Text(
                                "SEE ALL",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
            Box(modifier = Modifier.width(90.dp)) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 8.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(
                        modifier = Modifier.clickable { onNavigateToMagazines() }
                    ) {
                        if (magazine.coverImageResId != null) {
                            Image(
                                painter = painterResource(id = magazine.coverImageResId),
                                contentDescription = magazine.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                        Text(
                            text = magazine.title,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextDark,
                            fontSize = 9.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(145.dp)
            ) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = ScrapbookOrange,
                    cornerRadius = 8.dp,
                    shadowOffset = 3.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onNavigateToMagazines() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", color = Color.White, fontSize = 20.sp,
                                fontFamily = BangersFontFamily)
                            Text(
                                "SEE ALL",
                                fontFamily = BangersFontFamily,
                                color = Color.White,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookGreen.copy(alpha = 0.15f),
            borderColor = ScrapbookGreen,
            cornerRadius = 12.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕹️ DID YOU KNOW?",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Next fact",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = fact,
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextDark,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
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
                delay(80L + it * 8L)
            }
            val finalGame = retrogames.random()
            displayGame = finalGame
            onSpinComplete(finalGame)
        }
    }

    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎲 RANDOM GAME PICKER",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Game display
                Box(modifier = Modifier.fillMaxWidth()) {
                    ScrapbookCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = ScrapbookYellow.copy(alpha = 0.3f),
                        borderColor = ScrapbookYellowDark,
                        cornerRadius = 8.dp,
                        shadowOffset = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSpinning) displayGame
                                else (pickedGame ?: "Press SPIN!"),
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = if (isSpinning) 16.sp else 20.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSpin,
                    enabled = !isSpinning,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScrapbookDark,
                        disabledContainerColor = ScrapbookDark.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(2.dp, ScrapbookBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ScrapbookYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpinning) "SPINNING..." else "SPIN!",
                        fontFamily = BangersFontFamily,
                        fontSize = 18.sp,
                        color = ScrapbookYellow,
                        letterSpacing = 1.sp
                    )
                }

                if (pickedGame != null && !isSpinning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tonight's retro pick! 🎮",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = ScrapbookTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
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
                    CircularProgressIndicator(color = ScrapbookYellowDark)
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookRed,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScrapbookDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "RETRY",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookYellow,
                            fontSize = 16.sp
                        )
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
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newsItems, key = { it.id }) { newsItem ->
                        NewsItemCard(newsItem = newsItem)
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
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (newsItem.sourceUrl.isNotBlank()) {
                        try { uriHandler.openUri(newsItem.sourceUrl) }
                        catch (e: Exception) { }
                    }
                },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp
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
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 20.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = newsItem.summary,
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookYellow)
                            .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = newsItem.sourceName,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ScrapbookDark,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = formatEpochMillisToReadableDate(newsItem.publishedDate),
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 11.sp
                    )
                }
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

// --- Copyright Footer ---
@Composable
fun CopyrightFooter(name: String, blogUrl: String, modifier: Modifier = Modifier) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookDark,
            borderColor = ScrapbookDark,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "© $currentYear $name. All Rights Reserved.",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                val annotatedString = buildAnnotatedString {
                    append("Visit my blog: ")
                    pushStringAnnotation(tag = "URL", annotation = blogUrl)
                    withStyle(
                        style = SpanStyle(
                            color = ScrapbookYellow,
                            textDecoration = TextDecoration.Underline,
                            fontFamily = NunitoFontFamily,
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
                        fontFamily = NunitoFontFamily,
                        color = Color.White
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { annotation ->
                            try { uriHandler.openUri(annotation.item) }
                            catch (e: Exception) { }
                        }
                    }
                )
            }
        }
    }
}
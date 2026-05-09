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

// --- Today in Retro Gaming facts by month/day ---
val todayInRetroGaming = listOf(
    "On this day in 1985, Super Mario Bros. launched in Japan and changed gaming forever.",
    "On this day in 1989, the Game Boy was released — 118 million units would follow.",
    "On this day in 1991, Sonic the Hedgehog debuted on the Sega Genesis.",
    "On this day in 1996, the Nintendo 64 launched in Japan with Super Mario 64.",
    "On this day in 1998, The Legend of Zelda: Ocarina of Time released to universal acclaim.",
    "On this day in 1993, Doom was released as shareware and defined a generation of shooters.",
    "On this day in 1994, the PlayStation launched in Japan, selling 100,000 units in one day.",
    "On this day in 2001, the Game Boy Advance launched with 6 titles.",
    "On this day in 1977, the Atari 2600 launched — the first truly successful home console.",
    "On this day in 1980, Pac-Man made its arcade debut in Japan."
)

// --- Scrapbook Card ---
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
            .background(ScrapbookShadow.copy(alpha = 0.15f), RoundedCornerShape(cornerRadius))
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
        fontSize = 26.sp,
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
    onNavigateToStreams: () -> Unit = {},
    onNavigateToDiscover: () -> Unit = {},
    newsViewModel: NewsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val newsItemsList by newsViewModel.newsItems.collectAsState()
    val isLoadingNews by newsViewModel.isLoading.collectAsState()
    val newsErrorMessage by newsViewModel.error.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var currentFactIndex by remember { mutableStateOf((0 until retroFacts.size).random()) }
    var currentQuoteIndex by remember { mutableStateOf((0 until retroQuotes.size).random()) }
    var todayFactIndex by remember { mutableStateOf((0 until todayInRetroGaming.size).random()) }
    var pickedGame by remember { mutableStateOf<String?>(null) }
    var isSpinning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.fetchAllUsers()
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

    // Recent community activity from users
    val recentActivity = remember(allUsers) {
        allUsers.take(5)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // ✅ 1. Hero Section — image with overlay title
        HeroSection(
            imageModel = WELCOME_IMAGE_RESOURCE_ID,
            onNavigateToDiscover = onNavigateToDiscover
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 2. Today in Retro Gaming
        TodayInRetroSection(
            fact = todayInRetroGaming[todayFactIndex],
            onNext = { todayFactIndex = (todayFactIndex + 1) % todayInRetroGaming.size }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 3. Quote Card
        RetroQuoteCard(quote = retroQuotes[currentQuoteIndex])

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 4. Explore Nav — taller cards
        HomeSectionTitle(title = "EXPLORE RETROHUB")
        Spacer(modifier = Modifier.height(12.dp))
        VisualNavGrid(
            onNavigateToAlbums = onNavigateToAlbums,
            onNavigateToMagazines = onNavigateToMagazines,
            onNavigateToArticles = onNavigateToArticles,
            onNavigateToProfile = onNavigateToProfile
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 5. Community Activity Feed
        if (recentActivity.isNotEmpty()) {
            CommunityActivitySection(
                users = recentActivity,
                onUserTap = { onNavigateToDiscover() }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ✅ 6. Featured Albums
        HomeSectionTitle(title = "🎵 FEATURED ALBUMS")
        Spacer(modifier = Modifier.height(10.dp))
        FeaturedAlbumsCarousel(onNavigateToAlbums = onNavigateToAlbums)

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 7. Featured Magazines
        HomeSectionTitle(title = "📰 FEATURED MAGAZINES")
        Spacer(modifier = Modifier.height(10.dp))
        FeaturedMagazinesCarousel(onNavigateToMagazines = onNavigateToMagazines)

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 8. Did You Know
        DidYouKnowCard(
            fact = retroFacts[currentFactIndex],
            onNext = { currentFactIndex = (currentFactIndex + 1) % retroFacts.size }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 9. Random Game Picker
        RandomGamePicker(
            pickedGame = pickedGame,
            isSpinning = isSpinning,
            onSpin = { isSpinning = true; pickedGame = null },
            onSpinComplete = { game -> pickedGame = game; isSpinning = false }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 10. Streams section — scrapbook style
        FeaturedStreamsSection(onNavigateToStreams = onNavigateToStreams)

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 11. Latest News
        NewsSection(
            newsItems = newsItemsList,
            isLoading = isLoadingNews,
            errorMessage = newsErrorMessage,
            onRetry = { newsViewModel.fetchNews() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ 12. Footer
        CopyrightFooter(
            name = "Carlos Zabala",
            blogUrl = "https://charlysblog.framer.website"
        )
    }
}

// ✅ IMPROVED Hero Section — image with gradient overlay + title on top
@Composable
fun HeroSection(
    imageModel: Any,
    onNavigateToDiscover: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background image
        AsyncImage(
            model = imageModel,
            contentDescription = "RetroHub Hero",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay — dark at bottom, transparent at top
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Yellow top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ScrapbookYellow)
                .align(Alignment.TopCenter)
        )

        // Content overlaid on image
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = "RETROHUB",
                fontFamily = BangersFontFamily,
                color = ScrapbookYellow,
                fontSize = 52.sp,
                letterSpacing = 3.sp
            )
            Text(
                text = "Your retro gaming universe",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Description
            Text(
                text = "Dive into the digital past — curated soundtracks, vintage magazines, retro articles and a community of explorers.",
                fontFamily = NunitoFontFamily,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            // CTA button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ScrapbookYellow)
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                    .clickable { onNavigateToDiscover() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "EXPLORE NOW →",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ✅ NEW — Today in Retro Gaming
@Composable
fun TodayInRetroSection(fact: String, onNext: () -> Unit) {
    val today = remember {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    }
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookDark,
            cornerRadius = 14.dp,
            shadowOffset = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📅 TODAY IN RETRO",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookYellow,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = today,
                            fontFamily = NunitoFontFamily,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Next",
                            tint = ScrapbookYellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = fact,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// ✅ NEW — Community Activity Feed
@Composable
fun CommunityActivitySection(
    users: List<UserProfileData>,
    onUserTap: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionTitle(title = "👥 COMMUNITY")
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ScrapbookCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = ScrapbookCardWhite,
                cornerRadius = 14.dp,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WHO'S ON RETROHUB",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fellow retro explorers in the community",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // User avatars row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(users, key = { it.uid }) { user ->
                            CommunityUserChip(user = user, onTap = onUserTap)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Discover button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                            .clickable { onUserTap() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FIND MORE PEOPLE →",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityUserChip(user: UserProfileData, onTap: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onTap() }
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ScrapbookPaper)
                .border(2.dp, ScrapbookBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!user.profilePictureUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = user.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = user.username.take(1).uppercase(),
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.username,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = ScrapbookDark,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ✅ IMPROVED Quote Card
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
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = quote,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = ScrapbookTextDark,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

// ✅ IMPROVED Nav Grid — taller cards (160dp)
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
        ScrapbookYellow, ScrapbookOrange, ScrapbookBlue, ScrapbookPurple
    )
    val emojis = listOf("🎵", "📰", "📝", "👤")

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
                    emoji = emojis[index],
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
                    emoji = emojis[index + 2],
                    imageResId = imageRes,
                    accentColor = colors[index + 2],
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ✅ IMPROVED Nav Card — taller with emoji + subtitle
@Composable
fun VisualNavCard(
    title: String,
    emoji: String,
    @DrawableRes imageResId: Int,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp) // ⬆ was 130
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
                    alpha = 0.35f
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accentColor.copy(alpha = 0.55f))
                )
                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
                // Emoji top-left
                Text(
                    text = emoji,
                    fontSize = 28.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                )
                // Title at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ScrapbookDark)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = BangersFontFamily,
                        color = Color.White,
                        fontSize = 22.sp,
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
            Box(modifier = Modifier.width(130.dp)) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 10.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(modifier = Modifier.clickable { onNavigateToAlbums() }) {
                        if (album.coverImageResId != null) {
                            Image(
                                painter = painterResource(id = album.coverImageResId),
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = album.title,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookTextDark,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = album.artist,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        item {
            Box(modifier = Modifier.width(80.dp).height(170.dp)) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = ScrapbookYellow,
                    cornerRadius = 10.dp,
                    shadowOffset = 3.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable { onNavigateToAlbums() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", color = ScrapbookDark, fontSize = 24.sp, fontFamily = BangersFontFamily)
                            Text("SEE ALL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, textAlign = TextAlign.Center)
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
            Box(modifier = Modifier.width(100.dp)) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 8.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(modifier = Modifier.clickable { onNavigateToMagazines() }) {
                        if (magazine.coverImageResId != null) {
                            Image(
                                painter = painterResource(id = magazine.coverImageResId),
                                contentDescription = magazine.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                        }
                        Text(
                            text = magazine.title,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = ScrapbookTextDark,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
        item {
            Box(modifier = Modifier.width(80.dp).height(155.dp)) {
                ScrapbookCard(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = ScrapbookOrange,
                    cornerRadius = 8.dp,
                    shadowOffset = 3.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable { onNavigateToMagazines() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("→", color = Color.White, fontSize = 22.sp, fontFamily = BangersFontFamily)
                            Text("SEE ALL", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// --- Did You Know Card ---
@Composable
fun DidYouKnowCard(fact: String, onNext: () -> Unit) {
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
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onNext, modifier = Modifier.size(28.dp)) {
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
                    fontWeight = FontWeight.Medium,
                    color = ScrapbookTextDark,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
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
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎲 RANDOM GAME PICKER",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 24.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                                text = if (isSpinning) displayGame else (pickedGame ?: "Press SPIN!"),
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = if (isSpinning) 18.sp else 22.sp,
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
                        modifier = Modifier.size(18.dp),
                        tint = ScrapbookYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSpinning) "SPINNING..." else "SPIN!",
                        fontFamily = BangersFontFamily,
                        fontSize = 20.sp,
                        color = ScrapbookYellow,
                        letterSpacing = 1.sp
                    )
                }
                if (pickedGame != null && !isSpinning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tonight's retro pick! 🎮",
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ✅ IMPROVED Streams section — scrapbook style
@Composable
fun FeaturedStreamsSection(onNavigateToStreams: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionTitle(title = "📺 STREAMS & VIDEOS")
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            ScrapbookCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStreams() },
                backgroundColor = ScrapbookCardWhite,
                cornerRadius = 14.dp,
                shadowOffset = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Watch live retro gaming streams and classic gaming videos from the community.",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Twitch button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF9146FF))
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToStreams() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔴 LIVE STREAMS",
                                fontFamily = BangersFontFamily,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                        // YouTube button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF0000))
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToStreams() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶ VIDEOS",
                                fontFamily = BangersFontFamily,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
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
                    modifier = Modifier.fillMaxWidth().height(200.dp),
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
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = ScrapbookDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                    }
                }
            }
            newsItems.isEmpty() && !isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No news articles found at the moment.",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
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
                        try { uriHandler.openUri(newsItem.sourceUrl) } catch (e: Exception) { }
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
                    fontSize = 22.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = newsItem.summary,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = ScrapbookTextMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
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
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = formatEpochMillisToReadableDate(newsItem.publishedDate),
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 13.sp
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
    } catch (e: Exception) { "Date N/A" }
}

// --- Copyright Footer ---
@Composable
fun CopyrightFooter(name: String, blogUrl: String, modifier: Modifier = Modifier) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val uriHandler = LocalUriHandler.current
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookDark,
            borderColor = ScrapbookDark,
            cornerRadius = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "© $currentYear $name. All Rights Reserved.",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
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
                    ) { append("charlysblog.framer.website") }
                    pop()
                }
                ClickableText(
                    text = annotatedString,
                    style = TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontFamily = NunitoFontFamily,
                        color = Color.White
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let {
                                try { uriHandler.openUri(it.item) } catch (e: Exception) { }
                            }
                    }
                )
            }
        }
    }
}
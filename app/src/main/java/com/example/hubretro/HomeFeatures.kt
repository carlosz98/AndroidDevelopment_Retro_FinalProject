package com.example.hubretro

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Added for news list
import androidx.compose.foundation.lazy.items // Added for news list
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card // Added for NewsCard
import androidx.compose.material3.CardDefaults // Added for NewsCard
import androidx.compose.material3.CircularProgressIndicator // Added for loading
import androidx.compose.material3.Divider // Added for news list
import androidx.compose.material3.MaterialTheme // Added for news styling
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // Added for ViewModel
import androidx.compose.runtime.getValue // Added for ViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // For potential use in NewsCard click
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // Added for ViewModel
import coil.compose.AsyncImage
import com.example.hubretro.data.models.NewsItem // IMPORT YOUR NEWS ITEM MODEL
import com.example.hubretro.ui.news.NewsViewModel // IMPORT YOUR NEWS VIEWMODEL
import com.example.hubretro.ui.theme.*
import java.text.SimpleDateFormat // For formatting date
import java.util.Calendar
import java.util.Date // For formatting date
import java.util.Locale // For formatting date
import android.util.Log
import androidx.compose.animation.core.copy


// --- Constants for Image Resources ---
val WELCOME_IMAGE_RESOURCE_ID = R.drawable.welcome
val ALBUMS_CARD_IMAGE = R.drawable.ostcover6
val MAGAZINES_CARD_IMAGE = R.drawable.cover1
val ARTICLES_CARD_IMAGE = R.drawable.article1
val PROFILE_CARD_IMAGE = R.drawable.p1

// --- Main Home Screen Composable ---
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToAlbums: () -> Unit,
    onNavigateToMagazines: () -> Unit,
    onNavigateToArticles: () -> Unit,
    onNavigateToProfile: () -> Unit,
    newsViewModel: NewsViewModel = viewModel() // Inject NewsViewModel
) {
    // Collect states from the NewsViewModel
    val newsItemsList by newsViewModel.newsItems.collectAsState()
    val isLoadingNews by newsViewModel.isLoading.collectAsState()
    val newsErrorMessage by newsViewModel.error.collectAsState()

    // The main Column should be scrollable IF its content overflows the screen height.
    // However, the news list itself will be a LazyColumn, handling its own internal scrolling.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Make the overall screen scrollable
            .padding(bottom = 16.dp)
    ) {
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
                .padding(top = 24.dp, bottom = 20.dp)
                .fillMaxWidth()
        )

        WelcomeSection(
            imageModel = WELCOME_IMAGE_RESOURCE_ID,
            title = "WELCOME",
            description = "Dive into the digital past with RetroHub! Explore curated collections of classic game soundtracks, vintage tech magazines, and insightful articles celebrating the golden era of computing and gaming. Let the nostalgia begin!"
        )

        Spacer(modifier = Modifier.height(24.dp)) // Added some space

        // --- NEWS SECTION ---
        NewsSection(
            newsItems = newsItemsList,
            isLoading = isLoadingNews,
            errorMessage = newsErrorMessage,
            onRetry = { newsViewModel.fetchNews() } // Provide retry mechanism
        )
        // --- END OF NEWS SECTION ---

        Spacer(modifier = Modifier.height(32.dp))

        FeatureNavigationCard(
            title = "ALBUMS",
            description = "Groove to the classics. Soundtracks from legendary games await your ears.",
            imageResId = ALBUMS_CARD_IMAGE,
            buttonText = "TAKE ME THERE",
            onButtonClick = onNavigateToAlbums,
            gradientColors = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.7f))
        )
        Spacer(modifier = Modifier.height(20.dp))
        FeatureNavigationCard(
            title = "MAGAZINES",
            description = "Flip through history. Vintage tech and gaming magazines, digitized for you.",
            imageResId = MAGAZINES_CARD_IMAGE,
            buttonText = "TAKE ME THERE",
            onButtonClick = onNavigateToMagazines,
            gradientColors = listOf(VaporwavePurple, VaporwaveCyan.copy(alpha = 0.7f))
        )
        Spacer(modifier = Modifier.height(20.dp))
        FeatureNavigationCard(
            title = "ARTICLES",
            description = "Read insightful retrospectives and analyses on the golden age of digital.",
            imageResId = ARTICLES_CARD_IMAGE,
            buttonText = "TAKE ME THERE",
            onButtonClick = onNavigateToArticles,
            gradientColors = listOf(SynthwaveOrange, VaporwavePink.copy(alpha = 0.7f))
        )
        Spacer(modifier = Modifier.height(20.dp))
        FeatureNavigationCard(
            title = "PROFILE",
            description = "Manage your settings and view your retro journey.",
            imageResId = PROFILE_CARD_IMAGE,
            buttonText = "VIEW PROFILE",
            onButtonClick = onNavigateToProfile,
            gradientColors = listOf(RetroGold, VaporwavePink.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.height(48.dp))
        CopyrightFooter(
            name = "Carlos Zabala",
            blogUrl = "https://charlysblog.framer.website"
        )
    }
}


// --- News Section Composable ---
// --- News Section Composable ---
@Composable
fun NewsSection(
    newsItems: List<NewsItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier // Added modifier parameter
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "LATEST NEWS",
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 22.sp, // Appropriate smaller size for a section title
                fontWeight = FontWeight.Bold,
                shadow = Shadow( // Enhanced shadow for better visibility
                    color = VaporwavePink.copy(alpha = 0.7f), // Same alpha as "HOME" title shadow
                    offset = Offset(x = 2.5f, y = 2.5f),    // Slightly smaller offset
                    blurRadius = 4f                       // Slightly smaller blur
                )
                // textAlign = TextAlign.Start is default for Text in a Column
            ),
            modifier = Modifier.padding(bottom = 12.dp) // Padding below the title
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp), // Give some space for the indicator
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
                        text = errorMessage, // Display the actual error message
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = SynthwaveOrange, // Error color
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
            newsItems.isEmpty() && !isLoading -> { // Check !isLoading here too
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp), // Give some space for the message
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No news articles found at the moment.",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.7f), // Subdued text color
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
                        .heightIn(max = 400.dp) // Constrain height for internal scrolling
                ) {
                    items(newsItems, key = { it.id }) { newsItem ->
                        NewsItemCard(newsItem = newsItem) // Your existing NewsItemCard
                        Divider(color = VaporwavePink.copy(alpha = 0.3f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}


// --- Individual News Item Card Composable ---
@Composable
fun NewsItemCard(newsItem: NewsItem, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // Open the article URL when the card is clicked
                if (newsItem.sourceUrl.isNotBlank()) {
                    try {
                        uriHandler.openUri(newsItem.sourceUrl)
                    } catch (e: Exception) {
                        Log.e("NewsItemCard", "Could not open URI: ${newsItem.sourceUrl}", e)
                        // Optionally show a toast to the user
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroDarkPurple.copy(alpha = 0.5f) // Semi-transparent card
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

// Helper function to format date (place it in this file or a utils file)
fun formatEpochMillisToReadableDate(epochMillis: Long): String {
    return try {
        val date = Date(epochMillis)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) // e.g., Oct 27, 2023
        format.format(date)
    } catch (e: Exception) {
        "Date N/A"
    }
}


// --- Welcome Section Composable (existing - no changes needed for news) ---
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

// --- Feature Navigation Card Composable (existing - no changes needed for news) ---
@Composable
fun FeatureNavigationCard(
    title: String,
    description: String,
    @DrawableRes imageResId: Int,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>
) {
    val cardShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(170.dp)
            .clip(cardShape)
            .background(Brush.horizontalGradient(colors = gradientColors))
            .border(BorderStroke(1.dp, RetroTextOffWhite.copy(alpha = 0.5f)), cardShape)
            .clickable(onClick = onButtonClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Column {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = Offset(x = 1f, y = 1f),
                                blurRadius = 2f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = description,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onButtonClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetroTextOffWhite,
                        contentColor = gradientColors.firstOrNull() ?: VaporwavePink
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontFamily = RetroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, RetroTextOffWhite.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            )
        }
    }
}

// --- Copyright Footer Composable (existing - no changes) ---
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
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
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
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E) // Dark retro background
@Composable
fun HomeScreenWithNewsPreview() {
    HubRetroTheme {
        HomeScreen(
            onNavigateToAlbums = { Log.d("Preview", "Navigate to Albums") },
            onNavigateToMagazines = { Log.d("Preview", "Navigate to Magazines") },
            onNavigateToArticles = { Log.d("Preview", "Navigate to Articles") },
            onNavigateToProfile = { Log.d("Preview", "Navigate to Profile") }
            // The ViewModel will be default-instantiated in preview, likely showing empty/loading state
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun NewsSection_LoadingPreview() {
    HubRetroTheme {
        NewsSection(newsItems = emptyList(), isLoading = true, errorMessage = null, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun NewsSection_ErrorPreview() {
    HubRetroTheme {
        NewsSection(newsItems = emptyList(), isLoading = false, errorMessage = "Network failed. Oof!", onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun NewsSection_EmptyPreview() {
    HubRetroTheme {
        NewsSection(newsItems = emptyList(), isLoading = false, errorMessage = null, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun NewsSection_WithItemsPreview() {
    HubRetroTheme {
        val sampleNews = listOf(
            NewsItem("1", "Retro Rewind: The Year 1990", "A look back at the pivotal games and hardware of 1990. So much nostalgia!", "RetroGamer Mag", "https://example.com/1", "https://picsum.photos/seed/1/300/200", System.currentTimeMillis() - 100000000, "Review"),
            NewsItem("2", "Hidden Gems on Obscure Consoles", "You won't believe these titles existed. Pure pixel magic, folks!", "Console Dreams", "https://example.com/2", "https://picsum.photos/seed/2/300/200", System.currentTimeMillis() - 200000000, "Feature")
        )
        NewsSection(newsItems = sampleNews, isLoading = false, errorMessage = null, onRetry = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun NewsItemCardPreview() {
    HubRetroTheme {
        NewsItemCard(
            newsItem = NewsItem(
                id = "prev1",
                title = "Amazing Pixel Art Game Re-released!",
                summary = "This long-lost classic is back and better than ever. Get your controllers ready for an epic adventure into the world of pixels and chiptunes. It's a blast from the past!",
                sourceName = "Pixel Times",
                sourceUrl = "https://example.com/article123",
                imageUrl = "https://picsum.photos/seed/preview/600/400", // Placeholder image URL
                publishedDate = System.currentTimeMillis() - 3600000 * 24 * 3, // 3 days ago
                category = "News"
            )
        )
    }
}


// Original previews (can be kept or removed if the new HomeScreenPreview is sufficient)

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HomeScreenPreview_WithNavCardsAndFooter() {
    HubRetroTheme {
        HomeScreen(
            onNavigateToAlbums = { Log.d("Preview", "Navigate to Albums") },
            onNavigateToMagazines = { Log.d("Preview", "Navigate to Magazines") },
            onNavigateToArticles = { Log.d("Preview", "Navigate to Articles") },
            onNavigateToProfile = { Log.d("Preview", "Navigate to Profile") }
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 200)
@Composable
fun FeatureNavigationCardPreview() {
    HubRetroTheme {
        Box(modifier = Modifier.background(Color(0xFF1A1A2E)).padding(16.dp)) {
            FeatureNavigationCard(
                title = "ALBUMS",
                description = "Groove to the classics. Soundtracks from legendary games await your ears.",
                imageResId = ALBUMS_CARD_IMAGE,
                buttonText = "TAKE ME THERE",
                onButtonClick = {},
                gradientColors = listOf(VaporwavePink, VaporwaveBlue.copy(alpha = 0.7f))
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun CopyrightFooterPreview() {
    HubRetroTheme {
        CopyrightFooter(name = "Carlos Zabala", blogUrl = "https://charlysblog.framer.website")
    }
}


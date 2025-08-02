package com.example.hubretro

import androidx.compose.animation.animateContentSize
// --- Animation Imports (existing) ---
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// --- End Animation Imports ---
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Ensure this specific items import is used
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.* // For remember, mutableStateOf, getValue, setValue, DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// Explicit getValue/setValue imports are fine but covered by the wildcard androidx.compose.runtime.*
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.hubretro.ui.theme.* // Make sure your theme definitions are here
// --- YouTube Player Imports ---
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

// 1. Data Class
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val date: String? = null,
    val imageResId: Int? = null,
    val youtubeVideoId: String? = null
)

// 2. Sample Article Data
val sampleArticles = listOf(
    ArticleItem(
        id = "1",
        title = "The Rise of Retro Gaming in 2024",
        snippet = "An exploration into the resurgence of classic video games and their impact on modern culture.",
        fullContent = "The year 2024 has seen an unprecedented boom in the popularity of retro gaming...",
        date = "Oct 26, 2023",
        imageResId = R.drawable.article1, // Replace with your actual drawable
        youtubeVideoId = "dQw4w9WgXcQ"
    ),
    ArticleItem(
        id = "2",
        title = "Vaporwave Aesthetics: Beyond the Music",
        snippet = "Delving into the visual elements of vaporwave...",
        fullContent = "Vaporwave is more than just a musical microgenre...",
        date = "Oct 15, 2023",
        imageResId = R.drawable.article2, // Replace with your actual drawable
        youtubeVideoId = "aQkPcPqTq4M"
    ),
    ArticleItem(
        id = "example_no_image_or_video",
        title = "Article Without Image or Video",
        snippet = "This article demonstrates how cards look when no media is provided.",
        fullContent = "Even without a captivating header image or video, the structure of an article card needs to be clear...",
        date = "Oct 10, 2023",
        imageResId = null,
        youtubeVideoId = null
    ),
    ArticleItem(
        id = "5",
        title = "Building Your Own Retro PC: A Guide",
        snippet = "Tips and tricks for sourcing parts and assembling a personal computer with a vintage feel.",
        fullContent = "There's a unique satisfaction in piecing together a computer that boots up...",
        date = "Aug 28, 2023",
        imageResId = R.drawable.article3, // Replace with your actual drawable
        youtubeVideoId = null
    ),
    ArticleItem(
        id = "only_video_example",
        title = "Gameplay Highlights: Retro Edition",
        snippet = "Check out these amazing retro gameplay moments.",
        fullContent = "This article features a collection of curated video clips showcasing some of the best retro gameplay experiences.",
        date = "Nov 01, 2023",
        imageResId = null, // No image, but has video
        youtubeVideoId = "L_Hgh7sPDLM"
    )
)

// --- YoutubePlayerCard --- (This remains unchanged from your provided code)
@Composable
fun YoutubePlayerCard(
    youtubeVideoId: String?,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val context = LocalContext.current
    val youTubePlayerView = remember { YouTubePlayerView(context) }

    DisposableEffect(lifecycleOwner, youtubeVideoId, youTubePlayerView) {
        if (youtubeVideoId.isNullOrBlank()) {
            youTubePlayerView.release()
            return@DisposableEffect onDispose {}
        }

        val playerListener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.cueVideo(youtubeVideoId, 0f)
            }
        }

        youTubePlayerView.enableAutomaticInitialization = false
        val playerOptions = IFramePlayerOptions.Builder().build()
        youTubePlayerView.initialize(playerListener, playerOptions)
        lifecycleOwner.lifecycle.addObserver(youTubePlayerView)

        onDispose {
            youTubePlayerView.release()
            lifecycleOwner.lifecycle.removeObserver(youTubePlayerView)
        }
    }

    if (!youtubeVideoId.isNullOrBlank()) {
        AndroidView(
            factory = { youTubePlayerView },
            modifier = modifier // The border will be applied to this modifier by the caller
        )
    } else {
        Box(modifier = modifier.background(Color.Transparent))
    }
}

// 3. ArticleCard Composable (MODIFIED: Added borders to Image and Video)
@Composable
fun ArticleCard(article: ArticleItem, modifier: Modifier = Modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val infiniteTransition = rememberInfiniteTransition(label = "shadow_wiggle_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "shadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "shadowOffsetY"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ArticleCardDarkGrey) // This is the main card background
            .border(width = 1.dp, color = RetroBorderColor) // This is the main card border
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        // --- 1. Image Section (Top) ---
        article.imageResId?.let { imageRes ->
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Header image for ${article.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp) // Padding AROUND the image
                    .border(1.dp, VaporwavePink), // Border for the image itself
                contentScale = ContentScale.Crop
            )
            Divider(
                color = RetroBorderColor.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // --- 2. Text Content Section (Middle) ---
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = if (article.imageResId == null) 16.dp else 8.dp,
                bottom = 8.dp
            )
        ) {
            Text(
                text = article.title.uppercase(),
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        color = VaporwavePink,
                        offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                        blurRadius = 0.5f
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) article.fullContent else article.snippet,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isExpanded) "READ LESS..." else "READ MORE...",
                fontFamily = RetroFontFamily,
                color = VaporwavePink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )

            article.date?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Published: $it",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        } // End of Text Content Section

        // --- 3. YouTube Video Section (Bottom) ---
        val hasVideo = !article.youtubeVideoId.isNullOrBlank()
        val showVideoPlayer = isExpanded && hasVideo

        if (showVideoPlayer) {
            Divider(
                color = RetroBorderColor.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
            )
            YoutubePlayerCard(
                youtubeVideoId = article.youtubeVideoId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp) // Padding AROUND the player
                    .border(1.dp, VaporwavePink), // Border for the YouTube player
                lifecycleOwner = lifecycleOwner
            )
        } else if (!isExpanded && hasVideo && article.imageResId == null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Divider(
                    color = RetroBorderColor.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Video available when expanded",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}


// 4. ArticlesScreen Composable (MODIFIED: Added shadow to main title)
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier,
    articles: List<ArticleItem> = sampleArticles
) {
    // --- Animation for the main screen title shadow ---
    val infiniteTransition = rememberInfiniteTransition(label = "screen_title_shadow_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f, // You can adjust these values if needed
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "screenTitleShadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f, // You can adjust these values if needed
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "screenTitleShadowOffsetY"
    )
    // --- End Animation ---

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // Overall padding for the screen content
    ) {
        Text(
            text = "LATEST ARTICLES",
            fontFamily = RetroFontFamily,
            color = RetroTextOffWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle( // Apply the shadow here
                shadow = Shadow(
                    color = VaporwavePink,
                    offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                    blurRadius = 0.5f // Consistent blurRadius
                )
            ),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = articles,
                key = { article -> article.id } // Key for item stability
            ) { article ->
                ArticleCard(article = article)
            }
        }
    }
}

// 5. Previews (These remain unchanged but will reflect the new borders in the actual UI)
@Preview(showBackground = true, backgroundColor = 0xFF000020 /* Dark blue like RetroDarkBlue */)
@Composable
fun ArticleCardPreviewWithVideo() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue).padding(16.dp)) {
            val previewArticle = sampleArticles.firstOrNull { it.youtubeVideoId != null && it.imageResId != null }
                ?: sampleArticles.firstOrNull { it.youtubeVideoId != null} // Corrected fallback
                ?: sampleArticles.first()
            ArticleCard(article = previewArticle)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000020)
@Composable
fun ArticleCardPreviewWithImage() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue).padding(16.dp)) {
            val previewArticle = sampleArticles.firstOrNull { it.imageResId != null && it.youtubeVideoId == null }
                ?: sampleArticles.firstOrNull {it.imageResId != null} // Corrected fallback
                ?: sampleArticles.first() // Absolute fallback
            ArticleCard(article = previewArticle)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000020)
@Composable
fun ArticleCardPreviewNoMedia() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue).padding(16.dp)) {
            val previewArticle = sampleArticles.firstOrNull { it.imageResId == null && it.youtubeVideoId == null }
                ?: ArticleItem("no-media-fallback", "Text Only Article", "Snippet...", "Full content...", date="Some Date")
            ArticleCard(article = previewArticle)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000020)
@Composable
fun ArticleCardPreviewOnlyVideoNoImage() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue).padding(16.dp)) {
            val previewArticle = sampleArticles.firstOrNull { it.youtubeVideoId != null && it.imageResId == null }
                ?: ArticleItem("only-video-fallback", "Video, No Image", "Snippet...", "Full content...", youtubeVideoId = "someID")
            ArticleCard(article = previewArticle)
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000020)
@Composable
fun ArticlesScreenPreview() {
    HubRetroTheme {
        Box(Modifier.background(RetroDarkBlue)) {
            ArticlesScreen(articles = sampleArticles)
        }
    }
}

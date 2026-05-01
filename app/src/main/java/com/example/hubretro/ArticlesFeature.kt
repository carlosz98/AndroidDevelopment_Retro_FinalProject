package com.example.hubretro


import androidx.compose.material3.OutlinedButton
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.tasks.await
import java.util.UUID

// 1. Data Class — added headerImageUrl
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val date: String? = null,
    val author: String? = null,
    val imageResId: Int? = null,
    val imageUrl: String? = null, // Firebase Storage or URL
    val youtubeVideoId: String? = null
)

// 2. Sample Articles
val sampleArticles = listOf(
    ArticleItem(
        id = "1",
        title = "The Pixelated Pull: Why Retro Gaming is Booming Again",
        snippet = "Beyond nostalgia, discover the reasons for the resurgence of classic video games and their timeless appeal in a modern world.",
        fullContent = """
            The year 2024 isn't just about the next generation of hyper-realistic graphics; it's also witnessing an unprecedented boom in the popularity of retro gaming. From dusty attics to digital storefronts, classic titles from the 80s, 90s, and early 2000s are capturing the hearts of both seasoned gamers and a new generation of players. But what's fueling this pixelated renaissance?

            **More Than Just Memory Lane:**
            While nostalgia is undoubtedly a powerful catalyst, the retro revival runs deeper. For many who grew up with these games, it's a comforting return to simpler times, a way to reconnect with cherished childhood memories and the joy of discovering virtual worlds with friends.

            **The Allure of Simplicity and Challenge:**
            In an era of sprawling open worlds and complex game mechanics, retro games offer a refreshing directness. They often feature straightforward objectives, intuitive controls, and a level of challenge that demands skill and perseverance.

            **Accessibility and Community:**
            The rise of emulation, dedicated retro consoles, and online communities has made these classics more accessible than ever.

            **Timeless Design and Innovation:**
            Many retro games are lauded for their innovative game design and artistic vision, achieved despite significant hardware limitations.
        """.trimIndent(),
        date = "Nov 15, 2023",
        author = "Don Carlos",
        imageResId = R.drawable.article1,
        youtubeVideoId = "fuSRjyR_ZJU"
    ),
    ArticleItem(
        id = "2",
        title = "The Digital Ghosts: Exploring the World of Abandonware",
        snippet = "Unearthing lost classics and forgotten gems from the digital past. What happens when software is left behind?",
        fullContent = """
            In the fast-paced world of software development, titles that once graced magazine covers and topped sales charts can eventually fade into obscurity. This is the realm of **abandonware**: software that is no longer commercially available and for which official support has ceased.

            **What Qualifies as Abandonware?**
            The definition can be murky, as copyright technically still applies to most of these works. Generally, software is considered abandonware if it's no longer sold through official channels.

            **Why the Enduring Appeal?**
            The fascination with abandonware stems from nostalgia, historical significance, accessibility, and the dedication of fan communities who archive and preserve these games.

            **The Preservationist's Dilemma:**
            Abandonware communities serve as unofficial digital archaeologists, ensuring that important pieces of software history aren't lost to time.
        """.trimIndent(),
        date = "July 29, 2025",
        author = "Topin99",
        imageResId = R.drawable.article2,
        youtubeVideoId = "onP3tHaHmQs"
    ),
    ArticleItem(
        id = "3",
        title = "The Serene Symphony: Minecraft's Enduring Soundtrack",
        snippet = "Exploring the subtle genius of C418's compositions and how they define the Minecraft experience.",
        fullContent = """
            Beyond the blocky landscapes and endless creative possibilities, one of the most iconic aspects of Minecraft is its unique soundtrack, composed by Daniel Rosenfeld, also known as C418.

            **A World of Calm and Wonder:**
            Unlike the bombastic scores of many action-packed games, Minecraft's music is predominantly ambient, minimalist, and deeply atmospheric.

            **The Genius of Subtlety:**
            The power of C418's work lies in its subtlety. The music often fades in and out, never overstaying its welcome or becoming intrusive.

            **An Enduring Legacy:**
            Even as Minecraft has evolved, C418's original compositions remain the heart and soul of the game's auditory identity.
        """.trimIndent(),
        date = "July 28, 2025",
        author = "HomicidalYellio",
        imageResId = R.drawable.article3,
        youtubeVideoId = "9EvH-2e5at4"
    ),
    ArticleItem(
        id = "4",
        title = "Why Modern Games Embrace the Low-Polygon Aesthetic",
        snippet = "Exploring the resurgence of low-poly graphics as a deliberate and impactful art style.",
        fullContent = """
            In an era where photorealism often dominates gaming, a distinct trend has emerged: the deliberate use of low-polygon aesthetics.

            **What is Low-Poly?**
            The term refers to 3D models constructed with a relatively small number of polygons, resulting in a distinct stylized look with sharp edges and flat shading.

            **Beyond Nostalgia:**
            The modern resurgence of low-poly is driven by artistic expression, performance benefits, clarity, and faster development cycles.
        """.trimIndent(),
        date = "Aug 1, 2025",
        author = "Carollerm",
        imageResId = R.drawable.article4,
        youtubeVideoId = "9E0XPzB9wZU"
    ),
    ArticleItem(
        id = "5",
        title = "Why Pixel Art is Still Gorgeous",
        snippet = "Exploring the timeless appeal of pixel art and why this art form continues to captivate.",
        fullContent = """
            What began as a necessity due to hardware limitations has evolved into a deliberate and beloved art style.

            **The Beauty in Limitation:**
            Every pixel is placed with intention. Artists must make careful choices about color palettes, shading, and form.

            **A Thriving Modern Art Form:**
            Titles like Stardew Valley, Celeste, and Hyper Light Drifter are testaments to the modern power of pixel art.
        """.trimIndent(),
        date = "July 12, 2025",
        author = "LadiesMan61",
        imageResId = R.drawable.article5,
        youtubeVideoId = "lT9VVMF10Hk"
    ),
    ArticleItem(
        id = "6",
        title = "The Enduring Nostalgia of Habbo Hotel",
        snippet = "A look back at Habbo Hotel and why its pixelated world still holds a special place in our hearts.",
        fullContent = """
            For a certain generation, the words "Bobba," "Furni," and "Pool's Closed" evoke an instant wave of nostalgia.

            **A Pixelated Universe:**
            Launched in 2000 by Sulake, Habbo Hotel offered users the ability to create avatars, design rooms, play games, and chat with people from around the globe.

            **Why We Still Remember It:**
            Habbo fostered a sense of community, creative expression, and early online identity.
        """.trimIndent(),
        date = "August 05, 2024",
        author = "Fabriko98",
        imageResId = R.drawable.article6,
        youtubeVideoId = "RCATF_Y3VAE"
    )
)

// 3. YouTube Player Card
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
        AndroidView(factory = { youTubePlayerView }, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color.Transparent))
    }
}

// 4. Styled Article Content
@Composable
fun StyledArticleContentWithLargeInitial(
    text: String,
    defaultStyle: TextStyle,
    subheadingStyle: SpanStyle,
    largeInitialStyle: SpanStyle
) {
    if (text.isEmpty()) {
        Text("", style = defaultStyle)
        return
    }
    val annotatedString = buildAnnotatedString {
        withStyle(style = largeInitialStyle) { append(text.first()) }
        val restOfText = text.substring(1)
        val regex = """\*\*(.*?)\*\*""".toRegex()
        var lastIndex = 0
        regex.findAll(restOfText).forEach { matchResult ->
            val subheadingText = matchResult.groups[1]?.value ?: ""
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            if (startIndex > lastIndex) {
                withStyle(defaultStyle.toSpanStyle()) {
                    append(restOfText.substring(lastIndex, startIndex))
                }
            }
            withStyle(style = subheadingStyle) { append(subheadingText) }
            lastIndex = endIndex
        }
        if (lastIndex < restOfText.length) {
            withStyle(defaultStyle.toSpanStyle()) {
                append(restOfText.substring(lastIndex))
            }
        }
    }
    Text(text = annotatedString, style = defaultStyle)
}

// 5. Community Article Card — now supports imageUrl too
@Composable
fun ArticleCard(
    article: ArticleItem,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val infiniteTransition = rememberInfiniteTransition(label = "card_shadow_wiggle_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "cardShadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "cardShadowOffsetY"
    )

    val cardShape = RoundedCornerShape(12.dp)
    val hasImage = article.imageResId != null || !article.imageUrl.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.linearGradient(gradientColors))
            .border(width = 1.dp, color = RetroBorderColor.copy(alpha = 0.5f), shape = cardShape)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        // Header image — drawable or URL
        if (hasImage) {
            Box {
                if (article.imageResId != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = article.imageResId),
                        contentDescription = "Header image for ${article.title}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                            .border(1.dp, VaporwavePink),
                        contentScale = ContentScale.Crop
                    )
                } else if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = "Header image for ${article.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A2A3A))
                            .border(1.dp, VaporwavePink)
                    )
                }
                // Bookmark overlay on image
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 20.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isBookmarked) VaporwavePink
                            else RetroTextOffWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Divider(
                color = RetroTextOffWhite.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bookmark button when no image
        if (!hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier.background(
                        Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                ) {
                    IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark
                            else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isBookmarked) VaporwavePink
                            else RetroTextOffWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(
                start = 16.dp, end = 16.dp,
                top = if (!hasImage) 4.dp else 8.dp,
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
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                        blurRadius = 1.5f
                    )
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded) {
                val defaultFullContentStyle = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                StyledArticleContentWithLargeInitial(
                    text = article.fullContent,
                    defaultStyle = defaultFullContentStyle,
                    subheadingStyle = SpanStyle(
                        fontFamily = RetroFontFamily,
                        color = VaporwavePink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                    largeInitialStyle = defaultFullContentStyle.toSpanStyle().copy(
                        fontSize = defaultFullContentStyle.fontSize?.times(2.5) ?: 35.sp,
                        fontWeight = FontWeight.Bold,
                        color = RetroTextOffWhite,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(1f, 1f),
                            blurRadius = 1f
                        )
                    )
                )
            } else {
                Text(
                    text = article.snippet,
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            }

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
                    color = RetroTextOffWhite.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            article.author?.let { authorName ->
                Spacer(modifier = Modifier.height(if (article.date != null) 4.dp else 8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )) { append("Written by: ") }
                        withStyle(SpanStyle(
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )) { append(authorName) }
                    }
                )
            }
        }

        val hasVideo = !article.youtubeVideoId.isNullOrBlank()
        val showVideoPlayer = isExpanded && hasVideo

        if (showVideoPlayer) {
            Divider(
                color = RetroTextOffWhite.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            YoutubePlayerCard(
                youtubeVideoId = article.youtubeVideoId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .border(1.dp, VaporwavePink),
                lifecycleOwner = lifecycleOwner
            )
        } else if (!isExpanded && hasVideo && !hasImage) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Divider(
                    color = RetroTextOffWhite.copy(alpha = 0.2f),
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

// 6. Archive Article Card
@Composable
fun ArchiveArticleCard(
    item: ArchiveItem,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {}
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "archive_card_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "archiveShadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "archiveShadowOffsetY"
    )

    val cardShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(Brush.linearGradient(gradientColors))
            .border(width = 1.dp, color = RetroBorderColor.copy(alpha = 0.5f), shape = cardShape)
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Box {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A3A))
                    .border(1.dp, VaporwavePink)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 20.dp, end = 20.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            ) {
                IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark
                        else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isBookmarked) VaporwavePink
                        else RetroTextOffWhite.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Divider(
            color = RetroTextOffWhite.copy(alpha = 0.3f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(VaporwaveCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "INTERNET ARCHIVE",
                    fontFamily = RetroFontFamily,
                    color = VaporwaveCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title.uppercase(),
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                        blurRadius = 1.5f
                    )
                ),
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isExpanded && item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = if (isExpanded) "READ LESS..." else "READ MORE...",
                fontFamily = RetroFontFamily,
                color = VaporwavePink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )

            item.creator?.let { creator ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )) { append("By: ") }
                        withStyle(SpanStyle(
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )) { append(creator) }
                    }
                )
            }

            item.year?.let { year ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Year: $year",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.webUrl))
                        try { context.startActivity(intent) } catch (e: Exception) { }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VaporwavePink),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "READ ON INTERNET ARCHIVE",
                        fontFamily = RetroFontFamily,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 7. Image Picker composable — gallery or URL
@Composable
fun ArticleImagePicker(
    headerImageUri: Uri?,
    headerImageUrl: String,
    onGalleryImagePicked: (Uri) -> Unit,
    onUrlChanged: (String) -> Unit,
    onClearImage: () -> Unit,
    isUploading: Boolean
) {
    var selectedTab by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { onGalleryImagePicked(it) } }

    Column {
        Text(
            text = "HEADER IMAGE (optional)",
            fontFamily = RetroFontFamily,
            color = VaporwavePink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF12122A),
            contentColor = VaporwavePink
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "GALLERY",
                            fontFamily = RetroFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "URL",
                            fontFamily = RetroFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Gallery picker
                if (headerImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, VaporwavePink, RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = headerImageUri,
                            contentDescription = "Header image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = VaporwavePink,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Uploading...",
                                        fontFamily = RetroFontFamily,
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        // Remove button
                        IconButton(
                            onClick = onClearImage,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VaporwavePink)
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = VaporwavePink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CHANGE IMAGE",
                            fontFamily = RetroFontFamily,
                            fontSize = 11.sp,
                            color = VaporwavePink
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF12122A))
                            .border(
                                1.dp,
                                RetroTextOffWhite.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = RetroTextOffWhite.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "TAP TO PICK FROM GALLERY",
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.4f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            1 -> {
                // URL input
                OutlinedTextField(
                    value = headerImageUrl,
                    onValueChange = onUrlChanged,
                    placeholder = {
                        Text(
                            "https://example.com/image.jpg",
                            fontFamily = RetroFontFamily,
                            fontSize = 12.sp,
                            color = RetroTextOffWhite.copy(alpha = 0.3f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = null,
                            tint = VaporwavePink,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (headerImageUrl.isNotBlank()) {
                            IconButton(onClick = { onUrlChanged("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear URL",
                                    tint = RetroTextOffWhite.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = RetroFontFamily,
                        fontSize = 12.sp,
                        color = RetroTextOffWhite
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaporwavePink,
                        unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                        focusedContainerColor = Color(0xFF12122A),
                        unfocusedContainerColor = Color(0xFF12122A),
                        cursorColor = VaporwavePink,
                        focusedTextColor = RetroTextOffWhite,
                        unfocusedTextColor = RetroTextOffWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // URL preview
                if (headerImageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, VaporwavePink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = headerImageUrl,
                            contentDescription = "Image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "PREVIEW",
                            fontFamily = RetroFontFamily,
                            color = Color.White,
                            fontSize = 9.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// 8. Article Editor Screen
@Composable
fun ArticleEditorScreen(
    authViewModel: AuthViewModel,
    userArticlesViewModel: UserArticlesViewModel,
    activityViewModel: ActivityViewModel? = null,
    onBack: () -> Unit
) {
    val firebaseProfile by authViewModel.userProfile.collectAsState()
    val publishState by userArticlesViewModel.publishState.collectAsState()

    var title by remember { mutableStateOf("") }
    var snippet by remember { mutableStateOf("") }
    var fullContent by remember { mutableStateOf("") }
    var youtubeVideoId by remember { mutableStateOf("") }
    var showPreview by remember { mutableStateOf(false) }

    // Image state
    var headerImageUri by remember { mutableStateOf<Uri?>(null) }
    var headerImageUrl by remember { mutableStateOf("") }
    var uploadedImageUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    // Auto navigate back on success
    LaunchedEffect(publishState) {
        if (publishState is UserArticlesViewModel.PublishState.Success) {
            userArticlesViewModel.resetPublishState()
            onBack()
        }
    }

    // Upload image to Firebase Storage when URI is picked
    val context = LocalContext.current
    LaunchedEffect(headerImageUri) {
        val uri = headerImageUri ?: return@LaunchedEffect
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        isUploading = true
        try {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("article_images/$uid/${UUID.randomUUID()}.jpg")
            storageRef.putFile(uri).await()
            uploadedImageUrl = storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            uploadedImageUrl = ""
        } finally {
            isUploading = false
        }
    }

    // Final image URL to use — uploaded takes priority over manual URL
    val finalImageUrl = when {
        uploadedImageUrl.isNotBlank() -> uploadedImageUrl
        headerImageUrl.isNotBlank() -> headerImageUrl
        else -> ""
    }

    val isValid = title.isNotBlank() && snippet.isNotBlank() && fullContent.isNotBlank() && !isUploading

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.my_retro_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // --- Top Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = RetroTextOffWhite
                    )
                }
                Text(
                    text = if (showPreview) "PREVIEW" else "WRITE ARTICLE",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { showPreview = !showPreview },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showPreview) VaporwavePink
                        else RetroDarkPurple.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (showPreview) "EDIT" else "PREVIEW",
                        fontFamily = RetroFontFamily,
                        fontSize = 11.sp,
                        color = RetroTextOffWhite
                    )
                }
            }

            Divider(color = VaporwavePink.copy(alpha = 0.3f))

            if (showPreview) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "THIS IS HOW YOUR ARTICLE WILL LOOK:",
                            fontFamily = RetroFontFamily,
                            color = VaporwaveCyan.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ArticleCard(
                            article = ArticleItem(
                                id = "preview",
                                title = title.ifBlank { "Your Title Here" },
                                snippet = snippet.ifBlank { "Your snippet here..." },
                                fullContent = fullContent.ifBlank { "Your full content here..." },
                                date = "Today",
                                author = firebaseProfile?.username ?: "You",
                                imageUrl = finalImageUrl.ifBlank { null },
                                youtubeVideoId = youtubeVideoId.ifBlank { null }
                            ),
                            gradientColors = articleGradientColorsList[0],
                            initiallyExpanded = false
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Image Picker
                    ArticleImagePicker(
                        headerImageUri = headerImageUri,
                        headerImageUrl = headerImageUrl,
                        onGalleryImagePicked = { uri ->
                            headerImageUri = uri
                            headerImageUrl = "" // clear URL if gallery picked
                            uploadedImageUrl = ""
                        },
                        onUrlChanged = { url ->
                            headerImageUrl = url
                            headerImageUri = null // clear gallery if URL entered
                            uploadedImageUrl = ""
                        },
                        onClearImage = {
                            headerImageUri = null
                            headerImageUrl = ""
                            uploadedImageUrl = ""
                        },
                        isUploading = isUploading
                    )

                    // Title
                    ArticleEditorField(
                        value = title,
                        onValueChange = { title = it },
                        label = "TITLE *",
                        placeholder = "Enter your article title...",
                        singleLine = true,
                        accentColor = VaporwavePink
                    )

                    // Snippet
                    ArticleEditorField(
                        value = snippet,
                        onValueChange = { snippet = it },
                        label = "SNIPPET *",
                        placeholder = "A short summary that appears as preview (2-3 sentences)...",
                        singleLine = false,
                        minLines = 3,
                        accentColor = VaporwavePink
                    )

                    // Full Content
                    Column {
                        Text(
                            text = "FULL CONTENT *",
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tip: Use **text** to create bold section headings",
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ArticleEditorField(
                            value = fullContent,
                            onValueChange = { fullContent = it },
                            label = "",
                            placeholder = "Write your full article here...\n\n**Section Heading:**\nYour content under this section...",
                            singleLine = false,
                            minLines = 8,
                            accentColor = VaporwavePink
                        )
                    }

                    // YouTube Video ID
                    Column {
                        ArticleEditorField(
                            value = youtubeVideoId,
                            onValueChange = { youtubeVideoId = it },
                            label = "YOUTUBE VIDEO ID (optional)",
                            placeholder = "e.g. dQw4w9WgXcQ",
                            singleLine = true,
                            accentColor = VaporwaveCyan
                        )
                        Text(
                            text = "Paste just the video ID from the YouTube URL (the part after ?v=)",
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // --- Publish Button ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12122A).copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        userArticlesViewModel.publishArticle(
                            title = title.trim(),
                            snippet = snippet.trim(),
                            fullContent = fullContent.trim(),
                            youtubeVideoId = youtubeVideoId.trim(),
                            headerImageUrl = finalImageUrl,
                            username = firebaseProfile?.username ?: "Anonymous",
                            activityViewModel = activityViewModel
                        )
                    },
                    enabled = isValid && publishState !is UserArticlesViewModel.PublishState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VaporwavePink,
                        disabledContainerColor = RetroTextOffWhite.copy(alpha = 0.2f)
                    )
                ) {
                    if (publishState is UserArticlesViewModel.PublishState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "PUBLISH ARTICLE",
                            fontFamily = RetroFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Reusable editor field
@Composable
fun ArticleEditorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean,
    accentColor: Color,
    minLines: Int = 1
) {
    Column {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontFamily = RetroFontFamily,
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    fontFamily = RetroFontFamily,
                    fontSize = 12.sp,
                    color = RetroTextOffWhite.copy(alpha = 0.3f),
                    lineHeight = 18.sp
                )
            },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
            textStyle = TextStyle(
                fontFamily = RetroFontFamily,
                fontSize = 13.sp,
                color = RetroTextOffWhite,
                lineHeight = 20.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = RetroTextOffWhite.copy(alpha = 0.3f),
                focusedContainerColor = Color(0xFF12122A),
                unfocusedContainerColor = Color(0xFF12122A),
                cursorColor = accentColor,
                focusedTextColor = RetroTextOffWhite,
                unfocusedTextColor = RetroTextOffWhite
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 9. Articles Screen
@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = null,
    authViewModel: AuthViewModel = viewModel(),
    activityViewModel: ActivityViewModel? = null,
    userArticlesViewModel: UserArticlesViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val articlesState by contentViewModel.articlesState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val userArticles by userArticlesViewModel.userArticles.collectAsState()

    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState()
        ?: remember { mutableStateOf(emptySet<String>()) })

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        kotlinx.coroutines.delay(600)
        if (searchQuery != lastSearched) {
            lastSearched = searchQuery
            contentViewModel.fetchArticles(searchQuery)
        }
    }

    val userArticleItems = remember(userArticles) {
        userArticles.map { it.toArticleItem() }
    }

    val allCommunityArticles = remember(userArticleItems, searchQuery) {
        val combined = sampleArticles + userArticleItems
        if (searchQuery.isBlank()) combined
        else combined.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.snippet.contains(searchQuery, ignoreCase = true) ||
                    it.author?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "screen_title_shadow_transition")
    val shadowOffsetX by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "screenTitleShadowOffsetX"
    )
    val shadowOffsetY by infiniteTransition.animateFloat(
        initialValue = 4.5f, targetValue = 3.5f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "screenTitleShadowOffsetY"
    )

    if (showEditor) {
        ArticleEditorScreen(
            authViewModel = authViewModel,
            userArticlesViewModel = userArticlesViewModel,
            activityViewModel = activityViewModel,
            onBack = { showEditor = false }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.size(40.dp))
                Text(
                    text = "ARTICLES",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = VaporwavePink.copy(alpha = 0.7f),
                            offset = Offset(x = shadowOffsetX, y = shadowOffsetY),
                            blurRadius = 0.5f
                        )
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) {
                            searchQuery = ""
                            focusManager.clearFocus()
                            contentViewModel.fetchArticles()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = if (searchVisible) "Close search" else "Search articles",
                        tint = if (searchVisible) VaporwavePink else RetroTextOffWhite,
                        modifier = Modifier.size(24.dp)
                    )
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
                            "Search by title, topic or author...",
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
                                contentViewModel.fetchArticles()
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
                        .padding(bottom = 12.dp)
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (allCommunityArticles.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMUNITY",
                                fontFamily = RetroFontFamily,
                                color = VaporwavePink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = VaporwavePink.copy(alpha = 0.3f)
                            )
                        }
                    }
                    itemsIndexed(
                        items = allCommunityArticles,
                        key = { _, article -> article.id }
                    ) { index, article ->
                        ArticleCard(
                            article = article,
                            gradientColors = articleGradientColorsList[index % articleGradientColorsList.size],
                            initiallyExpanded = false,
                            isBookmarked = favoriteIds.contains(article.id),
                            onBookmarkToggle = {
                                favoritesViewModel?.toggleFavorite(
                                    FavoriteItem(
                                        id = article.id,
                                        title = article.title,
                                        description = article.snippet,
                                        thumbnailUrl = article.imageUrl,
                                        webUrl = "",
                                        category = "ARTICLE",
                                        creator = article.author,
                                        year = article.date
                                    )
                                )
                            }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (allCommunityArticles.isNotEmpty()) 8.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTERNET ARCHIVE",
                            fontFamily = RetroFontFamily,
                            color = VaporwaveCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = VaporwaveCyan.copy(alpha = 0.3f)
                        )
                    }
                }

                when (val state = articlesState) {
                    is ContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = VaporwaveCyan,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Loading articles...",
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
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
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
                                        onClick = { contentViewModel.fetchArticles() },
                                        colors = ButtonDefaults.buttonColors(containerColor = VaporwaveCyan),
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
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> "archive_${item.id}" }
                        ) { index, item ->
                            ArchiveArticleCard(
                                item = item,
                                gradientColors = articleGradientColorsList[
                                    (index + allCommunityArticles.size) % articleGradientColorsList.size
                                ],
                                isBookmarked = favoriteIds.contains(item.id),
                                onBookmarkToggle = {
                                    favoritesViewModel?.toggleFavorite(item.toFavoriteItem())
                                }
                            )
                        }
                    }
                    else -> { }
                }
            }
        }

        if (currentUser != null) {
            FloatingActionButton(
                onClick = { showEditor = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = VaporwavePink,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Write article",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF2A2A3D, name = "Articles Screen")
@Composable
fun ArticlesScreenPreviewDarkContext() {
    HubRetroTheme {
        ArticlesScreen()
    }
}
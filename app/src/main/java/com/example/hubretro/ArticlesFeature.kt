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
import androidx.compose.ui.graphics.Color
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

// 1. Data Class
data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val date: String? = null,
    val author: String? = null,
    val imageResId: Int? = null,
    val imageUrl: String? = null,
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
            While nostalgia is undoubtedly a powerful catalyst, the retro revival runs deeper. For many who grew up with these games, it's a comforting return to simpler times.

            **The Allure of Simplicity and Challenge:**
            In an era of sprawling open worlds and complex game mechanics, retro games offer a refreshing directness.

            **Accessibility and Community:**
            The rise of emulation, dedicated retro consoles, and online communities has made these classics more accessible than ever.
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
            In the fast-paced world of software development, titles that once graced magazine covers and topped sales charts can eventually fade into obscurity. This is the realm of abandonware.

            **What Qualifies as Abandonware?**
            The definition can be murky, as copyright technically still applies to most of these works.

            **Why the Enduring Appeal?**
            The fascination with abandonware stems from nostalgia, historical significance, and the dedication of fan communities.
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
            Beyond the blocky landscapes and endless creative possibilities, one of the most iconic aspects of Minecraft is its unique soundtrack by C418.

            **A World of Calm and Wonder:**
            Unlike the bombastic scores of many action-packed games, Minecraft's music is predominantly ambient and deeply atmospheric.

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
            The term refers to 3D models constructed with a relatively small number of polygons.

            **Beyond Nostalgia:**
            The modern resurgence of low-poly is driven by artistic expression, performance benefits, and faster development cycles.
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
            Titles like Stardew Valley, Celeste, and Hyper Light Drifter prove pixel art's power.
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
            For a certain generation, the words Bobba, Furni, and Pool's Closed evoke an instant wave of nostalgia.

            **A Pixelated Universe:**
            Launched in 2000 by Sulake, Habbo Hotel offered users the ability to create avatars, design rooms, and chat globally.

            **Why We Still Remember It:**
            Habbo fostered a sense of community, creative expression, and early online identity.
        """.trimIndent(),
        date = "August 05, 2024",
        author = "Fabriko98",
        imageResId = R.drawable.article6,
        youtubeVideoId = "RCATF_Y3VAE"
    )
)

// 3. YouTube Player
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

// 5. ✅ Scrapbook Article Card
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
    val hasImage = article.imageResId != null || !article.imageUrl.isNullOrBlank()

    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .animateContentSize(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 4.dp
        ) {
            Column {
                // Image
                if (hasImage) {
                    Box {
                        if (article.imageResId != null) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = article.imageResId),
                                contentDescription = article.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else if (!article.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = article.imageUrl,
                                contentDescription = article.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    .background(ScrapbookPaper)
                            )
                        }
                        // Bookmark on image
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(ScrapbookYellow)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                        ) {
                            IconButton(
                                onClick = onBookmarkToggle,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                    else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = ScrapbookDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {

                    // No-image bookmark
                    if (!hasImage) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ScrapbookYellow)
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "★ Magazine",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookDark,
                                    fontSize = 10.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ScrapbookYellow)
                                    .border(2.dp, ScrapbookBorder, CircleShape)
                            ) {
                                IconButton(
                                    onClick = onBookmarkToggle,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                        else Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = ScrapbookDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Title
                    Text(
                        text = article.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 22.sp,
                        letterSpacing = 0.5.sp,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Date + Author row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        article.date?.let {
                            Text(
                                text = it,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        article.author?.let {
                            Text(
                                text = it,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Divider
                    Divider(color = ScrapbookBorder.copy(alpha = 0.15f), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Content
                    if (isExpanded) {
                        StyledArticleContentWithLargeInitial(
                            text = article.fullContent,
                            defaultStyle = TextStyle(
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextDark,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            ),
                            subheadingStyle = SpanStyle(
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 18.sp
                            ),
                            largeInitialStyle = SpanStyle(
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 36.sp
                            )
                        )
                    } else {
                        Text(
                            text = article.snippet,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Read More button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "READ LESS" else "READ MORE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // YouTube player
                if (isExpanded && !article.youtubeVideoId.isNullOrBlank()) {
                    Divider(
                        color = ScrapbookBorder.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    YoutubePlayerCard(
                        youtubeVideoId = article.youtubeVideoId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .padding(16.dp)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)),
                        lifecycleOwner = lifecycleOwner
                    )
                }
            }
        }
    }
}

// 6. ✅ Scrapbook Archive Article Card
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

    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .animateContentSize(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 4.dp
        ) {
            Column {
                // Thumbnail
                Box {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(ScrapbookPaper)
                    )
                    // Archive badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ScrapbookDark)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "INTERNET ARCHIVE",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ScrapbookYellow,
                            fontSize = 9.sp
                        )
                    }
                    // Bookmark
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                    ) {
                        IconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark
                                else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = ScrapbookDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = item.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        item.creator?.let {
                            Text(
                                text = it,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        item.year?.let {
                            Text(
                                text = it,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = ScrapbookBorder.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookYellow)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                .clickable { isExpanded = !isExpanded }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "READ LESS" else "READ MORE",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 14.sp
                            )
                        }

                        if (isExpanded) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ScrapbookDark)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(item.webUrl)
                                        )
                                        try { context.startActivity(intent) }
                                        catch (e: Exception) { }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "OPEN →",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. Image Picker
@Composable
fun ArticleImagePicker(
    headerImageUri: android.net.Uri?,
    headerImageUrl: String,
    onGalleryImagePicked: (android.net.Uri) -> Unit,
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
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ScrapbookPaper,
            contentColor = ScrapbookDark
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
                            fontFamily = BangersFontFamily,
                            fontSize = 14.sp
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
                            fontFamily = BangersFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                if (headerImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
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
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ScrapbookYellow,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onClearImage,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = ScrapbookDark,
                                modifier = Modifier
                                    .background(ScrapbookYellow, CircleShape)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable { launcher.launch("image/*") }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CHANGE IMAGE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable { launcher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = ScrapbookDark.copy(alpha = 0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "TAP TO PICK FROM GALLERY",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            1 -> {
                OutlinedTextField(
                    value = headerImageUrl,
                    onValueChange = onUrlChanged,
                    placeholder = {
                        Text(
                            "https://example.com/image.jpg",
                            fontFamily = NunitoFontFamily,
                            fontSize = 12.sp,
                            color = ScrapbookTextMuted
                        )
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = NunitoFontFamily,
                        fontSize = 13.sp,
                        color = ScrapbookTextDark
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookDark,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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

    var headerImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var headerImageUrl by remember { mutableStateOf("") }
    var uploadedImageUrl by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(publishState) {
        if (publishState is UserArticlesViewModel.PublishState.Success) {
            userArticlesViewModel.resetPublishState()
            onBack()
        }
    }

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

    val finalImageUrl = when {
        uploadedImageUrl.isNotBlank() -> uploadedImageUrl
        headerImageUrl.isNotBlank() -> headerImageUrl
        else -> ""
    }

    val isValid = title.isNotBlank() && snippet.isNotBlank() && fullContent.isNotBlank() && !isUploading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(
                        androidx.compose.foundation.BorderStroke(2.dp, ScrapbookBorder)
                    )
                    .padding(top = 48.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScrapbookDark
                        )
                    }
                    Text(
                        text = if (showPreview) "PREVIEW" else "WRITE ARTICLE",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookDark)
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                            .clickable { showPreview = !showPreview }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (showPreview) "EDIT" else "PREVIEW",
                            fontFamily = BangersFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookYellow
                        )
                    }
                }
            }

            if (showPreview) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
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
                    ArticleImagePicker(
                        headerImageUri = headerImageUri,
                        headerImageUrl = headerImageUrl,
                        onGalleryImagePicked = { uri ->
                            headerImageUri = uri
                            headerImageUrl = ""
                            uploadedImageUrl = ""
                        },
                        onUrlChanged = { url ->
                            headerImageUrl = url
                            headerImageUri = null
                            uploadedImageUrl = ""
                        },
                        onClearImage = {
                            headerImageUri = null
                            headerImageUrl = ""
                            uploadedImageUrl = ""
                        },
                        isUploading = isUploading
                    )

                    ArticleEditorField(
                        value = title,
                        onValueChange = { title = it },
                        label = "TITLE *",
                        placeholder = "Enter your article title...",
                        singleLine = true,
                        accentColor = ScrapbookDark
                    )
                    ArticleEditorField(
                        value = snippet,
                        onValueChange = { snippet = it },
                        label = "SNIPPET *",
                        placeholder = "A short summary (2-3 sentences)...",
                        singleLine = false,
                        minLines = 3,
                        accentColor = ScrapbookDark
                    )
                    Column {
                        Text(
                            text = "FULL CONTENT *",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Tip: Use **text** to create bold section headings",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ArticleEditorField(
                            value = fullContent,
                            onValueChange = { fullContent = it },
                            label = "",
                            placeholder = "Write your full article here...",
                            singleLine = false,
                            minLines = 8,
                            accentColor = ScrapbookDark
                        )
                    }
                    ArticleEditorField(
                        value = youtubeVideoId,
                        onValueChange = { youtubeVideoId = it },
                        label = "YOUTUBE VIDEO ID (optional)",
                        placeholder = "e.g. dQw4w9WgXcQ",
                        singleLine = true,
                        accentColor = ScrapbookDark
                    )
                }
            }

            // Publish Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .border(
                        androidx.compose.foundation.BorderStroke(2.dp, ScrapbookBorder)
                    )
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isValid) ScrapbookDark
                            else ScrapbookDark.copy(alpha = 0.3f)
                        )
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = isValid) {
                            userArticlesViewModel.publishArticle(
                                title = title.trim(),
                                snippet = snippet.trim(),
                                fullContent = fullContent.trim(),
                                youtubeVideoId = youtubeVideoId.trim(),
                                headerImageUrl = finalImageUrl,
                                username = firebaseProfile?.username ?: "Anonymous",
                                activityViewModel = activityViewModel
                            )
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (publishState is UserArticlesViewModel.PublishState.Loading) {
                        CircularProgressIndicator(
                            color = ScrapbookYellow,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "PUBLISH ARTICLE",
                            fontFamily = BangersFontFamily,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            color = ScrapbookYellow
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
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    color = ScrapbookTextMuted,
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
                fontFamily = NunitoFontFamily,
                fontSize = 14.sp,
                color = ScrapbookTextDark,
                lineHeight = 20.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ScrapbookDark,
                unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                focusedContainerColor = ScrapbookCardWhite,
                unfocusedContainerColor = ScrapbookCardWhite,
                cursorColor = ScrapbookDark,
                focusedTextColor = ScrapbookTextDark,
                unfocusedTextColor = ScrapbookTextDark
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// 9. ✅ Scrapbook Articles Screen
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

    if (showEditor) {
        ArticleEditorScreen(
            authViewModel = authViewModel,
            userArticlesViewModel = userArticlesViewModel,
            activityViewModel = activityViewModel,
            onBack = { showEditor = false }
        )
        return
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
                    .border(
                        androidx.compose.foundation.BorderStroke(2.dp, ScrapbookBorder)
                    )
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ARTICLES",
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
                                contentViewModel.fetchArticles()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Filled.Close
                            else Icons.Filled.Search,
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
                            "Search by title, topic or author...",
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
                                contentViewModel.fetchArticles()
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 80.dp
                )
            ) {
                if (allCommunityArticles.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMUNITY",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 22.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Divider(
                                modifier = Modifier.weight(1f),
                                color = ScrapbookBorder.copy(alpha = 0.2f)
                            )
                        }
                    }
                    itemsIndexed(
                        items = allCommunityArticles,
                        key = { _, article -> article.id }
                    ) { _, article ->
                        ArticleCard(
                            article = article,
                            gradientColors = articleGradientColorsList[0],
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
                            .padding(
                                top = if (allCommunityArticles.isNotEmpty()) 8.dp else 0.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTERNET ARCHIVE",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = ScrapbookBorder.copy(alpha = 0.2f)
                        )
                    }
                }

                when (val state = articlesState) {
                    is ContentState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookRed,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookDark)
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                        .clickable { contentViewModel.fetchArticles() }
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
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> "archive_${item.id}" }
                        ) { _, item ->
                            ArchiveArticleCard(
                                item = item,
                                gradientColors = articleGradientColorsList[0],
                                isBookmarked = favoriteIds.contains(item.id),
                                onBookmarkToggle = {
                                    favoritesViewModel?.toggleFavorite(item.toFavoriteItem())
                                }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // FAB
        if (currentUser != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ScrapbookDark)
                    .border(3.dp, ScrapbookYellow, CircleShape)
                    .clickable { showEditor = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Write article",
                    tint = ScrapbookYellow,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0, name = "Articles Screen")
@Composable
fun ArticlesScreenPreviewDarkContext() {
    HubRetroTheme {
        ArticlesScreen()
    }
}
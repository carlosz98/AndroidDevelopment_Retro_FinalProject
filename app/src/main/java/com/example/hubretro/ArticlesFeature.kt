package com.example.hubretro

import androidx.compose.material3.OutlinedButton
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.tasks.await
import java.util.UUID
import androidx.compose.foundation.*

// ─── Category Helpers ─────────────────────────────────────────────────────────

fun categoryColor(category: String): Color = when (category.uppercase()) {
    "RETRO" -> Color(0xFFFFB300)
    "GAMING" -> Color(0xFF00C853)
    "MUSIC" -> Color(0xFF2979FF)
    "CULTURE" -> Color(0xFFD500F9)
    "PIXEL ART" -> Color(0xFFFF6D00)
    else -> ScrapbookYellow
}

fun categoryEmoji(category: String): String = when (category.uppercase()) {
    "RETRO" -> "📼"
    "GAMING" -> "🎮"
    "MUSIC" -> "🎵"
    "CULTURE" -> "🌍"
    "PIXEL ART" -> "🖼️"
    else -> "📝"
}

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class ArticleItem(
    val id: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val date: String? = null,
    val author: String? = null,
    val authorUid: String? = null,
    val imageResId: Int? = null,
    val imageUrl: String? = null,
    val youtubeVideoId: String? = null,
    val viewCount: Int = 0,
    val category: String = "RETRO",
    val reactions: Map<String, Int> = emptyMap()
)

// ─── Helpers ──────────────────────────────────────────────────────────────────

val articleCategories = listOf("ALL", "RETRO", "GAMING", "MUSIC", "CULTURE", "PIXEL ART")

// ─── Sample Articles ──────────────────────────────────────────────────────────

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
        date = "Nov 15, 2023", author = "Don Carlos", imageResId = R.drawable.article1,
        youtubeVideoId = "fuSRjyR_ZJU", viewCount = 124, category = "RETRO",
        reactions = mapOf("🔥" to 24, "❤️" to 11, "🎮" to 8)
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
        date = "July 29, 2025", author = "Topin99", imageResId = R.drawable.article2,
        youtubeVideoId = "onP3tHaHmQs", viewCount = 38, category = "RETRO",
        reactions = mapOf("🔥" to 5, "❤️" to 3, "🎮" to 2)
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
        date = "July 28, 2025", author = "HomicidalYellio", imageResId = R.drawable.article3,
        youtubeVideoId = "9EvH-2e5at4", viewCount = 72, category = "MUSIC",
        reactions = mapOf("🔥" to 18, "❤️" to 31, "🎮" to 4)
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
        date = "Aug 1, 2025", author = "Carollerm", imageResId = R.drawable.article4,
        youtubeVideoId = "9E0XPzB9wZU", viewCount = 19, category = "PIXEL ART",
        reactions = mapOf("🔥" to 2, "❤️" to 4, "🎮" to 7)
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
        date = "July 12, 2025", author = "LadiesMan61", imageResId = R.drawable.article5,
        youtubeVideoId = "lT9VVMF10Hk", viewCount = 55, category = "PIXEL ART",
        reactions = mapOf("🔥" to 9, "❤️" to 14, "🎮" to 5)
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
        date = "August 05, 2024", author = "Fabriko98", imageResId = R.drawable.article6,
        youtubeVideoId = "RCATF_Y3VAE", viewCount = 88, category = "CULTURE",
        reactions = mapOf("🔥" to 33, "❤️" to 21, "🎮" to 16)
    )
)

val articleGradientColorsList = listOf(
    listOf(ScrapbookYellow, ScrapbookPaper),
    listOf(ScrapbookBlue, ScrapbookPaper),
    listOf(ScrapbookGreen, ScrapbookPaper)
)

// ─── YouTube Player ───────────────────────────────────────────────────────────

@Composable
fun YoutubePlayerCard(
    youtubeVideoId: String?,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    if (youtubeVideoId.isNullOrBlank()) { Box(modifier = modifier.background(Color.Transparent)); return }
    key(youtubeVideoId) {
        AndroidView(
            factory = { ctx ->
                YouTubePlayerView(ctx).apply {
                    enableAutomaticInitialization = false
                    lifecycleOwner.lifecycle.addObserver(this)
                    initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) { youTubePlayer.cueVideo(youtubeVideoId, 0f) }
                    }, IFramePlayerOptions.Builder().build())
                }
            },
            onRelease = { view -> lifecycleOwner.lifecycle.removeObserver(view); view.release() },
            modifier = modifier
        )
    }
}

// ─── Styled Content ───────────────────────────────────────────────────────────

@Composable
fun StyledArticleContentWithLargeInitial(text: String, defaultStyle: TextStyle, subheadingStyle: SpanStyle, largeInitialStyle: SpanStyle) {
    if (text.isEmpty()) { Text("", style = defaultStyle); return }
    val annotatedString = buildAnnotatedString {
        withStyle(style = largeInitialStyle) { append(text.first()) }
        val restOfText = text.substring(1)
        val regex = """\*\*(.*?)\*\*""".toRegex()
        var lastIndex = 0
        regex.findAll(restOfText).forEach { matchResult ->
            val subheadingText = matchResult.groups[1]?.value ?: ""
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            if (startIndex > lastIndex) { withStyle(defaultStyle.toSpanStyle()) { append(restOfText.substring(lastIndex, startIndex)) } }
            withStyle(style = subheadingStyle) { append(subheadingText) }
            lastIndex = endIndex
        }
        if (lastIndex < restOfText.length) { withStyle(defaultStyle.toSpanStyle()) { append(restOfText.substring(lastIndex)) } }
    }
    Text(text = annotatedString, style = defaultStyle)
}

// ─── Shimmer Skeleton ─────────────────────────────────────────────────────────

@Composable
fun ShimmerArticleCard() {
    val shimmerT = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerT.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ScrapbookPaper, Color.White.copy(alpha = 0.9f), ScrapbookPaper),
        start = androidx.compose.ui.geometry.Offset(shimmerX - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerX + 200f, 0f)
    )
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(ScrapbookCardWhite)
            .border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(shimmerBrush))
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.35f).height(11.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
            Box(modifier = Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
            Box(modifier = Modifier.fillMaxWidth(0.65f).height(22.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
            Box(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(shimmerBrush))
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(20.dp)).background(shimmerBrush))
                Box(modifier = Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(20.dp)).background(shimmerBrush))
                Box(modifier = Modifier.width(60.dp).height(28.dp).clip(RoundedCornerShape(20.dp)).background(shimmerBrush))
            }
        }
    }
}

// ─── Author Avatar Pill ───────────────────────────────────────────────────────

@Composable
fun AuthorAvatarPill(author: String?, authorUid: String? = null, catColor: Color = ScrapbookYellow) {
    if (author.isNullOrBlank()) return
    var habboUsername by remember { mutableStateOf("") }
    var habboRegion by remember { mutableStateOf("habbo.com") }
    LaunchedEffect(authorUid) {
        if (!authorUid.isNullOrBlank()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(authorUid).get().await()
                habboUsername = doc.getString("habboUsername") ?: ""
                habboRegion = doc.getString("habboRegion")?.ifBlank { "habbo.com" } ?: "habbo.com"
            } catch (e: Exception) { }
        }
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(catColor.copy(alpha = 0.2f)), contentAlignment = Alignment.BottomCenter) {
            if (habboUsername.isNotBlank()) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context).data(habboAvatarUrl(habboUsername, habboRegion)).crossfade(true).diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).build(),
                    contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(author.take(1).uppercase(), fontFamily = BangersFontFamily, color = catColor, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(author, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Reaction Bar ─────────────────────────────────────────────────────────────

@Composable
fun ArticleReactionBar(articleId: String, initialReactions: Map<String, Int> = emptyMap()) {
    val reactionEmojis = listOf("🔥", "❤️", "🎮")
    var reactions by remember { mutableStateOf(initialReactions.toMutableMap()) }
    var userReacted by remember { mutableStateOf<String?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        reactionEmojis.forEach { emoji ->
            val count = reactions[emoji] ?: 0
            val isReacted = userReacted == emoji
            var popped by remember { mutableStateOf(false) }
            val popScale by animateFloatAsState(targetValue = if (popped) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "pop_$emoji")
            Box(
                modifier = Modifier.scale(popScale).clip(RoundedCornerShape(20.dp))
                    .background(if (isReacted) ScrapbookYellow.copy(alpha = 0.15f) else ScrapbookPaper)
                    .border(1.5.dp, if (isReacted) ScrapbookYellow.copy(alpha = 0.8f) else ScrapbookBorder.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .clickable {
                        popped = true
                        if (userReacted == emoji) {
                            reactions = reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 1) - 1 }
                            userReacted = null
                        } else {
                            userReacted?.let { prev -> reactions = reactions.toMutableMap().apply { this[prev] = (this[prev] ?: 1) - 1 } }
                            reactions = reactions.toMutableMap().apply { this[emoji] = (this[emoji] ?: 0) + 1 }
                            userReacted = emoji
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(emoji, fontSize = 14.sp)
                    Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) ScrapbookYellow else ScrapbookTextMuted, fontSize = 13.sp)
                }
            }
            LaunchedEffect(popped) { if (popped) { kotlinx.coroutines.delay(200); popped = false } }
        }
    }
}

// ─── Related Articles Row ─────────────────────────────────────────────────────

@Composable
fun RelatedArticlesRow(currentArticle: ArticleItem, onArticleClick: (ArticleItem) -> Unit) {
    val related = remember(currentArticle) {
        sampleArticles.filter { it.id != currentArticle.id && it.category == currentArticle.category }.take(3)
            .ifEmpty { sampleArticles.filter { it.id != currentArticle.id }.take(3) }
    }
    if (related.isEmpty()) return
    val catColor = categoryColor(currentArticle.category)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(catColor))
            Spacer(modifier = Modifier.width(8.dp))
            Text("YOU MIGHT ALSO LIKE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(related) { article ->
                val artCatColor = categoryColor(article.category)
                var pressed by remember { mutableStateOf(false) }
                val cardScale by animateFloatAsState(targetValue = if (pressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "relScale")
                Box(modifier = Modifier.width(200.dp).scale(cardScale).clip(RoundedCornerShape(12.dp)).background(ScrapbookCardWhite).border(1.5.dp, artCatColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clickable { pressed = true; onArticleClick(article) }) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper)) {
                            if (article.imageResId != null) {
                                androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = article.imageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else if (!article.imageUrl.isNullOrBlank()) {
                                AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(categoryEmoji(article.category), fontSize = 32.sp) }
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                            Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(artCatColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp)
                            }
                        }
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Timer, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(10.dp))
                                Text(estimateReadingTime(article.fullContent), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
                LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
            }
        }
    }
}

// ─── Reading Progress Bar ─────────────────────────────────────────────────────

@Composable
fun ReadingProgressBar(scrollState: androidx.compose.foundation.ScrollState, catColor: Color = ScrapbookYellow) {
    val progress by remember {
        derivedStateOf {
            if (scrollState.maxValue == 0) 0f
            else scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        }
    }
    val animProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(100), label = "readProgress")
    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(ScrapbookDark.copy(alpha = 0.15f))) {
        Box(modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight().background(Brush.horizontalGradient(colors = listOf(catColor, catColor.copy(alpha = 0.7f)))))
    }
}

// ─── Featured Article Hero Card ───────────────────────────────────────────────

@Composable
fun FeaturedArticleHeroCard(article: ArticleItem, onRead: () -> Unit) {
    val catColor = categoryColor(article.category)
    val neonT = rememberInfiniteTransition(label = "heroNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "heroNeonAlpha")
    val btnScale by neonT.animateFloat(initialValue = 1f, targetValue = 1.04f, animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse), label = "heroBtnScale")

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(16.dp).background(catColor.copy(alpha = neonAlpha * 0.2f), RoundedCornerShape(16.dp)))
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha), catColor.copy(alpha = 0.2f), catColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(16.dp)),
            backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                if (article.imageResId != null) {
                    androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = article.imageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.45f)
                } else if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.45f)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(catColor.copy(alpha = 0.3f), ScrapbookDark))), contentAlignment = Alignment.Center) {
                        Text(categoryEmoji(article.category), fontSize = 64.sp)
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.98f)))))

                // ✅ Animated scan line
                val scanT = rememberInfiniteTransition(label = "heroScan")
                val scanY by scanT.animateFloat(initialValue = -280f, targetValue = 280f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "heroScanY")
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).offset(y = scanY.dp).background(catColor.copy(alpha = 0.15f)))

                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catColor).border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("⭐ FEATURED", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                        }
                        if (isTrending(article.viewCount)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookRed).border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text("🔥 TRENDING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(catColor.copy(alpha = 0.25f)).border(1.dp, catColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = catColor, fontSize = 11.sp)
                            }
                            Text(estimateReadingTime(article.fullContent), fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text("· ${article.viewCount} views", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(article.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(8.dp))
                        AuthorAvatarPill(author = article.author, authorUid = article.authorUid, catColor = catColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            // ✅ Reactions preview
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                article.reactions.entries.take(3).forEach { (emoji, count) ->
                                    Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(emoji, fontSize = 12.sp)
                                        Text("$count", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    }
                                }
                            }
                            Box(modifier = Modifier.scale(btnScale).clip(RoundedCornerShape(10.dp)).background(catColor).border(width = 2.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp)).clickable { onRead() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                Text("READ ARTICLE →", fontFamily = BangersFontFamily, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
// ─── Full Article Detail Screen ───────────────────────────────────────────────

@Composable
fun ArticleDetailScreen(
    article: ArticleItem,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    onBack: () -> Unit,
    onRelatedArticleClick: ((ArticleItem) -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    val catColor = categoryColor(article.category)

    LaunchedEffect(article.id) {
        try { FirebaseFirestore.getInstance().collection("articles").document(article.id).update("viewCount", com.google.firebase.firestore.FieldValue.increment(1)) } catch (e: Exception) { }
    }

    val neonT = rememberInfiniteTransition(label = "detailNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "detailNeonAlpha")

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        // ✅ Reading progress bar
        ReadingProgressBar(scrollState = scrollState, catColor = catColor)

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(top = 3.dp)) {
            // Hero image
            Box(modifier = Modifier.fillMaxWidth().height(310.dp)) {
                if (article.imageResId != null) {
                    androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = article.imageResId), contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(catColor.copy(alpha = 0.4f), ScrapbookDark))), contentAlignment = Alignment.Center) { Text(categoryEmoji(article.category), fontSize = 64.sp) }
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.8f)))))

                // ✅ Back button using category color
                Box(modifier = Modifier.align(Alignment.TopStart).padding(top = 44.dp, start = 12.dp).clip(CircleShape).background(catColor).border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape).clickable { onBack() }.padding(9.dp)) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // ✅ Bookmark + share buttons
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(CircleShape).background(if (isBookmarked) catColor else ScrapbookDark.copy(alpha = 0.75f)).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha), catColor.copy(alpha = 0.3f))), shape = CircleShape).clickable { onBookmarkToggle() }.padding(9.dp)) {
                        Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Category + title at bottom
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(catColor).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                        }
                        if (isTrending(article.viewCount)) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookRed).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("🔥 TRENDING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                            }
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.7f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Timer, contentDescription = null, tint = catColor, modifier = Modifier.size(10.dp))
                                Text(estimateReadingTime(article.fullContent), fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(article.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, lineHeight = 28.sp)
                }
            }

            // ✅ Author + metadata
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AuthorAvatarPill(author = article.author, authorUid = article.authorUid, catColor = catColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(13.dp))
                            Text("${article.viewCount}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                        }
                        article.date?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = catColor.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Snippet box with category color
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookDark).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha), catColor.copy(alpha = 0.3f), catColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Row {
                        Box(modifier = Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(catColor))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(article.snippet, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 15.sp, lineHeight = 22.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ✅ Full content — subheadings use category color
                StyledArticleContentWithLargeInitial(
                    text = article.fullContent,
                    defaultStyle = TextStyle(fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 16.sp, lineHeight = 27.sp),
                    subheadingStyle = SpanStyle(fontFamily = BangersFontFamily, color = catColor, fontSize = 20.sp),
                    largeInitialStyle = SpanStyle(fontFamily = BangersFontFamily, color = catColor, fontSize = 48.sp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ✅ Reactions section
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ScrapbookCardWhite).border(1.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("REACTIONS", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, letterSpacing = 1.sp)
                        ArticleReactionBar(articleId = article.id, initialReactions = article.reactions)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ✅ YouTube section
            if (!article.youtubeVideoId.isNullOrBlank()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = catColor.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(catColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📺 RELATED VIDEO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha), catColor.copy(alpha = 0.2f), catColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                    YoutubePlayerCard(youtubeVideoId = article.youtubeVideoId, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), lifecycleOwner = lifecycleOwner)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ✅ Related articles
            RelatedArticlesRow(currentArticle = article, onArticleClick = { onRelatedArticleClick?.invoke(it) })
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── Article Card ─────────────────────────────────────────────────────────────

@Composable
fun ArticleCard(
    article: ArticleItem,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    onOpenFullScreen: ((ArticleItem) -> Unit)? = null,
    animationDelay: Int = 0
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasImage = article.imageResId != null || !article.imageUrl.isNullOrBlank()
    val catColor = categoryColor(article.category)

    val neonT = rememberInfiniteTransition(label = "cardNeon_${article.id}")
    val neonAlpha by neonT.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "cardNeonAlpha")

    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "articleCardScale")

    // ✅ Staggered entrance
    var visible by remember { mutableStateOf(false) }
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 60f, animationSpec = tween(500, delayMillis = animationDelay, easing = LinearOutSlowInEasing), label = "enterOffset")
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(500, delayMillis = animationDelay, easing = LinearOutSlowInEasing), label = "enterAlpha")
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = modifier.scale(cardScale).offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }) {
        if (isTrending(article.viewCount)) {
            Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(12.dp).background(catColor.copy(alpha = neonAlpha * 0.12f), RoundedCornerShape(12.dp)))
        }
        // ✅ Category color left accent bar
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)).background(catColor).align(Alignment.CenterStart))

        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().animateContentSize()
                .then(if (isTrending(article.viewCount)) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha * 0.6f), catColor.copy(alpha = 0.1f), catColor.copy(alpha = neonAlpha * 0.6f))), shape = RoundedCornerShape(12.dp)) else Modifier),
            backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp
        ) {
            Column {
                if (hasImage) {
                    Box(modifier = Modifier.fillMaxWidth().height(190.dp).clickable { pressed = true; onOpenFullScreen?.invoke(article) ?: run { isExpanded = !isExpanded } }) {
                        if (article.imageResId != null) {
                            androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(id = article.imageResId), contentDescription = article.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentScale = ContentScale.Crop)
                        } else if (!article.imageUrl.isNullOrBlank()) {
                            AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper))
                        }
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))

                        // ✅ Reading time pill
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookDark.copy(alpha = 0.75f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Timer, contentDescription = null, tint = catColor, modifier = Modifier.size(10.dp))
                                Text(estimateReadingTime(article.fullContent), fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                            }
                        }

                        if (isTrending(article.viewCount)) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookRed).border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("🔥 TRENDING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                            }
                        }

                        // ✅ Category badge
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(catColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                        }

                        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                            IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(14.dp)) {
                    if (!hasImage) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(catColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                                }
                                if (isTrending(article.viewCount)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookRed).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("🔥 TRENDING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) }
                                }
                            }
                            Box(modifier = Modifier.clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                                IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(36.dp)) {
                                    Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 0.5.sp, lineHeight = 24.sp, modifier = Modifier.clickable { pressed = true; onOpenFullScreen?.invoke(article) })

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ Author pill + view count
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        AuthorAvatarPill(author = article.author, authorUid = article.authorUid, catColor = catColor)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Visibility, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(11.dp))
                                Text("${article.viewCount}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                            }
                            article.date?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp) }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = catColor.copy(alpha = 0.15f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isExpanded) {
                        StyledArticleContentWithLargeInitial(
                            text = article.fullContent,
                            defaultStyle = TextStyle(fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 14.sp, lineHeight = 22.sp),
                            subheadingStyle = SpanStyle(fontFamily = BangersFontFamily, color = catColor, fontSize = 18.sp),
                            largeInitialStyle = SpanStyle(fontFamily = BangersFontFamily, color = catColor, fontSize = 36.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ✅ Reactions
                    ArticleReactionBar(articleId = article.id, initialReactions = article.reactions)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { isExpanded = !isExpanded }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (isExpanded) "COLLAPSE" else "QUICK PREVIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                        }
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha), catColor.copy(alpha = 0.3f), catColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(8.dp)).clickable { pressed = true; onOpenFullScreen?.invoke(article) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("READ FULL →", fontFamily = BangersFontFamily, color = catColor, fontSize = 13.sp)
                        }
                    }
                }

                if (isExpanded && !article.youtubeVideoId.isNullOrBlank()) {
                    HorizontalDivider(color = catColor.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 14.dp))
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(catColor.copy(alpha = neonAlpha), catColor.copy(alpha = 0.2f), catColor.copy(alpha = neonAlpha))), shape = RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp))) {
                        YoutubePlayerCard(youtubeVideoId = article.youtubeVideoId, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), lifecycleOwner = lifecycleOwner)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

// ─── Archive Article Card ─────────────────────────────────────────────────────

@Composable
fun ArchiveArticleCard(item: ArchiveItem, gradientColors: List<Color>, modifier: Modifier = Modifier, isBookmarked: Boolean = false, onBookmarkToggle: () -> Unit = {}) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "archiveScale")

    Box(modifier = modifier.scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().animateContentSize(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).clickable { pressed = true; isExpanded = !isExpanded }) {
                    AsyncImage(model = item.thumbnailUrl, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper))
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.4f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("INTERNET ARCHIVE", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookYellow, fontSize = 9.sp)
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape)) {
                        IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 0.5.sp, maxLines = if (isExpanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        item.creator?.let { Text(it, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 12.sp) }
                        item.year?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))
                    if (item.description.isNotBlank()) {
                        Text(item.description, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, lineHeight = 19.sp, maxLines = if (isExpanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { pressed = true; isExpanded = !isExpanded }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                            Text(if (isExpanded) "READ LESS" else "READ MORE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                        }
                        if (isExpanded) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.webUrl)); try { context.startActivity(intent) } catch (e: Exception) { } }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                Text("OPEN →", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

// ─── Image Picker ─────────────────────────────────────────────────────────────

@Composable
fun ArticleImagePicker(headerImageUri: android.net.Uri?, headerImageUrl: String, onGalleryImagePicked: (android.net.Uri) -> Unit, onUrlChanged: (String) -> Unit, onClearImage: () -> Unit, isUploading: Boolean) {
    var selectedTab by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { onGalleryImagePicked(it) } }
    Column {
        Text("HEADER IMAGE (optional)", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(6.dp))
        TabRow(selectedTabIndex = selectedTab, containerColor = ScrapbookPaper, contentColor = ScrapbookDark) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("GALLERY", fontFamily = BangersFontFamily, fontSize = 14.sp) } })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("URL", fontFamily = BangersFontFamily, fontSize = 14.sp) } })
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (selectedTab) {
            0 -> {
                if (headerImageUri != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))) {
                        AsyncImage(model = headerImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        if (isUploading) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Uploading...", fontFamily = NunitoFontFamily, color = ScrapbookYellow, fontSize = 12.sp) } } }
                        IconButton(onClick = onClearImage, modifier = Modifier.align(Alignment.TopEnd)) { Icon(Icons.Filled.Close, contentDescription = "Remove", tint = ScrapbookDark, modifier = Modifier.background(ScrapbookYellow, CircleShape).padding(4.dp).size(16.dp)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { launcher.launch("image/*") }.padding(12.dp), contentAlignment = Alignment.Center) { Text("CHANGE IMAGE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp) }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(36.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("TAP TO PICK FROM GALLERY", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 14.sp) }
                    }
                }
            }
            1 -> {
                OutlinedTextField(value = headerImageUrl, onValueChange = onUrlChanged, placeholder = { Text("https://example.com/image.jpg", fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookTextMuted) }, singleLine = true, textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─── Article Editor ───────────────────────────────────────────────────────────

@Composable
fun ArticleEditorScreen(authViewModel: AuthViewModel, userArticlesViewModel: UserArticlesViewModel, activityViewModel: ActivityViewModel? = null, onBack: () -> Unit) {
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

    val neonT = rememberInfiniteTransition(label = "publishNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "publishNeonAlpha")

    LaunchedEffect(publishState) { if (publishState is UserArticlesViewModel.PublishState.Success) { userArticlesViewModel.resetPublishState(); onBack() } }
    LaunchedEffect(headerImageUri) {
        val uri = headerImageUri ?: return@LaunchedEffect
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        isUploading = true
        try { val storageRef = FirebaseStorage.getInstance().reference.child("article_images/$uid/${UUID.randomUUID()}.jpg"); storageRef.putFile(uri).await(); uploadedImageUrl = storageRef.downloadUrl.await().toString() } catch (e: Exception) { uploadedImageUrl = "" } finally { isUploading = false }
    }

    val finalImageUrl = when { uploadedImageUrl.isNotBlank() -> uploadedImageUrl; headerImageUrl.isNotBlank() -> headerImageUrl; else -> "" }
    val isValid = title.isNotBlank() && snippet.isNotBlank() && fullContent.isNotBlank() && !isUploading

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ✅ Golden header
            Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 48.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookDark) }
                    Text(text = if (showPreview) "PREVIEW" else "WRITE ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { showPreview = !showPreview }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(if (showPreview) "EDIT" else "PREVIEW", fontFamily = BangersFontFamily, fontSize = 14.sp, color = ScrapbookYellow)
                    }
                }
            }

            if (showPreview) {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { ArticleCard(article = ArticleItem(id = "preview", title = title.ifBlank { "Your Title Here" }, snippet = snippet.ifBlank { "Your snippet here..." }, fullContent = fullContent.ifBlank { "Your full content here..." }, date = "Today", author = firebaseProfile?.username ?: "You", imageUrl = finalImageUrl.ifBlank { null }, youtubeVideoId = youtubeVideoId.ifBlank { null }), gradientColors = articleGradientColorsList[0], initiallyExpanded = false) }
                }
            } else {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ArticleImagePicker(headerImageUri = headerImageUri, headerImageUrl = headerImageUrl, onGalleryImagePicked = { uri -> headerImageUri = uri; headerImageUrl = ""; uploadedImageUrl = "" }, onUrlChanged = { url -> headerImageUrl = url; headerImageUri = null; uploadedImageUrl = "" }, onClearImage = { headerImageUri = null; headerImageUrl = ""; uploadedImageUrl = "" }, isUploading = isUploading)
                    ArticleEditorField(value = title, onValueChange = { title = it }, label = "TITLE *", placeholder = "Enter your article title...", singleLine = true, accentColor = ScrapbookDark)
                    ArticleEditorField(value = snippet, onValueChange = { snippet = it }, label = "SNIPPET *", placeholder = "A short summary (2-3 sentences)...", singleLine = false, minLines = 3, accentColor = ScrapbookDark)
                    Column {
                        Text("FULL CONTENT *", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                        Text("Tip: Use **text** to create bold section headings", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        ArticleEditorField(value = fullContent, onValueChange = { fullContent = it }, label = "", placeholder = "Write your full article here...", singleLine = false, minLines = 8, accentColor = ScrapbookDark)
                    }
                    ArticleEditorField(value = youtubeVideoId, onValueChange = { youtubeVideoId = it }, label = "YOUTUBE VIDEO ID (optional)", placeholder = "e.g. dQw4w9WgXcQ", singleLine = true, accentColor = ScrapbookDark)
                }
            }

            // ✅ Publish button with neon glow
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookCream).border(BorderStroke(2.dp, ScrapbookBorder)).padding(16.dp)) {
                Box(modifier = Modifier.matchParentSize().padding(4.dp).blur(if (isValid) 12.dp else 0.dp).background(ScrapbookYellow.copy(alpha = if (isValid) neonAlpha * 0.2f else 0f), RoundedCornerShape(12.dp)))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isValid) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f))
                        .border(width = 2.dp, brush = if (isValid) Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = RoundedCornerShape(12.dp))
                        .clickable(enabled = isValid) { userArticlesViewModel.publishArticle(title = title.trim(), snippet = snippet.trim(), fullContent = fullContent.trim(), youtubeVideoId = youtubeVideoId.trim(), headerImageUrl = finalImageUrl, username = firebaseProfile?.username ?: "Anonymous", activityViewModel = activityViewModel) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (publishState is UserArticlesViewModel.PublishState.Loading) { ThreeDotsAnimation() }
                    else { Text("PUBLISH ARTICLE", fontFamily = BangersFontFamily, fontSize = 20.sp, letterSpacing = 1.sp, color = if (isValid) ScrapbookYellow else ScrapbookYellow.copy(alpha = 0.3f)) }
                }
            }
        }
    }
}

@Composable
fun ArticleEditorField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, singleLine: Boolean, accentColor: Color, minLines: Int = 1) {
    Column {
        if (label.isNotBlank()) { Text(text = label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp); Spacer(modifier = Modifier.height(4.dp)) }
        OutlinedTextField(value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted, lineHeight = 18.sp) }, singleLine = singleLine, minLines = minLines, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) ImeAction.Next else ImeAction.Default), textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark, lineHeight = 20.sp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
    }
}

// ─── Articles Screen ──────────────────────────────────────────────────────────

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
    val favoriteIds by (favoritesViewModel?.favoriteIds?.collectAsState() ?: remember { mutableStateOf(emptySet<String>()) })

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastSearched by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var fullScreenArticle by remember { mutableStateOf<ArticleItem?>(null) }

    val featuredArticle = remember(sampleArticles) { sampleArticles.maxByOrNull { it.viewCount } }

    val neonT = rememberInfiniteTransition(label = "articlesNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "articlesNeonAlpha")

    LaunchedEffect(searchQuery) { kotlinx.coroutines.delay(600); if (searchQuery != lastSearched) { lastSearched = searchQuery; contentViewModel.fetchArticles(searchQuery) } }

    val userArticleItems = remember(userArticles) { userArticles.map { it.toArticleItem() } }
    val allCommunityArticles = remember(userArticleItems, searchQuery, selectedCategory) {
        val combined = sampleArticles + userArticleItems
        combined.filter { article ->
            val matchesSearch = searchQuery.isBlank() || article.title.contains(searchQuery, ignoreCase = true) || article.snippet.contains(searchQuery, ignoreCase = true) || article.author?.contains(searchQuery, ignoreCase = true) == true
            val matchesCategory = selectedCategory == "ALL" || article.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    if (fullScreenArticle != null) {
        ArticleDetailScreen(
            article = fullScreenArticle!!,
            isBookmarked = favoriteIds.contains(fullScreenArticle!!.id),
            onBookmarkToggle = { val a = fullScreenArticle!!; favoritesViewModel?.toggleFavorite(FavoriteItem(id = a.id, title = a.title, description = a.snippet, thumbnailUrl = a.imageUrl, webUrl = "", category = "ARTICLE", creator = a.author, year = a.date)) },
            onBack = { fullScreenArticle = null },
            onRelatedArticleClick = { fullScreenArticle = it }  // ✅ related articles navigation
        )
        return
    }
    if (showEditor) { ArticleEditorScreen(authViewModel = authViewModel, userArticlesViewModel = userArticlesViewModel, activityViewModel = activityViewModel, onBack = { showEditor = false }); return }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Golden header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                // ✅ Animated scan line on header
                val scanT = rememberInfiniteTransition(label = "headerScan")
                val scanX by scanT.animateFloat(initialValue = -400f, targetValue = 400f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "headerScanX")
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.2f), Color.Transparent), startX = scanX, endX = scanX + 200f)))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        // ✅ Shimmer on title
                        val shimmerT = rememberInfiniteTransition(label = "titleShimmer")
                        val shimmerX by shimmerT.animateFloat(initialValue = -300f, targetValue = 600f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "titleShimmerX")
                        Box {
                            Text("ARTICLES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 32.sp, letterSpacing = 2.sp)
                            Text("ARTICLES", fontFamily = BangersFontFamily, fontSize = 32.sp, letterSpacing = 2.sp, style = TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 100f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, 0f))))
                        }
                        Text("Retro culture & gaming", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    // ✅ Article count badge
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("${allCommunityArticles.size} ARTICLES", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // ✅ Search button
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape).clickable { searchVisible = !searchVisible; if (!searchVisible) { searchQuery = ""; focusManager.clearFocus(); contentViewModel.fetchArticles() } }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Search", tint = ScrapbookYellow, modifier = Modifier.size(20.dp))
                    }
                }
            }

            AnimatedVisibility(visible = searchVisible, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title, topic or author...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp)) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = ""; contentViewModel.fetchArticles() }) { Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp)) } } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ✅ Category chips — unique color per category + emoji
            LazyRow(modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(articleCategories) { category ->
                    val isSelected = selectedCategory == category
                    val chipColor = if (category == "ALL") ScrapbookYellow else categoryColor(category)
                    var pressed by remember { mutableStateOf(false) }
                    val chipScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chipScale")
                    Box(
                        modifier = Modifier.scale(chipScale).clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                            .border(width = if (isSelected) 2.dp else 1.5.dp, brush = if (isSelected) Brush.linearGradient(colors = listOf(chipColor.copy(alpha = neonAlpha), chipColor.copy(alpha = 0.3f), chipColor.copy(alpha = neonAlpha))) else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)), shape = RoundedCornerShape(20.dp))
                            .clickable { pressed = true; selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (category == "ALL") "ALL" else "${categoryEmoji(category)} $category",
                            fontFamily = BangersFontFamily,
                            color = if (isSelected) chipColor else ScrapbookDark,
                            fontSize = 12.sp
                        )
                    }
                    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp)) {

                // ✅ Featured hero
                if (featuredArticle != null && searchQuery.isBlank() && selectedCategory == "ALL") {
                    item { FeaturedArticleHeroCard(article = featuredArticle, onRead = { fullScreenArticle = featuredArticle }); Spacer(modifier = Modifier.height(4.dp)); HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f)) }
                }

                // ✅ Community section header
                if (allCommunityArticles.isNotEmpty()) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COMMUNITY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookDark).border(1.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("${allCommunityArticles.size}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
                            }
                        }
                    }
                    // ✅ Staggered article cards
                    itemsIndexed(items = allCommunityArticles, key = { _, article -> article.id }) { index, article ->
                        ArticleCard(
                            article = article,
                            gradientColors = articleGradientColorsList[0],
                            initiallyExpanded = false,
                            isBookmarked = favoriteIds.contains(article.id),
                            onBookmarkToggle = { favoritesViewModel?.toggleFavorite(FavoriteItem(id = article.id, title = article.title, description = article.snippet, thumbnailUrl = article.imageUrl, webUrl = "", category = "ARTICLE", creator = article.author, year = article.date)) },
                            onOpenFullScreen = { fullScreenArticle = it },
                            animationDelay = index * 80  // ✅ staggered
                        )
                    }
                } else if (searchQuery.isNotBlank() || selectedCategory != "ALL") {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (selectedCategory != "ALL") categoryEmoji(selectedCategory) else "🔍", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(if (selectedCategory != "ALL") "No $selectedCategory articles yet" else "No results for \"$searchQuery\"", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // ✅ Archive section header
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(top = if (allCommunityArticles.isNotEmpty()) 8.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = neonAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INTERNET ARCHIVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                        // ✅ Blinking cursor on archive header
                        val blinkT = rememberInfiniteTransition(label = "blink")
                        val blinkAlpha by blinkT.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "blinkAlpha")
                        Text("_", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = blinkAlpha), fontSize = 22.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                when (val state = articlesState) {
                    is ContentState.Loading -> {
                        // ✅ Shimmer skeletons
                        items(3) { ShimmerArticleCard() }
                    }
                    is ContentState.Error -> {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 14.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { contentViewModel.fetchArticles() }.padding(horizontal = 24.dp, vertical = 10.dp)) {
                                    Text("RETRY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    is ContentState.Success -> {
                        itemsIndexed(items = state.items, key = { _, item -> "archive_${item.id}" }) { _, item ->
                            ArchiveArticleCard(item = item, gradientColors = articleGradientColorsList[0], isBookmarked = favoriteIds.contains(item.id), onBookmarkToggle = { favoritesViewModel?.toggleFavorite(item.toFavoriteItem()) })
                        }
                    }
                    else -> { }
                }
            }
        }

        // ✅ FAB with neon glow
        if (currentUser != null) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                Box(modifier = Modifier.size(64.dp).blur(16.dp).background(ScrapbookYellow.copy(alpha = neonAlpha * 0.4f), CircleShape))
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(ScrapbookDark).border(width = 3.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = CircleShape).clickable { showEditor = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = "Write article", tint = ScrapbookYellow, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0, name = "Articles Screen")
@Composable
fun ArticlesScreenPreviewDarkContext() {
    HubRetroTheme { ArticlesScreen() }
}
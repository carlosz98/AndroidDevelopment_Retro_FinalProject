package com.example.hubretro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.R
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─── Data Models ──────────────────────────────────────────────────────────────
// NOTE: ArticleItem lives in SharedComposables.kt — do NOT redeclare it here.

data class ArticleComment(
    val id: String = "",
    val articleId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorPicUrl: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val likes: Int = 0
)

enum class ArticleSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    MOST_LIKED("Most Liked"),
    MOST_VIEWED("Most Viewed")
}

enum class ReadingTimeFilter(val label: String, val maxMinutes: Int) {
    ALL("All", Int.MAX_VALUE),
    QUICK("< 2 min", 2),
    MEDIUM("2–5 min", 5),
    LONG("> 5 min", Int.MAX_VALUE)
}

// ─── Constants ────────────────────────────────────────────────────────────────

// ─── Original Hardcoded Articles (with local images) ─────────────────────────

val sampleArticles = listOf(
    ArticleItem(
        id = "s1", title = "The Pixelated Pull: Why Retro Gaming is Booming Again",
        snippet = "Beyond nostalgia, discover the reasons for the resurgence of classic video games and their timeless appeal in a modern world.",
        fullContent = """The year 2026 isn't just about the next generation of hyper-realistic graphics; it's also witnessing an unprecedented boom in the popularity of retro gaming. From dusty attics to digital storefronts, classic titles from the 80s, 90s, and early 2000s are capturing the hearts of both seasoned gamers and a new generation of players.

## More Than Just Memory Lane
While nostalgia is undoubtedly a powerful catalyst, the retro revival runs deeper. For many who grew up with these games, it's a comforting return to simpler times — and for younger players, it's discovery of something genuinely new to them.

## The Allure of Simplicity and Challenge
In an era of sprawling open worlds and complex game mechanics, retro games offer a refreshing directness. A platformer from 1990 doesn't waste your time. It respects your intelligence and punishes your mistakes.

## Accessibility and Community
The rise of emulation, dedicated retro consoles, and online communities has made these classics more accessible than ever. RetroHub exists precisely because of this movement.""".trimIndent(),
        date = "Nov 15, 2023", author = "Don Carlos", imageResId = R.drawable.article1,
        youtubeVideoId = "fuSRjyR_ZJU", viewCount = 124, category = "RETRO",
        reactions = mapOf("🔥" to 24, "❤️" to 11, "🎮" to 8), likeCount = 35, readingTimeMinutes = 3
    ),
    ArticleItem(
        id = "s2", title = "The Digital Ghosts: Exploring the World of Abandonware",
        snippet = "Unearthing lost classics and forgotten gems from the digital past. What happens when software is left behind?",
        fullContent = """In the fast-paced world of software development, titles that once graced magazine covers and topped sales charts can eventually fade into obscurity. This is the realm of abandonware.

## What Qualifies as Abandonware?
The definition can be murky, as copyright technically still applies to most of these works. In practice, abandonware refers to software no longer sold or supported by its creator — often because the company no longer exists.

## Why the Enduring Appeal?
The fascination with abandonware stems from nostalgia, historical significance, and the dedication of fan communities who preserve digital history that would otherwise be lost forever.""".trimIndent(),
        date = "Jul 29, 2025", author = "Topin99", imageResId = R.drawable.article2,
        youtubeVideoId = "onP3tHaHmQs", viewCount = 38, category = "RETRO",
        reactions = mapOf("🔥" to 5, "❤️" to 3, "🎮" to 2), likeCount = 8, readingTimeMinutes = 2
    ),
    ArticleItem(
        id = "s3", title = "The Serene Symphony: Minecraft's Enduring Soundtrack",
        snippet = "Exploring the subtle genius of C418's compositions and how they define the Minecraft experience.",
        fullContent = """Beyond the blocky landscapes and endless creative possibilities, one of the most iconic aspects of Minecraft is its unique soundtrack by C418.

## A World of Calm and Wonder
Unlike the bombastic scores of many action-packed games, Minecraft's music is predominantly ambient and deeply atmospheric. Tracks like "Calm" and "Sweden" feel less like game music and more like a companion.

## An Enduring Legacy
Even as Minecraft has evolved with new composers, C418's original compositions remain the heart and soul of the game's auditory identity. They are deeply tied to the memory of discovery.""".trimIndent(),
        date = "Jul 28, 2025", author = "HomicidalYellio", imageResId = R.drawable.article3,
        youtubeVideoId = "9EvH-2e5at4", viewCount = 72, category = "OST",
        reactions = mapOf("🔥" to 18, "❤️" to 31, "🎮" to 4), likeCount = 49, readingTimeMinutes = 3
    ),
    ArticleItem(
        id = "s4", title = "Why Modern Games Embrace the Low-Polygon Aesthetic",
        snippet = "Exploring the resurgence of low-poly graphics as a deliberate and impactful art style.",
        fullContent = """In an era where photorealism often dominates gaming, a distinct trend has emerged: the deliberate use of low-polygon aesthetics.

## What is Low-Poly?
The term refers to 3D models constructed with a relatively small number of polygons, creating a geometric, faceted appearance. PS1-era games had it by necessity. Today's indie studios choose it intentionally.

## Beyond Nostalgia
The modern resurgence of low-poly is driven by artistic expression, performance benefits, and faster development cycles. A small team can create a visually coherent world without needing hundreds of artists.""".trimIndent(),
        date = "Aug 1, 2025", author = "Carollerm", imageResId = R.drawable.article4,
        youtubeVideoId = "9E0XPzB9wZU", viewCount = 19, category = "PIXEL ART",
        reactions = mapOf("🔥" to 2, "❤️" to 4, "🎮" to 7), likeCount = 13, readingTimeMinutes = 2
    ),
    ArticleItem(
        id = "s5", title = "Why Pixel Art is Still Gorgeous",
        snippet = "Exploring the timeless appeal of pixel art and why this art form continues to captivate artists and gamers alike.",
        fullContent = """What began as a necessity due to hardware limitations has evolved into a deliberate and beloved art style that continues to push creative boundaries.

## The Beauty in Limitation
Every pixel is placed with intention. Artists must make careful choices about color palettes, shading, and form. A 16×16 sprite character can convey more personality than a detailed 3D model.

## A Thriving Modern Art Form
Titles like Stardew Valley, Celeste, and Hyper Light Drifter prove pixel art's power. The constraints breed creativity, and the aesthetic has built its own passionate following that goes far beyond retro nostalgia.""".trimIndent(),
        date = "Jul 12, 2025", author = "LadiesMan61", imageResId = R.drawable.article5,
        youtubeVideoId = "lT9VVMF10Hk", viewCount = 55, category = "PIXEL ART",
        reactions = mapOf("🔥" to 9, "❤️" to 14, "🎮" to 5), likeCount = 23, readingTimeMinutes = 2
    ),
    ArticleItem(
        id = "s6", title = "The Enduring Nostalgia of Habbo Hotel",
        snippet = "A look back at Habbo Hotel and why its pixelated world still holds a special place in our hearts.",
        fullContent = """For a certain generation, the words Bobba, Furni, and Pool's Closed evoke an instant wave of nostalgia. Habbo Hotel was more than a game — it was many people's first online social world.

## A Pixelated Universe
Launched in 2000 by Sulake, Habbo Hotel offered users the ability to create avatars, design rooms, and chat with people from around the world using its distinctive isometric pixel art style.

## Why We Still Remember It
Habbo fostered a sense of community, creative expression, and early online identity. For many, it was their first taste of building something online — your room was yours in a way that mattered.""".trimIndent(),
        date = "Aug 5, 2024", author = "Fabriko98", imageResId = R.drawable.article6,
        youtubeVideoId = "RCATF_Y3VAE", viewCount = 88, category = "CULTURE",
        reactions = mapOf("🔥" to 33, "❤️" to 21, "🎮" to 16), likeCount = 54, readingTimeMinutes = 3
    )
)

val articleGradientColorsList = listOf(
    listOf(ComicGlassBg, ComicGlassBg),
    listOf(ComicGlassBg, ComicGlassBg),
    listOf(ComicGlassBg, ComicGlassBg),
    listOf(ComicGlassBg, ComicGlassBg),
    listOf(ComicGlassBg, ComicGlassBg)
)

val articleCategories = listOf("ALL", "RETRO", "GAMING", "PIXEL ART", "OST", "CULTURE", "REVIEW", "OPINION")

fun categoryEmoji(category: String): String = when (category) {
    "RETRO" -> "🕹️"; "GAMING" -> "🎮"; "PIXEL ART" -> "🖼️"
    "NINTENDO" -> "🍄"; "PLAYSTATION" -> "🎯"; "XBOX" -> "💚"
    "OST" -> "🎵"; "CULTURE" -> "🌍"; "REVIEW" -> "⭐"
    "OPINION" -> "💬"; else -> "📝"
}

fun categoryColor(category: String): Color = when (category) {
    "RETRO" -> CAcPurple; "GAMING" -> CAcBlue
    "PIXEL ART" -> CGreenDeep; "OST" -> CAcRed
    "CULTURE" -> CGreenDeep; "REVIEW" -> CAcYellow
    "OPINION" -> CAcYellow; else -> CGreen
}

fun estimateReadingTime(content: String): Int {
    val wordCount = content.trim().split("\\s+".toRegex()).size
    return ((wordCount / 200.0).coerceAtLeast(1.0)).toInt()
}

fun shareArticle(context: Context, article: ArticleItem) {
    val text = if (article.sourceUrl.isNotBlank()) {
        "Check out \"${article.title}\" on RetroHub!\n${article.sourceUrl}"
    } else {
        "Check out \"${article.title}\" on RetroHub!\n\n${article.snippet}"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Article"))
}

// ─── Article content helpers (heading parsing / TOC) ─────────────────────────
// Content convention: a line starting with "##" is treated as a section heading.
// e.g. "## Why It Mattered" becomes a heading; everything else is body text.

fun extractHeadings(content: String): List<String> {
    if (content.isBlank()) return emptyList()
    return content.lines()
        .map { it.trim() }
        .filter { it.startsWith("##") }
        .map { it.trimStart('#').trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

// ─── Firestore helpers ────────────────────────────────────────────────────────

fun articleDocRef(articleId: String) =
    FirebaseFirestore.getInstance().collection("articles").document(articleId)

fun likesCollection(articleId: String) =
    articleDocRef(articleId).collection("likes")

fun savesCollection(articleId: String) =
    articleDocRef(articleId).collection("saves")

fun reactionsCollection(articleId: String) =
    articleDocRef(articleId).collection("reactions")

fun commentsQuery(articleId: String) =
    articleDocRef(articleId).collection("comments")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(50)

// ─── Engagement state holder ──────────────────────────────────────────────────

data class ArticleEngagement(
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val commentCount: Int = 0,
    val viewCount: Int = 0,
    val myReaction: String? = null,
    val reactions: Map<String, Int> = emptyMap()
)

@Composable
fun rememberArticleEngagement(articleId: String, currentUserId: String?): ArticleEngagement {
    var state by remember(articleId) { mutableStateOf(ArticleEngagement()) }
    LaunchedEffect(articleId, currentUserId) {
        if (articleId.isBlank() || currentUserId.isNullOrBlank()) return@LaunchedEffect
        try {
            val db = FirebaseFirestore.getInstance()
            val engDoc = db.collection("article_engagement").document(articleId).get().await()
            val likeCount = (engDoc.getLong("likeCount") ?: 0).toInt()
            val saveCount = (engDoc.getLong("saveCount") ?: 0).toInt()
            val viewCount = (engDoc.getLong("viewCount") ?: 0).toInt()
            val reactions = engDoc.get("reactions") as? Map<String, Long> ?: emptyMap()
            val isLiked = db.collection("article_engagement").document(articleId)
                .collection("likes").document(currentUserId).get().await().exists()
            val isSaved = db.collection("article_engagement").document(articleId)
                .collection("saves").document(currentUserId).get().await().exists()
            val myReactionDoc = db.collection("article_engagement").document(articleId)
                .collection("user_reactions").document(currentUserId).get().await()
            val myReaction = myReactionDoc.getString("emoji") ?: ""
            state = ArticleEngagement(
                isLiked = isLiked, isSaved = isSaved,
                likeCount = likeCount, saveCount = saveCount, viewCount = viewCount,
                myReaction = myReaction,
                reactions = reactions.mapValues { it.value.toInt() }
            )
        } catch (e: Exception) { }
    }
    return state
}

suspend fun toggleLike(articleId: String, userId: String, isCurrentlyLiked: Boolean) {
    val db = FirebaseFirestore.getInstance()
    val engRef = db.collection("article_engagement").document(articleId)
    val likeRef = engRef.collection("likes").document(userId)
    if (isCurrentlyLiked) {
        likeRef.delete().await()
        engRef.update("likeCount", FieldValue.increment(-1)).await()
    } else {
        likeRef.set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
        engRef.update("likeCount", FieldValue.increment(1)).await()
    }
}

suspend fun toggleSave(articleId: String, userId: String, isCurrentlySaved: Boolean) {
    val db = FirebaseFirestore.getInstance()
    val engRef = db.collection("article_engagement").document(articleId)
    val saveRef = engRef.collection("saves").document(userId)
    if (isCurrentlySaved) {
        saveRef.delete().await()
        engRef.update("saveCount", FieldValue.increment(-1)).await()
    } else {
        saveRef.set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
        engRef.update("saveCount", FieldValue.increment(1)).await()
    }
}

suspend fun setReaction(articleId: String, userId: String, emoji: String, previousEmoji: String) {
    val db = FirebaseFirestore.getInstance()
    val engRef = db.collection("article_engagement").document(articleId)
    val reactionRef = engRef.collection("user_reactions").document(userId)
    if (previousEmoji.isNotBlank()) {
        engRef.update("reactions.$previousEmoji", FieldValue.increment(-1)).await()
    }
    if (emoji.isNotBlank() && emoji != previousEmoji) {
        reactionRef.set(mapOf("emoji" to emoji)).await()
        engRef.update("reactions.$emoji", FieldValue.increment(1)).await()
    } else {
        reactionRef.delete().await()
    }
}

suspend fun incrementViewCount(articleId: String) {
    try {
        FirebaseFirestore.getInstance()
            .collection("article_engagement").document(articleId)
            .update("viewCount", FieldValue.increment(1)).await()
    } catch (e: Exception) { }
}

suspend fun postComment(articleId: String, userId: String, username: String, picUrl: String, text: String) {
    val text = Moderation.gate(text, "article_comment") ?: return
    val db = FirebaseFirestore.getInstance()
    val commentRef = db.collection("article_engagement").document(articleId).collection("comments").document()
    commentRef.set(mapOf(
        "id" to commentRef.id, "authorUid" to userId, "authorUsername" to username,
        "authorPicUrl" to picUrl, "text" to text,
        "timestamp" to FieldValue.serverTimestamp(), "likes" to 0
    )).await()
    db.collection("article_engagement").document(articleId)
        .update("commentCount", FieldValue.increment(1)).await()
}

suspend fun deleteComment(articleId: String, commentId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("article_engagement").document(articleId).collection("comments").document(commentId).delete().await()
    db.collection("article_engagement").document(articleId).update("commentCount", FieldValue.increment(-1)).await()
}

suspend fun fetchComments(articleId: String): List<ArticleComment> {
    return try {
        val db = FirebaseFirestore.getInstance()
        db.collection("article_engagement").document(articleId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get().await()
            .documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ArticleComment(
                    id = doc.id,
                    authorUid = data["authorUid"] as? String ?: "",
                    authorUsername = data["authorUsername"] as? String ?: "Anonymous",
                    authorPicUrl = data["authorPicUrl"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L,
                    likes = (data["likes"] as? Long)?.toInt() ?: 0
                )
            }
    } catch (e: Exception) { emptyList() }
}
// ─── ArticleWritePromptBanner ─────────────────────────────────────────────────
// Decorative parchment strip encouraging community writing

@Composable
fun ArticleWritePromptBanner(neonAlpha: Float) {
    val prompts = remember {
        listOf(
            "🎮 What's your all-time favourite retro game?",
            "👾 Share a hidden gem from the 8-bit era",
            "🕹️ Write about the game that got you into retro gaming",
            "📼 Which console do you miss the most?",
            "⭐ Review a classic — give it the rating it deserves"
        )
    }
    val prompt = remember { prompts.random() }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(ComicGlassBg, Color.White.copy(alpha = 0.70f), ComicGlassBg)))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            // Gloss highlight
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White, Color.Transparent))))
            // Decorative corner stamps
            Text("✍️", fontSize = 32.sp, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).graphicsLayer { alpha = 0.25f })
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookDark).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("WRITING PROMPT", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                }
                Text(prompt, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp)
                Text("Tap ✍️ above to share your take with the community", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}

// ─── ArticlesScreen ───────────────────────────────────────────────────────────

@Composable
fun ArticlesScreen(
    modifier: Modifier = Modifier,
    contentViewModel: ContentViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel? = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val articlesState by contentViewModel.articlesState.collectAsState()
    val liveArticlesState by contentViewModel.liveArticlesState.collectAsState()
    val freshNewsCount by contentViewModel.freshNewsCount.collectAsState()
    val favoriteIds by favoritesViewModel!!.favoriteIds.collectAsState()

    // Tab state — 0 = COMMUNITY, 1 = NEWS
    var selectedTab by remember { mutableStateOf(0) }

    // Community tab state
    var allFirebaseArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var isLoadingCommunity by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedSort by remember { mutableStateOf(ArticleSortOption.NEWEST) }
    var selectedReadingTime by remember { mutableStateOf(ReadingTimeFilter.ALL) }
    var showEditor by remember { mutableStateOf(false) }
    var fullScreenArticle by remember { mutableStateOf<ArticleItem?>(null) }
    var featuredArticle by remember { mutableStateOf<ArticleItem?>(null) }

    // News tab state
    var selectedLiveTopic by remember { mutableStateOf("ALL") }
    var newsFullScreenArticle by remember { mutableStateOf<ArticleItem?>(null) }

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    // Community articles from Firestore — LIVE: new posts appear without reopening the page
    DisposableEffect(Unit) {
        isLoadingCommunity = true
        val reg = FirebaseFirestore.getInstance().collection("articles")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { docs, err ->
                if (docs == null) {
                    if (err != null && allFirebaseArticles.isEmpty()) {
                        allFirebaseArticles = sampleArticles
                        featuredArticle = sampleArticles.firstOrNull()
                    }
                    isLoadingCommunity = false
                    return@addSnapshotListener
                }
                val live = docs.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val ts = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()
                    ArticleItem(
                        id = doc.id,
                        title = data["title"] as? String ?: "",
                        snippet = data["snippet"] as? String ?: "",
                        fullContent = data["fullContent"] as? String ?: "",
                        date = ts?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it) } ?: "",
                        author = data["authorUsername"] as? String ?: "Anonymous",
                        authorUid = data["authorUid"] as? String,
                        imageUrl = data["headerImageUrl"] as? String,
                        youtubeVideoId = data["youtubeVideoId"] as? String,
                        viewCount = (data["viewCount"] as? Long)?.toInt() ?: 0,
                        category = data["category"] as? String ?: "GAMING",
                        likeCount = (data["likeCount"] as? Long)?.toInt() ?: 0,
                        saveCount = (data["saveCount"] as? Long)?.toInt() ?: 0,
                        commentCount = (data["commentCount"] as? Long)?.toInt() ?: 0,
                        readingTimeMinutes = (data["readingTimeMinutes"] as? Long)?.toInt()
                            ?: estimateReadingTime(data["fullContent"] as? String ?: ""),
                        publishedAt = ts?.time ?: 0L
                    )
                }.filter { it.title.isNotBlank() }
                val ids = live.map { it.id }.toSet()
                allFirebaseArticles = (live + sampleArticles.filter { it.id !in ids }).distinctBy { it.id }
                featuredArticle = allFirebaseArticles.maxByOrNull { it.likeCount + it.viewCount }
                isLoadingCommunity = false
            }
        onDispose { reg.remove() }
    }

    // Filter + sort community articles
    val filteredArticles = remember(allFirebaseArticles, searchQuery, selectedCategory, selectedSort, selectedReadingTime) {
        var list = allFirebaseArticles
        if (searchQuery.isNotBlank()) list = list.filter { it.title.contains(searchQuery, ignoreCase = true) || it.snippet.contains(searchQuery, ignoreCase = true) }
        if (selectedCategory != "ALL") list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        list = when (selectedReadingTime) {
            ReadingTimeFilter.QUICK -> list.filter { it.readingTimeMinutes < 2 }
            ReadingTimeFilter.MEDIUM -> list.filter { it.readingTimeMinutes in 2..5 }
            ReadingTimeFilter.LONG -> list.filter { it.readingTimeMinutes > 5 }
            else -> list
        }
        list = when (selectedSort) {
            ArticleSortOption.NEWEST -> list.sortedByDescending { it.publishedAt }
            ArticleSortOption.OLDEST -> list.sortedBy { it.publishedAt }
            ArticleSortOption.MOST_LIKED -> list.sortedByDescending { it.likeCount }
            ArticleSortOption.MOST_VIEWED -> list.sortedByDescending { it.viewCount }
        }
        list
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())

        // ── Full screen article reader (community) ─────────────────────────
        fullScreenArticle?.let { article ->
            ArticleDetailScreen(
                article = article,
                currentUser = currentUser,
                userProfile = userProfile,
                onBack = { fullScreenArticle = null },
                onNavigateToAuthor = { }
            )
            return@Box
        }

        // ── Full screen news article reader (in-app WebView) ───────────────
        newsFullScreenArticle?.let { article ->
            NewsArticleReaderScreen(
                article = article,
                onBack = { newsFullScreenArticle = null }
            )
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Page header (Discover style) ───────────────────────────────
            ComicPageHeader(
                title = "ARTICLES",
                subtitle = if (selectedTab == 0) "${filteredArticles.size} community articles" else "Live gaming news",
                marquee = pageMarqueeFor("ARTICLES")
            ) {
                if (selectedTab == 0 && currentUser != null) {
                    ComicIconButton(Icons.Filled.Edit, "Write article") { showEditor = true }
                }
                if (selectedTab == 1) {
                    ComicIconButton(Icons.Filled.Refresh, "Refresh news") { contentViewModel.fetchLiveArticles() }
                }
            }

            // ── Tab bar (ink underline) ─────────────────────────────────────
            InkTabRow(
                tabs = listOf("✍️ COMMUNITY", "📡 NEWS"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Tab content ────────────────────────────────────────────────
            when (selectedTab) {
                0 -> CommunityArticlesTab(
                    articles = filteredArticles,
                    featuredArticle = featuredArticle,
                    isLoading = isLoadingCommunity,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    selectedSort = selectedSort,
                    selectedReadingTime = selectedReadingTime,
                    favoriteIds = favoriteIds,
                    currentUser = currentUser,
                    favoritesViewModel = favoritesViewModel,
                    neonAlpha = neonAlpha,
                    onSearchChange = { searchQuery = it },
                    onCategoryChange = { selectedCategory = it },
                    onSortChange = { selectedSort = it },
                    onReadingTimeChange = { selectedReadingTime = it },
                    onOpenArticle = { fullScreenArticle = it },
                    onClearFilters = { searchQuery = ""; selectedCategory = "ALL"; selectedSort = ArticleSortOption.NEWEST; selectedReadingTime = ReadingTimeFilter.ALL }
                )
                1 -> NewsArticlesTab(
                    liveArticlesState = liveArticlesState,
                    selectedTopic = selectedLiveTopic,
                    neonAlpha = neonAlpha,
                    onTopicChange = { topic ->
                        selectedLiveTopic = topic
                        if (topic != "ALL") contentViewModel.fetchLiveArticles(topic)
                        else contentViewModel.fetchLiveArticles()
                    },
                    onOpenArticle = { newsFullScreenArticle = it },
                    onRetry = { contentViewModel.fetchLiveArticles() },
                    freshCount = freshNewsCount,
                    onFreshSeen = { contentViewModel.clearFreshNews() }
                )
            }
        }

        // ── Write article FAB (community tab only) ─────────────────────────
        if (selectedTab == 0 && currentUser != null) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                val fabBob by rememberGlowRange(-3f, 3f)
                Box(modifier = Modifier.graphicsLayer { translationY = fabBob * density }) {
                    Box(modifier = Modifier.size(58.dp).offset(x = 4.dp, y = 4.dp).clip(CircleShape).background(ScrapbookDark))
                    Box(
                        modifier = Modifier.size(58.dp).clip(CircleShape)
                            .background(CGreen)
                            .border(3.dp, ScrapbookDark, CircleShape)
                            .clickable { Chiptune.play(Chiptune.Sfx.POP); showEditor = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Write article", tint = ScrapbookDark, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
    }

    // ── Article editor overlay ─────────────────────────────────────────────
    if (showEditor) {
        ArticleEditorScreen(
            currentUser = currentUser,
            userProfile = userProfile,
            onDismiss = { showEditor = false },
            onPublished = { newArticle ->
                showEditor = false
                fullScreenArticle = newArticle
            }
        )
    }
}

// ─── Community Articles Tab ───────────────────────────────────────────────────

@Composable
fun CommunityArticlesTab(
    articles: List<ArticleItem>,
    featuredArticle: ArticleItem?,
    isLoading: Boolean,
    searchQuery: String,
    selectedCategory: String,
    selectedSort: ArticleSortOption,
    selectedReadingTime: ReadingTimeFilter,
    favoriteIds: Set<String>,
    currentUser: Any?,
    favoritesViewModel: FavoritesViewModel?,
    neonAlpha: Float,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSortChange: (ArticleSortOption) -> Unit,
    onReadingTimeChange: (ReadingTimeFilter) -> Unit,
    onOpenArticle: (ArticleItem) -> Unit,
    onClearFilters: () -> Unit
) {
    val progressContext = LocalContext.current
    LaunchedEffect(Unit) { ArticleProgressStore.load(progressContext) }
    val inProgress = articles.filter { (ArticleProgressStore.progress[it.id] ?: 0) in 5..94 }.take(8)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
    ) {
        // Search bar
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ComicGlassBg)
                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(14.dp))
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search articles...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
                        }
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Category filter chips
        item {
            ArticleCategoryFilter(selectedCategory = selectedCategory, onCategoryChange = onCategoryChange)
        }

        // Sort + reading time filter
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArticleSortBar(selectedSort = selectedSort, onSortChange = onSortChange, modifier = Modifier.weight(1f))
                ArticleReadingTimeFilter(selectedFilter = selectedReadingTime, onFilterChange = onReadingTimeChange, modifier = Modifier.weight(1f))
            }
        }

        // 📖 Continue reading — articles you started but didn't finish
        if (inProgress.isNotEmpty() && searchQuery.isBlank()) {
            item { ContinueReadingRow(articles = inProgress, onOpen = onOpenArticle) }
        }

        // Featured article hero
        if (featuredArticle != null && searchQuery.isBlank() && selectedCategory == "ALL" && selectedSort == ArticleSortOption.NEWEST && selectedReadingTime == ReadingTimeFilter.ALL) {
            item {
                FeaturedArticleHeroCard(article = featuredArticle, onRead = { onOpenArticle(featuredArticle) })
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
            }
        }

        // Write Your Story card (only when no filters active and no articles yet)
        if (searchQuery.isBlank() && selectedCategory == "ALL") {
            item {
                ArticleWritePromptBanner(neonAlpha = neonAlpha)
            }
        }

        // Section header
        item {
            RetroSectionHeader(title = "COMMUNITY ARTICLES", emoji = "✍️")
        }

        when {
            isLoading -> items(4) { ShimmerArticleCard() }
            articles.isEmpty() -> item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (searchQuery.isNotBlank() || selectedCategory != "ALL") "🔍" else "📝", fontSize = 48.sp)
                        Text(
                            if (searchQuery.isNotBlank() || selectedCategory != "ALL") "No articles match your filters"
                            else "No community articles yet.\nBe the first to write one!",
                            fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        if (searchQuery.isNotBlank() || selectedCategory != "ALL") {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onClearFilters() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text("CLEAR FILTERS", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            else -> itemsIndexed(items = articles, key = { _, a -> a.id }) { index, article ->
                SpringEntrance(delayMs = index * 60) {
                    ArticleCard(
                        article = article,
                        gradientColors = articleGradientColorsList[index % articleGradientColorsList.size],
                        initiallyExpanded = false,
                        isBookmarked = favoriteIds.contains(article.id),
                        onBookmarkToggle = {
                            favoritesViewModel?.toggleFavorite(
                                FavoriteItem(id = article.id, title = article.title, description = article.snippet, thumbnailUrl = article.imageUrl, webUrl = "", category = "ARTICLE", creator = article.author, year = article.date)
                            )
                        },
                        onOpenFullScreen = { onOpenArticle(it) },
                        animationDelay = index * 60
                    )
                    if ((ArticleProgressStore.progress[article.id] ?: 0) >= 95) {
                        ArticleReadStamp(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 6.dp))
                    }
                }
            }
        }
    }
}

// ─── News Articles Tab ────────────────────────────────────────────────────────

@Composable
fun NewsArticlesTab(
    liveArticlesState: LiveArticlesState,
    selectedTopic: String,
    neonAlpha: Float,
    onTopicChange: (String) -> Unit,
    onOpenArticle: (ArticleItem) -> Unit,
    onRetry: () -> Unit,
    freshCount: Int = 0,
    onFreshSeen: () -> Unit = {}
) {
    val liveTopics = listOf("ALL", "retro gaming", "video game", "Nintendo", "pixel art games", "arcade games", "indie games retro")

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ComicGlassBg),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
    ) {
        // News header card
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Green stripe
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                    // Gloss highlight
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent))))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, CGreen, CircleShape), contentAlignment = Alignment.Center) { Text("📡", fontSize = 22.sp) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LIVE GAMING NEWS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
                            Text("Tap any article to read it right here in RetroHub", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp)
                        }
                        PulsingDot(color = CGreen, size = 8.dp)
                    }
                }
            }
        }

        // Topic filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                liveTopics.forEach { topic ->
                    val isSelected = selectedTopic == topic
                    var pressed by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "topicScale_$topic")
                    Box(
                        modifier = Modifier.scale(scale)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.46f))
                            .border(width = if (isSelected) 1.5.dp else 1.dp, color = if (isSelected) CGreen.copy(alpha = 0.8f) else ScrapbookBorder, shape = RoundedCornerShape(20.dp))
                            .clickable { pressed = true; onTopicChange(topic) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(topic.replaceFirstChar { it.uppercase() }, fontFamily = BangersFontFamily, color = if (isSelected) CGreen else ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                }
            }
        }

        // Section header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Retro ticker strip
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScrapbookDark)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        PulsingDot(color = CGreen, size = 6.dp)
                        Text(
                            "LIVE  •  RETRO GAMING  •  PIXEL ART  •  INDIE  •  NINTENDO  •  ARCADE  •  CLASSICS  •  LIVE",
                            fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp, letterSpacing = 1.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = neonAlpha)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FROM THE WEB", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                }
            }
        }

        // "▲ N NEW STORIES" — appears when the background refresh finds fresh news
        if (freshCount > 0) {
            item(key = "fresh_pill") {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "▲ $freshCount NEW ${if (freshCount == 1) "STORY" else "STORIES"} · TAP TO DISMISS",
                        modifier = Modifier.stampIn("fresh_$freshCount")
                            .clip(RoundedCornerShape(20.dp)).background(CGreen)
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                            .clickable { Chiptune.play(Chiptune.Sfx.BLIP); onFreshSeen() }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp, letterSpacing = 1.sp
                    )
                }
            }
        }

        // Articles content
        when (val state = liveArticlesState) {
            is LiveArticlesState.Loading -> items(4) { ShimmerArticleCard() }
            is LiveArticlesState.Error -> item {
                GameOverState(message = "Couldn't load live articles.", onRetry = { onRetry() })
            }
            is LiveArticlesState.Success -> itemsIndexed(items = state.items, key = { _, item -> item.id }) { index, article ->
                SpringEntrance(delayMs = index * 50) {
                    NewsArticleCard(article = article, onTap = { onOpenArticle(article) }, animationDelay = index * 50)
                }
            }
        }
    }
}

// ─── NewsArticleCard ──────────────────────────────────────────────────────────
// Card for news articles — shows source badge, tapping opens in-app reader

@Composable
fun NewsArticleCard(article: ArticleItem, onTap: () -> Unit, animationDelay: Int = 0) {
    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(350, delayMillis = animationDelay), label = "newsCardAlpha_${article.id}")
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 20f, animationSpec = tween(350, delayMillis = animationDelay, easing = LinearOutSlowInEasing), label = "newsCardOffset_${article.id}")
    LaunchedEffect(Unit) { visible = true }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "newsCardScale_${article.id}")
    val accentColor = categoryColor(article.category)

    Box(
        modifier = Modifier.fillMaxWidth()
            .offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }
            .scale(pressScale)
    ) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                .clickable { pressed = true; onTap() }
        ) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            // Gloss shine
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent))))
            Column {
                // Header image
                article.imageUrl?.let { imageUrl ->
                    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        AsyncImage(model = imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(imageUrl).fillMaxSize())
                        // Source badge
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                            HoloBadge(label = article.sourceName.ifBlank { "NEWS" }, cornerRadius = 8.dp, fontSize = 11.sp)
                        }
                        // Category badge
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                            HoloBadge(label = "${categoryEmoji(article.category)} ${article.category}", cornerRadius = 8.dp, fontSize = 10.sp)
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)))))
                        ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.05f)
                    }
                }

                Column(modifier = Modifier.padding(14.dp)) {
                    // No image source badge
                    if (article.imageUrl == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            HoloBadge(label = article.sourceName.ifBlank { "NEWS" }, cornerRadius = 6.dp, fontSize = 11.sp)
                            HoloBadge(label = "${categoryEmoji(article.category)} ${article.category}", cornerRadius = 6.dp, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 0.5.sp, maxLines = 2, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(article.snippet, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 20.sp, maxLines = 3)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FreshBadge(article.publishedAt)
                            Text(if (article.publishedAt > 0) LiveFeeds.friendlyDate(article.publishedAt) else article.date,
                                fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
                        }
                        // "Read in app" chip
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(ScrapbookDark)
                                .border(1.dp, CGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Article, null, tint = CGreen, modifier = Modifier.size(12.dp))
                                Text("READ", fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 16.dp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
}

// ─── NewsArticleReaderScreen ──────────────────────────────────────────────────
// In-app reader — loads article URL in WebView, with clean header + back button

@Composable
fun NewsArticleReaderScreen(article: ArticleItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var pageTitle by remember { mutableStateOf(article.title) }
    var isLoading by remember { mutableStateOf(true) }
    val accentColor = categoryColor(article.category)

    Column(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {

        // Top bar
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(ComicGlassBg, Color.White.copy(alpha = 0.46f))))
                .border(BorderStroke(1.dp, ScrapbookBorder))
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null, tint = ScrapbookDark) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(article.sourceName.ifBlank { "Gaming News" }, fontFamily = BangersFontFamily, color = CGreen.copy(alpha = 0.85f), fontSize = 12.sp, letterSpacing = 1.sp)
                    Text(article.title, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                }
                if (article.sourceUrl.isNotBlank()) {
                    IconButton(onClick = {
                        try { uriHandler.openUri(article.sourceUrl) } catch (e: Exception) { }
                    }) {
                        Icon(Icons.Filled.OpenInNew, null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = { shareArticle(context, article) }) {
                    Icon(Icons.Filled.Share, null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }

        // Source/category info strip
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(accentColor.copy(alpha = 0.12f))
                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(accentColor.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = accentColor, fontSize = 10.sp)
                    }
                    if (article.sourceName.isNotBlank()) Text("via ${article.sourceName}", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp)
                }
                Text(article.date, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
            }
        }

        // WebView or fallback content
        Box(modifier = Modifier.fillMaxSize()) {
            if (article.sourceUrl.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isLoading = true
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    view?.title?.let { if (it.isNotBlank()) pageTitle = it }
                                }
                            }
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            loadUrl(article.sourceUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = CGreen, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Text("Loading article...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // Fallback: show article content directly
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(ComicGlassBg),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    article.imageUrl?.let { imageUrl ->
                        item { AsyncImage(model = imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(imageUrl).fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp))) }
                    }
                    item { TypewriterText(article.title, androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, lineHeight = 32.sp)) }
                    item { Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 15.sp, lineHeight = 24.sp) }
                    if (article.fullContent.isNotBlank() && article.fullContent != article.snippet) {
                        item { Text(article.fullContent, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 14.sp, lineHeight = 22.sp) }
                    }
                }
            }
        }
    }
}
// ─── ArticleCard ──────────────────────────────────────────────────────────────

@Composable
fun ArticleCard(
    article: ArticleItem,
    gradientColors: List<Color>,
    initiallyExpanded: Boolean = false,
    isBookmarked: Boolean = false,
    onBookmarkToggle: () -> Unit = {},
    onOpenFullScreen: (ArticleItem) -> Unit = {},
    animationDelay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(400, delayMillis = animationDelay), label = "articleCardAlpha_${article.id}")
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 24f, animationSpec = tween(400, delayMillis = animationDelay, easing = LinearOutSlowInEasing), label = "articleCardOffset_${article.id}")
    LaunchedEffect(Unit) { visible = true }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "articleCardScale_${article.id}")
    val accentColor = categoryColor(article.category)

    val neonAlpha by rememberGlowRange(0.3f, 0.8f)
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "acPress_${article.id}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "acShOff_${article.id}")

    Box(
        modifier = Modifier.fillMaxWidth()
            .offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }
            .scale(pressScale)
    ) {
        // Glow halo
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(16.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(16.dp)).background(CGreen))
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                .clickable { pressed = true; onOpenFullScreen(article) }
        ) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            // Gloss shine
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent))))
            Column {
                // Header image — local drawable or remote URL
                val hasImage = article.imageResId != null || article.imageUrl != null
                if (hasImage) {
                    // Green stripe
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                    Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                        val kbT = rememberInfiniteTransition(label = "articleKB_${article.id}")
                        val kbScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 12000; 1f at 0; 1.06f at 6000; 1f at 12000 }, RepeatMode.Restart), label = "articleKBScale_${article.id}")
                        when {
                            article.imageResId != null -> Image(painter = painterResource(id = article.imageResId), contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                            article.imageUrl != null -> AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize().scale(kbScale))
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
                        // Category badge
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                            HoloBadge(label = "${categoryEmoji(article.category)} ${article.category}", cornerRadius = 8.dp, fontSize = 10.sp)
                        }
                        // Reading time badge
                        if (article.readingTimeMinutes > 0) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                                Text("⏱ ${article.readingTimeMinutes} min", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                            }
                        }
                        ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.05f)
                    }
                }

                Column(modifier = Modifier.padding(14.dp)) {
                    // No-image category badge
                    if (!hasImage) {
                        HoloBadge(label = "${categoryEmoji(article.category)} ${article.category}", cornerRadius = 6.dp, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 0.5.sp, maxLines = 2, lineHeight = 24.sp, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 13.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Author + date row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        // Author badge — white glass pill + stamp tilt + green shadow
                        Box(modifier = Modifier.graphicsLayer { rotationZ = -1.5f }) {
                            Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(14.dp)).background(CGreen))
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(1.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Text(article.author.take(1).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 9.sp)
                                }
                                Text(article.author, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FreshBadge(article.publishedAt)
                            Text(article.date, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Stats + actions row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (article.likeCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Text("❤️", fontSize = 12.sp); Text("${article.likeCount}", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
                            if (article.viewCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Filled.Visibility, null, tint = ScrapbookTextMuted, modifier = Modifier.size(13.dp)); Text("${formatCount(article.viewCount)}", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
                            if (article.commentCount > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) { Icon(Icons.Filled.ChatBubbleOutline, null, tint = ScrapbookTextMuted, modifier = Modifier.size(13.dp)); Text("${article.commentCount}", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Bookmark
                            IconButton(onClick = onBookmarkToggle, modifier = Modifier.size(28.dp)) {
                                Icon(if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, tint = if (isBookmarked) CGreen else ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                            }
                            // Read button
                            RetroGlassButton(
                                text = "READ →",
                                onClick = { pressed = true; onOpenFullScreen(article) },
                                cornerRadius = 8.dp
                            )
                        }
                    }
                }
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 16.dp)
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
}

// ─── FeaturedArticleHeroCard ──────────────────────────────────────────────────

@Composable
fun FeaturedArticleHeroCard(article: ArticleItem, onRead: () -> Unit) {
    val accentColor = categoryColor(article.category)
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "featuredHeroScale")
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Box(modifier = Modifier.fillMaxWidth().scale(pressScale)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(18.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(18.dp))
                .clickable { pressed = true; onRead() }
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                val kbT = rememberInfiniteTransition(label = "featuredKB")
                val kbScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 14000; 1f at 0; 1.07f at 7000; 1f at 14000 }, RepeatMode.Restart), label = "featuredKBScale")
                when {
                    article.imageResId != null -> Image(painter = painterResource(id = article.imageResId), contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).scale(kbScale))
                    article.imageUrl != null -> AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize().clip(RoundedCornerShape(18.dp)).scale(kbScale))
                    else -> Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.5f), ScrapbookDark))))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))

                // FEATURED badge
                Box(modifier = Modifier.align(Alignment.TopStart).padding(14.dp)) {
                    HoloBadge(label = "⭐ FEATURED", cornerRadius = 8.dp, fontSize = 11.sp)
                }

                // Category + reading time
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HoloBadge(label = "${categoryEmoji(article.category)} ${article.category}", cornerRadius = 8.dp, fontSize = 10.sp)
                    if (article.readingTimeMinutes > 0) Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 8.dp, vertical = 5.dp)) { Text("⏱ ${article.readingTimeMinutes} min", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) }
                }

                ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.05f)
                // Bottom content
                Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp)) {
                    Text(article.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(article.snippet, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.3f)).border(1.dp, accentColor.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) { Text(article.author.take(1).uppercase(), fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp) }
                            Text(article.author, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.wrapContentSize()) {
                            Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(20.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(CGreen).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text("READ →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                                ComicShimmer(Modifier.matchParentSize(), cornerRadius = 20.dp)
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
}

// ─── ShimmerArticleCard ───────────────────────────────────────────────────────

@Composable
fun ShimmerArticleCard() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ComicGlassBg).border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp))) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(180.dp), cornerRadius = 10.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(22.dp), cornerRadius = 6.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp), cornerRadius = 4.dp)
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp), cornerRadius = 4.dp)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    ShimmerBox(modifier = Modifier.width(80.dp).height(12.dp), cornerRadius = 4.dp)
                    ShimmerBox(modifier = Modifier.width(60.dp).height(12.dp), cornerRadius = 4.dp)
                }
            }
        }
    }
}

// ─── ArticleCategoryFilter ────────────────────────────────────────────────────

@Composable
fun ArticleCategoryFilter(selectedCategory: String, onCategoryChange: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp)
    ) {
        itemsIndexed(articleCategories) { jumpIndex, category ->
            Box(modifier = Modifier.jumpIn(jumpIndex)) {
            val isSelected = selectedCategory == category
            val accentColor = if (category == "ALL") CGreen else categoryColor(category)
            var pressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "catScale_$category")
            // White glass chip + green shadow when selected
            Box(modifier = Modifier.scale(scale)) {
                if (isSelected) {
                    Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp).clip(RoundedCornerShape(20.dp)).background(CGreen))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(width = if (isSelected) 1.5.dp else 1.dp, color = if (isSelected) ScrapbookDark else ScrapbookBorder, shape = RoundedCornerShape(20.dp))
                        .clickable { pressed = true; onCategoryChange(category) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (category != "ALL") Text(categoryEmoji(category), fontSize = 12.sp)
                        Text(category, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.7f), fontSize = 11.sp, letterSpacing = 0.5.sp)
                    }
                }
            }
            LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
                    }
        }
    }
}

// ─── ArticleSortBar ───────────────────────────────────────────────────────────

@Composable
fun ArticleSortBar(selectedSort: ArticleSortOption, onSortChange: (ArticleSortOption) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ComicGlassBg)
                .border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Sort, null, tint = ScrapbookDark.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Text(selectedSort.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                }
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(ComicGlassBg).border(1.dp, ScrapbookBorder, RoundedCornerShape(8.dp))) {
            ArticleSortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontFamily = BangersFontFamily, color = if (selectedSort == option) CGreen else ScrapbookDark, fontSize = 13.sp) },
                    onClick = { onSortChange(option); expanded = false },
                    modifier = Modifier.background(if (selectedSort == option) ScrapbookDark else Color.Transparent)
                )
            }
        }
    }
}

// ─── ArticleReadingTimeFilter ─────────────────────────────────────────────────

@Composable
fun ArticleReadingTimeFilter(selectedFilter: ReadingTimeFilter, onFilterChange: (ReadingTimeFilter) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ComicGlassBg)
                .border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⏱", fontSize = 13.sp)
                    Text(selectedFilter.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                }
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(ComicGlassBg).border(1.dp, ScrapbookBorder, RoundedCornerShape(8.dp))) {
            ReadingTimeFilter.values().forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label, fontFamily = BangersFontFamily, color = if (selectedFilter == filter) CGreen else ScrapbookDark, fontSize = 13.sp) },
                    onClick = { onFilterChange(filter); expanded = false },
                    modifier = Modifier.background(if (selectedFilter == filter) ScrapbookDark else Color.Transparent)
                )
            }
        }
    }
}
// ─── ArticleTableOfContents ───────────────────────────────────────────────────
// Simple numbered list of section headings, built from "##" markers in content.

@Composable
fun ArticleTableOfContents(headings: List<String>) {
    if (headings.isEmpty()) return
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(14.dp)).padding(16.dp)) {
            // Gloss
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent))))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.List, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                    Text("IN THIS ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                headings.forEachIndexed { index, heading ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 5.dp)
                    ) {
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(CGreen), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                        }
                        Text(heading, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ─── StyledArticleContentWithLargeInitial ────────────────────────────────────
// Renders article body text. Lines starting with "##" become section headings;
// the very first regular paragraph gets a large drop-cap initial letter.

@Composable
fun StyledArticleContentWithLargeInitial(content: String, accentColor: Color) {
    if (content.isBlank()) return
    val paragraphs = remember(content) {
        content.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }
    var firstParagraphRendered = false

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        paragraphs.forEach { line ->
            when {
                line.startsWith("##") -> {
                    val heading = line.trimStart('#').trim()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
                        Text(heading, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 19.sp, letterSpacing = 0.5.sp)
                    }
                }
                !firstParagraphRendered -> {
                    firstParagraphRendered = true
                    val firstChar = line.firstOrNull()?.toString() ?: ""
                    val rest = if (line.length > 1) line.substring(1) else ""
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Top) {
                        if (firstChar.isNotBlank()) {
                            Text(firstChar, fontFamily = BangersFontFamily, color = accentColor, fontSize = 48.sp, lineHeight = 44.sp)
                        }
                        Text(
                            rest, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 15.sp,
                            lineHeight = 24.sp, modifier = Modifier.weight(1f).padding(top = 6.dp)
                        )
                    }
                }
                else -> {
                    Text(line, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 15.sp, lineHeight = 24.sp)
                }
            }
        }
    }
}

// ─── ArticleDetailScreen ──────────────────────────────────────────────────────

@Composable
fun ArticleDetailScreen(
    article: ArticleItem,
    currentUser: Any?,
    userProfile: UserProfileData?,
    onBack: () -> Unit,
    onNavigateToAuthor: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var engagement by remember(article.id) { mutableStateOf(ArticleEngagement()) }
    var comments by remember { mutableStateOf<List<ArticleComment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var isSubmittingComment by remember { mutableStateOf(false) }
    var readingProgress by remember { mutableStateOf(0f) }

    val neonAlpha by rememberGlowRange(0.4f, 1f)
    val accentColor = categoryColor(article.category)
    val headings = remember(article.fullContent) { extractHeadings(article.fullContent) }

    // ── HP-bar reading progress + STAGE CLEAR ──
    val achievementsViewModel: AchievementsViewModel = viewModel()
    val detailListState = rememberLazyListState()
    val stageClearBurst = rememberBurstState()
    var contentItemIndex by remember(article.id) { mutableStateOf(-1) }
    var stageCleared by remember(article.id) { mutableStateOf(false) }

    LaunchedEffect(detailListState, article.id) {
        snapshotFlow {
            val info = detailListState.layoutInfo
            val contentItem = info.visibleItemsInfo.firstOrNull { it.key == "article_content" }
            when {
                contentItem != null -> {
                    contentItemIndex = contentItem.index
                    if (contentItem.size <= 0) 0f
                    else ((info.viewportEndOffset - contentItem.offset).toFloat() / contentItem.size.toFloat()).coerceIn(0f, 1f)
                }
                contentItemIndex >= 0 && detailListState.firstVisibleItemIndex > contentItemIndex -> 1f
                else -> 0f
            }
        }.collect { p ->
            readingProgress = p
            if (article.id.isNotBlank()) ArticleProgressStore.save(context, article.id, (p * 100).toInt())
            if (p >= 0.95f && !stageCleared && article.id.isNotBlank()) {
                stageCleared = true
                val prefs = context.getSharedPreferences("article_read_xp", Context.MODE_PRIVATE)
                val done = prefs.getStringSet("read_ids", emptySet())!!.toMutableSet()
                if (article.id !in done) {
                    done.add(article.id)
                    prefs.edit().putStringSet("read_ids", done).apply()
                    stageClearBurst.fire("STAGE CLEAR!", CGreenMint)
                    Chiptune.play(Chiptune.Sfx.POWER_UP)
                    achievementsViewModel.awardXP(10, "READ")
                    AchievementToastBus.queue.add(
                        Badge(
                            id = "read_${article.id}_${System.currentTimeMillis()}",
                            name = "ARTICLE FINISHED",
                            description = "+10 XP",
                            emoji = "📖",
                            color = CGreen,
                            isEarned = true
                        )
                    )
                }
            }
        }
    }

    // Fetch engagement on open
    LaunchedEffect(article.id, userId) {
        if (article.id.isBlank() || userId.isBlank()) return@LaunchedEffect
        incrementViewCount(article.id)
        try {
            val db = FirebaseFirestore.getInstance()
            val engDoc = db.collection("article_engagement").document(article.id).get().await()
            val likeCount = (engDoc.getLong("likeCount") ?: article.likeCount.toLong()).toInt()
            val saveCount = (engDoc.getLong("saveCount") ?: article.saveCount.toLong()).toInt()
            val viewCount = (engDoc.getLong("viewCount") ?: article.viewCount.toLong()).toInt()
            val reactions = engDoc.get("reactions") as? Map<String, Long> ?: emptyMap()
            val commentCount = (engDoc.getLong("commentCount") ?: article.commentCount.toLong()).toInt()
            val isLiked = db.collection("article_engagement").document(article.id).collection("likes").document(userId).get().await().exists()
            val isSaved = db.collection("article_engagement").document(article.id).collection("saves").document(userId).get().await().exists()
            val myReactionDoc = db.collection("article_engagement").document(article.id).collection("user_reactions").document(userId).get().await()
            val myReaction = myReactionDoc.getString("emoji") ?: ""
            engagement = ArticleEngagement(isLiked = isLiked, isSaved = isSaved, likeCount = likeCount, saveCount = saveCount, viewCount = viewCount, myReaction = myReaction, reactions = reactions.mapValues { it.value.toInt() })
        } catch (e: Exception) { }
    }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Reading progress — retro HP bar (sticky above the scrolling reader)
            ArticleReadingHpBar(progress = readingProgress)

            LazyColumn(
                state = detailListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Hero image + back button
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        val kbT = rememberInfiniteTransition(label = "detailKB")
                        val kbScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 16000; 1f at 0; 1.06f at 8000; 1f at 16000 }, RepeatMode.Restart), label = "detailKBScale")
                        when {
                            article.imageResId != null -> Image(painter = painterResource(id = article.imageResId), contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                            article.imageUrl != null -> AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize().scale(kbScale))
                            else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.6f), ScrapbookDark))))
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.6f)))))

                        // Back button
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        // Share button
                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)).clickable { shareArticle(context, article) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Category badge at bottom
                        Box(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).clip(RoundedCornerShape(8.dp)).background(accentColor).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("${categoryEmoji(article.category)} ${article.category}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                // Title + meta
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        TypewriterText(article.title, androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.5.sp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            AuthorAvatarPill(article = article, onTap = { if (!article.authorUid.isNullOrBlank()) onNavigateToAuthor(article.authorUid) })
                            Column(horizontalAlignment = Alignment.End) {
                                Text(article.date, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                if (article.readingTimeMinutes > 0) Text("⏱ ${article.readingTimeMinutes} min read", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Stats bar
                item { ArticleStatsBar(engagement = engagement, neonAlpha = neonAlpha) }

                // Author greeter / Habbo
                if (!article.authorUid.isNullOrBlank()) {
                    item { AuthorHabboGreeter(authorUid = article.authorUid, authorName = article.author, onTapProfile = { onNavigateToAuthor(article.authorUid) }) }
                }

                // Table of contents
                if (headings.size >= 3) {
                    item { ArticleTableOfContents(headings = headings) }
                }

                // Full content
                item(key = "article_content") {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        StyledArticleContentWithLargeInitial(content = article.fullContent, accentColor = accentColor)
                    }
                }

                // Like + save bar
                if (userId.isNotBlank()) {
                    item {
                        ArticleLikeSaveBar(
                            engagement = engagement,
                            onLike = {
                                scope.launch {
                                    val wasLiked = engagement.isLiked
                                    engagement = engagement.copy(isLiked = !wasLiked, likeCount = if (wasLiked) engagement.likeCount - 1 else engagement.likeCount + 1)
                                    try { toggleLike(article.id, userId, wasLiked) } catch (e: Exception) { engagement = engagement.copy(isLiked = wasLiked) }
                                }
                            },
                            onSave = {
                                scope.launch {
                                    val wasSaved = engagement.isSaved
                                    engagement = engagement.copy(isSaved = !wasSaved, saveCount = if (wasSaved) engagement.saveCount - 1 else engagement.saveCount + 1)
                                    try { toggleSave(article.id, userId, wasSaved) } catch (e: Exception) { engagement = engagement.copy(isSaved = wasSaved) }
                                }
                            },
                            onShare = { shareArticle(context, article) }
                        )
                    }
                }

                // Reaction bar
                if (userId.isNotBlank()) {
                    item {
                        ArticleReactionBar(
                            engagement = engagement,
                            onReact = { emoji ->
                                scope.launch {
                                    val previous = engagement.myReaction
                                    val newReactions = engagement.reactions.toMutableMap()
                                    if (!previous.isNullOrBlank()) newReactions[previous] = (newReactions[previous] ?: 1) - 1
                                    val newEmoji = if (emoji == previous) "" else emoji
                                    if (newEmoji.isNotBlank()) newReactions[newEmoji] = (newReactions[newEmoji] ?: 0) + 1
                                    engagement = engagement.copy(myReaction = newEmoji, reactions = newReactions)
                                    try { setReaction(article.id, userId, newEmoji, previous ?: "") } catch (e: Exception) { }
                                }
                            }
                        )
                    }
                }

                // Related articles
                item { RelatedArticlesRow(currentArticleId = article.id, category = article.category, onOpenArticle = { /* handled at screen level */ }) }

                // Comments section
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).clickable {
                        showComments = !showComments
                        if (showComments && comments.isEmpty()) {
                            isLoadingComments = true
                            scope.launch { comments = fetchComments(article.id); isLoadingComments = false }
                        }
                    }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.ChatBubbleOutline, null, tint = CGreenDeep, modifier = Modifier.size(18.dp))
                                Text("COMMENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                                if (engagement.reactions["comment"] != null || comments.isNotEmpty()) {
                                    Box(modifier = Modifier.clip(CircleShape).background(CGreen.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("${comments.size}", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 12.sp) }
                                }
                            }
                            Icon(if (showComments) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (showComments) {
                    // Comment input
                    if (userId.isNotBlank()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                                    if (!userProfile?.profilePictureUrl.isNullOrBlank()) AsyncImage(model = userProfile!!.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(userProfile!!.profilePictureUrl).fillMaxSize())
                                    else Text(userProfile?.username?.take(1)?.uppercase() ?: "?", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
                                }
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(ComicGlassBg).border(1.dp, ScrapbookBorder, RoundedCornerShape(12.dp))) {
                                    OutlinedTextField(value = commentText, onValueChange = { if (it.length <= 300) commentText = it }, placeholder = { Text("Add a comment...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp) }, singleLine = false, maxLines = 4, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookDark), modifier = Modifier.fillMaxWidth())
                                }
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (commentText.isNotBlank()) ScrapbookDark else ScrapbookBorder).border(1.dp, if (commentText.isNotBlank()) CGreen.copy(alpha = 0.5f) else ScrapbookBorder, CircleShape).clickable {
                                    if (commentText.isNotBlank() && !isSubmittingComment) {
                                        isSubmittingComment = true
                                        scope.launch {
                                            try {
                                                postComment(article.id, userId, userProfile?.username ?: "Anonymous", userProfile?.profilePictureUrl ?: "", commentText.trim())
                                                commentText = ""
                                                comments = fetchComments(article.id)
                                            } catch (e: Exception) { }
                                            isSubmittingComment = false
                                        }
                                    }
                                }, contentAlignment = Alignment.Center) {
                                    if (isSubmittingComment) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CGreen, strokeWidth = 2.dp)
                                    else Icon(Icons.Filled.Send, null, tint = if (commentText.isNotBlank()) CGreen else ScrapbookTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (isLoadingComments) {
                        items(3) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), cornerRadius = 10.dp) }
                    } else {
                        items(comments.distinctBy { it.id }, key = { "c_" + it.id.ifBlank { it.hashCode().toString() } }) { comment ->
                            ArticleCommentCard(comment = comment, currentUserId = userId, onDelete = {
                                scope.launch { deleteComment(article.id, comment.id); comments = fetchComments(article.id) }
                            })
                        }
                        if (comments.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No comments yet. Be the first! 💬", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center) } }
                        }
                    }
                }
            }
        }
        ComicBurst(stageClearBurst, Modifier.align(Alignment.Center), burstSize = 170.dp)
    }
}

// ─── ArticleLikeSaveBar ───────────────────────────────────────────────────────

@Composable
fun ArticleLikeSaveBar(engagement: ArticleEngagement, onLike: () -> Unit, onSave: () -> Unit, onShare: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(14.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        // Like
        var likePressed by remember { mutableStateOf(false) }
        val likeScale by animateFloatAsState(targetValue = if (likePressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "likeScale")
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(likeScale).clickable { likePressed = true; onLike() }) {
            Text(if (engagement.isLiked) "❤️" else "🤍", fontSize = 24.sp)
            Text("${engagement.likeCount}", fontFamily = BangersFontFamily, color = if (engagement.isLiked) CAcRed else ScrapbookTextMuted, fontSize = 12.sp)
            Text("LIKE", fontFamily = BangersFontFamily, color = if (engagement.isLiked) CAcRed else ScrapbookTextMuted, fontSize = 9.sp, letterSpacing = 0.5.sp)
        }
        LaunchedEffect(likePressed) { if (likePressed) { delay(200); likePressed = false } }

        Box(modifier = Modifier.width(1.dp).height(36.dp).background(ScrapbookBorder.copy(alpha = 0.3f)))

        // Save
        var savePressed by remember { mutableStateOf(false) }
        val saveScale by animateFloatAsState(targetValue = if (savePressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "saveScale")
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(saveScale).clickable { savePressed = true; onSave() }) {
            Icon(if (engagement.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, tint = if (engagement.isSaved) CGreen else ScrapbookTextMuted, modifier = Modifier.size(24.dp))
            Text("${engagement.saveCount}", fontFamily = BangersFontFamily, color = if (engagement.isSaved) CGreen else ScrapbookTextMuted, fontSize = 12.sp)
            Text("SAVE", fontFamily = BangersFontFamily, color = if (engagement.isSaved) CGreen else ScrapbookTextMuted, fontSize = 9.sp, letterSpacing = 0.5.sp)
        }
        LaunchedEffect(savePressed) { if (savePressed) { delay(200); savePressed = false } }

        Box(modifier = Modifier.width(1.dp).height(36.dp).background(ScrapbookBorder.copy(alpha = 0.3f)))

        // Share
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onShare() }) {
            Icon(Icons.Filled.Share, null, tint = ScrapbookTextMuted, modifier = Modifier.size(24.dp))
            Text("SHARE", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, letterSpacing = 0.5.sp)
        }
    }
}

// ─── ArticleReactionBar ───────────────────────────────────────────────────────

@Composable
fun ArticleReactionBar(engagement: ArticleEngagement, onReact: (String) -> Unit) {
    val reactionEmojis = listOf("🔥", "🤯", "👾", "💯", "😍", "🏆")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text("REACTIONS", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reactionEmojis) { emoji ->
                val count = engagement.reactions[emoji] ?: 0
                val isReacted = engagement.myReaction == emoji
                var pressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(targetValue = if (pressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "reactScale_$emoji")
                Box(
                    modifier = Modifier.scale(scale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isReacted) CGreen else Color.White.copy(alpha = 0.46f))
                        .border(width = if (isReacted) 1.5.dp else 1.dp, color = if (isReacted) CGreen.copy(alpha = 0.7f) else ScrapbookBorder, shape = RoundedCornerShape(20.dp))
                        .clickable { pressed = true; onReact(emoji) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(emoji, fontSize = 16.sp)
                        if (count > 0) Text("$count", fontFamily = BangersFontFamily, color = if (isReacted) CGreen else ScrapbookTextMuted, fontSize = 12.sp)
                    }
                }
                LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
            }
        }
    }
}

// ─── ArticleStatsBar ──────────────────────────────────────────────────────────

@Composable
fun ArticleStatsBar(engagement: ArticleEngagement, neonAlpha: Float) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(ComicGlassBg, ComicGlassBg))).border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Filled.Visibility, null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(13.dp)); Text("${formatCount(engagement.viewCount)}", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Text("❤️", fontSize = 11.sp); Text("${engagement.likeCount}", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Filled.Bookmark, null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(13.dp)); Text("${engagement.saveCount}", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp) }
    }
}

// ─── ArticleCommentCard ───────────────────────────────────────────────────────

@Composable
fun ArticleCommentCard(comment: ArticleComment, currentUserId: String, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder, RoundedCornerShape(12.dp)).padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(CGreen.copy(alpha = 0.2f)).border(1.dp, CGreen.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                            if (comment.authorPicUrl.isNotBlank()) AsyncImage(model = comment.authorPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(comment.authorPicUrl).fillMaxSize())
                            else Text(comment.authorUsername.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                        }
                        Column {
                            Text(comment.authorUsername, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                            Text(timeAgoFromMillis(comment.timestamp), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                        }
                    }
                    if (comment.authorUid == currentUserId) {
                        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Delete, null, tint = CAcRed.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Text(comment.text, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(modifier = Modifier.comicPop(), onDismissRequest = { showDeleteConfirm = false }, containerColor = ComicGlassBg, titleContentColor = ScrapbookDark, textContentColor = ScrapbookDark, title = { Text("DELETE COMMENT", fontFamily = BangersFontFamily, fontSize = 18.sp) }, text = { Text("Are you sure you want to delete this comment?", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
            confirmButton = { Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CAcRed.copy(alpha = 0.2f)).border(1.dp, CAcRed, RoundedCornerShape(8.dp)).clickable { showDeleteConfirm = false; onDelete() }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("DELETE", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 14.sp) } },
            dismissButton = { Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookDark, RoundedCornerShape(8.dp)).clickable { showDeleteConfirm = false }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp) } }
        )
    }
}

// ─── RelatedArticlesRow ───────────────────────────────────────────────────────

@Composable
fun RelatedArticlesRow(currentArticleId: String, category: String, onOpenArticle: (ArticleItem) -> Unit) {
    var related by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    LaunchedEffect(currentArticleId) {
        try {
            val db = FirebaseFirestore.getInstance()
            val docs = db.collection("articles").whereEqualTo("category", category).limit(6).get().await()
            related = docs.documents.mapNotNull { doc ->
                if (doc.id == currentArticleId) return@mapNotNull null
                val data = doc.data ?: return@mapNotNull null
                ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String, category = data["category"] as? String ?: "GAMING")
            }.filter { it.title.isNotBlank() }.take(4)
        } catch (e: Exception) { }
    }
    if (related.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("MORE LIKE THIS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(related.distinctBy { it.id }.filter { it.id.isNotBlank() }, key = { "r_" + it.id }) { article ->
                val accentColor = categoryColor(article.category)
                Box(modifier = Modifier.width(160.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder, RoundedCornerShape(12.dp)).clickable { onOpenArticle(article) }) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                            if (article.imageUrl != null) AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(article.imageUrl).fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                            else Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Brush.verticalGradient(colors = listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.08f)))), contentAlignment = Alignment.Center) { Text(categoryEmoji(article.category), fontSize = 32.sp) }
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(article.author, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

// ─── AuthorAvatarPill ─────────────────────────────────────────────────────────

@Composable
fun AuthorAvatarPill(article: ArticleItem, onTap: () -> Unit) {
    var authorData by remember { mutableStateOf<UserProfileData?>(null) }
    LaunchedEffect(article.authorUid) {
        if (!article.authorUid.isNullOrBlank()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(article.authorUid).get().await()
                val data = doc.data ?: return@LaunchedEffect
                authorData = UserProfileData(uid = doc.id, username = data["username"] as? String ?: article.author, userHandle = data["userHandle"] as? String ?: "", profilePictureUrl = data["profilePictureUrl"] as? String ?: "", habboUsername = data["habboUsername"] as? String ?: "", habboRegion = data["habboRegion"] as? String ?: "habbo.com")
            } catch (e: Exception) { }
        }
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(20.dp)).clickable { onTap() }.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
            val picUrl = authorData?.profilePictureUrl
            when {
                !picUrl.isNullOrBlank() -> AsyncImage(model = picUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(picUrl).fillMaxSize())
                authorData?.habboUsername?.isNotBlank() == true -> AsyncImage(model = "https://www.${authorData!!.habboRegion}/habbo-imaging/avatarimage?user=${authorData!!.habboUsername}&action=std&direction=2&head_direction=2&size=s&gesture=sml", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                else -> Text(article.author.take(1).uppercase(), fontFamily = BangersFontFamily, color = CGreen, fontSize = 11.sp)
            }
        }
        Text(article.author, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!article.authorUid.isNullOrBlank()) Icon(Icons.Filled.ChevronRight, null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp))
    }
}

// ─── AuthorHabboGreeter ───────────────────────────────────────────────────────

@Composable
fun AuthorHabboGreeter(authorUid: String, authorName: String, onTapProfile: () -> Unit) {
    var authorData by remember(authorUid) { mutableStateOf<UserProfileData?>(null) }
    LaunchedEffect(authorUid) {
        try {
            val doc = FirebaseFirestore.getInstance().collection("users").document(authorUid).get().await()
            val data = doc.data ?: return@LaunchedEffect
            authorData = UserProfileData(uid = doc.id, username = data["username"] as? String ?: authorName, userHandle = data["userHandle"] as? String ?: "", profilePictureUrl = data["profilePictureUrl"] as? String ?: "", habboUsername = data["habboUsername"] as? String ?: "", habboRegion = data["habboRegion"] as? String ?: "habbo.com", bio = data["bio"] as? String ?: "", followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0)
        } catch (e: Exception) { }
    }
    val author = authorData ?: return
    val neonAlpha by rememberGlowRange(0.4f, 1f)

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp)).clickable { onTapProfile() }) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            // Gloss
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent))))
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Habbo avatar or profile pic
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(CGreen, CGreen, CGreen))))
                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(ComicGlassBg).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                        when {
                            author.habboUsername.isNotBlank() -> AsyncImage(model = "https://www.${author.habboRegion}/habbo-imaging/avatarimage?user=${author.habboUsername}&action=std&direction=2&head_direction=2&size=m&gesture=sml", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            author.profilePictureUrl.isNotBlank() -> AsyncImage(model = author.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(author.profilePictureUrl).fillMaxSize())
                            else -> Text(author.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookDark).padding(horizontal = 6.dp, vertical = 2.dp)) { Text("WRITTEN BY", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 1.sp) }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(author.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp)
                    if (author.userHandle.isNotBlank()) Text(author.userHandle, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
                    if (author.bio.isNotBlank()) { Spacer(modifier = Modifier.height(4.dp)); Text(author.bio, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp) }
                    if (author.followersCount > 0) { Spacer(modifier = Modifier.height(4.dp)); Text("${formatCount(author.followersCount)} followers", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 10.sp) }
                }
                Icon(Icons.Filled.ChevronRight, null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// Article Editor Screen

@Composable
fun ArticleEditorScreen(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    userProfile: UserProfileData?,
    onDismiss: () -> Unit,
    onPublished: (ArticleItem) -> Unit
) {
    // TODO: implement article editor
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier.fillMaxSize().background(ComicGlassBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Article Editor Coming Soon", fontFamily = BangersFontFamily, color = ScrapbookDark)
        }
    }
}


// ─── Reading progress as a retro HP bar ───────────────────────────────────────

@Composable
fun ArticleReadingHpBar(progress: Float) {
    val segments = 10
    val filled = (progress * segments).toInt().coerceIn(0, segments)
    val pct = (progress * 100).toInt().coerceIn(0, 100)
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f))
            .border(BorderStroke(2.dp, ScrapbookDark))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("HP", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp)
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(segments) { i ->
                val on = i < filled
                Box(
                    modifier = Modifier.weight(1f).height(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (on) CGreen else ScrapbookDark.copy(alpha = 0.08f))
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(2.dp))
                )
            }
        }
        RollingCounterText("$pct%", androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, color = ScrapbookDark))
    }
}


// ─── Reading progress store (per article, on device) ──────────────────────────

object ArticleProgressStore {
    private const val PREFS = "article_progress"
    val progress = mutableStateMapOf<String, Int>()
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.forEach { (k, v) ->
            (v as? Int)?.let { progress[k] = it }
        }
    }

    /** Keeps the furthest point reached; writes only every 5% to avoid spamming prefs while scrolling. */
    fun save(context: Context, articleId: String, pct: Int) {
        load(context)
        val old = progress[articleId] ?: 0
        if (pct <= old) return
        if (pct < old + 5 && pct < 95) return
        progress[articleId] = pct.coerceAtMost(100)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(articleId, pct.coerceAtMost(100)).apply()
    }
}

@Composable
fun ContinueReadingRow(articles: List<ArticleItem>, onOpen: (ArticleItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RetroSectionHeader(title = "CONTINUE READING", emoji = "📖")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
            items(articles, key = { "cont_${it.id}" }) { article ->
                val pct = ArticleProgressStore.progress[article.id] ?: 0
                Box(modifier = Modifier.width(210.dp)) {
                    Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                        .clip(RoundedCornerShape(12.dp)).background(CGreen))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                            .clickable { onOpen(article) }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(article.title, fontFamily = BangersFontFamily, fontSize = 15.sp, color = ScrapbookDark,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("HP", fontFamily = BangersFontFamily, fontSize = 11.sp, color = CGreenDeep)
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(10) { i ->
                                    Box(Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(1.dp))
                                        .background(if (i < pct / 10) CGreen else ScrapbookDark.copy(alpha = 0.08f))
                                        .border(1.dp, ScrapbookDark, RoundedCornerShape(1.dp)))
                                }
                            }
                            Text("$pct%", fontFamily = BangersFontFamily, fontSize = 11.sp, color = ScrapbookDark)
                        }
                        Text("▶ RESUME", fontFamily = BangersFontFamily, fontSize = 12.sp, color = CGreenDeep, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleReadStamp(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = 8f }
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(2.dp, CGreenDeep, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text("✔ READ", fontFamily = BangersFontFamily, fontSize = 12.sp, color = CGreenDeep, letterSpacing = 1.sp)
    }
}

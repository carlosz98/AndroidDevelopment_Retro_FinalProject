package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// ─── Constants ────────────────────────────────────────────────────────────────

private val YOUTUBE_API_KEY get() = BuildConfig.YOUTUBE_API_KEY
private const val YOUTUBE_SEARCH_BASE = "https://www.googleapis.com/youtube/v3/search"

// ─── Data Model ───────────────────────────────────────────────────────────────

data class RetroShort(
    val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val publishedAt: String = ""
)

// ─── Session Cache ─────────────────────────────────────────────────────────────

object RetroBytesCache {
    var shorts: List<RetroShort> = emptyList()
    var isFetched: Boolean = false
}

// ─── YouTube Queries ──────────────────────────────────────────────────────────

private val retroQueries = listOf(
    "retro gaming shorts",
    "videogames shorts",
    "retro gaming classic videogames",
    "NES SNES gameplay shorts",
    "retro gaming funny moments shorts"
)

// ─── English Filter ───────────────────────────────────────────────────────────

private fun isEnglishTitle(title: String): Boolean {
    if (title.isBlank()) return false
    val asciiCount = title.count { it.code in 32..126 }
    return asciiCount.toFloat() / title.length.toFloat() > 0.85f
}

// ─── Fetch Shorts ─────────────────────────────────────────────────────────────

suspend fun fetchRetroBytesShorts(): List<RetroShort> = withContext(Dispatchers.IO) {
    if (RetroBytesCache.isFetched) return@withContext RetroBytesCache.shorts
    try {
        val client = OkHttpClient()
        val allResults = mutableListOf<RetroShort>()
        coroutineScope {
            retroQueries.map { query ->
                async {
                    try {
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val url = "$YOUTUBE_SEARCH_BASE" +
                                "?part=snippet" +
                                "&q=$encodedQuery" +
                                "&type=video" +
                                "&videoDuration=short" +
                                "&maxResults=10" +
                                "&relevanceLanguage=en" +
                                "&key=$YOUTUBE_API_KEY"
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string()
                            ?: return@async emptyList<RetroShort>()
                        val json = JSONObject(body)
                        val items = json.optJSONArray("items")
                            ?: return@async emptyList<RetroShort>()
                        val results = mutableListOf<RetroShort>()
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            val videoId = item
                                .optJSONObject("id")
                                ?.optString("videoId") ?: continue
                            val snippet = item.optJSONObject("snippet") ?: continue
                            val title = snippet.optString("title", "")
                            val channel = snippet.optString("channelTitle", "")
                            val publishedAt = snippet.optString("publishedAt", "")
                            val thumbnails = snippet.optJSONObject("thumbnails")
                            val thumbnail = thumbnails
                                ?.optJSONObject("high")
                                ?.optString("url")
                                ?: thumbnails
                                    ?.optJSONObject("medium")
                                    ?.optString("url")
                                ?: "https://img.youtube.com/vi/$videoId/0.jpg"
                            if (title.isBlank() || videoId.isBlank()) continue
                            if (!isEnglishTitle(title)) continue
                            results.add(
                                RetroShort(
                                    videoId = videoId,
                                    title = title,
                                    channelName = channel,
                                    thumbnailUrl = thumbnail,
                                    publishedAt = publishedAt
                                )
                            )
                        }
                        results
                    } catch (e: Exception) {
                        emptyList<RetroShort>()
                    }
                }
            }.awaitAll().forEach { allResults.addAll(it) }
        }
        val deduplicated = allResults.distinctBy { it.videoId }.shuffled()
        RetroBytesCache.shorts = deduplicated
        RetroBytesCache.isFetched = true
        deduplicated
    } catch (e: Exception) {
        emptyList()
    }
}

// ─── Firestore Like Functions ─────────────────────────────────────────────────

suspend fun toggleRetroBytesLike(videoId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext false
        val docRef = FirebaseFirestore.getInstance()
            .collection("retroBytes")
            .document(videoId)
            .collection("likes")
            .document(uid)
        val doc = docRef.get().await()
        if (doc.exists()) {
            docRef.delete().await()
            false
        } else {
            docRef.set(
                mapOf("uid" to uid, "timestamp" to System.currentTimeMillis())
            ).await()
            true
        }
    } catch (e: Exception) { false }
}

suspend fun fetchLikeCount(videoId: String): Int = withContext(Dispatchers.IO) {
    try {
        FirebaseFirestore.getInstance()
            .collection("retroBytes")
            .document(videoId)
            .collection("likes")
            .get()
            .await()
            .size()
    } catch (e: Exception) { 0 }
}

suspend fun fetchUserLiked(videoId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext false
        FirebaseFirestore.getInstance()
            .collection("retroBytes")
            .document(videoId)
            .collection("likes")
            .document(uid)
            .get()
            .await()
            .exists()
    } catch (e: Exception) { false }
}

// ─── RetroBytesScreen ─────────────────────────────────────────────────────────

@Composable
fun RetroBytesScreen(modifier: Modifier = Modifier) {
    var shorts by remember { mutableStateOf<List<RetroShort>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        shorts = fetchRetroBytesShorts()
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> RetroBytesLoadingState()
            shorts.isEmpty() -> RetroBytesEmptyState()
            else -> RetroBytesFeed(shorts = shorts)
        }

        // Watermark — always on top
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 52.dp, start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("📱", fontSize = 14.sp)
            Text(
                text = "RETROBYTES",
                fontFamily = BangersFontFamily,
                color = CGreen,
                fontSize = 16.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

// ─── Feed ─────────────────────────────────────────────────────────────────────

@Composable
fun RetroBytesFeed(shorts: List<RetroShort>) {
    val pagerState = rememberPagerState(pageCount = { shorts.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        RetroBytesVideoCard(
            short = shorts[page],
            isActive = pagerState.currentPage == page,
            pageIndex = page,
            totalPages = shorts.size
        )
    }
}

// ─── Loading State ────────────────────────────────────────────────────────────

@Composable
fun RetroBytesLoadingState() {
    val shimmerT = rememberInfiniteTransition(label = "bytesShimmer")
    val shimmerAlpha by shimmerT.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = EaseInOut), RepeatMode.Reverse
        ),
        label = "bytesShimmerAlpha"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "LOADING RETROBYTES",
            fontFamily = BangersFontFamily,
            color = CGreen.copy(alpha = shimmerAlpha),
            fontSize = 20.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Fetching retro gaming shorts...",
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        CircularProgressIndicator(
            color = CGreen,
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp
        )
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun RetroBytesEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📡", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NO SHORTS FOUND",
            fontFamily = BangersFontFamily,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 20.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check back later · 📱",
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 13.sp
        )
    }
}
// ─── Video Card ───────────────────────────────────────────────────────────────

@Composable
fun RetroBytesVideoCard(
    short: RetroShort,
    isActive: Boolean,
    pageIndex: Int,
    totalPages: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(0) }
    var isLiking by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "videoEnter_$pageIndex"
    )

    LaunchedEffect(isActive) {
        if (isActive) {
            visible = true
            isLiked = fetchUserLiked(short.videoId)
            likeCount = fetchLikeCount(short.videoId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = enterAlpha }
    ) {
        // ─── Player or thumbnail ──────────────────────────────────────────────
        if (isActive) {
            YoutubePlayerCard(
                youtubeVideoId = short.videoId,
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner
            )
        } else {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(short.thumbnailUrl).fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, fontSize = 24.sp)
                }
            }
        }

        // ─── Bottom gradient ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // ─── Right side buttons ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Like
            var likePop by remember { mutableStateOf(false) }
            val likeScale by animateFloatAsState(
                targetValue = if (likePop) 1.4f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "likeScale_$pageIndex"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(likeScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            if (!isLiking) {
                                likePop = true
                                isLiking = true
                                scope.launch {
                                    val nowLiked = toggleRetroBytesLike(short.videoId)
                                    isLiked = nowLiked
                                    likeCount = fetchLikeCount(short.videoId)
                                    isLiking = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isLiked) "❤️" else "🤍", fontSize = 24.sp)
                }
                Text(
                    text = if (likeCount > 0) "$likeCount" else "Like",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            LaunchedEffect(likePop) { if (likePop) { delay(200); likePop = false } }

            // Share
            var sharePressed by remember { mutableStateOf(false) }
            val shareScale by animateFloatAsState(
                targetValue = if (sharePressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "shareScale_$pageIndex"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(shareScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable {
                            sharePressed = true
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out this retro gaming short! 🎮\nhttps://youtube.com/shorts/${short.videoId}"
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share via")
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Share",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            LaunchedEffect(sharePressed) { if (sharePressed) { delay(150); sharePressed = false } }

            // Open in YouTube
            var ytPressed by remember { mutableStateOf(false) }
            val ytScale by animateFloatAsState(
                targetValue = if (ytPressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ytScale_$pageIndex"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .scale(ytScale)
                        .clip(CircleShape)
                        .background(CAcRed.copy(alpha = 0.25f))
                        .border(1.dp, CAcRed.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            ytPressed = true
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://youtube.com/shorts/${short.videoId}")
                                    )
                                )
                            } catch (e: Exception) { }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = CAcRed, fontSize = 20.sp)
                }
                Text(
                    text = "YouTube",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            LaunchedEffect(ytPressed) { if (ytPressed) { delay(150); ytPressed = false } }
        }

        // ─── Bottom left info ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 40.dp, end = 8.dp)
        ) {
            // Channel avatar + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CGreen.copy(alpha = 0.2f))
                        .border(1.5.dp, CGreen.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = short.channelName.take(1).uppercase(),
                        fontFamily = BangersFontFamily,
                        color = CGreen,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "@${short.channelName}",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = short.title,
                fontFamily = BangersFontFamily,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(CGreen.copy(alpha = 0.15f))
                    .border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🎮 RETRO GAMING",
                    fontFamily = BangersFontFamily,
                    color = CGreen,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // ─── Page indicator ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val start = (pageIndex - 2).coerceAtLeast(0)
            val end = (pageIndex + 2).coerceAtMost(totalPages - 1)
            (start..end).forEach { i ->
                if (i == pageIndex) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(CGreen)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

// ─── Home Preview Section ─────────────────────────────────────────────────────

@Composable
fun HomeRetroBytesPreview(onOpenFeed: () -> Unit) {
    var shorts by remember { mutableStateOf<List<RetroShort>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        shorts = fetchRetroBytesShorts()
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        // Header — glassy retrobytes banner
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(ComicGlassBg, ComicGlassBg, ComicGlassBg)))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Specular highlight
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent))
            ))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📱", fontSize = 26.sp)
                    Column {
                        Text("RETROBYTES", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 26.sp, letterSpacing = 1.5.sp)
                        Text("Retro gaming in 60 seconds", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box {
                    GlowPulse(
                        modifier = Modifier.matchParentSize(),
                        glowColor = CAcRed,
                        cornerRadius = 12.dp,
                        maxAlpha = 0.4f
                    )
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(CAcRed, CAcRedD)))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("● LIVE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }

        // Thumbnail cards
        if (isLoading) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    ShimmerBox(
                        modifier = Modifier.width(140.dp).height(240.dp),
                        cornerRadius = 12.dp
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(shorts.take(4)) { index, short ->
                    SpringEntrance(delayMs = index * 60) {
                        RetroBytesPreviewCard(short = short, onClick = onOpenFeed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Open feed button
        RetroGlassButton(
            text = "📱 OPEN RETROBYTES →",
            onClick = onOpenFeed,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            cornerRadius = 14.dp
        )
    }
}

// ─── Preview Card ─────────────────────────────────────────────────────────────

@Composable
fun RetroBytesPreviewCard(short: RetroShort, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "rbPress_${short.videoId}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "rbShadow_${short.videoId}")

    Box(modifier = Modifier.width(140.dp).height(240.dp)) {
        // Glow halo
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(12.dp)).background(CGreen))
        // Card content
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                .background(Color.Black)
                .clickable { pressed = true; onClick() }
        ) {
            // Thumbnail
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.halftoneReveal(short.thumbnailUrl).fillMaxSize()
            )

            // Scanline overlay on thumbnail
            ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.06f)

            // Comic shimmer layer
            ComicShimmer(modifier = Modifier.matchParentSize(), cornerRadius = 12.dp)

            // Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Play button
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, fontSize = 18.sp)
            }

            // SHORTS badge → HoloBadge
            HoloBadge(
                label = "📱 SHORTS",
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                cornerRadius = 6.dp,
                fontSize = 8.sp
            )

            // Title + channel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = short.title,
                    fontFamily = BangersFontFamily,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = short.channelName,
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(200); pressed = false } }
}
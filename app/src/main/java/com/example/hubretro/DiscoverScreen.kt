package com.example.hubretro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

// ─── Data Models ──────────────────────────────────────────────────────────────

data class NewsItem(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val url: String,
    val source: String,
    val sourceColor: Color,
    val publishedAt: String
)

data class GameDeal(
    val title: String,
    val salePrice: String,
    val normalPrice: String,
    val savings: Int,
    val thumb: String,
    val storeId: String
)

data class RetroGameOfDay(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val summary: String?,
    val rating: Double?,
    val releaseYear: Int?
)

data class DiscoverResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: DiscoverCategory
)

enum class DiscoverCategory(val label: String, val color: Color) {
    USER("USER", ScrapbookDark),
    MAGAZINE("MAGAZINE", ScrapbookDark),
    ALBUM("ALBUM", ScrapbookDark),
    ARTICLE("ARTICLE", ScrapbookDark)
}

val discoverFilters = listOf("ALL", "PEOPLE", "ARTICLES", "MAGAZINES", "ALBUMS", "LIVE")

// ─── RSS Sources ──────────────────────────────────────────────────────────────

val rssSources = listOf(
    Triple("IGN",       Color(0xFFFF0000), "https://feeds.feedburner.com/ign/games-all"),
    Triple("Kotaku",    Color(0xFF00AE7E), "https://kotaku.com/rss"),
    Triple("Eurogamer", Color(0xFF0066CC), "https://www.eurogamer.net/?format=rss")
)

val newsPageSize = 5

// ─── Curated retro game IDs ───────────────────────────────────────────────────

val retroGameIds = listOf(
    1020, 1942, 768, 1877, 472, 119, 324, 2131,
    282, 510, 481, 1030, 1941, 542, 1746, 311,
    1074, 236, 533, 1069
)

// ─── Network Helpers ──────────────────────────────────────────────────────────

suspend fun fetchNewsFromRss(
    sourceName: String,
    sourceColor: Color,
    url: String,
    limit: Int = 8
): List<NewsItem> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()

        val items = mutableListOf<NewsItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(body))

        var currentTitle = ""
        var currentDesc = ""
        var currentLink = ""
        var currentPubDate = ""
        var currentImage: String? = null
        var insideItem = false
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT && items.size < limit) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name ?: ""
                    if (currentTag == "item") {
                        insideItem = true
                        currentTitle = ""; currentDesc = ""; currentLink = ""
                        currentPubDate = ""; currentImage = null
                    }
                    if (insideItem) {
                        when (currentTag) {
                            "content", "thumbnail" -> {
                                val imgUrl = parser.getAttributeValue(null, "url")
                                if (!imgUrl.isNullOrBlank() && currentImage == null) currentImage = imgUrl
                            }
                            "enclosure" -> {
                                val encType = parser.getAttributeValue(null, "type") ?: ""
                                if (encType.startsWith("image")) {
                                    val imgUrl = parser.getAttributeValue(null, "url")
                                    if (!imgUrl.isNullOrBlank()) currentImage = imgUrl
                                }
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideItem) {
                        when (currentTag) {
                            "title" -> currentTitle += parser.text ?: ""
                            "description" -> {
                                val raw = parser.text ?: ""
                                if (currentImage == null) {
                                    Regex("""<img[^>]+src=["']([^"']+)["']""").find(raw)
                                        ?.groupValues?.getOrNull(1)?.let { currentImage = it }
                                }
                                currentDesc += raw.replace(Regex("<[^>]*>"), "").trim()
                            }
                            "link" -> currentLink += parser.text ?: ""
                            "pubDate" -> currentPubDate += parser.text ?: ""
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && insideItem) {
                        if (currentTitle.isNotBlank() && currentLink.isNotBlank()) {
                            items.add(NewsItem(
                                title = currentTitle.trim(),
                                description = currentDesc.trim().take(200),
                                imageUrl = currentImage,
                                url = currentLink.trim(),
                                source = sourceName,
                                sourceColor = sourceColor,
                                publishedAt = formatRssDate(currentPubDate.trim())
                            ))
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        items
    } catch (e: Exception) { emptyList() }
}

suspend fun fetchArticleContent(url: String): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Android)")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext ""
        var text = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<nav[^>]*>[\\s\\S]*?</nav>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<header[^>]*>[\\s\\S]*?</header>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<footer[^>]*>[\\s\\S]*?</footer>", RegexOption.IGNORE_CASE), "")
        val paragraphs = Regex("<p[^>]*>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE)
            .findAll(text)
            .map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }
            .filter { it.length > 50 }
            .take(20)
            .joinToString("\n\n")
        paragraphs.ifBlank {
            text.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim().take(3000)
        }
    } catch (e: Exception) { "" }
}

suspend fun fetchGameDeals(): List<GameDeal> = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://www.cheapshark.com/api/1.0/deals?sortBy=Deal&onSale=1&upperPrice=20&pageSize=8")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        val arr = JSONArray(body)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            GameDeal(
                title = obj.optString("title"),
                salePrice = obj.optString("salePrice"),
                normalPrice = obj.optString("normalPrice"),
                savings = obj.optDouble("savings", 0.0).toInt(),
                thumb = obj.optString("thumb"),
                storeId = obj.optString("storeID")
            )
        }.filter { it.title.isNotBlank() }
    } catch (e: Exception) { emptyList() }
}

suspend fun fetchRetroGameOfDay(): RetroGameOfDay? {
    return try { IGDBRepository.fetchGameById(retroGameIds.random()) }
    catch (e: Exception) { null }
}

fun formatRssDate(raw: String): String {
    return try {
        val parts = raw.split(" ")
        if (parts.size >= 4) "${parts[1]} ${parts[2]} ${parts[3]}" else raw
    } catch (e: Exception) { raw }
}

fun storeName(storeId: String) = when (storeId) {
    "1" -> "Steam"; "2" -> "GamersGate"; "3" -> "GreenManGaming"
    "7" -> "GOG"; "8" -> "Origin"; "11" -> "Humble"
    "13" -> "Uplay"; "15" -> "Fanatical"; "25" -> "Epic"
    else -> "Store"
}

// ─── News Section ─────────────────────────────────────────────────────────────

@Composable
fun NewsSection(
    allNews: List<NewsItem>,
    isLoadingNews: Boolean,
    onNewsClick: (NewsItem) -> Unit
) {
    var selectedTab by remember { mutableStateOf("LATEST") }
    var visibleCount by remember { mutableStateOf(newsPageSize) }

    val filteredNews = remember(allNews, selectedTab) {
        when (selectedTab) {
            "IGN" -> allNews.filter { it.source == "IGN" }
            "KOTAKU" -> allNews.filter { it.source == "Kotaku" }
            "EUROGAMER" -> allNews.filter { it.source == "Eurogamer" }
            "TRENDING" -> allNews.sortedByDescending { it.title.length }
            else -> allNews
        }
    }

    val heroNews = filteredNews.firstOrNull()
    val listNews = filteredNews.drop(1).take(visibleCount)
    val hasMore = filteredNews.drop(1).size > visibleCount

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        DiscoverSectionRow(title = "BREAKING NEWS", emoji = "📡", onSeeAll = null)

        // ✅ Tab filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(listOf("LATEST", "TRENDING", "IGN", "KOTAKU", "EUROGAMER")) { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                        .border(
                            2.dp,
                            when (tab) {
                                "IGN" -> Color(0xFFFF0000)
                                "KOTAKU" -> Color(0xFF00AE7E)
                                "EUROGAMER" -> Color(0xFF0066CC)
                                else -> ScrapbookBorder
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedTab = tab; visibleCount = newsPageSize }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = tab,
                        fontFamily = BangersFontFamily,
                        color = when {
                            isSelected -> ScrapbookYellow
                            tab == "IGN" -> Color(0xFFFF0000)
                            tab == "KOTAKU" -> Color(0xFF00AE7E)
                            tab == "EUROGAMER" -> Color(0xFF0066CC)
                            else -> ScrapbookDark
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }

        when {
            isLoadingNews -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Loading latest news...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                    }
                }
            }

            filteredNews.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No news from $selectedTab right now", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                    }
                }
            }

            else -> {
                // ✅ Hero card
                heroNews?.let { hero ->
                    NewsHeroCard(news = hero, isBookmarked = false, onClick = { onNewsClick(hero) })
                }

                // ✅ Vertical list cards
                listNews.forEach { news ->
                    NewsListCard(news = news, onClick = { onNewsClick(news) })
                }

                // ✅ Infinite scroll trigger
                if (hasMore) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LaunchedEffect(visibleCount) {
                            delay(300)
                            visibleCount += newsPageSize
                        }
                        CircularProgressIndicator(
                            color = ScrapbookYellowDark,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (filteredNews.size > 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookPaper)
                            .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📡 You're all caught up!",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ScrapbookTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── News List Card ───────────────────────────────────────────────────────────

@Composable
fun NewsListCard(news: NewsItem, onClick: () -> Unit) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScrapbookPaper),
                    contentAlignment = Alignment.Center
                ) {
                    if (!news.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = news.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("📰", fontSize = 28.sp)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(news.sourceColor)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(news.sourceColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                        }
                        if (news.publishedAt.isNotBlank()) {
                            Text(news.publishedAt, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = news.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        letterSpacing = 0.2.sp
                    )
                    if (news.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = news.description,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = ScrapbookDark.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── News Hero Card ───────────────────────────────────────────────────────────

@Composable
fun NewsHeroCard(
    news: NewsItem,
    isBookmarked: Boolean = false,
    onClick: () -> Unit
) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            backgroundColor = ScrapbookDark,
            cornerRadius = 16.dp,
            shadowOffset = 5.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                if (!news.imageUrl.isNullOrBlank()) {
                    AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.5f)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF1A1A2E), ScrapbookDark))))
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f)))))
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFF3B30)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("🔴 BREAKING", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(news.sourceColor).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text(news.source.uppercase(), fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Column {
                        Text(news.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 22.sp, lineHeight = 26.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, letterSpacing = 0.3.sp)
                        if (news.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(news.description, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (news.publishedAt.isNotBlank()) {
                                Text("🕐 ${news.publishedAt}", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("READ INSIDE APP →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── News Compact Card ────────────────────────────────────────────────────────

@Composable
fun NewsCompactCard(news: NewsItem, onClick: () -> Unit) {
    Box(modifier = Modifier.width(200.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }, backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper),
                    contentAlignment = Alignment.Center
                ) {
                    if (!news.imageUrl.isNullOrBlank()) {
                        AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else { Text("📰", fontSize = 32.sp) }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(news.sourceColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(news.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    if (news.publishedAt.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(news.publishedAt, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ─── News Reader Screen ───────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NewsReaderScreen(
    news: NewsItem,
    currentUserId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isBookmarked by remember { mutableStateOf(false) }
    var articleContent by remember { mutableStateOf("") }
    var isLoadingContent by remember { mutableStateOf(true) }
    var useWebView by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId, news.url) {
        if (currentUserId.isNotBlank()) {
            try {
                val docId = news.url.hashCode().toString()
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(currentUserId)
                    .collection("saved_news").document(docId)
                    .get().await()
                isBookmarked = doc.exists()
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(news.url) {
        isLoadingContent = true
        val content = fetchArticleContent(news.url)
        if (content.length > 200) {
            articleContent = content
            useWebView = false
        } else {
            useWebView = true
        }
        isLoadingContent = false
    }

    fun toggleBookmark() {
        if (currentUserId.isBlank()) return
        val docId = news.url.hashCode().toString()
        val ref = FirebaseFirestore.getInstance()
            .collection("users").document(currentUserId)
            .collection("saved_news").document(docId)
        if (isBookmarked) {
            ref.delete(); isBookmarked = false
        } else {
            ref.set(mapOf(
                "title" to news.title, "url" to news.url,
                "imageUrl" to (news.imageUrl ?: ""), "source" to news.source,
                "publishedAt" to news.publishedAt, "savedAt" to System.currentTimeMillis()
            ))
            isBookmarked = true
        }
    }

    fun shareArticle() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, news.title)
            putExtra(Intent.EXTRA_TEXT, "${news.title}\n\n${news.url}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookDark)
                    .padding(top = 40.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(news.sourceColor).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { toggleBookmark() }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) ScrapbookYellow else Color.White
                        )
                    }
                    IconButton(onClick = { shareArticle() }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                        try { context.startActivity(intent) } catch (e: Exception) { }
                    }) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = "Open in browser", tint = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            when {
                isLoadingContent -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading article...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp)
                        }
                    }
                }
                useWebView -> {
                    if (isWebViewLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ScrapbookYellowDark, trackColor = ScrapbookPaper)
                    }
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                settings.apply {
                                    javaScriptEnabled = true; domStorageEnabled = true
                                    loadWithOverviewMode = true; useWideViewPort = true
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) { isWebViewLoading = false }
                                }
                                webChromeClient = WebChromeClient()
                                loadUrl(news.url)
                                webViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
                        item {
                            if (!news.imageUrl.isNullOrBlank()) {
                                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                                    AsyncImage(model = news.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, ScrapbookCream.copy(alpha = 0.8f)))))
                                }
                            }
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(news.sourceColor).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(news.source, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                                    }
                                    if (news.publishedAt.isNotBlank()) {
                                        Text("🕐 ${news.publishedAt}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(news.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = 0.3.sp)
                                if (news.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(ScrapbookYellow.copy(alpha = 0.15f))
                                            .border(2.dp, ScrapbookYellowDark, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = news.description,
                                            fontFamily = NunitoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = ScrapbookDark,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(articleContent, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 16.sp, lineHeight = 26.sp)
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                                            try { context.startActivity(intent) } catch (e: Exception) { }
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
                                        Text("OPEN FULL ARTICLE →", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Game Deal Card ───────────────────────────────────────────────────────────

@Composable
fun GameDealCard(deal: GameDeal) {
    val savingsColor = when {
        deal.savings >= 70 -> Color(0xFF00C853)
        deal.savings >= 40 -> Color(0xFFFFB300)
        else -> ScrapbookDark
    }
    Box(modifier = Modifier.width(160.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper),
                    contentAlignment = Alignment.Center
                ) {
                    if (deal.thumb.isNotBlank()) {
                        AsyncImage(model = deal.thumb, contentDescription = deal.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else { Text("🎮", fontSize = 32.sp) }
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(savingsColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("-${deal.savings}%", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                    }
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.8f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(storeName(deal.storeId), fontFamily = BangersFontFamily, color = Color.White, fontSize = 8.sp)
                    }
                }
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(deal.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$${deal.salePrice}", fontFamily = BangersFontFamily, color = savingsColor, fontSize = 16.sp)
                        Text("$${deal.normalPrice}", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                }
            }
        }
    }
}

// ─── Retro Game of the Day Card ───────────────────────────────────────────────

@Composable
fun RetroGameOfDayCard(game: RetroGameOfDay, onViewInDatabase: () -> Unit) {
    Box {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, cornerRadius = 16.dp, shadowOffset = 5.dp) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                if (!game.coverUrl.isNullOrBlank()) {
                    AsyncImage(model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.35f)
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(colors = listOf(ScrapbookDark.copy(alpha = 0.97f), ScrapbookDark.copy(alpha = 0.7f)))))
                Row(modifier = Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookPaper).border(3.dp, ScrapbookYellow, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!game.coverUrl.isNullOrBlank()) {
                            AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else { Text("🎮", fontSize = 40.sp) }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("🎮 GAME OF THE DAY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(game.name, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        game.releaseYear?.let { Text("$it", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp) }
                        game.rating?.let { rating ->
                            Spacer(modifier = Modifier.height(4.dp))
                            val score = (rating / 10).toInt()
                            val ratingColor = when { rating >= 80 -> Color(0xFF00C853); rating >= 60 -> Color(0xFFFFB300); else -> Color(0xFFFF5252) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(ratingColor).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text("$score/10", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp)
                                }
                                Text(when { rating >= 80 -> "⭐ OUTSTANDING"; rating >= 70 -> "👍 GREAT"; rating >= 60 -> "✅ GOOD"; else -> "😐 MIXED" }, fontFamily = BangersFontFamily, color = ratingColor, fontSize = 12.sp)
                            }
                        }
                        game.summary?.let { summary ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(summary, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onViewInDatabase() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("VIEW IN DATABASE →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Discover Screen ──────────────────────────────────────────────────────────

@Composable
fun DiscoverScreen(
    authViewModel: AuthViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    streamsViewModel: StreamsViewModel = viewModel(),
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToMagazines: () -> Unit = {},
    onNavigateToArticles: () -> Unit = {},
    onNavigateToStreams: () -> Unit = {},
    onNavigateToGameDatabase: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()
    val twitchState by streamsViewModel.twitchStreams.collectAsState()
    val communityStreamers by streamsViewModel.communityStreamers.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var discoverFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearchingUsers by remember { mutableStateOf(false) }
    var realUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserProfileData?>(null) }
    var selectedNews by remember { mutableStateOf<NewsItem?>(null) }

    var trendingUsers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var recentArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var featuredArticle by remember { mutableStateOf<ArticleItem?>(null) }
    var topPlayers by remember { mutableStateOf<List<UserProfileData>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(true) }

    var newsList by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isLoadingNews by remember { mutableStateOf(true) }
    var gameDeals by remember { mutableStateOf<List<GameDeal>>(emptyList()) }
    var isLoadingDeals by remember { mutableStateOf(true) }
    var gameOfDay by remember { mutableStateOf<RetroGameOfDay?>(null) }
    var isLoadingGameOfDay by remember { mutableStateOf(true) }

    val liveStreamers = remember(twitchState, communityStreamers) {
        if (twitchState is StreamsState.Success<*>) {
            @Suppress("UNCHECKED_CAST")
            val streams = (twitchState as StreamsState.Success<TwitchStream>).data
            communityStreamers.filter { streamer ->
                streams.any { it.userName.lowercase() == streamer.twitchUsername.lowercase() }
            }.take(3)
        } else emptyList()
    }

    LaunchedEffect(Unit) {
        isLoadingNews = true
        try {
            val allNews = rssSources.flatMap { (name, color, url) ->
                fetchNewsFromRss(name, color, url, limit = 8)
            }.shuffled()
            newsList = allNews
        } catch (e: Exception) { }
        finally { isLoadingNews = false }
    }

    LaunchedEffect(Unit) {
        isLoadingDeals = true
        try { gameDeals = fetchGameDeals() } catch (e: Exception) { }
        finally { isLoadingDeals = false }
    }

    LaunchedEffect(Unit) {
        isLoadingGameOfDay = true
        try { gameOfDay = fetchRetroGameOfDay() } catch (e: Exception) { }
        finally { isLoadingGameOfDay = false }
    }

    LaunchedEffect(Unit) {
        isLoadingTrending = true
        try {
            val firestore = FirebaseFirestore.getInstance()
            val usersDoc = firestore.collection("users")
                .orderBy("followersCount", Query.Direction.DESCENDING)
                .limit(8).get().await()
            val allFetched = usersDoc.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                if (doc.id == currentUser?.uid) return@mapNotNull null
                UserProfileData(
                    uid = doc.id,
                    username = data["username"] as? String ?: "",
                    userHandle = data["userHandle"] as? String ?: "",
                    bio = data["bio"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                    bannerUrl = data["bannerUrl"] as? String ?: "",
                    followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                    setupComplete = data["setupComplete"] as? Boolean ?: false,
                    topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                    twitchUsername = data["twitchUsername"] as? String ?: "",
                    youtubeUsername = data["youtubeUsername"] as? String ?: ""
                )
            }
            trendingUsers = allFetched.take(5)
            topPlayers = allFetched.take(3)

            try {
                val featuredDoc = firestore.collection("articles")
                    .orderBy("viewCount", Query.Direction.DESCENDING)
                    .limit(1).get().await()
                featuredArticle = featuredDoc.documents.firstOrNull()?.let { doc ->
                    val data = doc.data ?: return@let null
                    ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String, imageUrl = data["headerImageUrl"] as? String)
                }?.takeIf { it.title.isNotBlank() }
            } catch (e: Exception) {
                val fallback = firestore.collection("articles").orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get().await()
                featuredArticle = fallback.documents.firstOrNull()?.let { doc ->
                    val data = doc.data ?: return@let null
                    ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String, imageUrl = data["headerImageUrl"] as? String)
                }?.takeIf { it.title.isNotBlank() }
            }

            val articlesDoc = firestore.collection("articles").orderBy("timestamp", Query.Direction.DESCENDING).limit(6).get().await()
            recentArticles = articlesDoc.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", author = data["authorUsername"] as? String, imageUrl = data["headerImageUrl"] as? String)
            }.filter { it.title.isNotBlank() }

        } catch (e: Exception) { } finally { isLoadingTrending = false }
        authViewModel.fetchAllUsers()
    }

    LaunchedEffect(allUsers) { streamsViewModel.loadCommunityStreamers(allUsers) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(600)
            isSearchingUsers = true
            try {
                val firestore = FirebaseFirestore.getInstance()
                val byUsername = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("username", searchQuery.lowercase())
                    .whereLessThanOrEqualTo("username", searchQuery.lowercase() + "\uf8ff")
                    .limit(10).get().await()
                val byHandle = firestore.collection("users")
                    .whereGreaterThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}")
                    .whereLessThanOrEqualTo("userHandle", "@${searchQuery.lowercase()}\uf8ff")
                    .limit(10).get().await()
                realUsers = (byUsername.documents + byHandle.documents).distinctBy { it.id }.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    UserProfileData(uid = doc.id, username = data["username"] as? String ?: "", userHandle = data["userHandle"] as? String ?: "", bio = data["bio"] as? String ?: "", email = data["email"] as? String ?: "", profilePictureUrl = data["profilePictureUrl"] as? String ?: "", bannerUrl = data["bannerUrl"] as? String ?: "", followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0, followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0, setupComplete = data["setupComplete"] as? Boolean ?: false, topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(), topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList())
                }.filter { it.uid != currentUser?.uid }
            } catch (e: Exception) { realUsers = emptyList() } finally { isSearchingUsers = false }
        } else { realUsers = emptyList() }
    }

    val localResults: List<DiscoverResult> = remember {
        sampleMagazineCovers.map { DiscoverResult(id = "mag_${it.id}", title = it.title, subtitle = "Virtual Magazine", category = DiscoverCategory.MAGAZINE) } +
                sampleAlbums.map { DiscoverResult(id = "alb_${it.id}", title = it.title, subtitle = it.artist, category = DiscoverCategory.ALBUM) } +
                sampleArticles.map { DiscoverResult(id = "art_${it.id}", title = it.title, subtitle = it.author ?: "Unknown author", category = DiscoverCategory.ARTICLE) }
    }

    val filteredLocal = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else localResults.filter { it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true) }
    }

    val groupedLocal = filteredLocal.groupBy { it.category }

    if (selectedNews != null) {
        NewsReaderScreen(news = selectedNews!!, currentUserId = currentUser?.uid ?: "", onBack = { selectedNews = null })
        return
    }

    if (selectedUser != null) {
        UserProfileViewScreen(user = selectedUser!!, authViewModel = authViewModel, onBack = { selectedUser = null })
        return
    }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DISCOVER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 36.sp, letterSpacing = 2.sp)
                        Text("News, deals, people & more", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    if (!isLoadingNews && newsList.isNotEmpty()) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF00FF88)))
                                Text("LIVE FEED", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ✅ Search bar
            Box(modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; hasSearched = it.isNotBlank() },
                    placeholder = { Text("Search users, games, articles...", fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextMuted) },
                    leadingIcon = {
                        if (isSearchingUsers) CircularProgressIndicator(color = ScrapbookDark, modifier = Modifier.size(20.dp).padding(2.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; hasSearched = false; realUsers = emptyList(); focusManager.clearFocus() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookDark, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ✅ Filter chips
            if (!hasSearched) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoverFilters) { filter ->
                        val isSelected = discoverFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) ScrapbookDark else ScrapbookCardWhite)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                .clickable { discoverFilter = filter }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(filter, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookYellow else ScrapbookDark, fontSize = 13.sp)
                        }
                    }
                }
            }

            when {
                !hasSearched -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp)
                    ) {

                        // ─── ALL FILTER ONLY SECTIONS ─────────────────────────
                        if (discoverFilter == "ALL") {

                            // 1. News Section
                            item {
                                NewsSection(
                                    allNews = newsList,
                                    isLoadingNews = isLoadingNews,
                                    onNewsClick = { selectedNews = it }
                                )
                            }

                            // 2. Game of the Day
                            item {
                                DiscoverSectionRow(title = "RETRO GAME OF THE DAY", emoji = "🎮", onSeeAll = { onNavigateToGameDatabase() })
                                Spacer(modifier = Modifier.height(10.dp))
                                when {
                                    isLoadingGameOfDay -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Picking today's game...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                    gameOfDay != null -> {
                                        RetroGameOfDayCard(game = gameOfDay!!, onViewInDatabase = { onNavigateToGameDatabase() })
                                    }
                                    else -> { }
                                }
                            }

                            // 3. Game Deals
                            if (gameDeals.isNotEmpty() || isLoadingDeals) {
                                item {
                                    DiscoverSectionRow(title = "GAMING DEALS", emoji = "🔥", onSeeAll = null)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (isLoadingDeals) {
                                        Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(28.dp))
                                        }
                                    } else {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                                            items(gameDeals, key = { it.title }) { deal ->
                                                GameDealCard(deal = deal)
                                            }
                                        }
                                    }
                                }
                            }
                        } // ✅ END if (discoverFilter == "ALL")

                        // ─── LIVE NOW ─────────────────────────────────────────
                        if (liveStreamers.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "LIVE")) {
                            item {
                                DiscoverSectionRow(title = "LIVE NOW", emoji = "🔴", onSeeAll = { onNavigateToStreams() })
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                    items(liveStreamers, key = { it.uid }) { streamer ->
                                        LiveStreamerCard(streamer = streamer)
                                    }
                                }
                            }
                        }

                        // ─── TOP PLAYERS ──────────────────────────────────────
                        if (topPlayers.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "PEOPLE")) {
                            item {
                                DiscoverSectionRow(title = "TOP PLAYERS", emoji = "🏆", onSeeAll = null)
                                Spacer(modifier = Modifier.height(10.dp))
                                TrendingPlayersCard(
                                    users = topPlayers,
                                    followingUids = followingUids,
                                    currentUid = currentUser?.uid ?: "",
                                    onFollowClick = { user ->
                                        if (followingUids.contains(user.uid)) authViewModel.unfollowUser(user.uid)
                                        else authViewModel.followUser(user.uid)
                                    },
                                    onTap = { selectedUser = it }
                                )
                            }
                        }

                        // ─── TRENDING USERS ───────────────────────────────────
                        if (trendingUsers.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "PEOPLE")) {
                            item {
                                DiscoverSectionRow(title = "TRENDING USERS", emoji = "🔥", onSeeAll = null)
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                    items(trendingUsers, key = { it.uid }) { user ->
                                        ScrapbookTrendingUserCard(
                                            user = user,
                                            isFollowing = followingUids.contains(user.uid),
                                            onTap = { selectedUser = user },
                                            onFollowClick = {
                                                if (followingUids.contains(user.uid)) authViewModel.unfollowUser(user.uid)
                                                else authViewModel.followUser(user.uid)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ─── FEATURED ARTICLE ─────────────────────────────────
                        if (featuredArticle != null && (discoverFilter == "ALL" || discoverFilter == "ARTICLES")) {
                            item {
                                DiscoverSectionRow(title = "FEATURED ARTICLE", emoji = "⭐", onSeeAll = { onNavigateToArticles() })
                                Spacer(modifier = Modifier.height(10.dp))
                                FeaturedArticleCard(article = featuredArticle!!)
                            }
                        }

                        // ─── RECENT ARTICLES ──────────────────────────────────
                        if (recentArticles.isNotEmpty() && (discoverFilter == "ALL" || discoverFilter == "ARTICLES")) {
                            item {
                                DiscoverSectionRow(title = "RECENT ARTICLES", emoji = "📝", onSeeAll = { onNavigateToArticles() })
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                    items(recentArticles, key = { "art_${it.id}" }) { article ->
                                        ArticleMiniCard(article = article)
                                    }
                                }
                            }
                        }

                        // ─── MAGAZINES ────────────────────────────────────────
                        if (discoverFilter == "ALL" || discoverFilter == "MAGAZINES") {
                            item {
                                DiscoverSectionRow(title = "FEATURED MAGAZINES", emoji = "📰", onSeeAll = { onNavigateToMagazines() })
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                    items(sampleMagazineCovers.take(6), key = { "mag_${it.id}" }) { mag ->
                                        ScrapbookTrendingMagazineCard(magazine = mag)
                                    }
                                }
                            }
                        }

                        // ─── ALBUMS ───────────────────────────────────────────
                        if (discoverFilter == "ALL" || discoverFilter == "ALBUMS") {
                            item {
                                DiscoverSectionRow(title = "FEATURED ALBUMS", emoji = "🎵", onSeeAll = { onNavigateToAlbums() })
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
                                    items(sampleAlbums.take(6), key = { "alb_${it.id}" }) { album ->
                                        ScrapbookTrendingAlbumCard(album = album)
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Search empty state ────────────────────────────────────────
                realUsers.isEmpty() && filteredLocal.isEmpty() && !isSearchingUsers -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No results for \"$searchQuery\"", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }

                // ─── Search results ────────────────────────────────────────────
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        if (realUsers.isNotEmpty()) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("USERS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("${realUsers.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                    }
                                }
                            }
                            items(realUsers, key = { it.uid }) { user ->
                                ScrapbookDiscoverUserCard(user = user, isFollowing = followingUids.contains(user.uid), isCurrentUser = user.uid == currentUser?.uid, onFollowClick = { if (followingUids.contains(user.uid)) authViewModel.unfollowUser(user.uid) else authViewModel.followUser(user.uid) }, onTap = { selectedUser = user })
                            }
                        }
                        DiscoverCategory.values().filter { it != DiscoverCategory.USER }.forEach { category ->
                            val categoryResults = groupedLocal[category]
                            if (!categoryResults.isNullOrEmpty()) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(category.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text("${categoryResults.size}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                        }
                                    }
                                }
                                items(categoryResults, key = { it.id }) { result ->
                                    ScrapbookDiscoverResultCard(result = result)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Row ──────────────────────────────────────────────────────────────

@Composable
fun DiscoverSectionRow(title: String, emoji: String, onSeeAll: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        if (onSeeAll != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onSeeAll() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("SEE ALL →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
            }
        }
    }
}

// ─── Featured Article Card ────────────────────────────────────────────────────

@Composable
fun FeaturedArticleCard(article: ArticleItem) {
    Box {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 14.dp, shadowOffset = 5.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))) {
                    if (!article.imageUrl.isNullOrBlank()) {
                        AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(ScrapbookPaper), contentAlignment = Alignment.Center) { Text("📰", fontSize = 48.sp) }
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("⭐ COMMUNITY PICK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                        Text(article.title, fontFamily = BangersFontFamily, color = Color.White, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
                        article.author?.let { Text("by $it", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) }
                    }
                }
                if (article.snippet.isNotBlank()) {
                    Text(article.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

// ─── Article Mini Card ────────────────────────────────────────────────────────

@Composable
fun ArticleMiniCard(article: ArticleItem) {
    Box(modifier = Modifier.width(190.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(ScrapbookPaper), contentAlignment = Alignment.Center) {
                    if (!article.imageUrl.isNullOrBlank()) {
                        AsyncImage(model = article.imageUrl, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (article.imageResId != null) {
                        Image(painter = painterResource(id = article.imageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else { Text("📝", fontSize = 32.sp) }
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)))))
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 17.sp)
                    article.author?.let { Spacer(modifier = Modifier.height(3.dp)); Text("by $it", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

// ─── Live Streamer Card ───────────────────────────────────────────────────────

@Composable
fun LiveStreamerCard(streamer: CommunityStreamer) {
    Box(modifier = Modifier.width(120.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(ScrapbookPaper).border(3.dp, Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                        if (streamer.profilePicUrl.isNotBlank()) {
                            AsyncImage(model = streamer.profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else { Text(streamer.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp) }
                    }
                    Box(modifier = Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(Color.Red).border(2.dp, Color.White, CircleShape))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(streamer.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.Red).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("🔴 LIVE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                }
                if (streamer.twitchUsername.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("/${streamer.twitchUsername}", fontFamily = NunitoFontFamily, color = Color(0xFF9146FF), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ─── Trending Players Card ────────────────────────────────────────────────────

@Composable
fun TrendingPlayersCard(users: List<UserProfileData>, followingUids: Set<String>, currentUid: String, onFollowClick: (UserProfileData) -> Unit, onTap: (UserProfileData) -> Unit) {
    Box {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                users.forEachIndexed { index, user ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onTap(user) }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(when (index) { 0 -> Color(0xFFFFD700); 1 -> Color(0xFFC0C0C0); else -> Color(0xFFCD7F32) }).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                            if (!user.profilePictureUrl.isNullOrBlank()) {
                                AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else { Text(user.username.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp) }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${formatCount(user.followersCount)} followers", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                        }
                        if (user.uid != currentUid) {
                            val isFollowing = followingUids.contains(user.uid)
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isFollowing) ScrapbookPaper else ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onFollowClick(user) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(if (isFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, fontSize = 12.sp, color = if (isFollowing) ScrapbookDark else ScrapbookYellow)
                            }
                        }
                    }
                    if (index < users.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = ScrapbookBorder.copy(alpha = 0.15f))
                }
            }
        }
    }
}

// ─── Trending User Card ───────────────────────────────────────────────────────

@Composable
fun ScrapbookTrendingUserCard(user: UserProfileData, isFollowing: Boolean, onTap: () -> Unit, onFollowClick: () -> Unit) {
    Box(modifier = Modifier.width(110.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onTap() }, backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                    if (!user.profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else { Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(28.dp)) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(user.username, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("${formatCount(user.followersCount)} followers", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isFollowing) ScrapbookPaper else ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onFollowClick() }.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                    Text(if (isFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, fontSize = 11.sp, color = if (isFollowing) ScrapbookDark else ScrapbookYellow)
                }
            }
        }
    }
}

// ─── Magazine Card ────────────────────────────────────────────────────────────

@Composable
fun ScrapbookTrendingMagazineCard(magazine: MagazineCover) {
    Box(modifier = Modifier.width(100.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper)) {
                    when {
                        magazine.coverImageResId != null -> Image(painter = painterResource(id = magazine.coverImageResId), contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        magazine.coverImageUrl != null -> AsyncImage(model = magazine.coverImageUrl, contentDescription = magazine.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📰", fontSize = 28.sp) }
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(magazine.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 13.sp, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─── Album Card ───────────────────────────────────────────────────────────────

@Composable
fun ScrapbookTrendingAlbumCard(album: Album) {
    Box(modifier = Modifier.width(100.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper), contentAlignment = Alignment.Center) {
                    if (album.coverImageResId != null) {
                        Image(painter = painterResource(id = album.coverImageResId), contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else { Text("🎵", fontSize = 28.sp) }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(album.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 13.sp, modifier = Modifier.fillMaxWidth())
                Text(album.artist, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─── User Search Card ─────────────────────────────────────────────────────────

@Composable
fun ScrapbookDiscoverUserCard(user: UserProfileData, isFollowing: Boolean, isCurrentUser: Boolean, onFollowClick: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { onTap() }, backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                    if (!user.profilePictureUrl.isNullOrBlank()) {
                        AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else { Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(24.dp)) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.username.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(user.userHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                    if (user.bio.isNotBlank()) Text(user.bio, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!isCurrentUser) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isFollowing) ScrapbookPaper else ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onFollowClick() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(if (isFollowing) "FOLLOWING" else "FOLLOW", fontFamily = BangersFontFamily, fontSize = 13.sp, color = if (isFollowing) ScrapbookDark else ScrapbookYellow)
                    }
                }
            }
        }
    }
}

// ─── Result Card ──────────────────────────────────────────────────────────────

@Composable
fun ScrapbookDiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { }, backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(ScrapbookDark, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(result.subtitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(result.category.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                }
            }
        }
    }
}

// ─── Backward Compat Wrappers ─────────────────────────────────────────────────

@Composable fun ScrapbookDiscoverHeader(title: String, emoji: String) { DiscoverSectionRow(title = title, emoji = emoji, onSeeAll = null) }
@Composable fun TrendingSectionHeader(title: String, color: Color) { DiscoverSectionRow(title = title, emoji = "🔥", onSeeAll = null) }
@Composable fun TrendingUserCard(user: UserProfileData, isFollowing: Boolean, onTap: () -> Unit, onFollowClick: () -> Unit) { ScrapbookTrendingUserCard(user = user, isFollowing = isFollowing, onTap = onTap, onFollowClick = onFollowClick) }
@Composable fun TrendingMagazineCard(magazine: MagazineCover) { ScrapbookTrendingMagazineCard(magazine = magazine) }
@Composable fun TrendingAlbumCard(album: Album) { ScrapbookTrendingAlbumCard(album = album) }
@Composable fun DiscoverUserCard(user: UserProfileData, isFollowing: Boolean, isCurrentUser: Boolean, onFollowClick: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) { ScrapbookDiscoverUserCard(user = user, isFollowing = isFollowing, isCurrentUser = isCurrentUser, onFollowClick = onFollowClick, onTap = onTap, modifier = modifier) }
@Composable fun DiscoverResultCard(result: DiscoverResult, modifier: Modifier = Modifier) { ScrapbookDiscoverResultCard(result = result, modifier = modifier) }
@Composable fun TrendingArticleCard(article: ArticleItem) { ScrapbookTrendingArticleCard(article = article) }
@Composable fun ScrapbookTrendingArticleCard(article: ArticleItem) {
    Box(modifier = Modifier.padding(vertical = 4.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    if (!article.imageUrl.isNullOrBlank()) { AsyncImage(model = article.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    else if (article.imageResId != null) { Image(painter = painterResource(id = article.imageResId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    else { Text("📝", fontSize = 24.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                    article.author?.let { Text("by $it", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp) }
                }
            }
        }
    }
}
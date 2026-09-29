package com.example.hubretro

import android.util.Log
import android.util.Xml
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════════════════════════
// LIVE FEEDS — free RSS sources that update many times a day (no API key needed).
// Merged with NewsAPI in ContentViewModel, used by the weekly digest and the
// background sync worker ("new stories" notifications).
// ═══════════════════════════════════════════════════════════════════════════════

object LiveFeeds {
    private const val TAG = "LiveFeeds"

    data class Feed(val name: String, val url: String, val category: String)

    val feeds = listOf(
        Feed("Time Extension", "https://www.timeextension.com/feeds/latest", "RETRO"),
        Feed("Nintendo Life",  "https://www.nintendolife.com/feeds/latest",  "NINTENDO"),
        Feed("Push Square",    "https://www.pushsquare.com/feeds/latest",    "PLAYSTATION"),
        Feed("Pure Xbox",      "https://www.purexbox.com/feeds/latest",      "XBOX"),
        Feed("Gematsu",        "https://www.gematsu.com/feed",               "GAMING")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile private var cache: List<ArticleItem> = emptyList()
    @Volatile private var cacheAt = 0L
    private const val CACHE_MS = 5 * 60 * 1000L

    /** Newest-first stories from every feed (cached 5 min). */
    suspend fun fetchAll(force: Boolean = false): List<ArticleItem> = withContext(Dispatchers.IO) {
        if (!force && cache.isNotEmpty() && System.currentTimeMillis() - cacheAt < CACHE_MS) return@withContext cache
        val all = coroutineScope {
            feeds.map { f -> async { runCatching { fetchFeed(f) }.getOrElse { Log.w(TAG, "${f.name}: ${it.message}"); emptyList() } } }
                .awaitAll().flatten()
        }
        val merged = all
            .distinctBy { it.title.lowercase().filter { c -> c.isLetterOrDigit() }.take(60) }
            .sortedByDescending { it.publishedAt }
        if (merged.isNotEmpty()) { cache = merged; cacheAt = System.currentTimeMillis() }
        merged
    }

    /** Filter for the topic chips in the News tab. */
    fun matchesTopic(a: ArticleItem, topic: String): Boolean {
        if (topic == "ALL") return true
        val t = topic.lowercase()
        if (t.contains("retro") || t.contains("arcade") || t.contains("pixel")) {
            if (a.category == "RETRO") return true
        }
        if (t.contains("nintendo") && a.category == "NINTENDO") return true
        val words = t.split(" ").filter { it.length > 3 && it !in setOf("game", "games", "video", "gaming") }
        val hay = (a.title + " " + a.snippet).lowercase()
        return words.any { hay.contains(it) }
    }

    private fun fetchFeed(feed: Feed): List<ArticleItem> {
        val req = Request.Builder().url(feed.url)
            .header("User-Agent", "RetroHub/1.0 (Android)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val stream = resp.body?.byteStream() ?: return emptyList()
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, null)

            val out = mutableListOf<ArticleItem>()
            var inItem = false
            var title = ""; var link = ""; var desc = ""; var pub = ""; var image: String? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "item") {
                            inItem = true; title = ""; link = ""; desc = ""; pub = ""; image = null
                        } else if (inItem) {
                            when (name) {
                                "title" -> title = parser.nextText()
                                "link" -> link = parser.nextText()
                                "description" -> desc = parser.nextText()
                                "pubDate" -> pub = parser.nextText()
                                "media:content", "media:thumbnail" -> {
                                    val u = parser.getAttributeValue(null, "url")
                                    // prefer the large media:content over the small thumbnail
                                    if (!u.isNullOrBlank() && (image == null || name == "media:content")) image = u
                                }
                                "enclosure" -> {
                                    val u = parser.getAttributeValue(null, "url")
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    if (image == null && !u.isNullOrBlank() && type.startsWith("image")) image = u
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                        inItem = false
                        buildItem(feed, title, link, desc, pub, image)?.let { out.add(it) }
                    }
                }
                event = parser.next()
            }
            return out
        }
    }

    private fun buildItem(feed: Feed, rawTitle: String, link: String, desc: String, pub: String, image: String?): ArticleItem? {
        val title = Jsoup.parse(rawTitle).text().trim()
        if (title.isBlank() || link.isBlank()) return null
        val doc = Jsoup.parse(desc)
        val img = image ?: doc.select("img[src]").firstOrNull()?.attr("src")?.takeIf { it.startsWith("http") }
        val text = doc.text()
            .replace(Regex("Read the full article on \\S+"), "")
            .replace("Source", "")
            .trim()
        val published = parseRssDate(pub)
        return ArticleItem(
            id = "rss_" + link.hashCode(),
            title = title,
            snippet = text.take(220).ifBlank { "Read more on ${feed.name}" },
            fullContent = text,
            date = friendlyDate(published),
            author = feed.name,
            imageUrl = img,
            category = feed.category,
            sourceUrl = link.trim(),
            sourceName = feed.name,
            isNewsArticle = true,
            publishedAt = published
        )
    }

    private val rssFormats = listOf("EEE, dd MMM yyyy HH:mm:ss zzz", "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss Z")

    fun parseRssDate(s: String): Long {
        val v = s.trim()
        for (p in rssFormats) {
            try { return SimpleDateFormat(p, Locale.US).parse(v)?.time ?: continue } catch (_: Exception) {}
        }
        return 0L
    }

    fun parseIsoDate(s: String): Long = try { java.time.Instant.parse(s.trim()).toEpochMilli() } catch (_: Exception) { 0L }

    /** "5m ago", "3h ago", "Yesterday", "Sep 21" */
    fun friendlyDate(millis: Long): String {
        if (millis <= 0L) return ""
        val diff = System.currentTimeMillis() - millis
        return when {
            diff < 0 -> SimpleDateFormat("MMM d", Locale.getDefault()).format(java.util.Date(millis))
            diff < 60 * 60_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m ago"
            diff < 24 * 3_600_000L -> "${diff / 3_600_000L}h ago"
            diff < 48 * 3_600_000L -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(java.util.Date(millis))
        }
    }
}

/** Blinking "JUST IN" / "NEW" sticker for fresh stories & articles. Renders nothing when old. */
@Composable
fun FreshBadge(publishedAt: Long, modifier: Modifier = Modifier) {
    if (publishedAt <= 0L) return
    val age = System.currentTimeMillis() - publishedAt
    val label = when {
        age < 0 -> return
        age < 6 * 3_600_000L -> "🆕 JUST IN"
        age < 48 * 3_600_000L -> "NEW"
        else -> return
    }
    val pulse by rememberGlowRange(0.94f, 1.06f)
    Text(
        label,
        modifier = modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(RoundedCornerShape(6.dp))
            .background(CGreen)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = TextStyle(fontFamily = BangersFontFamily, fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White)
    )
}

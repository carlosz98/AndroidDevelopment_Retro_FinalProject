package com.example.hubretro.ui.news // Or your chosen package structure

import android.app.Application
import android.os.Build
import android.text.Html
import android.util.Log
import android.util.Xml
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hubretro.data.models.NewsItem // Your NewsItem data class
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val _newsItems = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsItems: StateFlow<List<NewsItem>> = _newsItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        // Updated to the retronews.com feed URL
        private const val NEWS_RSS_FEED_URL = "https://www.retronews.com/feed/"
        private const val TAG = "NewsViewModel"
        // Default Source Name - you might want to extract this from the feed's <channel><title>
        private const val DEFAULT_SOURCE_NAME = "Retro News"
    }

    init {
        fetchNews()
    }

    fun fetchNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val fetchedItems = fetchAndParseRssFeed() // Changed to call the new function
                _newsItems.value = fetchedItems
                if (fetchedItems.isEmpty() && _error.value == null) {
                    Log.w(TAG, "Fetched RSS feed successfully, but the list is empty.")
                    // Optionally: _error.value = "No news articles found."
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network or IO error fetching RSS feed", e)
                _error.value = "Network error. Please check connection."
                _newsItems.value = emptyList()
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "XML Parsing error for RSS feed", e)
                _error.value = "Error reading news data (bad format)."
                _newsItems.value = emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching or parsing RSS feed", e)
                _error.value = "Failed to load news. Try again later."
                _newsItems.value = emptyList() // Clear data on other errors too
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchAndParseRssFeed(): List<NewsItem> = withContext(Dispatchers.IO) {
        val httpClient = OkHttpClient()
        val request = Request.Builder().url(NEWS_RSS_FEED_URL).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Fetch failed: ${response.code} from ${request.url}")
                throw IOException("Unexpected code ${response.code} from ${request.url}")
            }
            response.body?.byteStream()?.let { inputStream ->
                // Here we will also try to get the channel title for sourceName
                val channelInfo = parseChannelInfo(inputStream) // We need a rewindable stream or two parses
                // For simplicity now, let's assume we re-fetch or use a default for sourceName
                // A better solution would involve a custom InputStream that can be reset or parsing in one go.

                // Re-open stream for item parsing (simplistic approach, not most efficient)
                // This is needed because the first parseChannelInfo consumes the stream.
                // A more advanced solution would parse channel and items in one pass or use a resettable stream.
                // For now, we'll re-request for simplicity or use a default sourceName.
                // To avoid re-requesting, we'll use DEFAULT_SOURCE_NAME for now and improve later if needed.
                // If you want accurate sourceName from feed, the parsing logic needs to be more complex.
                // Let's re-open the stream by making the call again or passing the body string
                // For now, let's just make the call again for the actual item parsing.
                httpClient.newCall(request).execute().use { itemResponse ->
                    itemResponse.body?.byteStream()?.let { itemInputStream ->
                        return@withContext parseRssItems(itemInputStream, channelInfo.channelTitle ?: DEFAULT_SOURCE_NAME)
                    } ?: throw IOException("Response body for items is null from ${request.url}")
                }


            } ?: run {
                Log.e(TAG, "Response body is null from ${request.url}")
                throw IOException("Response body is null from ${request.url}")
            }
        }
    }

    // Helper data class for channel info (mainly for source name)
    private data class RssChannelInfo(val channelTitle: String?)

    // This function is a placeholder for a more complex one-pass parser.
    // For now, it's not used to avoid stream consumption issues before item parsing.
    // We'll rely on DEFAULT_SOURCE_NAME.
    @Suppress("unused") // Temporarily unused
    private fun parseChannelInfo(inputStream: InputStream): RssChannelInfo {
        // Simplified: In a real scenario, you'd parse <channel><title> here
        // without fully consuming the stream if items are to be parsed next from the same stream.
        // For now, this is a conceptual placeholder.
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) // RSS usually doesn't rely heavily on item namespaces
        parser.setInput(inputStream, "UTF-8")
        var eventType = parser.eventType
        var inChannel = false
        var channelTitle: String? = null
        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.ROOT)
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "channel") {
                            inChannel = true
                        } else if (inChannel && tagName == "title") {
                            // Advance parser to get text
                            if (parser.next() == XmlPullParser.TEXT) {
                                channelTitle = parser.text?.trim()
                            }
                            // Found what we need, can break from channel parsing for this simple version
                            return RssChannelInfo(channelTitle)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "channel") {
                            inChannel = false
                            // If we didn't find title earlier, break after channel
                            return RssChannelInfo(channelTitle)
                        }
                    }
                }
                if (channelTitle != null) break // Optimization: stop if title found
                eventType = parser.next()
            }
        } catch (e: Exception){
            Log.e(TAG, "Could not parse channel title", e)
        }
        return RssChannelInfo(channelTitle)
    }


    private fun parseRssItems(inputStream: InputStream, sourceName: String): List<NewsItem> {
        val parser: XmlPullParser = Xml.newPullParser()
        // Enable namespace processing for potential <media:thumbnail> or other namespaced elements
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(inputStream, "UTF-8")

        val newsItemsList = mutableListOf<NewsItem>()
        var eventType = parser.eventType
        var currentItemHelper: RssItemDataHelper? = null
        var text: String? = null
        var inItemTag = false

        try {
            while (eventType != XmlPullParser.END_DOCUMENT && newsItemsList.size < 20) { // Limit to 20 items
                val tagName = parser.name?.lowercase(Locale.ROOT)

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item") {
                            currentItemHelper = RssItemDataHelper()
                            inItemTag = true
                        } else if (inItemTag && currentItemHelper != null) {
                            when (tagName) {
                                "enclosure" -> { // Common for images/podcasts in RSS
                                    if (currentItemHelper.imageUrl.isNullOrBlank()) {
                                        val type = parser.getAttributeValue(null, "type")
                                        if (type != null && type.startsWith("image/")) {
                                            currentItemHelper.imageUrl = parser.getAttributeValue(null, "url")
                                        }
                                    }
                                }
                                "thumbnail", "content" -> { // For <media:thumbnail> or <media:content>
                                    if (parser.namespace == "http://search.yahoo.com/mrss/" && currentItemHelper.imageUrl.isNullOrBlank()) {
                                        val imageUrl = parser.getAttributeValue(null, "url")
                                        // For <media:content>, also check if medium is image
                                        val medium = parser.getAttributeValue(null, "medium")
                                        if (tagName == "content" && medium != null && medium != "image") {
                                            // Skip if media:content is not an image
                                        } else if (!imageUrl.isNullOrBlank()){
                                            currentItemHelper.imageUrl = imageUrl
                                        }
                                    }
                                }
                                // Other tags like category could be parsed here
                                "category" -> {
                                    // For simplicity, we'll grab the first category if NewsItem.category is singular
                                    if (currentItemHelper.category.isNullOrBlank() && parser.next() == XmlPullParser.TEXT) {
                                        currentItemHelper.category = parser.text?.trim()
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        text = parser.text?.trim()
                    }
                    XmlPullParser.END_TAG -> {
                        if (inItemTag && currentItemHelper != null) {
                            when (tagName) {
                                "title" -> currentItemHelper.title = text ?: ""
                                "link" -> currentItemHelper.sourceUrl = text ?: ""
                                "description", "content:encoded" -> { // content:encoded is often full content
                                    val htmlContent = text ?: ""
                                    if (currentItemHelper.summary.isBlank()){ // Prefer description, but take content if summary still blank
                                        currentItemHelper.summary = stripHtmlWithJsoup(htmlContent).take(200) +
                                                if (stripHtmlWithJsoup(htmlContent).length > 200) "..." else ""
                                    }
                                    // Try to extract image from description if no other source found yet
                                    if (currentItemHelper.imageUrl.isNullOrBlank()) {
                                        currentItemHelper.imageUrl = extractFirstImageWithJsoup(htmlContent)
                                    }
                                }
                                "pubdate" -> currentItemHelper.publishedDateString = text ?: ""
                                "guid" -> currentItemHelper.guid = text ?: ""
                                "item" -> {
                                    val finalItem = NewsItem(
                                        id = currentItemHelper.guid.ifBlank { currentItemHelper.sourceUrl ?: System.currentTimeMillis().toString() },
                                        title = currentItemHelper.title,
                                        summary = currentItemHelper.summary,
                                        sourceName = sourceName, // Use the determined source name
                                        sourceUrl = currentItemHelper.sourceUrl ?: "",
                                        imageUrl = currentItemHelper.imageUrl,
                                        publishedDate = parseRssDate(currentItemHelper.publishedDateString)
                                            ?: System.currentTimeMillis(), // Fallback to current time
                                        category = currentItemHelper.category
                                    )
                                    // Ensure essential fields are present
                                    if (finalItem.title.isNotBlank() && finalItem.sourceUrl.isNotBlank()) {
                                        newsItemsList.add(finalItem)
                                    }
                                    currentItemHelper = null
                                    inItemTag = false
                                }
                            }
                        }
                        text = null // Reset text
                    }
                }
                if (eventType != XmlPullParser.END_DOCUMENT) { // Prevent issues at end of doc
                    eventType = parser.next()
                }
            }
        } catch (e: XmlPullParserException) {
            Log.e(TAG, "Error during RSS XML parsing items", e)
            throw e // Re-throw to be caught by the calling function's error handling
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during RSS XML parsing items", e)
            // Depending on policy, you might want to re-throw or return partial list
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to close item input stream", e)
            }
        }
        return newsItemsList
    }

    // Helper data class for temporarily storing parsed RSS item data
    private data class RssItemDataHelper(
        var title: String = "",
        var sourceUrl: String? = null,
        var summary: String = "",
        var publishedDateString: String? = null,
        var guid: String = "",
        var imageUrl: String? = null,
        var category: String? = null
    )

    private fun parseRssDate(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null

        // Common RSS <pubDate> format: RFC 822 (or 1123)
        // Examples: "Wed, 02 Oct 2002 08:00:00 EST", "Mon, 20 May 2024 10:00:00 GMT"
        // "EEE, dd MMM yyyy HH:mm:ss zzz" is a good pattern.
        val rfc822Pattern = "EEE, dd MMM yyyy HH:mm:ss zzz"
        try {
            val sdf = SimpleDateFormat(rfc822Pattern, Locale.US) // US Locale for day/month names
            return sdf.parse(dateString)?.time
        } catch (e: ParseException) {
            Log.w(TAG, "Failed to parse RSS date with '$rfc822Pattern': $dateString", e)
        }

        // Fallback for dates that might be in ISO 8601 format (less common for <pubDate> but possible)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return java.time.OffsetDateTime.parse(dateString).toInstant().toEpochMilli()
            } catch (e: java.time.format.DateTimeParseException) {
                Log.w(TAG, "Failed to parse date with java.time.OffsetDateTime: $dateString", e)
                try { // Attempt common ISO without offset if that failed
                    return java.time.ZonedDateTime.parse(dateString).toInstant().toEpochMilli()
                } catch (e2: java.time.format.DateTimeParseException) {
                    Log.w(TAG, "Failed to parse date with java.time.ZonedDateTime: $dateString", e2)
                }
            }
        } else {
            // Fallback SimpleDateFormat patterns for ISO-like dates if API < 26
            val isoPatterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            )
            for (pattern in isoPatterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    if (pattern.endsWith("'Z'") || (!pattern.contains("X") && !(dateString.endsWith("Z") || dateString.contains("+") || (dateString.length > 19 && dateString.substring(19).contains("-")) ))) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    return sdf.parse(dateString)?.time
                } catch (e: ParseException) { /* Try next pattern */ }
            }
        }
        Log.w(TAG, "All date parsing attempts failed for: $dateString")
        return null
    }

    private fun stripHtmlWithJsoup(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return try {
            Jsoup.parse(html).text().trim()
        } catch (e: Exception) {
            Log.w(TAG, "Jsoup failed to strip HTML: $html", e)
            // Fallback to Android's Html class if Jsoup fails or for simpler HTML
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html).toString().trim()
            }
        }
    }

    private fun extractFirstImageWithJsoup(html: String?): String? {
        if (html.isNullOrBlank()) return null
        return try {
            val doc = Jsoup.parse(html)
            // Prefer images with absolute URLs
            var imageUrl = doc.select("img[src~=^https?://]").firstOrNull()?.attr("abs:src")
            if (imageUrl.isNullOrBlank()) { // Fallback to any src and hope it's absolute or Jsoup resolves it
                imageUrl = doc.select("img[src]").firstOrNull()?.attr("abs:src") // Jsoup tries to make it absolute
            }
            if (!imageUrl.isNullOrBlank()) imageUrl.trim() else null
        } catch (e: Exception) {
            Log.w(TAG, "Jsoup failed to extract image: ${html.take(100)}...", e)
            null
        }
    }
}

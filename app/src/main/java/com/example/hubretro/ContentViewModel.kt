package com.example.hubretro

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed class ContentState {
    object Idle : ContentState()
    object Loading : ContentState()
    data class Success(val items: List<ArchiveItem>) : ContentState()
    data class Error(val message: String) : ContentState()
}

sealed class LiveArticlesState {
    object Loading : LiveArticlesState()
    data class Success(val items: List<ArticleItem>) : LiveArticlesState()
    data class Error(val message: String) : LiveArticlesState()
}

class ContentViewModel(application: Application) : AndroidViewModel(application) {

    private val _albumsState = MutableStateFlow<ContentState>(ContentState.Idle)
    val albumsState: StateFlow<ContentState> = _albumsState.asStateFlow()

    private val _magazinesState = MutableStateFlow<ContentState>(ContentState.Idle)
    val magazinesState: StateFlow<ContentState> = _magazinesState.asStateFlow()

    private val _articlesState = MutableStateFlow<ContentState>(ContentState.Idle)
    val articlesState: StateFlow<ContentState> = _articlesState.asStateFlow()

    private val _searchState = MutableStateFlow<ContentState>(ContentState.Idle)
    val searchState: StateFlow<ContentState> = _searchState.asStateFlow()

    private val _liveArticlesState = MutableStateFlow<LiveArticlesState>(LiveArticlesState.Loading)
    val liveArticlesState: StateFlow<LiveArticlesState> = _liveArticlesState.asStateFlow()

    val selectedLiveTopic = MutableStateFlow("ALL")

    private var magazinesCurrentPage = 1
    private var magazinesCurrentQuery = ""
    private val magazinesAllItems = mutableListOf<ArchiveItem>()

    private val _isLoadingMoreMagazines = MutableStateFlow(false)
    val isLoadingMoreMagazines: StateFlow<Boolean> = _isLoadingMoreMagazines.asStateFlow()

    private val _hasMoreMagazines = MutableStateFlow(true)
    val hasMoreMagazines: StateFlow<Boolean> = _hasMoreMagazines.asStateFlow()

    private val newsApiKey get() = BuildConfig.NEWS_API_KEY

    /** How many stories appeared since the last refresh (drives the "N NEW STORIES" pill). */
    private val _freshNewsCount = MutableStateFlow(0)
    val freshNewsCount: StateFlow<Int> = _freshNewsCount.asStateFlow()
    fun clearFreshNews() { _freshNewsCount.value = 0 }

    private var currentLiveTopic: String = "ALL"
    private var knownNewsIds: Set<String> = emptySet()

    init {
        fetchAlbums()
        fetchMagazines()
        fetchArticles()
        fetchLiveArticles()
        // Keep news fresh while the app is open: quiet refresh every 10 minutes
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10 * 60 * 1000L)
                fetchLiveArticles(currentLiveTopic, quiet = true)
            }
        }
    }

    /**
     * Live news = RSS feeds (Time Extension, Nintendo Life, Push Square, Pure Xbox, Gematsu)
     * + NewsAPI. Always newest first. quiet = keep current list on screen while refreshing.
     */
    fun fetchLiveArticles(topicOverride: String? = null, quiet: Boolean = false) {
        val topic = topicOverride ?: "ALL"
        currentLiveTopic = topic
        viewModelScope.launch(Dispatchers.IO) {
            val showing = _liveArticlesState.value is LiveArticlesState.Success
            if (!quiet || !showing) _liveArticlesState.value = LiveArticlesState.Loading
            try {
                val rss = try {
                    LiveFeeds.fetchAll(force = !quiet).filter { LiveFeeds.matchesTopic(it, topic) }
                } catch (e: Exception) { emptyList() }
                val api = if (newsApiKey.isNotBlank()) fetchNewsApi(topic) else emptyList()

                val merged = (rss + api)
                    .distinctBy { it.title.lowercase().filter { c -> c.isLetterOrDigit() }.take(60) }
                    .sortedByDescending { it.publishedAt }
                    .take(80)

                if (merged.isEmpty()) {
                    if (!showing || !quiet) _liveArticlesState.value = LiveArticlesState.Error("No live articles found")
                    return@launch
                }
                val ids = merged.map { it.id }.toSet()
                if (knownNewsIds.isNotEmpty()) {
                    val fresh = ids.count { it !in knownNewsIds }
                    if (fresh > 0) _freshNewsCount.value = _freshNewsCount.value + fresh
                }
                knownNewsIds = knownNewsIds + ids
                _liveArticlesState.value = LiveArticlesState.Success(merged)
            } catch (e: Exception) {
                android.util.Log.e("LiveNews", "fetchLiveArticles failed: ${e.message}", e)
                if (!showing) _liveArticlesState.value = LiveArticlesState.Error(e.message ?: "Failed to fetch live articles")
            }
        }
    }

    private fun fetchNewsApi(topic: String): List<ArticleItem> {
        val out = mutableListOf<ArticleItem>()
        val client = OkHttpClient()
        val queries = if (topic != "ALL") listOf(topic) else listOf("retro gaming", "video games", "arcade games")
        for (query in queries) {
            try {
                val url = "https://newsapi.org/v2/everything" +
                        "?q=" + java.net.URLEncoder.encode(query, "UTF-8") +
                        "&language=en&pageSize=15&sortBy=publishedAt&apiKey=$newsApiKey"
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    if (json.optString("status") != "ok") return@use
                    val articles = json.optJSONArray("articles") ?: return@use
                    for (i in 0 until articles.length()) {
                        val article = articles.getJSONObject(i)
                        val title = article.optString("title", "")
                        if (title.isBlank() || title == "[Removed]") continue
                        val description = article.optString("description", "")
                        val content = article.optString("content", description)
                        val published = LiveFeeds.parseIsoDate(article.optString("publishedAt", ""))
                        val cleanedContent = content.ifBlank { description }.let {
                            val cut = it.indexOf("[+"); if (cut > 0) it.substring(0, cut).trim() else it
                        }.ifBlank { description }
                        val sourceName = article.optJSONObject("source")?.optString("name", "Gaming News") ?: "Gaming News"
                        out.add(
                            ArticleItem(
                                id = "news_${title.hashCode()}",
                                title = title,
                                snippet = description.take(200).ifBlank { "Read more about $title" },
                                fullContent = cleanedContent,
                                date = LiveFeeds.friendlyDate(published),
                                author = sourceName,
                                imageUrl = article.optString("urlToImage", "").ifBlank { null },
                                category = if (query.contains("retro", true) || query.contains("arcade", true)) "RETRO" else "GAMING",
                                sourceUrl = article.optString("url", ""),
                                sourceName = sourceName,
                                isNewsArticle = true,
                                publishedAt = published
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NewsAPI", "Query $query failed: ${e.message}")
            }
        }
        return out
    }

    private fun formatGNewsDate(publishedAt: String): String {
        return try {
            val parts = publishedAt.split("T")[0].split("-")
            val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            val month = months.getOrElse(parts[1].toInt() - 1) { parts[1] }
            val day = parts[2].toIntOrNull() ?: parts[2]
            val year = parts[0]
            "$month $day, $year"
        } catch (e: Exception) {
            publishedAt.take(10)
        }
    }

    fun fetchAlbums(query: String = "") {
        viewModelScope.launch {
            _albumsState.value = ContentState.Loading
            try {
                val items: List<ArchiveItem> = InternetArchiveRepository.fetchGameSoundtracks(query)
                _albumsState.value = if (items.isEmpty())
                    ContentState.Error("No albums found")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _albumsState.value = ContentState.Error(e.message ?: "Failed to load albums")
            }
        }
    }

    fun fetchMagazines(query: String = "") {
        viewModelScope.launch {
            magazinesCurrentPage = 1
            magazinesCurrentQuery = query
            magazinesAllItems.clear()
            _hasMoreMagazines.value = true
            _magazinesState.value = ContentState.Loading
            try {
                val items: List<ArchiveItem> = InternetArchiveRepository.fetchRetroMagazines(
                    query = query, page = 1
                )
                magazinesAllItems.addAll(items)
                _hasMoreMagazines.value = items.size >= 20
                _magazinesState.value = if (items.isEmpty())
                    ContentState.Error("No magazines found")
                else
                    ContentState.Success(magazinesAllItems.toList())
            } catch (e: Exception) {
                _magazinesState.value = ContentState.Error(e.message ?: "Failed to load magazines")
            }
        }
    }

    fun loadMoreMagazines() {
        if (_isLoadingMoreMagazines.value || !_hasMoreMagazines.value) return
        viewModelScope.launch {
            _isLoadingMoreMagazines.value = true
            try {
                magazinesCurrentPage++
                val newItems: List<ArchiveItem> = InternetArchiveRepository.fetchRetroMagazines(
                    query = magazinesCurrentQuery, page = magazinesCurrentPage
                )
                if (newItems.isEmpty()) {
                    _hasMoreMagazines.value = false
                } else {
                    magazinesAllItems.addAll(newItems)
                    _hasMoreMagazines.value = newItems.size >= 20
                    _magazinesState.value = ContentState.Success(magazinesAllItems.toList())
                }
            } catch (e: Exception) { } finally {
                _isLoadingMoreMagazines.value = false
            }
        }
    }

    fun fetchArticles(query: String = "") {
        viewModelScope.launch {
            _articlesState.value = ContentState.Loading
            try {
                val items: List<ArchiveItem> = InternetArchiveRepository.fetchRetroArticles(query)
                _articlesState.value = if (items.isEmpty())
                    ContentState.Error("No articles found")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _articlesState.value = ContentState.Error(e.message ?: "Failed to load articles")
            }
        }
    }

    fun searchAll(query: String) {
        if (query.isBlank()) { _searchState.value = ContentState.Idle; return }
        viewModelScope.launch {
            _searchState.value = ContentState.Loading
            try {
                val items: List<ArchiveItem> = InternetArchiveRepository.searchAll(query)
                _searchState.value = if (items.isEmpty())
                    ContentState.Error("No results found for \"$query\"")
                else
                    ContentState.Success(items)
            } catch (e: Exception) {
                _searchState.value = ContentState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun resetSearch() { _searchState.value = ContentState.Idle }
}
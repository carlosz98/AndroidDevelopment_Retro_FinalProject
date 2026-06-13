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

    private val newsApiKey = "734d7d7185b54974b5c9756cec1634d2"

    init {
        fetchAlbums()
        fetchMagazines()
        fetchArticles()
        fetchLiveArticles()
    }

    fun fetchLiveArticles(topicOverride: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _liveArticlesState.value = LiveArticlesState.Loading
            try {
                val allFetched = mutableListOf<ArticleItem>()
                val client = OkHttpClient()

                val queries = if (topicOverride != null && topicOverride != "ALL") {
                    listOf(topicOverride)
                } else {
                    listOf("retro gaming", "video games", "arcade games")
                }

                for (query in queries) {
                    try {
                        val encodedQuery = query.replace(" ", "%20")
                        val url = "https://newsapi.org/v2/everything" +
                                "?q=$encodedQuery" +
                                "&language=en" +
                                "&pageSize=5" +
                                "&sortBy=publishedAt" +
                                "&apiKey=$newsApiKey"

                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val body = response.body?.string() ?: continue
                        val json = JSONObject(body)

                        if (json.optString("status") != "ok") continue

                        val articles = json.optJSONArray("articles") ?: continue

                        for (i in 0 until articles.length()) {
                            val article = articles.getJSONObject(i)
                            val title = article.optString("title", "")
                            if (title.isBlank() || title == "[Removed]") continue
                            val description = article.optString("description", "")
                            val content = article.optString("content", description)
                            val imageUrl = article.optString("urlToImage", "").ifBlank { null }
                            val sourceObj = article.optJSONObject("source")
                            val sourceName = sourceObj?.optString("name", "Gaming News") ?: "Gaming News"
                            val publishedAt = article.optString("publishedAt", "")
                            val articleUrl = article.optString("url", "").ifBlank { null }

                            val category = when {
                                query.contains("retro", ignoreCase = true) ||
                                        query.contains("arcade", ignoreCase = true) -> "RETRO"
                                query.contains("pixel", ignoreCase = true) -> "PIXEL ART"
                                query.contains("indie", ignoreCase = true) -> "GAMING"
                                else -> "GAMING"
                            }

                            // Clean truncated content
                            val cleanedContent = run {
                                val cleaned = content.ifBlank { description }
                                val cutIndex = cleaned.indexOf("[+")
                                if (cutIndex > 0) cleaned.substring(0, cutIndex).trim() else cleaned
                            }.ifBlank { description }

                            if (allFetched.none { it.title.equals(title, ignoreCase = true) }) {
                                allFetched.add(
                                    ArticleItem(
                                        id = "news_${title.hashCode()}",
                                        title = title,
                                        snippet = description.take(200).ifBlank { "Read more about $title" },
                                        fullContent = cleanedContent,
                                        date = formatGNewsDate(publishedAt),
                                        author = sourceName,
                                        authorUid = null,
                                        imageUrl = imageUrl,
                                        youtubeVideoId = null,
                                        viewCount = 0,
                                        category = category,
                                        reactions = emptyMap(),
                                        webUrl = articleUrl
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewsAPI", "Query $query failed: ${e.message}", e)
                    }
                }

                _liveArticlesState.value = if (allFetched.isEmpty()) {
                    LiveArticlesState.Error("No live articles found")
                } else {
                    LiveArticlesState.Success(allFetched.shuffled())
                }
            } catch (e: Exception) {
                android.util.Log.e("NewsAPI", "fetchLiveArticles failed: ${e.message}", e)
                _liveArticlesState.value = LiveArticlesState.Error(
                    e.message ?: "Failed to fetch live articles"
                )
            }
        }
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
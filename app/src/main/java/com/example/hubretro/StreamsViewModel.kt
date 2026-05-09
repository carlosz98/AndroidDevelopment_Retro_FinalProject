package com.example.hubretro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class TwitchStream(
    val id: String,
    val userId: String,
    val userName: String,
    val gameName: String,
    val title: String,
    val viewerCount: Int,
    val thumbnailUrl: String,
    val isLive: Boolean = true
)

data class YouTubeVideo(
    val id: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val viewCount: String = "",
    val videoUrl: String
)

data class CommunityStreamer(
    val uid: String,
    val username: String,
    val profilePicUrl: String,
    val twitchUsername: String,
    val youtubeUsername: String,
    val isLive: Boolean = false,
    val liveTitle: String = "",
    val viewerCount: Int = 0
)

sealed class StreamsState {
    object Loading : StreamsState()
    data class Success<T>(val data: List<T>) : StreamsState()
    data class Error(val message: String) : StreamsState()
    object Empty : StreamsState()
}

class StreamsViewModel : ViewModel() {

    companion object {
        private const val TWITCH_CLIENT_ID = "3u77qqm0pknkp8ceuya50ff8kvmna8"
        private const val TWITCH_CLIENT_SECRET = "p8zzy5hckfmvchpdz2eq174c7kv7y5"
        private const val YOUTUBE_API_KEY = "AIzaSyDEqbT2eB-iVVCJi8XL4qlcror2zzoi9pI"
        private const val RETRO_GAME_ID = "27284"
    }

    private val client = OkHttpClient()

    private val _twitchStreams = MutableStateFlow<StreamsState>(StreamsState.Loading)
    val twitchStreams: StateFlow<StreamsState> = _twitchStreams

    private val _youtubeVideos = MutableStateFlow<StreamsState>(StreamsState.Loading)
    val youtubeVideos: StateFlow<StreamsState> = _youtubeVideos

    private val _communityStreamers = MutableStateFlow<List<CommunityStreamer>>(emptyList())
    val communityStreamers: StateFlow<List<CommunityStreamer>> = _communityStreamers

    private var twitchToken: String = ""

    init {
        fetchAll()
    }

    fun fetchAll() {
        viewModelScope.launch(Dispatchers.IO) {
            getTwitchToken()
            launch { fetchTwitchStreams() }
            launch { fetchYouTubeVideos() }
        }
    }

    private suspend fun getTwitchToken() = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", TWITCH_CLIENT_ID)
                .add("client_secret", TWITCH_CLIENT_SECRET)
                .add("grant_type", "client_credentials")
                .build()
            val request = Request.Builder()
                .url("https://id.twitch.tv/oauth2/token")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext
            val json = JSONObject(responseBody)
            twitchToken = json.optString("access_token", "")
        } catch (e: Exception) {
            twitchToken = ""
        }
    }

    private suspend fun fetchTwitchStreams() = withContext(Dispatchers.IO) {
        _twitchStreams.value = StreamsState.Loading
        try {
            if (twitchToken.isBlank()) {
                _twitchStreams.value = StreamsState.Error("Could not connect to Twitch")
                return@withContext
            }
            val request = Request.Builder()
                .url("https://api.twitch.tv/helix/streams?game_id=$RETRO_GAME_ID&first=20")
                .addHeader("Client-ID", TWITCH_CLIENT_ID)
                .addHeader("Authorization", "Bearer $twitchToken")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _twitchStreams.value = StreamsState.Error("Twitch error: ${response.code}")
                return@withContext
            }
            val responseBody = response.body?.string() ?: run {
                _twitchStreams.value = StreamsState.Empty
                return@withContext
            }
            val json = JSONObject(responseBody)
            val dataArray = json.getJSONArray("data")
            val streams = mutableListOf<TwitchStream>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val thumb = item.optString("thumbnail_url", "")
                    .replace("{width}", "320")
                    .replace("{height}", "180")
                streams.add(
                    TwitchStream(
                        id = item.optString("id"),
                        userId = item.optString("user_id"),
                        userName = item.optString("user_name"),
                        gameName = item.optString("game_name", "Retro Gaming"),
                        title = item.optString("title"),
                        viewerCount = item.optInt("viewer_count", 0),
                        thumbnailUrl = thumb
                    )
                )
            }
            _twitchStreams.value = if (streams.isEmpty()) StreamsState.Empty
            else StreamsState.Success(streams)
        } catch (e: Exception) {
            _twitchStreams.value = StreamsState.Error("Failed to load streams: ${e.message}")
        }
    }

    private suspend fun fetchYouTubeVideos() = withContext(Dispatchers.IO) {
        _youtubeVideos.value = StreamsState.Loading
        try {
            val query = "retro+gaming+nostalgia+classic+games"
            val url = "https://www.googleapis.com/youtube/v3/search" +
                    "?part=snippet" +
                    "&q=$query" +
                    "&type=video" +
                    "&videoCategoryId=20" +
                    "&order=viewCount" +
                    "&maxResults=20" +
                    "&key=$YOUTUBE_API_KEY"
            val request = Request.Builder()
                .url(url)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _youtubeVideos.value = StreamsState.Error("YouTube error: ${response.code}")
                return@withContext
            }
            val responseBody = response.body?.string() ?: run {
                _youtubeVideos.value = StreamsState.Empty
                return@withContext
            }
            val json = JSONObject(responseBody)

            // Check for API errors
            if (json.has("error")) {
                val errorMsg = json.getJSONObject("error").optString("message", "API error")
                _youtubeVideos.value = StreamsState.Error(errorMsg)
                return@withContext
            }

            val items = json.optJSONArray("items") ?: run {
                _youtubeVideos.value = StreamsState.Empty
                return@withContext
            }
            val videos = mutableListOf<YouTubeVideo>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val idObj = item.optJSONObject("id") ?: continue
                val videoId = idObj.optString("videoId", "")
                if (videoId.isBlank()) continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val thumbs = snippet.optJSONObject("thumbnails")
                val thumb = when {
                    thumbs?.has("high") == true ->
                        thumbs.getJSONObject("high").optString("url", "")
                    thumbs?.has("medium") == true ->
                        thumbs.getJSONObject("medium").optString("url", "")
                    thumbs?.has("default") == true ->
                        thumbs.getJSONObject("default").optString("url", "")
                    else -> ""
                }
                videos.add(
                    YouTubeVideo(
                        id = videoId,
                        title = snippet.optString("title", ""),
                        channelTitle = snippet.optString("channelTitle", ""),
                        thumbnailUrl = thumb,
                        publishedAt = snippet.optString("publishedAt", "").take(10),
                        videoUrl = "https://www.youtube.com/watch?v=$videoId"
                    )
                )
            }
            _youtubeVideos.value = if (videos.isEmpty()) StreamsState.Empty
            else StreamsState.Success(videos)
        } catch (e: Exception) {
            _youtubeVideos.value = StreamsState.Error("Failed to load videos: ${e.message}")
        }
    }

    fun loadCommunityStreamers(users: List<UserProfileData>) {
        val streamers = users.filter {
            it.twitchUsername.isNotBlank() || it.youtubeUsername.isNotBlank()
        }.map { user ->
            CommunityStreamer(
                uid = user.uid,
                username = user.username,
                profilePicUrl = user.profilePictureUrl,
                twitchUsername = user.twitchUsername,
                youtubeUsername = user.youtubeUsername
            )
        }
        _communityStreamers.value = streamers
    }
}
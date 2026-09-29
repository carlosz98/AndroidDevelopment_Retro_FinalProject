package com.example.hubretro

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.SimpleDateFormat
import java.util.*

// ─── Data Models ──────────────────────────────────────────────────────────────

data class Checkpoint(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorHandle: String = "",
    val authorPicUrl: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val gifUrl: String = "",
    val songTitle: String = "",
    val songArtist: String = "",
    val songPreviewUrl: String = "",
    val songArtworkUrl: String = "",
    val reactions: Map<String, Int> = emptyMap(),
    val userReactions: Map<String, String> = emptyMap(),
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val witnessCount: Int = 0,
    val witnessedBy: List<String> = emptyList(),
    val gameTags: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val viewCount: Int = 0
)

data class GifResult(
    val id: String = "",
    val title: String = "",
    val previewUrl: String = "",    // downsized gif for grid preview
    val originalUrl: String = ""    // full gif for the post
)

data class MusicSearchResult(
    val trackId: Long = 0L,
    val trackName: String = "",
    val artistName: String = "",
    val artworkUrl: String = "",
    val previewUrl: String = "",
    val collectionName: String = ""
)

data class CheckpointComment(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorPicUrl: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class EmojiParticle(val id: Long, val emoji: String, val xOffset: Float)

data class ConfettiParticleData(
    val startX: Float, val startY: Float,
    val vx: Float, val vy: Float,
    val color: Color, val size: Float,
    val rotationSpeed: Float, val shape: Int
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class CheckpointViewModel : ViewModel() {
    private val db      = FirebaseFirestore.getInstance()
    private val auth    = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _checkpoints = MutableStateFlow<List<Checkpoint>>(emptyList())
    val checkpoints: StateFlow<List<Checkpoint>> = _checkpoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var hasSeeded = false

    init {
        fetchCheckpoints()
        seedExampleIfNeeded()
    }

    fun fetchCheckpoints() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docs = db.collection("checkpoints")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50).get().await()
                _checkpoints.value = docs.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    Checkpoint(
                        id             = doc.id,
                        authorUid      = d["authorUid"] as? String ?: "",
                        authorUsername = d["authorUsername"] as? String ?: "Anonymous",
                        authorHandle   = d["authorHandle"] as? String ?: "",
                        authorPicUrl   = d["authorPicUrl"] as? String ?: "",
                        title          = d["title"] as? String ?: "",
                        body           = d["body"] as? String ?: "",
                        imageUrl       = d["imageUrl"] as? String ?: "",
                        gifUrl         = d["gifUrl"] as? String ?: "",
                        songTitle      = d["songTitle"] as? String ?: "",
                        songArtist     = d["songArtist"] as? String ?: "",
                        songPreviewUrl = d["songPreviewUrl"] as? String ?: "",
                        songArtworkUrl = d["songArtworkUrl"] as? String ?: "",
                        reactions      = (d["reactions"] as? Map<String, Long>)
                            ?.mapValues { it.value.toInt() } ?: emptyMap(),
                        userReactions  = (d["userReactions"] as? Map<String, String>) ?: emptyMap(),
                        commentCount   = (d["commentCount"] as? Long)?.toInt() ?: 0,
                        likeCount      = (d["likeCount"] as? Long)?.toInt() ?: 0,
                        likedBy        = (d["likedBy"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        witnessCount   = (d["witnessCount"] as? Long)?.toInt() ?: 0,
                        witnessedBy    = (d["witnessedBy"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        gameTags       = (d["gameTags"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                        timestamp      = (d["timestamp"] as? com.google.firebase.Timestamp)
                            ?.toDate()?.time ?: d["timestamp"] as? Long ?: 0L,
                        viewCount      = (d["viewCount"] as? Long)?.toInt() ?: 0
                    )
                }
            } catch (ignored: Exception) { }
            _isLoading.value = false
        }
    }

    private fun seedExampleIfNeeded() {
        if (hasSeeded) return
        hasSeeded = true
        viewModelScope.launch {
            try {
                val existing = db.collection("checkpoints").limit(3).get().await()
                // Patch any demo doc that's missing a gifUrl so GIF display can be tested
                existing.documents.filter { it.getString("authorUid") == "retrohub_demo" }
                    .forEach { doc ->
                        if (doc.getString("gifUrl").isNullOrBlank()) {
                            doc.reference.update(
                                "gifUrl", "https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif",
                                "imageUrl", ""
                            ).await()
                        }
                    }
                if (!existing.isEmpty) return@launch
                val docRef = db.collection("checkpoints").document()
                docRef.set(mapOf(
                    "id"             to docRef.id,
                    "authorUid"      to "retrohub_demo",
                    "authorUsername" to "RetroHub",
                    "authorHandle"   to "@retrohub",
                    "authorPicUrl"   to "",
                    "title"          to "First Time Beating GoldenEye 007",
                    "body"           to "Never thought this day would come. After weeks grinding the Facility on Secret Agent difficulty, I finally did it. The moment those credits rolled I just sat there staring at the screen. This game was my entire childhood — summer afternoons at a friend's house, split-screen battles that turned into actual arguments. If you know, you know. A true milestone.",
                    "imageUrl"       to "",
                    "gifUrl"         to "https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif",
                    "youtubeUrl"     to "",
                    "songTitle"      to "GoldenEye",
                    "songArtist"     to "Tina Turner",
                    "songPreviewUrl" to "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview125/v4/92/08/a7/9208a76f-6d69-73cf-80fb-b91e91cce34b/mzaf_4791044749595009551.plus.aac.p.m4a",
                    "songArtworkUrl" to "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/e7/8b/37/e78b37d0-90aa-2d40-6b68-ca6a91d2c8f3/cover.jpg/300x300bb.jpg",
                    "reactions"      to mapOf("🔥" to 14, "🏆" to 9, "😍" to 6, "💯" to 4),
                    "userReactions"  to emptyMap<String, String>(),
                    "commentCount"   to 5,
                    "likeCount"      to 14,
                    "likedBy"        to emptyList<String>(),
                    "witnessCount"   to 7,
                    "witnessedBy"    to emptyList<String>(),
                    "gameTags"       to listOf("N64", "GoldenEye 007", "FPS"),
                    "timestamp"      to FieldValue.serverTimestamp()
                )).await()
                fetchCheckpoints()
            } catch (ignored: Exception) { }
        }
    }

    fun postCheckpoint(
        title: String, body: String, imageUri: Uri?,
        gifUrl: String, selectedMusic: MusicSearchResult?,
        userProfile: UserProfileData?,
        onDone: () -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val safeCp = Moderation.gateAll(title, body, where = "checkpoint") ?: return
        val title = safeCp[0]
        val body = safeCp[1]
        viewModelScope.launch {
            try {
                var finalImageUrl = ""
                if (imageUri != null) {
                    val ref = storage.reference.child("checkpoints/$uid/${System.currentTimeMillis()}.jpg")
                    ref.putFile(imageUri).await()
                    finalImageUrl = ref.downloadUrl.await().toString()
                }
                val docRef = db.collection("checkpoints").document()
                docRef.set(mapOf(
                    "id"             to docRef.id,
                    "authorUid"      to uid,
                    "authorUsername" to (userProfile?.username ?: "Anonymous"),
                    "authorHandle"   to (userProfile?.userHandle ?: ""),
                    "authorPicUrl"   to (userProfile?.profilePictureUrl ?: ""),
                    "title"          to title.trim(),
                    "body"           to body.trim(),
                    "imageUrl"       to finalImageUrl,
                    "gifUrl"         to gifUrl.trim(),
                    "songTitle"      to (selectedMusic?.trackName ?: ""),
                    "songArtist"     to (selectedMusic?.artistName ?: ""),
                    "songPreviewUrl" to (selectedMusic?.previewUrl ?: ""),
                    "songArtworkUrl" to (selectedMusic?.artworkUrl ?: ""),
                    "reactions"      to emptyMap<String, Int>(),
                    "userReactions"  to emptyMap<String, String>(),
                    "commentCount"   to 0,
                    "likeCount"      to 0,
                    "likedBy"        to emptyList<String>(),
                    "witnessCount"   to 0,
                    "witnessedBy"    to emptyList<String>(),
                    "gameTags"       to emptyList<String>(),
                    "timestamp"      to FieldValue.serverTimestamp()
                )).await()
                // Update streak
                updateStreak(uid)
                fetchCheckpoints()
                onDone()
            } catch (ignored: Exception) { onDone() }
        }
    }

    fun searchMusic(query: String, onResult: (List<MusicSearchResult>) -> Unit) {
        if (query.isBlank()) { onResult(emptyList()); return }
        viewModelScope.launch {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    java.net.URL("https://itunes.apple.com/search?term=$encodedQuery&media=music&limit=12&entity=song")
                        .openConnection().apply {
                            connectTimeout = 8000
                            readTimeout = 8000
                        }.getInputStream().bufferedReader().readText()
                }
                val json = org.json.JSONObject(response)
                val results = json.getJSONArray("results")
                val list = mutableListOf<MusicSearchResult>()
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val preview = obj.optString("previewUrl", "")
                    if (preview.isBlank()) continue
                    list.add(MusicSearchResult(
                        trackId        = obj.optLong("trackId", 0L),
                        trackName      = obj.optString("trackName", ""),
                        artistName     = obj.optString("artistName", ""),
                        artworkUrl     = obj.optString("artworkUrl100", "").replace("100x100bb", "300x300bb"),
                        previewUrl     = preview,
                        collectionName = obj.optString("collectionName", "")
                    ))
                }
                onResult(list)
            } catch (e: Exception) { android.util.Log.e("CheckpointVM", "searchMusic error", e); onResult(emptyList()) }
        }
    }

    // GIF search state exposed to the UI
    var gifResults    = androidx.compose.runtime.mutableStateOf<List<GifResult>>(emptyList())
    var gifSearching  = androidx.compose.runtime.mutableStateOf(false)
    var gifError      = androidx.compose.runtime.mutableStateOf("")

    // DNS-over-HTTPS via Cloudflare 1.1.1.1 — bypasses network-level DNS blocks on api.giphy.com
    private val httpClient: okhttp3.OkHttpClient = run {
        val bootstrap = okhttp3.OkHttpClient()
        val doh = okhttp3.dnsoverhttps.DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                java.net.InetAddress.getByName("1.1.1.1"),
                java.net.InetAddress.getByName("1.0.0.1")
            )
            .build()
        okhttp3.OkHttpClient.Builder()
            .dns(doh)
            .connectTimeout(14, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(14, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // ── Giphy API key — "Retro Games" app on developers.giphy.com
    private val GIPHY_API_KEY = "cqqt9V8jWLMgC9VHDyjWGMpMYBvpU92m"

    fun searchGifs(query: String) {
        if (query.isBlank()) { gifResults.value = emptyList(); return }
        gifSearching.value = true
        gifError.value = ""
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                // Giphy API v1 — DNS resolved via Cloudflare DoH to bypass local network blocks
                val url = "https://api.giphy.com/v1/gifs/search" +
                    "?api_key=$GIPHY_API_KEY" +
                    "&q=$encodedQuery" +
                    "&limit=24" +
                    "&rating=g" +
                    "&lang=en"
                val request = okhttp3.Request.Builder().url(url)
                    .addHeader("User-Agent", "RetroHub/1.0")
                    .get().build()
                val body = httpClient.newCall(request).execute().use { resp ->
                    android.util.Log.d("GifSearch", "HTTP ${resp.code} for '$query'")
                    resp.body?.string() ?: ""
                }
                android.util.Log.d("GifSearch", "Body preview: ${body.take(300)}")
                val json   = org.json.JSONObject(body)
                val data   = json.optJSONArray("data")
                if (data == null) {
                    val meta = json.optJSONObject("meta")
                    val msg  = meta?.optString("msg") ?: body.take(120)
                    android.util.Log.e("GifSearch", "No data array: $msg")
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        gifError.value = msg; gifResults.value = emptyList(); gifSearching.value = false
                    }
                    return@launch
                }
                val list = mutableListOf<GifResult>()
                for (i in 0 until data.length()) {
                    val obj      = data.getJSONObject(i)
                    val images   = obj.optJSONObject("images") ?: continue
                    // preview: fixed_width_small (usually ~200px, animated)
                    val preview  = images.optJSONObject("fixed_width_small")?.optString("url", "")?.takeIf { it.isNotBlank() }
                        ?: images.optJSONObject("fixed_width")?.optString("url", "")?.takeIf { it.isNotBlank() }
                        ?: continue
                    val full     = images.optJSONObject("original")?.optString("url", "")?.takeIf { it.isNotBlank() }
                        ?: preview
                    list.add(GifResult(
                        id          = obj.optString("id", ""),
                        title       = obj.optString("title", ""),
                        previewUrl  = preview,
                        originalUrl = full
                    ))
                }
                android.util.Log.d("GifSearch", "Parsed ${list.size} GIFs for '$query'")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    gifResults.value = list; gifSearching.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("GifSearch", "Exception: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    gifError.value = e.message ?: "Network error"
                    gifResults.value = emptyList(); gifSearching.value = false
                }
            }
        }
    }

    fun toggleReaction(checkpointId: String, emoji: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val ref = db.collection("checkpoints").document(checkpointId)
                val snap = ref.get().await()
                val userReactions = (snap["userReactions"] as? Map<*, *>)
                    ?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() }
                    ?.toMutableMap() ?: mutableMapOf()
                val reactions = (snap["reactions"] as? Map<*, *>)
                    ?.mapKeys { it.key.toString() }
                    ?.mapValues { (it.value as? Long)?.toInt() ?: 0 }
                    ?.toMutableMap() ?: mutableMapOf()
                val prev = userReactions[uid]
                if (prev != null) {
                    reactions[prev] = maxOf(0, (reactions[prev] ?: 1) - 1)
                    userReactions.remove(uid)
                }
                if (prev != emoji) {
                    reactions[emoji] = (reactions[emoji] ?: 0) + 1
                    userReactions[uid] = emoji
                }
                ref.update(mapOf("reactions" to reactions, "userReactions" to userReactions)).await()
                _checkpoints.value = _checkpoints.value.map { cp ->
                    if (cp.id == checkpointId) cp.copy(reactions = reactions, userReactions = userReactions)
                    else cp
                }
            } catch (ignored: Exception) { }
        }
    }

    fun toggleLike(checkpointId: String, authorUid: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val ref = db.collection("checkpoints").document(checkpointId)
                val snap = ref.get().await()
                val likedBy = (snap["likedBy"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val hasLiked = uid in likedBy
                if (hasLiked) {
                    ref.update(
                        "likedBy", FieldValue.arrayRemove(uid),
                        "likeCount", FieldValue.increment(-1)
                    ).await()
                    _checkpoints.value = _checkpoints.value.map { cp ->
                        if (cp.id == checkpointId) cp.copy(
                            likedBy = cp.likedBy - uid,
                            likeCount = maxOf(0, cp.likeCount - 1)
                        ) else cp
                    }
                } else {
                    ref.update(
                        "likedBy", FieldValue.arrayUnion(uid),
                        "likeCount", FieldValue.increment(1)
                    ).await()
                    _checkpoints.value = _checkpoints.value.map { cp ->
                        if (cp.id == checkpointId) cp.copy(
                            likedBy = cp.likedBy + uid,
                            likeCount = cp.likeCount + 1
                        ) else cp
                    }
                    // Notify author (skip self-likes)
                    if (authorUid.isNotBlank() && authorUid != uid) {
                        val me = db.collection("users").document(uid).get().await()
                        sendNotification(
                            toUid = authorUid,
                            type = "like",
                            fromUid = uid,
                            fromUsername = me.getString("username") ?: "Someone",
                            fromPicUrl = me.getString("profilePictureUrl") ?: "",
                            targetId = checkpointId
                        )
                    }
                }
            } catch (ignored: Exception) { }
        }
    }

    fun toggleWitness(checkpointId: String, authorUid: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val ref = db.collection("checkpoints").document(checkpointId)
                val snap = ref.get().await()
                val witnessedBy = (snap["witnessedBy"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val hasWitnessed = uid in witnessedBy
                if (hasWitnessed) {
                    ref.update(
                        "witnessedBy", FieldValue.arrayRemove(uid),
                        "witnessCount", FieldValue.increment(-1)
                    ).await()
                    _checkpoints.value = _checkpoints.value.map { cp ->
                        if (cp.id == checkpointId) cp.copy(
                            witnessedBy = cp.witnessedBy - uid,
                            witnessCount = maxOf(0, cp.witnessCount - 1)
                        ) else cp
                    }
                } else {
                    ref.update(
                        "witnessedBy", FieldValue.arrayUnion(uid),
                        "witnessCount", FieldValue.increment(1)
                    ).await()
                    _checkpoints.value = _checkpoints.value.map { cp ->
                        if (cp.id == checkpointId) cp.copy(
                            witnessedBy = cp.witnessedBy + uid,
                            witnessCount = cp.witnessCount + 1
                        ) else cp
                    }
                    if (authorUid.isNotBlank() && authorUid != uid) {
                        val me = db.collection("users").document(uid).get().await()
                        sendNotification(
                            toUid = authorUid,
                            type = "witness",
                            fromUid = uid,
                            fromUsername = me.getString("username") ?: "Someone",
                            fromPicUrl = me.getString("profilePictureUrl") ?: "",
                            targetId = checkpointId
                        )
                    }
                }
            } catch (ignored: Exception) { }
        }
    }

    fun incrementViewCount(checkpointId: String) {
        viewModelScope.launch {
            try {
                db.collection("checkpoints").document(checkpointId)
                    .update("viewCount", FieldValue.increment(1)).await()
                _checkpoints.value = _checkpoints.value.map { cp ->
                    if (cp.id == checkpointId) cp.copy(viewCount = cp.viewCount + 1) else cp
                }
            } catch (ignored: Exception) { }
        }
    }

    private suspend fun updateStreak(uid: String) {
        try {
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val today = java.util.Calendar.getInstance().let {
                it.set(java.util.Calendar.HOUR_OF_DAY, 0)
                it.set(java.util.Calendar.MINUTE, 0)
                it.set(java.util.Calendar.SECOND, 0)
                it.set(java.util.Calendar.MILLISECOND, 0)
                it.timeInMillis
            }
            val lastDate = snap.getLong("lastCheckpointDate") ?: 0L
            val oneDayMs = 86_400_000L
            val currentStreak = snap.getLong("currentStreak")?.toInt() ?: 0
            val maxStreak = snap.getLong("maxStreak")?.toInt() ?: 0
            val newStreak = when {
                lastDate == today -> currentStreak // already posted today
                lastDate == today - oneDayMs -> currentStreak + 1 // consecutive day
                else -> 1 // streak broken
            }
            userRef.update(mapOf(
                "lastCheckpointDate" to today,
                "currentStreak" to newStreak,
                "maxStreak" to maxOf(maxStreak, newStreak),
                "totalCheckpoints" to FieldValue.increment(1)
            )).await()
        } catch (ignored: Exception) { }
    }

    fun deleteCheckpoint(checkpointId: String) {
        viewModelScope.launch {
            try {
                db.collection("checkpoints").document(checkpointId).delete().await()
                _checkpoints.value = _checkpoints.value.filter { it.id != checkpointId }
            } catch (ignored: Exception) { }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fun extractYouTubeId(url: String): String? {
    val patterns = listOf(
        Regex("""(?:v=|youtu\.be/)([a-zA-Z0-9_-]{11})"""),
        Regex("""(?:embed/)([a-zA-Z0-9_-]{11})""")
    )
    return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }
}

fun youtubeThumbnailUrl(videoId: String) =
    "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

fun generateConfettiParticles(width: Float): List<ConfettiParticleData> {
    val colors = listOf(
        CGreen, Color(0xFFE63946), CGreen,
        CAcBlue, CGreen, Color(0xFFFFFFFF),
        Color(0xFF7B2FBE), CGreenMint, CGreenMint
    )
    val rng = Random()
    return (0 until 70).map { i ->
        ConfettiParticleData(
            startX        = rng.nextFloat() * width,
            startY        = -rng.nextFloat() * 200f - 20f,
            vx            = (rng.nextFloat() - 0.5f) * 3f,
            vy            = rng.nextFloat() * 2f + 1.2f,
            color         = colors[i % colors.size],
            size          = rng.nextFloat() * 10f + 8f,
            rotationSpeed = (rng.nextFloat() - 0.5f) * 8f,
            shape         = i % 3
        )
    }
}

// ─── CheckpointScreen ─────────────────────────────────────────────────────────

@Composable
fun CheckpointScreen(
    onBack: () -> Unit = {},
    viewModel: CheckpointViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val checkpoints    by viewModel.checkpoints.collectAsState()
    val isLoading      by viewModel.isLoading.collectAsState()
    val userProfile    by authViewModel.userProfile.collectAsState()
    val currentUid     = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var showCreateSheet by remember { mutableStateOf(false) }
    var showCommentsFor by remember { mutableStateOf<Checkpoint?>(null) }
    var showConfetti    by remember { mutableStateOf(false) }
    var showSaving      by remember { mutableStateOf(false) }
    var filterTab       by remember { mutableStateOf("ALL") }

    // Live new-post banner
    var newPostBanner  by remember { mutableStateOf<Checkpoint?>(null) }
    var bannerVisible  by remember { mutableStateOf(false) }

    val filteredCheckpoints = remember(checkpoints, filterTab, currentUid) {
        when (filterTab) {
            "TRENDING" -> checkpoints.sortedByDescending { cp -> cp.reactions.values.sum() }
            "MINE"     -> checkpoints.filter { it.authorUid == currentUid }
            else       -> checkpoints
        }
    }

    // Real-time listener for new checkpoints from others — powers the live pixel banner
    DisposableEffect(currentUid) {
        val db = FirebaseFirestore.getInstance()
        val seenIds = mutableSetOf<String>()
        var initialized = false
        val listener = db.collection("checkpoints")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                if (!initialized) {
                    seenIds.addAll(snap.documents.map { it.id })
                    initialized = true
                    return@addSnapshotListener
                }
                snap.documentChanges.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val docId = change.document.id
                        if (docId !in seenIds) {
                            val authorUid = change.document.getString("authorUid") ?: ""
                            if (authorUid != currentUid && authorUid != "retrohub_demo" && authorUid.isNotBlank()) {
                                newPostBanner = Checkpoint(
                                    id             = docId,
                                    authorUsername = change.document.getString("authorUsername") ?: "Someone",
                                    authorHandle   = change.document.getString("authorHandle") ?: ""
                                )
                                bannerVisible = true
                            }
                            seenIds.add(docId)
                        }
                    }
                }
            }
        onDispose { listener.remove() }
    }

    // Auto-dismiss banner after 4 seconds
    LaunchedEffect(newPostBanner) {
        if (newPostBanner != null) {
            delay(4000)
            bannerVisible = false
            delay(450)
            newPostBanner = null
        }
    }

    // Header shimmer
    val shimmerT = rememberInfiniteTransition(label = "cpShimmer")
    val shimmerX by shimmerT.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "cpShimmerX"
    )

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header (Discover style) ───────────────────────────────────────
            ComicPageHeader(
                title = "CHECKPOINTS",
                subtitle = "milestones • memories • moments",
                onBack = onBack,
                extra = {
                    if (checkpoints.isNotEmpty()) {
                        val totalReactions = remember(checkpoints) { checkpoints.sumOf { it.reactions.values.sum() } }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("${checkpoints.size} posts", "$totalReactions reactions").forEach { label ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)) {
                                    Text(label, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            ) {
                ComicHeaderChip("▶ POST") { showCreateSheet = true }
            }

            // Marquee ticker strip
            CheckpointMarqueeStrip()

            // Filter tabs
            FilterTabRow(selectedTab = filterTab, onTabSelected = { filterTab = it })

            // ── Feed ────────────────────────────────────────────────────────
            when {
                isLoading -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(4) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(320.dp), cornerRadius = 20.dp) }
                }

                filteredCheckpoints.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    // Retro pixel empty state
                    val blinkT = rememberInfiniteTransition(label = "blink")
                    val blinkA by blinkT.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "bl")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Pixel game over style frame
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.92f))
                                .border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                                .padding(horizontal = 28.dp, vertical = 22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (filterTab == "MINE") "👤" else "🏁", fontSize = 48.sp)
                                Text(
                                    if (filterTab == "MINE") "NO SAVES FOUND" else "NO DATA",
                                    fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 24.sp, letterSpacing = 2.sp
                                )
                                HorizontalDivider(color = CGreen.copy(alpha = 0.2f), modifier = Modifier.width(140.dp))
                                Text(
                                    if (filterTab == "MINE") "Post your first milestone to see it here."
                                    else "Be the first to share a milestone, memory, or moment.",
                                    fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                if (filterTab != "MINE") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "▸ INSERT COIN TO CONTINUE ◂",
                                        fontFamily = BangersFontFamily,
                                        color = CGreen.copy(alpha = blinkA),
                                        fontSize = 13.sp, letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                        if (filterTab != "MINE") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.wrapContentSize()) {
                                Box(
                                    modifier = Modifier.matchParentSize()
                                        .offset(x = 4.dp, y = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ScrapbookDark.copy(alpha = 0.18f))
                                )
                                Box(modifier = Modifier.wrapContentSize()) {
                                    Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.18f)))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Brush.linearGradient(listOf(CGreen, CGreenMint)))
                                            .border(2.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .clickable { showCreateSheet = true }
                                            .padding(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("▶", color = ScrapbookDark, fontSize = 14.sp)
                                            Text("POST CHECKPOINT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                                        }
                                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Welcome banner ──────────────────────────────────────
                    item(key = "welcome") { CheckpointWelcomeBanner(currentUser = userProfile) }

                    itemsIndexed(filteredCheckpoints, key = { _, cp -> cp.id }) { index, cp ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(cp.id) {
                            delay(minOf(index, 7) * 75L)
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInVertically(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                initialOffsetY = { it / 2 }
                            ) + fadeIn(tween(280))
                        ) {
                            SpringEntrance(delayMs = minOf(index, 7) * 60) {
                                CheckpointCard(
                                    checkpoint           = cp,
                                    currentUid           = currentUid,
                                    currentUserProfile   = userProfile,
                                    onReaction           = { emoji -> viewModel.toggleReaction(cp.id, emoji) },
                                    onComments           = { showCommentsFor = cp },
                                    onLike               = { viewModel.toggleLike(cp.id, cp.authorUid) },
                                    onWitness            = { viewModel.toggleWitness(cp.id, cp.authorUid) },
                                    onViewExpand         = { viewModel.incrementViewCount(cp.id) },
                                    onDelete             = if (cp.authorUid == currentUid) ({ viewModel.deleteCheckpoint(cp.id) }) else null
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        // ── Overlays ────────────────────────────────────────────────────────
        if (showCreateSheet) {
            CheckpointCreateSheet(
                userProfile = userProfile,
                viewModel   = viewModel,
                onDismiss   = {
                    showCreateSheet = false
                    showSaving = true
                }
            )
        }

        showCommentsFor?.let { cp ->
            CheckpointCommentsSheet(
                checkpoint         = cp,
                currentUid         = currentUid,
                currentUserProfile = userProfile,
                onDismiss          = { showCommentsFor = null; viewModel.fetchCheckpoints() }
            )
        }

        // PS1-style SAVING overlay — appears before confetti
        if (showSaving) {
            RetroSaveOverlay(onFinished = { showSaving = false; showConfetti = true })
        }

        // Confetti celebration overlay
        if (showConfetti) {
            ConfettiOverlay(onFinished = { showConfetti = false })
        }

        // Live new-post pixel banner — slides in from the top
        AnimatedVisibility(
            visible = bannerVisible,
            enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(260)),
            exit  = slideOutVertically(tween(320)) { -it } + fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
        ) {
            val banner = newPostBanner
            if (banner != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComicGlassBg)
                        .border(1.5.dp, CGreen.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { bannerVisible = false }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🏁", fontSize = 16.sp)
                        Column {
                            Text(
                                banner.authorUsername,
                                fontFamily = BangersFontFamily, color = CGreen,
                                fontSize = 13.sp, letterSpacing = 0.5.sp
                            )
                            Text(
                                "just logged a checkpoint",
                                fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.60f),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CGreen.copy(alpha = 0.15f))
                                .border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text("NEW ▶", fontFamily = BangersFontFamily, color = CGreen, fontSize = 9.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Checkpoint Marquee Strip ─────────────────────────────────────────────────

@Composable
fun CheckpointMarqueeStrip() {
    var chunkWidthPx by remember { mutableStateOf(0f) }
    val scrollOffset = remember { Animatable(0f) }

    LaunchedEffect(chunkWidthPx) {
        if (chunkWidthPx <= 0f) return@LaunchedEffect
        while (true) {
            scrollOffset.snapTo(0f)
            scrollOffset.animateTo(
                targetValue = -chunkWidthPx,
                animationSpec = tween(
                    durationMillis = (chunkWidthPx / 120f * 1000f).toInt().coerceIn(4000, 14000),
                    easing = LinearEasing
                )
            )
        }
    }

    val marqueeText = "  🏁 CHECKPOINTS  •  MILESTONES  •  MOMENTS  •  MEMORIES  •  YOUR STORY  •  "

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(CGreen)
            .clipToBounds()
    ) {
        // Diagonal racing stripes
        Canvas(modifier = Modifier.matchParentSize()) {
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = ScrapbookDark.copy(alpha = 0.16f),
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = 16f
                )
                x += 32f
            }
        }

        // Scrolling text
        Row(
            modifier = Modifier
                .offset { IntOffset(scrollOffset.value.toInt(), 0) }
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(10) { index ->
                Text(
                    text = marqueeText,
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    modifier = if (index == 0) {
                        Modifier.onSizeChanged { s ->
                            if (chunkWidthPx == 0f) chunkWidthPx = s.width.toFloat()
                        }
                    } else Modifier
                )
            }
        }
    }
}

// ─── Filter Tab Row ───────────────────────────────────────────────────────────

@Composable
fun FilterTabRow(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("ALL", "TRENDING", "MINE")
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val pillPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "filterPill"
    )

    var rowWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(ComicGlassBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.6f))
                .border(2.dp, ScrapbookDark, RoundedCornerShape(24.dp))
                .onSizeChanged { rowWidthPx = it.width }
        ) {
            val tabWidthDp = with(density) { (rowWidthPx / tabs.size.coerceAtLeast(1)).toDp() }

            // Sliding pill
            Box(
                modifier = Modifier
                    .offset(x = tabWidthDp * pillPosition)
                    .width(tabWidthDp)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(CGreen, CGreenMint)))
            )

            // Labels
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            when (tab) {
                                "TRENDING" -> Text("🔥", fontSize = 11.sp)
                                "MINE"     -> Text("👤", fontSize = 11.sp)
                            }
                            Text(
                                tab,
                                fontFamily = BangersFontFamily,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Welcome Banner ───────────────────────────────────────────────────────────

@Composable
fun CheckpointWelcomeBanner(currentUser: UserProfileData? = null) {
    val glow by rememberGlowRange(0.5f, 1f)

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── PSX / PS1 Low-Poly Hero ───────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(ScrapbookDark.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
            // Low-poly triangulated background (PS1 style)
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val palette = listOf(
                    ComicGlassBg, Color(0xFFDDF0E4), Color(0xFFC8E8D4),
                    ComicGlassBg, Color(0xFFB7E0C6), Color(0xFFE6F4EA),
                    Color(0xFFD2ECDC), ComicGlassBg, Color(0xFFA8D9BA),
                    Color(0xFFE0F2E7), Color(0xFFC0E4CD), ComicGlassBg,
                    Color(0xFFD8EFE0), Color(0xFFB0DCC0), ComicGlassBg
                )
                val cols = 7; val rows = 6
                val cw = w / cols; val ch = h / rows
                // Build vertex grid with jitter for low-poly look
                val verts = Array(rows + 1) { r ->
                    Array(cols + 1) { c ->
                        val jx = if (c in 1 until cols) ((r * 17 + c * 31) % 24 - 12) * (cw / 40f) else 0f
                        val jy = if (r in 1 until rows) ((r * 13 + c * 23) % 24 - 12) * (ch / 40f) else 0f
                        Offset(c * cw + jx, r * ch + jy)
                    }
                }
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val tl = verts[r][c]; val tr = verts[r][c + 1]
                        val bl = verts[r + 1][c]; val br = verts[r + 1][c + 1]
                        val i1 = (r * 3 + c * 7) % palette.size
                        val i2 = (r * 5 + c * 11 + 3) % palette.size
                        drawPath(Path().apply { moveTo(tl.x, tl.y); lineTo(tr.x, tr.y); lineTo(bl.x, bl.y); close() }, palette[i1])
                        drawPath(Path().apply { moveTo(tr.x, tr.y); lineTo(br.x, br.y); lineTo(bl.x, bl.y); close() }, palette[i2])
                    }
                }
                // Scanlines (PS1 CRT)
                var y = 0f
                while (y < h) { drawLine(Color.Black.copy(alpha = 0.04f), Offset(0f, y), Offset(w, y), 1.5f); y += 3f }
                // Soft light wash for text legibility
                drawRect(brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.25f))), size = size)
            }

            // Centered text overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Pixel dot bar
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (i in 0..4) Box(modifier = Modifier.size(4.dp).background(CGreen.copy(alpha = 0.2f + i * 0.16f)))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("THIS IS THE", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 15.sp, letterSpacing = 5.sp)
                Text(
                    "CHECKPOINT",
                    fontFamily = BangersFontFamily,
                    style = TextStyle(brush = Brush.linearGradient(listOf(CGreenDeep, ScrapbookDark, CGreenDeep)), fontSize = 44.sp, letterSpacing = 3.sp)
                )
                Text(
                    "▸  YOU CAN RELAX HERE  ◂",
                    fontFamily = NunitoFontFamily,
                    color = CGreenDeep.copy(alpha = glow * 0.5f + 0.5f),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (i in 4 downTo 0) Box(modifier = Modifier.size(4.dp).background(CGreen.copy(alpha = 0.2f + i * 0.16f)))
                }
            }
            // PS1-style pixel corner markers
            for (corner in listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)) {
                Text("■", color = CGreen.copy(alpha = 0.35f), fontSize = 7.sp, modifier = Modifier.align(corner).padding(8.dp))
            }
            ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.04f)
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 22.dp, alpha = 0.15f)
            }
        }

        // ── Guide card (Habbo avatar or bot) ─────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ScrapbookDark.copy(alpha = 0.12f))
            )
            Box(modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.5.dp, Color(0xFF3949AB).copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF3949AB), Color(0xFF1A237E)))),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.habboUsername?.isNotBlank() == true) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data("https://www.${currentUser.habboRegion}/habbo-imaging/avatarimage?user=${currentUser.habboUsername}&action=std&direction=2&head_direction=2&size=l&gesture=sml")
                            .crossfade(true).build(),
                        contentDescription = "Your Habbo avatar",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                } else {
                    Text("🤖", fontSize = 28.sp)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (currentUser?.habboUsername?.isNotBlank() == true) currentUser.habboUsername.uppercase() else "RETRO BOT",
                        fontFamily = BangersFontFamily, color = Color(0xFF3949AB), fontSize = 13.sp, letterSpacing = 1.sp
                    )
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF3949AB).copy(alpha = 0.1f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("GUIDE", fontFamily = BangersFontFamily, color = Color(0xFF3949AB), fontSize = 9.sp)
                    }
                }
                Text(
                    "Rest here, traveler. Checkpoints are your milestones — the moments worth remembering. Post a game you finally beat, a place you visited, or anything that shaped your story. React, comment, and vibe with others on their journey too! 🎮",
                    fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.75f),
                    fontSize = 12.sp, lineHeight = 17.sp
                )
            }
            }
            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 18.dp, alpha = 0.18f)
            } // closes guide card wrapper Box
        }
    }
}

// ─── CheckpointCard ───────────────────────────────────────────────────────────

@Composable
fun CheckpointCard(
    checkpoint: Checkpoint,
    currentUid: String,
    currentUserProfile: UserProfileData? = null,
    onReaction: (String) -> Unit,
    onComments: () -> Unit,
    onLike: () -> Unit = {},
    onWitness: () -> Unit = {},
    onViewExpand: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    val db         = remember { FirebaseFirestore.getInstance() }
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val myReaction = checkpoint.userReactions[currentUid]
    var expanded   by remember { mutableStateOf(false) }
    var showMenu   by remember { mutableStateOf(false) }
    var bursts     by remember { mutableStateOf<List<EmojiParticle>>(emptyList()) }
    var inlineComments    by remember { mutableStateOf<List<CheckpointComment>>(emptyList()) }
    var loadingComments   by remember { mutableStateOf(false) }
    var commentInputText  by remember { mutableStateOf("") }
    var isSendingComment  by remember { mutableStateOf(false) }
    var viewCountedThisSession by remember { mutableStateOf(false) }

    // Glitch reveal — card image scrambles then resolves on first appearance
    var glitchResolved by remember { mutableStateOf(false) }
    val glitchAlpha by animateFloatAsState(
        targetValue = if (glitchResolved) 0f else 1f,
        animationSpec = tween(180, easing = LinearEasing),
        label = "glitchAlpha_${checkpoint.id}"
    )
    LaunchedEffect(checkpoint.id) {
        delay(520)
        glitchResolved = true
    }

    // Fetch inline comments when expanded
    LaunchedEffect(expanded) {
        if (expanded && inlineComments.isEmpty() && !loadingComments) {
            loadingComments = true
            try {
                val snap = db.collection("checkpoints").document(checkpoint.id)
                    .collection("comments").orderBy("timestamp").get().await()
                inlineComments = snap.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    CheckpointComment(
                        id             = doc.id,
                        authorUid      = d["authorUid"] as? String ?: "",
                        authorUsername = d["authorUsername"] as? String ?: "?",
                        authorPicUrl   = d["authorPicUrl"] as? String ?: "",
                        text           = d["text"] as? String ?: "",
                        timestamp      = (d["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    )
                }
            } catch (ignored: Exception) { }
            loadingComments = false
        }
        if (expanded && !viewCountedThisSession) {
            viewCountedThisSession = true
            onViewExpand()
        }
    }

    suspend fun postInlineComment() {
        val rawText = commentInputText.trim()
        if (rawText.isBlank() || isSendingComment) return
        val text = Moderation.gate(rawText, "checkpoint_comment") ?: return
        isSendingComment = true
        try {
            val profile = currentUserProfile
            val commentRef = db.collection("checkpoints").document(checkpoint.id).collection("comments").document()
            commentRef.set(mapOf(
                "authorUid"      to currentUid,
                "authorUsername" to (profile?.username ?: "Anonymous"),
                "authorPicUrl"   to (profile?.profilePictureUrl ?: ""),
                "text"           to text,
                "timestamp"      to FieldValue.serverTimestamp()
            )).await()
            val newComment = CheckpointComment(
                id = commentRef.id, authorUid = currentUid,
                authorUsername = profile?.username ?: "Anonymous",
                authorPicUrl = profile?.profilePictureUrl ?: "",
                text = text, timestamp = System.currentTimeMillis()
            )
            inlineComments = inlineComments + newComment
            commentInputText = ""
            db.collection("checkpoints").document(checkpoint.id)
                .update("commentCount", FieldValue.increment(1)).await()
        } catch (ignored: Exception) { }
        isSendingComment = false
    }

    val mediaUrl = when {
        checkpoint.imageUrl.isNotBlank() -> checkpoint.imageUrl
        checkpoint.gifUrl.isNotBlank()   -> checkpoint.gifUrl
        else                             -> null
    }
    val isGif = checkpoint.gifUrl.isNotBlank() && checkpoint.imageUrl.isBlank()

    // Reaction aura glow
    val totalReactionsForAura = checkpoint.reactions.values.sum()
    val hasAura = totalReactionsForAura >= 5
    val auraColor = when {
        totalReactionsForAura >= 15 -> CAcYellow
        totalReactionsForAura >= 10 -> CAcYellow
        else                        -> CGreenMint
    }
    val auraT = rememberInfiniteTransition(label = "aura_${checkpoint.id}")
    val auraPulse by auraT.animateFloat(
        initialValue = if (hasAura) 0.28f else 0f,
        targetValue  = if (hasAura) (if (totalReactionsForAura >= 10) 0.80f else 0.50f) else 0f,
        animationSpec = infiniteRepeatable(
            tween(if (totalReactionsForAura >= 10) 800 else 1300, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "auraPulse_${checkpoint.id}"
    )

    // Animated CRT scanline offset
    val crtT = rememberInfiniteTransition(label = "crt_${checkpoint.id}")
    val scanOffset by crtT.animateFloat(
        initialValue = 0f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "scanOff_${checkpoint.id}"
    )
    var cardPressed by remember { mutableStateOf(false) }
    val cardPressAnim by animateFloatAsState(if (cardPressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cpPress_${checkpoint.id}")

    Box(modifier = Modifier.fillMaxWidth()) {
        // Drop shadow / aura glow
        Box(modifier = Modifier.fillMaxWidth().padding(top = 5.dp, start = 5.dp)
            .offset(x = cardPressAnim.dp, y = cardPressAnim.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (hasAura) auraColor.copy(alpha = auraPulse)
                else ScrapbookDark.copy(alpha = 0.12f)
            ))
        Column(
            modifier = Modifier.fillMaxWidth()
                .offset(x = cardPressAnim.dp, y = cardPressAnim.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                .clickable { cardPressed = true; expanded = !expanded }
        ) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            // ── Thumbnail ────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (expanded) 240.dp else 200.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Brush.verticalGradient(listOf(ComicGlassBg, ComicGlassBg)))
            ) {
                if (mediaUrl != null) {
                    if (isGif) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(checkpoint.gifUrl)
                                .decoderFactory(GifDecoder.Factory())
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = mediaUrl, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.halftoneReveal(mediaUrl).fillMaxSize()
                        )
                    }
                } else {
                    // Placeholder with scanlines
                    Canvas(modifier = Modifier.matchParentSize()) {
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color.White.copy(alpha = 0.02f), Offset(0f, y), Offset(size.width, y), 1f)
                            y += 3f
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🏁", fontSize = 48.sp)
                            Text("CHECKPOINT", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp, letterSpacing = 2.sp)
                        }
                    }
                }
                // Animated CRT scanlines — slowly drifting downward like a real CRT refresh
                Canvas(modifier = Modifier.matchParentSize()) {
                    var yLine = (scanOffset % 4f) - 4f
                    while (yLine < size.height) {
                        drawLine(Color.Black.copy(alpha = 0.065f), Offset(0f, yLine), Offset(size.width, yLine), 1.5f)
                        yLine += 4f
                    }
                }
                // Glitch reveal overlay — static colored bands that dissolve as the card appears
                if (glitchAlpha > 0.01f) {
                    Canvas(modifier = Modifier.matchParentSize().graphicsLayer { alpha = glitchAlpha }) {
                        val seed = checkpoint.id.hashCode()
                        val bands = 10
                        val bandH = size.height / bands
                        val glitchColors = listOf(
                            Color.Cyan.copy(alpha = 0.40f),
                            Color.Magenta.copy(alpha = 0.38f),
                            Color(0xFF00FF66).copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.20f),
                            Color(0xFFFFFF00).copy(alpha = 0.25f)
                        )
                        repeat(bands) { i ->
                            val offsetX = ((seed + i * 137) % 40 - 20).toFloat()
                            drawRect(
                                color = glitchColors[(i + seed.and(0xFF)) % glitchColors.size],
                                topLeft = Offset(offsetX, bandH * i),
                                size = androidx.compose.ui.geometry.Size(size.width, bandH * 0.85f)
                            )
                        }
                        // Horizontal tear lines
                        repeat(3) { i ->
                            val y = size.height * ((seed + i * 73) and 0xFF) / 255f
                            drawLine(Color.White.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 2f)
                        }
                    }
                }
                // Bottom gradient fade
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)), startY = 60f)
                ))

                // Title overlay at bottom of thumbnail
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                    if (checkpoint.title.isNotBlank()) {
                        Text(
                            checkpoint.title,
                            fontFamily = BangersFontFamily, color = Color.White,
                            fontSize = 22.sp, letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    offset = Offset(1f, 2f), blurRadius = 6f
                                )
                            )
                        )
                    }
                    if (checkpoint.songTitle.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)) {
                            Text("♪", color = CGreen, fontSize = 11.sp)
                            Text(checkpoint.songTitle, fontFamily = NunitoFontFamily, color = CGreen.copy(alpha = 0.9f),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Badges row top-right — glassy glass style
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (checkpoint.viewCount > 0) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Brush.verticalGradient(listOf(
                                Color.White.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.55f)
                            )))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("👁", fontSize = 9.sp)
                                Text("${checkpoint.viewCount}", fontFamily = BangersFontFamily,
                                    color = Color.White, fontSize = 11.sp, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(
                            Color.White.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.55f)
                        )))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("🏁 ${timeAgoFromMillis(checkpoint.timestamp)}", fontFamily = BangersFontFamily,
                            color = Color.White, fontSize = 11.sp, letterSpacing = 0.3.sp)
                    }
                }

                // Overflow menu (own posts)
                if (onDelete != null) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp)) {
                        Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(0.4f))
                            .clickable { showMenu = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MoreVert, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Delete", color = CAcRed) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = CAcRed) })
                        }
                    }
                }

                // Pixel corner markers (retro game HUD)
                Text("▪", color = CGreen.copy(alpha = 0.55f), fontSize = 7.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp))
                Text("▪", color = CGreen.copy(alpha = 0.55f), fontSize = 7.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                // Expand hint chevron
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (expanded) CGreen else CGreen.copy(alpha = 0.85f))
                    .border(1.dp, ScrapbookDark, RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = ScrapbookDark, modifier = Modifier.size(14.dp)
                    )
                }
            }

            // ── Author row ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                    .border(2.dp, CGreen.copy(alpha = 0.5f), CircleShape)) {
                    if (checkpoint.authorPicUrl.isNotBlank())
                        AsyncImage(model = checkpoint.authorPicUrl, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(checkpoint.authorPicUrl).fillMaxSize().clip(CircleShape))
                    else Box(modifier = Modifier.fillMaxSize().background(ScrapbookDark.copy(0.15f)),
                        contentAlignment = Alignment.Center) { Text("👤", fontSize = 16.sp) }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(checkpoint.authorUsername, fontFamily = BangersFontFamily, color = ScrapbookDark,
                        fontSize = 15.sp, letterSpacing = 0.3.sp)
                    Text("@${checkpoint.authorHandle.ifBlank { checkpoint.authorUsername.lowercase() }}",
                        fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                }
                // Mini reaction count
                val totalReactions = checkpoint.reactions.values.sum()
                if (totalReactions > 0) {
                    Text("${checkpoint.userReactions.values.distinct().take(3).joinToString("")} $totalReactions",
                        fontFamily = NunitoFontFamily, color = ScrapbookTextMuted,
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Expanded section ─────────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Body text
                    if (checkpoint.body.isNotBlank()) {
                        Text(
                            checkpoint.body,
                            fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.80f),
                            fontSize = 13.sp, lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 10.dp)
                        )
                    }

                    // Game tags
                    if (checkpoint.gameTags.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(checkpoint.gameTags) { tag ->
                                HoloBadge(label = tag, cornerRadius = 6.dp, fontSize = 10.sp)
                            }
                        }
                    }
                    // Song pill (plays when expanded)
                    if (checkpoint.songTitle.isNotBlank()) {
                        CheckpointSongPill(checkpoint = checkpoint)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    HorizontalDivider(color = ScrapbookDark.copy(alpha = 0.10f))

                    // Reactions + comments
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pixel scanline decoration
                        listOf("😍", "🔥", "💀", "🏆", "💯").forEach { emoji ->
                            CheckpointReactionChip(
                                emoji    = emoji,
                                count    = checkpoint.reactions[emoji] ?: 0,
                                isActive = myReaction == emoji,
                                onClick  = {
                                    onReaction(emoji)
                                    bursts = bursts + EmojiParticle(
                                        id = System.currentTimeMillis() + emoji.hashCode().toLong() + bursts.size,
                                        emoji = emoji, xOffset = (30 + (bursts.size % 5) * 52).toFloat()
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        val hasLiked = currentUid in checkpoint.likedBy
                        val hasWitnessed = currentUid in checkpoint.witnessedBy
                        if (checkpoint.likeCount >= 5) {
                            CheckpointHotBadge()
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        // Like button
                        val likeBurst = rememberBurstState()
                        Box {
                        Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (hasLiked) Color(0xFFE63946).copy(alpha = 0.12f) else ScrapbookDark.copy(alpha = 0.06f))
                            .border(1.dp, if (hasLiked) Color(0xFFE63946).copy(alpha = 0.55f) else ScrapbookDark.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable { if (!hasLiked) likeBurst.fire("LOVE!", CGreenMint); onLike() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (hasLiked) "❤️" else "🤍", fontSize = 13.sp)
                            if (checkpoint.likeCount > 0)
                                RollingCounterText("${checkpoint.likeCount}", androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily,
                                    color = if (hasLiked) Color(0xFFE63946) else ScrapbookTextMuted,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        ComicBurst(likeBurst, Modifier.align(Alignment.Center), burstSize = 84.dp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Witness button
                        Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (hasWitnessed) CGreen.copy(alpha = 0.18f) else ScrapbookDark.copy(alpha = 0.06f))
                            .border(1.dp, if (hasWitnessed) CGreen.copy(alpha = 0.6f) else ScrapbookDark.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable { onWitness() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("👀", fontSize = 13.sp)
                            if (checkpoint.witnessCount > 0)
                                Text("${checkpoint.witnessCount}", fontFamily = NunitoFontFamily,
                                    color = if (hasWitnessed) CGreenDeep else ScrapbookTextMuted,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Comments button
                        Row(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(ScrapbookDark.copy(alpha = 0.06f))
                            .border(1.dp, ScrapbookDark.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .clickable { onComments() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ChatBubbleOutline, null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp))
                            val displayCount = maxOf(checkpoint.commentCount, inlineComments.size)
                            if (displayCount > 0)
                                Text("$displayCount", fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // ── Inline Comments Thread ────────────────────────────────
                    if (inlineComments.isNotEmpty() || loadingComments) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = ScrapbookDark.copy(alpha = 0.08f)
                        )
                        if (loadingComments) {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CGreen, strokeWidth = 2.dp)
                            }
                        } else {
                            Column(modifier = Modifier.padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                inlineComments.take(5).forEach { comment ->
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Avatar
                                        Box(modifier = Modifier.size(26.dp).clip(CircleShape)
                                            .background(CGreen.copy(alpha = 0.20f))
                                            .border(1.dp, CGreen.copy(alpha = 0.45f), CircleShape)) {
                                            if (comment.authorPicUrl.isNotBlank()) {
                                                AsyncImage(model = comment.authorPicUrl, contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.halftoneReveal(comment.authorPicUrl).fillMaxSize().clip(CircleShape))
                                            } else {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text("👤", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                        // Comment bubble
                                        Column(modifier = Modifier
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                                            .background(Color.White.copy(alpha = 0.92f))
                                            .border(1.dp, ScrapbookDark.copy(alpha = 0.08f),
                                                RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .weight(1f, fill = false)) {
                                            Text(comment.authorUsername, fontFamily = BangersFontFamily,
                                                color = CGreenDeep, fontSize = 11.sp, letterSpacing = 0.2.sp)
                                            Text(comment.text, fontFamily = NunitoFontFamily,
                                                color = ScrapbookDark.copy(alpha = 0.82f), fontSize = 12.sp, lineHeight = 17.sp)
                                        }
                                    }
                                }
                                if (inlineComments.size > 5) {
                                    Text("+ ${inlineComments.size - 5} more comments…",
                                        fontFamily = NunitoFontFamily, color = ScrapbookTextMuted,
                                        fontSize = 11.sp, modifier = Modifier.padding(start = 34.dp))
                                }
                            }
                        }
                    }

                    // ── Inline Comment Input ──────────────────────────────────
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .border(1.dp, ScrapbookDark.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)) {
                            BasicTextField(
                                value = commentInputText,
                                onValueChange = { if (it.length <= 200) commentInputText = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = NunitoFontFamily, fontSize = 12.sp,
                                    color = ScrapbookDark
                                ),
                                decorationBox = { inner ->
                                    if (commentInputText.isEmpty()) {
                                        Text("Add a comment…", fontFamily = NunitoFontFamily,
                                            color = ScrapbookTextMuted, fontSize = 12.sp)
                                    }
                                    inner()
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(CGreen)
                            )
                        }
                        Box(modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                            .border(1.5.dp, ScrapbookDark, CircleShape)
                            .clickable {
                                if (!isSendingComment && commentInputText.isNotBlank()) {
                                    scope.launch { postInlineComment() }
                                }
                            },
                            contentAlignment = Alignment.Center) {
                            if (isSendingComment) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = ScrapbookDark, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Send, null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Collapsed: mini action bar
            AnimatedVisibility(visible = !expanded) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                        .border(1.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                        .clickable { expanded = true }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("TAP TO EXPAND", fontFamily = BangersFontFamily, color = ScrapbookDark,
                            fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Like (collapsed)
                    Row(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (currentUid in checkpoint.likedBy) Color(0xFFE63946).copy(alpha = 0.12f) else ScrapbookDark.copy(alpha = 0.06f))
                        .border(1.dp, if (currentUid in checkpoint.likedBy) Color(0xFFE63946).copy(alpha = 0.4f) else ScrapbookDark.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { onLike() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (currentUid in checkpoint.likedBy) "❤️" else "🤍", fontSize = 12.sp)
                        if (checkpoint.likeCount > 0)
                            Text("${checkpoint.likeCount}", fontFamily = NunitoFontFamily,
                                color = if (currentUid in checkpoint.likedBy) Color(0xFFE63946) else ScrapbookTextMuted,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    // Witness (collapsed)
                    Row(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (currentUid in checkpoint.witnessedBy) CGreen.copy(alpha = 0.18f) else ScrapbookDark.copy(alpha = 0.06f))
                        .border(1.dp, if (currentUid in checkpoint.witnessedBy) CGreen.copy(alpha = 0.5f) else ScrapbookDark.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { onWitness() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("👀", fontSize = 12.sp)
                        if (checkpoint.witnessCount > 0)
                            Text("${checkpoint.witnessCount}", fontFamily = NunitoFontFamily,
                                color = if (currentUid in checkpoint.witnessedBy) CGreenDeep else ScrapbookTextMuted,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    // Comments (collapsed)
                    Row(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookDark.copy(alpha = 0.06f))
                        .border(1.dp, ScrapbookDark.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { onComments() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Filled.ChatBubbleOutline, null, tint = ScrapbookTextMuted, modifier = Modifier.size(13.dp))
                        if (checkpoint.commentCount > 0)
                            Text("${checkpoint.commentCount}", fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        ComicShimmer(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp)), cornerRadius = 20.dp)
        // Floating burst particles
        bursts.forEach { particle ->
            key(particle.id) {
                Box(modifier = Modifier.align(Alignment.BottomStart).offset(x = particle.xOffset.dp, y = (-8).dp)) {
                    FloatingEmojiParticle(emoji = particle.emoji,
                        onFinished = { bursts = bursts.filter { it.id != particle.id } })
                }
            }
        }
    }
    LaunchedEffect(cardPressed) { if (cardPressed) { delay(150); cardPressed = false } }
}

// ─── Floating Emoji Burst ──────────────────────────────────────────────────────

@Composable
fun FloatingEmojiParticle(emoji: String, onFinished: () -> Unit) {
    var trigger by remember { mutableStateOf(false) }

    val offsetY by animateFloatAsState(
        targetValue = if (trigger) -80f else 0f,
        animationSpec = tween(950, easing = FastOutSlowInEasing),
        label = "burstY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (trigger) 0f else 1f,
        animationSpec = tween(950, easing = LinearEasing),
        finishedListener = { if (trigger) onFinished() },
        label = "burstAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (trigger) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "burstScale"
    )

    LaunchedEffect(Unit) { trigger = true }

    Text(
        emoji,
        fontSize = 22.sp,
        modifier = Modifier
            .offset(y = offsetY.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    )
}

// ─── Retro Save Overlay ────────────────────────────────────────────────────────

@Composable
fun RetroSaveOverlay(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val diskDrop = remember { Animatable(-140f) }
    val stamp = remember { Animatable(2.6f) }
    var saved by remember { mutableStateOf(false) }
    val haptic = rememberComicHaptic()

    LaunchedEffect(Unit) {
        // Floppy slides down into the drive slot
        diskDrop.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 260f))
        Chiptune.play(Chiptune.Sfx.BLIP)
        progress.animateTo(1f, animationSpec = tween(1500, easing = LinearEasing))
        saved = true
        haptic()
        Chiptune.play(Chiptune.Sfx.POWER_UP)
        stamp.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 600f))
        delay(900)
        onFinished()
    }

    val blinkA by rememberGlowRange(0.35f, 1f)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth(0.84f)) {
            Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp)
                .clip(RoundedCornerShape(18.dp)).background(CGreen))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Floppy disk + drive slot ──
                Box(modifier = Modifier.size(width = 120.dp, height = 110.dp), contentAlignment = Alignment.BottomCenter) {
                    // disk (drawn behind the drive so it looks like it slides in)
                    Box(
                        modifier = Modifier.size(78.dp).align(Alignment.TopCenter)
                            .graphicsLayer { translationY = diskDrop.value * density; alpha = if (saved) 0f else 1f }
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(ScrapbookDark)
                    ) {
                        // metal shutter
                        Box(Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(width = 36.dp, height = 22.dp)
                            .clip(RoundedCornerShape(2.dp)).background(Color(0xFFC9C9C9)))
                        // label
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(width = 58.dp, height = 30.dp)
                            .clip(RoundedCornerShape(2.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            Text("RETROHUB", fontFamily = BangersFontFamily, fontSize = 9.sp, color = CGreenDeep)
                        }
                    }
                    // drive
                    Box(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .clip(RoundedCornerShape(8.dp)).background(Color(0xFFE6E6E6))
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(ScrapbookDark))
                        Box(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(7.dp).clip(CircleShape)
                            .background(if (saved) CGreen else CGreenMint.copy(alpha = blinkA)))
                    }
                }

                if (!saved) {
                    Text(
                        "SAVING TO MEMORY CARD...",
                        fontFamily = BangersFontFamily,
                        color = CGreenDeep.copy(alpha = blinkA),
                        fontSize = 15.sp, letterSpacing = 1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Chunky pixel progress bar
                Box(
                    modifier = Modifier.fillMaxWidth().height(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ScrapbookDark.copy(alpha = 0.08f))
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(3.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth(progress.value).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(CGreenMint, CGreen, CGreenDeep))))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val segments = 20
                        repeat(segments - 1) { i ->
                            val x = size.width / segments * (i + 1)
                            drawLine(Color.White.copy(alpha = 0.6f), Offset(x, 0f), Offset(x, size.height), 2f)
                        }
                    }
                }

                // GAME SAVED! stamp
                if (saved) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = stamp.value; scaleY = stamp.value
                                rotationZ = -8f
                                alpha = (2.6f - stamp.value).coerceIn(0f, 1f)
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(CGreen)
                            .border(3.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                    ) {
                        Text("GAME SAVED!", fontFamily = BangersFontFamily, color = ScrapbookDark,
                            fontSize = 28.sp, letterSpacing = 2.sp)
                    }
                } else {
                    Text("${(progress.value * 100).toInt()}%", fontFamily = BangersFontFamily,
                        color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Confetti Overlay ─────────────────────────────────────────────────────────

@Composable
fun ConfettiOverlay(onFinished: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val particles = remember { generateConfettiParticles(screenWidthPx) }

    val infT = rememberInfiniteTransition(label = "confetti")
    val progress by infT.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiProg"
    )

    LaunchedEffect(Unit) {
        delay(3200)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dt = progress * 2.8f
            particles.forEach { p ->
                val x   = p.startX + p.vx * dt * 90f
                val y   = p.startY + p.vy * dt * 130f + 25f * dt * dt
                val rot = p.rotationSpeed * dt * 200f

                if (y > size.height + 30) return@forEach

                withTransform({
                    translate(x.coerceIn(-20f, size.width + 20f), y)
                    rotate(rot % 360f)
                }) {
                    when (p.shape) {
                        1    -> drawCircle(p.color, radius = p.size * 0.5f)
                        2    -> drawRect(p.color, topLeft = Offset(-p.size * 0.2f, -p.size * 0.5f), size = Size(p.size * 0.4f, p.size))
                        else -> drawRect(p.color, topLeft = Offset(-p.size * 0.5f, -p.size * 0.3f), size = Size(p.size, p.size * 0.6f))
                    }
                }
            }
        }

        // "🎉 Posted!" banner
        Box(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.horizontalGradient(listOf(CGreen, CGreenMint)))
                .border(2.dp, ScrapbookDark, RoundedCornerShape(28.dp))
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text("🎉 CHECKPOINT POSTED!", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
        }
    }
}

// ─── Reaction Chip ────────────────────────────────────────────────────────────

@Composable
fun CheckpointReactionChip(emoji: String, count: Int, isActive: Boolean, onClick: () -> Unit) {
    var popped by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            popped   -> 1.25f
            isActive -> 1.08f
            else     -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { if (popped) popped = false },
        label = "reactionScale"
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) CGreen.copy(alpha = 0.30f) else ScrapbookDark.copy(alpha = 0.07f))
            .border(1.dp, if (isActive) CGreen.copy(alpha = 0.75f) else ScrapbookDark.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable { popped = true; onClick() }
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(emoji, fontSize = 14.sp)
            if (count > 0) {
                Text("$count", fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Checkpoint Song Pill (playable) ─────────────────────────────────────────

@Composable
fun CheckpointSongPill(checkpoint: Checkpoint) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    val hasPreview = checkpoint.songPreviewUrl.isNotBlank()

    // Play as soon as the pill enters composition (card was tapped/expanded)
    LaunchedEffect(checkpoint.id) {
        if (hasPreview) {
            delay(150) // tiny settle delay for animation
            isPlaying = true
        }
    }

    // Manage MediaPlayer lifecycle
    DisposableEffect(isPlaying, checkpoint.id) {
        if (isPlaying && hasPreview) {
            val mp = android.media.MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(checkpoint.songPreviewUrl)
                isLooping = true  // loop the 30s preview continuously while user reads
                setOnPreparedListener { start() }
                prepareAsync()
            }
            mediaPlayer = mp
            onDispose {
                mp.stop()
                mp.release()
                mediaPlayer = null
            }
        } else {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            onDispose { }
        }
    }

    val borderA by rememberGlowRange(0.2f, 0.6f)

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(
                1.5.dp,
                if (isPlaying && hasPreview) CGreen.copy(alpha = borderA + 0.4f) else ScrapbookDark.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .then(if (hasPreview) Modifier.clickable { isPlaying = !isPlaying } else Modifier)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Artwork or music note icon
            if (checkpoint.songArtworkUrl.isNotBlank()) {
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))) {
                    AsyncImage(model = checkpoint.songArtworkUrl, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(checkpoint.songArtworkUrl).fillMaxSize())
                    // Play/pause overlay
                    if (hasPreview) {
                        Box(modifier = Modifier.fillMaxSize()
                            .background(Color.Black.copy(alpha = if (isPlaying) 0f else 0.5f)),
                            contentAlignment = Alignment.Center) {
                            if (!isPlaying) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(CGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Text(if (isPlaying && hasPreview) "▶" else "♪", color = CGreenDeep, fontSize = 12.sp)
                }
            }

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(checkpoint.songTitle, fontFamily = NunitoFontFamily, color = ScrapbookDark,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (checkpoint.songArtist.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(checkpoint.songArtist, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f),
                            fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (isPlaying && hasPreview) {
                            Text("• playing", fontFamily = NunitoFontFamily, color = CGreenDeep,
                                fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Wave bars (animated when playing, static otherwise) or play icon
            if (hasPreview) {
                if (isPlaying) {
                    SongWaveBars()
                } else {
                    Icon(Icons.Filled.PlayArrow, null, tint = CGreenDeep, modifier = Modifier.size(18.dp))
                }
            } else {
                SongWaveBars()
            }
        }
    }
}

// ─── Song wave animation ──────────────────────────────────────────────────────

@Composable
fun SongWaveBars() {
    val t = rememberInfiniteTransition(label = "wave")
    val h1 by t.animateFloat(initialValue = 4f, targetValue = 14f, animationSpec = infiniteRepeatable(tween(400, easing = EaseInOut), RepeatMode.Reverse), label = "h1")
    val h2 by t.animateFloat(initialValue = 8f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse), label = "h2")
    val h3 by t.animateFloat(initialValue = 3f, targetValue = 12f, animationSpec = infiniteRepeatable(tween(500, easing = EaseInOut), RepeatMode.Reverse), label = "h3")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(h1, h2, h3).forEach { h ->
            Box(modifier = Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(CGreen.copy(alpha = 0.8f)))
        }
    }
}

// ─── Create Sheet ─────────────────────────────────────────────────────────────

@Composable
fun CheckpointCreateSheet(
    userProfile: UserProfileData?,
    viewModel: CheckpointViewModel,
    onDismiss: () -> Unit
) {
    var title           by remember { mutableStateOf("") }
    var body            by remember { mutableStateOf("") }
    var selectedMusic   by remember { mutableStateOf<MusicSearchResult?>(null) }
    var selectedGif     by remember { mutableStateOf<GifResult?>(null) }
    var imageUri        by remember { mutableStateOf<Uri?>(null) }
    var isPosting           by remember { mutableStateOf(false) }
    var showMusicSearch     by remember { mutableStateOf(false) }
    var showGifSearch       by remember { mutableStateOf(false) }
    var isPreviewingMusic   by remember { mutableStateOf(false) }
    var createPreviewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri = it; selectedGif = null }
    }
    DisposableEffect(Unit) {
        onDispose { createPreviewPlayer?.stop(); createPreviewPlayer?.release() }
    }

    val shimmer = rememberInfiniteTransition(label = "cs")
    val shimmerX by shimmer.animateFloat(
        -300f, 900f, infiniteRepeatable(tween(2200, easing = LinearEasing)), "csShimmer"
    )
    val pulseScale by shimmer.animateFloat(
        1f, 1.03f, infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), "csPulse"
    )

    Dialog(
        onDismissRequest = { if (!isPosting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().fillMaxHeight(0.94f), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(ComicGlassBg)
            ) {
                // ── Yellow racing header ────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(CGreen, Color(0xFFFFD000))))
                ) {
                    // Diagonal stripe decoration
                    Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                        var x = -size.height
                        while (x < size.width + size.height) {
                            drawLine(Color.Black.copy(alpha = 0.07f), Offset(x, size.height), Offset(x + size.height, 0f), 20f)
                            x += 40f
                        }
                        // Shimmer sweep
                        drawRect(
                            brush = Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(0.25f), Color.Transparent),
                                start = Offset(shimmerX, 0f), end = Offset(shimmerX + 200f, size.height)
                            )
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 14.dp, start = 18.dp, end = 12.dp)) {
                        // Drag handle
                        Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp)
                            .width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.Black.copy(alpha = 0.2f)))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🏁", fontSize = 28.sp)
                                Column {
                                    Text("NEW", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f),
                                        fontSize = 13.sp, letterSpacing = 3.sp)
                                    Text("CHECKPOINT", fontFamily = BangersFontFamily, color = ScrapbookDark,
                                        fontSize = 28.sp, letterSpacing = 1.sp, lineHeight = 28.sp)
                                }
                            }
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.12f))
                                .clickable { if (!isPosting) onDismiss() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // ── Scrollable form body ────────────────────────────────────
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // TITLE
                    CreateSectionCard(accentColor = CGreen) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookDark),
                                contentAlignment = Alignment.Center) { Text("📌", fontSize = 14.sp) }
                            Text("CHECKPOINT TITLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                        }
                        BrightTextField(value = title, onValueChange = { title = it },
                            placeholder = "Name this milestone...", maxLines = 2, singleLine = false)
                    }

                    // BODY
                    CreateSectionCard(accentColor = Color(0xFF3949AB)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)) {
                            Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF3949AB)),
                                contentAlignment = Alignment.Center) { Text("✍️", fontSize = 14.sp) }
                            Text("THE STORY", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, letterSpacing = 1.sp)
                        }
                        BrightTextField(value = body, onValueChange = { body = it },
                            placeholder = "What happened? Why does this matter?", maxLines = 5, singleLine = false)
                    }

                    // PHOTO + GIF side by side
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                        // Photo
                        Box(modifier = Modifier.weight(1f).height(110.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (imageUri != null) Color.Transparent else Color.White.copy(alpha = 0.46f))
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(18.dp))
                            .clickable { selectedGif = null; imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.halftoneReveal(imageUri).fillMaxSize().clip(RoundedCornerShape(18.dp)))
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.28f)).clip(RoundedCornerShape(18.dp)))
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp)
                                    .clip(CircleShape).background(Color.White.copy(0.9f)).clickable { imageUri = null },
                                    contentAlignment = Alignment.Center) {
                                    Text("✕", color = ScrapbookDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📷", fontSize = 26.sp)
                                    Text("PHOTO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, letterSpacing = 1.sp)
                                }
                            }
                        }

                        // GIF
                        Box(modifier = Modifier.weight(1f).height(110.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedGif != null) Color.Transparent else Color.White.copy(alpha = 0.46f))
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(18.dp))
                            .clickable { showGifSearch = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedGif != null) {
                                AsyncImage(model = selectedGif!!.previewUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.halftoneReveal(selectedGif!!.previewUrl).fillMaxSize().clip(RoundedCornerShape(18.dp)))
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.22f)).clip(RoundedCornerShape(18.dp)))
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp)
                                    .clip(CircleShape).background(Color.White.copy(0.9f)).clickable { selectedGif = null },
                                    contentAlignment = Alignment.Center) {
                                    Text("✕", color = ScrapbookDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp)).background(CAcPurple)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("GIF", fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🎞️", fontSize = 26.sp)
                                    Text("GIF", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }

                    // SOUNDTRACK
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (selectedMusic != null)
                                    Brush.linearGradient(listOf(CGreen.copy(alpha = 0.18f), Color.White.copy(alpha = 0.46f)))
                                else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.46f)))
                            )
                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(18.dp))
                            .clickable { showMusicSearch = true }
                            .padding(14.dp)
                    ) {
                        if (selectedMusic != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                                    .background(CGreen.copy(0.2f))) {
                                    if (selectedMusic!!.artworkUrl.isNotBlank())
                                        AsyncImage(model = selectedMusic!!.artworkUrl, contentDescription = null,
                                            contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(selectedMusic!!.artworkUrl).fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                    // Vinyl ring overlay
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.Black.copy(0.5f)))
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CGreen))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("♪ NOW PLAYING", fontFamily = BangersFontFamily, color = CGreenDeep,
                                        fontSize = 10.sp, letterSpacing = 2.sp)
                                    Text(selectedMusic!!.trackName, fontFamily = BangersFontFamily, color = ScrapbookDark,
                                        fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(selectedMusic!!.artistName, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f),
                                        fontSize = 11.sp, maxLines = 1)
                                }
                                // Preview play/pause
                                if (selectedMusic!!.previewUrl.isNotBlank()) {
                                    val neonAnim = rememberInfiniteTransition(label = "prevNeon")
                                    val neonA by neonAnim.animateFloat(
                                        0.3f, 0.9f, infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), "pN"
                                    )
                                    Box(modifier = Modifier.size(34.dp).clip(CircleShape)
                                        .background(if (isPreviewingMusic) CGreen else ScrapbookDark.copy(alpha = 0.08f))
                                        .border(1.5.dp, if (isPreviewingMusic) CGreen.copy(alpha = neonA) else Color.Transparent, CircleShape)
                                        .clickable {
                                            if (isPreviewingMusic) {
                                                createPreviewPlayer?.stop(); createPreviewPlayer?.release(); createPreviewPlayer = null; isPreviewingMusic = false
                                            } else {
                                                createPreviewPlayer?.stop(); createPreviewPlayer?.release(); createPreviewPlayer = null
                                                val mp = android.media.MediaPlayer().apply {
                                                    setAudioAttributes(android.media.AudioAttributes.Builder()
                                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA).build())
                                                    setDataSource(selectedMusic!!.previewUrl)
                                                    setOnPreparedListener { start() }
                                                    setOnCompletionListener { isPreviewingMusic = false }
                                                    prepareAsync()
                                                }
                                                createPreviewPlayer = mp; isPreviewingMusic = true
                                            }
                                        }, contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isPreviewingMusic) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            null,
                                            tint = if (isPreviewingMusic) ScrapbookDark else CGreenDeep,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                                    .background(ScrapbookDark.copy(alpha = 0.08f)).clickable { selectedMusic = null; isPreviewingMusic = false; createPreviewPlayer?.stop(); createPreviewPlayer?.release(); createPreviewPlayer = null },
                                    contentAlignment = Alignment.Center) {
                                    Text("✕", color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(CGreen.copy(alpha = 0.35f), ComicGlassBg))),
                                    contentAlignment = Alignment.Center) {
                                    Text("🎵", fontSize = 20.sp)
                                }
                                Column {
                                    Text("SOUNDTRACK", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 15.sp, letterSpacing = 1.sp)
                                    Text("Tap to add the vibe", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ── Post button ─────────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth().background(ComicGlassBgAlt).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    val canPost = title.isNotBlank() && !isPosting
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (canPost)
                                    Brush.linearGradient(listOf(CGreen, Color(0xFFFFD000), Color(0xFFFFAA00)))
                                else Brush.linearGradient(listOf(Color(0xFFDDCCAA), Color(0xFFCCBB99)))
                            )
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                            .then(if (canPost) Modifier.clickable {
                                isPosting = true
                                viewModel.postCheckpoint(title, body, imageUri, selectedGif?.originalUrl ?: "", selectedMusic, userProfile) { onDismiss() }
                            } else Modifier)
                            .padding(vertical = 17.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPosting) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = ScrapbookDark, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                                Text("POSTING...", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏁", fontSize = 18.sp)
                                Text("POST CHECKPOINT", fontFamily = BangersFontFamily,
                                    color = if (canPost) ScrapbookDark else Color(0xFF998866),
                                    fontSize = 20.sp, letterSpacing = 1.sp)
                            }
                        }
                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 16.dp, alpha = 0.20f)
                    }
                }
            }
        }
    }

    if (showMusicSearch) {
        MusicSearchSheet(viewModel = viewModel, onSelect = { selectedMusic = it; showMusicSearch = false }, onDismiss = { showMusicSearch = false })
    }
    if (showGifSearch) {
        GifSearchSheet(viewModel = viewModel, onSelect = { selectedGif = it; imageUri = null; showGifSearch = false }, onDismiss = { showGifSearch = false })
    }
}

@Composable
private fun CreateSectionCard(accentColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(2.dp, ScrapbookDark, RoundedCornerShape(18.dp))
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.5f))))
                .align(Alignment.CenterStart)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            content = content
        )
    }
}

@Composable
private fun BrightTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String,
    maxLines: Int = 1, singleLine: Boolean = true
) {
    BasicTextField(
        value = value, onValueChange = onValueChange,
        maxLines = if (singleLine) 1 else maxLines, singleLine = singleLine,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
        ),
        cursorBrush = SolidColor(ScrapbookDark),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontFamily = NunitoFontFamily,
                    color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp)
                innerTextField()
            }
        }
    )
}

// ─── Music Search Sheet ───────────────────────────────────────────────────────

@Composable
fun MusicSearchSheet(viewModel: CheckpointViewModel, onSelect: (MusicSearchResult) -> Unit, onDismiss: () -> Unit) {
    var query       by remember { mutableStateOf("") }
    var results     by remember { mutableStateOf<List<MusicSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var playingId   by remember { mutableStateOf<Long?>(null) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

    LaunchedEffect(query) {
        if (query.length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(400); isSearching = true
        viewModel.searchMusic(query) { results = it; isSearching = false }
    }
    DisposableEffect(Unit) { onDispose { mediaPlayer?.stop(); mediaPlayer?.release() } }

    fun playPreview(track: MusicSearchResult) {
        if (playingId == track.trackId) {
            mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; playingId = null; return
        }
        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
        playingId = track.trackId
        val mp = android.media.MediaPlayer().apply {
            setAudioAttributes(android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA).build())
            setDataSource(track.previewUrl)
            setOnPreparedListener { start() }
            setOnCompletionListener { playingId = null }
            prepareAsync()
        }
        mediaPlayer = mp
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().fillMaxHeight(0.88f), contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(ComicGlassBg)
                .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            ) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                    .width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookDark.copy(alpha = 0.3f)))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.MusicNote, null, tint = CGreenDeep, modifier = Modifier.size(20.dp))
                        Text("ADD SOUNDTRACK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.5.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.7f)) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.7f))
                    .border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Search, null, tint = ScrapbookDark.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
                    BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 14.sp),
                        cursorBrush = SolidColor(ScrapbookDark),
                        decorationBox = { inner ->
                            Box { if (query.isEmpty()) Text("Search songs, artists...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp); inner() }
                        }, modifier = Modifier.weight(1f))
                    if (isSearching) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else if (query.isNotEmpty()) Text("✕", color = ScrapbookDark.copy(alpha = 0.55f), modifier = Modifier.clickable { query = "" })
                }
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    isSearching -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = CGreen, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Text("Searching...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp)
                        }
                    }
                    results.isNotEmpty() -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results, key = { it.trackId }) { track ->
                            val isPlaying = playingId == track.trackId
                            val glowAnim = rememberInfiniteTransition(label = "glow_${track.trackId}")
                            val glowAlpha by glowAnim.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "ga_${track.trackId}")
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(if (isPlaying) Brush.linearGradient(listOf(CGreen.copy(alpha = 0.22f), Color.White.copy(alpha = 0.46f))) else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.46f), Color.White.copy(alpha = 0.46f))))
                                .border(1.5.dp, if (isPlaying) CGreenDeep.copy(alpha = glowAlpha) else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            ) {
                                // Playing accent bar on left
                                if (isPlaying) Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(CGreen, CGreenMint))).align(Alignment.CenterStart))
                                Row(modifier = Modifier.fillMaxWidth().padding(start = if (isPlaying) 16.dp else 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Album art with glow
                                    Box(modifier = Modifier.size(58.dp)) {
                                        if (isPlaying) Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)).background(CGreen.copy(alpha = 0.25f * glowAlpha)).align(Alignment.Center))
                                        Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(11.dp)).align(Alignment.Center)) {
                                            AsyncImage(model = track.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(track.artworkUrl).fillMaxSize())
                                            if (isPlaying) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                                SongWaveBars()
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(track.trackName, fontFamily = NunitoFontFamily, color = if (isPlaying) CGreenDeep else ScrapbookDark,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.artistName, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (track.collectionName.isNotBlank()) {
                                            Text(track.collectionName, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                                            .background(if (isPlaying) CGreen.copy(alpha = 0.2f) else ScrapbookDark.copy(alpha = 0.06f))
                                            .border(1.dp, if (isPlaying) CGreen.copy(alpha = 0.6f) else ScrapbookDark.copy(alpha = 0.3f), CircleShape)
                                            .clickable { playPreview(track) }, contentAlignment = Alignment.Center) {
                                            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null,
                                                tint = if (isPlaying) CGreenDeep else ScrapbookDark.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                        Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(Brush.linearGradient(listOf(CGreen, CGreenMint)))
                                            .clickable { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; onSelect(track) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)) {
                                            Text("USE ♪", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    query.length >= 2 -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎵", fontSize = 36.sp)
                            Text("No tracks found", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp)
                        }
                    }
                    else -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🎧", fontSize = 48.sp)
                            Text("WHAT'S THE VIBE?", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 20.sp, letterSpacing = 1.sp)
                            Text("Search any song, artist or game soundtrack", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Create field helpers ─────────────────────────────────────────────────────

@Composable
private fun CreateFieldLabel(text: String) {
    Text(text, fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp, letterSpacing = 1.sp)
}

@Composable
private fun CreateTextField(
    value: String, onValueChange: (String) -> Unit, placeholder: String,
    maxLines: Int = 1, singleLine: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value, onValueChange = onValueChange,
        maxLines = if (singleLine) 1 else maxLines, singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp),
        cursorBrush = SolidColor(ScrapbookDark),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, if (focused) ScrapbookDark else ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp)
                innerTextField()
            }
        }
    )
}

// ─── Comments Sheet ───────────────────────────────────────────────────────────

@Composable
fun CheckpointCommentsSheet(
    checkpoint: Checkpoint,
    currentUid: String,
    currentUserProfile: UserProfileData?,
    onDismiss: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var comments    by remember { mutableStateOf<List<CheckpointComment>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var isSending   by remember { mutableStateOf(false) }

    LaunchedEffect(checkpoint.id) {
        try {
            val snap = db.collection("checkpoints").document(checkpoint.id)
                .collection("comments").orderBy("timestamp").get().await()
            comments = snap.documents.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                CheckpointComment(
                    id             = doc.id,
                    authorUid      = d["authorUid"] as? String ?: "",
                    authorUsername = d["authorUsername"] as? String ?: "Anonymous",
                    authorPicUrl   = d["authorPicUrl"] as? String ?: "",
                    text           = d["text"] as? String ?: "",
                    timestamp      = (d["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                )
            }
        } catch (ignored: Exception) { }
        isLoading = false
    }

    fun sendComment() {
        val rawText = commentText.trim()
        if (rawText.isBlank() || isSending) return
        val text = Moderation.gate(rawText, "checkpoint_comment") ?: return
        isSending = true
        val docRef = db.collection("checkpoints").document(checkpoint.id).collection("comments").document()
        docRef.set(mapOf(
            "authorUid"      to (currentUserProfile?.uid ?: currentUid),
            "authorUsername" to (currentUserProfile?.username ?: "Anonymous"),
            "authorPicUrl"   to (currentUserProfile?.profilePictureUrl ?: ""),
            "text"           to text,
            "timestamp"      to FieldValue.serverTimestamp()
        ))
        db.collection("checkpoints").document(checkpoint.id)
            .update("commentCount", FieldValue.increment(1))
        comments = comments + CheckpointComment(
            id = docRef.id, authorUid = currentUid,
            authorUsername = currentUserProfile?.username ?: "Anonymous",
            authorPicUrl = currentUserProfile?.profilePictureUrl ?: "",
            text = text, timestamp = System.currentTimeMillis()
        )
        commentText = ""
        isSending = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().fillMaxHeight(0.85f), contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(ComicGlassBg)
                .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                    .width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookBorder.copy(alpha = 0.4f)))

                // Header with checkpoint summary
                AeroGlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable {}, accentColor = CGreen, glowAlpha = 0.2f, cornerRadius = 14.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val thumbUrl = when {
                            checkpoint.imageUrl.isNotBlank() -> checkpoint.imageUrl
                            checkpoint.gifUrl.isNotBlank()   -> checkpoint.gifUrl
                            else                             -> null
                        }
                        Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.1f))) {
                            if (thumbUrl != null) AsyncImage(model = thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(thumbUrl).fillMaxSize())
                            else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🏁", fontSize = 28.sp) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(checkpoint.title.ifBlank { "Checkpoint" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(checkpoint.authorUsername, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            if (checkpoint.songTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("♪", color = ScrapbookDark, fontSize = 11.sp)
                                    Text(checkpoint.songTitle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = ScrapbookTextMuted.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.ChatBubbleOutline, null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                        Text("COMMENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${comments.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null, tint = ScrapbookTextMuted) }
                }

                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CGreen)
                        }
                        comments.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("💬", fontSize = 36.sp)
                                Text("NO COMMENTS YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                                Text("Be the first to share your memory!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                            }
                        }
                        else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(comments, key = { it.id }) { comment ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.1f)).border(1.dp, CGreen.copy(alpha = 0.4f), CircleShape)) {
                                        if (comment.authorPicUrl.isNotBlank()) AsyncImage(model = comment.authorPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(comment.authorPicUrl).fillMaxSize().clip(CircleShape))
                                        else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("👤", fontSize = 16.sp) }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(comment.authorUsername, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                            Text(timeAgoFromMillis(comment.timestamp), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)).background(Color.White.copy(alpha = 0.92f)).padding(horizontal = 10.dp, vertical = 7.dp)) {
                                            Text(comment.text, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.15f))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicTextField(
                        value = commentText, onValueChange = { commentText = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp),
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                            .background(ComicGlassBg).border(1.dp, CGreenDeep.copy(alpha=0.3f).copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        decorationBox = { inner ->
                            Box { if (commentText.isEmpty()) Text("Share your memory...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp); inner() }
                        }
                    )
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(if (commentText.isNotBlank()) Brush.linearGradient(listOf(CGreen, CGreenMint)) else Brush.linearGradient(listOf(ScrapbookBorder, ScrapbookBorder)))
                            .then(if (commentText.isNotBlank()) Modifier.clickable { sendComment() } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSending) CircularProgressIndicator(color = ScrapbookDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Send, null, tint = if (commentText.isNotBlank()) ScrapbookDark else Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─── GIF Search Sheet ─────────────────────────────────────────────────────────

@Composable
fun GifSearchSheet(viewModel: CheckpointViewModel, onSelect: (GifResult) -> Unit, onDismiss: () -> Unit) {
    var query       by remember { mutableStateOf("") }
    val results     = viewModel.gifResults.value
    val isSearching = viewModel.gifSearching.value
    val gifError    = viewModel.gifError.value

    LaunchedEffect(Unit) { viewModel.gifResults.value = emptyList(); viewModel.gifError.value = "" }
    LaunchedEffect(query) {
        if (query.length < 2) { viewModel.gifResults.value = emptyList(); return@LaunchedEffect }
        delay(500)
        viewModel.searchGifs(query)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().fillMaxHeight(0.88f), contentAlignment = Alignment.BottomCenter) {
            Column(modifier = Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(ComicGlassBg)
                .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            ) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)
                    .width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookDark.copy(alpha = 0.3f)))

                // Header
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🇮", fontSize = 18.sp)
                        Text("GIF", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 22.sp, letterSpacing = 2.sp)
                        Text("SEARCH", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 2.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.7f)) }
                }

                // Quick category chips
                LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    val categories = listOf("Gaming","Retro","RPG","Anime","Memes","Hype","Win","Rage")
                    val icons      = listOf("🎮","🕹️","⚔️","🌸","😂","🔥","🏆","😤")
                    for (idx in categories.indices) {
                        item {
                            val cat = categories[idx]
                            val active = query.equals(cat, ignoreCase = true)
                            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (active) CGreen else Color.White.copy(alpha = 0.46f))
                                .border(1.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                                .clickable { query = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("${icons[idx]} $cat", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
                                    color = if (active) ScrapbookDark else ScrapbookDark.copy(alpha = 0.75f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Search bar
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.7f))
                    .border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Search, null, tint = ScrapbookDark.copy(alpha = 0.55f), modifier = Modifier.size(18.dp))
                    BasicTextField(value = query, onValueChange = { query = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 14.sp),
                        cursorBrush = SolidColor(ScrapbookDark),
                        decorationBox = { inner ->
                            Box { if (query.isEmpty()) Text("Search GIFs... e.g. Skyrim, Final Fantasy", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp); inner() }
                        }, modifier = Modifier.weight(1f))
                    if (isSearching) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else if (query.isNotEmpty()) Text("✕", color = ScrapbookDark.copy(alpha = 0.55f), modifier = Modifier.clickable { query = "" })
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Content
                when {
                    isSearching -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = CGreen, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Text("Searching...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.55f), fontSize = 13.sp)
                        }
                    }
                    results.isNotEmpty() -> LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results.chunked(2), key = { it.first().id }) { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (gif in row) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1.5f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                                        .clickable { onSelect(gif) }
                                    ) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                .data(gif.previewUrl).decoderFactory(coil.decode.GifDecoder.Factory()).crossfade(true).build(),
                                            contentDescription = gif.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                                        )
                                        Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                                            .padding(horizontal = 8.dp, vertical = 5.dp)) {
                                            if (gif.title.isNotBlank()) Text(gif.title, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                            Text("GIF", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp, letterSpacing = 0.5.sp)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    gifError.isNotBlank() -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠️", fontSize = 36.sp)
                            Text("Error: $gifError", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    query.length >= 2 -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("😶", fontSize = 36.sp)
                            Text("No GIFs found", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 13.sp)
                            Text("Try a different search", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.45f), fontSize = 11.sp)
                        }
                    }
                    else -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🎮", fontSize = 52.sp)
                            Text("FIND THE PERFECT GIF", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 20.sp, letterSpacing = 1.sp)
                            Text("Tap a category above or type anything", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Preview card ─────────────────────────────────────────────────────────────

@Composable
fun CheckpointPreviewCard(checkpoint: Checkpoint, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val glowAlpha by rememberGlowPhase(0.4f)
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cpPreviewPress")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cpPreviewShadow")

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(16.dp)).background(CGreen.copy(alpha = glowAlpha * 0.28f)))
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(16.dp)).background(CGreen))
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                .clickable { pressed = true; onClick() }
        ) {
            // Green stripe
            Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val thumbUrl = when {
                    checkpoint.imageUrl.isNotBlank() -> checkpoint.imageUrl
                    checkpoint.gifUrl.isNotBlank()   -> checkpoint.gifUrl
                    else                             -> null
                }
                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.08f)).border(1.dp, CGreen.copy(alpha = 0.30f), RoundedCornerShape(12.dp))) {
                    // Green stripe
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                        .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                    if (thumbUrl != null) AsyncImage(model = thumbUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(thumbUrl).fillMaxSize())
                    else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🏁", fontSize = 28.sp) }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(checkpoint.title.ifBlank { "Checkpoint" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold)
                    Text(checkpoint.authorUsername, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    if (checkpoint.body.isNotBlank()) {
                        Text(checkpoint.body, fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}


// ─── 🔥 HOT badge for popular checkpoints ─────────────────────────────────────

@Composable
fun CheckpointHotBadge() {
    val flicker by rememberGlowRange(-8f, 8f)
    val pulse by rememberGlowRange(1f, 1.08f)
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(RoundedCornerShape(8.dp))
            .background(ScrapbookDark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("🔥", fontSize = 12.sp, modifier = Modifier.graphicsLayer {
            rotationZ = flicker
            transformOrigin = TransformOrigin(0.5f, 1f)
        })
        Text("HOT", fontFamily = BangersFontFamily, color = CGreenMint, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

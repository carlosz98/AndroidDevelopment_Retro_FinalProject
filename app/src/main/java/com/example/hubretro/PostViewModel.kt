package com.example.hubretro

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

// ─── Data Models ──────────────────────────────────────────────────────────────

data class RetroPost(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorHandle: String = "",
    val authorPicUrl: String = "",
    val type: String = "text",
    val title: String = "",
    val content: String = "",
    val gameId: String = "",
    val gameName: String = "",
    val gameCoverUrl: String = "",
    val imageUrl: String = "",
    val gifUrl: String = "",
    val videoUrl: String = "",
    val rating: Int = 0,
    val ostName: String = "",
    val ostCoverUrl: String = "",
    val songTitle: String = "",
    val songArtist: String = "",
    val songPreviewUrl: String = "",
    val songArtworkUrl: String = "",
    val reactions: Map<String, Int> = emptyMap(),
    val userReactions: Map<String, String> = emptyMap(),
    val commentCount: Int = 0,
    val timestamp: Long = 0L,
    val moodTag: String = "",
    val musicTrack: String = "",
    val filter: String = "",
    val visibility: String = "public",
    val hashtags: List<String> = emptyList()
)

data class RetroStory(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorPicUrl: String = "",
    val authorHabboUrl: String = "",
    val type: String = "now_playing",
    val content: String = "",
    val imageUrl: String = "",
    val videoUrl: String = "",
    val gameName: String = "",
    val gameCoverUrl: String = "",
    val musicTrack: String = "",
    val filter: String = "",
    val sticker: String = "",
    val textOverlay: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val viewedBy: List<String> = emptyList()
)

data class PostComment(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorPicUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

// ─── RetroFilter ──────────────────────────────────────────────────────────────

enum class RetroFilter(val emoji: String, val label: String) {
    NONE("", "None"),
    SEPIA("🟤", "SEPIA"),
    VHS("📼", "VHS"),
    SCANLINE("📺", "SCANLINE"),
    NOIR("⬛", "NOIR"),
    WARM("🟠", "WARM")
}

fun RetroFilter.toColorFilter(): ColorFilter? = when (this) {
    RetroFilter.NONE     -> null
    RetroFilter.SEPIA    -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    RetroFilter.NOIR     -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    RetroFilter.VHS      -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.5f) })
    RetroFilter.WARM     -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        1.2f, 0f, 0f, 0f, 10f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 0.8f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )))
    RetroFilter.SCANLINE -> null
}

// ─── PostViewModel ────────────────────────────────────────────────────────────

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _posts = MutableStateFlow<List<RetroPost>>(emptyList())
    val posts: StateFlow<List<RetroPost>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<List<RetroStory>>(emptyList())
    val stories: StateFlow<List<RetroStory>> = _stories.asStateFlow()

    private val _myStory = MutableStateFlow<RetroStory?>(null)
    val myStory: StateFlow<RetroStory?> = _myStory.asStateFlow()

    private val _isLoadingPosts = MutableStateFlow(false)
    val isLoadingPosts: StateFlow<Boolean> = _isLoadingPosts.asStateFlow()

    private val _isLoadingStories = MutableStateFlow(false)
    val isLoadingStories: StateFlow<Boolean> = _isLoadingStories.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<UserProfileData?>(null)

    fun setCurrentUserProfile(profile: UserProfileData?) {
        Log.d("PostVM", "setCurrentUserProfile called: ${profile?.username}")
        _currentUserProfile.value = profile
    }

    // ─── Get or fetch profile ─────────────────────────────────────────────────
    // Critical fix — if profile wasn't set yet, fetch it directly from Firestore

    private suspend fun getOrFetchProfile(): UserProfileData? {
        val existing = _currentUserProfile.value
        if (existing != null) {
            Log.d("PostVM", "Profile already set: ${existing.username}")
            return existing
        }
        val uid = auth.currentUser?.uid ?: run {
            Log.e("PostVM", "getOrFetchProfile — no auth uid")
            return null
        }
        return try {
            Log.d("PostVM", "Fetching profile from Firestore for uid: $uid")
            val doc = firestore.collection("users").document(uid).get().await()
            val data = doc.data ?: run {
                Log.e("PostVM", "getOrFetchProfile — doc data is null")
                return null
            }
            val profile = UserProfileData(
                uid = uid,
                username = data["username"] as? String ?: "Retro User",
                userHandle = data["userHandle"] as? String ?: "@retrouser",
                bio = data["bio"] as? String ?: "",
                email = data["email"] as? String ?: "",
                profilePictureUrl = data["profilePictureUrl"] as? String ?: "",
                bannerUrl = data["bannerUrl"] as? String ?: "",
                followersCount = (data["followersCount"] as? Long)?.toInt() ?: 0,
                followingCount = (data["followingCount"] as? Long)?.toInt() ?: 0,
                setupComplete = data["setupComplete"] as? Boolean ?: false,
                topGames = (data["topGames"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                topSoundtracks = (data["topSoundtracks"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList(),
                location = data["location"] as? String ?: "",
                website = data["website"] as? String ?: "",
                createdAt = data["createdAt"] as? Long ?: 0L,
                psnUsername = data["psnUsername"] as? String ?: "",
                xboxUsername = data["xboxUsername"] as? String ?: "",
                steamUsername = data["steamUsername"] as? String ?: "",
                nintendoUsername = data["nintendoUsername"] as? String ?: "",
                twitchUsername = data["twitchUsername"] as? String ?: "",
                youtubeUsername = data["youtubeUsername"] as? String ?: "",
                habboUsername = data["habboUsername"] as? String ?: "",
                habboRegion = data["habboRegion"] as? String ?: "habbo.com"
            )
            _currentUserProfile.value = profile
            Log.d("PostVM", "Profile fetched successfully: ${profile.username}")
            profile
        } catch (e: Exception) {
            Log.e("PostVM", "getOrFetchProfile failed: ${e.message}")
            null
        }
    }

    // ─── Upload media helper ──────────────────────────────────────────────────

    private suspend fun uploadMedia(uri: Uri, folder: String, uid: String, isVideo: Boolean): String {
        return try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null) {
                Log.e("PostVM", "uploadMedia — bytes null for uri: $uri")
                return ""
            }
            val ext = if (isVideo) "mp4" else "jpg"
            val ref = storage.reference.child("$folder/$uid/${System.currentTimeMillis()}.$ext")
            Log.d("PostVM", "Uploading ${bytes.size} bytes to ${ref.path}")
            ref.putBytes(bytes).await()
            val url = ref.downloadUrl.await().toString()
            Log.d("PostVM", "Upload successful: $url")
            url
        } catch (e: Exception) {
            Log.e("PostVM", "uploadMedia failed: ${e.message}")
            ""
        }
    }

    // ─── Fetch Feed Posts ─────────────────────────────────────────────────────

    fun fetchFeedPosts(followingUids: Set<String>) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoadingPosts.value = true
            try {
                val allUids = followingUids + currentUid
                val fetched = mutableListOf<RetroPost>()

                allUids.chunked(10).forEach { chunk ->
                    try {
                        val docs = firestore.collection("posts")
                            .whereIn("authorUid", chunk)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(20)
                            .get().await()
                        fetched += docs.documents.mapNotNull { doc ->
                            doc.data?.toRetroPost(doc.id)
                        }
                    } catch (e: Exception) {
                        Log.e("PostVM", "Chunk fetch failed: ${e.message}")
                    }
                }

                if (fetched.size < 5) {
                    try {
                        val publicDocs = firestore.collection("posts")
                            .whereEqualTo("visibility", "public")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(20)
                            .get().await()
                        fetched += publicDocs.documents.mapNotNull { doc ->
                            doc.data?.toRetroPost(doc.id)
                        }
                    } catch (e: Exception) {
                        Log.e("PostVM", "Public posts fetch failed: ${e.message}")
                    }
                }

                _posts.value = fetched
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e("PostVM", "fetchFeedPosts failed: ${e.message}")
            }
            _isLoadingPosts.value = false
        }
    }

    // ─── Fetch Stories ────────────────────────────────────────────────────────

    fun fetchStories(followingUids: Set<String>) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoadingStories.value = true
            try {
                val now = System.currentTimeMillis()

                try {
                    val myStoryDocs = firestore.collection("users")
                        .document(currentUid)
                        .collection("stories")
                        .whereGreaterThan("expiresAt", now)
                        .orderBy("expiresAt", Query.Direction.DESCENDING)
                        .limit(1).get().await()
                    _myStory.value = myStoryDocs.documents.firstOrNull()?.let { doc ->
                        doc.data?.toRetroStory(doc.id)
                    }
                } catch (e: Exception) {
                    Log.e("PostVM", "fetchMyStory failed: ${e.message}")
                }

                val followingStories = mutableListOf<RetroStory>()
                followingUids.forEach { uid ->
                    try {
                        val docs = firestore.collection("users")
                            .document(uid)
                            .collection("stories")
                            .whereGreaterThan("expiresAt", now)
                            .orderBy("expiresAt", Query.Direction.DESCENDING)
                            .limit(1).get().await()
                        docs.documents.firstOrNull()?.let { doc ->
                            doc.data?.toRetroStory(doc.id)?.let { followingStories.add(it) }
                        }
                    } catch (e: Exception) {
                        Log.e("PostVM", "fetchStory for $uid failed: ${e.message}")
                    }
                }
                _stories.value = followingStories.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e("PostVM", "fetchStories failed: ${e.message}")
            }
            _isLoadingStories.value = false
        }
    }

    // ─── Create Post ──────────────────────────────────────────────────────────

    fun createPost(
        type: String,
        title: String = "",
        content: String,
        gameName: String = "",
        gameCoverUrl: String = "",
        rating: Int = 0,
        ostName: String = "",
        ostCoverUrl: String = "",
        moodTag: String = "",
        musicTrack: String = "",
        songTitle: String = "",
        songArtist: String = "",
        songPreviewUrl: String = "",
        songArtworkUrl: String = "",
        gifUrl: String = "",
        filter: String = "",
        hashtags: List<String> = emptyList(),
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        visibility: String = "public",
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentUid = auth.currentUser?.uid ?: run {
            Log.e("PostVM", "createPost FAILED — no auth uid")
            onComplete(false)
            return
        }

        // 🛡️ Moderation on everything the user typed
        val safe = Moderation.gateAll(title, content, hashtags.joinToString(" "), where = "post") ?: run {
            onComplete(false); return
        }
        val title = safe[0]
        val content = safe[1]
        val hashtags = safe[2].split(" ").filter { it.isNotBlank() }

        viewModelScope.launch {
            _isPosting.value = true
            try {
                // Always get profile — fetch from Firestore if not set
                val profile = getOrFetchProfile() ?: run {
                    Log.e("PostVM", "createPost FAILED — could not get profile")
                    onComplete(false)
                    _isPosting.value = false
                    return@launch
                }

                Log.d("PostVM", "Creating post as: ${profile.username}")

                // Upload image if provided
                var uploadedImageUrl = ""
                if (imageUri != null) {
                    uploadedImageUrl = uploadMedia(imageUri, "posts", currentUid, false)
                }

                // Upload video if provided
                var uploadedVideoUrl = ""
                if (videoUri != null) {
                    uploadedVideoUrl = uploadMedia(videoUri, "posts/videos", currentUid, true)
                }

                val postId = firestore.collection("posts").document().id
                val post = mapOf(
                    "id" to postId,
                    "authorUid" to currentUid,
                    "authorUsername" to profile.username,
                    "authorHandle" to profile.userHandle,
                    "authorPicUrl" to profile.profilePictureUrl,
                    "type" to type,
                    "title" to title.trim(),
                    "content" to content,
                    "gameName" to gameName,
                    "gameCoverUrl" to gameCoverUrl,
                    "imageUrl" to uploadedImageUrl,
                    "gifUrl" to gifUrl,
                    "videoUrl" to uploadedVideoUrl,
                    "rating" to rating,
                    "ostName" to ostName,
                    "ostCoverUrl" to ostCoverUrl,
                    "songTitle" to songTitle,
                    "songArtist" to songArtist,
                    "songPreviewUrl" to songPreviewUrl,
                    "songArtworkUrl" to songArtworkUrl,
                    "moodTag" to moodTag,
                    "musicTrack" to musicTrack,
                    "filter" to filter,
                    "hashtags" to hashtags,
                    "visibility" to "public",
                    "reactions" to mapOf("🔥" to 0, "❤️" to 0, "🕹️" to 0, "👾" to 0),
                    "commentCount" to 0,
                    "timestamp" to System.currentTimeMillis()
                )

                Log.d("PostVM", "Saving post to Firestore: $postId")
                firestore.collection("posts").document(postId).set(post).await()
                Log.d("PostVM", "Post saved successfully!")

                val newPost = post.toRetroPost(postId)
                _posts.value = listOf(newPost) + _posts.value
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PostVM", "createPost failed: ${e.message}", e)
                onComplete(false)
            }
            _isPosting.value = false
        }
    }

    // ─── Create Story ─────────────────────────────────────────────────────────

    fun createStory(
        type: String,
        content: String,
        gameName: String = "",
        gameCoverUrl: String = "",
        musicTrack: String = "",
        filter: String = "",
        sticker: String = "",
        textOverlay: String = "",
        imageUri: Uri? = null,
        videoUri: Uri? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val currentUid = auth.currentUser?.uid ?: run {
            Log.e("PostVM", "createStory FAILED — no auth uid")
            onComplete(false)
            return
        }

        val safeStory = Moderation.gateAll(content, textOverlay, where = "story") ?: run {
            onComplete(false); return
        }
        val content = safeStory[0]
        val textOverlay = safeStory[1]

        viewModelScope.launch {
            try {
                // Always get profile — fetch from Firestore if not set
                val profile = getOrFetchProfile() ?: run {
                    Log.e("PostVM", "createStory FAILED — could not get profile")
                    onComplete(false)
                    return@launch
                }

                Log.d("PostVM", "Creating story as: ${profile.username}")

                var uploadedImageUrl = ""
                if (imageUri != null) {
                    uploadedImageUrl = uploadMedia(imageUri, "stories", currentUid, false)
                }

                var uploadedVideoUrl = ""
                if (videoUri != null) {
                    uploadedVideoUrl = uploadMedia(videoUri, "stories/videos", currentUid, true)
                }

                val now = System.currentTimeMillis()
                val storyId = firestore.collection("users")
                    .document(currentUid)
                    .collection("stories")
                    .document().id

                val story = mapOf(
                    "id" to storyId,
                    "authorUid" to currentUid,
                    "authorUsername" to profile.username,
                    "authorPicUrl" to profile.profilePictureUrl,
                    "authorHabboUrl" to if (profile.habboUsername.isNotBlank())
                        "https://www.${profile.habboRegion}/habbo-imaging/avatarimage?user=${profile.habboUsername}&action=std&direction=2&head_direction=2&size=l&gesture=sml"
                    else "",
                    "type" to type,
                    "content" to content,
                    "gameName" to gameName,
                    "gameCoverUrl" to gameCoverUrl,
                    "imageUrl" to uploadedImageUrl,
                    "videoUrl" to uploadedVideoUrl,
                    "musicTrack" to musicTrack,
                    "filter" to filter,
                    "sticker" to sticker,
                    "textOverlay" to textOverlay,
                    "timestamp" to now,
                    "expiresAt" to (now + 24 * 60 * 60 * 1000L),
                    "viewedBy" to emptyList<String>()
                )

                Log.d("PostVM", "Saving story to Firestore: $storyId")
                firestore.collection("users").document(currentUid)
                    .collection("stories").document(storyId).set(story).await()
                Log.d("PostVM", "Story saved successfully!")

                _myStory.value = story.toRetroStory(storyId)
                onComplete(true)
            } catch (e: Exception) {
                Log.e("PostVM", "createStory failed: ${e.message}", e)
                onComplete(false)
            }
        }
    }

    // ─── React to Post ────────────────────────────────────────────────────────

    fun reactToPost(postId: String, emoji: String) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val postRef = firestore.collection("posts").document(postId)
                val doc = postRef.get().await()
                val data = doc.data ?: return@launch
                val reactions = (data["reactions"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                val userReactions = (data["userReactions"] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                val prevReaction = userReactions[currentUid] as? String

                if (prevReaction == emoji) {
                    reactions[emoji] = ((reactions[emoji] as? Long)?.toInt() ?: 1) - 1
                    userReactions.remove(currentUid)
                } else {
                    if (prevReaction != null) {
                        reactions[prevReaction] = ((reactions[prevReaction] as? Long)?.toInt() ?: 1) - 1
                    }
                    reactions[emoji] = ((reactions[emoji] as? Long)?.toInt() ?: 0) + 1
                    userReactions[currentUid] = emoji
                }

                postRef.update(mapOf(
                    "reactions" to reactions,
                    "userReactions" to userReactions
                )).await()

                _posts.value = _posts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            reactions = reactions.mapKeys { it.key.toString() }
                                .mapValues { (it.value as? Long)?.toInt() ?: it.value as? Int ?: 0 },
                            userReactions = userReactions.mapKeys { it.key.toString() }
                                .mapValues { it.value.toString() }
                        )
                    } else post
                }
            } catch (e: Exception) {
                Log.e("PostVM", "reactToPost failed: ${e.message}")
            }
        }
    }

    // ─── Delete Post ──────────────────────────────────────────────────────────

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts").document(postId).delete().await()
                _posts.value = _posts.value.filter { it.id != postId }
                Log.d("PostVM", "Post deleted: $postId")
            } catch (e: Exception) {
                Log.e("PostVM", "deletePost failed: ${e.message}")
            }
        }
    }

    // ─── Mark Story Viewed ────────────────────────────────────────────────────

    fun markStoryViewed(storyAuthorUid: String, storyId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(storyAuthorUid)
                    .collection("stories").document(storyId)
                    .update("viewedBy", FieldValue.arrayUnion(currentUid)).await()
            } catch (e: Exception) {
                Log.e("PostVM", "markStoryViewed failed: ${e.message}")
            }
        }
    }

    // ─── Refresh feed ─────────────────────────────────────────────────────────

    fun refreshFeed(followingUids: Set<String>) {
        fetchFeedPosts(followingUids)
        fetchStories(followingUids)
    }

    // ─── Parse Helpers ────────────────────────────────────────────────────────

    private fun Map<String, Any>.toRetroPost(id: String): RetroPost {
        return RetroPost(
            id = id,
            authorUid = this["authorUid"] as? String ?: "",
            authorUsername = this["authorUsername"] as? String ?: "",
            authorHandle = this["authorHandle"] as? String ?: "",
            authorPicUrl = this["authorPicUrl"] as? String ?: "",
            type = this["type"] as? String ?: "text",
            title = this["title"] as? String ?: "",
            content = this["content"] as? String ?: "",
            gameName = this["gameName"] as? String ?: "",
            gameCoverUrl = this["gameCoverUrl"] as? String ?: "",
            imageUrl = this["imageUrl"] as? String ?: "",
            gifUrl = this["gifUrl"] as? String ?: "",
            videoUrl = this["videoUrl"] as? String ?: "",
            rating = (this["rating"] as? Long)?.toInt() ?: 0,
            ostName = this["ostName"] as? String ?: "",
            ostCoverUrl = this["ostCoverUrl"] as? String ?: "",
            songTitle = this["songTitle"] as? String ?: "",
            songArtist = this["songArtist"] as? String ?: "",
            songPreviewUrl = this["songPreviewUrl"] as? String ?: "",
            songArtworkUrl = this["songArtworkUrl"] as? String ?: "",
            reactions = (this["reactions"] as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { (it.value as? Long)?.toInt() ?: 0 } ?: emptyMap(),
            userReactions = (this["userReactions"] as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value.toString() } ?: emptyMap(),
            commentCount = (this["commentCount"] as? Long)?.toInt() ?: 0,
            timestamp = this["timestamp"] as? Long ?: 0L,
            moodTag = this["moodTag"] as? String ?: "",
            musicTrack = this["musicTrack"] as? String ?: "",
            filter = this["filter"] as? String ?: "",
            visibility = this["visibility"] as? String ?: "public",
            hashtags = (this["hashtags"] as? List<*>)?.map { it.toString() } ?: emptyList()
        )
    }

    private fun Map<String, Any>.toRetroStory(id: String): RetroStory {
        return RetroStory(
            id = id,
            authorUid = this["authorUid"] as? String ?: "",
            authorUsername = this["authorUsername"] as? String ?: "",
            authorPicUrl = this["authorPicUrl"] as? String ?: "",
            authorHabboUrl = this["authorHabboUrl"] as? String ?: "",
            type = this["type"] as? String ?: "text",
            content = this["content"] as? String ?: "",
            imageUrl = this["imageUrl"] as? String ?: "",
            videoUrl = this["videoUrl"] as? String ?: "",
            gameName = this["gameName"] as? String ?: "",
            gameCoverUrl = this["gameCoverUrl"] as? String ?: "",
            musicTrack = this["musicTrack"] as? String ?: "",
            filter = this["filter"] as? String ?: "",
            sticker = this["sticker"] as? String ?: "",
            textOverlay = this["textOverlay"] as? String ?: "",
            timestamp = this["timestamp"] as? Long ?: 0L,
            expiresAt = this["expiresAt"] as? Long ?: 0L,
            viewedBy = (this["viewedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        )
    }
}
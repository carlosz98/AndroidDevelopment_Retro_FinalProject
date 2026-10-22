package com.example.hubretro

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════════════════
// Profile, upgraded (scrapbook style)
//   1. Posts grid (3 columns) with a full post view
//   2. Story highlights (saved stories that don't expire)
//   3. Pinned post
//   4. "Now playing" status (auto from the album player, or set by hand)
//   5. Mutual followers + games you both like (other people's profiles)
//   6. Share a retro player card image
//   7. Activity calendar (days you posted)
//   8. Quick edit: photo, banner and status buttons in the editor
//
// Everything new lives on the user's own document (statusText, pinnedPostId,
// highlights), which the owner can already update, so no new collections.
// ═══════════════════════════════════════════════════════════════════════════════

data class ProfileHighlight(val id: String, val title: String, val coverUrl: String, val stories: List<RetroStory>)

data class ProfileExtras(
    val statusText: String = "",
    val pinnedPostId: String = "",
    val highlights: List<ProfileHighlight> = emptyList()
)

// ─── Data ─────────────────────────────────────────────────────────────────────

private fun Map<String, Any>.toProfilePost(id: String): RetroPost = RetroPost(
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
    rating = (this["rating"] as? Long)?.toInt() ?: 0,
    songTitle = this["songTitle"] as? String ?: "",
    songArtist = this["songArtist"] as? String ?: "",
    songPreviewUrl = this["songPreviewUrl"] as? String ?: "",
    songArtworkUrl = this["songArtworkUrl"] as? String ?: "",
    reactions = (this["reactions"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { (it.value as? Long)?.toInt() ?: 0 } ?: emptyMap(),
    userReactions = (this["userReactions"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap(),
    commentCount = (this["commentCount"] as? Long)?.toInt() ?: 0,
    timestamp = this["timestamp"] as? Long ?: 0L,
    moodTag = this["moodTag"] as? String ?: "",
    filter = this["filter"] as? String ?: "",
    visibility = this["visibility"] as? String ?: "public",
    hashtags = (this["hashtags"] as? List<*>)?.map { it.toString() } ?: emptyList()
)

private fun RetroStory.toMap(): Map<String, Any> = mapOf(
    "id" to id, "authorUid" to authorUid, "authorUsername" to authorUsername, "authorPicUrl" to authorPicUrl,
    "type" to type, "content" to content, "imageUrl" to imageUrl, "gameName" to gameName, "gameCoverUrl" to gameCoverUrl,
    "musicTrack" to musicTrack, "filter" to filter, "sticker" to sticker, "textOverlay" to textOverlay, "timestamp" to timestamp
)

private fun Map<*, *>.toStory(): RetroStory = RetroStory(
    id = this["id"] as? String ?: "", authorUid = this["authorUid"] as? String ?: "",
    authorUsername = this["authorUsername"] as? String ?: "", authorPicUrl = this["authorPicUrl"] as? String ?: "",
    type = this["type"] as? String ?: "now_playing", content = this["content"] as? String ?: "",
    imageUrl = this["imageUrl"] as? String ?: "", gameName = this["gameName"] as? String ?: "",
    gameCoverUrl = this["gameCoverUrl"] as? String ?: "", musicTrack = this["musicTrack"] as? String ?: "",
    filter = this["filter"] as? String ?: "", sticker = this["sticker"] as? String ?: "",
    textOverlay = this["textOverlay"] as? String ?: "", timestamp = this["timestamp"] as? Long ?: 0L
)

object ProfileData {
    private val db get() = FirebaseFirestore.getInstance()

    /** A user's posts, newest first (sorted here so no extra Firestore index is needed). */
    suspend fun posts(uid: String): List<RetroPost> = try {
        db.collection("posts").whereEqualTo("authorUid", uid).limit(60).get().await()
            .documents.mapNotNull { d -> d.data?.toProfilePost(d.id) }
            .filter { it.visibility != "private" || it.authorUid == FirebaseAuth.getInstance().currentUser?.uid }
            .sortedByDescending { it.timestamp }
    } catch (_: Exception) { emptyList() }

    suspend fun extras(uid: String): ProfileExtras = try {
        val d = db.collection("users").document(uid).get().await()
        val hl = (d.get("highlights") as? List<*>)?.mapNotNull { raw ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            ProfileHighlight(
                id = m["id"] as? String ?: "",
                title = m["title"] as? String ?: "",
                coverUrl = m["coverUrl"] as? String ?: "",
                stories = (m["stories"] as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.toStory() } ?: emptyList()
            )
        } ?: emptyList()
        ProfileExtras(d.getString("statusText") ?: "", d.getString("pinnedPostId") ?: "", hl)
    } catch (_: Exception) { ProfileExtras() }

    suspend fun setStatus(text: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try { db.collection("users").document(uid).update(mapOf("statusText" to text.take(60), "statusUpdatedAt" to System.currentTimeMillis())).await() } catch (_: Exception) { }
    }

    suspend fun setPinnedPost(postId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try { db.collection("users").document(uid).update("pinnedPostId", postId).await() } catch (_: Exception) { }
    }

    /** Adds a story to a highlight (creates the highlight when the title is new). */
    suspend fun addToHighlight(title: String, story: RetroStory, current: List<ProfileHighlight>): List<ProfileHighlight> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return current
        val clean = title.trim().take(20).ifBlank { "Highlights" }
        val existing = current.firstOrNull { it.title.equals(clean, true) }
        val cover = story.imageUrl.ifBlank { story.gameCoverUrl }
        val updated = if (existing != null) {
            current.map { if (it.id == existing.id && it.stories.none { s -> s.id == story.id }) it.copy(stories = it.stories + story) else it }
        } else current + ProfileHighlight(id = "hl_${System.currentTimeMillis()}", title = clean, coverUrl = cover, stories = listOf(story))
        val payload = updated.map { h -> mapOf("id" to h.id, "title" to h.title, "coverUrl" to h.coverUrl, "stories" to h.stories.map { it.toMap() }) }
        try { db.collection("users").document(uid).update("highlights", payload).await() } catch (_: Exception) { return current }
        return updated
    }

    suspend fun removeHighlight(id: String, current: List<ProfileHighlight>): List<ProfileHighlight> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return current
        val updated = current.filter { it.id != id }
        val payload = updated.map { h -> mapOf("id" to h.id, "title" to h.title, "coverUrl" to h.coverUrl, "stories" to h.stories.map { it.toMap() }) }
        try { db.collection("users").document(uid).update("highlights", payload).await() } catch (_: Exception) { return current }
        return updated
    }

    /** Names of people you follow who also follow this user. */
    suspend fun mutualFollowers(targetUid: String, myFollowing: Set<String>): List<String> {
        if (myFollowing.isEmpty()) return emptyList()
        return try {
            val followerIds = db.collection("follows").whereEqualTo("followingId", targetUid).limit(300).get().await()
                .documents.mapNotNull { it.getString("followerId") }.filter { it in myFollowing }
            followerIds.take(10).mapNotNull { id -> try { db.collection("users").document(id).get().await().getString("username") } catch (_: Exception) { null } }
                .filter { it.isNotBlank() }
        } catch (_: Exception) { emptyList() }
    }
}

/** The album player keeps your status fresh ("🎵 Listening to …"). */
object NowPlayingStatus {
    private var last = ""
    fun listening(title: String, artist: String) {
        val text = "🎵 Listening to ${title.take(40)}" + if (artist.isNotBlank()) " · ${artist.take(20)}" else ""
        if (title.isBlank() || text == last) return
        last = text
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch { ProfileData.setStatus(text) }
    }
}

// ─── 4. Status line ───────────────────────────────────────────────────────────

@Composable
fun ProfileStatusLine(text: String, isOwner: Boolean, onEdit: () -> Unit = {}) {
    if (text.isBlank() && !isOwner) return
    Row(
        modifier = Modifier
            .drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = size, cornerRadius = CornerRadius(14.dp.toPx())) }
            .clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
            .then(if (isOwner) Modifier.clickable { onEdit() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PulsingDot(color = CGreen, size = 7.dp)
        Text(
            text.ifBlank { "Set your status: what are you playing?" },
            fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold,
            color = if (text.isBlank()) ScrapbookTextMuted else ScrapbookDark, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusEditorDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    val quick = listOf("🎮 Playing ", "🎵 Listening to ", "📖 Reading ", "🏆 Grinding ", "😴 Away")
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("YOUR STATUS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quick) { q ->
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(ComicGlassBg).border(1.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                        .clickable { text = q }.padding(horizontal = 8.dp, vertical = 4.dp)) { Text(q.trim(), fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 12.sp) }
                }
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).padding(10.dp)) {
                BasicTextField(value = text, onValueChange = { if (it.length <= 60) text = it }, singleLine = true,
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScrapbookDark),
                    cursorBrush = SolidColor(ScrapbookDark), modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> if (text.isEmpty()) Text("🎮 Playing Chrono Trigger", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp); inner() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                    .clickable { onSave("") }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text("CLEAR", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp) }
                Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                    .clickable { onSave(text.trim()) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text("SAVE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp) }
            }
        }
    }
}

// ─── 2. Highlights ────────────────────────────────────────────────────────────

@Composable
fun ProfileHighlightsRow(highlights: List<ProfileHighlight>, isOwner: Boolean, onOpen: (ProfileHighlight) -> Unit, onLongPressRemove: (ProfileHighlight) -> Unit = {}) {
    if (highlights.isEmpty() && !isOwner) return
    LazyRow(contentPadding = PaddingValues(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(highlights, key = { it.id }) { h ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(68.dp)) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(ComicGlassBgAlt).border(2.5.dp, ScrapbookDark, CircleShape).padding(3.dp)
                        .clip(CircleShape).border(2.dp, CGreen, CircleShape).clickable { onOpen(h) },
                    contentAlignment = Alignment.Center
                ) {
                    if (h.coverUrl.isNotBlank()) AsyncImage(model = h.coverUrl, contentDescription = h.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Text(storyTypeEmoji(h.stories.firstOrNull()?.type ?: ""), fontSize = 24.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(h.title, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (isOwner) Text("remove", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 9.sp, modifier = Modifier.clickable { onLongPressRemove(h) })
            }
        }
        if (isOwner && highlights.isEmpty()) {
            item(key = "hl_hint") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(180.dp)) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White).border(2.dp, ScrapbookDark.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = ScrapbookTextMuted)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Open your story and tap ★ HIGHLIGHT to keep it here", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun AddToHighlightDialog(existing: List<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("ADD TO HIGHLIGHT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp)
            existing.forEach { t ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                    .clickable { onPick(t) }.padding(10.dp)) { Text("★ $t", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp) }
            }
            Text("NEW HIGHLIGHT", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 13.sp)
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ComicGlassBg).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).padding(10.dp)) {
                BasicTextField(value = name, onValueChange = { if (it.length <= 20) name = it }, singleLine = true,
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScrapbookDark),
                    cursorBrush = SolidColor(ScrapbookDark), modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner -> if (name.isEmpty()) Text("PS2 Era, Speedruns, Collection…", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp); inner() })
            }
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (name.isNotBlank()) CGreen else Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                .clickable(enabled = name.isNotBlank()) { onPick(name.trim()) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text("CREATE + ADD", fontFamily = BangersFontFamily, color = if (name.isNotBlank()) ScrapbookDark else ScrapbookTextMuted, fontSize = 15.sp)
            }
        }
    }
}

// ─── 1 + 3. Posts grid, pinned post, full post view ───────────────────────────

@Composable
fun ProfilePostsGrid(posts: List<RetroPost>, pinnedPostId: String, isLoading: Boolean, emptyText: String, onOpen: (RetroPost) -> Unit) {
    val context = LocalContext.current
    when {
        isLoading -> Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { ShimmerCard(modifier = Modifier.weight(1f).aspectRatio(1f)) }
        }
        posts.isEmpty() -> Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp)).padding(18.dp), contentAlignment = Alignment.Center) {
            Text(emptyText, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        else -> {
            val ordered = posts.sortedByDescending { it.id == pinnedPostId }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ordered.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { p ->
                            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(ComicGlassBgAlt).border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp)).clickable { onOpen(p) }) {
                                when {
                                    p.gifUrl.isNotBlank() -> AsyncImage(model = ImageRequest.Builder(context).data(p.gifUrl).decoderFactory(GifDecoder.Factory()).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    p.imageUrl.isNotBlank() -> AsyncImage(model = p.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    p.gameCoverUrl.isNotBlank() -> AsyncImage(model = p.gameCoverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    else -> Box(Modifier.fillMaxSize().background(Color.White).padding(8.dp), contentAlignment = Alignment.Center) {
                                        Text(p.title.ifBlank { p.content }.take(70), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                                    }
                                }
                                if (p.id == pinnedPostId) {
                                    Box(Modifier.align(Alignment.TopStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(CAcYellow).border(1.dp, ScrapbookDark, RoundedCornerShape(6.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                        Text("📌", fontSize = 10.sp)
                                    }
                                }
                                val total = p.reactions.values.sum()
                                if (total > 0) {
                                    Box(Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                        Text("🔥 $total", fontFamily = BangersFontFamily, color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfilePostDialog(post: RetroPost, currentUid: String, isOwner: Boolean, isPinned: Boolean, postViewModel: PostViewModel, onPin: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.96f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (isOwner) {
                    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (isPinned) CAcYellow else Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                        .clickable { onPin() }.padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text(if (isPinned) "📌 UNPIN" else "📌 PIN TO PROFILE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp)
                    }
                } else Spacer(Modifier.width(1.dp))
                Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White).border(2.dp, ScrapbookDark, CircleShape).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                }
            }
            PostCard(post = post, currentUserId = currentUid, postViewModel = postViewModel)
        }
    }
}

// ─── 7. Activity calendar ─────────────────────────────────────────────────────

@Composable
fun ActivityCalendar(timestamps: List<Long>, weeks: Int = 15) {
    val dayMs = 86_400_000L
    val counts = remember(timestamps) { timestamps.groupingBy { it / dayMs }.eachCount() }
    val today = System.currentTimeMillis() / dayMs
    // Align so the last column ends on today; rows = Sun..Sat
    val cal = Calendar.getInstance()
    val todayDow = cal.get(Calendar.DAY_OF_WEEK) - 1
    val start = today - (weeks - 1) * 7 - todayDow
    val activeDays = counts.keys.count { it >= start }
    var streak = 0
    var d = if (counts.containsKey(today)) today else today - 1   // today not posted yet still keeps yesterday's streak
    while (counts.containsKey(d)) { streak++; d-- }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🔥 $streak-DAY STREAK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    Text("$activeDays active days", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(weeks) { w ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(7) { dow ->
                                val day = start + w * 7 + dow
                                val c = counts[day] ?: 0
                                val color = when {
                                    day > today -> Color.Transparent
                                    c == 0 -> ScrapbookDark.copy(alpha = 0.07f)
                                    c == 1 -> CGreenMint
                                    c == 2 -> CGreen
                                    else -> CGreenDeep
                                }
                                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(2.dp)).background(color))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Less", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    listOf(ScrapbookDark.copy(alpha = 0.07f), CGreenMint, CGreen, CGreenDeep).forEach { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(it)) }
                    Text("More · days you posted", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

// ─── 5. Mutuals + shared games ────────────────────────────────────────────────

@Composable
fun MutualsAndSharedGames(targetUid: String, myFollowing: Set<String>, myGames: List<String>, theirGames: List<String>) {
    var mutuals by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(targetUid, myFollowing.size) { mutuals = ProfileData.mutualFollowers(targetUid, myFollowing) }
    val shared = remember(myGames, theirGames) {
        val mine = myGames.map { it.lowercase().trim() }.toSet()
        theirGames.filter { it.lowercase().trim() in mine }
    }
    if (mutuals.isEmpty() && shared.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (mutuals.isNotEmpty()) {
            val text = when (mutuals.size) {
                1 -> "Followed by ${mutuals[0]}"
                2 -> "Followed by ${mutuals[0]} and ${mutuals[1]}"
                else -> "Followed by ${mutuals[0]}, ${mutuals[1]} and ${mutuals.size - 2} other${if (mutuals.size - 2 > 1) "s" else ""} you know"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👥", fontSize = 14.sp)
                Text(text, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 12.sp)
            }
        }
        if (shared.isNotEmpty()) {
            Text("🎮 GAMES YOU BOTH LIKE", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(shared) { g ->
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(g, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ─── 6. Share player card ─────────────────────────────────────────────────────

@Composable
fun ProfileShareButton(profile: UserProfileData?, xp: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Box(
        modifier.drawBehind { drawRoundRect(ScrapbookDark, topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = size, cornerRadius = CornerRadius(10.dp.toPx())) }
            .clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
            .clickable(enabled = !busy && profile != null) {
                busy = true
                scope.launch { sharePlayerCard(context, profile!!, xp); busy = false }
            }.padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(if (busy) "MAKING CARD…" else "📤 SHARE PLAYER CARD", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 1.sp) }
}

/** Draws a 1080×1350 retro player card, saves it to Pictures/RetroHub and opens the share sheet. */
suspend fun sharePlayerCard(context: Context, profile: UserProfileData, xp: Int) {
    val bmp = withContext(Dispatchers.Default) { Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888) }
    val avatar: Bitmap? = if (profile.profilePictureUrl.isNotBlank()) try {
        val res = ImageLoader(context).execute(ImageRequest.Builder(context).data(profile.profilePictureUrl).allowHardware(false).size(360).build())
        (res as? SuccessResult)?.drawable?.toBitmap(360, 360)
    } catch (_: Exception) { null } else null

    withContext(Dispatchers.Default) {
        val c = android.graphics.Canvas(bmp)
        val ink = ScrapbookDark.toArgb(); val green = CGreen.toArgb(); val mint = CGreenMint.toArgb(); val page = ComicGlassBg.toArgb()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bangers = try { ResourcesCompat.getFont(context, R.font.bangers_regular) } catch (_: Exception) { null } ?: Typeface.DEFAULT_BOLD
        val nunito = try { ResourcesCompat.getFont(context, R.font.nunito_bold) } catch (_: Exception) { null } ?: Typeface.DEFAULT_BOLD
        c.drawColor(page)
        // halftone dots
        p.color = (ink and 0x00FFFFFF) or (0x18 shl 24)
        var y = 20f; while (y < 1350f) { var x = 20f; while (x < 1080f) { c.drawCircle(x, y, 3f, p); x += 36f }; y += 36f }
        // card
        val card = RectF(70f, 90f, 1010f, 1260f)
        p.color = ink; c.drawRoundRect(RectF(card.left + 18, card.top + 18, card.right + 18, card.bottom + 18), 48f, 48f, p)
        p.color = 0xFFFFFFFF.toInt(); c.drawRoundRect(card, 48f, 48f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 10f; p.color = ink; c.drawRoundRect(card, 48f, 48f, p); p.style = Paint.Style.FILL
        // header band
        p.color = green; c.drawRect(RectF(card.left + 5, card.top + 5, card.right - 5, card.top + 150), p)
        p.color = ink; p.typeface = bangers; p.textSize = 84f; p.textAlign = Paint.Align.CENTER
        c.drawText("RETROHUB PLAYER CARD", 540f, card.top + 110, p)
        // avatar
        val cx = 540f; val cy = 470f; val r = 165f
        p.color = ink; c.drawCircle(cx + 10, cy + 10, r + 12, p)
        p.color = mint; c.drawCircle(cx, cy, r + 12, p)
        if (avatar != null) {
            val shader = android.graphics.BitmapShader(avatar, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
            val m = android.graphics.Matrix(); val s = (2 * r) / avatar.width; m.setScale(s, s); m.postTranslate(cx - r, cy - r); shader.setLocalMatrix(m)
            val ap = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }; c.drawCircle(cx, cy, r, ap)
        } else {
            p.color = 0xFFFFFFFF.toInt(); c.drawCircle(cx, cy, r, p)
            p.color = ink; p.textSize = 170f; c.drawText(profile.username.take(1).uppercase(), cx, cy + 60, p)
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = 9f; p.color = ink; c.drawCircle(cx, cy, r + 12, p); p.style = Paint.Style.FILL
        // name + handle
        p.color = ink; p.typeface = bangers; p.textSize = 104f; c.drawText(profile.username.uppercase().take(16), 540f, 760f, p)
        p.typeface = nunito; p.textSize = 40f; p.color = 0xFF555555.toInt(); c.drawText(profile.userHandle.ifBlank { "@${profile.username}" }, 540f, 815f, p)
        // level pill
        val level = getRetroLevel(xp)
        val pill = RectF(250f, 860f, 830f, 950f)
        p.color = ink; c.drawRoundRect(RectF(pill.left + 8, pill.top + 8, pill.right + 8, pill.bottom + 8), 45f, 45f, p)
        p.color = green; c.drawRoundRect(pill, 45f, 45f, p)
        p.color = ink; p.typeface = bangers; p.textSize = 58f; c.drawText("LV ${level.level} · ${level.title.uppercase()} · $xp XP", 540f, 925f, p)
        // top games
        val games = profile.topGames.mapNotNull { it["name"]?.toString() }.filter { it.isNotBlank() }.take(3)
        p.typeface = bangers; p.textSize = 50f; p.color = ink; c.drawText("TOP GAMES", 540f, 1030f, p)
        p.typeface = nunito; p.textSize = 40f; p.color = 0xFF333333.toInt()
        games.ifEmpty { listOf("Still choosing…") }.forEachIndexed { i, g -> c.drawText("${i + 1}. ${g.take(30)}", 540f, 1090f + i * 52f, p) }
        // footer
        p.typeface = nunito; p.textSize = 34f; p.color = ink
        c.drawText("Find me on RetroHub · ${profile.userHandle.ifBlank { profile.username }}", 540f, 1320f, p)
    }

    val uri = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "retrohub_${profile.username}_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RetroHub")
        }
        val u = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        u?.let { context.contentResolver.openOutputStream(it)?.use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) } }
        u
    } ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Add me on RetroHub! ${profile.userHandle.ifBlank { profile.username }} 🎮")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share your player card").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}


// ─── Empty favorites: prompt instead of sample data ───────────────────────────

@Composable
fun FavoritesPromptCard(emoji: String, title: String, body: String, button: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Box(Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(14.dp)).background(ScrapbookDark))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .clickable { onClick() }.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(ComicGlassBgAlt).border(2.dp, ScrapbookDark.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 26.sp)
            }
            Text(title.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, textAlign = TextAlign.Center)
            Text(body, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(button, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FavoritesPickerDialog(title: String, onDone: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.85f).clip(RoundedCornerShape(18.dp)).background(Color.White).border(2.5.dp, ScrapbookDark, RoundedCornerShape(18.dp))
        ) {
            Row(Modifier.fillMaxWidth().background(CGreen).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).clickable { onDone() }.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("DONE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp)) { content() }
        }
    }
}

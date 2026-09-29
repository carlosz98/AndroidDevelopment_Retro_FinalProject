package com.example.hubretro

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ─── Notification data model ──────────────────────────────────────────────────

data class RetroNotification(
    val id: String = "",
    val type: String = "",        // follow | reaction | comment | mention | repost
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromPicUrl: String = "",
    val fromHabboUrl: String = "",
    val targetId: String = "",    // post id or article id
    val targetPreview: String = "",
    val emoji: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false
)

// ─── NotificationsViewModel ───────────────────────────────────────────────────

class NotificationsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _notifications = MutableStateFlow<List<RetroNotification>>(emptyList())
    val notifications: StateFlow<List<RetroNotification>> = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val unreadCount: Int get() = _notifications.value.count { !it.isRead }

    fun fetchNotifications() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docs = db.collection("users").document(uid)
                    .collection("notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()
                _notifications.value = docs.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    RetroNotification(
                        id = doc.id,
                        type = data["type"] as? String ?: "",
                        fromUid = data["fromUid"] as? String ?: "",
                        fromUsername = data["fromUsername"] as? String ?: "",
                        fromPicUrl = data["fromPicUrl"] as? String ?: "",
                        fromHabboUrl = data["fromHabboUrl"] as? String ?: "",
                        targetId = data["targetId"] as? String ?: "",
                        targetPreview = data["targetPreview"] as? String ?: "",
                        emoji = data["emoji"] as? String ?: "",
                        timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)
                            ?.toDate()?.time ?: data["timestamp"] as? Long ?: 0L,
                        isRead = data["isRead"] as? Boolean ?: false
                    )
                }
            } catch (e: Exception) { }
            _isLoading.value = false
        }
    }

    fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val unread = _notifications.value.filter { !it.isRead }
                unread.forEach { notif ->
                    db.collection("users").document(uid)
                        .collection("notifications").document(notif.id)
                        .update("isRead", true).await()
                }
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
            } catch (e: Exception) { }
        }
    }

    fun markRead(notifId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .collection("notifications").document(notifId)
                    .update("isRead", true).await()
                _notifications.value = _notifications.value.map {
                    if (it.id == notifId) it.copy(isRead = true) else it
                }
            } catch (e: Exception) { }
        }
    }
}

// Helper to write a notification to Firestore (call from AuthViewModel or PostViewModel)
suspend fun sendNotification(
    toUid: String,
    type: String,
    fromUid: String,
    fromUsername: String,
    fromPicUrl: String,
    fromHabboUrl: String = "",
    targetId: String = "",
    targetPreview: String = "",
    emoji: String = ""
) {
    if (toUid == fromUid) return // don't notify yourself
    try {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(toUid)
            .collection("notifications").document()
        ref.set(mapOf(
            "id" to ref.id,
            "type" to type,
            "fromUid" to fromUid,
            "fromUsername" to fromUsername,
            "fromPicUrl" to fromPicUrl,
            "fromHabboUrl" to fromHabboUrl,
            "targetId" to targetId,
            "targetPreview" to targetPreview,
            "emoji" to emoji,
            "timestamp" to FieldValue.serverTimestamp(),
            "isRead" to false
        )).await()
    } catch (e: Exception) { }
}

// ─── NotificationsScreen ──────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    val notifications by notificationsViewModel.notifications.collectAsState()
    val isLoading by notificationsViewModel.isLoading.collectAsState()

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    LaunchedEffect(Unit) { notificationsViewModel.fetchNotifications() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.matchParentSize().background(
            Brush.verticalGradient(colors = listOf(
                ComicGlassBg, ComicGlassBg, ComicGlassBg
            ))
        ))
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Box(modifier = Modifier.matchParentSize().background(
            Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                startY = 0f, endY = 400f
            )
        ))
    Column(modifier = Modifier.fillMaxSize()) {

        // Header (Discover style)
        val unreadCount = notifications.count { !it.isRead }
        ComicPageHeader(
            title = "NOTIFICATIONS",
            subtitle = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
            onBack = onBack
        ) {
            if (unreadCount > 0) ComicHeaderChip("MARK ALL READ") { notificationsViewModel.markAllRead() }
        }

        when {
            isLoading -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(6) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(76.dp), cornerRadius = 14.dp) }
            }

            notifications.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("🔔", fontSize = 56.sp)
                    Text("NO NOTIFICATIONS YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                    Text("When people follow you, react to your posts,\nor comment, you'll see it here.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Group by today vs earlier
                val today = System.currentTimeMillis() - 86_400_000
                val todayNotifs = notifications.filter { it.timestamp > today }
                val earlierNotifs = notifications.filter { it.timestamp <= today }

                if (todayNotifs.isNotEmpty()) {
                    item {
                        RetroSectionHeader(title = "TODAY", emoji = "📅")
                    }
                    items(todayNotifs, key = { it.id }) { notif ->
                        NotificationCard(notif = notif, neonAlpha = neonAlpha, onTap = {
                            notificationsViewModel.markRead(notif.id)
                            if (notif.fromUid.isNotBlank()) onNavigateToProfile(notif.fromUid)
                        })
                    }
                }

                if (earlierNotifs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        RetroSectionHeader(title = "EARLIER", emoji = "🕰️")
                    }
                    items(earlierNotifs, key = { it.id }) { notif ->
                        NotificationCard(notif = notif, neonAlpha = neonAlpha, onTap = {
                            notificationsViewModel.markRead(notif.id)
                            if (notif.fromUid.isNotBlank()) onNavigateToProfile(notif.fromUid)
                        })
                    }
                }
            }
        }
    }
    } // end glass Box
}

// ─── NotificationCard ─────────────────────────────────────────────────────────

@Composable
fun NotificationCard(notif: RetroNotification, neonAlpha: Float, onTap: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(300), label = "notifAlpha_${notif.id}")
    val enterOffset by animateFloatAsState(targetValue = if (visible) 0f else 16f, animationSpec = tween(300, easing = LinearOutSlowInEasing), label = "notifOffset_${notif.id}")
    LaunchedEffect(Unit) { visible = true }

    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "notifPress_${notif.id}")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "notifShadow_${notif.id}")
    val glowAlpha by rememberGlowPhase(0.4f)

    val typeEmoji = when (notif.type) {
        "follow" -> "👤"; "reaction" -> notif.emoji.ifBlank { "🔥" }
        "comment" -> "💬"; "mention" -> "📣"; "repost" -> "🔁"
        else -> "🔔"
    }
    val typeText = when (notif.type) {
        "follow" -> "started following you"
        "like" -> "liked your Checkpoint ❤️"
        "witness" -> "witnessed your Checkpoint 👀"
        "reaction" -> "reacted ${notif.emoji} to your post"
        "comment" -> "commented on your post"
        "mention" -> "mentioned you in a post"
        "repost" -> "reposted your post"
        else -> "sent you a notification"
    }
    val accentColor = when (notif.type) {
        "follow" -> CAcBlue; "reaction" -> CAcRed
        "comment" -> CAcPurple; "mention" -> CAcYellow
        "repost" -> CGreenDeep; else -> CGreen
    }

    SpringEntrance(modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .offset(y = enterOffset.dp).graphicsLayer { alpha = enterAlpha }
    ) {
        // Outer glow halo
        Box(modifier = Modifier.matchParentSize().offset(x = 6.dp, y = 6.dp).clip(RoundedCornerShape(14.dp))
            .background(CGreen.copy(alpha = if (pressed) 0f else glowAlpha * 0.25f)))
        // Hard green shadow
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp).clip(RoundedCornerShape(14.dp))
            .background(CGreen.copy(alpha = if (pressed) 0.2f else 1f)))
        // Glass card panel
        Box(
            modifier = Modifier.fillMaxWidth()
                .offset(x = pressAnim.dp, y = pressAnim.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp))
                .clickable { pressed = true; onTap() }
                .padding(12.dp)
        ) {
            HalftoneDots(Modifier.matchParentSize(), spacing = 7.dp, dotRadius = 1.2.dp, color = Color.Black.copy(alpha = 0.08f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Avatar
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)).border(if (notif.isRead) 1.dp else 2.dp, accentColor.copy(alpha = if (notif.isRead) 0.3f else 0.7f), CircleShape), contentAlignment = Alignment.Center) {
                        when {
                            notif.fromPicUrl.isNotBlank() -> AsyncImage(model = notif.fromPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(notif.fromPicUrl).fillMaxSize())
                            notif.fromHabboUrl.isNotBlank() -> AsyncImage(model = notif.fromHabboUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.halftoneReveal(notif.fromHabboUrl).fillMaxSize())
                            else -> Text(notif.fromUsername.take(1).uppercase(), fontFamily = BangersFontFamily, color = accentColor, fontSize = 18.sp)
                        }
                    }
                    // Type badge
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(accentColor).border(2.dp, Color.White, CircleShape).align(Alignment.BottomEnd), contentAlignment = Alignment.Center) {
                        Text(typeEmoji, fontSize = 10.sp)
                    }
                }

                // Content
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(notif.fromUsername, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (!notif.isRead) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                        }
                    }
                    Text(typeText, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    if (notif.targetPreview.isNotBlank()) {
                        Text(
                            "\"${notif.targetPreview.take(60)}${if (notif.targetPreview.length > 60) "..." else ""}\"",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(timeAgoFromMillis(notif.timestamp), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
    } // end SpringEntrance
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}
// ─── PostCommentsSheet ────────────────────────────────────────────────────────
// Bottom sheet / dialog that shows comments on a post

data class PostCommentItem(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorPicUrl: String = "",
    val authorHabboUrl: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val replyTo: String = ""   // username being replied to, if any
)

@Composable
fun PostCommentsSheet(
    post: RetroPost,
    currentUserId: String,
    currentUserProfile: UserProfileData?,
    onDismiss: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    var comments by remember { mutableStateOf<List<PostCommentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<PostCommentItem?>(null) }

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    // Load comments
    LaunchedEffect(post.id) {
        isLoading = true
        try {
            val docs = db.collection("posts").document(post.id)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(100).get().await()
            comments = docs.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                PostCommentItem(
                    id = doc.id,
                    authorUid = data["authorUid"] as? String ?: "",
                    authorUsername = data["authorUsername"] as? String ?: "Anonymous",
                    authorPicUrl = data["authorPicUrl"] as? String ?: "",
                    authorHabboUrl = data["authorHabboUrl"] as? String ?: "",
                    text = data["text"] as? String ?: "",
                    timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: data["timestamp"] as? Long ?: 0L,
                    likes = (data["likes"] as? Long)?.toInt() ?: 0,
                    likedBy = (data["likedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    replyTo = data["replyTo"] as? String ?: ""
                )
            }.filter { it.text.isNotBlank() }
        } catch (e: Exception) { }
        isLoading = false
    }

    suspend fun submitComment() {
        if (commentText.isBlank() || isSubmitting) return
        val uid = currentUserId.ifBlank { return }
        val safeComment = Moderation.gate(commentText.trim(), "comment") ?: return
        val username = currentUserProfile?.username ?: "Anonymous"
        val picUrl = currentUserProfile?.profilePictureUrl ?: ""
        val habboUrl = if (!currentUserProfile?.habboUsername.isNullOrBlank())
            "https://www.${currentUserProfile?.habboRegion}/habbo-imaging/avatarimage?user=${currentUserProfile?.habboUsername}&action=std&direction=2&head_direction=2&size=m&gesture=sml"
        else ""
        isSubmitting = true
        try {
            val ref = db.collection("posts").document(post.id).collection("comments").document()
            ref.set(mapOf(
                "id" to ref.id,
                "authorUid" to uid,
                "authorUsername" to username,
                "authorPicUrl" to picUrl,
                "authorHabboUrl" to habboUrl,
                "text" to safeComment,
                "replyTo" to (replyingTo?.authorUsername ?: ""),
                "timestamp" to FieldValue.serverTimestamp(),
                "likes" to 0,
                "likedBy" to emptyList<String>()
            )).await()
            // Increment comment count on post
            db.collection("posts").document(post.id)
                .update("commentCount", FieldValue.increment(1)).await()

            // Send notification to post author
            if (post.authorUid.isNotBlank() && post.authorUid != uid) {
                sendNotification(
                    toUid = post.authorUid,
                    type = "comment",
                    fromUid = uid,
                    fromUsername = username,
                    fromPicUrl = picUrl,
                    fromHabboUrl = habboUrl,
                    targetId = post.id,
                    targetPreview = commentText.take(80)
                )
            }

            // Add locally
            val newComment = PostCommentItem(
                id = ref.id, authorUid = uid, authorUsername = username,
                authorPicUrl = picUrl, authorHabboUrl = habboUrl,
                text = commentText.trim(), timestamp = System.currentTimeMillis(),
                replyTo = replyingTo?.authorUsername ?: ""
            )
            comments = comments + newComment
            commentText = ""
            replyingTo = null
        } catch (e: Exception) { }
        isSubmitting = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.comicPop().fillMaxWidth().fillMaxHeight(0.88f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                // Green stripe
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                // Handle
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookBorder.copy(alpha = 0.4f)))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.ChatBubbleOutline, null, tint = CGreen, modifier = Modifier.size(20.dp))
                        Text("COMMENTS", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text("${comments.size}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null, tint = ScrapbookTextMuted) }
                }

                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))

                // Comments list
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        isLoading -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(4) { ShimmerBox(modifier = Modifier.fillMaxWidth().height(70.dp), cornerRadius = 12.dp) }
                        }
                        comments.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("💬", fontSize = 40.sp)
                                Text("NO COMMENTS YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp)
                                Text("Be the first to comment!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                            }
                        }
                        else -> LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(comments, key = { it.id }) { comment ->
                                PostCommentCard(
                                    comment = comment,
                                    currentUserId = currentUserId,
                                    onReply = { replyingTo = comment },
                                    onDelete = {
                                        scope.launch {
                                            try {
                                                db.collection("posts").document(post.id)
                                                    .collection("comments").document(comment.id).delete().await()
                                                db.collection("posts").document(post.id)
                                                    .update("commentCount", FieldValue.increment(-1)).await()
                                                comments = comments.filter { it.id != comment.id }
                                            } catch (e: Exception) { }
                                        }
                                    },
                                    onLike = {
                                        scope.launch {
                                            try {
                                                val isLiked = comment.likedBy.contains(currentUserId)
                                                val ref = db.collection("posts").document(post.id)
                                                    .collection("comments").document(comment.id)
                                                if (isLiked) {
                                                    ref.update(mapOf("likes" to FieldValue.increment(-1), "likedBy" to FieldValue.arrayRemove(currentUserId))).await()
                                                } else {
                                                    ref.update(mapOf("likes" to FieldValue.increment(1), "likedBy" to FieldValue.arrayUnion(currentUserId))).await()
                                                }
                                                comments = comments.map {
                                                    if (it.id == comment.id) {
                                                        if (isLiked) it.copy(likes = it.likes - 1, likedBy = it.likedBy - currentUserId)
                                                        else it.copy(likes = it.likes + 1, likedBy = it.likedBy + currentUserId)
                                                    } else it
                                                }
                                            } catch (e: Exception) { }
                                        }
                                    },
                                    onTapAuthor = { onNavigateToProfile(comment.authorUid) }
                                )
                            }
                        }
                    }
                }

                // Reply indicator
                AnimatedVisibility(visible = replyingTo != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    replyingTo?.let { reply ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(CGreen.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Reply, null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                                Text("Replying to ", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                                Text("@${reply.authorUsername}", fontFamily = BangersFontFamily, color = CGreen, fontSize = 13.sp)
                            }
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, null, tint = ScrapbookTextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Comment input
                HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth().background(ComicGlassBg).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // User avatar
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(CGreen.copy(alpha = 0.15f)).border(1.dp, CGreen.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                        when {
                            !currentUserProfile?.profilePictureUrl.isNullOrBlank() -> AsyncImage(model = currentUserProfile!!.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(currentUserProfile!!.profilePictureUrl).fillMaxSize())
                            !currentUserProfile?.habboUsername.isNullOrBlank() -> AsyncImage(model = "https://www.${currentUserProfile!!.habboRegion}/habbo-imaging/avatarimage?user=${currentUserProfile.habboUsername}&action=std&direction=2&head_direction=2&size=m&gesture=sml", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            else -> Text(currentUserProfile?.username?.take(1)?.uppercase() ?: "?", fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
                        }
                    }

                    // Text field
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.7f)).border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(20.dp))) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { if (it.length <= 300) commentText = it },
                            placeholder = {
                                Text(
                                    if (replyingTo != null) "Reply to @${replyingTo!!.authorUsername}..." else "Add a comment...",
                                    fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp
                                )
                            },
                            singleLine = false, maxLines = 4,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookDark),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Send button
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(if (commentText.isNotBlank()) ScrapbookDark else ScrapbookBorder)
                            .border(1.dp, if (commentText.isNotBlank()) CGreen.copy(alpha = 0.5f) else ScrapbookBorder, CircleShape)
                            .clickable { scope.launch { submitComment() } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) CircularProgressIndicator(color = CGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Send, null, tint = if (commentText.isNotBlank()) CGreen else ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─── PostCommentCard ──────────────────────────────────────────────────────────

@Composable
fun PostCommentCard(
    comment: PostCommentItem,
    currentUserId: String,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onLike: () -> Unit,
    onTapAuthor: () -> Unit
) {
    val isLiked = comment.likedBy.contains(currentUserId)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(CGreen.copy(alpha = 0.1f))
                .border(1.dp, ScrapbookBorder.copy(alpha = 0.4f), CircleShape)
                .clickable { onTapAuthor() },
            contentAlignment = Alignment.Center
        ) {
            when {
                comment.authorPicUrl.isNotBlank() -> AsyncImage(model = comment.authorPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(comment.authorPicUrl).fillMaxSize())
                comment.authorHabboUrl.isNotBlank() -> AsyncImage(model = comment.authorHabboUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.halftoneReveal(comment.authorHabboUrl).fillMaxSize())
                else -> Text(comment.authorUsername.take(1).uppercase(), fontFamily = BangersFontFamily, color = CGreen, fontSize = 14.sp)
            }
        }

        // Bubble
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.matchParentSize()
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                        .padding(10.dp)
                ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            comment.authorUsername,
                            fontFamily = BangersFontFamily,
                            color = CGreen,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { onTapAuthor() }
                        )
                        Text(timeAgoFromMillis(comment.timestamp), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    }
                    if (comment.replyTo.isNotBlank()) {
                        Text("↩ @${comment.replyTo}", fontFamily = NunitoFontFamily, color = CGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(comment.text, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 13.sp, lineHeight = 19.sp)
                }
                }
            }

            // Actions row
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                var likePressed by remember { mutableStateOf(false) }
                val likeScale by animateFloatAsState(targetValue = if (likePressed) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessHigh), label = "commentLikeScale")
                val likeBurst = rememberBurstState()
                Box {
                Row(
                    modifier = Modifier.scale(likeScale).clickable {
                        likePressed = true
                        if (!isLiked) likeBurst.fire("LOVE!", CGreenMint)
                        onLike()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(if (isLiked) "❤️" else "🤍", fontSize = 13.sp)
                    if (comment.likes > 0) Text("${comment.likes}", fontFamily = BangersFontFamily, color = if (isLiked) CAcRed else ScrapbookTextMuted, fontSize = 11.sp)
                }
                ComicBurst(likeBurst, Modifier.align(Alignment.Center), burstSize = 76.dp)
                }
                LaunchedEffect(likePressed) { if (likePressed) { delay(200); likePressed = false } }

                // Reply
                Text("Reply", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, modifier = Modifier.clickable { onReply() })

                // Delete (own comment only)
                if (comment.authorUid == currentUserId) {
                    Text("Delete", fontFamily = BangersFontFamily, color = CAcRed.copy(alpha = 0.5f), fontSize = 11.sp, modifier = Modifier.clickable { showDeleteConfirm = true })
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            modifier = Modifier.comicPop(),
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = ComicGlassBg,
            titleContentColor = CGreenDeep,
            textContentColor = ScrapbookDark,
            title = { Text("DELETE COMMENT", fontFamily = BangersFontFamily, fontSize = 18.sp) },
            text = { Text("Delete this comment?", fontFamily = NunitoFontFamily, fontSize = 14.sp) },
            confirmButton = {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(CAcRed.copy(alpha = 0.2f)).border(1.dp, CAcRed, RoundedCornerShape(8.dp)).clickable { showDeleteConfirm = false; onDelete() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("DELETE", fontFamily = BangersFontFamily, color = CAcRed, fontSize = 14.sp)
                }
            },
            dismissButton = {
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f)).border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp)).clickable { showDeleteConfirm = false }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
        )
    }
}
// ─── UnreadNotificationBadge ──────────────────────────────────────────────────
// Drop this wherever you show the bell icon (e.g. HomeScreen header)
// ─── PostCardWithSocial ───────────────────────────────────────────────────────
// Wrapper that adds comments + profile tap to any PostCard.
// Use this everywhere instead of PostCard directly.

@Composable
fun PostCardWithSocial(
    post: RetroPost,
    currentUserId: String,
    currentUserProfile: UserProfileData?,
    postViewModel: PostViewModel,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToGames: () -> Unit = {}
) {
    var showComments by remember { mutableStateOf(false) }

    // Intercept the comment count tap to open the sheet
    SpringEntrance(modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.stampIn("post_${post.id}")) {
        PostCard(
            post = post,
            currentUserId = currentUserId,
            postViewModel = postViewModel,
            onCommentTap = { showComments = true },
            onAuthorTap = { onNavigateToProfile(post.authorUid) }
        )
    }
    } // end SpringEntrance

    if (showComments) {
        PostCommentsSheet(
            post = post,
            currentUserId = currentUserId,
            currentUserProfile = currentUserProfile,
            onDismiss = { showComments = false },
            onNavigateToProfile = onNavigateToProfile
        )
    }
}

// ─── Story / Composer Stubs ───────────────────────────────────────────────────

@Composable
fun StoryViewerDialog(story: RetroStory, onDismiss: () -> Unit, postViewModel: PostViewModel) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.comicPop().fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ComicGlassBg).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Text("Story Viewer Coming Soon", fontFamily = BangersFontFamily, color = CGreenDeep, modifier = Modifier.padding(32.dp))
        }
    }
}

@Composable
fun PostComposerSheet(currentUser: UserProfileData?, postViewModel: PostViewModel, onDismiss: () -> Unit) {
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()
    val isPosting     by postViewModel.isPosting.collectAsState()

    // Content
    var title         by remember { mutableStateOf("") }
    var content       by remember { mutableStateOf("") }
    var hashtagInput  by remember { mutableStateOf("") }
    var hashtags      by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedMood  by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(0) }

    // Media — GIF and music are INDEPENDENT (can stack)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedGifUrl   by remember { mutableStateOf("") }

    // GIF search
    var gifQuery      by remember { mutableStateOf("") }
    var gifResults    by remember { mutableStateOf<List<GifResult>>(emptyList()) }
    var gifSearching  by remember { mutableStateOf(false) }

    // Music
    var musicQuery      by remember { mutableStateOf("") }
    var musicResults    by remember { mutableStateOf<List<MusicSearchResult>>(emptyList()) }
    var musicSearching  by remember { mutableStateOf(false) }
    var selectedMusic   by remember { mutableStateOf<MusicSearchResult?>(null) }
    var musicPlayer     by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    // Each section independently toggleable — GIF + Music can coexist
    var showGifPanel    by remember { mutableStateOf(false) }
    var showMusicPanel  by remember { mutableStateOf(false) }
    var showRatingPanel by remember { mutableStateOf(false) }

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    DisposableEffect(Unit) {
        onDispose { musicPlayer?.release(); musicPlayer = null }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedImageUri = uri; selectedGifUrl = "" }
    }

    fun addHashtag() {
        val tag = hashtagInput.trim().trimStart('#')
        if (tag.isNotBlank() && !hashtags.contains(tag) && hashtags.size < 5) {
            hashtags = hashtags + tag; hashtagInput = ""
        }
    }

    fun searchGifs(q: String) {
        if (q.isBlank()) return
        gifSearching = true
        scope.launch(Dispatchers.IO) {
            try {
                val enc = URLEncoder.encode(q.trim(), "UTF-8")
                val url = "https://api.giphy.com/v1/gifs/search?api_key=cqqt9V8jWLMgC9VHDyjWGMpMYBvpU92m&q=$enc&limit=16&rating=g"
                val body = URL(url).openConnection().apply { connectTimeout = 8000; readTimeout = 8000 }
                    .getInputStream().bufferedReader().readText()
                val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<GifResult>()
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val imgs = obj.optJSONObject("images") ?: continue
                    val preview = imgs.optJSONObject("fixed_width_small")?.optString("url", "")?.takeIf { it.isNotBlank() } ?: continue
                    val full = imgs.optJSONObject("original")?.optString("url", "") ?: preview
                    list.add(GifResult(id = obj.optString("id"), title = obj.optString("title"), previewUrl = preview, originalUrl = full))
                }
                withContext(Dispatchers.Main) { gifResults = list; gifSearching = false }
            } catch (e: Exception) { withContext(Dispatchers.Main) { gifSearching = false } }
        }
    }

    fun searchMusic(q: String) {
        if (q.isBlank()) return
        musicSearching = true
        scope.launch(Dispatchers.IO) {
            try {
                val enc = URLEncoder.encode(q.trim(), "UTF-8")
                val body = URL("https://itunes.apple.com/search?term=$enc&media=music&limit=10&entity=song")
                    .openConnection().apply { connectTimeout = 8000; readTimeout = 8000 }
                    .getInputStream().bufferedReader().readText()
                val results = JSONObject(body).getJSONArray("results")
                val list = mutableListOf<MusicSearchResult>()
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val preview = obj.optString("previewUrl", "")
                    if (preview.isBlank()) continue
                    list.add(MusicSearchResult(
                        trackId = obj.optLong("trackId"),
                        trackName = obj.optString("trackName"),
                        artistName = obj.optString("artistName"),
                        artworkUrl = obj.optString("artworkUrl100", "").replace("100x100bb", "300x300bb"),
                        previewUrl = preview,
                        collectionName = obj.optString("collectionName")
                    ))
                }
                withContext(Dispatchers.Main) { musicResults = list; musicSearching = false }
            } catch (e: Exception) { withContext(Dispatchers.Main) { musicSearching = false } }
        }
    }

    fun toggleMusicPreview(music: MusicSearchResult) {
        if (isPlayingPreview) {
            musicPlayer?.pause()
            isPlayingPreview = false
        } else {
            musicPlayer?.release()
            musicPlayer = MediaPlayer().apply {
                setDataSource(music.previewUrl)
                setOnPreparedListener { start(); isPlayingPreview = true }
                setOnCompletionListener { isPlayingPreview = false }
                prepareAsync()
            }
        }
    }

    Dialog(onDismissRequest = { musicPlayer?.release(); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.comicPop().fillMaxWidth(0.96f).fillMaxHeight(0.92f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
            ) {
                // Green stripe
                Box(modifier = Modifier.fillMaxWidth().height(4.dp)
                    .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint, CGreen, CGreenDeep))))
                // ── Header ────────────────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NEW POST", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
                    IconButton(onClick = { musicPlayer?.release(); onDismiss() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }

                // ── Scrollable content ────────────────────────────────────────
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Author + text area
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(CGreen.copy(alpha = 0.15f))
                            .border(1.5.dp, CGreen.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center) {
                            if (!currentUser?.profilePictureUrl.isNullOrBlank())
                                AsyncImage(model = currentUser!!.profilePictureUrl, contentDescription = null,
                                    contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(currentUser!!.profilePictureUrl).fillMaxSize())
                            else Text(currentUser?.username?.take(1)?.uppercase() ?: "?",
                                fontFamily = BangersFontFamily, color = CGreen, fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(currentUser?.username ?: "", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                            // Title field
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                                .border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)) {
                                BasicTextField(
                                    value = title, onValueChange = { if (it.length <= 80) title = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookDark, letterSpacing = 0.3.sp),
                                    cursorBrush = SolidColor(ScrapbookDark), singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (title.isEmpty()) Text("Title (optional)…", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 16.sp)
                                        inner()
                                    }
                                )
                            }
                            // Content field
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                                    .clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.12f)))
                                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)) {
                                    OutlinedTextField(
                                        value = content, onValueChange = { if (it.length <= 280) content = it },
                                        placeholder = { Text("What's your retro moment?", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp) },
                                        singleLine = false, maxLines = 6,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookDark),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
                                }
                            }
                            Text("${content.length}/280", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                        }
                    }

                    // ── Media toolbar — each section independently toggleable ──
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Photo
                        Box {
                            val photoActive = selectedImageUri != null
                            if (photoActive) GlowPulse(Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 8.dp)
                            MediaToolButton(label = "PHOTO", emoji = "📷", active = photoActive,
                                onClick = { imagePicker.launch("image/*") })
                        }
                        // GIF — independent; can stack with music
                        Box {
                            val gifActive = selectedGifUrl.isNotBlank() || showGifPanel
                            if (gifActive) GlowPulse(Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 8.dp)
                            MediaToolButton(label = "GIF", emoji = "🎞", active = gifActive,
                                onClick = { showGifPanel = !showGifPanel })
                        }
                        // Music — independent; can stack with GIF
                        Box {
                            val musicActive = selectedMusic != null || showMusicPanel
                            if (musicActive) GlowPulse(Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 8.dp)
                            MediaToolButton(label = "MUSIC", emoji = "🎵", active = musicActive,
                                onClick = { showMusicPanel = !showMusicPanel })
                        }
                        // Rating
                        Box {
                            val ratingActive = selectedRating > 0 || showRatingPanel
                            if (ratingActive) GlowPulse(Modifier.matchParentSize(), glowColor = CGreen, cornerRadius = 8.dp)
                            MediaToolButton(label = "RATE", emoji = "⭐", active = ratingActive,
                                onClick = { showRatingPanel = !showRatingPanel })
                        }
                    }

                    // ── Image preview ─────────────────────────────────────────
                    AnimatedVisibility(selectedImageUri != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                            .background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))) {
                            AsyncImage(model = selectedImageUri, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(selectedImageUri).fillMaxSize())
                            ScanlineOverlay(Modifier.matchParentSize(), lineAlpha = 0.05f)
                            // Remove button
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                                .size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f))
                                .clickable { selectedImageUri = null },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // ── GIF selected preview ──────────────────────────────────
                    AnimatedVisibility(selectedGifUrl.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                            .background(ScrapbookDark).border(1.dp, CGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(selectedGifUrl).decoderFactory(GifDecoder.Factory()).build(),
                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                                .size(26.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f))
                                .clickable { selectedGifUrl = "" },
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // ── GIF search panel ──────────────────────────────────────
                    AnimatedVisibility(showGifPanel && selectedGifUrl.isBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.7f)).border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    BasicTextField(
                                        value = gifQuery, onValueChange = { gifQuery = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                                        cursorBrush = SolidColor(ScrapbookDark),
                                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (gifQuery.isEmpty()) Text("Search GIFs…", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp)
                                            inner()
                                        }
                                    )
                                }
                                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                                    .clickable { searchGifs(gifQuery) }, contentAlignment = Alignment.Center) {
                                    if (gifSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ScrapbookDark, strokeWidth = 2.dp)
                                    else Icon(Icons.Filled.Search, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (gifResults.isNotEmpty()) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    gridItems(gifResults) { gif ->
                                        Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                                            .background(ScrapbookDark)
                                            .border(1.dp, CGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .clickable { selectedGifUrl = gif.originalUrl; selectedImageUri = null; showGifPanel = false }) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(gif.previewUrl).decoderFactory(GifDecoder.Factory()).build(),
                                                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Music selected pill ───────────────────────────────────
                    AnimatedVisibility(selectedMusic != null) {
                        selectedMusic?.let { music ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (music.artworkUrl.isNotBlank()) {
                                    AsyncImage(model = music.artworkUrl, contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.halftoneReveal(music.artworkUrl).size(40.dp).clip(RoundedCornerShape(6.dp)))
                                } else {
                                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                                        Text("🎵", fontSize = 18.sp)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(music.trackName, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(music.artistName, fontFamily = NunitoFontFamily, color = CGreenDeep, fontSize = 11.sp)
                                }
                                // Play/pause preview
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                                    .background(CGreen.copy(alpha = 0.18f))
                                    .clickable { toggleMusicPreview(music) }, contentAlignment = Alignment.Center) {
                                    Icon(if (isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        null, tint = CGreen, modifier = Modifier.size(18.dp))
                                }
                                // Remove
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape)
                                    .background(ScrapbookDark.copy(alpha = 0.08f))
                                    .clickable { selectedMusic = null; musicPlayer?.release(); isPlayingPreview = false },
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                }
                            }
                            ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
                            } // end music pill overlay Box
                        }
                    }

                    // ── Music search panel ────────────────────────────────────
                    AnimatedVisibility(showMusicPanel && selectedMusic == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("MUSIC", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp, letterSpacing = 1.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.7f)).border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    BasicTextField(
                                        value = musicQuery, onValueChange = { musicQuery = it },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                                        cursorBrush = SolidColor(ScrapbookDark), singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (musicQuery.isEmpty()) Text("Search song or artist…", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp)
                                            inner()
                                        }
                                    )
                                }
                                Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                                    .clickable { searchMusic(musicQuery) }, contentAlignment = Alignment.Center) {
                                    if (musicSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ScrapbookDark, strokeWidth = 2.dp)
                                    else Icon(Icons.Filled.Search, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (musicResults.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                    musicResults.forEach { music ->
                                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.92f))
                                            .border(1.dp, ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable { selectedMusic = music; showMusicPanel = false }
                                            .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (music.artworkUrl.isNotBlank())
                                                AsyncImage(model = music.artworkUrl, contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.halftoneReveal(music.artworkUrl).size(36.dp).clip(RoundedCornerShape(4.dp)))
                                            else Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(ScrapbookDark), contentAlignment = Alignment.Center) { Text("🎵", fontSize = 16.sp) }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(music.trackName, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(music.artistName, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 1)
                                            }
                                            Text("30s ▶", fontFamily = NunitoFontFamily, color = CGreenDeep, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Rating section ────────────────────────────────────────
                    AnimatedVisibility(showRatingPanel) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("RATE THIS GAME", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp, letterSpacing = 1.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..5).forEach { star ->
                                    Text(
                                        if (star <= selectedRating) "⭐" else "☆",
                                        fontSize = 28.sp,
                                        modifier = Modifier.clickable { selectedRating = if (selectedRating == star) 0 else star }
                                    )
                                }
                                if (selectedRating > 0) {
                                    Text("$selectedRating/5", fontFamily = BangersFontFamily,
                                        color = CGreen, fontSize = 22.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically))
                                }
                            }
                        }
                    }

                    // ── Hashtags ──────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("HASHTAGS", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp, letterSpacing = 1.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.7f)).border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))) {
                                OutlinedTextField(
                                    value = hashtagInput,
                                    onValueChange = { v ->
                                        val clean = v.trimStart('#')
                                        if (clean.endsWith(" ") || clean.endsWith(",")) addHashtag()
                                        else if (clean.length <= 24) hashtagInput = clean
                                    },
                                    placeholder = { Text("#retro #gaming ...", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = ScrapbookDark),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                )
                            }
                            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                                .border(1.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                .clickable { addHashtag() }, contentAlignment = Alignment.Center) {
                                Text("+", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp)
                            }
                        }
                        if (hashtags.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                hashtags.forEachIndexed { index, tag ->
                                    val rot = listOf(-3f, 2f, -2f, 3f, -1f)[index % 5]
                                    // RetroLabel D style chip
                                    Box(modifier = Modifier.graphicsLayer { rotationZ = rot }) {
                                        // Green offset shadow
                                        Box(modifier = Modifier.matchParentSize().offset(x = 2.dp, y = 2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CGreen))
                                        // Glass chip
                                        Box(modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.92f))
                                            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))
                                            .clickable { hashtags = hashtags - tag }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)) {
                                            HalftoneDots(Modifier.matchParentSize(), spacing = 5.dp, dotRadius = 0.9.dp, color = Color.Black.copy(alpha = 0.10f))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text("#$tag", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                                                Text("×", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Mood ──────────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("MOOD", fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp, letterSpacing = 1.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(moodTags) { mood ->
                                val isSelected = selectedMood == mood
                                Box(modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.44f))))
                                    .border(1.5.dp, if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { selectedMood = if (isSelected) "" else mood }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(mood, fontFamily = BangersFontFamily, color = if (isSelected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.7f), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ── Post button (pinned at bottom) ────────────────────────────
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = ScrapbookDark.copy(alpha = 0.2f))
                val postBurst = rememberBurstState()
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Box(modifier = Modifier.matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScrapbookDark.copy(alpha = 0.12f)))
                    Box(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (content.isNotBlank())
                                Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep))
                            else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.44f)))
                        )
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                        .clickable(enabled = content.isNotBlank() && !isPosting) {
                            musicPlayer?.release(); isPlayingPreview = false
                            postBurst.fire("KAPOW!")
                            postViewModel.createPost(
                                type = if (selectedImageUri != null) "image" else if (selectedGifUrl.isNotBlank()) "gif" else "text",
                                title = title,
                                content = content,
                                moodTag = selectedMood,
                                hashtags = hashtags,
                                rating = selectedRating,
                                songTitle = selectedMusic?.trackName ?: "",
                                songArtist = selectedMusic?.artistName ?: "",
                                songPreviewUrl = selectedMusic?.previewUrl ?: "",
                                songArtworkUrl = selectedMusic?.artworkUrl ?: "",
                                gifUrl = selectedGifUrl,
                                imageUri = selectedImageUri,
                                onComplete = { success -> if (success) onDismiss() }
                            )
                        }
                        .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) {
                        if (isPosting)
                            CircularProgressIndicator(color = ScrapbookDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Text("POST IT →", fontFamily = BangersFontFamily,
                                color = if (content.isNotBlank()) ScrapbookDark else ScrapbookDark.copy(alpha = 0.35f),
                                fontSize = 16.sp, letterSpacing = 1.sp)
                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 12.dp)
                    }
                    ComicBurst(postBurst, Modifier.align(Alignment.Center), burstSize = 120.dp)
                }
            }
        }
    }
}

@Composable
private fun MediaToolButton(label: String, emoji: String, active: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (active) Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep))
            else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.44f))))
        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(emoji, fontSize = 14.sp)
            Text(label, fontFamily = BangersFontFamily, color = if (active) ScrapbookDark else ScrapbookDark.copy(alpha = 0.75f), fontSize = 11.sp, letterSpacing = 0.5.sp)
        }
    }
}

private val storyGradientPresets = listOf(
    listOf(CGreenDeep, CGreen, Color(0xFF1B4332)),
    listOf(CAcPurple, Color(0xFF4A1C75), Color(0xFF240E3A)),
    listOf(CAcBlue, Color(0xFF2A3D99), Color(0xFF141E4D)),
    listOf(Color(0xFF002B36), Color(0xFF004D5C), Color(0xFF001A22)),
    listOf(Color(0xFF2C1810), Color(0xFF5C3317), Color(0xFF1A0F00)),
    listOf(ScrapbookDark, Color(0xFF2E2E2E), ScrapbookDark),
    listOf(Color(0xFF1A001A), Color(0xFF330033), Color(0xFF0D000D)),
    listOf(Color(0xFF001A00), Color(0xFF003300), Color(0xFF001A00)),
)

private val storyStickers = listOf("🎮", "🕹️", "🏆", "⭐", "💾", "📼", "🎯", "👾", "🔥", "💯", "🎵", "🎨", "🌟", "💎", "⚡", "🎲")

@Composable
fun StoryComposerSheet(currentUser: UserProfileData?, postViewModel: PostViewModel, onDismiss: () -> Unit) {
    val scope         = rememberCoroutineScope()
    val isPosting     by postViewModel.isPosting.collectAsState()

    var selectedImageUri    by remember { mutableStateOf<Uri?>(null) }
    var selectedGradientIdx by remember { mutableStateOf(0) }
    var storyType           by remember { mutableStateOf("now_playing") }

    var textOverlay         by remember { mutableStateOf("") }
    var textColor           by remember { mutableStateOf(Color.White) }
    var textStyleIdx        by remember { mutableStateOf(0) }

    var canvasStickers      by remember { mutableStateOf<List<String>>(emptyList()) }

    var musicQuery          by remember { mutableStateOf("") }
    var musicResults        by remember { mutableStateOf<List<MusicSearchResult>>(emptyList()) }
    var musicSearching      by remember { mutableStateOf(false) }
    var selectedMusic       by remember { mutableStateOf<MusicSearchResult?>(null) }
    var musicPlayer         by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlayingPreview    by remember { mutableStateOf(false) }

    var activePanel         by remember { mutableStateOf<String?>(null) }
    var selectedFilter      by remember { mutableStateOf(RetroFilter.NONE) }

    val waveT = rememberInfiniteTransition(label = "storyWave")
    val wavePhase by waveT.animateFloat(0f, 1f,
        infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart), label = "storyWavePhase")

    DisposableEffect(Unit) { onDispose { musicPlayer?.release() } }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { selectedImageUri = uri; activePanel = null }
    }

    fun searchMusic(q: String) {
        if (q.isBlank()) return
        musicSearching = true
        scope.launch(Dispatchers.IO) {
            try {
                val enc  = URLEncoder.encode(q.trim(), "UTF-8")
                val body = URL("https://itunes.apple.com/search?term=$enc&media=music&limit=10&entity=song")
                    .openConnection().apply { connectTimeout = 8000; readTimeout = 8000 }
                    .getInputStream().bufferedReader().readText()
                val arr  = JSONObject(body).getJSONArray("results")
                val list = mutableListOf<MusicSearchResult>()
                for (i in 0 until arr.length()) {
                    val obj     = arr.getJSONObject(i)
                    val preview = obj.optString("previewUrl", "")
                    if (preview.isBlank()) continue
                    list.add(MusicSearchResult(
                        trackId        = obj.optLong("trackId"),
                        trackName      = obj.optString("trackName"),
                        artistName     = obj.optString("artistName"),
                        artworkUrl     = obj.optString("artworkUrl100", "").replace("100x100bb", "300x300bb"),
                        previewUrl     = preview,
                        collectionName = obj.optString("collectionName")
                    ))
                }
                withContext(Dispatchers.Main) { musicResults = list; musicSearching = false }
            } catch (e: Exception) { withContext(Dispatchers.Main) { musicSearching = false } }
        }
    }

    fun playPreview(music: MusicSearchResult) {
        musicPlayer?.release()
        musicPlayer = MediaPlayer().apply {
            setDataSource(music.previewUrl)
            setOnPreparedListener { start(); isPlayingPreview = true }
            setOnCompletionListener { isPlayingPreview = false }
            prepareAsync()
        }
    }

    val storyTypeList   = listOf(Triple("now_playing","🎮","NOW PLAYING"), Triple("flashback","📼","FLASHBACK"), Triple("challenge","🏆","CHALLENGE"))
    val textColorList   = listOf(Color.White, CGreen, CAcRed, CGreenMint, CAcYellow)
    val textStyleNames  = listOf("Normal", "Bold", "Neon", "Type")

    Dialog(
        onDismissRequest = { musicPlayer?.release(); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Box(modifier = Modifier.comicPop().fillMaxSize().background(Color.Black)) {

            // ─── Canvas ────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).align(Alignment.TopCenter)) {

                // Background
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = selectedFilter.toColorFilter(),
                        modifier = Modifier.halftoneReveal(selectedImageUri).fillMaxSize()
                    )
                    if (selectedFilter == RetroFilter.SCANLINE || selectedFilter == RetroFilter.VHS) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var y = 0f
                            while (y < size.height) {
                                drawLine(Color.Black.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), 2f)
                                y += 5f
                            }
                        }
                    }
                    if (selectedFilter == RetroFilter.VHS) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4400FF).copy(alpha = 0.07f)))
                        Text("REC ●", fontFamily = BangersFontFamily, color = Color.Red.copy(alpha = 0.85f), fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
                    }
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))
                    ))
                } else {
                    Box(modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(storyGradientPresets[selectedGradientIdx])))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 32.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(x, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                }

                // Story type badge
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)) {
                    val (_, typeEmoji, typeLabel) = storyTypeList.first { it.first == storyType }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(typeEmoji, fontSize = 12.sp)
                        Text(typeLabel, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp, letterSpacing = 0.5.sp)
                    }
                }

                // Author badge
                Row(modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                        .border(2.dp, CGreen, CircleShape)
                        .background(ScrapbookDark)) {
                        if (!currentUser?.profilePictureUrl.isNullOrBlank())
                            AsyncImage(model = currentUser!!.profilePictureUrl, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(currentUser!!.profilePictureUrl).fillMaxSize())
                        else Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(currentUser?.username?.take(1)?.uppercase() ?: "?",
                                fontFamily = BangersFontFamily, color = CGreen, fontSize = 12.sp)
                        }
                    }
                    Text(currentUser?.username ?: "", fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(6.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                }

                // Canvas stickers
                canvasStickers.take(6).forEachIndexed { i, sticker ->
                    val xPct = listOf(0.1f, 0.6f, 0.25f, 0.55f, 0.08f, 0.65f)[i % 6]
                    val yPct = listOf(0.18f, 0.28f, 0.48f, 0.54f, 0.68f, 0.63f)[i % 6]
                    val rot  = listOf(-12f, 8f, -5f, 14f, -9f, 6f)[i % 6]
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(sticker, fontSize = 34.sp, modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (xPct * 300).dp, y = (yPct * 500).dp)
                            .rotate(rot)
                            .clickable { canvasStickers = canvasStickers.toMutableList().also { l -> l.removeAt(i) } })
                    }
                }

                // Text overlay
                if (textOverlay.isNotBlank()) {
                    Box(modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp)
                        .then(when (textStyleIdx) {
                            1    -> Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                            2    -> Modifier.background(textColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).border(1.dp, textColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                            3    -> Modifier.background(ScrapbookDark.copy(alpha = 0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 5.dp)
                            else -> Modifier
                        })) {
                        Text(textOverlay,
                            fontFamily = if (textStyleIdx == 1 || textStyleIdx == 2) BangersFontFamily else NunitoFontFamily,
                            color = textColor,
                            fontSize = if (textStyleIdx == 1) 22.sp else 18.sp,
                            fontWeight = if (textStyleIdx == 1) FontWeight.ExtraBold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Music pill floating on canvas
                selectedMusic?.let { music ->
                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                        .clickable {
                            if (isPlayingPreview) { musicPlayer?.pause(); isPlayingPreview = false }
                            else playPreview(music)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🎵", fontSize = 13.sp)
                            Column {
                                Text(music.trackName, fontFamily = BangersFontFamily, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                Text(music.artistName, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                            if (isPlayingPreview) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    listOf(0f, 0.33f, 0.66f, 0.45f, 0.2f).forEachIndexed { idx, base ->
                                        val barH = (4 + (kotlin.math.sin((wavePhase + base + idx * 0.2f).toDouble() * 2 * Math.PI) * 8).toInt().coerceAtLeast(2))
                                        Box(modifier = Modifier.width(3.dp).height(barH.dp).clip(RoundedCornerShape(2.dp)).background(CGreen))
                                    }
                                }
                            } else {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ─── Right floating toolbar ─────────────────────────────────────────
            Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                StoryToolBtn("Aa", activePanel == "text") { activePanel = if (activePanel == "text") null else "text" }
                StoryToolBtn("🎵", activePanel == "music" || selectedMusic != null) { activePanel = if (activePanel == "music") null else "music" }
                StoryToolBtn("✨", activePanel == "sticker") { activePanel = if (activePanel == "sticker") null else "sticker" }
                StoryToolBtn("🖼", selectedImageUri != null) { imagePicker.launch("image/*") }
                if (selectedImageUri == null) {
                    StoryToolBtn("🎨", activePanel == "bg") { activePanel = if (activePanel == "bg") null else "bg" }
                }
                StoryToolBtn("📽", activePanel == "filter") { activePanel = if (activePanel == "filter") null else "filter" }
            }

            // ─── Top bar ────────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).padding(12.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                    .clickable { musicPlayer?.release(); onDismiss() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // ─── Bottom section ─────────────────────────────────────────────────
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))))) {
                // Story type tabs
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    storyTypeList.forEach { (type, emoji, label) ->
                        val sel = storyType == type
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.12f))))
                            .border(1.dp, if (sel) ScrapbookDark else Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { storyType = type }.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(emoji, fontSize = 13.sp)
                                Text(label, fontFamily = BangersFontFamily,
                                    color = if (sel) ScrapbookDark else Color.White.copy(alpha = 0.85f), fontSize = 10.sp, letterSpacing = 0.3.sp)
                            }
                        }
                    }
                }
                // Post button
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.matchParentSize()
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ScrapbookDark.copy(alpha = 0.12f)))
                    Box(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                        .border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isPosting) {
                            musicPlayer?.release(); isPlayingPreview = false
                            postViewModel.createStory(
                                type = storyType,
                                content = textOverlay,
                                musicTrack = selectedMusic?.let { "${it.trackName} - ${it.artistName}" } ?: "",
                                textOverlay = textOverlay,
                                imageUri = selectedImageUri,
                                onComplete = { success -> if (success) onDismiss() }
                            )
                        }.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                        if (isPosting)
                            CircularProgressIndicator(color = ScrapbookDark, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else
                            Text("YOUR STORY →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp, letterSpacing = 1.sp)
                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 14.dp)
                    }
                }
            }

            // ─── Panel: Text editor ──────────────────────────────────────────────
            AnimatedVisibility(activePanel == "text",
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 16.dp),
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    .background(ComicGlassBg)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("TEXT OVERLAY", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 13.sp, letterSpacing = 1.sp)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.85f)).border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp)).padding(12.dp)) {
                        BasicTextField(
                            value = textOverlay, onValueChange = { if (it.length <= 60) textOverlay = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 16.sp, color = textColor),
                            cursorBrush = SolidColor(CGreen), modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (textOverlay.isEmpty()) Text("Type something…", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.45f), fontSize = 16.sp)
                                inner()
                            })
                    }
                    // Style picker
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        textStyleNames.forEachIndexed { i, name ->
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (textStyleIdx == i) CGreen else Color.White.copy(alpha = 0.46f))
                                .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                .clickable { textStyleIdx = i }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(name, fontFamily = BangersFontFamily,
                                    color = if (textStyleIdx == i) ScrapbookDark else ScrapbookDark.copy(alpha = 0.65f), fontSize = 10.sp)
                            }
                        }
                    }
                    // Color dots
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        textColorList.forEach { color ->
                            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(color)
                                .border(2.dp, if (textColor == color) ScrapbookDark else ScrapbookDark.copy(alpha = 0.25f), CircleShape)
                                .clickable { textColor = color })
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(CGreen.copy(alpha = 0.15f))
                        .border(1.dp, CGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable { activePanel = null }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text("DONE ✓", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 12.sp)
                    }
                }
            }

            // ─── Panel: Music ────────────────────────────────────────────────────
            AnimatedVisibility(activePanel == "music",
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ComicGlassBg)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🎵 ADD MUSIC", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp)
                        if (selectedMusic != null) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE63946).copy(alpha = 0.18f))
                                .clickable { selectedMusic = null; musicPlayer?.release(); isPlayingPreview = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("REMOVE", fontFamily = BangersFontFamily, color = Color(0xFFE63946), fontSize = 11.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(1.5.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp)) {
                            BasicTextField(
                                value = musicQuery, onValueChange = { musicQuery = it },
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                                cursorBrush = SolidColor(ScrapbookDark), singleLine = true, modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (musicQuery.isEmpty()) Text("Search song or artist…", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp)
                                    inner()
                                })
                        }
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.verticalGradient(listOf(CGreenMint, CGreen, CGreenDeep)))
                            .clickable { searchMusic(musicQuery) }, contentAlignment = Alignment.Center) {
                            if (musicSearching) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ScrapbookDark, strokeWidth = 2.dp)
                            else Icon(Icons.Filled.Search, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (musicResults.isNotEmpty()) {
                        Column(modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            musicResults.forEach { music ->
                                val isSel = selectedMusic?.trackId == music.trackId
                                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) CGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.44f))
                                    .border(1.dp, if (isSel) CGreen else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (music.artworkUrl.isNotBlank())
                                        AsyncImage(model = music.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                            modifier = Modifier.halftoneReveal(music.artworkUrl).size(40.dp).clip(RoundedCornerShape(6.dp)))
                                    else Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
                                        Text("🎵", fontSize = 18.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(music.trackName, fontFamily = NunitoFontFamily, color = if (isSel) CGreenDeep else ScrapbookDark,
                                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(music.artistName, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp, maxLines = 1)
                                    }
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                                        .background(if (isSel && isPlayingPreview) CGreen.copy(alpha = 0.2f) else ScrapbookDark.copy(alpha = 0.08f))
                                        .clickable {
                                            if (isSel && isPlayingPreview) { musicPlayer?.pause(); isPlayingPreview = false }
                                            else { selectedMusic = music; playPreview(music) }
                                        }, contentAlignment = Alignment.Center) {
                                        Icon(if (isSel && isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            null, tint = if (isSel) CGreenDeep else ScrapbookDark.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                    Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                                        .background(if (isSel) CGreen else ScrapbookDark.copy(alpha = 0.08f))
                                        .clickable { selectedMusic = music; musicPlayer?.release(); isPlayingPreview = false; activePanel = null },
                                        contentAlignment = Alignment.Center) {
                                        Icon(if (isSel) Icons.Filled.Check else Icons.Filled.Add, null,
                                            tint = if (isSel) ScrapbookDark else ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ─── Panel: Stickers ─────────────────────────────────────────────────
            AnimatedVisibility(activePanel == "sticker",
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ComicGlassBg)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("✨ STICKERS", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        gridItems(storyStickers) { sticker ->
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.92f))
                                .clickable { if (canvasStickers.size < 6) canvasStickers = canvasStickers + sticker; activePanel = null },
                                contentAlignment = Alignment.Center) {
                                Text(sticker, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ─── Panel: Background ───────────────────────────────────────────────
            AnimatedVisibility(activePanel == "bg" && selectedImageUri == null,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ComicGlassBg)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎨 BACKGROUND", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(storyGradientPresets.size) { idx ->
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                .background(Brush.verticalGradient(storyGradientPresets[idx]))
                                .border(2.dp, if (selectedGradientIdx == idx) ScrapbookDark else ScrapbookDark.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                .clickable { selectedGradientIdx = idx; activePanel = null })
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // ─── Panel: Filters ──────────────────────────────────────────────────
            AnimatedVisibility(activePanel == "filter",
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                Column(modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ComicGlassBg)
                    .border(2.dp, ScrapbookDark, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📽 RETRO FILTERS", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 14.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(RetroFilter.values().toList()) { filter ->
                            val isSel = selectedFilter == filter
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Brush.verticalGradient(listOf(CGreenMint, CGreen))
                                    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.44f))))
                                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                .clickable { selectedFilter = filter; activePanel = null }
                                .padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(filter.emoji, fontSize = 16.sp)
                                    Text(filter.label, fontFamily = BangersFontFamily,
                                        color = if (isSel) ScrapbookDark else ScrapbookDark.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StoryToolBtn(label: String, active: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(if (active) CGreen else Color.Black.copy(alpha = 0.55f))
        .border(1.5.dp, if (active) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f), CircleShape)
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Text(label, fontFamily = BangersFontFamily,
            color = if (active) ScrapbookDark else Color.White,
            fontSize = if (label == "Aa") 15.sp else 18.sp)
    }
}

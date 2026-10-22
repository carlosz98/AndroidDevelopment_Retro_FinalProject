package com.example.hubretro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// Stories, Instagram-style
//   • StoryBubbleRow  — round bubbles: green ring = new, grey ring = seen, "+" on yours
//   • StoryViewerScreen — full screen, progress bars on top, tap right/left for
//     next/previous, hold to pause, swipe down to close, auto-advances every 5s.
// ═══════════════════════════════════════════════════════════════════════════════

private const val STORY_DURATION_MS = 5000

/** Stories opened in this session, so rings turn grey right away (Firestore catches up later). */
object SeenStories {
    val ids: SnapshotStateList<String> = mutableStateListOf()
    fun mark(id: String) { if (id.isNotBlank() && id !in ids) ids.add(id) }
}

fun RetroStory.isSeenBy(uid: String?): Boolean =
    id in SeenStories.ids || (uid != null && uid in viewedBy)

// ─── Bubbles ──────────────────────────────────────────────────────────────────

@Composable
fun StoryBubbleRow(
    myStory: RetroStory?,
    stories: List<RetroStory>,
    currentUser: UserProfileData?,
    isLoading: Boolean,
    onAddStory: () -> Unit,
    onOpen: (RetroStory) -> Unit
) {
    val uid = currentUser?.uid
    // New stories first, then the ones you've seen
    val ordered = remember(stories, SeenStories.ids.size) {
        stories.sortedWith(compareBy<RetroStory> { it.isSeenBy(uid) }.thenByDescending { it.timestamp })
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "mine") {
            StoryBubble(
                label = "Your story",
                picUrl = currentUser?.profilePictureUrl.orEmpty(),
                initial = currentUser?.username?.take(1).orEmpty(),
                ring = if (myStory != null) StoryRing.MINE else StoryRing.NONE,
                showAdd = true,
                onClick = { if (myStory != null) onOpen(myStory) else onAddStory() },
                onAdd = onAddStory
            )
        }
        if (isLoading) {
            items(5) { StoryBubbleShimmer() }
        } else {
            items(ordered, key = { it.id }) { story ->
                StoryBubble(
                    label = story.authorUsername,
                    picUrl = story.authorPicUrl,
                    initial = story.authorUsername.take(1),
                    ring = if (story.isSeenBy(uid)) StoryRing.SEEN else StoryRing.NEW,
                    showAdd = false,
                    badge = storyTypeEmoji(story.type),
                    onClick = { onOpen(story) }
                )
            }
        }
    }
}

enum class StoryRing { NEW, SEEN, MINE, NONE }

@Composable
private fun StoryBubble(
    label: String,
    picUrl: String,
    initial: String,
    ring: StoryRing,
    showAdd: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
    onAdd: () -> Unit = {}
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "bubblePress")
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(140); pressed = false } }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp)) {
        Box(modifier = Modifier.size(74.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            // Ring
            Box(
                modifier = Modifier.fillMaxSize().drawBehind {
                    val w = 3.5.dp.toPx()
                    when (ring) {
                        StoryRing.NEW -> drawCircle(
                            brush = Brush.sweepGradient(listOf(CGreenMint, CGreen, CGreenDeep, CAcYellow, CGreenMint)),
                            radius = size.minDimension / 2f - w / 2f, style = Stroke(w)
                        )
                        StoryRing.MINE -> drawCircle(color = CGreen, radius = size.minDimension / 2f - w / 2f, style = Stroke(w))
                        StoryRing.SEEN -> drawCircle(color = ScrapbookDark.copy(alpha = 0.22f), radius = size.minDimension / 2f - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                        StoryRing.NONE -> {}
                    }
                }
            )
            // Photo
            Box(
                modifier = Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape)
                    .background(ComicGlassBgAlt).border(2.dp, ScrapbookDark, CircleShape)
                    .clickable { pressed = true; onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (picUrl.isNotBlank()) {
                    AsyncImage(model = picUrl, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(initial.uppercase().ifBlank { "?" }, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp)
                }
            }
            // "+" on your own bubble
            if (showAdd) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                        .background(CGreen).border(2.dp, ScrapbookDark, CircleShape).clickable { onAdd() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, contentDescription = "Add story", tint = ScrapbookDark, modifier = Modifier.size(16.dp)) }
            } else if (!badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape)
                        .background(Color.White).border(1.5.dp, ScrapbookDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text(badge, fontSize = 10.sp) }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label, fontFamily = NunitoFontFamily, fontWeight = if (ring == StoryRing.NEW) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (ring == StoryRing.SEEN) ScrapbookTextMuted else ScrapbookDark, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StoryBubbleShimmer() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp)) {
        Box(Modifier.size(70.dp).clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.08f)))
        Spacer(Modifier.height(7.dp))
        Box(Modifier.width(46.dp).height(9.dp).clip(RoundedCornerShape(5.dp)).background(ScrapbookDark.copy(alpha = 0.08f)))
    }
}

// ─── Full-screen viewer ───────────────────────────────────────────────────────

@Composable
fun StoryViewerScreen(
    stories: List<RetroStory>,
    startStory: RetroStory,
    currentUid: String?,
    postViewModel: PostViewModel,
    onDismiss: () -> Unit,
    onAddStory: () -> Unit = {}
) {
    if (stories.isEmpty()) { onDismiss(); return }
    var index by remember { mutableIntStateOf(stories.indexOfFirst { it.id == startStory.id }.coerceAtLeast(0)) }
    var paused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    val story = stories[index.coerceIn(0, stories.lastIndex)]
    val isMine = story.authorUid == currentUid
    // Highlights (save your story to your profile)
    var myHighlights by remember { mutableStateOf<List<ProfileHighlight>>(emptyList()) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    var savedNote by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentUid) { if (currentUid != null && stories.any { it.authorUid == currentUid }) myHighlights = ProfileData.extras(currentUid).highlights }
    LaunchedEffect(savedNote) { if (savedNote != null) { kotlinx.coroutines.delay(1800); savedNote = null } }
    LaunchedEffect(showHighlightPicker) { if (showHighlightPicker) paused = true }

    fun next() { if (index < stories.lastIndex) index++ else onDismiss() }
    fun prev() { if (index > 0) index-- else scope.launch { progress.snapTo(0f) } }

    // Mark seen + run the timer for the current story
    var timerFor by remember { mutableStateOf("") }
    LaunchedEffect(story.id) {
        SeenStories.mark(story.id)
        if (!isMine && currentUid != null && currentUid !in story.viewedBy) postViewModel.markStoryViewed(story.authorUid, story.id)
    }
    LaunchedEffect(story.id, paused) {
        if (timerFor != story.id) { progress.snapTo(0f); timerFor = story.id }
        if (!paused) {
            val remaining = ((1f - progress.value) * STORY_DURATION_MS).toInt().coerceAtLeast(1)
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            if (progress.value >= 1f) next()
        } else {
            progress.stop()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black)
                .graphicsLayer {
                    translationY = dragY.coerceAtLeast(0f)
                    val f = 1f - (dragY.coerceAtLeast(0f) / 2400f)
                    scaleX = f; scaleY = f
                }
                .pointerInput(story.id) {
                    detectVerticalDragGestures(
                        onDragStart = { paused = true },
                        onDragEnd = {
                            if (dragY > with(density) { 140.dp.toPx() }) onDismiss() else { dragY = 0f; paused = false }
                        },
                        onDragCancel = { dragY = 0f; paused = false },
                        onVerticalDrag = { _, amount -> dragY += amount }
                    )
                }
                .pointerInput(story.id) {
                    detectTapGestures(
                        onPress = {
                            paused = true
                            tryAwaitRelease()
                            paused = false
                        },
                        onTap = { offset -> if (offset.x < size.width * 0.33f) prev() else next() }
                    )
                }
        ) {
            StoryContent(story)
            savedNote?.let { note ->
                Box(Modifier.align(Alignment.Center).clip(RoundedCornerShape(14.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(note, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                }
            }
            if (showHighlightPicker) {
                AddToHighlightDialog(
                    existing = myHighlights.map { it.title },
                    onDismiss = { showHighlightPicker = false; paused = false },
                    onPick = { title ->
                        showHighlightPicker = false
                        scope.launch {
                            myHighlights = ProfileData.addToHighlight(title, story, myHighlights)
                            savedNote = "★ Saved to $title"
                            paused = false
                        }
                    }
                )
            }

            // Top: progress bars + author row
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                    .statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    stories.forEachIndexed { i, _ ->
                        val fill = when {
                            i < index -> 1f
                            i == index -> progress.value
                            else -> 0f
                        }
                        Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(Color.White))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(ComicGlassBgAlt).border(2.dp, CGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (story.authorPicUrl.isNotBlank()) AsyncImage(model = story.authorPicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Text(story.authorUsername.take(1).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (isMine) "Your story" else story.authorUsername, fontFamily = BangersFontFamily, color = Color.White, fontSize = 16.sp, letterSpacing = 0.5.sp)
                        Text("${storyTypeEmoji(story.type)} ${storyTypeLabel(story.type)} · ${timeAgoFromMillis(story.timestamp)}", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                    if (paused) Text("PAUSED", fontFamily = BangersFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(end = 10.dp))
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }

            // Bottom: music chip + seen-by (yours) or quick reactions hint
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (story.musicTrack.isNotBlank()) {
                    Row(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.92f)).border(2.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🎵", fontSize = 13.sp)
                        Text(story.musicTrack, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (isMine) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text("Seen by ${story.viewedBy.size}", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                                    .clickable { showHighlightPicker = true }.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("★ HIGHLIGHT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp) }
                            Box(
                                Modifier.clip(RoundedCornerShape(16.dp)).background(CGreen).border(2.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                                    .clickable { onDismiss(); onAddStory() }.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) { Text("+ ADD", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 13.sp) }
                        }
                    }
                } else {
                    Text("Tap to skip · hold to pause · swipe down to close", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

/** The story itself: photo (with its retro filter) or a green gradient card with the game. */
@Composable
private fun StoryContent(story: RetroStory) {
    val filter = remember(story.filter) { RetroFilter.values().find { it.name == story.filter } ?: RetroFilter.NONE }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (story.imageUrl.isNotBlank()) {
            AsyncImage(
                model = story.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                colorFilter = filter.toColorFilter(), modifier = Modifier.fillMaxSize()
            )
            if (story.filter == "SCANLINE" || story.filter == "VHS") {
                Column(Modifier.fillMaxSize()) {
                    repeat(90) { Box(Modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.18f))); Spacer(Modifier.height(4.dp)) }
                }
            }
        } else {
            val accent = storyTypeColor(story.type)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CGreenDeep, accent, ScrapbookPurple))))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                if (story.gameCoverUrl.isNotBlank()) {
                    Box(
                        Modifier.size(170.dp).drawBehind {
                            drawRoundRect(ScrapbookDark, topLeft = Offset(6.dp.toPx(), 6.dp.toPx()), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
                        }.clip(RoundedCornerShape(16.dp)).border(3.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                    ) { AsyncImage(model = story.gameCoverUrl, contentDescription = story.gameName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                    Spacer(Modifier.height(16.dp))
                }
                if (story.gameName.isNotBlank()) {
                    Text(story.gameName.uppercase(), fontFamily = BangersFontFamily, color = Color.White, fontSize = 30.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp)
                }
                if (story.content.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(story.content, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                }
            }
        }
        // Text overlay + sticker, like the composer draws them
        if (story.textOverlay.isNotBlank()) {
            Text(
                story.textOverlay, fontFamily = BangersFontFamily, color = Color.White, fontSize = 34.sp, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        if (story.sticker.isNotBlank()) {
            Text(story.sticker, fontSize = 64.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 150.dp).graphicsLayer { rotationZ = 10f })
        }
    }
}

private fun storyTypeLabel(type: String): String = when (type) {
    "now_playing" -> "Now playing"
    "flashback" -> "Flashback"
    "challenge" -> "Challenge"
    else -> type.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

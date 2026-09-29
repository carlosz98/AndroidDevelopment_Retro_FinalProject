package com.example.hubretro

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════════
// DAILY QUESTS — 3 small tasks a day, tracked locally, reward real XP
// ═══════════════════════════════════════════════════════════════════════════════

object DailyQuests {
    data class Quest(val id: String, val emoji: String, val title: String, val goal: Int, val xp: Int)

    const val READ_NEWS = "read_news"
    const val WARP = "warp"
    const val FOLLOW = "follow"
    const val SEARCH = "search"
    const val HIGHER_LOWER = "higher_lower"

    private val pool = listOf(
        Quest(READ_NEWS, "📰", "Read a news story", 1, 20),
        Quest(WARP, "🌀", "Warp to 3 different pages", 3, 30),
        Quest(FOLLOW, "🤝", "Follow a new player", 1, 25),
        Quest(SEARCH, "🔍", "Search for a game or player", 1, 15),
        Quest(HIGHER_LOWER, "🎲", "Hit a 3-streak in Higher or Lower", 1, 30)
    )

    private const val PREFS = "retrohub_quests"
    val progress = mutableStateMapOf<String, Int>()
    val claimed = mutableStateMapOf<String, Boolean>()
    private var loadedDay = ""

    fun today(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun questsForToday(): List<Quest> =
        pool.shuffled(Random(today().toLong())).take(3)

    fun load(context: Context) {
        val day = today()
        if (loadedDay == day) return
        loadedDay = day
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pool.forEach { q ->
            progress[q.id] = prefs.getInt("p_${day}_${q.id}", 0)
            claimed[q.id] = prefs.getBoolean("c_${day}_${q.id}", false)
        }
    }

    /**
     * Records progress. For WARP pass uniqueKey (destination) so repeats don't count twice.
     * Auto-claims XP + shows a toast when a quest in today's list completes.
     */
    fun report(context: Context, id: String, achievementsViewModel: AchievementsViewModel?, uniqueKey: String? = null) {
        load(context)
        val day = today()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val quest = questsForToday().firstOrNull { it.id == id } ?: return
        if (claimed[id] == true) return
        if (uniqueKey != null) {
            val setKey = "u_${day}_$id"
            val seen = prefs.getStringSet(setKey, emptySet())!!.toMutableSet()
            if (!seen.add(uniqueKey)) return
            prefs.edit().putStringSet(setKey, seen).apply()
        }
        val newValue = ((progress[id] ?: 0) + 1).coerceAtMost(quest.goal)
        progress[id] = newValue
        prefs.edit().putInt("p_${day}_$id", newValue).apply()
        if (newValue >= quest.goal) {
            claimed[id] = true
            prefs.edit().putBoolean("c_${day}_$id", true).apply()
            achievementsViewModel?.awardXP(quest.xp, "QUEST")
            AchievementToastBus.queue.add(
                Badge(
                    id = "quest_${day}_$id",
                    name = "QUEST COMPLETE",
                    description = "${quest.title} · +${quest.xp} XP",
                    emoji = quest.emoji,
                    color = CGreen,
                    isEarned = true
                )
            )
        }
    }
}

@Composable
fun DailyQuestsCard() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { DailyQuests.load(context) }
    val quests = remember { DailyQuests.questsForToday() }
    val doneCount = quests.count { DailyQuests.claimed[it.id] == true }
    val totalXp = quests.sumOf { it.xp }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp)
            .clip(RoundedCornerShape(16.dp)).background(CAcYellow))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            // Header strip
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(CAcYellowL, CAcYellow)))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.White.copy(alpha = 0.35f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚔️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DAILY QUESTS", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp,
                            letterSpacing = 1.5.sp, color = ScrapbookDark))
                        Text("Resets at midnight · up to $totalXp XP", style = TextStyle(fontFamily = NunitoFontFamily,
                            fontSize = 11.sp, color = ScrapbookDark.copy(alpha = 0.65f)))
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        RollingCounterText("$doneCount/${quests.size}", TextStyle(fontFamily = BangersFontFamily,
                            fontSize = 15.sp, color = CAcYellowL))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.5.dp).background(ScrapbookDark))
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quests.forEach { q ->
                    val prog = DailyQuests.progress[q.id] ?: 0
                    val done = DailyQuests.claimed[q.id] == true
                    val fill by animateFloatAsState((prog.toFloat() / q.goal).coerceIn(0f, 1f),
                        tween(700, easing = FastOutSlowInEasing), label = "questFill_${q.id}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (done) CGreen else Color.White)
                                .border(2.dp, ScrapbookDark, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(if (done) "✔" else q.emoji, fontSize = 16.sp, color = ScrapbookDark) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.title, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp,
                                color = if (done) ScrapbookTextMuted else ScrapbookDark,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                .background(ScrapbookDark.copy(alpha = 0.1f))
                                .border(1.dp, ScrapbookDark.copy(alpha = 0.4f), RoundedCornerShape(4.dp))) {
                                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(if (done) CGreen else CAcYellow))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (done) "DONE" else "+${q.xp} XP", style = TextStyle(fontFamily = BangersFontFamily,
                            fontSize = 13.sp, color = if (done) CGreenDeep else ScrapbookDark))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WARP ZONE — comic-panel portals to every other page in the app
// ═══════════════════════════════════════════════════════════════════════════════

data class WarpDestination(
    val label: String,
    val emoji: String,
    val color: Color,
    val badge: String? = null,
    val onClick: () -> Unit
)

@Composable
fun WarpZoneGrid(destinations: List<WarpDestination>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        destinations.chunked(4).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { i, dest ->
                    WarpTile(dest, index = rowIndex * 4 + i, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WarpTile(dest: WarpDestination, index: Int, modifier: Modifier = Modifier) {
    val tap = rememberTapHaptic()
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 3f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "warpPress")
    val tilt = if (index % 2 == 0) -1.5f else 1.5f
    val bob by rememberGlowRange(0f, 1f)

    Box(modifier = modifier.stampIn("warp_${dest.label}", delayMs = index * 60).graphicsLayer { rotationZ = tilt }) {
        Box(modifier = Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp)
            .clip(RoundedCornerShape(10.dp)).background(ScrapbookDark))
        Column(
            modifier = Modifier.fillMaxWidth()
                .offset(x = press.dp, y = press.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                .clickable {
                    pressed = true
                    tap()
                    Chiptune.play(Chiptune.Sfx.POP)
                    dest.onClick()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp).background(dest.color),
                contentAlignment = Alignment.Center
            ) {
                HalftoneDots(Modifier.matchParentSize(), spacing = 5.dp, dotRadius = 1.1.dp, color = Color.White.copy(alpha = 0.3f))
                Text(dest.emoji, fontSize = 24.sp, modifier = Modifier.graphicsLayer { translationY = (bob - 0.5f) * 5f })
                if (dest.badge != null) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(3.dp)
                            .clip(RoundedCornerShape(4.dp)).background(CAcRed)
                            .border(1.dp, ScrapbookDark, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(dest.badge, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 8.sp, color = Color.White))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(ScrapbookDark))
            Text(
                dest.label,
                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 11.sp, letterSpacing = 0.5.sp,
                    color = ScrapbookDark, textAlign = TextAlign.Center),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(140); pressed = false } }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WHO TO FOLLOW — swipeable card deck (right = follow, left = skip)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SwipeFollowDeck(
    users: List<UserProfileData>,
    onFollow: (UserProfileData) -> Unit,
    onOpen: (UserProfileData) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val burst = rememberBurstState()
    val tap = rememberTapHaptic()
    var busy by remember { mutableStateOf(false) }
    val threshold = 280f

    val top = users.getOrNull(index)

    val fling: (Boolean) -> Unit = { right ->
        val user = users.getOrNull(index)
        if (user != null && !busy) {
            busy = true
            if (right) { onFollow(user); burst.fire("FOLLOWED!", CGreen) } else tap()
            scope.launch {
                launch { offsetY.animateTo(offsetY.value - 60f, tween(260)) }
                offsetX.animateTo(if (right) 1500f else -1500f, tween(260, easing = FastOutLinearInEasing))
                index++
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
                busy = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(262.dp), contentAlignment = Alignment.TopCenter) {
            if (top == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏁", fontSize = 40.sp)
                        Text("ALL CAUGHT UP!", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 24.sp, color = ScrapbookDark))
                        Text("You've seen every player for now.", style = TextStyle(fontFamily = NunitoFontFamily,
                            fontSize = 12.sp, color = ScrapbookTextMuted))
                        if (users.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            ComicHeaderChip("↺ SHUFFLE AGAIN") { index = 0 }
                        }
                    }
                }
            } else {
                // cards behind
                for (depth in 2 downTo 1) {
                    val u = users.getOrNull(index + depth) ?: continue
                    key(u.uid) {
                        DeckCard(
                            user = u, hint = 0f,
                            modifier = Modifier.graphicsLayer {
                                val s = 1f - depth * 0.05f
                                scaleX = s; scaleY = s
                                translationY = depth * 12.dp.toPx()
                                rotationZ = if (depth == 1) 2.5f else -2.5f
                            }
                        )
                    }
                }
                key(top.uid) {
                    DeckCard(
                        user = top,
                        hint = (offsetX.value / threshold).coerceIn(-1f, 1f),
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = offsetX.value
                                translationY = offsetY.value
                                rotationZ = offsetX.value / 30f
                            }
                            .pointerInput(top.uid) {
                                // Horizontal only, so vertical scrolling of the feed still works over the card
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            offsetX.value > threshold -> fling(true)
                                            offsetX.value < -threshold -> fling(false)
                                            else -> scope.launch {
                                                launch { offsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f)) }
                                                offsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                            }
                                        }
                                    }
                                ) { change, dx ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dx)
                                        offsetY.snapTo(-abs(offsetX.value) * 0.08f)
                                    }
                                }
                            }
                            .clickable { onOpen(top) }
                    )
                }
            }
            ComicBurst(burst, Modifier.align(Alignment.Center), burstSize = 140.dp)
        }
        if (top != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                RetroGlassButton(text = "✕ SKIP", onClick = { fling(false) }, modifier = Modifier.weight(1f))
                RetroGlassButton(text = "★ FOLLOW", onClick = { fling(true) }, modifier = Modifier.weight(1f))
            }
            Text("Swipe right to follow · left to skip · tap to view",
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted,
                    textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DeckCard(user: UserProfileData, hint: Float, modifier: Modifier = Modifier) {
    val favGame = (user.topGames.firstOrNull()?.get("name") as? String).orEmpty()
    Box(modifier = modifier.fillMaxWidth().height(240.dp)) {
        Box(modifier = Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp)
            .clip(RoundedCornerShape(16.dp)).background(CGreen))
        Column(
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
        ) {
            // Banner
            Box(modifier = Modifier.fillMaxWidth().height(92.dp)
                .background(Brush.horizontalGradient(listOf(CGreenDeep, CGreen, CGreenMint)))) {
                if (user.bannerUrl.isNotBlank()) {
                    AsyncImage(model = user.bannerUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.halftoneReveal(user.bannerUrl).fillMaxSize())
                }
                HalftoneDots(Modifier.matchParentSize(), spacing = 6.dp, dotRadius = 1.2.dp, color = Color.White.copy(alpha = 0.25f))
            }
            Box(Modifier.fillMaxWidth().height(2.5.dp).background(ScrapbookDark))
            Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(64.dp).offset(y = (-34).dp).clip(CircleShape)
                        .background(CGreenMint).border(3.dp, ScrapbookDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.profilePictureUrl.isNotBlank()) {
                        AsyncImage(model = user.profilePictureUrl, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.halftoneReveal(user.profilePictureUrl).fillMaxSize().clip(CircleShape))
                    } else {
                        Text(user.username.take(1).uppercase(), style = TextStyle(fontFamily = BangersFontFamily,
                            fontSize = 26.sp, color = ScrapbookDark))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(user.username.ifBlank { "Player" }, style = TextStyle(fontFamily = BangersFontFamily,
                        fontSize = 22.sp, color = ScrapbookDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (user.userHandle.isNotBlank()) Text(user.userHandle, style = TextStyle(fontFamily = NunitoFontFamily,
                        fontSize = 12.sp, color = ScrapbookTextMuted), maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatCount(user.followersCount), style = TextStyle(fontFamily = BangersFontFamily,
                        fontSize = 18.sp, color = CGreenDeep))
                    Text("FOLLOWERS", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 9.sp, color = ScrapbookTextMuted))
                }
            }
            Column(modifier = Modifier.padding(horizontal = 14.dp).offset(y = (-18).dp)) {
                Text(user.bio, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookDark,
                    lineHeight = 16.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (favGame.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CAcYellowL)
                            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("🎮 Favorite: $favGame", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 11.sp,
                            color = ScrapbookDark), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // FOLLOW / SKIP stamps while dragging
        if (hint > 0.15f) {
            DeckStamp("FOLLOW!", CGreen, -14f, Modifier.align(Alignment.TopStart).padding(16.dp).graphicsLayer { alpha = hint })
        } else if (hint < -0.15f) {
            DeckStamp("SKIP", CAcRed, 14f, Modifier.align(Alignment.TopEnd).padding(16.dp).graphicsLayer { alpha = abs(hint) })
        }
    }
}

@Composable
private fun DeckStamp(text: String, color: Color, rotation: Float, modifier: Modifier) {
    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotation }
            .clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.9f))
            .border(3.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(text, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 28.sp, letterSpacing = 2.sp, color = color))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTRA! EXTRA! — top story spins in like a thrown newspaper (once per story)
// ═══════════════════════════════════════════════════════════════════════════════

private val spunKeys = HashSet<Any>()

@Composable
fun Modifier.newspaperSpin(key: Any): Modifier {
    val first = remember(key) { spunKeys.add(key) }
    if (!first) return this
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(150)
        p.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        Chiptune.play(Chiptune.Sfx.STAMP)
    }
    return this.graphicsLayer {
        val v = p.value
        rotationZ = (1f - v) * 720f
        scaleX = 0.1f + 0.9f * v
        scaleY = 0.1f + 0.9f * v
        alpha = (v * 1.5f).coerceIn(0f, 1f)
    }
}

@Composable
fun ExtraExtraBanner() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier.graphicsLayer { rotationZ = -3f }
                .clip(RoundedCornerShape(6.dp)).background(CAcRed)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text("EXTRA! EXTRA!", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp,
                letterSpacing = 1.5.sp, color = Color.White))
        }
        ArcadeBlinkText("● HOT OFF THE PRESS", TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp,
            letterSpacing = 1.sp, color = CAcRed))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ROTATING TYPEWRITER HINT — search placeholder that types suggestions
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun RotatingTypewriterHint(hints: List<String>, style: TextStyle) {
    var idx by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    LaunchedEffect(hints) {
        while (true) {
            val h = hints[idx % hints.size]
            count = 0
            while (count < h.length) { delay(45); count++ }
            delay(1600)
            while (count > 0) { delay(18); count-- }
            delay(250)
            idx = (idx + 1) % hints.size
        }
    }
    Text(hints[idx % hints.size].take(count) + "▌", style = style, maxLines = 1)
}

package com.example.hubretro

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import kotlin.random.Random

// ─── Triggers ─────────────────────────────────────────────────────────────────

sealed class RobotTrigger(val key: String) {
    object AppEntry : RobotTrigger("app_entry")
    object ComebackEntry : RobotTrigger("comeback_entry")
    data class ScreenEnter(val screen: String) : RobotTrigger("screen_$screen")
    data class GameViewed(val gameName: String, val platform: String, val rating: Double?) : RobotTrigger("game_${gameName.lowercase()}")
    data class AlbumPlaying(val albumTitle: String, val artist: String) : RobotTrigger("album_${albumTitle.lowercase()}")
    data class ArticleOpened(val title: String, val category: String) : RobotTrigger("article_${title.lowercase()}")
    data class MagazineOpened(val title: String) : RobotTrigger("magazine_${title.lowercase()}")
    data class Idle(val screen: String, val level: Int) : RobotTrigger("idle_${screen}_$level")
    data class PendingMessage(val fromUsername: String, val hoursWaiting: Int) : RobotTrigger("pending_${fromUsername.lowercase()}")
    data class CommunityFindPosted(val itemName: String) : RobotTrigger("find_${itemName.lowercase()}")
    data class MarketplaceSearch(val query: String) : RobotTrigger("market_${query.lowercase()}")
    data class PriceDropAlert(val query: String, val oldPrice: String, val newPrice: String) : RobotTrigger("pricedrop_${query.lowercase()}")
    data class EmptyState(val screen: String) : RobotTrigger("empty_$screen")
    data class StreamingStarted(val source: String) : RobotTrigger("stream_$source")
    data class NearAchievement(val badgeName: String, val remaining: Int) : RobotTrigger("near_${badgeName.lowercase()}")
    data class FirstWatchlistItem(val query: String) : RobotTrigger("first_watchlist")
    data class ListingViewedTwice(val title: String) : RobotTrigger("viewed_twice_${title.lowercase()}")
    data class RarityHit(val tier: String, val query: String) : RobotTrigger("rarity_${tier}_${query.lowercase()}")
    object SessionMilestone : RobotTrigger("session_milestone")
    object HypeMoment : RobotTrigger("hype_moment")
}

// ─── Robot message event ───────────────────────────────────────────────────────

data class RobotMessage(
    val text: String,
    val triggerKey: String,
    val accentColor: Color = CGreen,
    val stance: RobotStance = RobotStance.STANDING
)

// ─── Local memory of recently seen items (for callbacks) ──────────────────────

private data class SeenItem(val label: String, val type: String, val atMs: Long)

// ─── RobotBrain — central decision engine ──────────────────────────────────────

object RobotBrain {

    // ─── Centralized per-screen accent colors ──────────────────────────────────
    private val screenAccentColors = mapOf(
        "games" to CGreenDeep,
        "magazines" to CAcRed,
        "albums" to CAcPurple,
        "articles" to CAcBlue,
        "streams" to CAcPurple,
        "marketplace" to CAcYellow,
        "discover" to CGreenDeep,
        "retrobytes" to CAcRed
    )

    fun colorForScreen(screen: String): Color = screenAccentColors[screen.lowercase()] ?: CGreen

    private val _currentMessage = MutableStateFlow<RobotMessage?>(null)
    val currentMessage: StateFlow<RobotMessage?> = _currentMessage.asStateFlow()

    private var prefs: SharedPreferences? = null

    private val firedKeysThisSession = mutableSetOf<String>()
    private val recentTemplateIndices = mutableMapOf<String, MutableList<Int>>()
    private val recentlySeenItems = ArrayDeque<SeenItem>()
    private val recentScreens = ArrayDeque<String>()
    private var lastSpokeAtMs = 0L
    private var gamesViewedThisSession = 0
    private var muted = false

    private const val GLOBAL_COOLDOWN_MS = 18_000L
    private const val MAX_SEEN_MEMORY = 5
    private const val MAX_SCREEN_MEMORY = 6
    private const val EASTER_EGG_CHANCE = 0.02

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("robot_brain_prefs", Context.MODE_PRIVATE)
        muted = prefs?.getBoolean("muted", false) ?: false
    }

    fun isMuted(): Boolean = muted

    fun setMuted(value: Boolean) {
        muted = value
        prefs?.edit()?.putBoolean("muted", value)?.apply()
    }

    private fun totalSessionCount(): Int = prefs?.getInt("total_sessions", 0) ?: 0
    private fun incrementSessionCount() {
        val n = totalSessionCount() + 1
        prefs?.edit()?.putInt("total_sessions", n)?.apply()
    }
    private fun lastSeenTimestamp(): Long = prefs?.getLong("last_seen_ts", 0L) ?: 0L
    private fun updateLastSeenTimestamp() {
        prefs?.edit()?.putLong("last_seen_ts", System.currentTimeMillis())?.apply()
    }

    fun startSession() {
        firedKeysThisSession.clear()
        recentTemplateIndices.clear()
        recentlySeenItems.clear()
        recentScreens.clear()
        lastSpokeAtMs = 0L
        gamesViewedThisSession = 0

        val daysSinceLastSeen = if (lastSeenTimestamp() > 0L) {
            (System.currentTimeMillis() - lastSeenTimestamp()) / 86_400_000L
        } else -1L

        incrementSessionCount()
        updateLastSeenTimestamp()

        if (daysSinceLastSeen >= 3) {
            notify(RobotTrigger.ComebackEntry)
        } else {
            notify(RobotTrigger.AppEntry)
        }
    }

    fun dismiss() {
        _currentMessage.value = null
    }

    private fun rememberItem(label: String, type: String) {
        recentlySeenItems.addLast(SeenItem(label, type, System.currentTimeMillis()))
        while (recentlySeenItems.size > MAX_SEEN_MEMORY) recentlySeenItems.removeFirst()
    }

    private fun rememberScreen(screen: String) {
        if (recentScreens.lastOrNull() != screen) {
            recentScreens.addLast(screen)
            while (recentScreens.size > MAX_SCREEN_MEMORY) recentScreens.removeFirst()
        }
    }

    private fun detectBouncePattern(): Boolean {
        if (recentScreens.size < 4) return false
        val last4 = recentScreens.toList().takeLast(4)
        return last4[0] == last4[2] && last4[1] == last4[3] && last4[0] != last4[1]
    }

    private fun lastSeenOfType(type: String, excluding: String): SeenItem? =
        recentlySeenItems.lastOrNull { it.type == type && it.label != excluding }

    fun notify(trigger: RobotTrigger, accentColor: Color? = null) {
        if (muted) return

        val resolvedColor = accentColor ?: when (trigger) {
            is RobotTrigger.ScreenEnter -> colorForScreen(trigger.screen)
            is RobotTrigger.EmptyState -> colorForScreen(trigger.screen)
            is RobotTrigger.MarketplaceSearch, is RobotTrigger.PriceDropAlert,
            is RobotTrigger.FirstWatchlistItem, is RobotTrigger.ListingViewedTwice,
            is RobotTrigger.RarityHit -> colorForScreen("marketplace")
            else -> CGreen
        }

        when (trigger) {
            is RobotTrigger.GameViewed -> {
                rememberItem(trigger.gameName, "game")
                gamesViewedThisSession++
                if (gamesViewedThisSession == 5) {
                    notify(RobotTrigger.SessionMilestone, colorForScreen("games"))
                }
            }
            is RobotTrigger.AlbumPlaying -> rememberItem(trigger.albumTitle, "album")
            is RobotTrigger.ArticleOpened -> rememberItem(trigger.title, "article")
            is RobotTrigger.MagazineOpened -> rememberItem(trigger.title, "magazine")
            is RobotTrigger.ScreenEnter -> rememberScreen(trigger.screen)
            else -> {}
        }

        val isRepeatable = trigger is RobotTrigger.Idle || trigger is RobotTrigger.PendingMessage || trigger is RobotTrigger.PriceDropAlert
        if (!isRepeatable && firedKeysThisSession.contains(trigger.key)) return

        val now = System.currentTimeMillis()
        if (now - lastSpokeAtMs < GLOBAL_COOLDOWN_MS && trigger !is RobotTrigger.PendingMessage) return

        val text = buildMessage(trigger)
        if (text.isBlank()) return

        val stance = when {
            trigger is RobotTrigger.Idle && trigger.level >= 2 -> RobotStance.SITTING
            trigger is RobotTrigger.AppEntry || trigger is RobotTrigger.ComebackEntry ||
                    trigger is RobotTrigger.HypeMoment || trigger is RobotTrigger.CommunityFindPosted ||
                    trigger is RobotTrigger.SessionMilestone -> RobotStance.SALUTING
            else -> RobotStance.STANDING
        }

        firedKeysThisSession.add(trigger.key)
        lastSpokeAtMs = now
        _currentMessage.value = RobotMessage(text = text, triggerKey = trigger.key, accentColor = resolvedColor, stance = stance)
    }

    private fun pickTemplate(poolKey: String, pool: List<String>): String {
        if (pool.isEmpty()) return ""
        val recent = recentTemplateIndices.getOrPut(poolKey) { mutableListOf() }
        val candidates = pool.indices.filter { it !in recent }
        val chosenIndex = if (candidates.isNotEmpty()) candidates.random() else pool.indices.random()
        recent.add(chosenIndex)
        while (recent.size > 2) recent.removeAt(0)
        return pool[chosenIndex]
    }

    private fun timeOfDayModifier(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 0..4 -> "lateNight"
            hour in 5..10 -> "morning"
            hour in 17..21 -> "evening"
            hour in 22..23 -> "lateNight"
            else -> "midday"
        }
    }

    private fun buildMessage(trigger: RobotTrigger): String {
        if (Random.nextDouble() < EASTER_EGG_CHANCE) {
            val egg = pickTemplate("easter_egg", RobotTemplates.easterEggs)
            if (egg.isNotBlank()) return egg
        }

        return when (trigger) {
            is RobotTrigger.AppEntry -> {
                val sessions = totalSessionCount()
                val tod = timeOfDayModifier()
                val pool = when {
                    sessions <= 1 -> RobotTemplates.greetingFirstTime
                    sessions in 2..6 -> RobotTemplates.greetingEarly
                    else -> RobotTemplates.greetingFamiliar
                }
                val timePool = RobotTemplates.timeOfDayGreetings[tod] ?: emptyList()
                val combined = if (timePool.isNotEmpty() && Random.nextBoolean()) timePool else pool
                pickTemplate("app_entry", combined)
            }

            is RobotTrigger.ComebackEntry -> {
                pickTemplate("comeback", RobotTemplates.comebackGreetings)
            }

            is RobotTrigger.ScreenEnter -> {
                val pool = RobotTemplates.screenGreetings[trigger.screen] ?: return ""
                pickTemplate("screen_${trigger.screen}", pool)
            }

            is RobotTrigger.GameViewed -> {
                val era = when {
                    trigger.platform.contains("NES", true) || trigger.platform.contains("SEGA", true) -> "8-bit"
                    trigger.platform.contains("SNES", true) -> "16-bit"
                    else -> "classic"
                }
                val ratingTier = when {
                    trigger.rating == null -> "unrated"
                    trigger.rating >= 80 -> "loved"
                    trigger.rating >= 60 -> "good"
                    else -> "mixed"
                }
                val callback = lastSeenOfType("game", excluding = trigger.gameName)
                val pool = if (callback != null && Random.nextInt(3) == 0) {
                    RobotTemplates.gameCallback
                } else {
                    RobotTemplates.gameByRating[ratingTier] ?: RobotTemplates.gameGeneric
                }
                val raw = pickTemplate("game_${ratingTier}", pool)
                raw.replace("{game}", trigger.gameName)
                    .replace("{platform}", trigger.platform)
                    .replace("{era}", era)
                    .replace("{prevItem}", callback?.label ?: "")
            }

            is RobotTrigger.AlbumPlaying -> {
                val raw = pickTemplate("album", RobotTemplates.albumPlaying)
                raw.replace("{album}", trigger.albumTitle).replace("{artist}", trigger.artist)
            }

            is RobotTrigger.ArticleOpened -> {
                val raw = pickTemplate("article", RobotTemplates.articleOpened)
                raw.replace("{title}", trigger.title).replace("{category}", trigger.category)
            }

            is RobotTrigger.MagazineOpened -> {
                val raw = pickTemplate("magazine", RobotTemplates.magazineOpened)
                raw.replace("{title}", trigger.title)
            }

            is RobotTrigger.Idle -> {
                val pool = RobotTemplates.idleByLevel[trigger.level.coerceIn(0, 2)] ?: return ""
                pickTemplate("idle_${trigger.level}", pool)
            }

            is RobotTrigger.PendingMessage -> {
                val raw = pickTemplate("pending", RobotTemplates.pendingMessage)
                raw.replace("{username}", trigger.fromUsername).replace("{hours}", trigger.hoursWaiting.toString())
            }

            is RobotTrigger.CommunityFindPosted -> {
                val raw = pickTemplate("find", RobotTemplates.communityFindPosted)
                raw.replace("{item}", trigger.itemName)
            }

            is RobotTrigger.MarketplaceSearch -> {
                val raw = pickTemplate("market", RobotTemplates.marketplaceSearch)
                raw.replace("{query}", trigger.query)
            }

            is RobotTrigger.PriceDropAlert -> {
                val raw = pickTemplate("pricedrop", RobotTemplates.priceDropAlert)
                raw.replace("{query}", trigger.query).replace("{old}", trigger.oldPrice).replace("{new}", trigger.newPrice)
            }

            is RobotTrigger.EmptyState -> {
                val pool = RobotTemplates.emptyStates[trigger.screen] ?: RobotTemplates.emptyStateGeneric
                pickTemplate("empty_${trigger.screen}", pool)
            }

            is RobotTrigger.StreamingStarted -> {
                val raw = pickTemplate("stream", RobotTemplates.streamingStarted)
                raw.replace("{source}", trigger.source)
            }

            is RobotTrigger.NearAchievement -> {
                val raw = pickTemplate("near_achievement", RobotTemplates.nearAchievement)
                raw.replace("{badge}", trigger.badgeName).replace("{remaining}", trigger.remaining.toString())
            }

            is RobotTrigger.FirstWatchlistItem -> {
                val raw = pickTemplate("first_watchlist", RobotTemplates.firstWatchlistItem)
                raw.replace("{query}", trigger.query)
            }

            is RobotTrigger.ListingViewedTwice -> {
                val raw = pickTemplate("viewed_twice", RobotTemplates.listingViewedTwice)
                raw.replace("{title}", trigger.title)
            }

            is RobotTrigger.RarityHit -> {
                val pool = RobotTemplates.rarityHit[trigger.tier] ?: RobotTemplates.rarityHit["STANDARD"]!!
                val raw = pickTemplate("rarity_${trigger.tier}", pool)
                raw.replace("{query}", trigger.query)
            }

            is RobotTrigger.SessionMilestone -> {
                pickTemplate("session_milestone", RobotTemplates.sessionMilestone)
            }

            is RobotTrigger.HypeMoment -> {
                pickTemplate("hype_moment", RobotTemplates.hypeReactions)
            }
        }.let { text ->
            if (detectBouncePattern() && trigger is RobotTrigger.ScreenEnter && Random.nextInt(4) == 0) {
                pickTemplate("bounce_pattern", RobotTemplates.bouncePattern)
            } else text
        }
    }
}

// ─── Talking Robot ────────────────────────────────────────────────────────────

@Composable
fun TalkingRobot(
    message: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    robotSpriteResId: Int? = null,
    habboUsername: String = "",
    habboRegion: String = "habbo.com",
    showHabboAvatar: Boolean = true,
    accentColor: Color = CGreen,
    stance: RobotStance = RobotStance.STANDING,
    autoDismissMs: Long = 5500L,
    onDismiss: () -> Unit = {}
) {
    var displayedText by remember(message) { mutableStateOf("") }
    var typingDone by remember(message) { mutableStateOf(false) }

    LaunchedEffect(message, isVisible) {
        if (!isVisible || message.isBlank()) return@LaunchedEffect
        displayedText = ""
        typingDone = false
        message.forEachIndexed { index, _ ->
            displayedText = message.substring(0, index + 1)
            delay(18L)
        }
        typingDone = true
    }

    LaunchedEffect(message, isVisible, typingDone) {
        if (!isVisible || !typingDone) return@LaunchedEffect
        delay(autoDismissMs)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it / 2 },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(animationSpec = tween(400)),
        exit = slideOutHorizontally(
            targetOffsetX = { it / 2 },
            animationSpec = tween(350)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                showHabboAvatar && habboUsername.isNotBlank() -> {
                    HabboAvatarBubble(habboUsername = habboUsername, habboRegion = habboRegion)
                }
                robotSpriteResId != null -> {
                    IdleBobWrapper {
                        Image(
                            painter = painterResource(id = robotSpriteResId),
                            contentDescription = "Talking Robot",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                else -> {
                    DefaultRobotIcon(accentColor = accentColor, stance = stance)
                }
            }

            SpeechBubble(
                message = displayedText,
                accentColor = accentColor,
                onTap = {
                    if (!typingDone) {
                        displayedText = message
                        typingDone = true
                    } else {
                        onDismiss()
                    }
                }
            )
        }
    }
}

// ─── Idle Bob Wrapper ───────────────────────────────────────────────────────────

@Composable
fun IdleBobWrapper(content: @Composable () -> Unit) {
    val offsetY by rememberGlowPhase(-4f)
    Box(modifier = Modifier.offset(y = offsetY.dp)) { content() }
}

// ─── Habbo Avatar Bubble ──────────────────────────────────────────────────────

@Composable
fun HabboAvatarBubble(
    habboUsername: String,
    habboRegion: String = "habbo.com"
) {
    val context = LocalContext.current
    Box(contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .offset(y = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CGreen.copy(alpha = 0.25f),
                            CGreen.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .blur(8.dp)
        )
        IdleBobWrapper {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(habboAvatarUrl(habboUsername, habboRegion))
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = "Habbo Avatar",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(10.dp)),
                error = rememberVectorPainter(image = Icons.Filled.SmartToy),
                placeholder = rememberVectorPainter(image = Icons.Filled.SmartToy)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(14.dp)
                .clip(CircleShape)
                .background(CGreen.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "H",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─── Default Robot Icon ───────────────────────────────────────────────────────

// ─── Robot Stance ───────────────────────────────────────────────────────────

enum class RobotStance { STANDING, SITTING, SALUTING }

// ─── Default Robot Icon — stance-aware ─────────────────────────────────────

@Composable
fun DefaultRobotIcon(accentColor: Color = CGreen, stance: RobotStance = RobotStance.STANDING) {
    val blinkT = rememberInfiniteTransition(label = "robotBlink")
    val blinkScale by blinkT.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                1f at 0
                1f at 2800
                0.15f at 2900
                1f at 3000
            },
            RepeatMode.Restart
        ),
        label = "robotBlinkScale"
    )

    val bodyOffsetY by animateFloatAsState(
        targetValue = if (stance == RobotStance.SITTING) 6f else 0f,
        animationSpec = tween(400, easing = EaseInOut),
        label = "stanceOffsetY"
    )
    val bodyScaleY by animateFloatAsState(
        targetValue = if (stance == RobotStance.SITTING) 0.85f else 1f,
        animationSpec = tween(400, easing = EaseInOut),
        label = "stanceScaleY"
    )

    val wrapWithBob: Boolean = stance != RobotStance.SITTING

    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ScrapbookDark)
                .border(2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = bodyOffsetY.dp)
                    .scale(scaleX = 1f, scaleY = bodyScaleY * blinkScale)
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = "Talking Robot",
                    modifier = Modifier.size(52.dp),
                    tint = accentColor
                )
            }

            // Saluting hand accent — small icon overlay top-right
            if (stance == RobotStance.SALUTING) {
                Icon(
                    imageVector = Icons.Filled.PanTool,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 14.dp)
                        .scale(scaleX = -1f, scaleY = 1f)
                )
            }
        }
    }

    if (wrapWithBob) {
        IdleBobWrapper { content() }
    } else {
        content()
    }
}

// ─── Speech Bubble — true Habbo style ──────────────────────────────────────────

@Composable
fun SpeechBubble(message: String, accentColor: Color = CGreen, onTap: () -> Unit = {}) {
    val blinkT = rememberInfiniteTransition(label = "caretBlink")
    val caretAlpha by blinkT.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "caretAlpha"
    )

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .widthIn(max = 230.dp)
            .padding(start = 6.dp)
    ) {
        // Refined tail — pointing left toward the avatar, matches bubble fill + border
        Canvas(
            modifier = Modifier
                .size(width = 11.dp, height = 18.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-9).dp, y = (-6).dp)
        ) {
            val path = Path().apply {
                moveTo(size.width, 2f)
                lineTo(0f, size.height / 2)
                lineTo(size.width, size.height - 2f)
                close()
            }
            drawPath(path = path, color = ScrapbookDark)
            drawPath(
                path = path,
                color = accentColor.copy(alpha = 0.4f),
                style = Stroke(width = 2.2f)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(ScrapbookDark)
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onTap() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "RETRO BOT",
                    fontFamily = BangersFontFamily,
                    color = accentColor,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = message,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "▌",
                        fontFamily = NunitoFontFamily,
                        color = accentColor.copy(alpha = caretAlpha),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─── Habbo Avatar Card ────────────────────────────────────────────────────────

@Composable
fun HabboAvatarCard(
    habboUsername: String,
    habboRegion: String = "habbo.com",
    modifier: Modifier = Modifier,
    size: Int = 100
) {
    if (habboUsername.isBlank()) return
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ScrapbookDark)
            .border(2.dp, CGreen, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(habboAvatarUrl(habboUsername, habboRegion))
                .crossfade(true)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
            contentDescription = "Habbo Avatar — $habboUsername",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            error = rememberVectorPainter(image = Icons.Filled.SmartToy),
            fallback = rememberVectorPainter(image = Icons.Filled.SmartToy)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScrapbookDark.copy(alpha = 0.85f))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = habboUsername,
                fontFamily = BangersFontFamily,
                color = CGreen,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}
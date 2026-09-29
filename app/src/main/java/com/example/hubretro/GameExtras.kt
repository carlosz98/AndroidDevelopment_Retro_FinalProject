package com.example.hubretro

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// APP-WIDE DEEP LINKS (so any screen can jump to another page)
// ═══════════════════════════════════════════════════════════════════════════════

/** MainActivity observes this and switches to the requested content label (e.g. "ALBUMS"). */
object AppNavBus {
    var request by mutableStateOf<String?>(null)
    fun go(label: String) { request = label }
}

/** MarketplaceScreen's Find-It tab consumes this and runs the search automatically. */
object MarketplaceSearchBus {
    var pendingQuery by mutableStateOf<String?>(null)
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. CONSOLE-STYLED CARTRIDGES / CASES
// ═══════════════════════════════════════════════════════════════════════════════

enum class GameMedia(val tag: String) {
    SNES("16-BIT"), N64("64-BIT"), NES("8-BIT"), GENESIS("16-BIT"),
    GAMEBOY("HANDHELD"), DISC("CD-ROM"), ARCADE("ARCADE"), DEFAULT("")
}

fun gameMediaFor(platforms: List<String>): GameMedia {
    val p = platforms.joinToString("|").lowercase()
    return when {
        "super nintendo" in p || "super famicom" in p || "snes" in p -> GameMedia.SNES
        "nintendo 64" in p -> GameMedia.N64
        "game boy" in p -> GameMedia.GAMEBOY
        "nintendo entertainment system" in p || "family computer" in p || "famicom" in p -> GameMedia.NES
        "genesis" in p || "mega drive" in p -> GameMedia.GENESIS
        "playstation" in p || "saturn" in p || "dreamcast" in p || "sega cd" in p ||
            "gamecube" in p || "pc engine cd" in p -> GameMedia.DISC
        "arcade" in p -> GameMedia.ARCADE
        else -> GameMedia.DEFAULT
    }
}

/**
 * Wraps a cover image in a shell that looks like the game's physical media:
 * SNES/NES/N64/Genesis/Game Boy cartridges, a CD jewel case, or an arcade board.
 */
@Composable
fun CartridgeFrame(media: GameMedia, modifier: Modifier = Modifier, label: @Composable BoxScope.() -> Unit) {
    if (media == GameMedia.DEFAULT) {
        Box(modifier.clip(RoundedCornerShape(10.dp)).background(ComicGlassBg),
            contentAlignment = Alignment.Center, content = label)
        return
    }
    val body: Color
    val tagColor: Color
    val shape: RoundedCornerShape
    val pad: PaddingValues
    when (media) {
        GameMedia.SNES -> { body = Color(0xFFBDBDBD); tagColor = ScrapbookDark
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
            pad = PaddingValues(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 13.dp) }
        GameMedia.N64 -> { body = Color(0xFF4A4A4A); tagColor = Color.White
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            pad = PaddingValues(start = 7.dp, end = 7.dp, top = 10.dp, bottom = 13.dp) }
        GameMedia.NES -> { body = Color(0xFF9E9E9E); tagColor = ScrapbookDark
            shape = RoundedCornerShape(4.dp)
            pad = PaddingValues(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 13.dp) }
        GameMedia.GENESIS -> { body = Color(0xFF1E1E1E); tagColor = Color.White
            shape = RoundedCornerShape(6.dp)
            pad = PaddingValues(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 13.dp) }
        GameMedia.GAMEBOY -> { body = Color(0xFFD2D2D2); tagColor = ScrapbookDark
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            pad = PaddingValues(start = 7.dp, end = 7.dp, top = 9.dp, bottom = 13.dp) }
        GameMedia.DISC -> { body = Color(0xFFF4F4F4); tagColor = ScrapbookDark
            shape = RoundedCornerShape(4.dp)
            pad = PaddingValues(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp) }
        else -> { body = Color(0xFF111111); tagColor = Color.White // ARCADE
            shape = RoundedCornerShape(4.dp)
            pad = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 13.dp) }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(body)
            .border(2.dp, ScrapbookDark, shape)
            .drawWithContent {
                val w = size.width
                val h = size.height
                val line = 1.2.dp.toPx()
                val dark = ScrapbookDark.copy(alpha = 0.35f)
                when (media) {
                    GameMedia.SNES, GameMedia.NES, GameMedia.GENESIS -> {
                        // grip ridges along the bottom
                        val count = 7
                        val gap = w / (count + 1)
                        for (i in 1..count) {
                            drawLine(if (media == GameMedia.GENESIS) Color.White.copy(alpha = 0.25f) else dark,
                                Offset(gap * i, h - 11.dp.toPx()), Offset(gap * i, h - 3.dp.toPx()), strokeWidth = line)
                        }
                    }
                    GameMedia.N64 -> {
                        // angled grooves on the top shoulders
                        for (i in 0..2) {
                            val o = (3 + i * 3).dp.toPx()
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(o, 0f), Offset(0f, o + 4.dp.toPx()), strokeWidth = line)
                            drawLine(Color.White.copy(alpha = 0.25f), Offset(w - o, 0f), Offset(w, o + 4.dp.toPx()), strokeWidth = line)
                        }
                    }
                    GameMedia.GAMEBOY -> {
                        // little arrow at the bottom like the real carts
                        val cx = w / 2f
                        val tri = Path().apply {
                            moveTo(cx - 4.dp.toPx(), h - 9.dp.toPx())
                            lineTo(cx + 4.dp.toPx(), h - 9.dp.toPx())
                            lineTo(cx, h - 4.dp.toPx())
                            close()
                        }
                        drawPath(tri, dark)
                    }
                    GameMedia.DISC -> {
                        // hinge spine on the left
                        drawRect(Color(0xFF2B2B2B), topLeft = Offset(0f, 0f), size = Size(8.dp.toPx(), h))
                        for (y in listOf(0.2f, 0.5f, 0.8f)) {
                            drawRect(Color(0xFF777777), topLeft = Offset(2.dp.toPx(), h * y - 4.dp.toPx()),
                                size = Size(4.dp.toPx(), 8.dp.toPx()))
                        }
                    }
                    GameMedia.ARCADE -> {
                        drawRect(CAcRed, topLeft = Offset(0f, h - 11.dp.toPx()), size = Size(w, 11.dp.toPx()))
                    }
                    else -> {}
                }
                drawContent()
                if (media == GameMedia.DISC) {
                    // plastic case glare over the art
                    drawLine(Color.White.copy(alpha = 0.45f), Offset(w * 0.55f, 0f), Offset(w * 0.15f, h), strokeWidth = 6.dp.toPx())
                }
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(pad)
                .clip(RoundedCornerShape(3.dp)).background(ComicGlassBg)
                .border(1.5.dp, ScrapbookDark, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
            content = label
        )
        Text(
            media.tag,
            style = TextStyle(fontFamily = BangersFontFamily, fontSize = 7.sp, letterSpacing = 0.5.sp, color = tagColor),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. COLLECTION TRACKER (Firestore: users/{uid}/game_collection/{gameId})
// ═══════════════════════════════════════════════════════════════════════════════

enum class CollectionStatus(val label: String, val emoji: String, val color: Color) {
    OWNED("OWNED", "📦", CAcBlue),
    PLAYING("PLAYING", "🕹️", CGreenDeep),
    BEATEN("BEATEN", "🏆", CAcYellow),
    WISHLIST("WISHLIST", "⭐", CAcPurple)
}

data class CollectionEntry(
    val gameId: Int,
    val name: String,
    val coverUrl: String?,
    val status: CollectionStatus,
    val updatedAt: Long
)

object GameCollection {
    val entries = mutableStateMapOf<Int, CollectionEntry>()
    private var listener: ListenerRegistration? = null
    private var listeningUid: String? = null

    private fun ref(uid: String) = FirebaseFirestore.getInstance()
        .collection("users").document(uid).collection("game_collection")

    fun ensureListening() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (listeningUid == uid) return
        listener?.remove()
        entries.clear()
        listeningUid = uid
        listener = ref(uid).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            val fresh = snap.documents.mapNotNull { d ->
                val id = (d.getLong("gameId") ?: return@mapNotNull null).toInt()
                val status = runCatching { CollectionStatus.valueOf(d.getString("status") ?: "") }.getOrNull()
                    ?: return@mapNotNull null
                CollectionEntry(id, d.getString("name") ?: "", d.getString("coverUrl"), status, d.getLong("updatedAt") ?: 0L)
            }
            entries.clear()
            fresh.forEach { entries[it.gameId] = it }
        }
    }

    /** status = null removes the game from the collection. */
    fun setStatus(game: IGDBGame, status: CollectionStatus?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val doc = ref(uid).document(game.id.toString())
        if (status == null) {
            entries.remove(game.id)
            doc.delete()
        } else {
            val now = System.currentTimeMillis()
            entries[game.id] = CollectionEntry(game.id, game.name, game.coverUrl, status, now)
            doc.set(
                mapOf(
                    "gameId" to game.id,
                    "name" to game.name,
                    "coverUrl" to (game.coverUrl ?: ""),
                    "status" to status.name,
                    "updatedAt" to now
                )
            )
        }
    }
}

/** Small rubber-stamp badge shown on game cards that are in your collection. */
@Composable
fun CollectionStampBadge(status: CollectionStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.graphicsLayer { rotationZ = -10f }
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.5.dp, ScrapbookDark, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text("${status.emoji} ${status.label}", style = TextStyle(fontFamily = BangersFontFamily,
            fontSize = 8.sp, letterSpacing = 0.5.sp, color = ScrapbookDark))
    }
}

@Composable
fun CollectionPicker(game: IGDBGame) {
    val context = LocalContext.current
    val achievementsViewModel: AchievementsViewModel = viewModel()
    LaunchedEffect(Unit) { GameCollection.ensureListening() }
    val current = GameCollection.entries[game.id]?.status
    val burst = rememberBurstState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
            .clip(RoundedCornerShape(16.dp)).background(current?.color ?: CGreen))
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📚", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("MY COLLECTION", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark))
                    Text(
                        if (current == null) "Not on your shelf yet — tap a stamp" else "Tap ${current.label} again to remove",
                        style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted)
                    )
                }
                if (current != null) CollectionStampBadge(current)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CollectionStatus.values().forEach { status ->
                    val selected = current == status
                    val pop by animateFloatAsState(if (selected) 1.06f else 1f,
                        spring(dampingRatio = 0.4f, stiffness = 500f), label = "stampPop_${status.name}")
                    Column(
                        modifier = Modifier.weight(1f)
                            .graphicsLayer { scaleX = pop; scaleY = pop; rotationZ = if (selected) -4f else 0f }
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) status.color else Color.White.copy(alpha = 0.6f))
                            .border(if (selected) 2.5.dp else 1.5.dp,
                                if (selected) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable {
                                if (selected) {
                                    GameCollection.setStatus(game, null)
                                } else {
                                    GameCollection.setStatus(game, status)
                                    burst.fire("${status.label}!", status.color)
                                    Chiptune.play(Chiptune.Sfx.STAMP)
                                    if (status == CollectionStatus.BEATEN) {
                                        // XP only the first time each game is beaten (no toggling for XP)
                                        val prefs = context.getSharedPreferences("retrohub_collection", Context.MODE_PRIVATE)
                                        val key = "beaten_xp_${FirebaseAuth.getInstance().currentUser?.uid}"
                                        val done = prefs.getStringSet(key, emptySet())!!.toMutableSet()
                                        if (done.add(game.id.toString())) {
                                            prefs.edit().putStringSet(key, done).apply()
                                            achievementsViewModel.awardXP(25, "BEATEN")
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(status.emoji, fontSize = 18.sp)
                        Text(status.label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp,
                            color = ScrapbookDark))
                    }
                }
            }
        }
        ComicBurst(burst, Modifier.align(Alignment.Center), burstSize = 120.dp)
    }
}

/** Profile card: collection counts, completion %, and recent shelf. */
@Composable
fun CollectionStatsCard() {
    LaunchedEffect(Unit) { GameCollection.ensureListening() }
    val all = GameCollection.entries.values.sortedByDescending { it.updatedAt }
    val countOf = { s: CollectionStatus -> all.count { it.status == s } }
    val owned = countOf(CollectionStatus.OWNED)
    val playing = countOf(CollectionStatus.PLAYING)
    val beaten = countOf(CollectionStatus.BEATEN)
    val wish = countOf(CollectionStatus.WISHLIST)
    val library = owned + playing + beaten
    val completion = if (library > 0) beaten * 100 / library else 0
    val fill by animateFloatAsState(completion / 100f, tween(1200, easing = FastOutSlowInEasing), label = "collectionFill")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(CollectionStatus.OWNED to owned, CollectionStatus.PLAYING to playing,
                CollectionStatus.BEATEN to beaten, CollectionStatus.WISHLIST to wish).forEach { (s, n) ->
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                        .background(s.color.copy(alpha = 0.12f))
                        .border(2.dp, s.color, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.emoji, fontSize = 16.sp)
                    RollingCounterText("$n", TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, color = ScrapbookDark))
                    Text(s.label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 9.sp, color = ScrapbookDark.copy(alpha = 0.7f)))
                }
            }
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("COMPLETION", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, color = ScrapbookDark),
                    modifier = Modifier.weight(1f))
                RollingCounterText("$completion%", TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, color = CGreenDeep))
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                .background(Color.White).border(2.dp, ScrapbookDark, RoundedCornerShape(6.dp))) {
                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(Brush.horizontalGradient(listOf(CGreen, CAcYellow))))
            }
        }
        if (all.isEmpty()) {
            Text("Mark games as Owned, Playing, Beaten or Wishlist from any game page in the Game Database.",
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookTextMuted, lineHeight = 16.sp))
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(all.take(12), key = { it.gameId }) { e ->
                    Box(modifier = Modifier.width(56.dp).height(74.dp)) {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(ComicGlassBg)
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            if (!e.coverUrl.isNullOrBlank()) AsyncImage(model = e.coverUrl, contentDescription = e.name,
                                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(e.coverUrl).fillMaxSize().clip(RoundedCornerShape(6.dp)))
                            else Text("🎮", fontSize = 20.sp)
                        }
                        Text(e.status.emoji, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. CONNECTIONS — link a game to Albums, Magazines, Marketplace, Streams, players
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun GameConnectionsSection(game: IGDBGame) {
    val authViewModel: AuthViewModel = viewModel()
    val allUsers by authViewModel.allUsers.collectAsState()
    LaunchedEffect(Unit) { if (allUsers.isEmpty()) authViewModel.fetchAllUsers() }
    val fans = remember(allUsers, game.name) {
        allUsers.filter { u ->
            u.topGames.any { (it["name"] as? String)?.equals(game.name, ignoreCase = true) == true }
        }
    }
    val shortPlatform = when (gameMediaFor(game.platforms)) {
        GameMedia.SNES -> "SNES"; GameMedia.N64 -> "N64"; GameMedia.NES -> "NES"; GameMedia.GENESIS -> "Genesis"
        GameMedia.GAMEBOY -> "Game Boy"; GameMedia.DISC -> game.platforms.firstOrNull() ?: ""; else -> ""
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔗", fontSize = 18.sp)
            Text("CONNECTIONS", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, color = ScrapbookDark))
        }
        val links = listOf(
            Triple("🎵", "SOUNDTRACKS", CGreen) to { AppNavBus.go("ALBUMS") },
            Triple("📰", "IN MAGAZINES", CGreenMint) to { AppNavBus.go("MAGAZINES") },
            Triple("🛒", "FIND IT FOR SALE", CGreenDeep) to {
                MarketplaceSearchBus.pendingQuery = "${game.name} $shortPlatform".trim()
                AppNavBus.go("MARKETPLACE")
            },
            Triple("📺", "WATCH STREAMS", CGreen) to { AppNavBus.go("STREAMS") }
        )
        links.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (info, action) ->
                    val (emoji, label, color) = info
                    ConnectionTile(emoji, label, color, Modifier.weight(1f), action)
                }
            }
        }
        // Players who have it on their shelf
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.6f))
                .border(2.dp, ScrapbookDark, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fans.isNotEmpty()) {
                Box(Modifier.width((28 + (fans.take(4).size - 1) * 18).dp).height(28.dp)) {
                    fans.take(4).forEachIndexed { i, u ->
                        Box(
                            modifier = Modifier.offset(x = (i * 18).dp).size(28.dp).clip(CircleShape)
                                .background(CGreenMint).border(2.dp, ScrapbookDark, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (u.profilePictureUrl.isNotBlank()) AsyncImage(model = u.profilePictureUrl, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(u.profilePictureUrl).fillMaxSize().clip(CircleShape))
                            else Text(u.username.take(1).uppercase(), style = TextStyle(fontFamily = BangersFontFamily,
                                fontSize = 12.sp, color = ScrapbookDark))
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
            } else {
                Text("👥", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                when (fans.size) {
                    0 -> "No players have this in their Top Games yet — be the first!"
                    1 -> "${fans.first().username} has this in their Top Games"
                    else -> "${fans.size} players have this in their Top Games"
                },
                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookDark,
                    fontWeight = FontWeight.Bold, lineHeight = 16.sp),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConnectionTile(emoji: String, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val tap = rememberTapHaptic()
    var pressed by remember { mutableStateOf(false) }
    val press by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "connPress")
    Box(modifier = modifier) {
        Box(Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).clip(RoundedCornerShape(12.dp)).background(color))
        Row(
            modifier = Modifier.fillMaxWidth().offset(x = press.dp, y = press.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                .clickable { pressed = true; tap(); Chiptune.play(Chiptune.Sfx.POP); onClick() }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, color = ScrapbookDark),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("→", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, color = color))
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(140); pressed = false } }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. HIGHER OR LOWER — guess which game has the higher IGDB score
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun HigherLowerEntryCard(onPlay: () -> Unit) {
    val bob by rememberGlowRange(-3f, 3f)
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp).clip(RoundedCornerShape(16.dp)).background(CAcPurple))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(CAcYellowL, Color.White)))
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                .clickable { Chiptune.play(Chiptune.Sfx.COIN); onPlay() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("▲▼", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 30.sp, color = CAcPurple),
                modifier = Modifier.graphicsLayer { translationY = bob })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("HIGHER OR LOWER?", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, color = ScrapbookDark))
                Text("Guess which game scored higher. Build a streak, earn XP!",
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookDark.copy(alpha = 0.7f)))
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("▶ PLAY", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 14.sp, color = CAcYellowL)) }
        }
    }
}

@Composable
fun HigherOrLowerGame(pool: List<IGDBGame>, onClose: () -> Unit, onOpenGame: (IGDBGame) -> Unit) {
    val context = LocalContext.current
    val achievementsViewModel: AchievementsViewModel = viewModel()
    val prefs = remember { context.getSharedPreferences("retrohub_higher_lower", Context.MODE_PRIVATE) }
    val games = remember(pool) { pool.filter { it.rating != null }.distinctBy { it.id } }
    val scope = rememberCoroutineScope()
    val burst = rememberBurstState()
    val shake = remember { Animatable(0f) }

    fun pick(exclude: Int?): IGDBGame? = games.filter { it.id != exclude }.randomOrNull()

    var left by remember { mutableStateOf(pick(null)) }
    var right by remember { mutableStateOf(pick(left?.id)) }
    var streak by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var revealed by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var xpEarned by remember { mutableIntStateOf(0) }

    fun guess(higher: Boolean) {
        val a = left ?: return
        val b = right ?: return
        if (revealed || gameOver) return
        revealed = true
        val ra = a.rating ?: 0.0
        val rb = b.rating ?: 0.0
        val correct = if (higher) rb >= ra else rb <= ra
        scope.launch {
            delay(700)
            if (correct) {
                streak++
                burst.fire(if (streak % 5 == 0) "COMBO x$streak!" else "CORRECT!", CGreen)
                Chiptune.play(Chiptune.Sfx.COIN)
                if (streak == 3) DailyQuests.report(context, DailyQuests.HIGHER_LOWER, achievementsViewModel)
                delay(700)
                left = b
                right = pick(b.id)
                revealed = false
            } else {
                Chiptune.play(Chiptune.Sfx.STAMP)
                repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 14f else -14f, tween(45)) }
                shake.animateTo(0f, tween(45))
                if (streak > best) { best = streak; prefs.edit().putInt("best", best).apply() }
                xpEarned = (streak * 5).coerceAtMost(50)
                if (xpEarned > 0) achievementsViewModel.awardXP(xpEarned, "MINIGAME")
                gameOver = true
            }
        }
    }

    fun restart() {
        left = pick(null); right = pick(left?.id)
        streak = 0; revealed = false; gameOver = false; xpEarned = 0
    }

    Box(modifier = Modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            ComicPageHeader(title = "HIGHER OR LOWER", subtitle = "Best streak: $best", onBack = onClose) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    RollingCounterText("🔥$streak", TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = CAcYellowL))
                }
            }
            if (games.size < 2 || left == null || right == null) {
                NoSaveDataState(title = "NOT ENOUGH GAMES", subtitle = "Browse a platform first so there are games to compare.",
                    actionText = "BACK", onAction = onClose)
                return@Column
            }
            val a = left!!
            val b = right!!
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).graphicsLayer { translationX = shake.value },
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HigherLowerCard(a, showScore = true, modifier = Modifier.weight(1f)) { onOpenGame(a) }
                // VS stamp
                Box(
                    modifier = Modifier.graphicsLayer { rotationZ = -6f }.clip(RoundedCornerShape(8.dp))
                        .background(CAcRed).border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) { Text("VS", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp, color = Color.White)) }
                HigherLowerCard(b, showScore = revealed, modifier = Modifier.weight(1f)) { onOpenGame(b) }
                if (!gameOver) {
                    Text("Does ${b.name} score HIGHER or LOWER?", style = TextStyle(fontFamily = NunitoFontFamily,
                        fontSize = 13.sp, color = ScrapbookDark, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        RetroGlassButton(text = "▲ HIGHER", onClick = { guess(true) }, modifier = Modifier.weight(1f))
                        RetroGlassButton(text = "▼ LOWER", onClick = { guess(false) }, modifier = Modifier.weight(1f))
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ScrapbookDark)
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(14.dp)).padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GAME OVER", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 30.sp, color = CAcRed))
                            Text("Streak: $streak  ·  Best: $best" + if (xpEarned > 0) "  ·  +$xpEarned XP" else "",
                                style = TextStyle(fontFamily = BangersFontFamily, fontSize = 15.sp, color = Color.White))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        RetroGlassButton(text = "↺ PLAY AGAIN", onClick = { restart() }, modifier = Modifier.weight(1f))
                        RetroGlassButton(text = "EXIT", onClick = onClose, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        ComicBurst(burst, Modifier.align(Alignment.Center), burstSize = 150.dp)
    }
}

@Composable
private fun HigherLowerCard(game: IGDBGame, showScore: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val media = gameMediaFor(game.platforms)
    Box(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp).clip(RoundedCornerShape(16.dp)).background(CGreen))
        Row(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(Color.White)
                .border(2.5.dp, ScrapbookDark, RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CartridgeFrame(media, Modifier.fillMaxHeight().aspectRatio(0.8f)) {
                if (game.coverUrl != null) AsyncImage(model = game.coverUrl, contentDescription = game.name,
                    contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(game.coverUrl).fillMaxSize())
                else Text("🎮", fontSize = 28.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(game.name, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 20.sp, color = ScrapbookDark,
                    lineHeight = 22.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                game.releaseYear?.let {
                    Text("$it", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 12.sp, color = ScrapbookTextMuted))
                }
                Spacer(Modifier.height(6.dp))
                if (showScore) {
                    RollingCounterText("${(game.rating ?: 0.0).toInt()}",
                        TextStyle(fontFamily = BangersFontFamily, fontSize = 40.sp, color = ratingColor(game.rating ?: 0.0)))
                } else {
                    Text("??", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 40.sp, color = ScrapbookDark.copy(alpha = 0.3f)))
                }
                Text("IGDB SCORE", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, color = ScrapbookTextMuted))
            }
        }
    }
}

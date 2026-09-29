package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════════════════
// SAVE ROOM — a cozy pixel room on the profile where you seat your 4 closest friends.
// Firestore: users/{uid}.saveRoomSlots = [uid, "", uid, ""]  +  users/{uid}.saveRoomTheme = "cabin"
//
// ARTWORK: drop PNGs into app/src/main/res/drawable/ named
//   room_cabin.png, room_loft.png, room_beach.png, room_cyber.png, room_treehouse.png
// They're looked up by name at runtime, so the app compiles without them and
// falls back to a code-drawn pixel room until you add the art.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Where a friend stands/sits in a room image.
 * x, y = fraction of the image width/height where the avatar's FEET (standing) or SEAT (sitting) go.
 * direction = Habbo facing 0–7 (2 = front-right, 4 = front-left; tweak freely if someone faces the wrong way).
 */
data class SeatSpot(val x: Float, val y: Float, val sitting: Boolean, val direction: Int)

enum class RoomTheme(
    val id: String,
    val label: String,
    val emoji: String,
    val unlockLevel: Int,
    val drawableRes: Int,
    val wall: Color,
    val floor: Color,
    val skyTop: Color,
    val skyBottom: Color,
    val flame: Color,
    val seats: List<SeatSpot>
) {
    // 🕹️ Arcade workshop — egg chair + standing spots around the arcades and rug
    ARCADE("arcade", "ARCADE WORKSHOP", "🕹️", 1, R.drawable.room_arcade,
        Color(0xFF5A3A26), Color(0xFF4A2F1B), Color(0xFF3B2D5C), Color(0xFFE9895A), Color(0xFFFF9A3C),
        listOf(
            SeatSpot(0.655f, 0.700f, sitting = true,  direction = 4),   // egg chair
            SeatSpot(0.560f, 0.520f, sitting = false, direction = 4),   // by the arcade cabinets
            SeatSpot(0.405f, 0.580f, sitting = false, direction = 2),   // left edge of the rug
            SeatSpot(0.300f, 0.690f, sitting = false, direction = 2)    // in front of the workbench
        )),
    // 🌆 NYC loft — two on the sofa, one in the egg chair, one standing by the rug
    LOFT("loft", "NYC LOFT", "🌆", 2, R.drawable.room_loft,
        Color(0xFF4E3526), Color(0xFF3E2716), Color(0xFF3B2D5C), Color(0xFFE9895A), Color(0xFFFF9A3C),
        listOf(
            SeatSpot(0.255f, 0.455f, sitting = true,  direction = 2),   // sofa left
            SeatSpot(0.330f, 0.405f, sitting = true,  direction = 2),   // sofa right
            SeatSpot(0.655f, 0.700f, sitting = true,  direction = 4),   // egg chair
            SeatSpot(0.450f, 0.760f, sitting = false, direction = 2)    // front of the rug
        )),
    // 🎮 Gamer den — one in the recliner, three standing around the rug with controllers
    DEN("den", "GAMER DEN", "🎮", 3, R.drawable.room_den,
        Color(0xFF4E3526), Color(0xFF3E2716), Color(0xFF14213D), Color(0xFF2D6A4F), Color(0xFFFF9A3C),
        listOf(
            SeatSpot(0.650f, 0.690f, sitting = true,  direction = 6),   // recliner (facing the TV)
            SeatSpot(0.400f, 0.600f, sitting = false, direction = 2),   // left of the rug
            SeatSpot(0.520f, 0.770f, sitting = false, direction = 2),   // front of the rug
            SeatSpot(0.765f, 0.540f, sitting = false, direction = 4)    // by the record shelf
        ));

    companion object {
        fun fromId(id: String?): RoomTheme = values().firstOrNull { it.id == id } ?: ARCADE
    }
}

/** Habbo avatar with a specific pose + facing (used inside illustrated rooms). */
fun habboPoseUrl(username: String, domain: String, sitting: Boolean, direction: Int) =
    "https://www.$domain/habbo-imaging/avatarimage?user=${username.trim()}" +
        "&action=${if (sitting) "sit" else "std"}&direction=$direction&head_direction=$direction&size=l&gesture=sml"

private const val SLOT_COUNT = 4

@Composable
fun SaveRoomSection(
    ownerUid: String,
    isOwner: Boolean,
    authViewModel: AuthViewModel = viewModel(),
    achievementsViewModel: AchievementsViewModel = viewModel()
) {
    if (ownerUid.isBlank()) return
    val context = LocalContext.current
    val allUsers by authViewModel.allUsers.collectAsState()
    val followingUids by authViewModel.followingUids.collectAsState()
    val achievements by achievementsViewModel.state.collectAsState()
    val myLevel = getRetroLevel(achievements.xp).level

    var slots by remember(ownerUid) { mutableStateOf(List(SLOT_COUNT) { "" }) }
    var theme by remember(ownerUid) { mutableStateOf(RoomTheme.ARCADE) }
    var pickerSlot by remember { mutableStateOf<Int?>(null) }
    var showThemes by remember { mutableStateOf(false) }
    var viewingUser by remember { mutableStateOf<UserProfileData?>(null) }
    val burst = rememberBurstState()

    // Load (live for the owner, so changes show instantly)
    DisposableEffect(ownerUid) {
        val reg = FirebaseFirestore.getInstance().collection("users").document(ownerUid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val raw = (snap.get("saveRoomSlots") as? List<*>)?.map { it as? String ?: "" } ?: emptyList()
                slots = List(SLOT_COUNT) { i -> raw.getOrNull(i) ?: "" }
                theme = RoomTheme.fromId(snap.getString("saveRoomTheme"))
            }
        onDispose { reg.remove() }
    }
    LaunchedEffect(Unit) { if (allUsers.isEmpty()) authViewModel.fetchAllUsers() }

    fun save(newSlots: List<String>, newTheme: RoomTheme = theme) {
        slots = newSlots
        theme = newTheme
        FirebaseFirestore.getInstance().collection("users").document(ownerUid)
            .set(mapOf("saveRoomSlots" to newSlots, "saveRoomTheme" to newTheme.id), SetOptions.merge())
    }

    val usersById = remember(allUsers) { allUsers.associateBy { it.uid } }
    val seated = slots.map { usersById[it] }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.matchParentSize().offset(x = 5.dp, y = 5.dp).clip(RoundedCornerShape(18.dp)).background(CGreen))
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(1289f / 816f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(3.dp, ScrapbookDark, RoundedCornerShape(18.dp))
            ) {
                // ── Room background: artwork if present, otherwise code-drawn pixel room ──
                // Room artwork (res/drawable/room_arcade.jpg, room_loft.jpg, room_den.jpg)
                val resId = theme.drawableRes
                if (resId != 0) {
                    Image(painter = painterResource(id = resId), contentDescription = theme.label,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    PixelRoomFallback(theme = theme, modifier = Modifier.fillMaxSize())
                }
                RoomAmbience(theme = theme, modifier = Modifier.fillMaxSize())

                // ── HUD (top-left) ──
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(8.dp)).background(ScrapbookDark.copy(alpha = 0.78f))
                        .border(1.5.dp, CGreen, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("💾", fontSize = 12.sp)
                    Column {
                        Text("SAVE ROOM", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, color = CGreenMint, letterSpacing = 1.sp))
                        Text("${theme.emoji} ${theme.label}", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 8.sp, color = Color.White))
                    }
                }
                if (isOwner) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .clip(RoundedCornerShape(8.dp)).background(ScrapbookDark.copy(alpha = 0.78f))
                            .border(1.5.dp, CGreen, RoundedCornerShape(8.dp))
                            .clickable { showThemes = !showThemes; Chiptune.play(Chiptune.Sfx.BLIP) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text("🎨 ROOM", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 11.sp, color = CGreenMint))
                    }
                }

                // ── The 4 seats ──
                if (resId != 0) {
                    RoomSeatsOnArtwork(
                        theme = theme,
                        seated = seated,
                        isOwner = isOwner,
                        onTapEmpty = { i -> if (isOwner) pickerSlot = i },
                        onTapUser = { viewingUser = it },
                        onLongPress = { i -> if (isOwner) pickerSlot = i }
                    )
                } else Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (i in 0 until SLOT_COUNT) {
                        SeatSlot(
                            index = i,
                            user = seated[i],
                            isOwner = isOwner,
                            modifier = Modifier.weight(1f),
                            onTapEmpty = { if (isOwner) pickerSlot = i },
                            onTapUser = { viewingUser = it },
                            onLongPress = { if (isOwner) pickerSlot = i }
                        )
                    }
                }
                ComicBurst(burst, Modifier.align(Alignment.Center), burstSize = 130.dp)
            }
        }

        // ── Theme picker (owner) ──
        if (showThemes && isOwner) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(RoomTheme.values().toList(), key = { it.id }) { t ->
                    val locked = myLevel < t.unlockLevel
                    val selected = t == theme
                    Column(
                        modifier = Modifier.jumpIn(t.ordinal)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CGreen else Color.White)
                            .border(2.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                            .clickable(enabled = !locked) {
                                save(slots, t); showThemes = false
                                Chiptune.play(Chiptune.Sfx.COIN)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (locked) "🔒" else t.emoji, fontSize = 20.sp)
                        Text(t.label, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp,
                            color = if (locked) ScrapbookTextMuted else ScrapbookDark))
                        if (locked) Text("LVL ${t.unlockLevel}", style = TextStyle(fontFamily = BangersFontFamily,
                            fontSize = 9.sp, color = CGreenDeep))
                    }
                }
            }
        }

        Text(
            if (isOwner) "Tap an empty seat to invite a friend · long-press a friend to swap or remove"
            else "${seated.count { it != null }}/4 friends in this Save Room",
            style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
    }

    // ── Friend picker ──
    pickerSlot?.let { slotIndex ->
        val candidates = allUsers.filter { it.uid in followingUids && it.uid !in slots }
        FriendPickerDialog(
            candidates = candidates,
            currentlySeated = seated[slotIndex],
            onDismiss = { pickerSlot = null },
            onPick = { user ->
                val newSlots = slots.toMutableList().also { it[slotIndex] = user.uid }
                save(newSlots)
                pickerSlot = null
                burst.fire("PLAYER ${slotIndex + 2}!", CGreen)
                Chiptune.play(Chiptune.Sfx.COIN)
            },
            onRemove = {
                val newSlots = slots.toMutableList().also { it[slotIndex] = "" }
                save(newSlots)
                pickerSlot = null
            }
        )
    }

    // ── Open a friend's profile ──
    viewingUser?.let { u ->
        Dialog(onDismissRequest = { viewingUser = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(ComicGlassBg)) {
                UserProfileViewScreen(user = u, authViewModel = authViewModel, onBack = { viewingUser = null })
            }
        }
    }
}

// ─── A single seat ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SeatSlot(
    index: Int,
    user: UserProfileData?,
    isOwner: Boolean,
    modifier: Modifier,
    onTapEmpty: () -> Unit,
    onTapUser: (UserProfileData) -> Unit,
    onLongPress: () -> Unit
) {
    val drop = remember(user?.uid) { Animatable(if (user != null) -60f else 0f) }
    LaunchedEffect(user?.uid) { if (user != null) drop.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 380f)) }
    // each friend bobs slightly out of sync
    val bob by rememberGlowRange(if (index % 2 == 0) -1.5f else 1.5f, if (index % 2 == 0) 1.5f else -1.5f)
    val blink by rememberGlowRange(0.4f, 1f)

    Column(
        modifier = modifier.jumpIn(index),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            // Speech bubble with their favourite game
            val fav = (user.topGames.firstOrNull()?.get("name") as? String).orEmpty()
            if (fav.isNotBlank()) {
                Box(
                    modifier = Modifier.padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(6.dp)).background(Color.White)
                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("🎮 $fav", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 7.sp, color = ScrapbookDark),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // Avatar: Habbo sitting pose if they have one, else profile picture
            Box(
                modifier = Modifier.size(48.dp)
                    .graphicsLayer { translationY = (drop.value + bob) * density }
                    .combinedClickable(onClick = { onTapUser(user) }, onLongClick = { if (isOwner) onLongPress() }),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (user.habboUsername.isNotBlank()) {
                    AsyncImage(
                        model = habboAvatarUrl(user.habboUsername, user.habboRegion.ifBlank { "habbo.com" }, action = "sit"),
                        contentDescription = user.username, contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(CGreenMint)
                            .border(2.5.dp, ScrapbookDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profilePictureUrl.isNotBlank()) AsyncImage(model = user.profilePictureUrl, contentDescription = user.username,
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                        else Text(user.username.take(1).uppercase(), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookDark))
                    }
                }
            }
        }
        // Nameplate / empty tile (like the mockup)
        Box(
            modifier = Modifier.fillMaxWidth().height(if (user == null) 46.dp else 24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ScrapbookDark.copy(alpha = 0.82f))
                .border(1.5.dp, if (user == null) CGreenMint.copy(alpha = blink) else CGreen, RoundedCornerShape(6.dp))
                .then(if (user == null) Modifier.clickable { onTapEmpty() } else Modifier.clickable { onTapUser(user) }),
            contentAlignment = Alignment.Center
        ) {
            if (user == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${index + 1}", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 13.sp, color = CGreenMint))
                    Text(if (isOwner) "+ ADD" else "EMPTY", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, color = CGreenMint))
                }
            } else {
                Text(user.username.ifBlank { "P${index + 2}" }, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 10.sp, color = Color.White),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 3.dp))
            }
        }
    }
}

// ─── Friend picker dialog ─────────────────────────────────────────────────────

@Composable
private fun FriendPickerDialog(
    candidates: List<UserProfileData>,
    currentlySeated: UserProfileData?,
    onDismiss: () -> Unit,
    onPick: (UserProfileData) -> Unit,
    onRemove: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = candidates.filter { query.isBlank() || it.username.contains(query, ignoreCase = true) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.comicPop().fillMaxWidth().heightIn(max = 520.dp)
                .clip(RoundedCornerShape(18.dp)).background(Color.White)
                .border(3.dp, ScrapbookDark, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("💾 CHOOSE A PLAYER", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 22.sp, color = ScrapbookDark))
            if (currentlySeated != null) {
                RetroGlassButton(text = "✕ REMOVE ${currentlySeated.username.uppercase()}", onClick = onRemove, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search friends…", style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookDark.copy(alpha = 0.4f))) },
                textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScrapbookDark, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f),
                    cursorColor = ScrapbookDark
                ),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            )
            if (filtered.isEmpty()) {
                Text(
                    if (candidates.isEmpty()) "Follow some players first — only people you follow can join your Save Room."
                    else "No friends match \"$query\".",
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.uid }) { u ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(ComicGlassBg).border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                .clickable { onPick(u) }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(CGreenMint).border(2.dp, ScrapbookDark, CircleShape),
                                contentAlignment = Alignment.Center) {
                                if (u.profilePictureUrl.isNotBlank()) AsyncImage(model = u.profilePictureUrl, contentDescription = null,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                else Text(u.username.take(1).uppercase(), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookDark))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.username, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookDark), maxLines = 1)
                                if (u.userHandle.isNotBlank()) Text(u.userHandle, style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 11.sp, color = ScrapbookTextMuted), maxLines = 1)
                            }
                            Text("SEAT ▶", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 12.sp, color = CGreenDeep))
                        }
                    }
                }
            }
        }
    }
}

// ─── Living ambience over any room (artwork or fallback) ──────────────────────

@Composable
private fun RoomAmbience(theme: RoomTheme, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "roomAmbience")
    val flicker by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "fireFlicker")
    val twinkle by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "twinkle")
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        // Warm fireplace glow (right side of the room)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(theme.flame.copy(alpha = 0.28f * flicker), Color.Transparent),
                center = Offset(size.width * 0.72f, size.height * 0.62f),
                radius = size.width * 0.32f
            ),
            radius = size.width * 0.32f,
            center = Offset(size.width * 0.72f, size.height * 0.62f)
        )
        // Twinkling lights in the window area (upper left)
        repeat(6) { i ->
            val phase = ((twinkle + i * 0.17f) % 1f)
            val a = if (phase < 0.5f) phase * 2f else (1f - phase) * 2f
            drawCircle(Color(0xFFFFF4C2).copy(alpha = 0.55f * a), radius = 1.6.dp.toPx(),
                center = Offset(size.width * (0.08f + i * 0.055f), size.height * (0.18f + (i % 3) * 0.07f)))
        }
        // Time-of-day tint: cozy night blue, golden sunset
        when (hour) {
            in 20..23, in 0..5 -> drawRect(Color(0xFF0B1A3A).copy(alpha = 0.18f))
            in 17..19 -> drawRect(Color(0xFFFF9A3C).copy(alpha = 0.10f))
            else -> {}
        }
    }
}

// ─── Code-drawn pixel room (used until artwork PNGs are added) ────────────────

@Composable
private fun PixelRoomFallback(theme: RoomTheme, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "fallbackFire")
    val f1 by t.animateFloat(0.7f, 1f, infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse), label = "f1")
    val f2 by t.animateFloat(1f, 0.75f, infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "f2")

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val floorY = h * 0.68f
        // wall
        drawRect(theme.wall, size = Size(w, floorY))
        // wall texture: logs / bricks / panels
        val lineColor = Color.Black.copy(alpha = 0.18f)
        var y = 0f
        var row = 0
        while (y < floorY) {
            drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
            if (theme == RoomTheme.LOFT) {
                var x = if (row % 2 == 0) 0f else w * 0.04f
                while (x < w) { drawLine(lineColor, Offset(x, y), Offset(x, y + h * 0.05f), strokeWidth = 1.5f); x += w * 0.08f }
            }
            y += h * 0.05f; row++
        }
        // window with sky
        val winL = w * 0.05f; val winT = h * 0.1f; val winW = w * 0.4f; val winH = h * 0.42f
        drawRect(Brush.verticalGradient(listOf(theme.skyTop, theme.skyBottom), startY = winT, endY = winT + winH),
            topLeft = Offset(winL, winT), size = Size(winW, winH))
        // skyline / scenery silhouette
        val sil = Path().apply {
            moveTo(winL, winT + winH)
            when (theme) {
                RoomTheme.DEN -> { lineTo(winL + winW * 0.2f, winT + winH * 0.3f); lineTo(winL + winW * 0.5f, winT + winH * 0.5f); lineTo(winL + winW * 0.75f, winT + winH * 0.25f); lineTo(winL + winW, winT + winH * 0.45f) }
                else -> { // city skyline
                    var x = winL
                    var k = 0
                    while (x < winL + winW) {
                        val bh = winH * (0.35f + (k * 37 % 5) * 0.08f)
                        lineTo(x, winT + winH - bh); lineTo(x + winW * 0.09f, winT + winH - bh)
                        x += winW * 0.09f; k++
                    }
                }
            }
            lineTo(winL + winW, winT + winH); close()
        }
        drawPath(sil, Color.Black.copy(alpha = 0.45f))
        drawRect(ScrapbookDark, topLeft = Offset(winL, winT), size = Size(winW, winH), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f))
        drawLine(ScrapbookDark, Offset(winL + winW / 2, winT), Offset(winL + winW / 2, winT + winH), strokeWidth = 4f)
        // floor + rug
        drawRect(theme.floor, topLeft = Offset(0f, floorY), size = Size(w, h - floorY))
        drawOval(CGreenDeep.copy(alpha = 0.55f), topLeft = Offset(w * 0.15f, h * 0.76f), size = Size(w * 0.7f, h * 0.18f))
        // fireplace
        val fpL = w * 0.6f; val fpT = h * 0.25f; val fpW = w * 0.25f; val fpH = floorY - fpT
        drawRect(Color(0xFF8A8A8A), topLeft = Offset(fpL, fpT), size = Size(fpW, fpH))
        drawRect(Color(0xFF1A1A1A), topLeft = Offset(fpL + fpW * 0.2f, fpT + fpH * 0.45f), size = Size(fpW * 0.6f, fpH * 0.55f))
        val baseY = floorY
        val cx = fpL + fpW / 2
        val flame1 = Path().apply { moveTo(cx - fpW * 0.22f, baseY); lineTo(cx, baseY - fpH * 0.45f * f1); lineTo(cx + fpW * 0.22f, baseY); close() }
        val flame2 = Path().apply { moveTo(cx - fpW * 0.12f, baseY); lineTo(cx + fpW * 0.02f, baseY - fpH * 0.3f * f2); lineTo(cx + fpW * 0.14f, baseY); close() }
        drawPath(flame1, theme.flame)
        drawPath(flame2, Color(0xFFFFE08A))
        drawRect(ScrapbookDark, topLeft = Offset(fpL, fpT), size = Size(fpW, fpH), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        // lamp glow top-left of fireplace
        drawCircle(Color(0xFFFFE6A0).copy(alpha = 0.35f), radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.36f))
    }
}


// ─── Friends placed on the illustrated room (standing / sitting on furniture) ──

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomSeatsOnArtwork(
    theme: RoomTheme,
    seated: List<UserProfileData?>,
    isOwner: Boolean,
    onTapEmpty: (Int) -> Unit,
    onTapUser: (UserProfileData) -> Unit,
    onLongPress: (Int) -> Unit
) {
    val pulse by rememberGlowRange(0.35f, 1f)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val roomW = maxWidth
        val roomH = maxHeight
        theme.seats.forEachIndexed { i, spot ->
            val user = seated.getOrNull(i)
            // Habbo "l" sprites are roughly 64 x 110; scale to ~24% of the room height when standing
            val avatarH = roomH * (if (spot.sitting) 0.20f else 0.24f)
            val avatarW = avatarH * 0.6f
            val drop = remember(user?.uid) { Animatable(if (user != null) -40f else 0f) }
            LaunchedEffect(user?.uid) { if (user != null) drop.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 380f)) }
            val bob by rememberGlowRange(if (i % 2 == 0) -1.2f else 1.2f, if (i % 2 == 0) 1.2f else -1.2f)

            if (user == null) {
                if (isOwner) {
                    // Pulsing "+" marker on the floor/seat where a friend can go
                    Box(
                        modifier = Modifier
                            .offset(x = roomW * spot.x - 15.dp, y = roomH * spot.y - 15.dp)
                            .size(30.dp)
                            .graphicsLayer { alpha = pulse }
                            .clip(CircleShape)
                            .background(ScrapbookDark.copy(alpha = 0.75f))
                            .border(2.dp, CGreenMint, CircleShape)
                            .clickable { onTapEmpty(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = CGreenMint))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .offset(x = roomW * spot.x - avatarW / 2, y = roomH * spot.y - avatarH)
                        .width(avatarW + 30.dp)
                        .offset(x = (-15).dp)
                        .graphicsLayer { translationY = (drop.value + if (spot.sitting) 0f else bob) * density },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(width = avatarW, height = avatarH)
                            .combinedClickable(onClick = { onTapUser(user) }, onLongClick = { if (isOwner) onLongPress(i) }),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (!spot.sitting) {
                            // soft floor shadow under standing friends
                            Box(Modifier.size(width = avatarW * 0.7f, height = 6.dp).clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.25f)))
                        }
                        if (user.habboUsername.isNotBlank()) {
                            AsyncImage(
                                model = habboPoseUrl(user.habboUsername, user.habboRegion.ifBlank { "habbo.com" }, spot.sitting, spot.direction),
                                contentDescription = user.username,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.padding(bottom = 6.dp).size(avatarW * 0.9f).clip(CircleShape)
                                    .background(CGreenMint).border(2.5.dp, ScrapbookDark, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (user.profilePictureUrl.isNotBlank()) AsyncImage(model = user.profilePictureUrl, contentDescription = user.username,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                else Text(user.username.take(1).uppercase(), style = TextStyle(fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookDark))
                            }
                        }
                    }
                    // tiny nameplate
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(ScrapbookDark.copy(alpha = 0.8f))
                            .border(1.dp, CGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(user.username.ifBlank { "P${i + 2}" }, style = TextStyle(fontFamily = BangersFontFamily, fontSize = 8.sp, color = Color.White),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

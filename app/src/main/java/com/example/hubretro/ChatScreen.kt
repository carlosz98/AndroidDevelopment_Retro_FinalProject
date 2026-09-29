package com.example.hubretro

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

val chatReactionEmojis = listOf("❤️", "😂", "😮", "😢", "👍", "🔥", "🎮", "👾")
val quickEmojis = listOf("👍", "❤️", "😂", "🔥", "🎮", "👾", "💯", "🏆")

// ─── Background definitions ───────────────────────────────────────────────────

data class ChatBackground(
    val key: String,
    val label: String,
    val emoji: String,
    val color: Color,
    val pattern: BackgroundPattern = BackgroundPattern.SOLID
)

enum class BackgroundPattern { SOLID, PIXEL_GRID, SCANLINES, DOTS, DIAGONAL }

private data class ChatMessageAction(val emoji: String, val label: String, val onClick: () -> Unit)

val chatBackgrounds = listOf(
    ChatBackground("default",    "Classic",     "📜", ComicGlassBg),
    ChatBackground("dark",       "Dark Mode",   "🌙", ComicGlassBg),
    ChatBackground("pixel_grid", "Pixel Grid",  "🟩", ComicGlassBg,       BackgroundPattern.PIXEL_GRID),
    ChatBackground("scanlines",  "Scanlines",   "📺", ComicGlassBg,    BackgroundPattern.SCANLINES),
    ChatBackground("dots",       "Retro Dots",  "🔵", ComicGlassBg,    BackgroundPattern.DOTS),
    ChatBackground("diagonal",   "Diagonal",    "⚡", Color.White.copy(alpha = 0.46f),       BackgroundPattern.DIAGONAL),
    ChatBackground("yellow",     "Golden",      "⭐", ComicGlassBg),
    ChatBackground("green",      "Forest",      "🌿", CGreenDeep),
    ChatBackground("purple",     "Neon Night",  "💜", CAcPurple),
    ChatBackground("red",        "Retro Red",   "❤️", ComicGlassBgAlt),
)

@Composable
fun ChatBackgroundBox(
    backgroundKey: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = chatBackgrounds.find { it.key == backgroundKey } ?: chatBackgrounds.first()
    Box(
        modifier = modifier.background(bg.color).drawBehind {
            when (bg.pattern) {
                BackgroundPattern.PIXEL_GRID -> drawPixelGrid(this)
                BackgroundPattern.SCANLINES  -> drawScanlines(this)
                BackgroundPattern.DOTS       -> drawDots(this)
                BackgroundPattern.DIAGONAL   -> drawDiagonal(this)
                BackgroundPattern.SOLID      -> {}
            }
        },
        content = content
    )
}

private fun drawPixelGrid(scope: DrawScope) {
    val gridSize = 24f; val lineColor = Color(0xFF000000).copy(alpha = 0.06f)
    var x = 0f; while (x < scope.size.width) { scope.drawLine(lineColor, Offset(x, 0f), Offset(x, scope.size.height), strokeWidth = 1f); x += gridSize }
    var y = 0f; while (y < scope.size.height) { scope.drawLine(lineColor, Offset(0f, y), Offset(scope.size.width, y), strokeWidth = 1f); y += gridSize }
}
private fun drawScanlines(scope: DrawScope) {
    val lineColor = Color(0xFF000000).copy(alpha = 0.05f); var y = 0f
    while (y < scope.size.height) { scope.drawLine(lineColor, Offset(0f, y), Offset(scope.size.width, y), strokeWidth = 2f); y += 4f }
}
private fun drawDots(scope: DrawScope) {
    val dotColor = Color(0xFF000000).copy(alpha = 0.08f); var x = 15f
    while (x < scope.size.width) { var y = 15f; while (y < scope.size.height) { scope.drawCircle(dotColor, radius = 2f, center = Offset(x, y)); y += 30f }; x += 30f }
}
private fun drawDiagonal(scope: DrawScope) {
    val lineColor = Color(0xFF000000).copy(alpha = 0.05f); var i = -scope.size.height
    while (i < scope.size.width) { scope.drawLine(lineColor, Offset(i, 0f), Offset(i + scope.size.height, scope.size.height), strokeWidth = 1f); i += 30f }
}

// ─── Chat Screen ──────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    chatRoom: ChatRoom,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    // Messages newer than this pop in like speech bubbles
    val chatOpenedAt = remember { System.currentTimeMillis() - 1500L }
    val messages      by chatViewModel.messages.collectAsState()
    val isUploading   by chatViewModel.isUploading.collectAsState()
    val profile       by authViewModel.userProfile.collectAsState()
    val typingUids    by chatViewModel.typingUids.collectAsState()
    val onlineUsers   by chatViewModel.onlineUsers.collectAsState()
    val listState     = rememberLazyListState()

    var messageText          by remember { mutableStateOf("") }
    var showImageWarning     by remember { mutableStateOf(false) }
    var pendingImageUri      by remember { mutableStateOf<android.net.Uri?>(null) }
    var reactionTargetId     by remember { mutableStateOf<String?>(null) }
    var replyingTo           by remember { mutableStateOf<ChatMessage?>(null) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showSearch           by remember { mutableStateOf(false) }
    var searchQuery          by remember { mutableStateOf("") }
    var showGifSearch        by remember { mutableStateOf(false) }
    var showSendBurst        by remember { mutableStateOf(false) }

    val backgroundKey  = chatRoom.backgroundKey
    val otherUid       = chatViewModel.getOtherUid(chatRoom)
    val isOtherOnline  = onlineUsers.contains(otherUid)

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingImageUri = it; showImageWarning = true }
    }

    LaunchedEffect(chatRoom.id)    { chatViewModel.listenToMessages(chatRoom.id) }
    LaunchedEffect(messages.size)  { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
    DisposableEffect(Unit)         { onDispose { chatViewModel.clearMessages(); chatViewModel.stopTyping(chatRoom.id) } }

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }
    val groupedMessages = remember(filteredMessages) {
        filteredMessages.groupBy { msg ->
            val cal = Calendar.getInstance(); cal.timeInMillis = msg.timestamp
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatBackgroundBox(backgroundKey = backgroundKey, modifier = Modifier.fillMaxSize()) {}
        HalftoneBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CGreen, CGreenMint, CGreen)
                        )
                    )
                    .border(BorderStroke(1.dp, CGreen.copy(alpha = 0.2f)))
                    .padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)
            ) {
                // Scan shimmer line at bottom
                val scanT = rememberInfiniteTransition(label = "hdrScan")
                val scanX by scanT.animateFloat(-400f, 600f,
                    infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "hdrScanX")
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter)
                    .background(Brush.horizontalGradient(
                        listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.3f), Color.Transparent),
                        startX = scanX, endX = scanX + 180f)))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Back button
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(2.dp, ScrapbookDark, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                            tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Avatar
                    Box(modifier = Modifier.size(42.dp)) {
                        val pic = chatViewModel.getChatProfilePic(chatRoom)
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.7f))
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.8f), CGreen.copy(alpha = 0.4f))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                chatRoom.type == "group" -> Text("👥", fontSize = 18.sp)
                                pic.isNotBlank() -> AsyncImage(model = pic, contentDescription = null,
                                    contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(pic).fillMaxSize())
                                else -> Icon(Icons.Filled.Person, contentDescription = null,
                                    tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                            }
                        }
                        if (isOtherOnline && chatRoom.type == "dm") {
                            Box(modifier = Modifier.size(12.dp).align(Alignment.BottomEnd).clip(CircleShape)
                                .background(CGreen).border(2.dp, ComicGlassBg, CircleShape))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            chatViewModel.getChatDisplayName(chatRoom).uppercase(),
                            fontFamily = BangersFontFamily, color = ScrapbookDark,
                            fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        when {
                            typingUids.isNotEmpty() -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ThreeDotsAnimation(color = ScrapbookDark, dotSize = 4.dp)
                                Text("typing", fontFamily = NunitoFontFamily,
                                    color = ScrapbookDark.copy(alpha = 0.8f), fontSize = 11.sp, fontStyle = FontStyle.Italic)
                            }
                            isOtherOnline && chatRoom.type == "dm" ->
                                Text("● Online", fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            chatRoom.type == "group" ->
                                Text("${chatRoom.memberUids.size} members", fontFamily = NunitoFontFamily,
                                    color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                            else ->
                                Text("Offline", fontFamily = NunitoFontFamily,
                                    color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }

                    // Search toggle
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(2.dp, ScrapbookDark, CircleShape)
                            .clickable { showSearch = !showSearch; searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showSearch) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = null, tint = ScrapbookDark,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // Background picker toggle
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(2.dp, ScrapbookDark, CircleShape)
                            .clickable { showBackgroundPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Palette, contentDescription = "Background",
                            tint = ScrapbookDark, modifier = Modifier.size(17.dp))
                    }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(BorderStroke(1.5.dp, ScrapbookDark))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it },
                        placeholder = { Text("Search messages...", fontFamily = NunitoFontFamily, fontSize = 13.sp,
                            color = ScrapbookDark.copy(alpha = 0.4f)) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = CGreenDeep, modifier = Modifier.size(17.dp)) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScrapbookDark,
                            unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f),
                            focusedContainerColor = Color.White.copy(alpha = 0.8f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = ScrapbookDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Pinned message ────────────────────────────────────────────────
            AnimatedVisibility(visible = !chatRoom.pinnedMessageText.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(CGreen.copy(alpha = 0.12f), Color.Transparent, CGreen.copy(alpha = 0.12f))
                            )
                        )
                        .border(BorderStroke(1.dp, CGreen.copy(alpha = 0.25f)))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PushPin, contentDescription = null,
                            tint = CGreenDeep, modifier = Modifier.size(13.dp))
                        Text(
                            "📌 ${chatRoom.pinnedMessageText ?: ""}",
                            fontFamily = NunitoFontFamily, color = ScrapbookDark,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { chatViewModel.unpinMessage(chatRoom.id) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.55f), modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            // ── Messages ──────────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedMessages.entries.forEach { groupEntry ->
                    val dayMessages = groupEntry.value
                    item {
                        val today     = Calendar.getInstance()
                        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                        val msgCal    = Calendar.getInstance().apply { timeInMillis = dayMessages.first().timestamp }
                        val dateLabel = when {
                            msgCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    msgCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                            msgCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                                    msgCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
                            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(dayMessages.first().timestamp))
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            // Frosted glass date chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(dateLabel, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                    itemsIndexed(dayMessages, key = { _, msg -> msg.id }) { index, message ->
                        val isMe       = message.senderId == chatViewModel.currentUid
                        val isLastRead = messages.lastOrNull { it.readBy.size > 1 }?.id == message.id
                        SpringEntrance(delayMs = index * 30, modifier = Modifier.bubblePop(message.id, fromRight = isMe, enabled = message.timestamp > chatOpenedAt)) {
                            ChatMessageBubble(
                                message       = message,
                                isMe          = isMe,
                                showReactions = reactionTargetId == message.id,
                                showMenu      = false,
                                isLastRead    = isLastRead && isMe,
                                currentUid    = chatViewModel.currentUid,
                                neonAlpha     = neonAlpha,
                                onLongPress   = { reactionTargetId = if (reactionTargetId == message.id) null else message.id },
                                onDoubleTap   = { chatViewModel.toggleReaction(chatRoom.id, message.id, "❤️") },
                                onReact       = { emoji -> chatViewModel.toggleReaction(chatRoom.id, message.id, emoji); reactionTargetId = null },
                                onReply       = { replyingTo = message; reactionTargetId = null },
                                onPin         = { chatViewModel.pinMessage(chatRoom.id, message); reactionTargetId = null },
                                onDelete      = { if (isMe) { chatViewModel.deleteMessage(chatRoom.id, message.id); reactionTargetId = null } }
                            )
                        }
                    }
                }

                // Typing indicator
                if (typingUids.isNotEmpty()) {
                    item(key = "typing") {
                        // Comic thought bubble: cloud + two trailing puffs, gently bobbing
                        val bob by rememberGlowRange(-2f, 2f)
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                                .graphicsLayer { translationY = bob * density },
                            horizontalAlignment = Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(50))
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                ThreeDotsAnimation(color = CGreenDeep, dotSize = 6.dp)
                            }
                            Box(Modifier.padding(start = 10.dp, top = 3.dp).size(12.dp).clip(CircleShape)
                                .background(Color.White).border(2.dp, ScrapbookDark, CircleShape))
                            Box(Modifier.padding(start = 4.dp, top = 2.dp).size(7.dp).clip(CircleShape)
                                .background(Color.White).border(1.5.dp, ScrapbookDark, CircleShape))
                        }
                    }
                }
            }

            // ── Quick emoji + retro sticker bar ──────────────────────────────
            val quickStickers = listOf("🎮🔥", "👾💀", "🏆✨", "❤️🎮", "💯🔥", "🕹️👑", "😤🎯", "👻🎮")
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(BorderStroke(1.dp, ScrapbookDark.copy(alpha = 0.3f)))
            ) {
                Column {
                    // Single emoji row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickEmojis) { emoji ->
                            var eq by remember { mutableStateOf(false) }
                            val es by animateFloatAsState(
                                targetValue = if (eq) 1.45f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                                label = "eq_$emoji"
                            )
                            Box(
                                modifier = Modifier.scale(es).size(34.dp).clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.7f))
                                    .border(1.5.dp, ScrapbookDark, CircleShape)
                                    .clickable { eq = true; chatViewModel.sendMessage(chatRoom.id, emoji, profile) },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 18.sp) }
                            LaunchedEffect(eq) { if (eq) { kotlinx.coroutines.delay(200); eq = false } }
                        }
                    }
                    // Retro sticker combos row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickStickers) { sticker ->
                            var sp by remember { mutableStateOf(false) }
                            val ss by animateFloatAsState(
                                targetValue = if (sp) 0.88f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "sp_$sticker"
                            )
                            Box(
                                modifier = Modifier.scale(ss)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ComicGlassBg)
                                    .border(1.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                    .clickable { sp = true; chatViewModel.sendMessage(chatRoom.id, sticker, profile) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) { Text(sticker, fontSize = 16.sp) }
                            LaunchedEffect(sp) { if (sp) { kotlinx.coroutines.delay(180); sp = false } }
                        }
                    }
                }
            }

            // ── Reply preview ─────────────────────────────────────────────────
            AnimatedVisibility(visible = replyingTo != null, enter = expandVertically(), exit = shrinkVertically()) {
                replyingTo?.let { reply ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.6f))
                            .border(BorderStroke(1.5.dp, ScrapbookDark))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp))
                                .background(Brush.verticalGradient(listOf(CGreen, CGreen.copy(alpha = 0.4f)))))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Replying to ${reply.senderUsername}", fontFamily = BangersFontFamily,
                                    color = CGreenDeep, fontSize = 11.sp)
                                Text(reply.text.take(60).ifBlank { "📷 Image" }, fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextMuted, fontSize = 12.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.55f), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                    .background(ScrapbookDark.copy(alpha = 0.12f)))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f))
                    .border(BorderStroke(2.dp, ScrapbookDark))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Top gloss line
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.TopCenter)
                    .background(Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent))))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Image attach button
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                            .border(
                                width = 1.5.dp,
                                color = ScrapbookDark,
                                shape = CircleShape
                            )
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = CGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Image, contentDescription = "Send image",
                                tint = ScrapbookDark.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }

                    // Text field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            if (it.isNotBlank()) chatViewModel.onTyping(chatRoom.id)
                            else chatViewModel.stopTyping(chatRoom.id)
                        },
                        placeholder = {
                            Text("Type a message...", fontFamily = NunitoFontFamily,
                                color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 14.sp)
                        },
                        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScrapbookDark,
                            unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f),
                            focusedContainerColor = Color.White.copy(alpha = 0.8f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = ScrapbookDark
                        ),
                        shape = RoundedCornerShape(22.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(chatRoom.id, messageText, profile, replyingTo)
                                messageText = ""; replyingTo = null
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    // Send button
                    var sendPressed by remember { mutableStateOf(false) }
                    val sendScale by animateFloatAsState(
                        targetValue = if (sendPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "sendScale"
                    )
                    val canSend = messageText.isNotBlank()
                    Box(modifier = Modifier.size(40.dp)) {
                        Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                            .clip(CircleShape).background(ScrapbookDark.copy(alpha = 0.18f)))
                    Box(
                        modifier = Modifier
                            .scale(sendScale)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend)
                                    Brush.linearGradient(listOf(CGreen, CGreenMint))
                                else
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.4f)))
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (canSend) ScrapbookDark else ScrapbookDark.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable(enabled = canSend) {
                                sendPressed = true
                                chatViewModel.sendMessage(chatRoom.id, messageText, profile, replyingTo)
                                messageText = ""; replyingTo = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send",
                            tint = if (canSend) ScrapbookDark else ScrapbookDark.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp))
                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 20.dp)
                    }
                    } // end send button shadow wrapper
                    LaunchedEffect(sendPressed) {
                        if (sendPressed) {
                            showSendBurst = true
                            kotlinx.coroutines.delay(150); sendPressed = false
                            kotlinx.coroutines.delay(800); showSendBurst = false
                        }
                    }
                }
                ComicShimmer(Modifier.matchParentSize(), alpha = 0.35f)
            }
            } // end input bar shadow wrapper
        }

        // ── Background picker sheet ───────────────────────────────────────────
        if (showBackgroundPicker) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showBackgroundPicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(ComicGlassBg)
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(24.dp)
                        .clickable { }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(3.dp).height(22.dp).clip(RoundedCornerShape(2.dp))
                                .background(Brush.verticalGradient(listOf(CGreen, CGreen.copy(alpha = 0.4f)))))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHAT BACKGROUND", fontFamily = BangersFontFamily, color = ScrapbookDark,
                                fontSize = 20.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showBackgroundPicker = false }) {
                                Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.6f))
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        ) {
                            gridItems(chatBackgrounds) { bg: ChatBackground ->
                                val isSelected = backgroundKey == bg.key
                                var bgPressed by remember { mutableStateOf(false) }
                                val bgScale by animateFloatAsState(
                                    targetValue = if (bgPressed) 0.88f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "bgScale_${bg.key}"
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.scale(bgScale).clickable {
                                        bgPressed = true
                                        chatViewModel.setChatBackground(chatRoom.id, bg.key)
                                        showBackgroundPicker = false
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(14.dp))
                                            .background(bg.color)
                                            .border(2.5.dp, ScrapbookDark, shape = RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(bg.emoji, fontSize = 22.sp)
                                        if (isSelected) {
                                            Box(modifier = Modifier.fillMaxSize()
                                                .background(Brush.verticalGradient(
                                                    listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                                                    startY = 0f, endY = 58f)))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(bg.label, fontFamily = NunitoFontFamily, color = if (isSelected) CGreenDeep else ScrapbookDark.copy(alpha = 0.7f),
                                        fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    LaunchedEffect(bgPressed) { if (bgPressed) { kotlinx.coroutines.delay(150); bgPressed = false } }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── GIF Search Sheet ──────────────────────────────────────────────────
        if (showGifSearch) {
            val gifResults  by chatViewModel.gifResults
            val gifSearching by chatViewModel.gifSearching
            var gifQuery by remember { mutableStateOf("") }
            LaunchedEffect(gifQuery) {
                if (gifQuery.length < 2) { chatViewModel.gifResults.value = emptyList(); return@LaunchedEffect }
                kotlinx.coroutines.delay(500)
                chatViewModel.searchGifs(gifQuery)
            }
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showGifSearch = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.70f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(ComicGlassBg)
                        .border(BorderStroke(2.5.dp, ScrapbookDark), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable {}
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp))
                                .background(Brush.verticalGradient(listOf(CGreenMint, CGreenMint.copy(alpha = 0.3f)))))
                            Text("SEND A GIF", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                            if (gifSearching) CircularProgressIndicator(color = CGreenDeep, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.6f))
                                .border(1.5.dp, ScrapbookDark, CircleShape)
                                .clickable { showGifSearch = false },
                                contentAlignment = Alignment.Center
                            ) { Text("✕", color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 14.sp) }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Search field
                        OutlinedTextField(
                            value = gifQuery, onValueChange = { gifQuery = it },
                            placeholder = { Text("Search GIFs…", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.4f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = CGreenDeep, modifier = Modifier.size(17.dp)) },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookDark),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScrapbookDark,
                                unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.4f),
                                focusedContainerColor = Color.White.copy(alpha = 0.8f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                                cursorColor = ScrapbookDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        // GIF grid
                        if (gifResults.isEmpty() && !gifSearching) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🎞️", fontSize = 40.sp)
                                    Text("Search for a GIF above", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                gridItems(gifResults) { gif: GifResult ->
                                    Box(
                                        modifier = Modifier.aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ComicGlassBg)
                                            .clickable {
                                                chatViewModel.sendGif(chatRoom.id, gif.originalUrl, profile)
                                                showGifSearch = false
                                            }
                                    ) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data(gif.previewUrl)
                                                .decoderFactory(coil.decode.GifDecoder.Factory())
                                                .build(),
                                            contentDescription = gif.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Send burst particles ──────────────────────────────────────────────
        if (showSendBurst) {
            val particles = remember { (0..10).map { Triple((10..350).random().toFloat(), (0.6f + Math.random() * 0.4f).toFloat(), listOf("⭐","🔥","✨","💫","🎮","❤️","💯","👾").random()) } }
            particles.forEach { ptcl ->
                    val xPct = ptcl.first; val speed = ptcl.second; val emoji = ptcl.third
                key(xPct) {
                    val offY by animateFloatAsState(targetValue = -140f * speed, animationSpec = tween((500 + (speed * 300).toInt()), easing = FastOutSlowInEasing), label = "by_$xPct")
                    val alph by animateFloatAsState(targetValue = 0f, animationSpec = tween(700), label = "ba_$xPct")
                    Box(modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-50 + xPct * 0.8f).dp, y = offY.dp).alpha(1f - alph)) {
                        Text(emoji, fontSize = (10 + speed * 7).sp)
                    }
                }
            }
        }

        // ── Image warning dialog ──────────────────────────────────────────────
        if (showImageWarning) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ComicGlassBg)
                        .border(2.5.dp, ScrapbookDark, RoundedCornerShape(20.dp))
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 48.sp)
                        Text("SHARE IMAGE?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, textAlign = TextAlign.Center)
                        Text(
                            "Images you send will be blurred for the recipient until they choose to reveal them.\n\nMake sure you trust this person before sharing.",
                            fontFamily = NunitoFontFamily, color = ScrapbookTextMuted,
                            fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                    .clickable { showImageWarning = false; pendingImageUri = null }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 16.sp) }
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(CGreen, CGreenMint)))
                                    .border(2.dp, ScrapbookDark, RoundedCornerShape(12.dp))
                                    .clickable {
                                        showImageWarning = false
                                        pendingImageUri?.let { uri -> chatViewModel.sendImage(chatRoom.id, uri, profile) }
                                        pendingImageUri = null
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("SEND", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Message Bubble ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showReactions: Boolean,
    showMenu: Boolean,
    isLastRead: Boolean,
    currentUid: String,
    neonAlpha: Float,
    onLongPress: () -> Unit,
    onDoubleTap: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    var revealedImage by remember { mutableStateOf(false) }
    val isDeleted = message.deleted

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

        if (!isMe && message.senderUsername.isNotBlank()) {
            Text(message.senderUsername, fontFamily = BangersFontFamily,
                color = ScrapbookTextMuted.copy(alpha = 0.7f), fontSize = 11.sp,
                modifier = Modifier.padding(start = 44.dp, bottom = 2.dp))
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
            if (!isMe) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(ScrapbookDark, ScrapbookDark.copy(alpha = 0.6f))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (message.senderProfilePicUrl.isNotBlank()) {
                        AsyncImage(model = message.senderProfilePicUrl, contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(message.senderProfilePicUrl).fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null,
                            tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

                // Reply preview
                if (!message.replyToId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.widthIn(max = 240.dp).padding(bottom = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isMe) CGreen.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.45f))
                            .border(2.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.width(2.dp).height(32.dp).clip(RoundedCornerShape(1.dp))
                                .background(Brush.verticalGradient(listOf(CGreen, CGreen.copy(alpha = 0.3f)))))
                            Column {
                                Text(message.replyToSender ?: "", fontFamily = BangersFontFamily, color = CGreenDeep, fontSize = 10.sp)
                                Text(message.replyToText ?: "", fontFamily = NunitoFontFamily,
                                    color = if (isMe) ScrapbookDark.copy(alpha = 0.6f) else ScrapbookTextMuted,
                                    fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                val bubbleShape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                )

                Box(modifier = Modifier.widthIn(max = 260.dp)) {
                    Box(modifier = Modifier.matchParentSize().offset(x = 4.dp, y = 4.dp)
                        .clip(bubbleShape).background(ScrapbookDark.copy(alpha = 0.12f)))
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(bubbleShape)
                        .background(
                            when {
                                isDeleted -> SolidColor(if (isMe) CGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f))
                                isMe -> Brush.linearGradient(
                                    colors = listOf(CGreen, CGreenDeep),
                                    start = Offset(0f, 0f), end = Offset(260f, 80f)
                                )
                                else -> SolidColor(Color.White.copy(alpha = 0.95f))
                            }
                        )
                        .border(1.5.dp, ScrapbookDark, shape = bubbleShape)
                        .combinedClickable(
                            onClick = { if (!isDeleted) onDoubleTap() },
                            onLongClick = { onLongPress() }
                        )
                        .padding(
                            horizontal = if (message.imageUrl != null) 0.dp else 12.dp,
                            vertical = if (message.imageUrl != null) 0.dp else 10.dp
                        )
                ) {
                    // Gloss dome on "me" bubbles
                    if (isMe && !isDeleted && message.imageUrl == null) {
                        Box(modifier = Modifier.matchParentSize()
                            .clip(bubbleShape)
                            .background(Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                                startY = 0f, endY = 30f)))
                    }
                    when {
                        isDeleted -> Text("🚫 This message was deleted", fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 13.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(4.dp))
                        message.gifUrl != null -> Box(modifier = Modifier.widthIn(max = 220.dp).clip(RoundedCornerShape(14.dp))) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(message.gifUrl)
                                    .decoderFactory(coil.decode.GifDecoder.Factory())
                                    .build(),
                                contentDescription = "GIF",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text("GIF", fontFamily = BangersFontFamily, color = Color.White, fontSize = 9.sp, letterSpacing = 1.sp)
                            }
                        }
                        message.imageUrl != null -> Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(model = message.imageUrl, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.halftoneReveal(message.imageUrl).fillMaxSize().then(if (!revealedImage && !isMe) Modifier.blur(20.dp) else Modifier))
                            if (!revealedImage && !isMe) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
                                        .clickable { revealedImage = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("⚠️", fontSize = 28.sp)
                                        Text("TAP TO REVEAL", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                                        Text("Be sure you trust\nthis person", fontFamily = NunitoFontFamily,
                                            color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                        else -> Text(message.text, fontFamily = NunitoFontFamily,
                            color = if (isMe) Color.White else ScrapbookDark,
                            fontSize = 14.sp, lineHeight = 20.sp)
                    }
                    // Shimmer overlay on own (sent) bubbles
                    if (isMe && !isDeleted) {
                        ComicShimmer(Modifier.matchParentSize(), cornerRadius = 16.dp, alpha = 0.55f)
                    }
                }
                } // end shadow wrapper

                // Comic speech-bubble tail pointing toward the sender side
                ComicBubbleTail(
                    isMe = isMe,
                    fill = when {
                        isDeleted -> if (isMe) CGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.25f)
                        isMe -> CGreenDeep
                        else -> Color.White.copy(alpha = 0.95f)
                    },
                    modifier = Modifier.padding(start = if (isMe) 0.dp else 6.dp, end = if (isMe) 6.dp else 0.dp)
                )

                // Timestamp + read receipt
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp, top = 3.dp)
                ) {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                        fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    if (isMe) {
                        Text(if (isLastRead) "✓✓" else "✓", fontFamily = NunitoFontFamily,
                            color = if (isLastRead) CGreen else ScrapbookTextMuted, fontSize = 10.sp)
                    }
                }

                // Reactions
                val allReactions = message.reactions.filter { it.value.isNotEmpty() }
                if (allReactions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(allReactions.entries.toList()) { entry ->
                            val emoji = entry.key
                            val uids = entry.value
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (uids.contains(currentUid))
                                            Brush.linearGradient(listOf(CGreen, CGreenMint))
                                        else
                                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.5f)))
                                    )
                                    .border(2.5.dp, ScrapbookDark, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text("$emoji ${uids.size}", fontSize = 12.sp, fontFamily = NunitoFontFamily, color = ScrapbookDark)
                            }
                        }
                    }
                }
            }
        }

        // Reaction + action picker
        AnimatedVisibility(
            visible = showReactions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(modifier = Modifier.padding(
                start = if (isMe) 0.dp else 44.dp,
                end = if (isMe) 4.dp else 0.dp,
                top = 4.dp
            )) {
                AeroGlassCard(
                    accentColor = CGreen,
                    cornerRadius = 20.dp,
                    glowAlpha = 0.4f,
                    showOffsetShadow = true
                ) {
                    Column {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            chatReactionEmojis.forEach { emoji ->
                                var ep by remember { mutableStateOf(false) }
                                val es by animateFloatAsState(targetValue = if (ep) 1.35f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "ep_$emoji")
                                Text(emoji, fontSize = 22.sp,
                                    modifier = Modifier.scale(es).clickable { ep = true; onReact(emoji) }.padding(4.dp))
                                LaunchedEffect(ep) { if (ep) { kotlinx.coroutines.delay(150); ep = false } }
                            }
                        }
                        HorizontalDivider(color = ScrapbookDark.copy(alpha = 0.2f))
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val actions = buildList {
                                add(ChatMessageAction("↩️", "Reply", onReply))
                                add(ChatMessageAction("📌", "Pin", onPin))
                                if (isMe) add(ChatMessageAction("🗑️", "Delete", onDelete))
                            }
                            actions.forEach { action: ChatMessageAction ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(ComicGlassBg)
                                        .border(1.5.dp, ScrapbookDark, RoundedCornerShape(8.dp))
                                        .clickable { action.onClick() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(action.emoji, fontSize = 14.sp)
                                        Text(action.label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ─── Comic speech-bubble tail ─────────────────────────────────────────────────
// Drawn just under the bubble; overlaps its bottom border by 2dp so it reads as one shape.

@Composable
fun ComicBubbleTail(isMe: Boolean, fill: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.offset(y = (-2).dp).size(width = 16.dp, height = 11.dp)
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            if (isMe) { moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, h); close() }
            else { moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, h); close() }
        }
        drawPath(path, fill)
        if (isMe) {
            drawLine(ScrapbookDark, Offset(0f, 0f), Offset(w, h), strokeWidth = stroke)
            drawLine(ScrapbookDark, Offset(w, 0f), Offset(w, h), strokeWidth = stroke)
        } else {
            drawLine(ScrapbookDark, Offset(w, 0f), Offset(0f, h), strokeWidth = stroke)
            drawLine(ScrapbookDark, Offset(0f, 0f), Offset(0f, h), strokeWidth = stroke)
        }
    }
}

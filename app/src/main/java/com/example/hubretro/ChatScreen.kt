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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

val chatBackgrounds = listOf(
    ChatBackground("default", "Classic", "📜", ScrapbookCream),
    ChatBackground("dark", "Dark Mode", "🌙", Color(0xFF0D0D1F)),
    ChatBackground("pixel_grid", "Pixel Grid", "🟩", ScrapbookCream, BackgroundPattern.PIXEL_GRID),
    ChatBackground("scanlines", "Scanlines", "📺", Color(0xFF0D0D1F), BackgroundPattern.SCANLINES),
    ChatBackground("dots", "Retro Dots", "🔵", Color(0xFF1A1A2E), BackgroundPattern.DOTS),
    ChatBackground("diagonal", "Diagonal", "⚡", ScrapbookPaper, BackgroundPattern.DIAGONAL),
    ChatBackground("yellow", "Golden", "⭐", Color(0xFFFFF8DC)),
    ChatBackground("green", "Forest", "🌿", Color(0xFF0D1F0D)),
    ChatBackground("purple", "Neon Night", "💜", Color(0xFF1A0D2E)),
    ChatBackground("red", "Retro Red", "❤️", Color(0xFF1F0D0D)),
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
                BackgroundPattern.SCANLINES -> drawScanlines(this)
                BackgroundPattern.DOTS -> drawDots(this)
                BackgroundPattern.DIAGONAL -> drawDiagonal(this)
                BackgroundPattern.SOLID -> {}
            }
        },
        content = content
    )
}

private fun drawPixelGrid(scope: DrawScope) {
    val gridSize = 24f
    val lineColor = Color(0xFF000000).copy(alpha = 0.06f)
    var x = 0f
    while (x < scope.size.width) {
        scope.drawLine(lineColor, Offset(x, 0f), Offset(x, scope.size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = 0f
    while (y < scope.size.height) {
        scope.drawLine(lineColor, Offset(0f, y), Offset(scope.size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

private fun drawScanlines(scope: DrawScope) {
    val lineSpacing = 4f
    val lineColor = Color.White.copy(alpha = 0.04f)
    var y = 0f
    while (y < scope.size.height) {
        scope.drawLine(lineColor, Offset(0f, y), Offset(scope.size.width, y), strokeWidth = 2f)
        y += lineSpacing
    }
}

private fun drawDots(scope: DrawScope) {
    val spacing = 30f
    val dotColor = Color.White.copy(alpha = 0.08f)
    var x = spacing / 2
    while (x < scope.size.width) {
        var y = spacing / 2
        while (y < scope.size.height) {
            scope.drawCircle(dotColor, radius = 2f, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
}

private fun drawDiagonal(scope: DrawScope) {
    val spacing = 30f
    val lineColor = Color(0xFF000000).copy(alpha = 0.05f)
    var i = -scope.size.height
    while (i < scope.size.width) {
        scope.drawLine(lineColor, Offset(i, 0f), Offset(i + scope.size.height, scope.size.height), strokeWidth = 1f)
        i += spacing
    }
}

// ─── Chat Screen ──────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    chatRoom: ChatRoom,
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val messages by chatViewModel.messages.collectAsState()
    val isUploading by chatViewModel.isUploading.collectAsState()
    val profile by authViewModel.userProfile.collectAsState()
    val typingUids by chatViewModel.typingUids.collectAsState()
    val onlineUsers by chatViewModel.onlineUsers.collectAsState()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var showImageWarning by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var reactionTargetId by remember { mutableStateOf<String?>(null) }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var showBackgroundPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenuFor by remember { mutableStateOf<String?>(null) }

    val backgroundKey = chatRoom.backgroundKey
    val otherUid = chatViewModel.getOtherUid(chatRoom)
    val isOtherOnline = onlineUsers.contains(otherUid)

    val neonT = rememberInfiniteTransition(label = "chatNeon")
    val neonAlpha by neonT.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse),
        label = "chatNeonAlpha"
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingImageUri = it; showImageWarning = true }
    }

    LaunchedEffect(chatRoom.id) { chatViewModel.listenToMessages(chatRoom.id) }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
    DisposableEffect(Unit) { onDispose { chatViewModel.clearMessages(); chatViewModel.stopTyping(chatRoom.id) } }

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages
        else messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    val groupedMessages = remember(filteredMessages) {
        filteredMessages.groupBy { msg ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = msg.timestamp
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatBackgroundBox(backgroundKey = backgroundKey, modifier = Modifier.fillMaxSize()) {}

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ScrapbookDark)
                    }
                    Box(modifier = Modifier.size(40.dp)) {
                        val pic = chatViewModel.getChatProfilePic(chatRoom)
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (chatRoom.type == "group") { Text("👥", fontSize = 16.sp) }
                            else if (pic.isNotBlank()) { AsyncImage(model = pic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                            else { Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
                        }
                        if (isOtherOnline && chatRoom.type == "dm") {
                            Box(modifier = Modifier.size(12.dp).align(Alignment.BottomEnd).clip(CircleShape).background(ScrapbookGreen).border(2.dp, ScrapbookYellow, CircleShape))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
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
                                Text("typing", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.7f), fontSize = 11.sp, fontStyle = FontStyle.Italic)
                            }
                            isOtherOnline && chatRoom.type == "dm" -> Text("● Online", fontFamily = NunitoFontFamily, color = ScrapbookGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            chatRoom.type == "group" -> Text("${chatRoom.memberUids.size} members", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                            else -> Text("Offline", fontFamily = NunitoFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = { showSearch = !showSearch; searchQuery = "" }) {
                        Icon(imageVector = if (showSearch) Icons.Filled.Close else Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showBackgroundPicker = true }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Background", tint = ScrapbookDark, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("Search messages...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = ScrapbookDark, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScrapbookYellow,
                        unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                        focusedContainerColor = ScrapbookCardWhite,
                        unfocusedContainerColor = ScrapbookCardWhite,
                        cursorColor = ScrapbookDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCardWhite).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // ── Pinned message ────────────────────────────────────────────────
            AnimatedVisibility(visible = !chatRoom.pinnedMessageText.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(ScrapbookYellow.copy(alpha = 0.15f))
                        .border(BorderStroke(1.dp, ScrapbookYellowDark.copy(alpha = 0.4f)))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, tint = ScrapbookYellowDark, modifier = Modifier.size(14.dp))
                        Text(
                            "📌 ${chatRoom.pinnedMessageText ?: ""}",
                            fontFamily = NunitoFontFamily, color = ScrapbookDark,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { chatViewModel.unpinMessage(chatRoom.id) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, null, tint = ScrapbookDark.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
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
                groupedMessages.entries.forEach { (_, dayMessages) ->
                    item {
                        val today = Calendar.getInstance()
                        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                        val msgCal = Calendar.getInstance().apply { timeInMillis = dayMessages.first().timestamp }
                        val dateLabel = when {
                            msgCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                    msgCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                            msgCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                                    msgCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
                            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(dayMessages.first().timestamp))
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(dateLabel, fontFamily = BangersFontFamily, color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp)
                            }
                        }
                    }
                    items(dayMessages, key = { it.id }) { message ->
                        val isMe = message.senderId == chatViewModel.currentUid
                        val isLastRead = messages.lastOrNull { it.readBy.size > 1 }?.id == message.id
                        ChatMessageBubble(
                            message = message,
                            isMe = isMe,
                            showReactions = reactionTargetId == message.id,
                            showMenu = showMenuFor == message.id,
                            isLastRead = isLastRead && isMe,
                            currentUid = chatViewModel.currentUid,
                            neonAlpha = neonAlpha,
                            onLongPress = {
                                reactionTargetId = if (reactionTargetId == message.id) null else message.id
                                showMenuFor = null
                            },
                            onDoubleTap = { chatViewModel.toggleReaction(chatRoom.id, message.id, "❤️") },
                            onReact = { emoji -> chatViewModel.toggleReaction(chatRoom.id, message.id, emoji); reactionTargetId = null },
                            onReply = { replyingTo = message; reactionTargetId = null },
                            onPin = { chatViewModel.pinMessage(chatRoom.id, message); reactionTargetId = null },
                            onDelete = { if (isMe) { chatViewModel.deleteMessage(chatRoom.id, message.id); reactionTargetId = null } }
                        )
                    }
                }

                // Typing indicator
                if (typingUids.isNotEmpty()) {
                    item(key = "typing") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                    .background(ScrapbookCardWhite)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                ThreeDotsAnimation(color = ScrapbookTextMuted, dotSize = 6.dp)
                            }
                        }
                    }
                }
            }

            // ── Quick emoji bar ───────────────────────────────────────────────
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(ScrapbookCardWhite.copy(alpha = 0.8f)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickEmojis) { emoji ->
                    var eq by remember { mutableStateOf(false) }
                    val es by animateFloatAsState(targetValue = if (eq) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "eq_$emoji")
                    Text(emoji, fontSize = 22.sp, modifier = Modifier.scale(es).clickable {
                        eq = true
                        chatViewModel.sendMessage(chatRoom.id, emoji, profile)
                    })
                    LaunchedEffect(eq) { if (eq) { kotlinx.coroutines.delay(200); eq = false } }
                }
            }

            // ── Reply preview ─────────────────────────────────────────────────
            AnimatedVisibility(visible = replyingTo != null, enter = expandVertically(), exit = shrinkVertically()) {
                replyingTo?.let { reply ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(ScrapbookPaper)
                            .border(BorderStroke(1.dp, ScrapbookBorder))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellowDark))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Replying to ${reply.senderUsername}", fontFamily = BangersFontFamily, color = ScrapbookYellowDark, fontSize = 11.sp)
                                Text(reply.text.take(60).ifBlank { "📷 Image" }, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(ScrapbookCardWhite)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) { CircularProgressIndicator(color = ScrapbookYellowDark, modifier = Modifier.size(18.dp), strokeWidth = 2.dp) }
                        else { Icon(Icons.Filled.Image, contentDescription = "Send image", tint = ScrapbookDark, modifier = Modifier.size(20.dp)) }
                    }
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            if (it.isNotBlank()) chatViewModel.onTyping(chatRoom.id)
                            else chatViewModel.stopTyping(chatRoom.id)
                        },
                        placeholder = { Text("Type a message...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp) },
                        textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ScrapbookDark,
                            unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f),
                            focusedContainerColor = ScrapbookCardWhite,
                            unfocusedContainerColor = ScrapbookCardWhite,
                            cursorColor = ScrapbookDark
                        ),
                        shape = RoundedCornerShape(20.dp),
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
                    var sendPressed by remember { mutableStateOf(false) }
                    val sendScale by animateFloatAsState(targetValue = if (sendPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "sendScale")
                    Box(
                        modifier = Modifier.scale(sendScale).size(40.dp).clip(CircleShape)
                            .background(if (messageText.isNotBlank()) ScrapbookDark else ScrapbookPaper)
                            .border(
                                width = 2.dp,
                                brush = if (messageText.isNotBlank()) Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha)))
                                else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)),
                                shape = CircleShape
                            )
                            .clickable(enabled = messageText.isNotBlank()) {
                                sendPressed = true
                                chatViewModel.sendMessage(chatRoom.id, messageText, profile, replyingTo)
                                messageText = ""; replyingTo = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = if (messageText.isNotBlank()) ScrapbookYellow else ScrapbookTextMuted, modifier = Modifier.size(18.dp))
                    }
                    LaunchedEffect(sendPressed) { if (sendPressed) { kotlinx.coroutines.delay(150); sendPressed = false } }
                }
            }
        }

        // ── Background picker sheet ───────────────────────────────────────────
        if (showBackgroundPicker) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showBackgroundPicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(ScrapbookCream)
                        .border(BorderStroke(2.dp, ScrapbookBorder))
                        .padding(24.dp)
                        .clickable { }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(22.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellowDark))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHAT BACKGROUND", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showBackgroundPicker = false }) {
                                Icon(Icons.Filled.Close, null, tint = ScrapbookDark)
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        ) {
                            items(chatBackgrounds) { bg ->
                                val isSelected = backgroundKey == bg.key
                                var bgPressed by remember { mutableStateOf(false) }
                                val bgScale by animateFloatAsState(targetValue = if (bgPressed) 0.9f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "bgScale_${bg.key}")
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.scale(bgScale).clickable {
                                        bgPressed = true
                                        chatViewModel.setChatBackground(chatRoom.id, bg.key)
                                        showBackgroundPicker = false
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                            .background(bg.color)
                                            .then(
                                                if (isSelected) Modifier.border(3.dp, ScrapbookYellowDark, RoundedCornerShape(12.dp))
                                                else Modifier.border(1.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(bg.emoji, fontSize = 22.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(bg.label, fontFamily = NunitoFontFamily, color = ScrapbookDark, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    LaunchedEffect(bgPressed) { if (bgPressed) { kotlinx.coroutines.delay(150); bgPressed = false } }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Image warning dialog ──────────────────────────────────────────────
        if (showImageWarning) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                ScrapbookCard(modifier = Modifier.fillMaxWidth().padding(32.dp), backgroundColor = ScrapbookCardWhite, cornerRadius = 16.dp, shadowOffset = 6.dp) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 48.sp)
                        Text("SHARE IMAGE?", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 26.sp, textAlign = TextAlign.Center)
                        Text(
                            "Images you send will be blurred for the recipient until they choose to reveal them.\n\nMake sure you trust this person before sharing.",
                            fontFamily = NunitoFontFamily, color = ScrapbookTextMuted,
                            fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ScrapbookPaper)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .clickable { showImageWarning = false; pendingImageUri = null }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("CANCEL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp) }
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        showImageWarning = false
                                        pendingImageUri?.let { uri -> chatViewModel.sendImage(chatRoom.id, uri, profile) }
                                        pendingImageUri = null
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("SEND", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp) }
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
            Text(message.senderUsername, fontFamily = BangersFontFamily, color = ScrapbookTextMuted.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(start = 44.dp, bottom = 2.dp))
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {

            if (!isMe) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(ScrapbookPaper).border(1.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (message.senderProfilePicUrl.isNotBlank()) {
                        AsyncImage(model = message.senderProfilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
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
                            .background(if (isMe) ScrapbookDark.copy(alpha = 0.5f) else ScrapbookPaper)
                            .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.width(2.dp).height(32.dp).clip(RoundedCornerShape(1.dp)).background(ScrapbookYellowDark))
                            Column {
                                Text(message.replyToSender ?: "", fontFamily = BangersFontFamily, color = ScrapbookYellowDark, fontSize = 10.sp)
                                Text(message.replyToText ?: "", fontFamily = NunitoFontFamily, color = if (isMe) Color.White.copy(alpha = 0.6f) else ScrapbookTextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                val bubbleShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMe) 16.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 16.dp)

                Box(
                    modifier = Modifier.widthIn(max = 260.dp)
                        .clip(bubbleShape)
                        .background(
                            when {
                                isDeleted -> if (isMe) ScrapbookDark.copy(alpha = 0.3f) else ScrapbookPaper
                                isMe -> ScrapbookDark
                                else -> ScrapbookCardWhite
                            }
                        )
                        .border(
                            2.dp,
                            if (isMe) Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.5f), ScrapbookYellow.copy(alpha = 0.1f)))
                            else Brush.linearGradient(colors = listOf(ScrapbookBorder, ScrapbookBorder)),
                            bubbleShape
                        )
                        .combinedClickable(
                            onClick = { if (!isDeleted) onDoubleTap() },
                            onLongClick = { onLongPress() }
                        )
                        .padding(
                            horizontal = if (message.imageUrl != null) 0.dp else 12.dp,
                            vertical = if (message.imageUrl != null) 0.dp else 10.dp
                        )
                ) {
                    if (isDeleted) {
                        Text("🚫 This message was deleted", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic, modifier = Modifier.padding(4.dp))
                    } else if (message.imageUrl != null) {
                        Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = message.imageUrl, contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().then(if (!revealedImage && !isMe) Modifier.blur(20.dp) else Modifier)
                            )
                            if (!revealedImage && !isMe) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable { revealedImage = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("⚠️", fontSize = 28.sp)
                                        Text("TAP TO REVEAL", fontFamily = BangersFontFamily, color = Color.White, fontSize = 13.sp)
                                        Text("Be sure you trust\nthis person", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(message.text, fontFamily = NunitoFontFamily, color = if (isMe) Color.White else ScrapbookTextDark, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }

                // Timestamp + read receipt
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = if (isMe) 0.dp else 4.dp, end = if (isMe) 4.dp else 0.dp, top = 2.dp)
                ) {
                    Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)), fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    if (isMe) {
                        Text(if (isLastRead) "✓✓" else "✓", fontFamily = NunitoFontFamily, color = if (isLastRead) ScrapbookGreen else ScrapbookTextMuted, fontSize = 10.sp)
                    }
                }

                // Reactions
                val allReactions = message.reactions.filter { it.value.isNotEmpty() }
                if (allReactions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                        items(allReactions.entries.toList()) { (emoji, uids) ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(if (uids.contains(currentUid)) ScrapbookYellow else ScrapbookCardWhite)
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
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
            Box(modifier = Modifier.padding(start = if (isMe) 0.dp else 44.dp, end = if (isMe) 4.dp else 0.dp, top = 4.dp)) {
                ScrapbookCard(backgroundColor = ScrapbookCardWhite, cornerRadius = 20.dp, shadowOffset = 4.dp) {
                    Column {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            chatReactionEmojis.forEach { emoji ->
                                var ep by remember { mutableStateOf(false) }
                                val es by animateFloatAsState(targetValue = if (ep) 1.3f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "ep_$emoji")
                                Text(emoji, fontSize = 22.sp, modifier = Modifier.scale(es).clickable { ep = true; onReact(emoji) }.padding(4.dp))
                                LaunchedEffect(ep) { if (ep) { kotlinx.coroutines.delay(150); ep = false } }
                            }
                        }
                        HorizontalDivider(color = ScrapbookBorder.copy(alpha = 0.2f))
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            data class Action(val emoji: String, val label: String, val onClick: () -> Unit)
                            val actions = buildList {
                                add(Action("↩️", "Reply", onReply))
                                add(Action("📌", "Pin", onPin))
                                if (isMe) add(Action("🗑️", "Delete", onDelete))
                            }
                            actions.forEach { action ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookPaper)
                                        .border(1.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
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
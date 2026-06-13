package com.example.hubretro

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
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

@Composable
fun ChatListScreen(
    chatViewModel: ChatViewModel,
    authViewModel: AuthViewModel,
    onOpenChat: (ChatRoom) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatRoomsState by chatViewModel.chatRooms.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val searchQuery by chatViewModel.searchQuery.collectAsState()
    val onlineUsers by chatViewModel.onlineUsers.collectAsState()
    val focusManager = LocalFocusManager.current
    var searchVisible by remember { mutableStateOf(false) }

    val neonT = rememberInfiniteTransition(label = "listNeon")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse), label = "listNeonAlpha")

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            chatViewModel.setOnline()
            chatViewModel.listenToChatRooms()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                val scanT = rememberInfiniteTransition(label = "scan")
                val scanX by scanT.animateFloat(initialValue = -400f, targetValue = 400f, animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "scanX")
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookDark.copy(alpha = 0.15f), Color.Transparent), startX = scanX, endX = scanX + 200f)))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        val shimmerT = rememberInfiniteTransition(label = "shimmer")
                        val shimmerX by shimmerT.animateFloat(initialValue = -300f, targetValue = 600f, animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "shimmerX")
                        Box {
                            Text("💬 MESSAGES", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 28.sp, letterSpacing = 2.sp)
                            Text("💬 MESSAGES", fontFamily = BangersFontFamily, fontSize = 28.sp, letterSpacing = 2.sp, style = androidx.compose.ui.text.TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.5f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 100f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 100f, 0f))))
                        }
                        Text("Retro gaming chat", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                    // Search button
                    var searchPressed by remember { mutableStateOf(false) }
                    val searchScale by animateFloatAsState(targetValue = if (searchPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "searchScale")
                    Box(modifier = Modifier.scale(searchScale).size(38.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape).clickable { searchPressed = true; searchVisible = !searchVisible; if (!searchVisible) { chatViewModel.setSearchQuery(""); focusManager.clearFocus() } }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
                    }
                    LaunchedEffect(searchPressed) { if (searchPressed) { kotlinx.coroutines.delay(150); searchPressed = false } }
                    Spacer(modifier = Modifier.width(8.dp))
                    // New chat button
                    var newChatPressed by remember { mutableStateOf(false) }
                    val newChatScale by animateFloatAsState(targetValue = if (newChatPressed) 0.88f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "newChatScale")
                    Box(modifier = Modifier.scale(newChatScale).size(38.dp).clip(CircleShape).background(ScrapbookDark).border(2.dp, ScrapbookBorder, CircleShape).clickable { newChatPressed = true; onNewChat() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Edit, contentDescription = "New chat", tint = ScrapbookYellow, modifier = Modifier.size(18.dp))
                    }
                    LaunchedEffect(newChatPressed) { if (newChatPressed) { kotlinx.coroutines.delay(150); newChatPressed = false } }
                }
            }

            // ── Search bar ────────────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = searchVisible, enter = androidx.compose.animation.expandVertically(), exit = androidx.compose.animation.shrinkVertically()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { chatViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search conversations...", fontFamily = NunitoFontFamily, fontSize = 13.sp, color = ScrapbookTextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { chatViewModel.setSearchQuery("") }) { Icon(Icons.Filled.Close, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(16.dp)) } } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().background(ScrapbookCream).padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when (val state = chatRoomsState) {
                is ChatUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThreeDotsAnimation()
                            Text("Loading chats...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                        }
                    }
                }
                is ChatUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("💬", fontSize = 56.sp)
                            Text("NO MESSAGES YET", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp)
                            Text("Start a conversation by tapping the\npencil icon above or messaging a user\nfrom their profile.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { onNewChat() }.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                Text("START A CHAT", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 18.sp)
                            }
                        }
                    }
                }
                is ChatUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, fontFamily = NunitoFontFamily, color = ScrapbookRed, fontSize = 14.sp)
                    }
                }
                is ChatUiState.Success -> {
                    val filtered = remember(state.rooms, searchQuery) {
                        if (searchQuery.isBlank()) state.rooms
                        else state.rooms.filter { room ->
                            chatViewModel.getChatDisplayName(room).contains(searchQuery, ignoreCase = true) ||
                                    room.lastMessage.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🔍", fontSize = 40.sp)
                                Text("No results for \"$searchQuery\"", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { room ->
                                ChatRoomItem(
                                    room = room,
                                    chatViewModel = chatViewModel,
                                    isOnline = onlineUsers.contains(chatViewModel.getOtherUid(room)),
                                    neonAlpha = neonAlpha,
                                    onClick = { onOpenChat(room) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Not logged in overlay
        if (currentUser == null) {
            Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔒", fontSize = 48.sp)
                    Text("SIGN IN TO CHAT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp)
                    Text("You need to be logged in to send and receive messages.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ChatRoomItem(
    room: ChatRoom,
    chatViewModel: ChatViewModel,
    isOnline: Boolean,
    neonAlpha: Float,
    onClick: () -> Unit
) {
    val displayName = chatViewModel.getChatDisplayName(room)
    val profilePic = chatViewModel.getChatProfilePic(room)
    val unread = room.unreadCounts[chatViewModel.currentUid] ?: 0
    val timeString = if (room.lastMessageTimestamp > 0L) {
        val now = System.currentTimeMillis()
        val diff = now - room.lastMessageTimestamp
        when {
            diff < 60_000L -> "now"
            diff < 3_600_000L -> "${diff / 60_000L}m"
            diff < 86_400_000L -> "${diff / 3_600_000L}h"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(room.lastMessageTimestamp))
        }
    } else ""

    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.97f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "roomScale")

    Box(modifier = Modifier.scale(cardScale)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth()
                .then(if (unread > 0) Modifier.border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(12.dp)) else Modifier)
                .clickable { pressed = true; onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Avatar with online dot
                Box(modifier = Modifier.size(54.dp)) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, if (unread > 0) ScrapbookYellowDark else ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                        if (room.type == "group") {
                            Text("👥", fontSize = 22.sp)
                        } else if (profilePic.isNotBlank()) {
                            AsyncImage(model = profilePic, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
                        }
                    }
                    // Online dot
                    if (isOnline && room.type == "dm") {
                        Box(modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).clip(CircleShape).background(ScrapbookGreen).border(2.dp, ScrapbookCardWhite, CircleShape))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (timeString.isNotBlank()) {
                            Text(timeString, fontFamily = NunitoFontFamily, color = if (unread > 0) ScrapbookYellowDark else ScrapbookTextMuted, fontSize = 11.sp, fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (room.lastMessageSenderId == chatViewModel.currentUid) {
                            Text("You: ", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp)
                        }
                        Text(
                            text = if (room.lastMessage.isBlank()) "No messages yet" else room.lastMessage,
                            fontFamily = NunitoFontFamily,
                            color = if (unread > 0) ScrapbookDark else ScrapbookTextMuted,
                            fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Typing indicator in list
                    if (room.typingUids.any { it != chatViewModel.currentUid }) {
                        Text("typing...", fontFamily = NunitoFontFamily, color = ScrapbookYellowDark, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }

                // Unread badge
                if (unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                        Text(if (unread > 9) "9+" else "$unread", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}
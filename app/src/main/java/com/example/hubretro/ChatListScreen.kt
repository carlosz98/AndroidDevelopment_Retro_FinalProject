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
import androidx.compose.ui.geometry.Offset
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

    val neonAlpha by rememberGlowRange(0.4f, 1f)

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            chatViewModel.setOnline()
            chatViewModel.listenToChatRooms()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(ComicGlassBg)) {
        HalftoneBackground(modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header (Discover style) ───────────────────────────────────────
            ComicPageHeader(title = "MESSAGES", subtitle = "Retro gaming chat", marquee = pageMarqueeFor("MESSAGES")) {
                ComicIconButton(
                    if (searchVisible) Icons.Filled.Close else Icons.Filled.Search, "Search"
                ) {
                    searchVisible = !searchVisible
                    if (!searchVisible) { chatViewModel.setSearchQuery(""); focusManager.clearFocus() }
                }
                Spacer(modifier = Modifier.width(8.dp))
                ComicIconButton(Icons.Filled.Edit, "New chat") { onNewChat() }
            }

            // ── Search bar ────────────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = searchVisible,
                enter = androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.shrinkVertically()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.55f))
                        .border(BorderStroke(1.5.dp, ScrapbookDark))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { chatViewModel.setSearchQuery(it) },
                        placeholder = {
                            Text("Search conversations...", fontFamily = NunitoFontFamily, fontSize = 13.sp,
                                color = ScrapbookDark.copy(alpha = 0.4f))
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = CGreen,
                                modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { chatViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = null,
                                        tint = ScrapbookDark.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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

            when (val state = chatRoomsState) {
                is ChatUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ThreeDotsAnimation(color = CGreenDeep)
                            Text("Loading chats...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                        }
                    }
                }
                is ChatUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NoSaveDataState(
                            title = "NO MESSAGES YET",
                            subtitle = "Start a conversation with the pencil icon above, or message a player from their profile.",
                            actionText = "START A CHAT",
                            onAction = { onNewChat() }
                        )
                    }
                }
                is ChatUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GameOverState(message = state.message, onRetry = { chatViewModel.listenToChatRooms() })
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
            Box(
                modifier = Modifier.fillMaxSize().background(ComicGlassBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔒", fontSize = 48.sp)
                    Text("SIGN IN TO CHAT", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp)
                    Text(
                        "You need to be logged in to send and receive messages.",
                        fontFamily = NunitoFontFamily, color = ScrapbookTextMuted,
                        fontSize = 14.sp, textAlign = TextAlign.Center
                    )
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

    val accentColor = if (unread > 0) CGreen else ScrapbookDark.copy(alpha = 0.3f)

    val glowAlpha by rememberGlowPhase(0.35f)
    var pressed by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(if (pressed) 3f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    val shadowOff by animateFloatAsState(if (pressed) 0f else 3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "shOff")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = pressAnim.dp)
    ) {
        Box(modifier = Modifier.matchParentSize().offset(x = shadowOff.dp, y = shadowOff.dp)
            .clip(RoundedCornerShape(16.dp)).background(ScrapbookDark.copy(alpha = 0.15f)))
        Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(16.dp))
            .background(CGreen.copy(alpha = glowAlpha * 0.22f)))
        AeroGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, ScrapbookDark, shape = RoundedCornerShape(16.dp))
                .clickable { pressed = true; onClick() },
            accentColor = accentColor,
            glowAlpha = if (unread > 0) 0.55f else 0.25f,
            cornerRadius = 16.dp,
            showOffsetShadow = false
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

                // Avatar with online ring
                Box(modifier = Modifier.size(54.dp)) {
                    // Glow ring for unread
                    if (unread > 0) {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(CGreen.copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )
                        )
                    }
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                            .align(Alignment.Center)
                            .background(Color.White.copy(alpha = 0.6f))
                            .border(
                                width = if (unread > 0) 2.dp else 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = if (unread > 0)
                                        listOf(CGreen.copy(alpha = neonAlpha), CGreenDeep)
                                    else
                                        listOf(ScrapbookDark, ScrapbookDark.copy(alpha = 0.6f))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (room.type == "group") {
                            Text("👥", fontSize = 22.sp)
                        } else if (profilePic.isNotBlank()) {
                            AsyncImage(
                                model = profilePic, contentDescription = null,
                                contentScale = ContentScale.Crop, modifier = Modifier.halftoneReveal(profilePic).fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null,
                                tint = ScrapbookDark.copy(alpha = 0.45f), modifier = Modifier.size(24.dp))
                        }
                    }
                    // Online dot
                    if (isOnline && room.type == "dm") {
                        Box(
                            modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(CGreen)
                                .border(2.dp, ScrapbookDark, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            displayName.uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (timeString.isNotBlank()) {
                            Text(
                                timeString,
                                fontFamily = NunitoFontFamily,
                                color = if (unread > 0) CGreenDeep else ScrapbookTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                    if (room.typingUids.any { it != chatViewModel.currentUid }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ThreeDotsAnimation(color = CGreenDeep, dotSize = 4.dp)
                            Text("typing", fontFamily = NunitoFontFamily, color = CGreenDeep,
                                fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }

                // Unread badge — pulsing
                if (unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    val badgePulse = rememberInfiniteTransition(label = "badgePulse")
                    val badgeScale by badgePulse.animateFloat(
                        initialValue = 1f, targetValue = 1.18f,
                        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "badgeScaleAnim"
                    )
                    val badgeGlow by badgePulse.animateFloat(
                        initialValue = 0.5f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                        label = "badgeGlowAnim"
                    )
                    Box(
                        modifier = Modifier.scale(badgeScale)
                            .size(26.dp).clip(CircleShape)
                            .background(CGreen)
                            .border(2.dp, CGreen.copy(alpha = badgeGlow), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (unread > 9) "9+" else "$unread",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = if (unread > 9) 9.sp else 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(150); pressed = false } }
}

package com.example.hubretro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) chatViewModel.listenToChatRooms()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookYellow)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MESSAGES",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    // New chat button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ScrapbookDark)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { onNewChat() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "New chat",
                            tint = ScrapbookYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            when (val state = chatRoomsState) {
                is ChatUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ScrapbookYellowDark,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                is ChatUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("💬", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO MESSAGES YET",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start a conversation by tapping the\npencil icon above or messaging a user\nfrom their profile.",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ScrapbookDark)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .clickable { onNewChat() }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "START A CHAT",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                is ChatUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookRed,
                            fontSize = 14.sp
                        )
                    }
                }

                is ChatUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 12.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.rooms, key = { it.id }) { room ->
                            ChatRoomItem(
                                room = room,
                                chatViewModel = chatViewModel,
                                onClick = { onOpenChat(room) }
                            )
                        }
                    }
                }
            }
        }

        // Must be logged in
        if (currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrapbookCream),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🔒", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SIGN IN TO CHAT",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You need to be logged in to send and receive messages.",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            else -> SimpleDateFormat("MMM d", Locale.getDefault())
                .format(Date(room.lastMessageTimestamp))
        }
    } else ""

    Box {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar or group icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (room.type == "group") {
                        Text("👥", fontSize = 22.sp)
                    } else if (profilePic.isNotBlank()) {
                        AsyncImage(
                            model = profilePic,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = ScrapbookDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
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
                            text = displayName.uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (timeString.isNotBlank()) {
                            Text(
                                text = timeString,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (room.lastMessage.isBlank()) "No messages yet"
                        else room.lastMessage,
                        fontFamily = NunitoFontFamily,
                        color = if (unread > 0) ScrapbookDark
                        else ScrapbookTextMuted,
                        fontWeight = if (unread > 0) FontWeight.Bold
                        else FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Unread badge
                if (unread > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unread > 9) "9+" else "$unread",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
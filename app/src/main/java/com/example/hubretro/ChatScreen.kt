package com.example.hubretro

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

val chatReactionEmojis = listOf("❤️", "😂", "😮", "😢", "👍", "🔥")

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
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var showImageWarning by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var reactionTargetId by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingImageUri = it
            showImageWarning = true
        }
    }

    LaunchedEffect(chatRoom.id) {
        chatViewModel.listenToMessages(chatRoom.id)
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    DisposableEffect(Unit) {
        onDispose { chatViewModel.clearMessages() }
    }

    Box(
        modifier = Modifier
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
                    .padding(top = 16.dp, bottom = 12.dp, start = 4.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ScrapbookDark
                        )
                    }

                    // Avatar
                    val pic = chatViewModel.getChatProfilePic(chatRoom)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chatRoom.type == "group") {
                            Text("👥", fontSize = 16.sp)
                        } else if (pic.isNotBlank()) {
                            AsyncImage(
                                model = pic,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = ScrapbookDark.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chatViewModel.getChatDisplayName(chatRoom).uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chatRoom.type == "group") {
                            Text(
                                text = "${chatRoom.memberUids.size} members",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookDark.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp,
                    top = 12.dp, bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isMe = message.senderId == chatViewModel.currentUid
                    ChatMessageBubble(
                        message = message,
                        isMe = isMe,
                        showReactions = reactionTargetId == message.id,
                        onLongPress = {
                            reactionTargetId =
                                if (reactionTargetId == message.id) null else message.id
                        },
                        onReact = { emoji ->
                            chatViewModel.toggleReaction(chatRoom.id, message.id, emoji)
                            reactionTargetId = null
                        }
                    )
                }
            }

            // Input bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCardWhite)
                    .border(BorderStroke(2.dp, ScrapbookBorder))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Image button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                color = ScrapbookYellowDark,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Send image",
                                tint = ScrapbookDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Text input
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = {
                            Text(
                                "Type a message...",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = NunitoFontFamily,
                            fontSize = 14.sp,
                            color = ScrapbookTextDark
                        ),
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
                                chatViewModel.sendMessage(chatRoom.id, messageText, profile)
                                messageText = ""
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (messageText.isNotBlank()) ScrapbookDark
                                else ScrapbookPaper
                            )
                            .border(2.dp, ScrapbookBorder, CircleShape)
                            .clickable(enabled = messageText.isNotBlank()) {
                                chatViewModel.sendMessage(chatRoom.id, messageText, profile)
                                messageText = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) ScrapbookYellow
                            else ScrapbookTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Image safety warning dialog
        if (showImageWarning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                ScrapbookCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 16.dp,
                    shadowOffset = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "SHARE IMAGE?",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Images you send will be blurred for the recipient until they choose to reveal them.\n\nMake sure you trust this person before sharing any images.",
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ScrapbookPaper)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        showImageWarning = false
                                        pendingImageUri = null
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "CANCEL",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 16.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ScrapbookDark)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        showImageWarning = false
                                        pendingImageUri?.let { uri ->
                                            chatViewModel.sendImage(chatRoom.id, uri, profile)
                                        }
                                        pendingImageUri = null
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "SEND",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookYellow,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showReactions: Boolean,
    onLongPress: () -> Unit,
    onReact: (String) -> Unit
) {
    var revealedImage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for other users)
        if (!isMe && message.senderUsername.isNotBlank()) {
            Text(
                text = message.senderUsername,
                fontFamily = BangersFontFamily,
                color = ScrapbookTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 46.dp, bottom = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            // Avatar for other users
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ScrapbookPaper)
                        .border(1.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (message.senderProfilePicUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.senderProfilePicUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = ScrapbookDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column {
                // Bubble
                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .background(
                            if (isMe) ScrapbookDark else ScrapbookCardWhite
                        )
                        .border(
                            2.dp,
                            ScrapbookBorder,
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .clickable { onLongPress() }
                        .padding(
                            horizontal = if (message.imageUrl != null) 0.dp else 12.dp,
                            vertical = if (message.imageUrl != null) 0.dp else 10.dp
                        )
                ) {
                    if (message.imageUrl != null) {
                        // Blurred image with reveal
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (!revealedImage && !isMe)
                                            Modifier.blur(20.dp)
                                        else Modifier
                                    )
                            )
                            // Blur overlay for received images
                            if (!revealedImage && !isMe) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clickable { revealedImage = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("⚠️", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "TAP TO REVEAL",
                                            fontFamily = BangersFontFamily,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Be sure you trust\nthis person",
                                            fontFamily = NunitoFontFamily,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = message.text,
                            fontFamily = NunitoFontFamily,
                            color = if (isMe) ScrapbookYellow else ScrapbookTextDark,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Timestamp
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(message.timestamp)),
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(
                        start = if (isMe) 0.dp else 4.dp,
                        end = if (isMe) 4.dp else 0.dp,
                        top = 2.dp
                    )
                )

                // Reactions display
                val allReactions = message.reactions.filter { it.value.isNotEmpty() }
                if (allReactions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(allReactions.entries.toList()) { (emoji, uids) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (uids.contains(chatViewModel.currentUid))
                                            ScrapbookYellow
                                        else ScrapbookPaper
                                    )
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$emoji ${uids.size}",
                                    fontSize = 12.sp,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reaction picker
        AnimatedVisibility(
            visible = showReactions,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.padding(
                    start = if (isMe) 0.dp else 38.dp,
                    top = 4.dp
                )
            ) {
                ScrapbookCard(
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 20.dp,
                    shadowOffset = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chatReactionEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clickable { onReact(emoji) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Make chatViewModel accessible in bubble
private lateinit var chatViewModel: ChatViewModel

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    showReactions: Boolean,
    onLongPress: () -> Unit,
    onReact: (String) -> Unit,
    currentUid: String
) {
    var revealedImage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe && message.senderUsername.isNotBlank()) {
            Text(
                text = message.senderUsername,
                fontFamily = BangersFontFamily,
                color = ScrapbookTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 46.dp, bottom = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ScrapbookPaper)
                        .border(1.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (message.senderProfilePicUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.senderProfilePicUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = ScrapbookDark.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column {
                Box(
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .background(if (isMe) ScrapbookDark else ScrapbookCardWhite)
                        .border(
                            2.dp, ScrapbookBorder,
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            )
                        )
                        .clickable { onLongPress() }
                        .padding(
                            horizontal = if (message.imageUrl != null) 0.dp else 12.dp,
                            vertical = if (message.imageUrl != null) 0.dp else 10.dp
                        )
                ) {
                    if (message.imageUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (!revealedImage && !isMe)
                                            Modifier.blur(20.dp)
                                        else Modifier
                                    )
                            )
                            if (!revealedImage && !isMe) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clickable { revealedImage = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("⚠️", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "TAP TO REVEAL",
                                            fontFamily = BangersFontFamily,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Be sure you trust\nthis person",
                                            fontFamily = NunitoFontFamily,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = message.text,
                            fontFamily = NunitoFontFamily,
                            color = if (isMe) ScrapbookYellow else ScrapbookTextDark,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(message.timestamp)),
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(
                        start = if (isMe) 0.dp else 4.dp,
                        end = if (isMe) 4.dp else 0.dp,
                        top = 2.dp
                    )
                )

                val allReactions = message.reactions.filter { it.value.isNotEmpty() }
                if (allReactions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        items(allReactions.entries.toList()) { (emoji, uids) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (uids.contains(currentUid)) ScrapbookYellow
                                        else ScrapbookPaper
                                    )
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$emoji ${uids.size}",
                                    fontSize = 12.sp,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookDark
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showReactions, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.padding(start = if (isMe) 0.dp else 38.dp, top = 4.dp)) {
                ScrapbookCard(
                    backgroundColor = ScrapbookCardWhite,
                    cornerRadius = 20.dp,
                    shadowOffset = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chatReactionEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 22.sp,
                                modifier = Modifier.clickable { onReact(emoji) }.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
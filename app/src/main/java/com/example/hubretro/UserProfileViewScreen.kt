package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun UserProfileViewScreen(
    user: UserProfileData,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel? = null,
    onOpenChat: ((ChatRoom) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val followingUids by authViewModel.followingUids.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val myProfile by authViewModel.userProfile.collectAsState()
    val isFollowing = followingUids.contains(user.uid)
    val isCurrentUser = user.uid == currentUser?.uid

    var userActivities by remember { mutableStateOf<List<ActivityEntry>>(emptyList()) }
    var isStartingChat by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid) {
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
                .collection("activity")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5).get().await()
            userActivities = docs.documents.mapNotNull { doc ->
                ActivityEntry(
                    id = doc.id,
                    type = doc.getString("type") ?: "",
                    description = doc.getString("description") ?: "",
                    itemTitle = doc.getString("itemTitle") ?: "",
                    itemCategory = doc.getString("itemCategory") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            }
        } catch (e: Exception) { }
    }

    val displayGames: List<Game> = remember(user) {
        user.topGames.map { gameMap ->
            Game(
                name = gameMap["name"] as? String ?: "",
                coverUrl = (gameMap["coverUrl"] as? String)?.ifBlank { null }
            )
        }.filter { it.name.isNotBlank() }
    }

    val displaySoundtracks: List<Soundtrack> = remember(user) {
        user.topSoundtracks.map { stMap ->
            Soundtrack(
                title = stMap["name"] as? String ?: "",
                artist = (stMap["gameName"] as? String)?.ifBlank { null },
                coverUrl = (stMap["coverUrl"] as? String)?.ifBlank { null }
            )
        }.filter { it.title.isNotBlank() }
    }

    val profilePicSize = 100.dp
    val bannerHeight = 160.dp

    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ScrapbookCream)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                item {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                    ) {
                        if (!user.bannerUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.bannerUrl,
                                contentDescription = "Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(ScrapbookYellow, ScrapbookPaper)
                                        )
                                    )
                            )
                        }
                        // Back button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 40.dp, start = 8.dp)
                                .clip(CircleShape)
                                .background(ScrapbookYellow)
                                .border(2.dp, ScrapbookBorder, CircleShape)
                                .clickable { onBack() }
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ScrapbookDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Profile pic overlapping banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = -(profilePicSize / 2)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(profilePicSize)
                                .clip(CircleShape)
                                .background(ScrapbookCardWhite)
                                .border(4.dp, ScrapbookCardWhite, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user.profilePictureUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = ScrapbookDark.copy(alpha = 0.4f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    // Username / Handle / Stats
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = -(profilePicSize / 2))
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user.username.uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 32.sp,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                        if (user.userHandle.isNotBlank()) {
                            Text(
                                text = user.userHandle,
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Followers / Following
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatCount(user.followersCount),
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "FOLLOWERS",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(40.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatCount(user.followingCount),
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 24.sp
                                )
                                Text(
                                    text = "FOLLOWING",
                                    fontFamily = NunitoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ScrapbookTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Follow + Message buttons
                        if (!isCurrentUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Follow button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isFollowing) ScrapbookPaper else ScrapbookDark
                                        )
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            if (isFollowing) authViewModel.unfollowUser(user.uid)
                                            else authViewModel.followUser(user.uid)
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                                        fontFamily = BangersFontFamily,
                                        color = if (isFollowing) ScrapbookDark else ScrapbookYellow,
                                        fontSize = 18.sp
                                    )
                                }

                                // Message button
                                if (chatViewModel != null && onOpenChat != null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ScrapbookYellow)
                                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(12.dp))
                                            .clickable(enabled = !isStartingChat) {
                                                isStartingChat = true
                                                chatViewModel.getOrCreateDm(
                                                    otherUser = user,
                                                    myProfile = myProfile?.let {
                                                        UserProfileData(
                                                            uid = currentUser?.uid ?: "",
                                                            username = it.username,
                                                            profilePictureUrl = it.profilePictureUrl ?: "",
                                                            userHandle = it.userHandle,
                                                            bio = it.bio,
                                                            email = it.email,
                                                            followersCount = it.followersCount,
                                                            followingCount = it.followingCount,
                                                            setupComplete = it.setupComplete,
                                                            bannerUrl = it.bannerUrl,
                                                            topGames = it.topGames,
                                                            topSoundtracks = it.topSoundtracks
                                                        )
                                                    },
                                                    onResult = { chatId ->
                                                        val state = chatViewModel.chatRooms.value
                                                        val room = if (state is ChatUiState.Success) {
                                                            state.rooms.firstOrNull { it.id == chatId }
                                                        } else null
                                                        room?.let { onOpenChat(it) }
                                                        isStartingChat = false
                                                    }
                                                )
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isStartingChat) {
                                            CircularProgressIndicator(
                                                color = ScrapbookDark,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Chat,
                                                    contentDescription = null,
                                                    tint = ScrapbookDark,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "MESSAGE",
                                                    fontFamily = BangersFontFamily,
                                                    color = ScrapbookDark,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bio
                    if (user.bio.isNotBlank()) {
                        ScrapbookSectionHeader(title = "ABOUT", emoji = "👤")
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ScrapbookCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = ScrapbookCardWhite,
                                cornerRadius = 12.dp
                            ) {
                                Text(
                                    text = user.bio,
                                    fontFamily = NunitoFontFamily,
                                    color = ScrapbookTextDark,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Top 6 Games
                if (displayGames.isNotEmpty()) {
                    item {
                        ScrapbookSectionHeader(title = "MY TOP 6 GAMES", emoji = "🎮")
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().height(750.dp)
                        ) {
                            items(displayGames.take(6)) { game -> GameItem(game = game) }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Top 3 Soundtracks
                if (displaySoundtracks.isNotEmpty()) {
                    item {
                        ScrapbookSectionHeader(title = "MY TOP 3 SOUNDTRACKS", emoji = "🎵")
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(displaySoundtracks.take(3)) { soundtrack ->
                                SoundtrackItem(soundtrack = soundtrack)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Recent Activity
                item {
                    ScrapbookSectionHeader(title = "RECENT ACTIVITY", emoji = "⚡")
                    if (userActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No activity yet",
                                fontFamily = NunitoFontFamily,
                                color = ScrapbookTextMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            userActivities.forEach { entry ->
                                Box {
                                    ScrapbookCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        backgroundColor = ScrapbookCardWhite,
                                        cornerRadius = 10.dp,
                                        shadowOffset = 3.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(ScrapbookPaper)
                                                    .border(2.dp, ScrapbookBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!user.profilePictureUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = user.profilePictureUrl,
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (entry.type == "ARTICLE")
                                                            Icons.Filled.Create
                                                        else Icons.Filled.Bookmark,
                                                        contentDescription = null,
                                                        tint = ScrapbookDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = entry.description,
                                                        fontFamily = NunitoFontFamily,
                                                        color = ScrapbookTextDark,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = entry.timeAgoString(),
                                                    fontFamily = NunitoFontFamily,
                                                    color = ScrapbookTextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
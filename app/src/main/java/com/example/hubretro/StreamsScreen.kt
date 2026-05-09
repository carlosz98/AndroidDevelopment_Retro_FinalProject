package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*

// Brand colors kept only for Twitch/YouTube logos
val TwitchPurple = Color(0xFF9146FF)
val YouTubeRed = Color(0xFFFF0000)

@Composable
fun StreamsScreen(
    modifier: Modifier = Modifier,
    streamsViewModel: StreamsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val twitchState by streamsViewModel.twitchStreams.collectAsState()
    val youtubeState by streamsViewModel.youtubeVideos.collectAsState()
    val communityStreamers by streamsViewModel.communityStreamers.collectAsState()
    val allUsers by authViewModel.allUsers.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🔴 LIVE", "🎬 VIDEOS", "👥 COMMUNITY")

    LaunchedEffect(allUsers) {
        streamsViewModel.loadCommunityStreamers(allUsers)
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchAllUsers()
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
                        text = "STREAMS & VIDEOS",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { streamsViewModel.fetchAll() }) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = ScrapbookDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScrapbookCream)
                    .border(BorderStroke(1.dp, ScrapbookBorder.copy(alpha = 0.3f)))
            ) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == index) ScrapbookYellow
                                else ScrapbookCardWhite
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (index < tabs.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(ScrapbookBorder)
                        )
                    }
                }
            }

            // Content
            when (selectedTab) {
                0 -> TwitchStreamsTab(state = twitchState)
                1 -> YouTubeVideosTab(state = youtubeState)
                2 -> CommunityStreamersTab(streamers = communityStreamers)
            }
        }
    }
}

// --- Twitch Streams Tab ---
@Composable
fun TwitchStreamsTab(state: StreamsState) {
    val context = LocalContext.current
    when (state) {
        is StreamsState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TwitchPurple, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading live streams...",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
        is StreamsState.Error -> {
            ErrorState(message = state.message)
        }
        is StreamsState.Empty -> {
            EmptyState(
                emoji = "📺",
                title = "NO LIVE STREAMS",
                subtitle = "No retro gaming streams live right now.\nCheck back later!"
            )
        }
        is StreamsState.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            val streams = state.data as List<TwitchStream>
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 12.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${streams.size} LIVE ON TWITCH",
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 18.sp
                        )
                    }
                }
                items(streams, key = { it.id }) { stream ->
                    TwitchStreamCard(stream = stream, onClick = {
                        val url = "https://www.twitch.tv/${stream.userName}"
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        )
                    })
                }
            }
        }
    }
}

// --- YouTube Videos Tab ---
@Composable
fun YouTubeVideosTab(state: StreamsState) {
    val context = LocalContext.current
    when (state) {
        is StreamsState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = YouTubeRed, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading videos...",
                        fontFamily = NunitoFontFamily,
                        color = ScrapbookTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
        is StreamsState.Error -> {
            ErrorState(message = state.message)
        }
        is StreamsState.Empty -> {
            EmptyState(
                emoji = "🎬",
                title = "NO VIDEOS FOUND",
                subtitle = "Could not load retro gaming videos.\nCheck back later!"
            )
        }
        is StreamsState.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            val videos = state.data as List<YouTubeVideo>
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 12.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "🎬 RETRO GAMING VIDEOS",
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(videos, key = { it.id }) { video ->
                    YouTubeVideoCard(video = video, onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(video.videoUrl))
                        )
                    })
                }
            }
        }
    }
}

// --- Community Streamers Tab ---
@Composable
fun CommunityStreamersTab(streamers: List<CommunityStreamer>) {
    val context = LocalContext.current
    if (streamers.isEmpty()) {
        EmptyState(
            emoji = "👥",
            title = "NO COMMUNITY STREAMERS",
            subtitle = "No RetroHub members have linked their\nTwitch or YouTube yet.\nAdd yours in your profile!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = 12.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "RETROHUB STREAMERS",
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(streamers, key = { it.uid }) { streamer ->
                CommunityStreamerCard(
                    streamer = streamer,
                    onTwitchClick = {
                        if (streamer.twitchUsername.isNotBlank()) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.twitch.tv/${streamer.twitchUsername}")
                                )
                            )
                        }
                    },
                    onYouTubeClick = {
                        if (streamer.youtubeUsername.isNotBlank()) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/@${streamer.youtubeUsername}")
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

// --- Twitch Stream Card ---
@Composable
fun TwitchStreamCard(stream: TwitchStream, onClick: () -> Unit) {
    Box {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(ScrapbookDark)
                ) {
                    AsyncImage(
                        model = stream.thumbnailUrl,
                        contentDescription = stream.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // LIVE badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Red)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "🔴 LIVE",
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                    // Viewer count
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${formatViewerCount(stream.viewerCount)} viewers",
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    // Twitch badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TwitchPurple)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "TWITCH",
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
                // Info
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stream.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stream.userName,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TwitchPurple,
                            fontSize = 13.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ScrapbookYellow)
                                .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stream.gameName,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookDark,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- YouTube Video Card ---
@Composable
fun YouTubeVideoCard(video: YouTubeVideo, onClick: () -> Unit) {
    Box {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(ScrapbookDark)
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Play button overlay
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color.White, fontSize = 20.sp)
                    }
                    // YouTube badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(YouTubeRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "YOUTUBE",
                            fontFamily = BangersFontFamily,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    // Date badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = video.publishedAt,
                            fontFamily = NunitoFontFamily,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
                // Info
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = video.title,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.channelTitle,
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = YouTubeRed,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// --- Community Streamer Card ---
@Composable
fun CommunityStreamerCard(
    streamer: CommunityStreamer,
    onTwitchClick: () -> Unit,
    onYouTubeClick: () -> Unit
) {
    Box {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ScrapbookPaper)
                        .border(2.dp, ScrapbookBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (streamer.profilePicUrl.isNotBlank()) {
                        AsyncImage(
                            model = streamer.profilePicUrl,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = streamer.username.uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (streamer.isLive) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Red)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "🔴 LIVE",
                                    fontFamily = BangersFontFamily,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (streamer.twitchUsername.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TwitchPurple)
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                    .clickable { onTwitchClick() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "TWITCH",
                                    fontFamily = BangersFontFamily,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (streamer.youtubeUsername.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(YouTubeRed)
                                    .border(1.dp, ScrapbookBorder, RoundedCornerShape(6.dp))
                                    .clickable { onYouTubeClick() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "YOUTUBE",
                                    fontFamily = BangersFontFamily,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Shared empty/error states ---
@Composable
fun EmptyState(emoji: String, title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(emoji, fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontFamily = NunitoFontFamily,
                color = ScrapbookTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontFamily = NunitoFontFamily,
                color = ScrapbookRed,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun formatViewerCount(count: Int): String = when {
    count >= 1000 -> "${count / 1000}K"
    else -> count.toString()
}
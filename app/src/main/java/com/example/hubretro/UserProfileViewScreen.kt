package com.example.hubretro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserProfileViewScreen(
    user: UserProfileData,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val followingUids by authViewModel.followingUids.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isFollowing = followingUids.contains(user.uid)
    val isCurrentUser = user.uid == currentUser?.uid

    // Fetch user's recent activity from Firestore
    var userActivities by remember { mutableStateOf<List<ActivityEntry>>(emptyList()) }
    LaunchedEffect(user.uid) {
        try {
            val docs = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("activity")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
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

    // Convert Firebase games/soundtracks
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
        Box(modifier = modifier.fillMaxSize()) {
            // Background
            Image(
                painter = painterResource(id = R.drawable.my_retro_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // --- Banner + Profile Picture ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                    ) {
                        // Banner
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
                                            colors = listOf(
                                                Color(0xFF1A1A2E),
                                                Color(0xFF2A1A3E)
                                            )
                                        )
                                    )
                            )
                        }

                        // Back button
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 40.dp, start = 8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        // Profile picture
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = profilePicSize / 2)
                                .size(profilePicSize)
                                .clip(CircleShape)
                                .background(RetroDarkPurple)
                                .border(3.dp, VaporwavePink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!user.profilePictureUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = RetroTextOffWhite.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height((profilePicSize / 2) + 12.dp))

                    // --- Username / Handle ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user.username.uppercase(),
                            style = TextStyle(
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                shadow = Shadow(
                                    color = VaporwavePink.copy(alpha = 0.7f),
                                    offset = Offset(3f, 3f),
                                    blurRadius = 5f
                                ),
                                textAlign = TextAlign.Center
                            )
                        )

                        if (user.userHandle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.userHandle,
                                style = TextStyle(
                                    fontFamily = RetroFontFamily,
                                    color = RetroTextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Followers / Following counts ---
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatCount(user.followersCount),
                                    style = TextStyle(
                                        fontFamily = RetroFontFamily,
                                        color = RetroTextOffWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "FOLLOWERS",
                                    style = TextStyle(
                                        fontFamily = RetroFontFamily,
                                        color = VaporwavePink,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(32.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatCount(user.followingCount),
                                    style = TextStyle(
                                        fontFamily = RetroFontFamily,
                                        color = RetroTextOffWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "FOLLOWING",
                                    style = TextStyle(
                                        fontFamily = RetroFontFamily,
                                        color = VaporwaveCyan,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Follow / Unfollow button ---
                        if (!isCurrentUser) {
                            Button(
                                onClick = {
                                    if (isFollowing) authViewModel.unfollowUser(user.uid)
                                    else authViewModel.followUser(user.uid)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color.Transparent
                                    else VaporwavePink
                                ),
                                border = if (isFollowing)
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp, VaporwaveCyan
                                    ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp)
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = if (isFollowing) "FOLLOWING" else "FOLLOW",
                                    fontFamily = RetroFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isFollowing) VaporwaveCyan else Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Bio ---
                    if (user.bio.isNotBlank()) {
                        ProfileSectionTitle("ABOUT")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = RetroDarkPurple.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = user.bio,
                                style = TextStyle(
                                    fontFamily = RetroFontFamily,
                                    color = RetroTextOffWhite,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // --- Top 6 Games ---
                if (displayGames.isNotEmpty()) {
                    item {
                        ProfileSectionTitle("TOP 6 GAMES")
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(750.dp)
                        ) {
                            items(displayGames.take(6)) { game ->
                                GameItem(game = game)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // --- Top 3 Soundtracks ---
                if (displaySoundtracks.isNotEmpty()) {
                    item {
                        ProfileSectionTitle("TOP 3 SOUNDTRACKS")
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp),
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

                // --- Recent Activity ---
                item {
                    ProfileSectionTitle("RECENT ACTIVITY")
                    if (userActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No activity yet",
                                style = TextStyle(
                                    fontFamily = RetroFontFamily,
                                    color = RetroTextOffWhite.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
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
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = RetroDarkPurple.copy(alpha = 0.6f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(RetroDarkPurple)
                                                .border(
                                                    1.dp,
                                                    VaporwavePink.copy(alpha = 0.5f),
                                                    CircleShape
                                                ),
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
                                                    tint = RetroTextOffWhite.copy(alpha = 0.5f),
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
                                                    tint = if (entry.type == "ARTICLE")
                                                        VaporwavePink else VaporwaveCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = entry.description,
                                                    style = TextStyle(
                                                        fontFamily = RetroFontFamily,
                                                        color = RetroTextOffWhite,
                                                        fontSize = 12.sp,
                                                        lineHeight = 16.sp
                                                    ),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = entry.timeAgoString(),
                                                style = TextStyle(
                                                    fontFamily = RetroFontFamily,
                                                    color = RetroTextSecondary.copy(alpha = 0.7f),
                                                    fontSize = 10.sp
                                                )
                                            )
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
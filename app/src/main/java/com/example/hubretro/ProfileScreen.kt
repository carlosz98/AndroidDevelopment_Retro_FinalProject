package com.example.hubretro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hubretro.R
import com.example.hubretro.ui.theme.*
import android.util.Log

// Data classes
data class Game(
    val name: String,
    val imageResId: Int
)

data class Soundtrack(
    val title: String,
    val artist: String? = null,
    val imageResId: Int
)

// Updated Data class for Activity Feed
data class ActivityItem(
    val description: String,
    val timeAgo: String,
    val userProfilePicResId: Int? = null,
    val articleSnippet: String? = null // Optional field for article snippet
)

data class UserProfile(
    val username: String = "Don Carlos",
    val userHandle: String = "@logodzip",
    val bio: String = "Que onda, soy carlos y esta es mi aplicacion, es solamente el draft pero de momento me gusta como va quedando",
    val profilePictureResId: Int = R.drawable.profile1,
    val bannerImageResId: Int = R.drawable.banner1,
    val followersCount: Int = 1234,
    val followingCount: Int = 150,
    val topGames: List<Game> = listOf(
        Game("Fez", R.drawable.game1),
        Game("Final Fantasy XIII", R.drawable.game2),
        Game("Final Fantasy X", R.drawable.game3),
        Game("Infamous Second Son", R.drawable.game4),
        Game("Minecraft", R.drawable.game5),
        Game("Cyberpunk 2077", R.drawable.game6)
    ),
    val topSoundtracks: List<Soundtrack> = listOf(
        Soundtrack("Minecraft OST", "C418", R.drawable.vinyl1),
        Soundtrack("The Sims OST", "EA", R.drawable.vinyl2),
        Soundtrack("Undertale OST", "Toby Fox", R.drawable.vinyl3)
    ),
    val recentActivities: List<ActivityItem> = listOf(
        ActivityItem(
            description = "Wrote an article: \"The Pixelated Pull: Why Retro Gaming is Booming Again\"",
            timeAgo = "Nov 15, 2023",
            userProfilePicResId = R.drawable.profile1,
            articleSnippet = "Beyond nostalgia, discover the reasons for the resurgence of classic video games and their timeless appeal in a modern world." // Snippet added
        ),
        ActivityItem(
            description = "Wrote a passionate article about the enduring magic of the SNES era and its impact on modern indie games. Check it out on the main feed!",
            timeAgo = "3 hours ago",
            userProfilePicResId = R.drawable.profile1
            // No snippet for this one
        ),
        ActivityItem(
            description = "Just beat Fez for the 5th time. Still a masterpiece! #indiegames #fez",
            timeAgo = "1 day ago",
            userProfilePicResId = R.drawable.profile1
        ),
        ActivityItem(
            description = "Shared a link to a cool retro gaming documentary.",
            timeAgo = "2 days ago",
            userProfilePicResId = R.drawable.profile1
        )
    )
)

// Helper function to format counts
fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0).replace(".0M", "M")
        count >= 1000 -> String.format("%.1fK", count / 1000.0).replace(".0K", "K")
        else -> count.toString()
    }
}

@Composable
fun UserStatItem(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = formatCount(count),
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextSecondary,
                fontSize = 12.sp
            )
        )
    }
}


@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val userProfile = UserProfile()
    val profilePicSize = 120.dp
    val bannerHeight = 180.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
        ) {
            Image(
                painter = painterResource(id = userProfile.bannerImageResId),
                contentDescription = "${userProfile.username}'s profile banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painterResource(id = userProfile.profilePictureResId),
                contentDescription = "${userProfile.username} profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = profilePicSize / 2)
                    .size(profilePicSize)
                    .clip(CircleShape)
                    .background(RetroAccentPurple.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.height((profilePicSize / 2) + 16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = userProfile.username.uppercase(),
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = VaporwavePink.copy(alpha = 0.7f),
                        offset = Offset(x = 3f, y = 3f),
                        blurRadius = 5f
                    ),
                    textAlign = TextAlign.Center
                )
            )
            if (userProfile.userHandle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userProfile.userHandle,
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(
                            color = RetroAccentBlue.copy(alpha = 0.4f),
                            offset = Offset(x = 1f, y = 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserStatItem(count = userProfile.followersCount, label = "Followers")
                Spacer(modifier = Modifier.width(32.dp))
                UserStatItem(count = userProfile.followingCount, label = "Following")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileSectionTitle("ABOUT ME")
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                BioCard(bioText = userProfile.bio)
            }

            Spacer(modifier = Modifier.height(20.dp))
            TopGamesSection(games = userProfile.topGames)

            Spacer(modifier = Modifier.height(20.dp))
            TopSoundtracksSection(soundtracks = userProfile.topSoundtracks)

            Spacer(modifier = Modifier.height(20.dp))
            RecentActivitySection(activities = userProfile.recentActivities) // New section
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GameItem(game: Game, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = game.imageResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = Float.POSITIVE_INFINITY,
                            endY = 0f
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black),
                            startY = 600f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Text(
                text = game.name.uppercase(),
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(1f, 1f), 2f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun TopGamesSection(games: List<Game>) {
    if (games.isNotEmpty()) {
        ProfileSectionTitle("MY TOP 6 GAMES")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(750.dp) // ADJUST MANUALLY
        ) {
            items(games.take(6)) { game ->
                GameItem(game = game)
            }
        }
    }
}

@Composable
fun SoundtrackItem(soundtrack: Soundtrack, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(150.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Image(
                painter = painterResource(id = soundtrack.imageResId),
                contentDescription = soundtrack.title,
                modifier = Modifier
                    .size(140.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = soundtrack.title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        soundtrack.artist?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = RetroTextSecondary.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TopSoundtracksSection(soundtracks: List<Soundtrack>) {
    if (soundtracks.isNotEmpty()) {
        ProfileSectionTitle("MY TOP 3 SOUNDTRACKS")
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(soundtracks.take(3)) { soundtrack ->
                SoundtrackItem(soundtrack = soundtrack)
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = TextStyle(
            fontFamily = RetroFontFamily,
            color = VaporwaveTeal,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            shadow = Shadow(
                color = RetroAccentBlue.copy(alpha = 0.5f),
                offset = Offset(x = 2f, y = 2f),
                blurRadius = 3f
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun BioCard(bioText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroBackgroundAlt.copy(alpha = 0.75f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = bioText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = RetroTextOffWhite,
                fontFamily = RetroFontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

// Updated Composable for individual activity items
@Composable
fun ActivityFeedItem(activity: ActivityItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroBackgroundAlt.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            activity.userProfilePicResId?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = "User avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = RetroTextOffWhite,
                        fontFamily = RetroFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                )
                // Display Article Snippet if available
                activity.articleSnippet?.let { snippet ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = snippet,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = RetroTextSecondary.copy(alpha = 0.9f),
                            fontFamily = RetroFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    // VVVVVV NEW: "See more..." Text for articles VVVVVV
                    Spacer(modifier = Modifier.height(4.dp)) // Small space before "See more..."
                    Text(
                        text = "See more...",
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = VaporwaveTeal, // Make it stand out, like other clickable elements
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable {
                                // TODO: Implement navigation to the full article view.
                                // You'll likely need an article ID from the 'activity' item
                                // (which we might need to add to ActivityItem if it's not just derived from description)
                                // and a NavController to navigate.
                                Log.d("ActivityFeed", "See more clicked for: ${activity.description}")
                            }
                            .padding(top = 2.dp) // นิดหน่อย padding to ensure clickable area is good
                    )
                    // ^^^^^^ END OF NEW "See more..." Text ^^^^^^
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.timeAgo,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = RetroTextSecondary.copy(alpha = 0.8f),
                        fontFamily = RetroFontFamily,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}


// New Composable for the Recent Activity section
@Composable
fun RecentActivitySection(activities: List<ActivityItem>) {
    if (activities.isNotEmpty()) {
        ProfileSectionTitle("RECENT ACTIVITY")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            activities.take(3).forEach { activity -> // Show up to 3 activities
                ActivityFeedItem(activity = activity)
            }
            if (activities.size > 3) { // Show "View all" if there are more than 3 activities
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "View all activity...",
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = VaporwaveTeal,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: Implement navigation to full activity feed */ }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ProfileScreenPreview() {
    HubRetroTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RetroBackground ?: Color.DarkGray)
        ) {
            ProfileScreen()
        }
    }
}

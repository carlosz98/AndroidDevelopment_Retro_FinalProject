package com.example.hubretro

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.hubretro.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GamingPlatform(
    val name: String,
    val iconResId: Int,
    val color: Color,
    val prefix: String
)

val gamingPlatforms = listOf(
    GamingPlatform("PlayStation", R.drawable.ic_playstation, Color(0xFF003791), "PSN"),
    GamingPlatform("Xbox", R.drawable.ic_xbox, Color(0xFF107C10), "Xbox"),
    GamingPlatform("Steam", R.drawable.ic_steam, Color(0xFF1B2838), "Steam"),
    GamingPlatform("Nintendo", R.drawable.ic_nintendo, Color(0xFFE4000F), "Nintendo")
)

data class Game(
    val name: String,
    val imageResId: Int? = null,
    val coverUrl: String? = null
)

data class Soundtrack(
    val title: String,
    val artist: String? = null,
    val imageResId: Int? = null,
    val coverUrl: String? = null
)

data class ActivityItem(
    val id: String = "",
    val description: String,
    val timeAgo: String,
    val type: String = "BOOKMARK",
    val itemTitle: String = "",
    val itemSnippet: String = "",
    val itemImageUrl: String = "",
    val targetUsername: String = "",
    val userProfilePicUrl: String? = null
)

data class UserProfile(
    val username: String = "Don Carlos",
    val userHandle: String = "@logodzip",
    val bio: String = "Retro enthusiast 🎮",
    val profilePictureResId: Int = R.drawable.profile1,
    val bannerImageResId: Int = R.drawable.banner1,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
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
    )
)

fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> String.format("%.1fM", count / 1000000.0).replace(".0M", "M")
        count >= 1000 -> String.format("%.1fK", count / 1000.0).replace(".0K", "K")
        else -> count.toString()
    }
}

fun formatMemberSince(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return "Joined " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    favoritesViewModel: FavoritesViewModel = viewModel(),
    activityViewModel: ActivityViewModel = viewModel(),
    achievementsViewModel: AchievementsViewModel = viewModel()
) {
    val firebaseProfile by authViewModel.userProfile.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val activities by activityViewModel.activities.collectAsState()
    val isLoadingActivity by activityViewModel.isLoading.collectAsState()
    val achievementsState by achievementsViewModel.state.collectAsState()
    val context = LocalContext.current
    val sampleProfile = UserProfile()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("PROFILE", "FAVORITES")
    var isEditing by remember { mutableStateOf(false) }
    var editUsername by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editHandle by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editWebsite by remember { mutableStateOf("") }
    var editPsn by remember { mutableStateOf("") }
    var editXbox by remember { mutableStateOf("") }
    var editSteam by remember { mutableStateOf("") }
    var editNintendo by remember { mutableStateOf("") }
    var showFollowersList by remember { mutableStateOf(false) }
    var showFollowingList by remember { mutableStateOf(false) }
    var selectedActivityArticle by remember { mutableStateOf<ActivityItem?>(null) }
    var isUploadingProfile by remember { mutableStateOf(false) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var editTwitch by remember { mutableStateOf("") }
    var editYoutube by remember { mutableStateOf("") }

    val profilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingProfile = true
            uploadMessage = null
            authViewModel.uploadProfilePicture(it) { success ->
                isUploadingProfile = false
                uploadMessage = if (success) "✅ Profile photo updated!" else "❌ Upload failed"
            }
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingBanner = true
            uploadMessage = null
            authViewModel.uploadBannerPicture(it) { success ->
                isUploadingBanner = false
                uploadMessage = if (success) "✅ Banner updated!" else "❌ Upload failed"
            }
        }
    }

    LaunchedEffect(uploadMessage) {
        if (uploadMessage != null) {
            kotlinx.coroutines.delay(3000L)
            uploadMessage = null
        }
    }

    LaunchedEffect(firebaseProfile) {
        firebaseProfile?.let {
            editUsername = it.username
            editBio = it.bio
            editHandle = it.userHandle
            editLocation = it.location
            editWebsite = it.website
            editPsn = it.psnUsername
            editXbox = it.xboxUsername
            editSteam = it.steamUsername
            editNintendo = it.nintendoUsername
            editTwitch = it.twitchUsername
            editYoutube = it.youtubeUsername

        }
        achievementsViewModel.fetchAchievements()
    }

    val displayUsername = firebaseProfile?.username ?: sampleProfile.username
    val displayHandle = firebaseProfile?.userHandle ?: sampleProfile.userHandle
    val displayBio = firebaseProfile?.bio ?: sampleProfile.bio
    val displayFollowersCount = firebaseProfile?.followersCount ?: 0
    val displayFollowingCount = firebaseProfile?.followingCount ?: 0
    val displayProfilePicUrl = firebaseProfile?.profilePictureUrl
    val displayLocation = firebaseProfile?.location ?: ""
    val displayWebsite = firebaseProfile?.website ?: ""
    val displayMemberSince = formatMemberSince(firebaseProfile?.createdAt ?: 0L)
    val displayPsn = firebaseProfile?.psnUsername ?: ""
    val displayXbox = firebaseProfile?.xboxUsername ?: ""
    val displaySteam = firebaseProfile?.steamUsername ?: ""
    val displayNintendo = firebaseProfile?.nintendoUsername ?: ""
    val displayTwitch = firebaseProfile?.twitchUsername ?: ""
    val displayYoutube = firebaseProfile?.youtubeUsername ?: ""

    val displayGames: List<Game> = remember(firebaseProfile) {
        val fbGames = firebaseProfile?.topGames
        if (!fbGames.isNullOrEmpty()) {
            fbGames.map { gameMap ->
                Game(
                    name = gameMap["name"] as? String ?: "",
                    coverUrl = (gameMap["coverUrl"] as? String)?.ifBlank { null }
                )
            }.filter { it.name.isNotBlank() }
        } else sampleProfile.topGames
    }

    val displaySoundtracks: List<Soundtrack> = remember(firebaseProfile) {
        val fbSoundtracks = firebaseProfile?.topSoundtracks
        if (!fbSoundtracks.isNullOrEmpty()) {
            fbSoundtracks.map { stMap ->
                Soundtrack(
                    title = stMap["name"] as? String ?: "",
                    artist = (stMap["gameName"] as? String)?.ifBlank { null },
                    coverUrl = (stMap["coverUrl"] as? String)?.ifBlank { null }
                )
            }.filter { it.title.isNotBlank() }
        } else sampleProfile.topSoundtracks
    }

    val displayActivities: List<ActivityItem> = remember(activities) {
        activities.map { entry ->
            ActivityItem(
                id = entry.id,
                description = entry.description,
                timeAgo = entry.timeAgoString(),
                type = entry.type,
                itemTitle = entry.itemTitle,
                itemSnippet = entry.itemSnippet,
                itemImageUrl = entry.itemImageUrl,
                targetUsername = entry.targetUsername
            )
        }
    }

    val currentLevel = getRetroLevel(achievementsState.xp)
    val levelProgress = getLevelProgress(achievementsState.xp)
    val profilePicSize = 110.dp
    val bannerHeight = 180.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrapbookCream)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // --- Banner ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight)
                        .then(
                            if (isEditing) Modifier.clickable {
                                bannerPickerLauncher.launch("image/*")
                            } else Modifier
                        )
                ) {
                    val bannerUrl = firebaseProfile?.bannerUrl
                    if (!bannerUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = bannerUrl,
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

                    if (isUploadingBanner) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ScrapbookYellow,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else if (isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(ScrapbookYellow)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = ScrapbookDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "TAP TO CHANGE BANNER",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Level badge top-right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ScrapbookYellow)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = currentLevel.emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LVL ${currentLevel.level} · ${currentLevel.title}",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // --- Profile pic overlapping banner ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-profilePicSize / 2)),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .size(profilePicSize)
                            .clip(CircleShape)
                            .background(ScrapbookCardWhite)
                            .border(4.dp, ScrapbookCardWhite, CircleShape)
                            .then(
                                if (isEditing) Modifier.clickable {
                                    profilePickerLauncher.launch("image/*")
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingProfile) {
                            CircularProgressIndicator(
                                color = ScrapbookYellowDark,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                        } else if (!displayProfilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = displayProfilePicUrl,
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = ScrapbookDark.copy(alpha = 0.4f),
                                    modifier = Modifier.size(40.dp)
                                )
                                if (isEditing) {
                                    Text(
                                        "TAP",
                                        fontFamily = BangersFontFamily,
                                        color = ScrapbookDark.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Username / Handle / XP / Stats ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-profilePicSize / 2))
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Upload toast
                    uploadMessage?.let { message ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ScrapbookYellow.copy(alpha = 0.3f))
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = message,
                                fontFamily = NunitoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ScrapbookDark,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = displayUsername.uppercase(),
                        fontFamily = BangersFontFamily,
                        color = ScrapbookDark,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    if (displayHandle.isNotBlank()) {
                        Text(
                            text = displayHandle,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (displayMemberSince.isNotBlank()) {
                        Text(
                            text = displayMemberSince,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    ScrapbookXPProgressBar(
                        xp = achievementsState.xp,
                        level = currentLevel,
                        progress = levelProgress
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        item {
                            ScrapbookStatCard(
                                value = formatCount(displayFollowersCount),
                                label = "FOLLOWERS",
                                onClick = { showFollowersList = true }
                            )
                        }
                        item {
                            ScrapbookStatCard(
                                value = formatCount(displayFollowingCount),
                                label = "FOLLOWING",
                                onClick = { showFollowingList = true }
                            )
                        }
                        item {
                            ScrapbookStatCard(
                                value = "${achievementsState.articleCount}",
                                label = "ARTICLES",
                                onClick = { selectedTab = 0 }
                            )
                        }
                        item {
                            ScrapbookStatCard(
                                value = "${achievementsState.bookmarkCount}",
                                label = "BOOKMARKS",
                                onClick = { selectedTab = 1 }
                            )
                        }
                        item {
                            ScrapbookStatCard(
                                value = "${achievementsState.xp}",
                                label = "TOTAL XP",
                                onClick = { }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inline tab selector — scrolls with content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
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
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp,
                                    color = ScrapbookDark
                                )
                            }
                            if (index == 0) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(48.dp)
                                        .background(ScrapbookBorder)
                                )
                            }
                        }
                    }
                }
            }

            // --- Tab Content ---
            when (selectedTab) {
                0 -> {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ScrapbookSectionHeader(title = "ACHIEVEMENTS", emoji = "🏆")
                    }

                    item { BadgeShelf(badges = achievementsState.badges) }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookDark)
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            firebaseProfile?.let {
                                                authViewModel.updateUserProfile(
                                                    it.copy(
                                                        username = editUsername,
                                                        bio = editBio,
                                                        userHandle = editHandle,
                                                        location = editLocation,
                                                        website = editWebsite,
                                                        psnUsername = editPsn,
                                                        xboxUsername = editXbox,
                                                        steamUsername = editSteam,
                                                        nintendoUsername = editNintendo,
                                                        twitchUsername = editTwitch,
                                                        youtubeUsername = editYoutube

                                                    )
                                                )
                                            }
                                            isEditing = false
                                            achievementsViewModel.fetchAchievements()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "SAVE",
                                        fontFamily = BangersFontFamily,
                                        fontSize = 16.sp,
                                        color = ScrapbookYellow
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookPaper)
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                        .clickable { isEditing = false }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "CANCEL",
                                        fontFamily = BangersFontFamily,
                                        fontSize = 16.sp,
                                        color = ScrapbookDark
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ScrapbookYellow)
                                        .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                        .clickable { isEditing = true }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        "EDIT PROFILE",
                                        fontFamily = BangersFontFamily,
                                        fontSize = 16.sp,
                                        color = ScrapbookDark
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    if (isEditing) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ScrapbookInputField(
                                    value = editUsername,
                                    onValueChange = { editUsername = it },
                                    label = "USERNAME"
                                )
                                ScrapbookInputField(
                                    value = editHandle,
                                    onValueChange = { editHandle = it },
                                    label = "HANDLE"
                                )
                                ScrapbookInputField(
                                    value = editBio,
                                    onValueChange = { editBio = it },
                                    label = "BIO"
                                )
                                ScrapbookInputField(
                                    value = editLocation,
                                    onValueChange = { editLocation = it },
                                    label = "LOCATION"
                                )
                                ScrapbookInputField(
                                    value = editWebsite,
                                    onValueChange = { editWebsite = it },
                                    label = "WEBSITE"
                                )
                                Text(
                                    "GAMING PLATFORMS",
                                    fontFamily = BangersFontFamily,
                                    color = ScrapbookDark,
                                    fontSize = 18.sp
                                )
                                PlatformInputField(
                                    value = editPsn,
                                    onValueChange = { editPsn = it },
                                    platform = gamingPlatforms[0]
                                )
                                PlatformInputField(
                                    value = editXbox,
                                    onValueChange = { editXbox = it },
                                    platform = gamingPlatforms[1]
                                )
                                PlatformInputField(
                                    value = editSteam,
                                    onValueChange = { editSteam = it },
                                    platform = gamingPlatforms[2]
                                )
                                PlatformInputField(
                                    value = editNintendo,
                                    onValueChange = { editNintendo = it },
                                    platform = gamingPlatforms[3]
                                )
                                // After Nintendo field:
                                ScrapbookInputField(
                                    value = editTwitch,
                                    onValueChange = { editTwitch = it },
                                    label = "TWITCH USERNAME"
                                )
                                ScrapbookInputField(
                                    value = editYoutube,
                                    onValueChange = { editYoutube = it },
                                    label = "YOUTUBE USERNAME"
                                )
                            }
                        }
                    } else {
                        item {
                            ScrapbookSectionHeader(title = "ABOUT ME", emoji = "👤")
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BioCard(bioText = displayBio)

                                if (displayLocation.isNotBlank() || displayWebsite.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (displayLocation.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Filled.LocationOn,
                                                    contentDescription = null,
                                                    tint = ScrapbookDark,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = displayLocation,
                                                    fontFamily = NunitoFontFamily,
                                                    color = ScrapbookTextMuted,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (displayWebsite.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val url =
                                                            if (displayWebsite.startsWith("http"))
                                                                displayWebsite
                                                            else "https://$displayWebsite"
                                                        try {
                                                            context.startActivity(
                                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                            )
                                                        } catch (e: Exception) { }
                                                    }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Language,
                                                    contentDescription = null,
                                                    tint = ScrapbookDark,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = displayWebsite,
                                                    fontFamily = NunitoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ScrapbookDark,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                val platforms = listOf(
                                    gamingPlatforms[0] to displayPsn,
                                    gamingPlatforms[1] to displayXbox,
                                    gamingPlatforms[2] to displaySteam,
                                    gamingPlatforms[3] to displayNintendo
                                ).filter { (_, u) -> u.isNotBlank() }

                                if (platforms.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(platforms) { (platform, username) ->
                                            PlatformBubble(
                                                platform = platform,
                                                username = username
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        ScrapbookSectionHeader(title = "MY TOP 6 GAMES", emoji = "🎮")
                        TopGamesSection(games = displayGames)
                        Spacer(modifier = Modifier.height(20.dp))
                        ScrapbookSectionHeader(title = "MY TOP 3 SOUNDTRACKS", emoji = "🎵")
                        TopSoundtracksSection(soundtracks = displaySoundtracks)
                        Spacer(modifier = Modifier.height(20.dp))
                        ScrapbookSectionHeader(title = "RECENT ACTIVITY", emoji = "⚡")
                        RealActivitySection(
                            activities = displayActivities,
                            isLoading = isLoadingActivity,
                            username = displayUsername,
                            profilePicUrl = displayProfilePicUrl,
                            onArticleClick = { selectedActivityArticle = it }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ScrapbookDark)
                                    .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                    .clickable { authViewModel.signOut() }
                                    .padding(horizontal = 32.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    "SIGN OUT",
                                    fontFamily = BangersFontFamily,
                                    fontSize = 18.sp,
                                    color = ScrapbookYellow
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                1 -> {
                    item {
                        FavoritesScreen(
                            favoritesViewModel = favoritesViewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(800.dp)
                        )
                    }
                }
            }
        }

        // --- Overlays ---
        if (showFollowersList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrapbookCream)
            ) {
                FollowListScreen(
                    listType = FollowListType.FOLLOWERS,
                    authViewModel = authViewModel,
                    onBack = { showFollowersList = false }
                )
            }
        }

        if (showFollowingList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrapbookCream)
            ) {
                FollowListScreen(
                    listType = FollowListType.FOLLOWING,
                    authViewModel = authViewModel,
                    onBack = { showFollowingList = false }
                )
            }
        }

        selectedActivityArticle?.let { activityItem ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrapbookCream)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScrapbookYellow)
                            .border(BorderStroke(2.dp, ScrapbookBorder))
                            .padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedActivityArticle = null }) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Close",
                                    tint = ScrapbookDark
                                )
                            }
                            Text(
                                text = "ARTICLE",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 24.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ArticleCard(
                        article = ArticleItem(
                            id = activityItem.id,
                            title = activityItem.itemTitle,
                            snippet = activityItem.itemSnippet,
                            fullContent = activityItem.itemSnippet,
                            imageUrl = activityItem.itemImageUrl.ifBlank { null }
                        ),
                        gradientColors = articleGradientColorsList[0],
                        initiallyExpanded = true,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ✅ Scrapbook XP Progress Bar
@Composable
fun ScrapbookXPProgressBar(xp: Int, level: RetroLevel, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "xp_progress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${level.emoji} ${level.title}",
                fontFamily = BangersFontFamily,
                color = ScrapbookDark,
                fontSize = 16.sp
            )
            Text(
                text = "$xp XP",
                fontFamily = NunitoFontFamily,
                fontWeight = FontWeight.Bold,
                color = ScrapbookTextMuted,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ScrapbookPaper)
                .border(1.dp, ScrapbookBorder, RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(ScrapbookYellow)
            )
        }
    }
}

// ✅ Scrapbook Stat Card
@Composable
fun ScrapbookStatCard(value: String, label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.width(80.dp)) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    fontFamily = BangersFontFamily,
                    color = ScrapbookDark,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = label,
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// ✅ Scrapbook Section Header
@Composable
fun ScrapbookSectionHeader(title: String, emoji: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 22.sp,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = ScrapbookBorder.copy(alpha = 0.2f),
            thickness = 2.dp
        )
    }
}

// ✅ Scrapbook Input Field
@Composable
fun ScrapbookInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
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
                cursorColor = ScrapbookDark,
                focusedTextColor = ScrapbookTextDark,
                unfocusedTextColor = ScrapbookTextDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BadgeShelf(badges: List<Badge>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(badges, key = { it.id }) { badge ->
            BadgeItem(badge = badge)
        }
    }
}

@Composable
fun BadgeItem(badge: Badge) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            ScrapbookCard(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = if (badge.isEarned) ScrapbookYellow.copy(alpha = 0.3f)
                else ScrapbookPaper,
                borderColor = if (badge.isEarned) ScrapbookBorder
                else ScrapbookBorder.copy(alpha = 0.2f),
                cornerRadius = 28.dp,
                shadowOffset = if (badge.isEarned) 3.dp else 1.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.emoji,
                        fontSize = 22.sp,
                        color = if (badge.isEarned) Color.Unspecified
                        else Color.Black.copy(alpha = 0.2f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = badge.name,
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = if (badge.isEarned) ScrapbookDark else ScrapbookTextMuted.copy(alpha = 0.5f),
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ProfileSectionHeader(title: String, emoji: String, color: Color) {
    ScrapbookSectionHeader(title = title, emoji = emoji)
}

@Composable
fun PlatformBubble(platform: GamingPlatform, username: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ScrapbookDark)
            .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = platform.iconResId),
            contentDescription = platform.name,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "@$username",
            fontFamily = NunitoFontFamily,
            fontWeight = FontWeight.Bold,
            color = ScrapbookYellow,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlatformInputField(
    value: String,
    onValueChange: (String) -> Unit,
    platform: GamingPlatform,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ScrapbookDark)
                .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = platform.iconResId),
                contentDescription = platform.name,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ScrapbookInputField(
            value = value,
            onValueChange = onValueChange,
            label = platform.name,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RealActivitySection(
    activities: List<ActivityItem>,
    isLoading: Boolean,
    username: String,
    profilePicUrl: String?,
    onArticleClick: ((ActivityItem) -> Unit)? = null
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ScrapbookYellowDark,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        activities.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activity yet!\nStart bookmarking or writing articles.",
                    fontFamily = NunitoFontFamily,
                    color = ScrapbookTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                activities.take(5).forEach { activity ->
                    RealActivityFeedItem(
                        activity = activity,
                        username = username,
                        profilePicUrl = profilePicUrl,
                        onArticleClick = onArticleClick
                    )
                }
            }
        }
    }
}

@Composable
fun RealActivityFeedItem(
    activity: ActivityItem,
    username: String,
    profilePicUrl: String?,
    onArticleClick: ((ActivityItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activityIcon = when (activity.type) {
        "ARTICLE" -> Icons.Filled.Create
        "FOLLOW" -> Icons.Filled.PersonAdd
        "JOINED" -> Icons.Filled.Star
        else -> Icons.Filled.Bookmark
    }

    Box(modifier = modifier.padding(vertical = 6.dp)) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 10.dp,
            shadowOffset = 3.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = ScrapbookDark.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = activityIcon,
                                contentDescription = null,
                                tint = ScrapbookDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activity.description,
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
                            text = activity.timeAgo,
                            fontFamily = NunitoFontFamily,
                            color = ScrapbookTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (activity.type == "ARTICLE" && activity.itemTitle.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp, end = 12.dp, bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScrapbookPaper)
                            .border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                            .clickable { onArticleClick?.invoke(activity) }
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = activity.itemTitle,
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "READ ARTICLE →",
                                fontFamily = BangersFontFamily,
                                color = ScrapbookDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameItem(game: Game, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrapbookCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp,
            shadowOffset = 3.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    game.coverUrl != null -> AsyncImage(
                        model = game.coverUrl,
                        contentDescription = game.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    game.imageResId != null -> Image(
                        painter = painterResource(id = game.imageResId),
                        contentDescription = game.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    else -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ScrapbookPaper),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game.name.take(2).uppercase(),
                            fontFamily = BangersFontFamily,
                            color = ScrapbookDark,
                            fontSize = 24.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(ScrapbookDark.copy(alpha = 0.8f))
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = game.name,
                        fontFamily = BangersFontFamily,
                        color = ScrapbookYellow,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun TopGamesSection(games: List<Game>) {
    if (games.isNotEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(750.dp)
        ) {
            items(games.take(6)) { game -> GameItem(game = game) }
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
        Box(modifier = Modifier.size(140.dp)) {
            ScrapbookCard(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = ScrapbookCardWhite,
                cornerRadius = 70.dp,
                shadowOffset = 3.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        soundtrack.coverUrl != null -> AsyncImage(
                            model = soundtrack.coverUrl,
                            contentDescription = soundtrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        soundtrack.imageResId != null -> Image(
                            painter = painterResource(id = soundtrack.imageResId),
                            contentDescription = soundtrack.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(ScrapbookCardWhite, CircleShape)
                            .border(2.dp, ScrapbookBorder, CircleShape)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = soundtrack.title,
            fontFamily = BangersFontFamily,
            color = ScrapbookDark,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        soundtrack.artist?.let {
            Text(
                text = it,
                fontFamily = NunitoFontFamily,
                color = ScrapbookTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
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
fun BioCard(bioText: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ScrapbookCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = ScrapbookCardWhite,
            cornerRadius = 12.dp
        ) {
            Text(
                text = bioText,
                fontFamily = NunitoFontFamily,
                color = ScrapbookTextDark,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontFamily = BangersFontFamily,
        color = ScrapbookDark,
        fontSize = 22.sp,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun RetroInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    ScrapbookInputField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier
    )
}

@Composable
fun XPProgressBar(xp: Int, level: RetroLevel, progress: Float) {
    ScrapbookXPProgressBar(xp = xp, level = level, progress = progress)
}

@Composable
fun StatCard(value: String, label: String, color: Color, onClick: () -> Unit) {
    ScrapbookStatCard(value = value, label = label, onClick = onClick)
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0)
@Composable
fun ProfileScreenPreview() {
    HubRetroTheme {
        ProfileScreen()
    }
}
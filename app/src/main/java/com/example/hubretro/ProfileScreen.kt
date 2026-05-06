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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

    val infiniteTransition = rememberInfiniteTransition(label = "banner_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val currentLevel = getRetroLevel(achievementsState.xp)
    val levelProgress = getLevelProgress(achievementsState.xp)

    val profilePicSize = 120.dp
    val bannerHeight = 180.dp

    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // --- Banner ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
                    .border(
                        2.dp,
                        currentLevel.color.copy(alpha = glowAlpha),
                        RoundedCornerShape(0.dp)
                    )
                    // ✅ Only clickable when editing
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
                        contentDescription = "Profile banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val animatedOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1000f,
                        animationSpec = infiniteRepeatable(
                            tween(4000, easing = LinearEasing),
                            RepeatMode.Reverse
                        ),
                        label = "gradient_offset"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1A1A2E),
                                        currentLevel.color.copy(alpha = 0.4f),
                                        Color(0xFF2A1A3E),
                                        VaporwavePink.copy(alpha = 0.3f)
                                    ),
                                    start = Offset(animatedOffset, 0f),
                                    end = Offset(animatedOffset + 800f, 400f)
                                )
                            )
                    )
                }

                // Upload spinner
                if (isUploadingBanner) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = VaporwavePink,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Uploading banner...",
                                fontFamily = RetroFontFamily,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (isEditing) {
                    // ✅ Only show hint when editing
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
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "TAP TO CHANGE BANNER",
                                fontFamily = RetroFontFamily,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
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
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(
                            1.dp,
                            currentLevel.color.copy(alpha = 0.8f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentLevel.emoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LVL ${currentLevel.level} · ${currentLevel.title}",
                            fontFamily = RetroFontFamily,
                            color = currentLevel.color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Profile picture
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = profilePicSize / 2)
                        .size(profilePicSize)
                        .clip(CircleShape)
                        .background(RetroDarkPurple)
                        .border(3.dp, currentLevel.color.copy(alpha = glowAlpha), CircleShape)
                        // ✅ Only clickable when editing
                        .then(
                            if (isEditing) Modifier.clickable {
                                profilePickerLauncher.launch("image/*")
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingProfile) {
                        CircularProgressIndicator(
                            color = VaporwavePink,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (!displayProfilePicUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = displayProfilePicUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // ✅ Only show camera overlay when editing
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Change photo",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Default avatar",
                                tint = RetroTextOffWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            // ✅ Only show TAP hint when editing
                            if (isEditing) {
                                Text(
                                    "TAP",
                                    fontFamily = RetroFontFamily,
                                    color = RetroTextOffWhite.copy(alpha = 0.5f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height((profilePicSize / 2) + 8.dp))

            // Upload toast
            uploadMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (message.startsWith("✅"))
                                VaporwaveGreen.copy(alpha = 0.2f)
                            else
                                SynthwaveOrange.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (message.startsWith("✅"))
                                VaporwaveGreen.copy(alpha = 0.5f)
                            else
                                SynthwaveOrange.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message,
                        fontFamily = RetroFontFamily,
                        color = if (message.startsWith("✅")) VaporwaveGreen else SynthwaveOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Username / Handle / Member Since
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = displayUsername.uppercase(),
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = currentLevel.color.copy(alpha = 0.7f),
                            offset = Offset(x = 3f, y = 3f),
                            blurRadius = 5f
                        ),
                        textAlign = TextAlign.Center
                    )
                )
                if (displayHandle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayHandle,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                if (displayMemberSince.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayMemberSince,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                XPProgressBar(
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
                        StatCard(
                            value = formatCount(displayFollowersCount),
                            label = "FOLLOWERS",
                            color = VaporwavePink,
                            onClick = { showFollowersList = true }
                        )
                    }
                    item {
                        StatCard(
                            value = formatCount(displayFollowingCount),
                            label = "FOLLOWING",
                            color = VaporwaveCyan,
                            onClick = { showFollowingList = true }
                        )
                    }
                    item {
                        StatCard(
                            value = "${achievementsState.articleCount}",
                            label = "ARTICLES",
                            color = VaporwaveGreen,
                            onClick = { selectedTab = 0 }
                        )
                    }
                    item {
                        StatCard(
                            value = "${achievementsState.bookmarkCount}",
                            label = "BOOKMARKS",
                            color = SynthwaveOrange,
                            onClick = { selectedTab = 1 }
                        )
                    }
                    item {
                        StatCard(
                            value = "${achievementsState.xp} XP",
                            label = "TOTAL XP",
                            color = currentLevel.color,
                            onClick = { }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = RetroTextOffWhite,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = VaporwavePink
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontFamily = RetroFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index)
                                    FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index)
                                    VaporwavePink else RetroTextOffWhite.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileSectionHeader(
                            title = "ACHIEVEMENTS",
                            emoji = "🏆",
                            color = SynthwaveOrange
                        )
                        BadgeShelf(badges = achievementsState.badges)

                        Spacer(modifier = Modifier.height(20.dp))

                        // Edit / Save / Cancel buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isEditing) {
                                Button(
                                    onClick = {
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
                                                    nintendoUsername = editNintendo
                                                )
                                            )
                                        }
                                        isEditing = false
                                        achievementsViewModel.fetchAchievements()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VaporwavePink
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "SAVE",
                                        fontFamily = RetroFontFamily,
                                        fontSize = 12.sp,
                                        color = RetroTextOffWhite
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { isEditing = false },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        RetroTextOffWhite.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        "CANCEL",
                                        fontFamily = RetroFontFamily,
                                        fontSize = 12.sp,
                                        color = RetroTextOffWhite
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { isEditing = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VaporwaveBlue.copy(alpha = 0.7f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "EDIT PROFILE",
                                        fontFamily = RetroFontFamily,
                                        fontSize = 12.sp,
                                        color = RetroTextOffWhite
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isEditing) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RetroInputField(
                                    value = editUsername,
                                    onValueChange = { editUsername = it },
                                    label = "USERNAME"
                                )
                                RetroInputField(
                                    value = editHandle,
                                    onValueChange = { editHandle = it },
                                    label = "HANDLE"
                                )
                                RetroInputField(
                                    value = editBio,
                                    onValueChange = { editBio = it },
                                    label = "BIO"
                                )
                                RetroInputField(
                                    value = editLocation,
                                    onValueChange = { editLocation = it },
                                    label = "LOCATION"
                                )
                                RetroInputField(
                                    value = editWebsite,
                                    onValueChange = { editWebsite = it },
                                    label = "WEBSITE"
                                )
                                Text(
                                    text = "GAMING PLATFORMS",
                                    fontFamily = RetroFontFamily,
                                    color = VaporwavePink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
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
                            }
                        } else {
                            ProfileSectionHeader(
                                title = "ABOUT ME",
                                emoji = "👤",
                                color = VaporwaveCyan
                            )
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
                                                    tint = VaporwaveCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = displayLocation,
                                                    fontFamily = RetroFontFamily,
                                                    color = RetroTextOffWhite.copy(alpha = 0.8f),
                                                    fontSize = 12.sp,
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
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(url)
                                                        )
                                                        try { context.startActivity(intent) }
                                                        catch (e: Exception) { }
                                                    }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Language,
                                                    contentDescription = null,
                                                    tint = VaporwavePink,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = displayWebsite,
                                                    fontFamily = RetroFontFamily,
                                                    color = VaporwavePink,
                                                    fontSize = 12.sp,
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
                                ).filter { (_, username) -> username.isNotBlank() }

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

                        Spacer(modifier = Modifier.height(20.dp))

                        ProfileSectionHeader(
                            title = "MY TOP 6 GAMES",
                            emoji = "🎮",
                            color = SynthwaveOrange
                        )
                        TopGamesSection(games = displayGames)

                        Spacer(modifier = Modifier.height(20.dp))

                        ProfileSectionHeader(
                            title = "MY TOP 3 SOUNDTRACKS",
                            emoji = "🎵",
                            color = VaporwaveCyan
                        )
                        TopSoundtracksSection(soundtracks = displaySoundtracks)

                        Spacer(modifier = Modifier.height(20.dp))

                        ProfileSectionHeader(
                            title = "RECENT ACTIVITY",
                            emoji = "⚡",
                            color = VaporwaveGreen
                        )
                        RealActivitySection(
                            activities = displayActivities,
                            isLoading = isLoadingActivity,
                            username = displayUsername,
                            profilePicUrl = displayProfilePicUrl,
                            onArticleClick = { selectedActivityArticle = it }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { authViewModel.signOut() },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(horizontal = 16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A1A1A)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SynthwaveOrange.copy(alpha = 0.7f))
                        ) {
                            Text(
                                "SIGN OUT",
                                fontFamily = RetroFontFamily,
                                fontSize = 12.sp,
                                color = SynthwaveOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                1 -> {
                    FavoritesScreen(
                        favoritesViewModel = favoritesViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Followers overlay
        if (showFollowersList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E).copy(alpha = 0.98f))
            ) {
                FollowListScreen(
                    listType = FollowListType.FOLLOWERS,
                    authViewModel = authViewModel,
                    onBack = { showFollowersList = false }
                )
            }
        }

        // Following overlay
        if (showFollowingList) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E).copy(alpha = 0.98f))
            ) {
                FollowListScreen(
                    listType = FollowListType.FOLLOWING,
                    authViewModel = authViewModel,
                    onBack = { showFollowingList = false }
                )
            }
        }

        // Article overlay
        selectedActivityArticle?.let { activityItem ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A1A).copy(alpha = 0.98f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedActivityArticle = null }) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "Close",
                                tint = RetroTextOffWhite
                            )
                        }
                        Text(
                            text = "ARTICLE",
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

// --- XP Progress Bar ---
@Composable
fun XPProgressBar(
    xp: Int,
    level: RetroLevel,
    progress: Float
) {
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
                fontFamily = RetroFontFamily,
                color = level.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$xp XP",
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            if (level.maxXP != Int.MAX_VALUE) {
                Text(
                    text = "${level.maxXP + 1} XP",
                    fontFamily = RetroFontFamily,
                    color = RetroTextOffWhite.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                level.color.copy(alpha = 0.7f),
                                level.color
                            )
                        )
                    )
            )
        }
    }
}

// --- Stat Card ---
@Composable
fun StatCard(
    value: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(RetroDarkPurple.copy(alpha = 0.7f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontFamily = RetroFontFamily,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite.copy(alpha = 0.6f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// --- Badge Shelf ---
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

// --- Individual Badge ---
@Composable
fun BadgeItem(badge: Badge) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_glow_${badge.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "badge_glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (badge.isEarned) badge.color.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f)
                )
                .border(
                    width = if (badge.isEarned) 2.dp else 1.dp,
                    color = if (badge.isEarned)
                        badge.color.copy(alpha = glowAlpha)
                    else
                        RetroTextOffWhite.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.emoji,
                fontSize = 24.sp,
                color = if (badge.isEarned) Color.Unspecified
                else Color.White.copy(alpha = 0.3f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = badge.name,
            fontFamily = RetroFontFamily,
            color = if (badge.isEarned) badge.color
            else RetroTextOffWhite.copy(alpha = 0.3f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}

// --- Profile Section Header ---
@Composable
fun ProfileSectionHeader(
    title: String,
    emoji: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = color,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = color.copy(alpha = 0.5f),
                        offset = Offset(2f, 2f),
                        blurRadius = 3f
                    )
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Divider(
                modifier = Modifier.weight(1f),
                color = color.copy(alpha = 0.4f),
                thickness = 1.dp
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

// --- Platform Bubble ---
@Composable
fun PlatformBubble(
    platform: GamingPlatform,
    username: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(platform.color)
            .border(1.dp, platform.color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
            fontFamily = RetroFontFamily,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Platform Input Field ---
@Composable
fun PlatformInputField(
    value: String,
    onValueChange: (String) -> Unit,
    platform: GamingPlatform,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(platform.color),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = platform.iconResId),
                contentDescription = platform.name,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        RetroInputField(
            value = value,
            onValueChange = onValueChange,
            label = platform.name,
            modifier = Modifier.weight(1f)
        )
    }
}

// --- Real Activity Section ---
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = VaporwavePink,
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
                    style = TextStyle(
                        fontFamily = RetroFontFamily,
                        color = RetroTextOffWhite.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
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

// --- Real Activity Feed Item ---
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
    val activityColor = when (activity.type) {
        "ARTICLE" -> VaporwavePink
        "FOLLOW" -> VaporwaveCyan
        "JOINED" -> SynthwaveOrange
        else -> VaporwaveCyan
    }

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
                        .background(RetroDarkPurple)
                        .border(1.dp, VaporwavePink.copy(alpha = 0.5f), CircleShape),
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
                            tint = RetroTextOffWhite.copy(alpha = 0.5f),
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
                            tint = activityColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activity.description,
                            style = TextStyle(
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activity.timeAgo,
                        style = TextStyle(
                            fontFamily = RetroFontFamily,
                            color = RetroTextSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (activity.type == "ARTICLE" && activity.itemTitle.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp, end = 12.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RetroDarkPurple.copy(alpha = 0.8f))
                        .border(
                            1.dp,
                            VaporwavePink.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onArticleClick?.invoke(activity) }
                        .padding(10.dp)
                ) {
                    Column {
                        if (activity.itemImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = activity.itemImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2A2A3A))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(
                            text = activity.itemTitle.uppercase(),
                            fontFamily = RetroFontFamily,
                            color = RetroTextOffWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activity.itemSnippet.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activity.itemSnippet,
                                fontFamily = RetroFontFamily,
                                color = RetroTextOffWhite.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "READ ARTICLE →",
                            fontFamily = RetroFontFamily,
                            color = VaporwavePink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// --- Game Item ---
@Composable
fun GameItem(game: Game, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                        .background(Color(0xFF2A2A3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = game.name.take(2).uppercase(),
                        fontFamily = RetroFontFamily,
                        color = VaporwavePink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = Float.POSITIVE_INFINITY,
                            endY = 0f
                        )
                    )
            )
            Text(
                text = game.name.uppercase(),
                style = TextStyle(
                    fontFamily = RetroFontFamily,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(1f, 1f), 2f)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .fillMaxWidth()
            )
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
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .border(2.dp, VaporwaveCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                soundtrack.coverUrl != null -> AsyncImage(
                    model = soundtrack.coverUrl,
                    contentDescription = soundtrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                soundtrack.imageResId != null -> Image(
                    painter = painterResource(id = soundtrack.imageResId),
                    contentDescription = soundtrack.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(Color.Black, CircleShape)
                    .border(1.dp, VaporwaveCyan.copy(alpha = 0.5f), CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = soundtrack.title,
            style = TextStyle(
                fontFamily = RetroFontFamily,
                color = RetroTextOffWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
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
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = RetroBackgroundAlt.copy(alpha = 0.75f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = bioText,
            style = TextStyle(
                color = RetroTextOffWhite,
                fontFamily = RetroFontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            modifier = Modifier.padding(16.dp)
        )
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
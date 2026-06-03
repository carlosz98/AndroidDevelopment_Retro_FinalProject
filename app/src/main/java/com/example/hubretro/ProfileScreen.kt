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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.hubretro.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.vector.rememberVectorPainter

// ─── Fixed card dimensions ────────────────────────────────────────────────────

private val GAME_IMAGE_HEIGHT = 160.dp
private val GAME_LABEL_HEIGHT = 40.dp

// ─── Neon Glow Helper ─────────────────────────────────────────────────────────

@Composable
fun NeonGlowBox(
    modifier: Modifier = Modifier,
    glowColor: Color = ScrapbookYellow,
    cornerRadius: Dp = 12.dp,
    glowAlpha: Float = 0.6f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.matchParentSize().padding(2.dp).clip(RoundedCornerShape(cornerRadius + 2.dp)).background(glowColor.copy(alpha = glowAlpha * 0.3f)).blur(8.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(cornerRadius)).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(glowColor.copy(alpha = 0.9f), glowColor.copy(alpha = 0.4f), glowColor.copy(alpha = 0.9f))), shape = RoundedCornerShape(cornerRadius)), content = content)
    }
}

// ─── Three Dot Loading Animation ──────────────────────────────────────────────

@Composable
fun ThreeDotsAnimation(color: Color = ScrapbookYellow, dotSize: Dp = 8.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val t = rememberInfiniteTransition(label = "dot_$index")
            val offsetY by t.animateFloat(initialValue = 0f, targetValue = -8f, animationSpec = infiniteRepeatable(tween(500, delayMillis = index * 160, easing = EaseInOut), RepeatMode.Reverse), label = "dotY_$index")
            val alpha by t.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500, delayMillis = index * 160, easing = EaseInOut), RepeatMode.Reverse), label = "dotAlpha_$index")
            Box(modifier = Modifier.size(dotSize).offset(y = offsetY.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
        }
    }
}

// ─── Data Classes ─────────────────────────────────────────────────────────────

data class GamingPlatform(val name: String, val iconResId: Int, val color: Color, val prefix: String)

val gamingPlatforms = listOf(
    GamingPlatform("PlayStation", R.drawable.ic_playstation, Color(0xFF003791), "PSN"),
    GamingPlatform("Xbox", R.drawable.ic_xbox, Color(0xFF107C10), "Xbox"),
    GamingPlatform("Steam", R.drawable.ic_steam, Color(0xFF1B2838), "Steam"),
    GamingPlatform("Nintendo", R.drawable.ic_nintendo, Color(0xFFE4000F), "Nintendo")
)

data class Game(val name: String, val imageResId: Int? = null, val coverUrl: String? = null)
data class Soundtrack(val title: String, val artist: String? = null, val imageResId: Int? = null, val coverUrl: String? = null)

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
        Game("Fez", R.drawable.game1), Game("Final Fantasy XIII", R.drawable.game2),
        Game("Final Fantasy X", R.drawable.game3), Game("Infamous Second Son", R.drawable.game4),
        Game("Minecraft", R.drawable.game5), Game("Cyberpunk 2077", R.drawable.game6)
    ),
    val topSoundtracks: List<Soundtrack> = listOf(
        Soundtrack("Minecraft OST", "C418", R.drawable.vinyl1),
        Soundtrack("The Sims OST", "EA", R.drawable.vinyl2),
        Soundtrack("Undertale OST", "Toby Fox", R.drawable.vinyl3)
    )
)

fun formatCount(count: Int): String = when {
    count >= 1000000 -> String.format("%.1fM", count / 1000000.0).replace(".0M", "M")
    count >= 1000 -> String.format("%.1fK", count / 1000.0).replace(".0K", "K")
    else -> count.toString()
}

fun formatMemberSince(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return "Joined " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

data class GenreScore(val genre: String, val score: Float)

fun detectGenresFromGames(games: List<Game>): List<GenreScore> {
    val genreMap = mapOf(
        "final fantasy" to "RPG", "pokemon" to "RPG", "chrono" to "RPG", "zelda" to "RPG",
        "earthbound" to "RPG", "undertale" to "RPG", "persona" to "RPG", "dragon quest" to "RPG",
        "sonic" to "Action", "devil may cry" to "Action", "god of war" to "Action",
        "batman" to "Action", "infamous" to "Action", "cyberpunk" to "Action",
        "mario" to "Platformer", "kirby" to "Platformer", "crash" to "Platformer",
        "fez" to "Platformer", "hollow knight" to "Platformer",
        "doom" to "Shooter", "halo" to "Shooter", "half-life" to "Shooter",
        "minecraft" to "Adventure", "uncharted" to "Adventure", "tomb raider" to "Adventure",
        "pac-man" to "Arcade", "street fighter" to "Arcade", "tetris" to "Arcade",
        "sims" to "Arcade", "mega man" to "Arcade"
    )
    val scores = mutableMapOf("RPG" to 0f, "Action" to 0f, "Platformer" to 0f, "Shooter" to 0f, "Adventure" to 0f, "Arcade" to 0f)
    games.forEach { game ->
        val n = game.name.lowercase()
        genreMap.forEach { (k, v) -> if (n.contains(k)) scores[v] = (scores[v] ?: 0f) + 1f }
    }
    val total = scores.values.sum()
    if (total == 0f) {
        scores["RPG"] = 0.6f; scores["Action"] = 0.8f; scores["Platformer"] = 0.5f
        scores["Shooter"] = 0.3f; scores["Adventure"] = 0.7f; scores["Arcade"] = 0.4f
    } else {
        val max = scores.values.max()
        scores.keys.forEach { key -> scores[key] = (scores[key] ?: 0f) / max }
    }
    return scores.map { GenreScore(it.key, it.value) }
}



// ─── Profile Screen ───────────────────────────────────────────────────────────

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
    var editTwitch by remember { mutableStateOf("") }
    var editYoutube by remember { mutableStateOf("") }
    var editHabbo by remember { mutableStateOf("") }
    var editHabboRegion by remember { mutableStateOf("habbo.com") }  // ✅ NEW// ✅ Habbo
    var showFollowersList by remember { mutableStateOf(false) }
    var showFollowingList by remember { mutableStateOf(false) }
    var selectedActivityArticle by remember { mutableStateOf<ActivityItem?>(null) }
    var isUploadingProfile by remember { mutableStateOf(false) }
    var isUploadingBanner by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var pinnedArticle by remember { mutableStateOf<ArticleItem?>(null) }
    var userArticles by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }

    val profilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { isUploadingProfile = true; uploadMessage = null; authViewModel.uploadProfilePicture(it) { success -> isUploadingProfile = false; uploadMessage = if (success) "✅ Profile photo updated!" else "❌ Upload failed" } }
    }
    val bannerPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { isUploadingBanner = true; uploadMessage = null; authViewModel.uploadBannerPicture(it) { success -> isUploadingBanner = false; uploadMessage = if (success) "✅ Banner updated!" else "❌ Upload failed" } }
    }

    LaunchedEffect(uploadMessage) { if (uploadMessage != null) { delay(3000L); uploadMessage = null } }

    LaunchedEffect(firebaseProfile) {
        firebaseProfile?.let {
            editUsername = it.username; editBio = it.bio; editHandle = it.userHandle
            editLocation = it.location; editWebsite = it.website; editPsn = it.psnUsername
            editXbox = it.xboxUsername; editSteam = it.steamUsername
            editNintendo = it.nintendoUsername; editTwitch = it.twitchUsername
            editYoutube = it.youtubeUsername; editHabbo = it.habboUsername  // ✅
            editHabboRegion = it.habboRegion.ifBlank { "habbo.com" }  // ✅ NEW
        }
        achievementsViewModel.fetchAchievements()
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            try {
                val db = FirebaseFirestore.getInstance()
                val docs = db.collection("articles").whereEqualTo("authorUid", uid)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10).get().await()
                userArticles = docs.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ArticleItem(id = doc.id, title = data["title"] as? String ?: "", snippet = data["snippet"] as? String ?: "", fullContent = data["fullContent"] as? String ?: "", imageUrl = data["headerImageUrl"] as? String)
                }.filter { it.title.isNotBlank() }
                val pinnedId = db.collection("users").document(uid).get().await().getString("pinnedArticleId")
                if (!pinnedId.isNullOrBlank()) pinnedArticle = userArticles.firstOrNull { it.id == pinnedId }
            } catch (e: Exception) { }
        }
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
    val displayHabbo = firebaseProfile?.habboUsername ?: ""  // ✅
    val displayHabboRegion = firebaseProfile?.habboRegion?.ifBlank { "habbo.com" } ?: "habbo.com"  // ✅ NEW



    val displayGames: List<Game> = remember(firebaseProfile) {
        val fbGames = firebaseProfile?.topGames
        if (!fbGames.isNullOrEmpty()) fbGames.map { Game(name = it["name"] as? String ?: "", coverUrl = (it["coverUrl"] as? String)?.ifBlank { null }) }.filter { it.name.isNotBlank() }
        else sampleProfile.topGames
    }

    val displaySoundtracks: List<Soundtrack> = remember(firebaseProfile) {
        val fb = firebaseProfile?.topSoundtracks
        if (!fb.isNullOrEmpty()) fb.map { Soundtrack(title = it["name"] as? String ?: "", artist = (it["gameName"] as? String)?.ifBlank { null }, coverUrl = (it["coverUrl"] as? String)?.ifBlank { null }) }.filter { it.title.isNotBlank() }
        else sampleProfile.topSoundtracks
    }

    val displayActivities: List<ActivityItem> = remember(activities) {
        activities.map { ActivityItem(id = it.id, description = it.description, timeAgo = it.timeAgoString(), type = it.type, itemTitle = it.itemTitle, itemSnippet = it.itemSnippet, itemImageUrl = it.itemImageUrl, targetUsername = it.targetUsername) }
    }

    val currentLevel = getRetroLevel(achievementsState.xp)
    val levelProgress = getLevelProgress(achievementsState.xp)
    val xpToNext = getXpToNextLevel(achievementsState.xp)
    val profilePicSize = 110.dp
    val bannerHeight = 220.dp
    val genreScores = remember(displayGames) { detectGenresFromGames(displayGames) }

    val neonT = rememberInfiniteTransition(label = "neonGlobal")
    val neonAlpha by neonT.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOut), RepeatMode.Reverse), label = "neonAlpha")

    Box(modifier = modifier.fillMaxSize().background(ScrapbookCream)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ─── Header (Discover style) ──────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow)))
                        .border(BorderStroke(2.dp, ScrapbookBorder))
                        .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PROFILE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 32.sp, letterSpacing = 2.sp)
                            Text(displayHandle.ifBlank { "Your retro identity" }, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        // ✅ Level badge in header
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(ScrapbookDark)
                                .border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(currentLevel.emoji, fontSize = 14.sp)
                                Text("LVL ${currentLevel.level}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // ─── Banner ───────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(bannerHeight)
                        .then(if (isEditing) Modifier.clickable { bannerPickerLauncher.launch("image/*") } else Modifier)
                ) {
                    val bannerUrl = firebaseProfile?.bannerUrl
                    if (!bannerUrl.isNullOrBlank()) {
                        val kbT = rememberInfiniteTransition(label = "bannerKB")
                        val bannerScale by kbT.animateFloat(initialValue = 1f, targetValue = 1.07f, animationSpec = infiniteRepeatable(keyframes { durationMillis = 16000; 1f at 0; 1.07f at 8000; 1f at 16000 }, RepeatMode.Restart), label = "bannerScale")
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))) {
                            AsyncImage(model = bannerUrl, contentDescription = "Banner", modifier = Modifier.fillMaxSize().scale(bannerScale), contentScale = ContentScale.Crop)
                        }
                    } else {
                        val gradT = rememberInfiniteTransition(label = "bannerGrad")
                        val gradOffset by gradT.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse), label = "gradOffset")
                        val scanT = rememberInfiniteTransition(label = "scan")
                        val scanY by scanT.animateFloat(initialValue = -220f, targetValue = 220f, animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "scanLine")
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(ScrapbookDark, ScrapbookYellow.copy(alpha = 0.25f + gradOffset * 0.3f), Color(0xFF1A1A2E)))))
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) { repeat(12) { Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(ScrapbookYellow.copy(alpha = 0.04f))) } }
                        Box(modifier = Modifier.fillMaxSize().offset(y = scanY.dp)) { Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(colors = listOf(Color.Transparent, ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = 0.6f), ScrapbookYellow.copy(alpha = 0.3f), Color.Transparent)))) }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🕹️", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("RETROHUB", fontFamily = BangersFontFamily, color = ScrapbookYellow.copy(alpha = 0.12f), fontSize = 52.sp, letterSpacing = 8.sp)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.55f)))))

                    if (isUploadingBanner) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Uploading banner...", fontFamily = NunitoFontFamily, color = ScrapbookYellow, fontSize = 13.sp) }
                        }
                    } else if (isEditing) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookYellow).border(2.dp, ScrapbookBorder, RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TAP TO CHANGE BANNER", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                            }
                        }
                    }

                    // ✅ Level badge with neon glow on banner
                    val lvlT = rememberInfiniteTransition(label = "lvlPulse")
                    val lvlScale by lvlT.animateFloat(initialValue = 1f, targetValue = 1.04f, animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "lvlScale")
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                        Box(modifier = Modifier.matchParentSize().blur(12.dp).background(currentLevel.color.copy(alpha = neonAlpha * 0.5f), RoundedCornerShape(20.dp)))
                        Box(modifier = Modifier.scale(lvlScale).clip(RoundedCornerShape(20.dp)).background(ScrapbookDark.copy(alpha = 0.92f)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(currentLevel.color, ScrapbookYellow, currentLevel.color)), shape = RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(currentLevel.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LVL ${currentLevel.level} · ${currentLevel.title}", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ─── Profile Picture ──────────────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().offset(y = (-profilePicSize / 2)), contentAlignment = Alignment.TopCenter) {
                    val ringT = rememberInfiniteTransition(label = "ringGlow")
                    val ringAlpha by ringT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "ringAlpha")
                    val ringScale by ringT.animateFloat(initialValue = 1f, targetValue = 1.06f, animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Reverse), label = "ringScale")
                    Box(modifier = Modifier.size(profilePicSize + 28.dp).scale(ringScale).clip(CircleShape).background(ScrapbookYellow.copy(alpha = ringAlpha * 0.25f)))
                    Box(modifier = Modifier.size(profilePicSize + 14.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = ringAlpha)))
                    Box(modifier = Modifier.size(profilePicSize + 6.dp).clip(CircleShape).background(ScrapbookCardWhite))
                    Box(
                        modifier = Modifier.size(profilePicSize).clip(CircleShape).background(ScrapbookCardWhite)
                            .then(if (isEditing) Modifier.clickable { profilePickerLauncher.launch("image/*") } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingProfile) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { ThreeDotsAnimation(color = ScrapbookYellowDark, dotSize = 7.dp) }
                        } else if (!displayProfilePicUrl.isNullOrBlank()) {
                            AsyncImage(model = displayProfilePicUrl, contentDescription = "Profile picture", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                            if (isEditing) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) } }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                if (isEditing) Text("TAP", fontFamily = BangersFontFamily, color = ScrapbookDark.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // ─── Username / Stats / Tabs ──────────────────────────────────────
            item {
                Column(modifier = Modifier.fillMaxWidth().offset(y = (-profilePicSize / 2)).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    uploadMessage?.let { message ->
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.3f)).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                            Text(message, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ✅ Username shimmer
                    val shimmerT = rememberInfiniteTransition(label = "nameShimmer")
                    val shimmerX by shimmerT.animateFloat(initialValue = -400f, targetValue = 800f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "nameShimmerX")
                    Box {
                        Text(displayUsername.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 34.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center)
                        Text(displayUsername.uppercase(), fontFamily = BangersFontFamily, fontSize = 34.sp, letterSpacing = 2.sp, textAlign = TextAlign.Center, style = TextStyle(brush = Brush.linearGradient(colors = listOf(Color.Transparent, ScrapbookYellow.copy(alpha = 0.7f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerX - 150f, 0f), end = androidx.compose.ui.geometry.Offset(shimmerX + 150f, 0f))))
                    }

                    if (displayHandle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(ScrapbookDark.copy(alpha = 0.08f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Text(displayHandle, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                        }
                    }
                    if (displayMemberSince.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = ScrapbookTextMuted, modifier = Modifier.size(11.dp))
                            Text(displayMemberSince, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ScrapbookXPProgressBar(xp = achievementsState.xp, level = currentLevel, progress = levelProgress, xpToNext = xpToNext)
                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ Stats row
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                        item { ScrapbookStatCard(formatCount(displayFollowersCount), "FOLLOWERS", neonAlpha) { showFollowersList = true } }
                        item { ScrapbookStatCard(formatCount(displayFollowingCount), "FOLLOWING", neonAlpha) { showFollowingList = true } }
                        item { ScrapbookStatCard("${achievementsState.articleCount}", "ARTICLES", neonAlpha) { selectedTab = 0 } }
                        item { ScrapbookStatCard("${achievementsState.bookmarkCount}", "BOOKMARKS", neonAlpha) { selectedTab = 1 } }
                        item { ScrapbookStatCard("${achievementsState.xp}", "TOTAL XP", neonAlpha) { } }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ✅ Tab switcher with neon border
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ScrapbookDark).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.8f), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha * 0.8f))), shape = RoundedCornerShape(14.dp)).padding(4.dp)) {
                        Row {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index
                                var tabPressed by remember { mutableStateOf(false) }
                                val tabScale by animateFloatAsState(targetValue = if (tabPressed) 0.94f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tab_$index")
                                Box(modifier = Modifier.weight(1f).scale(tabScale).clip(RoundedCornerShape(11.dp)).background(if (isSelected) ScrapbookYellow else Color.Transparent).clickable { tabPressed = true; selectedTab = index }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                                    Text(title, fontFamily = BangersFontFamily, fontSize = 18.sp, letterSpacing = 1.sp, color = if (isSelected) ScrapbookDark else Color.White.copy(alpha = 0.45f))
                                }
                                LaunchedEffect(tabPressed) { if (tabPressed) { delay(150); tabPressed = false } }
                            }
                        }
                    }
                }
            }

            // ─── Tab Content ──────────────────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        ScrapbookSectionHeader("ACHIEVEMENTS", "🏆")
                    }
                    item { BadgeShelf(badges = achievementsState.badges) }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isEditing) {
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                        .background(ScrapbookDark)
                                        .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.4f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp))
                                        .clickable {
                                            firebaseProfile?.let {
                                                authViewModel.updateUserProfile(it.copy(username = editUsername, bio = editBio, userHandle = editHandle, location = editLocation, website = editWebsite, psnUsername = editPsn, xboxUsername = editXbox, steamUsername = editSteam, nintendoUsername = editNintendo, twitchUsername = editTwitch, youtubeUsername = editYoutube, habboUsername = editHabbo))
                                            }
                                            // ✅ Also update Habbo separately to ensure it's saved
                                            if (editHabbo.isNotBlank()) authViewModel.updateHabboUsername(editHabbo, editHabboRegion)  // ✅ pass region
                                            isEditing = false; achievementsViewModel.fetchAchievements()
                                        }.padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(16.dp))
                                        Text("SAVE PROFILE", fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookYellow)
                                    }
                                }
                                Box(modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).clickable { isEditing = false }.padding(horizontal = 18.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
                                    Text("✕", fontFamily = BangersFontFamily, fontSize = 16.sp, color = ScrapbookDark)
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                        .background(Brush.horizontalGradient(colors = listOf(ScrapbookDark, Color(0xFF2A2A4A))))
                                        .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp))
                                        .clickable { isEditing = true }.padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = ScrapbookYellow, modifier = Modifier.size(16.dp))
                                        Text("EDIT PROFILE", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookYellow, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // ─── Edit Mode ────────────────────────────────────────────
                    if (isEditing) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

                                // ✅ Edit section: BASIC INFO
                                EditSectionLabel("👤 BASIC INFO")
                                EditCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ScrapbookInputField(editUsername, { editUsername = it }, "USERNAME")
                                        ScrapbookInputField(editHandle, { editHandle = it }, "HANDLE")
                                        ScrapbookInputField(editBio, { editBio = it }, "BIO")
                                        ScrapbookInputField(editLocation, { editLocation = it }, "LOCATION")
                                        ScrapbookInputField(editWebsite, { editWebsite = it }, "WEBSITE")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ✅ Edit section: GAMING PLATFORMS
                                EditSectionLabel("🎮 GAMING PLATFORMS")
                                EditCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        PlatformInputField(editPsn, { editPsn = it }, gamingPlatforms[0])
                                        PlatformInputField(editXbox, { editXbox = it }, gamingPlatforms[1])
                                        PlatformInputField(editSteam, { editSteam = it }, gamingPlatforms[2])
                                        PlatformInputField(editNintendo, { editNintendo = it }, gamingPlatforms[3])
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ✅ Edit section: STREAMING
                                EditSectionLabel("📡 STREAMING")
                                EditCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        ScrapbookInputField(editTwitch, { editTwitch = it }, "TWITCH USERNAME")
                                        ScrapbookInputField(editYoutube, { editYoutube = it }, "YOUTUBE USERNAME")
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ✅ Edit section: HABBO HOTEL
                                EditSectionLabel("🏨 HABBO HOTEL")
                                EditCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                        // ✅ Region selector
                                        Text("SELECT YOUR HOTEL", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                                            items(habboRegions) { (label, domain, _) ->
                                                val isSelected = editHabboRegion == domain
                                                var regionPressed by remember { mutableStateOf(false) }
                                                val regionScale by animateFloatAsState(
                                                    targetValue = if (regionPressed) 0.93f else 1f,
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                    label = "regionScale"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .scale(regionScale)
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(if (isSelected) ScrapbookDark else ScrapbookPaper)
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) ScrapbookYellow else ScrapbookBorder,
                                                            shape = RoundedCornerShape(20.dp)
                                                        )
                                                        .clickable { regionPressed = true; editHabboRegion = domain }
                                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontFamily = BangersFontFamily,
                                                        color = if (isSelected) ScrapbookYellow else ScrapbookDark,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                LaunchedEffect(regionPressed) { if (regionPressed) { delay(150); regionPressed = false } }
                                            }
                                        }

                                        // ✅ Selected region display
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ScrapbookDark.copy(alpha = 0.08f))
                                                .border(1.dp, ScrapbookBorder, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("🌐", fontSize = 14.sp)
                                                Text(
                                                    text = "Hotel: www.$editHabboRegion",
                                                    fontFamily = NunitoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ScrapbookDark,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }

                                        // ✅ Username field
                                        ScrapbookInputField(editHabbo, { editHabbo = it }, "HABBO USERNAME")

                                        // ✅ Live preview
                                        if (editHabbo.isNotBlank()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(ScrapbookDark)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = ScrapbookYellow.copy(alpha = 0.6f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(habboAvatarUrl(displayHabbo, displayHabboRegion))
                                                        .crossfade(true)
                                                        .diskCachePolicy(CachePolicy.DISABLED)
                                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                                        .build(),
                                                    contentDescription = "Habbo Avatar Preview",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(ScrapbookDark.copy(alpha = 0.5f)),
                                                    error = rememberVectorPainter(image = Icons.Filled.SmartToy),
                                                    fallback = rememberVectorPainter(image = Icons.Filled.SmartToy)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                        Text("LIVE PREVIEW", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 10.sp)
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(editHabbo, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 16.sp)
                                                    Text("www.$editHabboRegion", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                    Text("🤖 This will be your talking robot", fontFamily = NunitoFontFamily, color = ScrapbookYellow.copy(alpha = 0.6f), fontSize = 10.sp)
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(ScrapbookDark.copy(alpha = 0.08f))
                                                    .border(1.dp, ScrapbookYellow.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Text("🏨", fontSize = 28.sp)
                                                    Column {
                                                        Text("ADD YOUR HABBO USERNAME", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
                                                        Text("Pick your hotel above first!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    } else {
                        // ─── View Mode ────────────────────────────────────────
                        item {
                            ScrapbookSectionHeader("ABOUT ME", "👤")
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BioCard(displayBio)
                                if (displayLocation.isNotBlank() || displayWebsite.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (displayLocation.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(displayLocation, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (displayWebsite.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable {
                                                val url = if (displayWebsite.startsWith("http")) displayWebsite else "https://$displayWebsite"
                                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
                                            }) {
                                                Icon(Icons.Filled.Language, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(displayWebsite, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookDark, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                                val platforms = listOf(gamingPlatforms[0] to displayPsn, gamingPlatforms[1] to displayXbox, gamingPlatforms[2] to displaySteam, gamingPlatforms[3] to displayNintendo).filter { (_, u) -> u.isNotBlank() }
                                if (platforms.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                                        items(platforms) { (platform, username) -> PlatformBubble(platform, username) }
                                    }
                                }
                                StreamBubbles(displayTwitch, displayYoutube, context)

                                // ✅ Habbo avatar display card
                                if (displayHabbo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(ScrapbookDark)
                                            .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.2f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // ✅ Habbo avatar — no cache, always fresh
                                        Box(
                                            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(10.dp)).background(ScrapbookDark.copy(alpha = 0.5f)).border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha), ScrapbookYellow.copy(alpha = 0.3f), ScrapbookYellow.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(habboAvatarUrl(displayHabbo))
                                                    .crossfade(true)
                                                    .diskCachePolicy(CachePolicy.DISABLED)
                                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                                    .build(),
                                                contentDescription = "Habbo Avatar",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookYellow).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                                    Text("🏨 HABBO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 11.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(displayHabbo, fontFamily = BangersFontFamily, color = Color.White, fontSize = 18.sp, letterSpacing = 0.5.sp)
                                            Text("Habbo Hotel avatar", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookYellow.copy(alpha = 0.1f)).border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                Text("🤖 Used as your talking robot", fontFamily = NunitoFontFamily, color = ScrapbookYellow.copy(alpha = 0.8f), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            PinnedArticleSection(pinnedArticle = pinnedArticle, isEditing = isEditing, userArticles = userArticles,
                                onPin = { article -> pinnedArticle = article; currentUser?.uid?.let { uid -> FirebaseFirestore.getInstance().collection("users").document(uid).update("pinnedArticleId", article.id) } },
                                onUnpin = { pinnedArticle = null; currentUser?.uid?.let { uid -> FirebaseFirestore.getInstance().collection("users").document(uid).update("pinnedArticleId", "") } }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ScrapbookSectionHeader("MY TOP 6 GAMES", "🎮")
                        Spacer(modifier = Modifier.height(8.dp))
                        TopGamesSection(displayGames)
                        Spacer(modifier = Modifier.height(24.dp))
                        ScrapbookSectionHeader("MY TOP 3 SOUNDTRACKS", "🎵")
                        TopSoundtracksSection(displaySoundtracks)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ScrapbookSectionHeader("GAMING PERSONALITY", "🎯")
                        Spacer(modifier = Modifier.height(8.dp))
                        RetroRadarChart(genreScores)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ScrapbookSectionHeader("ACTIVITY", "⚡")
                        Spacer(modifier = Modifier.height(8.dp))
                        ActivityStreakSection(displayActivities, firebaseProfile?.createdAt ?: 0L)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        ScrapbookSectionHeader("RECENT ACTIVITY", "📋")
                        RealActivitySection(displayActivities, isLoadingActivity, displayUsername, displayProfilePicUrl) { selectedActivityArticle = it }
                        Spacer(modifier = Modifier.height(24.dp))
                        // ✅ Sign out button
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF8B0000).copy(alpha = 0.12f))
                                .border(width = 2.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookRed.copy(alpha = neonAlpha), ScrapbookRed.copy(alpha = 0.3f), ScrapbookRed.copy(alpha = neonAlpha))), shape = RoundedCornerShape(10.dp))
                                .clickable { authViewModel.signOut() }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Logout, contentDescription = null, tint = ScrapbookRed, modifier = Modifier.size(18.dp))
                                Text("SIGN OUT", fontFamily = BangersFontFamily, fontSize = 18.sp, color = ScrapbookRed, letterSpacing = 1.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                1 -> {
                    item { FavoritesScreen(favoritesViewModel = favoritesViewModel, modifier = Modifier.fillMaxWidth().height(800.dp)) }
                }
            }
        }

        // ─── Overlays ─────────────────────────────────────────────────────────
        if (showFollowersList) {
            Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
                FollowListScreen(listType = FollowListType.FOLLOWERS, authViewModel = authViewModel, onBack = { showFollowersList = false })
            }
        }
        if (showFollowingList) {
            Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
                FollowListScreen(listType = FollowListType.FOLLOWING, authViewModel = authViewModel, onBack = { showFollowingList = false })
            }
        }
        selectedActivityArticle?.let { activityItem ->
            Box(modifier = Modifier.fillMaxSize().background(ScrapbookCream)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, Color(0xFFFFE566), ScrapbookYellow))).border(BorderStroke(2.dp, ScrapbookBorder)).padding(top = 40.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedActivityArticle = null }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close", tint = ScrapbookDark) }
                            Text("ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 24.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ArticleCard(article = ArticleItem(id = activityItem.id, title = activityItem.itemTitle, snippet = activityItem.itemSnippet, fullContent = activityItem.itemSnippet, imageUrl = activityItem.itemImageUrl.ifBlank { null }), gradientColors = articleGradientColorsList[0], initiallyExpanded = true, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─── Edit Section Label ───────────────────────────────────────────────────────

@Composable
fun EditSectionLabel(title: String) {
    Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

// ─── Edit Card ────────────────────────────────────────────────────────────────

@Composable
fun EditCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ScrapbookCardWhite).border(2.dp, ScrapbookBorder, RoundedCornerShape(14.dp)).padding(16.dp)) {
        Column(content = content)
    }
}

// ─── XP Progress Bar ──────────────────────────────────────────────────────────

@Composable
fun ScrapbookXPProgressBar(xp: Int, level: RetroLevel, progress: Float, xpToNext: Int) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1200, easing = LinearOutSlowInEasing), label = "xp_progress")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${level.emoji} ${level.title}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 16.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(1.dp, level.color.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("$xp XP", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(ScrapbookDark).border(1.dp, level.color.copy(alpha = 0.4f), RoundedCornerShape(7.dp))) {
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(7.dp)).background(Brush.horizontalGradient(colors = listOf(ScrapbookYellow, level.color.copy(alpha = 0.8f)))))
            if (animatedProgress > 0f) {
                val shimmerT = rememberInfiniteTransition(label = "xpShimmer")
                val shimmerOffset by shimmerT.animateFloat(initialValue = -1f, targetValue = 2f, animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "xpShimmerOff")
                Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(RoundedCornerShape(7.dp)).background(Brush.linearGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent), start = androidx.compose.ui.geometry.Offset(shimmerOffset * 200f, 0f), end = androidx.compose.ui.geometry.Offset((shimmerOffset + 0.5f) * 200f, 0f))))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val nextText = if (level.maxXP == Int.MAX_VALUE) "MAX LEVEL REACHED 🌟" else "→ NEXT LEVEL: $xpToNext XP away"
        Text(text = nextText, fontFamily = NunitoFontFamily, color = if (level.maxXP == Int.MAX_VALUE) level.color else ScrapbookTextMuted, fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
fun ScrapbookStatCard(value: String, label: String, neonAlpha: Float = 0.6f, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (pressed) 0.90f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "statScale")
    Box(modifier = Modifier.width(82.dp).scale(cardScale)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth().clickable { pressed = true; onClick() }, backgroundColor = ScrapbookDark, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Box(modifier = Modifier.fillMaxWidth().border(width = 1.dp, brush = Brush.linearGradient(colors = listOf(ScrapbookYellow.copy(alpha = neonAlpha * 0.7f), ScrapbookYellow.copy(alpha = 0.1f), ScrapbookYellow.copy(alpha = neonAlpha * 0.7f))), shape = RoundedCornerShape(10.dp))) {
                Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 22.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(label, fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.55f), fontSize = 7.5.sp, textAlign = TextAlign.Center, maxLines = 1, letterSpacing = 0.5.sp)
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(150); pressed = false } }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun ScrapbookSectionHeader(title: String, emoji: String) {
    val glowT = rememberInfiniteTransition(label = "sectionGlow_$title")
    val glowAlpha by glowT.animateFloat(initialValue = 0.3f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse), label = "sectionGlowAlpha")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow.copy(alpha = glowAlpha)))
        Spacer(modifier = Modifier.width(10.dp))
        Text(emoji, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val dotT = rememberInfiniteTransition(label = "hdot_$title$i")
                val dotY by dotT.animateFloat(initialValue = 0f, targetValue = -4f, animationSpec = infiniteRepeatable(tween(400, delayMillis = i * 130, easing = EaseInOut), RepeatMode.Reverse), label = "hdotY_$i")
                Box(modifier = Modifier.size(5.dp).offset(y = dotY.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = glowAlpha)))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Divider(modifier = Modifier.weight(1f), color = ScrapbookBorder.copy(alpha = 0.2f), thickness = 2.dp)
    }
}

// ─── Input Field ──────────────────────────────────────────────────────────────

@Composable
fun ScrapbookInputField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = TextStyle(fontFamily = NunitoFontFamily, fontSize = 14.sp, color = ScrapbookTextDark),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ScrapbookYellow, unfocusedBorderColor = ScrapbookDark.copy(alpha = 0.3f), focusedContainerColor = ScrapbookCardWhite, unfocusedContainerColor = ScrapbookCardWhite, cursorColor = ScrapbookDark, focusedTextColor = ScrapbookTextDark, unfocusedTextColor = ScrapbookTextDark),
            shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Badge Shelf ──────────────────────────────────────────────────────────────

@Composable
fun BadgeShelf(badges: List<Badge>) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(badges, key = { it.id }) { badge -> BadgeItem(badge) }
    }
}

@Composable
fun BadgeItem(badge: Badge) {
    val pulseT = rememberInfiniteTransition(label = "badge_${badge.id}")
    val pulseScale by pulseT.animateFloat(initialValue = 1f, targetValue = if (badge.isEarned) 1.06f else 1f, animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "badgePulse")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp).scale(pulseScale)) {
        Box(modifier = Modifier.size(56.dp)) {
            if (badge.isEarned) { Box(modifier = Modifier.matchParentSize().clip(CircleShape).background(badge.color.copy(alpha = 0.2f))) }
            ScrapbookCard(modifier = Modifier.fillMaxSize(), backgroundColor = if (badge.isEarned) badge.color.copy(alpha = 0.15f) else ScrapbookPaper, borderColor = if (badge.isEarned) badge.color else ScrapbookBorder.copy(alpha = 0.2f), cornerRadius = 28.dp, shadowOffset = if (badge.isEarned) 3.dp else 1.dp) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(badge.emoji, fontSize = 22.sp, color = if (badge.isEarned) Color.Unspecified else Color.Black.copy(alpha = 0.2f)) }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(badge.name, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = if (badge.isEarned) ScrapbookDark else ScrapbookTextMuted.copy(alpha = 0.5f), fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 12.sp)
    }
}

// ─── Platform Bubble ──────────────────────────────────────────────────────────

@Composable
fun PlatformBubble(platform: GamingPlatform, username: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.clip(RoundedCornerShape(20.dp)).background(ScrapbookDark).border(width = 1.5.dp, brush = Brush.linearGradient(colors = listOf(platform.color.copy(alpha = 0.8f), ScrapbookYellow.copy(alpha = 0.4f), platform.color.copy(alpha = 0.8f))), shape = RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("@$username", fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookYellow, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ─── Platform Input Field ─────────────────────────────────────────────────────

@Composable
fun PlatformInputField(value: String, onValueChange: (String) -> Unit, platform: GamingPlatform, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookDark).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Image(painterResource(platform.iconResId), platform.name, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        ScrapbookInputField(value, onValueChange, platform.name, Modifier.weight(1f))
    }
}

// ─── Stream Bubbles ───────────────────────────────────────────────────────────

@Composable
fun StreamBubbles(twitchUsername: String, youtubeUsername: String, context: android.content.Context) {
    if (twitchUsername.isBlank() && youtubeUsername.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (twitchUsername.isNotBlank()) {
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF9146FF)).border(2.dp, Color(0xFFBB86FC), RoundedCornerShape(20.dp)).clickable { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/$twitchUsername"))) } catch (e: Exception) { } }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("🎮", fontSize = 14.sp); Text("TWITCH", fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp) }
            }
        }
        if (youtubeUsername.isNotBlank()) {
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFF0000)).border(2.dp, Color(0xFFFF6659), RoundedCornerShape(20.dp)).clickable { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@$youtubeUsername"))) } catch (e: Exception) { } }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("▶", fontSize = 14.sp, color = Color.White); Text("YOUTUBE", fontFamily = BangersFontFamily, color = Color.White, fontSize = 14.sp) }
            }
        }
    }
}

// ─── Radar Chart ──────────────────────────────────────────────────────────────

@Composable
fun RetroRadarChart(genres: List<GenreScore>) {
    val animProgress by animateFloatAsState(targetValue = 1f, animationSpec = tween(1400, easing = LinearOutSlowInEasing), label = "radar_anim")
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, ScrapbookYellow.copy(alpha = 0.3f), RoundedCornerShape(14.dp))) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯 GAMING PERSONALITY", fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 20.sp, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Based on your top games", fontFamily = NunitoFontFamily, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(220.dp).padding(16.dp)) {
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f
                        val count = genres.size
                        val angleStep = (2 * Math.PI / count).toFloat()
                        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
                            val rp = androidx.compose.ui.graphics.Path()
                            for (i in 0 until count) {
                                val angle = i * angleStep - (Math.PI / 2).toFloat()
                                val x = center.x + radius * ring * kotlin.math.cos(angle)
                                val y = center.y + radius * ring * kotlin.math.sin(angle)
                                if (i == 0) rp.moveTo(x, y) else rp.lineTo(x, y)
                            }
                            rp.close()
                            drawPath(rp, Color.White.copy(alpha = 0.06f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        }
                        for (i in 0 until count) {
                            val angle = i * angleStep - (Math.PI / 2).toFloat()
                            drawLine(Color.White.copy(alpha = 0.08f), center, androidx.compose.ui.geometry.Offset(center.x + radius * kotlin.math.cos(angle), center.y + radius * kotlin.math.sin(angle)), 1.dp.toPx())
                        }
                        val rp = androidx.compose.ui.graphics.Path()
                        genres.forEachIndexed { i, genre ->
                            val angle = i * angleStep - (Math.PI / 2).toFloat()
                            val r = radius * genre.score * animProgress
                            val x = center.x + r * kotlin.math.cos(angle)
                            val y = center.y + r * kotlin.math.sin(angle)
                            if (i == 0) rp.moveTo(x, y) else rp.lineTo(x, y)
                        }
                        rp.close()
                        drawPath(rp, ScrapbookYellow.copy(alpha = 0.25f))
                        drawPath(rp, ScrapbookYellow, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        genres.forEachIndexed { i, genre ->
                            val angle = i * angleStep - (Math.PI / 2).toFloat()
                            val r = radius * genre.score * animProgress
                            val cx = center.x + r * kotlin.math.cos(angle)
                            val cy = center.y + r * kotlin.math.sin(angle)
                            drawCircle(ScrapbookYellow, 5.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy))
                            drawCircle(ScrapbookDark, 2.5f.dp.toPx(), androidx.compose.ui.geometry.Offset(cx, cy))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    genres.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { genre ->
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(ScrapbookYellow))
                                    Text(genre.genre, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text("${(genre.score * 100).toInt()}%", fontFamily = NunitoFontFamily, color = ScrapbookYellow.copy(alpha = 0.7f), fontSize = 10.sp)
                                }
                            }
                            repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

// ─── Activity Streak ──────────────────────────────────────────────────────────

@Composable
fun ActivityStreakSection(activities: List<ActivityItem>, joinedDate: Long) {
    val streakDays = remember(activities) { if (activities.isEmpty()) 0 else (activities.size / 2).coerceIn(1, 30) }
    val longestStreak = remember(activities) { (streakDays + (0..5).random()).coerceIn(streakDays, 60) }
    val heatmapData = remember(activities) {
        List(49) { index ->
            when { index % 7 == 0 -> 0f; activities.size > index / 3 -> (0.4f + (index % 3) * 0.2f).coerceIn(0f, 1f); index % 5 == 0 -> 0.6f; index % 3 == 0 -> 0.3f; else -> 0f }
        }
    }
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚡ ACTIVITY STREAK", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreakStatBox("$streakDays", "CURRENT\nSTREAK", "🔥", Modifier.weight(1f))
                    StreakStatBox("$longestStreak", "LONGEST\nSTREAK", "🏆", Modifier.weight(1f))
                    StreakStatBox("${activities.size}", "TOTAL\nACTIVITIES", "📊", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("LAST 7 WEEKS", fontFamily = BangersFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("M","T","W","T","F","S","S").forEach { day -> Text(day, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = ScrapbookTextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (week in 0 until 7) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (day in 0 until 7) {
                                val intensity = heatmapData.getOrElse(week * 7 + day) { 0f }
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(3.dp)).background(when { intensity >= 0.8f -> ScrapbookDark; intensity >= 0.5f -> ScrapbookYellowDark; intensity >= 0.2f -> ScrapbookYellow.copy(alpha = 0.6f); else -> ScrapbookPaper }).border(1.dp, ScrapbookBorder.copy(alpha = 0.1f), RoundedCornerShape(3.dp)))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Less", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                    listOf(ScrapbookPaper, ScrapbookYellow.copy(alpha = 0.6f), ScrapbookYellowDark, ScrapbookDark).forEach { color -> Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color).border(0.5.dp, ScrapbookBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp))) }
                    Text("More", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun StreakStatBox(value: String, label: String, emoji: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookDark, cornerRadius = 10.dp, shadowOffset = 2.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(label, fontFamily = NunitoFontFamily, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
            }
        }
    }
}

// ─── Pinned Article ───────────────────────────────────────────────────────────

@Composable
fun PinnedArticleSection(pinnedArticle: ArticleItem?, isEditing: Boolean, userArticles: List<ArticleItem>, onPin: (ArticleItem) -> Unit, onUnpin: () -> Unit) {
    if (pinnedArticle == null && !isEditing) return
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookYellow.copy(alpha = 0.15f), borderColor = ScrapbookYellowDark, cornerRadius = 14.dp, shadowOffset = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📌 PINNED ARTICLE", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 20.sp, letterSpacing = 1.sp)
                    if (isEditing && pinnedArticle != null) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ScrapbookRed.copy(alpha = 0.15f)).border(1.dp, ScrapbookRed, RoundedCornerShape(6.dp)).clickable { onUnpin() }.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("UNPIN", fontFamily = BangersFontFamily, color = ScrapbookRed, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (pinnedArticle != null) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ScrapbookCardWhite).border(2.dp, ScrapbookBorder, RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Column {
                            if (!pinnedArticle.imageUrl.isNullOrBlank()) { AsyncImage(model = pinnedArticle.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp))); Spacer(modifier = Modifier.height(8.dp)) }
                            Text(pinnedArticle.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(pinnedArticle.snippet, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                        }
                    }
                } else if (isEditing && userArticles.isNotEmpty()) {
                    Text("Pick an article to pin:", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        userArticles.take(5).forEach { article ->
                            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookCardWhite).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onPin(article) }.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text("📝", fontSize = 16.sp); Text(article.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                } else if (isEditing) {
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("You haven't written any articles yet.\nWrite one to pin it here!", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

// ─── Activity Section ─────────────────────────────────────────────────────────

@Composable
fun RealActivitySection(activities: List<ActivityItem>, isLoading: Boolean, username: String, profilePicUrl: String?, onArticleClick: ((ActivityItem) -> Unit)? = null) {
    when {
        isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { ThreeDotsAnimation(); Text("Loading activity...", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 12.sp) }
        }
        activities.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
            Text("No activity yet!\nStart bookmarking or writing articles.", fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
        else -> Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { activities.take(5).forEach { RealActivityFeedItem(it, username, profilePicUrl, onArticleClick) } }
    }
}

@Composable
fun RealActivityFeedItem(activity: ActivityItem, username: String, profilePicUrl: String?, onArticleClick: ((ActivityItem) -> Unit)? = null, modifier: Modifier = Modifier) {
    val activityIcon = when (activity.type) { "ARTICLE" -> Icons.Filled.Create; "FOLLOW" -> Icons.Filled.PersonAdd; "JOINED" -> Icons.Filled.Star; else -> Icons.Filled.Bookmark }
    Box(modifier = modifier.padding(vertical = 6.dp)) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 10.dp, shadowOffset = 3.dp) {
            Column {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, CircleShape), contentAlignment = Alignment.Center) {
                        if (!profilePicUrl.isNullOrBlank()) AsyncImage(model = profilePicUrl, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Icon(Icons.Filled.Person, contentDescription = null, tint = ScrapbookDark.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(activityIcon, contentDescription = null, tint = ScrapbookDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(activity.description, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activity.timeAgo, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp)
                    }
                }
                if (activity.type == "ARTICLE" && activity.itemTitle.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 12.dp, bottom = 12.dp).clip(RoundedCornerShape(8.dp)).background(ScrapbookPaper).border(2.dp, ScrapbookBorder, RoundedCornerShape(8.dp)).clickable { onArticleClick?.invoke(activity) }.padding(10.dp)) {
                        Column {
                            Text(activity.itemTitle, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("READ ARTICLE →", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Game Item ────────────────────────────────────────────────────────────────

@Composable
fun GameItem(game: Game, index: Int = 0, modifier: Modifier = Modifier) {
    // ✅ Ken Burns zoom effect — zoomed in crop style
    val kbT = rememberInfiniteTransition(label = "gameKB_$index")
    val kbScale by kbT.animateFloat(
        initialValue = 1.12f, targetValue = 1.22f,  // ✅ starts zoomed in, zooms more
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 12000 + index * 1500; 1.12f at 0; 1.22f at (6000 + index * 750); 1.12f at (12000 + index * 1500) },
            repeatMode = RepeatMode.Restart
        ), label = "gameKB_scale_$index"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(targetValue = if (pressed) 0.93f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gamePress_$index")

    Box(modifier = modifier.scale(pressScale).clickable { pressed = true }) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp, shadowOffset = 4.dp) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(GAME_IMAGE_HEIGHT).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                    when {
                        game.coverUrl != null -> AsyncImage(model = game.coverUrl, contentDescription = game.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().scale(kbScale))
                        game.imageResId != null -> Image(painterResource(game.imageResId), game.name, modifier = Modifier.fillMaxSize().scale(kbScale), contentScale = ContentScale.Crop)
                        else -> Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(ScrapbookDark, Color(0xFF2A2A4A)))), contentAlignment = Alignment.Center) {
                            Text(game.name.take(2).uppercase(), fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 32.sp)
                        }
                    }
                    // ✅ Subtle dark overlay at bottom only
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))))
                    // ✅ Rank badge
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp).clip(CircleShape).background(ScrapbookYellow).border(1.5.dp, ScrapbookDark, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 12.sp)
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(GAME_LABEL_HEIGHT).background(ScrapbookDark).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    Text(game.name, fontFamily = BangersFontFamily, color = ScrapbookYellow, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(180); pressed = false } }
}

@Composable
fun TopGamesSection(games: List<Game>) {
    if (games.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        games.take(6).chunked(2).forEachIndexed { rowIndex, rowGames ->
            if (rowIndex > 0) Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowGames.forEachIndexed { colIndex, game ->
                    GameItem(game = game, index = rowIndex * 2 + colIndex, modifier = Modifier.weight(1f))
                }
                if (rowGames.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─── Soundtrack Item ──────────────────────────────────────────────────────────

@Composable
fun SoundtrackItem(soundtrack: Soundtrack, index: Int = 0, modifier: Modifier = Modifier) {
    val spinT = rememberInfiniteTransition(label = "vinyl_$index")
    val spinAngle by spinT.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(7000 + index * 500, easing = LinearEasing), RepeatMode.Restart), label = "vinylAngle_$index")
    val glowT = rememberInfiniteTransition(label = "vinylGlow_$index")
    val glowAlpha by glowT.animateFloat(initialValue = 0.2f, targetValue = 0.7f, animationSpec = infiniteRepeatable(tween(1200 + index * 200, easing = EaseInOut), RepeatMode.Reverse), label = "vinylGlow_$index")

    Column(modifier = modifier.width(155.dp).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // ✅ Outer glow ring
            Box(modifier = Modifier.size(156.dp).clip(CircleShape).background(ScrapbookYellow.copy(alpha = glowAlpha * 0.3f)))
            Box(modifier = Modifier.size(150.dp)) {
                ScrapbookCard(modifier = Modifier.fillMaxSize(), backgroundColor = ScrapbookDark, cornerRadius = 75.dp, shadowOffset = 4.dp) {
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = spinAngle }.clip(CircleShape), contentAlignment = Alignment.Center) {
                        // ✅ Dark vinyl background with groove rings
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF111111)))
                        // ✅ Groove rings
                        listOf(0.95f, 0.85f, 0.75f, 0.65f).forEach { ringFraction ->
                            Box(modifier = Modifier.size((150 * ringFraction).dp).clip(CircleShape).border(0.5.dp, Color.White.copy(alpha = 0.04f), CircleShape))
                        }
                        // ✅ Cover art — FULL SIZE, zoomed in to fill the disc
                        when {
                            soundtrack.coverUrl != null -> AsyncImage(
                                model = soundtrack.coverUrl,
                                contentDescription = soundtrack.title,
                                contentScale = ContentScale.Crop,  // ✅ Crop to fill entire disc
                                modifier = Modifier.size(150.dp).clip(CircleShape),
                                alpha = 0.85f
                            )
                            soundtrack.imageResId != null -> Image(
                                painterResource(soundtrack.imageResId),
                                soundtrack.title,
                                modifier = Modifier.size(150.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop,  // ✅ Crop to fill entire disc
                                alpha = 0.85f
                            )
                        }
                        // ✅ Dark overlay to keep vinyl look
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha = 0.25f)))
                    }
                    // ✅ Center hole
                    Box(modifier = Modifier.align(Alignment.Center).size(22.dp).clip(CircleShape).background(Color(0xFF111111)).border(2.dp, ScrapbookYellow.copy(alpha = 0.4f), CircleShape))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(soundtrack.title, fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
        soundtrack.artist?.let { Text(it, fontFamily = NunitoFontFamily, color = ScrapbookTextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth()) }
    }
}

@Composable
fun TopSoundtracksSection(soundtracks: List<Soundtrack>) {
    if (soundtracks.isEmpty()) return
    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(soundtracks.take(3).size) { index -> SoundtrackItem(soundtracks[index], index) }
    }
}

// ─── Bio Card ─────────────────────────────────────────────────────────────────

@Composable
fun BioCard(bioText: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ScrapbookCard(modifier = Modifier.fillMaxWidth(), backgroundColor = ScrapbookCardWhite, cornerRadius = 12.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(ScrapbookYellow))
                    Text("BIO", fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 14.sp, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(bioText, fontFamily = NunitoFontFamily, color = ScrapbookTextDark, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
    }
}

// ─── Aliases ──────────────────────────────────────────────────────────────────

@Composable fun ProfileSectionHeader(title: String, emoji: String, color: Color) { ScrapbookSectionHeader(title, emoji) }
@Composable fun ProfileSectionTitle(title: String) { Text(title.uppercase(), fontFamily = BangersFontFamily, color = ScrapbookDark, fontSize = 22.sp, letterSpacing = 1.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) }
@Composable fun RetroInputField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) { ScrapbookInputField(value, onValueChange, label, modifier) }
@Composable fun XPProgressBar(xp: Int, level: RetroLevel, progress: Float) { ScrapbookXPProgressBar(xp, level, progress, getXpToNextLevel(xp)) }
@Composable fun StatCard(value: String, label: String, color: Color, onClick: () -> Unit) { ScrapbookStatCard(value, label, onClick = onClick) }

@Preview(showBackground = true, backgroundColor = 0xFFFAF3E0)
@Composable
fun ProfileScreenPreview() { HubRetroTheme { ProfileScreen() } }